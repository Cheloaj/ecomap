package com.ecomap.socio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecomap.socio.data.model.Product
import com.ecomap.socio.domain.repository.ProductRepository
import com.ecomap.socio.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Idle)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _productDetailState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val productDetailState: StateFlow<UiState<Product>> = _productDetailState.asStateFlow()

    private val _deleteProductState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteProductState: StateFlow<UiState<Unit>> = _deleteProductState.asStateFlow()

    private val _toggleAvailabilityState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val toggleAvailabilityState: StateFlow<UiState<Unit>> = _toggleAvailabilityState.asStateFlow()

    private val _createProductState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val createProductState: StateFlow<UiState<Product>> = _createProductState.asStateFlow()

    private val _updateProductState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val updateProductState: StateFlow<UiState<Product>> = _updateProductState.asStateFlow()

    fun loadProducts(businessId: String) {
        viewModelScope.launch {
            _productsState.value = UiState.Loading
            println("📦 Cargando productos del negocio: $businessId")

            val result = productRepository.getProductsByBusinessId(businessId)
            result.fold(
                onSuccess = { products ->
                    _products.value = products
                    _productsState.value = UiState.Success(products)
                    println("✅ ${products.size} productos cargados")
                },
                onFailure = { error ->
                    println("❌ Error al cargar productos: ${error.message}")
                    _productsState.value = UiState.Error(error.message ?: "Error al cargar productos")
                }
            )
        }
    }

    fun loadProductById(productId: String) {
        viewModelScope.launch {
            _productDetailState.value = UiState.Loading
            println("📦 Cargando producto: $productId")

            val result = productRepository.getProductById(productId)
            result.fold(
                onSuccess = { product ->
                    _selectedProduct.value = product
                    _productDetailState.value = UiState.Success(product)
                    println("✅ Producto cargado: ${product.name}")
                },
                onFailure = { error ->
                    println("❌ Error al cargar producto: ${error.message}")
                    _productDetailState.value = UiState.Error(error.message ?: "Error al cargar producto")
                }
            )
        }
    }

    fun deleteProduct(productId: String, businessId: String) {
        viewModelScope.launch {
            _deleteProductState.value = UiState.Loading
            println("🗑️ Eliminando producto: $productId")

            val result = productRepository.deleteProduct(productId)
            result.fold(
                onSuccess = {
                    _deleteProductState.value = UiState.Success(Unit)
                    println("✅ Producto eliminado exitosamente")
                    // Recargar lista de productos
                    loadProducts(businessId)
                },
                onFailure = { error ->
                    println("❌ Error al eliminar producto: ${error.message}")
                    _deleteProductState.value = UiState.Error(error.message ?: "Error al eliminar producto")
                }
            )
        }
    }

    fun toggleProductAvailability(productId: String, isAvailable: Boolean, businessId: String) {
        viewModelScope.launch {
            _toggleAvailabilityState.value = UiState.Loading
            println("🔄 Cambiando disponibilidad a: ${if (isAvailable) "Disponible" else "Agotado"}")

            val result = productRepository.toggleProductAvailability(productId, isAvailable)
            result.fold(
                onSuccess = {
                    _toggleAvailabilityState.value = UiState.Success(Unit)
                    println("✅ Disponibilidad actualizada")
                    // Actualizar producto seleccionado
                    _selectedProduct.value = _selectedProduct.value?.copy(isAvailable = isAvailable)
                    // Recargar lista de productos
                    loadProducts(businessId)
                },
                onFailure = { error ->
                    println("❌ Error al actualizar disponibilidad: ${error.message}")
                    _toggleAvailabilityState.value = UiState.Error(error.message ?: "Error al actualizar disponibilidad")
                }
            )
        }
    }

    fun resetDeleteState() {
        _deleteProductState.value = UiState.Idle
    }

    fun resetToggleAvailabilityState() {
        _toggleAvailabilityState.value = UiState.Idle
    }

    fun createProductWithImage(product: Product, imageFile: java.io.File?, businessId: String) {
        viewModelScope.launch {
            try {
                _createProductState.value = UiState.Loading
                println("📦 [1/4] Iniciando creación de producto...")

                // 1. Subir imagen si existe
                val imageUrl = if (imageFile != null) {
                    println("📤 [2/4] Subiendo imagen...")
                    val uploadResult = productRepository.uploadProductImage(imageFile, businessId)

                    uploadResult.fold(
                        onSuccess = { url ->
                            println("✅ [2/4] Imagen subida exitosamente: $url")
                            url
                        },
                        onFailure = { error ->
                            println("❌ Error al subir imagen: ${error.message}")
                            _createProductState.value = UiState.Error("Error al subir imagen: ${error.message}")
                            return@launch
                        }
                    )
                } else {
                    println("⏭️ [2/4] Sin imagen, continuando...")
                    null
                }

                // 2. Crear producto con URL de imagen
                println("💾 [3/4] Guardando producto en base de datos...")
                val productWithImage = product.copy(imageUrl = imageUrl)
                val result = productRepository.createProduct(productWithImage)

                result.fold(
                    onSuccess = { createdProduct ->
                        println("✅ [4/4] Producto creado exitosamente: ${createdProduct.name}")
                        println("🎉 Poniendo estado en Success...")
                        _createProductState.value = UiState.Success(createdProduct)
                        println("✅ Estado cambiado a Success correctamente")
                    },
                    onFailure = { error ->
                        println("❌ Error al crear producto en DB: ${error.message}")
                        error.printStackTrace()
                        _createProductState.value = UiState.Error(error.message ?: "Error al crear producto")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error inesperado en createProductWithImage: ${e.message}")
                e.printStackTrace()
                _createProductState.value = UiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun updateProductWithImage(product: Product, imageFile: java.io.File?, businessId: String) {
        viewModelScope.launch {
            _updateProductState.value = UiState.Loading
            println("📦 Actualizando producto...")

            try {
                // 1. Subir nueva imagen si existe
                val imageUrl = if (imageFile != null) {
                    val uploadResult = productRepository.uploadProductImage(imageFile, businessId)
                    uploadResult.getOrNull() ?: run {
                        _updateProductState.value = UiState.Error("Error al subir imagen")
                        return@launch
                    }
                } else {
                    product.imageUrl // Mantener la imagen actual
                }

                // 2. Actualizar producto
                val productWithImage = product.copy(imageUrl = imageUrl)
                val result = productRepository.updateProduct(productWithImage)

                result.fold(
                    onSuccess = { updatedProduct ->
                        _updateProductState.value = UiState.Success(updatedProduct)
                        println("✅ Producto actualizado exitosamente")
                        // Recargar lista de productos
                        loadProducts(businessId)
                        // Actualizar producto seleccionado
                        _selectedProduct.value = updatedProduct
                    },
                    onFailure = { error ->
                        println("❌ Error al actualizar producto: ${error.message}")
                        _updateProductState.value = UiState.Error(error.message ?: "Error al actualizar producto")
                    }
                )
            } catch (e: Exception) {
                println("❌ Error inesperado: ${e.message}")
                _updateProductState.value = UiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun resetCreateProductState() {
        _createProductState.value = UiState.Idle
    }

    fun resetUpdateProductState() {
        _updateProductState.value = UiState.Idle
    }

    fun publishScheduledProducts(businessId: String) {
        viewModelScope.launch {
            println("📅 Verificando productos programados para publicar...")

            val result = productRepository.publishScheduledProducts()
            result.fold(
                onSuccess = { count ->
                    if (count > 0) {
                        println("✅ Se publicaron $count productos programados")
                        // Recargar lista de productos para reflejar cambios
                        loadProducts(businessId)
                    } else {
                        println("ℹ️ No hay productos programados para publicar")
                    }
                },
                onFailure = { error ->
                    println("❌ Error al publicar productos programados: ${error.message}")
                }
            )
        }
    }
}
