package com.ecomap.socio.data.repository

import com.ecomap.socio.data.model.Business
import com.ecomap.socio.domain.repository.BusinessRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock
import java.io.File
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : BusinessRepository {

    override suspend fun createBusiness(business: Business): Result<Business> {
        return try {
            val newBusiness = business.copy(
                createdAt = Clock.System.now().toString(),
                updatedAt = Clock.System.now().toString()
            )

            supabase.from("businesses").insert(newBusiness)

            Result.success(newBusiness)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBusinessByOwnerId(ownerId: String): Result<Business?> {
        return try {
            val business = supabase.from("businesses")
                .select {
                    filter {
                        eq("user_id", ownerId)
                    }
                }.decodeSingleOrNull<Business>()

            Result.success(business)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllBusinessesByOwnerId(ownerId: String): Result<List<Business>> {
        return try {
            val businesses = supabase.from("businesses")
                .select {
                    filter {
                        eq("user_id", ownerId)
                    }
                }.decodeList<Business>()

            Result.success(businesses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusiness(business: Business): Result<Business> {
        return try {
            val updatedBusiness = business.copy(
                updatedAt = Clock.System.now().toString()
            )

            supabase.from("businesses").update(updatedBusiness) {
                filter {
                    eq("id", business.id)
                }
            }

            Result.success(updatedBusiness)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadVerificationDocument(file: File, ownerId: String): Result<String> {
        return try {
            println("📤 Subiendo documento de verificación para usuario: $ownerId")

            // Ruta con carpeta del usuario para mejor organización
            val filePath = "${ownerId}/verification_${System.currentTimeMillis()}.jpg"
            val bucket = supabase.storage.from("verification-documents")

            bucket.upload(filePath, file.readBytes(), upsert = true)

            // Se guarda la RUTA del objeto, no una URL pública.
            //
            // Antes esto llamaba a publicUrl(), que produce un enlace sin
            // caducidad ni autenticación. Como aquí se suben INEs y comprobantes
            // de domicilio, cualquiera con ese enlace podía ver la
            // identificación oficial de un vendedor. Ahora se almacena la ruta
            // y quien necesite verla genera un enlace firmado y temporal con
            // createSignedUrl(), que caduca.
            val publicUrl = filePath
            println("✅ Documento subido exitosamente")

            // ✅ Actualizar el negocio del usuario con la URL del documento
            try {
                println("📝 Buscando negocio del usuario para actualizar URL...")

                // Obtener el negocio más reciente del usuario
                val business = supabase.from("businesses")
                    .select {
                        filter {
                            eq("user_id", ownerId)
                        }
                    }
                    .decodeList<Business>()
                    .maxByOrNull { it.createdAt }

                if (business != null) {
                    println("📝 Actualizando negocio ${business.id} con URL del documento...")

                    supabase.from("businesses")
                        .update({
                            set("document_ine_url", publicUrl)
                        }) {
                            filter {
                                eq("id", business.id)
                            }
                        }

                    println("✅ Negocio actualizado con URL del documento")
                } else {
                    println("⚠️ No se encontró negocio para el usuario")
                }
            } catch (updateError: Exception) {
                println("⚠️ Error al actualizar negocio con URL (continuando): ${updateError.message}")
                // No fallar si no se puede actualizar el negocio
            }

            Result.success(publicUrl)
        } catch (e: Exception) {
            println("❌ Error al subir documento: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateBusinessAvatar(file: File, businessId: String): Result<String> {
        return try {
            println("📸 Subiendo avatar para negocio: $businessId")

            val fileName = "business_${businessId}_${System.currentTimeMillis()}.jpg"
            val bucket = supabase.storage.from("avatars")

            bucket.upload(fileName, file.readBytes())

            val publicUrl = bucket.publicUrl(fileName)
            println("✅ Avatar subido a storage: $publicUrl")

            // ✅ IMPORTANTE: Actualizar la tabla businesses con el nuevo avatar_url
            try {
                println("📝 Actualizando negocio $businessId con nuevo avatar_url...")

                supabase.from("businesses")
                    .update({
                        set("avatar_url", publicUrl)
                    }) {
                        filter {
                            eq("id", businessId)
                        }
                    }

                println("✅ Negocio actualizado con nuevo avatar_url")
            } catch (updateError: Exception) {
                println("❌ Error al actualizar negocio con avatar_url: ${updateError.message}")
                // Si falla la actualización, devolvemos error
                return Result.failure(updateError)
            }

            Result.success(publicUrl)
        } catch (e: Exception) {
            println("❌ Error al subir avatar: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deactivateBusiness(businessId: String): Result<Unit> {
        return try {
            println("🗑️ Desactivando negocio en Supabase: $businessId")

            supabase.from("businesses")
                .update({
                    set("is_active", false)
                    set("deactivated_at", "NOW()")
                }) {
                    filter {
                        eq("id", businessId)
                    }
                }

            println("✅ Negocio desactivado en Supabase")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al desactivar negocio: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun reactivateBusiness(businessId: String): Result<Unit> {
        return try {
            println("🔄 Reactivando negocio en Supabase: $businessId")

            supabase.from("businesses")
                .update({
                    set("is_active", true)
                    set("deactivated_at", null as String?)
                }) {
                    filter {
                        eq("id", businessId)
                    }
                }

            println("✅ Negocio reactivado en Supabase")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al reactivar negocio: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun markApprovalAsSeen(businessId: String): Result<Unit> {
        return try {
            println("👁️ Marcando aprobación como vista en Supabase: $businessId")

            supabase.from("businesses")
                .update({
                    set("approval_seen", true)
                }) {
                    filter {
                        eq("id", businessId)
                    }
                }

            println("✅ Aprobación marcada como vista en Supabase")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Error al marcar aprobación como vista: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
