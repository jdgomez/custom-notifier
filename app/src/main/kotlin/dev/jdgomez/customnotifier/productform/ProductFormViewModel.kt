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

/** The state and actions of the form for a new product. The texts live here, so they survive configuration changes. */
class ProductFormViewModel(
    private val repository: ProductRepository,
    private val clock: Clock,
    private val zone: () -> ZoneId,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProductFormState())
    val state: StateFlow<ProductFormState> = mutableState

    private val eventChannel = Channel<ProductFormEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var saving = false

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
            val invalid = ProductFormField.entries.filter { !isValid(it, texts.getValue(it)) }
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

    /** The product the texts describe at the current moment, or null when any field is invalid. */
    private fun buildProduct(texts: Map<ProductFormField, String>): Product? {
        if (!ProductFormField.entries.all { isValid(it, texts.getValue(it)) }) return null

        fun number(field: ProductFormField) = texts.getValue(field).toInt()
        return Product.create(
            name = ProductName(texts.getValue(ProductFormField.Name)),
            unitLabel = UnitLabel(texts.getValue(ProductFormField.Unit)),
            packageSize = PackageSize(number(ProductFormField.PackageSize)),
            consumptionRate = ConsumptionRate(number(ProductFormField.ConsumptionUnits), number(ProductFormField.ConsumptionDays)),
            rule = Rule(LeadTime(number(ProductFormField.LeadTime))),
            initialUnits = number(ProductFormField.UnitsNow),
            now = clock.instant(),
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = customNotifierContainer()
                    ProductFormViewModel(container.productRepository, container.clock, container.zone)
                }
            }
    }
}
