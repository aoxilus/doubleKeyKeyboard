# Instalación

## Un solo APK

Double Key Keyboard se distribuye como **un único archivo APK** que contiene:

1. **La app** — configuración, onboarding, prueba del teclado
2. **El teclado IME** — servicio de entrada que funciona en todas las apps

No necesitas instalar dos apps ni descargar archivos adicionales.

## Pasos

### 1. Instalar el APK

```bash
adb install app-debug.apk
```

O abre el APK desde el explorador de archivos del teléfono.

### 2. Primera ejecución — Onboarding

Al abrir la app por primera vez verás un asistente de 3 pasos:

| Paso | Acción |
|------|--------|
| 1 | Activar Double Key Keyboard en Ajustes del sistema |
| 2 | Elegirlo como teclado predeterminado |
| 3 | Ir a la pantalla de configuración |

### 3. Activar manualmente (si saltaste el onboarding)

**Ajustes → Sistema → Idiomas y entrada → Teclado en pantalla**

- Activa **Double Key Keyboard**
- Toca **Teclado predeterminado** y selecciónalo

### 4. Verificar

Abre la app Double Key Keyboard. El estado debe mostrar:

> Teclado activo y predeterminado ✓

Usa el campo de prueba al final de la pantalla para escribir.

## Emulador Android

Si el teclado en pantalla no aparece en el emulador, desactiva el teclado físico del PC:

- AVD Manager → Editar dispositivo → **Enable keyboard input** desmarcado

O en `config.ini` del AVD: `hw.keyboard = no`
