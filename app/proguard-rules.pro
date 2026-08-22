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
-keep class com.aistudio.shreeshyamstore.pqwzkb.utils.CloudRestorableSnapshot { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotTableCounts { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotEnvelope { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.Category { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.Product { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.Sale { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.Customer { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.UdhaarTransaction { *; }
-keep class com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment { *; }
