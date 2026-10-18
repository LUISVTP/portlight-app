# Reglas R8 para el build de release (Fase 4b).
# Se verificó que `assembleRelease` compila y enlaza con ellas; antes de
# publicar, hacer una prueba en dispositivo (ver docs/RELEASING.md).

# --- kotlinx.serialization (reglas oficiales del proyecto) ---
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Serializadores generados (plugin) del catálogo y sus modelos
-keep,includedescriptorclasses class com.luisvtp.portlight.**$$serializer { *; }
-keepclassmembers class com.luisvtp.portlight.** { *** Companion; }
-keepclasseswithmembers class com.luisvtp.portlight.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Lookups reified (`decodeFromString<T>()`) bajo R8 full mode (AGP 8)
-keepclasseswithmembers,includedescriptorclasses class com.luisvtp.portlight.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**

# --- OkHttp 4 (plataformas opcionales presentes solo en determinados JDKs) ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
