# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# RestoreSnapshotCodec uses Moshi Kotlin reflection for these versioned snapshot models.
# Keep field names and constructors stable for authenticated backup/restore compatibility.
-keep class com.sevenzenlabs.zenmart.utils.CloudRestorableSnapshot { *; }
-keep class com.sevenzenlabs.zenmart.utils.SnapshotTableCounts { *; }
-keep class com.sevenzenlabs.zenmart.utils.SnapshotEnvelope { *; }
-keep class com.sevenzenlabs.zenmart.data.Category { *; }
-keep class com.sevenzenlabs.zenmart.data.Product { *; }
-keep class com.sevenzenlabs.zenmart.data.Sale { *; }
-keep class com.sevenzenlabs.zenmart.data.SaleItem { *; }
-keep class com.sevenzenlabs.zenmart.data.Customer { *; }
-keep class com.sevenzenlabs.zenmart.data.UdhaarTransaction { *; }
-keep class com.sevenzenlabs.zenmart.data.StockAdjustment { *; }
