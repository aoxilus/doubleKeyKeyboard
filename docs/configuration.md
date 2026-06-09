# Configuración

Abre la app **Double Key Keyboard** para acceder a todos los ajustes.

## Estado del teclado

La tarjeta superior muestra si el teclado está:

- **Activo y predeterminado** — todo correcto
- **Activo pero no predeterminado** — pulsa "Establecer como predeterminado"
- **No activado** — pulsa "Activar teclado"

## Apariencia

### Tema de colores

| Tema | Descripción |
|------|-------------|
| Pearl Burdeos | Tema burdeos clásico (por defecto) |
| Classic Negro | Negro/gris minimalista |
| Ocean Azul | Azul oscuro alternativo |

### Tamaño de teclas

Control deslizante **100% – 135%** para dedos grandes. Por defecto **115%**.

- Teclas más altas y anchas
- Área táctil ampliada con padding extra
- Rejilla centrada al 94% del ancho de pantalla

## Comportamiento

| Opción | Descripción |
|--------|-------------|
| Sonido al pulsar | Beep corto al tocar teclas |
| Aprender de mis palabras | Guarda palabras al escribir |

## Diccionario

- Muestra el **número de palabras** en el diccionario (base + aprendidas)
- **Borrar palabras aprendidas** — resetea el diccionario personal (no borra el diccionario base)

Los datos se guardan en:

```
/data/data/com.blackberrykeyboard/files/user_dictionary.txt
```

### Diccionarios open source (comprimidos)

Al instalar, la app extrae diccionarios **gzip** desde el APK:

| Archivo | Fuente | Palabras |
|---------|--------|----------|
| `es.txt.gz` | [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (MIT) | ~50 000 |
| `en.txt.gz` | FrequencyWords (MIT) | ~50 000 |
| `names.txt.gz` | Nombres propios frecuentes | ~150 |

El motor C++ usa un **trie** + **beam search** (48 candidatos por tecla) para resolver secuencias SureType como `nicanor` en lugar de `bucaboe`.

Solo en tu dispositivo, sin conexión a internet.

## Probar teclado

El campo de texto al final de la pantalla abre el teclado para pruebas rápidas sin salir de la app.
