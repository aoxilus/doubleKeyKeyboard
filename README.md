# Double Key Keyboard

Teclado Android nativo (C++) con **dos letras por tecla** — un solo APK con app de configuración + teclado IME.

Repositorio: [github.com/aoxilus/doubleKeyKeyboard](https://github.com/aoxilus/doubleKeyKeyboard)
## Documentación

Ver carpeta [`docs/`](docs/README.md):

- [Instalación](docs/installation.md)
- [Guía de usuario](docs/user-guide.md)
- [Configuración](docs/configuration.md)
- [Arquitectura](docs/architecture.md)

## Compilar

```powershell
.\gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

## Características

- Layout Pearl SureType simétrico, teclas grandes (dedos gruesos)
- Texto predictivo SureType + aprendizaje local
- Onboarding para activar y poner como predeterminado
- Temas de color, sonidos, diccionario configurable
- Android 7.0+ (API 24)
