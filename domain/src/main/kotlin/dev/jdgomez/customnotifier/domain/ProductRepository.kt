package dev.jdgomez.customnotifier.domain

import kotlinx.coroutines.flow.Flow

/** Port to the stored products. */
interface ProductRepository {
    /** All products ordered by name (case-insensitive) then identity: the current list first, then a new one after every change. */
    fun observeAll(): Flow<List<Product>>

    /** The product with [id], or null when none is stored. */
    suspend fun find(id: ProductId): Product?

    /** Stores [product], replacing the one with the same identity. */
    suspend fun save(product: Product)

    /** Removes the product with [id]; does nothing when none is stored. */
    suspend fun delete(id: ProductId)
}
