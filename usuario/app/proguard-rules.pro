# ================================================================================================
# ECOMAP USUARIO - PROGUARD RULES PARA MÁXIMA SEGURIDAD
# ================================================================================================

# ---- OPTIMIZACIÓN Y OFUSCACIÓN ----
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ---- OCULTAR MENSAJES DE WARNING ----
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---- OFUSCAR BuildConfig (CRÍTICO PARA OCULTAR KEYS) ----
-keepclassmembers class com.ecomap.usuario.BuildConfig {
    public static final java.lang.String SUPABASE_URL;
    public static final java.lang.String SUPABASE_ANON_KEY;
}

# ---- SUPABASE ----
-keep class io.github.jan.supabase.** { *; }
-keep class * extends io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ---- KOTLINX SERIALIZATION ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.ecomap.usuario.**$$serializer { *; }
-keepclassmembers class com.ecomap.usuario.** {
    *** Companion;
}
-keepclasseswithmembers class com.ecomap.usuario.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }

# ---- KTOR ----
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# ---- HILT / DAGGER ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.** class * { *; }
-keep @javax.inject.** class * { *; }

# ---- OSMDROID ----
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ---- COMPOSE ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---- MODELOS DE DATOS ----
-keep class com.ecomap.usuario.data.model.** { *; }
-keepclassmembers class com.ecomap.usuario.data.model.** { *; }

# ---- SECURITY CRYPTO (EncryptedSharedPreferences) ----
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ---- BIOMETRIC ----
-keep class androidx.biometric.** { *; }

# ---- LOCATION SERVICES ----
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ---- COIL (IMÁGENES) ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- REMOVER LOGS EN PRODUCCIÓN ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ---- MANTENER CRASHLYTICS TRACES ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- PRESERVAR ANOTACIONES PARA REFLECTION ----
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# ---- ENUM ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- PARCELABLE ----
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ---- SERIALIZABLE ----
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================================================================================
# FIN DE REGLAS DE SEGURIDAD
# ================================================================================================
