package dev.jdgomez.customnotifier.productform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jdgomez.customnotifier.customNotifierContainer
import dev.jdgomez.customnotifier.domain.ConsumptionRate
import dev.jdgomez.customnotifier.domain.LeadTime
import dev.jdgomez.customnotifier.domain.PackageSize
import dev.jdgomez.customnotifier.domain.Product
import dev.jdgomez.customnotifier.domain.ProductId
import dev.jdgomez.customnotifier.domain.ProductName
import dev.jdgomez.customnotifier.domain.ProductRepository
import dev.jdgomez.customnotifier.domain.Rule
import dev.jdgomez.customnotifier.domain.UnitLabel
import dev.jdgomez.customnotifier.productlist.statusAt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.ZoneId

/**
 * The state and actions of the form for a new product, or for the stored product [editId]. The texts live here, so they survive
 * configuration changes.
 */
class ProductFormViewModel(
    private val repository: ProductRepository,
    private val clock: Clock,
    private val editId: ProductId? = null,
    private val zone: () -> ZoneId,
) : ViewModel() {
    private val openingMode = if (editId == null) ProductFormMode.New else ProductFormMode.Loading
    private val mutableState = MutableStateFlow(ProductFormState(mode = openingMode))
    val state: StateFlow<ProductFormState> = mutableState

    private val eventChannel = Channel<ProductFormEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /** Set once a save or a delete is under way: the form closes after it, so no other one may start. */
    private var finishing = false

    /** The stored product being edited, as last read or written. */
    private var original: Product? = null

    /** Serializes the operations that read and write the stored product: stock updates, save and delete. */
    private val operations = Mutex()

    init {
        if (editId != null) viewModelScope.launch { load(editId) }
    }

    private suspend fun load(id: ProductId) {
        val product = repository.find(id)
        if (product == null) {
            eventChannel.send(ProductFormEvent.Closed)
            return
        }
        original = product
        val texts = product.toTexts()
        mutableState.value =
            ProductFormState(
                texts = texts,
                preview = product.statusAt(clock.instant(), zone()),
                initialTexts = texts,
                mode = ProductFormMode.Edit(product.name.value),
                stock = product.stockSummary(clock.instant()),
            )
    }

    /** Handles a tap in the stock section or its dialogs; the dialogs' stock after is computed by the domain. */
    fun onStockAction(action: StockAction) {
        val product = original ?: return
        when (val outcome = product.stockOutcome(mutableState.value.stockDialog, action, clock.instant())) {
            is StockOutcome.Show -> setStockDialog(outcome.dialog)
            StockOutcome.Close -> setStockDialog(null)
            is StockOutcome.Confirm -> confirmStock(outcome.dialog)
            StockOutcome.Ignore -> Unit
        }
    }

    /** Recomputes everything that depends on the current moment: the stock, the open dialog's stock after and the preview. */
    fun onTick() {
        val product = original
        val now = clock.instant()
        mutableState.update { current ->
            current.copy(
                preview = buildProduct(current.texts)?.statusAt(now, zone()),
                stock = product?.stockSummary(now) ?: current.stock,
                stockDialog = product?.let { current.stockDialog?.refreshedFor(it, now) } ?: current.stockDialog,
            )
        }
    }

    /** Shows [dialog], or closes the open one when null; nothing opens once a save or delete is under way. */
    private fun setStockDialog(dialog: StockDialog?) {
        if (dialog == null || !finishing) mutableState.update { it.copy(stockDialog = dialog) }
    }

    /**
     * Stores [dialog]'s result now and closes it; the form stays open with its unsaved edits. The update runs under [operations],
     * so a save or delete started meanwhile reads the stored product after it.
     */
    private fun confirmStock(dialog: StockDialog) {
        val id = editId
        if (finishing || id == null || dialog.stockAfter == null) return
        setStockDialog(null)
        viewModelScope.launch {
            operations.withLock {
                val stored = repository.find(id)
                if (stored == null) {
                    eventChannel.send(ProductFormEvent.Closed)
                    return@withLock
                }
                val now = clock.instant()
                val updated = dialog.applyTo(stored, now)
                repository.save(updated)
                original = updated
                mutableState.update { it.copy(stock = updated.stockSummary(now), preview = buildProduct(it.texts)?.statusAt(now, zone())) }
            }
        }
    }

    /** Deletes the stored product and closes the form. */
    fun onDelete() {
        val id = editId
        if (id == null || finishing) return
        finishing = true
        viewModelScope.launch {
            operations.withLock {
                repository.delete(id)
                eventChannel.send(ProductFormEvent.Closed)
            }
        }
    }

    /** Sets [field] to [text] keeping only what the field accepts, and clears the field's error. */
    fun onTextChange(
        field: ProductFormField,
        text: String,
    ) {
        val accepted = (if (field.range == null) text else text.filter(Char::isDigit)).take(field.maxLength)
        mutableState.update { current ->
            val texts = current.texts + (field to accepted)
            current.copy(texts = texts, errors = current.errors - field, preview = buildProduct(texts)?.statusAt(clock.instant(), zone()))
        }
    }

    /**
     * Stores the product when every field is valid; otherwise flags the invalid fields and focuses the first. The stored product is
     * read after any pending stock update, so saving never overwrites one.
     */
    fun onSave() {
        val state = mutableState.value
        if (finishing || state.mode is ProductFormMode.Loading) return
        val invalid = state.fields.filter { !isValid(it, state.texts.getValue(it)) }
        if (invalid.isNotEmpty()) {
            mutableState.update { it.copy(errors = invalid.toSet()) }
            eventChannel.trySend(ProductFormEvent.FocusField(invalid.first()))
            return
        }
        finishing = true
        viewModelScope.launch {
            operations.withLock {
                val stored = editId?.let { repository.find(it) ?: original }
                repository.save(build(state.texts, stored))
                eventChannel.send(ProductFormEvent.Saved)
            }
        }
    }

    /** The product the texts describe at the current moment, or null when any field is invalid or the stored one is not read yet. */
    private fun buildProduct(texts: Map<ProductFormField, String>): Product? {
        val stored = original
        val ready = (editId == null || stored != null) && mutableState.value.fields.all { isValid(it, texts.getValue(it)) }
        return if (ready) build(texts, stored) else null
    }

    /** A new product from valid [texts], or the [stored] one with them applied. */
    private fun build(
        texts: Map<ProductFormField, String>,
        stored: Product?,
    ): Product {
        fun number(field: ProductFormField) = texts.getValue(field).toInt()
        val name = ProductName(texts.getValue(ProductFormField.Name))
        val unitLabel = UnitLabel(texts.getValue(ProductFormField.Unit))
        val packageSize = PackageSize(number(ProductFormField.PackageSize))
        val rate = ConsumptionRate(number(ProductFormField.ConsumptionUnits), number(ProductFormField.ConsumptionDays))
        val rule = Rule(LeadTime(number(ProductFormField.LeadTime)))
        val now = clock.instant()
        if (stored == null) return Product.create(name, unitLabel, packageSize, rate, rule, number(ProductFormField.UnitsNow), now)
        // An unchanged rate must not re-anchor the stock.
        val edited =
            stored
                .rename(name)
                .relabelUnit(unitLabel)
                .changePackageSize(packageSize)
                .changeRule(rule)
        return if (edited.consumptionRate == rate) edited else edited.changeConsumptionRate(rate, now)
    }

    companion object {
        /** The factory for the form of a new product, or of the stored product [editId]. */
        fun factory(editId: ProductId? = null): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    ProductFormViewModel(container.productRepository, container.clock, editId, container.zone)
                }
            }
    }
}

private fun isValid(
    field: ProductFormField,
    text: String,
): Boolean = field.range?.let { text.toIntOrNull() in it } ?: text.isNotBlank()

/** The fields' texts for editing this product: the stock is not editable, so it has none. */
private fun Product.toTexts(): Map<ProductFormField, String> =
    mapOf(
        ProductFormField.Name to name.value,
        ProductFormField.Unit to unitLabel.value,
        ProductFormField.PackageSize to packageSize.units.toString(),
        ProductFormField.ConsumptionUnits to consumptionRate.units.toString(),
        ProductFormField.ConsumptionDays to consumptionRate.days.toString(),
        ProductFormField.LeadTime to rule.leadTime.days.toString(),
    )
