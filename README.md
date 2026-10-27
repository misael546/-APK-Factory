# APK Factory

Proyecto base para crear aplicaciones Android y generar APK de prueba mediante GitHub Actions.

## Cómo generar un APK

1. Abre la pestaña **Actions** del repositorio.
2. Selecciona **Build Android APK**.
3. Pulsa **Run workflow** y confirma.
4. Cuando termine correctamente, abre la ejecución y descarga el artefacto **APK-Factory-debug**.
5. Descomprime el ZIP y copia `app-debug.apk` al teléfono Android para instalarlo.

La primera compilación sirve para comprobar que el entorno funciona. El APK de depuración no es una versión firmada para publicar en Google Play.

## Requisitos

GitHub Actions, Java 17, Android SDK Platform 35 y Gradle 8.9.

## Personalizar

El código inicial está en `app/src/main/java/com/apkfactory/starter/MainActivity.java`.
