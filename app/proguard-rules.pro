# kotlinx.serialization: conserva los serializadores generados de los modelos.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class cl.ventanago.app.**$$serializer { *; }
-keepclassmembers class cl.ventanago.app.** {
    *** Companion;
}
-keepclasseswithmembers class cl.ventanago.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
