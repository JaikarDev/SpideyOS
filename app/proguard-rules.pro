# Keep SpideyOS classes used via reflection / manifest services
-keep class com.jaikar.spideyos.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.mail.**
-dontwarn javax.activation.**
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }
