# Arquitectura

## Visión general

```
┌─────────────────────────────────────────────┐
│  APK único (com.blackberrykeyboard)         │
├─────────────────────────────────────────────┤
│  OnboardingActivity   → guía activación     │
│  SettingsActivity     → prefs + prueba      │
│  KeyboardIME          → InputMethodService  │
│  BlackBerryKeyboardView → UI del teclado    │
│  NativeEngine (JNI)   → motor C++17         │
└─────────────────────────────────────────────┘
```

## Capa Java

| Clase | Rol |
|-------|-----|
| `KeyboardIME` | Servicio IME, conecta con apps |
| `BlackBerryKeyboardView` | Dibuja teclas, gestos, temas |
| `KeyboardPreferences` | SharedPreferences (tema, sonido, escala) |
| `KeyboardSetupHelper` | Detecta si IME está activo/predeterminado |
| `KeySoundPlayer` | Tonos al pulsar |

## Capa nativa (C++)

| Archivo | Rol |
|---------|-----|
| `keyboard_engine.cpp` | Estado de composición, SureType |
| `predictor.cpp` | Trie, bigramas, desambiguación |
| `builtin_words.cpp` | Diccionario base ES/EN |
| `jni_bridge.cpp` | Puente JNI |

## SureType / desambiguación

Al pulsar tecla doble (`Q/W`):

1. `onDualKey(a, b)` llama `predictor.disambiguate()`
2. Compara scores de prefijo `composing+a` vs `composing+b`
3. Elige la letra con mejor coincidencia en el diccionario
4. Actualiza sugerencias y texto en composición

## Flujo de aprendizaje

```
Usuario pulsa espacio
    → commitComposingWord()
    → predictor.learnWord()
    → predictor.learnSequence() (bigrama)
    → saveUserDictionary() al cerrar
```

## Compatibilidad

- **minSdk 24** (Android 7.0 Nougat)
- **targetSdk 34**
- ABIs: armeabi-v7a, arm64-v8a, x86, x86_64

## Estructura de archivos

```
app/src/main/
├── cpp/                 # Motor nativo
├── java/.../
│   ├── ime/             # Teclado
│   ├── prefs/           # Temas y preferencias
│   ├── nativeengine/    # JNI wrapper
│   └── util/            # Sonido, setup
└── res/                 # Layouts, strings, colores
```
