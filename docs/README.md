# Double Key Keyboard — Documentación

Teclado Android nativo (C++) con **dos letras por tecla**, en un **solo APK** que incluye la app de configuración y el teclado IME.

## Índice

| Documento | Descripción |
|-----------|-------------|
| [Instalación](installation.md) | Descarga, activación y teclado predeterminado |
| [Guía de usuario](user-guide.md) | Cómo escribir, SureType, símbolos |
| [Configuración](configuration.md) | Colores, sonidos, diccionario, tamaño de teclas |
| [Arquitectura](architecture.md) | C++, JNI, IME, predictor |

## Resumen rápido

- **minSdk 24** — Android 7.0+
- **Un solo APK** — no hay descargas separadas
- **Onboarding** — guía para activar y poner como predeterminado
- **Ajustes** — temas, sonidos, diccionario aprendido, teclas grandes

## Compilar

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
