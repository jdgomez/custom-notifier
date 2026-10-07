package dev.jdgomez.customnotifier.domain

import java.util.UUID

/** The identity of a product: assigned at creation, unique, never changed by edits. */
@JvmInline
value class ProductId(
    val value: UUID,
)
