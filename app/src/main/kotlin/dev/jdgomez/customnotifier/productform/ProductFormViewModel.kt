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

    private var saving = false

    /** The stored product being edited, once read. */
    private var original: Product? = null

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
            )
    }

    /** Deletes the stored product and closes the form. */
    fun onDelete() {
        val id = editId ?: return
        viewModelScope.launch {
            repository.delete(id)
            eventChannel.send(ProductFormEvent.Closed)
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

    /** Stores the product when every field is valid; otherwise flags the invalid fields and focuses the first. */
    fun onSave() {
        val texts = mutableState.value.texts
        val product = buildProduct(texts)
        if (product == null) {
            val invalid = mutableState.value.fields.filter { !isValid(it, texts.getValue(it)) }
            mutableState.update { it.copy(errors = invalid.toSet()) }
            eventChannel.trySend(ProductFormEvent.FocusField(invalid.first()))
        } else if (!saving) {
            saving = true
            viewModelScope.launch {
                repository.save(product)
                eventChannel.send(ProductFormEvent.Saved)
            }
        }
    }

    private fun isValid(
        field: ProductFormField,
        text: String,
    ): Boolean = field.range?.let { text.toIntOrNull() in it } ?: text.isNotBlank()

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

    private fun Product.toTexts(): Map<ProductFormField, String> =
        mapOf(
            ProductFormField.Name to name.value,
            ProductFormField.Unit to unitLabel.value,
            ProductFormField.PackageSize to packageSize.units.toString(),
            ProductFormField.ConsumptionUnits to consumptionRate.units.toString(),
            ProductFormField.ConsumptionDays to consumptionRate.days.toString(),
            ProductFormField.LeadTime to rule.leadTime.days.toString(),
            ProductFormField.UnitsNow to "",
        )

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
