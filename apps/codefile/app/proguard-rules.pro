# Keep our native entry points and the grammar engine stable for diagnostics.
-keep class com.TechHurts.CodeFile.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-keep class org.joni.** { *; }
-keep class org.jcodings.** { *; }
-dontwarn org.slf4j.impl.StaticLoggerBinder
# Registry models are deserialized reflectively by Gson.
-keep class io.github.rosemoe.sora.** { *; }

# JavaParser metamodel and symbol solver use reflective AST metadata.
-keep class com.github.javaparser.ast.** { *; }
-keep class com.github.javaparser.metamodel.** { *; }
# Unused Javassist JVM class-definition path, never used by the local reflection solver.
-dontwarn java.lang.Module
