# SwipeClean — Technical Design Document

Documento de diseño técnico v1.0 · Complementa a `PRD.md` · Paquete base `com.example.layouts` · Java

## 1. Objetivo del documento

Traducir los 10 requisitos funcionales del PRD en decisiones de arquitectura, modelo de datos y una secuencia de cambios concreta sobre el código existente, sin reescribir lo que ya funciona (Splash, Login, Register, el patrón de estado central de `PhotoGalleryManager`).

## 2. Arquitectura actual

Activities con lógica inline: cada pantalla construye su propia vista en `onCreate`, sin capa de repositorio ni persistencia. Los datos viven en memoria y desaparecen al cerrar la app.

```
UI:     SplashActivity, LoginActivity, RegisterActivity, HomeActivity
Estado: PhotoGalleryManager (List<PhotoItem> en memoria)
Datos:  8 PhotoItem hardcodeados
```

## 3. Arquitectura propuesta

Capa MVVM mínima: las Activities observan un `ViewModel`, que delega en un `MediaRepository`. El repositorio combina `MediaStore` (contenido real) y **Room** (papelera, estadísticas, logros).

```
UI:          SwipeDeckActivity, TrashActivity, DashboardActivity, FilterSheet
ViewModel:   GalleryViewModel, TrashViewModel, StatsViewModel
Repositorio: MediaRepository, StatsRepository, AchievementRepository
Fuentes:     MediaStore (ContentResolver)  ·  Room DB (trash_entry, stats, achievement)
```

**Decisión**: Room en vez de SharedPreferences o archivos planos — la papelera necesita consultas y las estadísticas necesitan acumular entre sesiones; ambas son consultas relacionales simples.

## 4. Modelo de datos

| Tipo | Origen | Campos clave |
|---|---|---|
| `MediaItem` (nuevo) | Reemplaza a `PhotoItem` | `uri, displayName, sizeBytes, mimeType, dateAdded, albumName, isVideo` |
| `TrashEntry` (nuevo) | Entidad Room | `mediaUri, movedToTrashAt, originalAlbum` |
| `CleanupStats` (nuevo) | Entidad Room, fila única acumulada | `reviewedCount, deletedCount, bytesFreed` |
| `Achievement` (nuevo) | Entidad Room | `thresholdFiles, unlockedAt` (seed: 10/50/100/200/500/1000) |
| `PhotoGalleryManager` (reutilizado) | Se convierte en `MediaRepository` | Conserva "estado central + re-render", respaldado por `MediaStore` + Room |

## 5. Permisos y acceso a medios — cubre RF01

```xml
<!-- Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Android 12 y anteriores -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

Lectura vía `ContentResolver.query()` sobre `MediaStore.Files`, proyectando `DISPLAY_NAME`, `SIZE`, `DATE_ADDED`, `MIME_TYPE`, `BUCKET_DISPLAY_NAME` — el mismo conjunto de campos hoy hardcodeado en `PhotoGalleryManager.createDefault()`.

## 6. Módulo de gestos swipe — cubre RF03, RF04

Un `MediaItem` a la vez (RF02), tarjeta con `onTouchListener` que traduce/rota según el arrastre.

| Gesto | Umbral | Acción |
|---|---|---|
| → derecha | dx > 40% del ancho | `repository.keep(item)` |
| ← izquierda | dx < -40% del ancho | `repository.moveToTrash(item)` → inserta `TrashEntry` |
| ↑ arriba | dy < -40% del alto | `repository.deferReview(item)` → vuelve a la cola |

**Deshacer**: pila `Deque<SwipeAction>` en memoria del `GalleryViewModel`; el botón deshacer hace *pop* y revierte el efecto.

**Decisión**: `onTouchListener` + `ObjectAnimator` en vez de una librería externa de card-stack, para no sumar dependencias de terceros.

## 7. Módulo de papelera — cubre RF05

1. **Papelera de la app**: el swipe izquierdo solo inserta `TrashEntry` en Room; reversible sin permisos adicionales.
2. **Vaciado**: desde `TrashActivity`, agrupa URIs pendientes y llama `MediaStore.createDeleteRequest(resolver, uris)` — el sistema pide confirmación nativa.
3. **Recuperar**: borra la fila `TrashEntry`; el archivo nunca dejó de existir.

El `AlertDialog` que ya existe en `HomeActivity.confirmarEliminacion()` se reutiliza como confirmación previa al vaciado masivo.

## 8. Filtros y ordenamiento — cubre RF06, RF07

Parámetros de una única consulta al repositorio:

- **Filtrar por**: tipo, capturas de pantalla, tamaño, fecha, álbum.
- **Ordenar por**: aleatorio, tamaño, fecha, tipo.

```java
MediaRepository.queue(
    MediaQuery.builder()
        .type(MediaType.ALL)
        .onlyScreenshots(false)
        .sizeRange(null)
        .dateRange(null)
        .album(null)
        .sortBy(SortKey.RANDOM)
        .build()
);
```

## 9. Estadísticas, logros y dashboard — cubre RF08, RF09, RF10

`CleanupStats` acumula en cada gesto: `reviewedCount` siempre sube; `deletedCount`/`bytesFreed` suben solo al vaciar la papelera. `AchievementRepository.checkThresholds()` marca `unlockedAt` al cruzar 10/50/100/200/500/1000. `DashboardActivity` muestra los contadores, logros y almacenamiento del dispositivo (`StatFs`).

## 10. Flujo de pantallas

```
Splash → Login → (Registro opcional) → Solicitud de permisos
                                              │
                                              ▼
                                     Swipe Deck (Home)
                                    ╱      │        ╲
                              Papelera  Filtros   Dashboard
```

## 11. Requisitos no funcionales → decisiones

| RNF | Decisión técnica |
|---|---|
| Privacidad | Sin dependencias de red; todo pasa por `ContentResolver` y Room, locales |
| Seguridad | Permisos solo cuando se necesitan; vaciado delegado a `MediaStore.createDeleteRequest` |
| Confiabilidad | El swipe nunca borra directo — siempre pasa por `TrashEntry` recuperable |
| Rendimiento | Consulta a `MediaStore` paginada; miniaturas vía `loadThumbnail()` con caché |
| Mantenibilidad | Repositorio/ViewModel/UI separados; se extiende `PhotoGalleryManagerTest` |
| Sin conexión | Ninguna pantalla depende de red |

## 12. Plan de migración paso a paso

1. Agregar permisos de medios al `AndroidManifest` y el flujo de solicitud en tiempo de ejecución. **✅ hecho**
2. Convertir `PhotoItem` en `MediaItem`, con datos leídos de `MediaStore`. **✅ hecho**
3. Extraer de `PhotoGalleryManager` la lógica hacia un `MediaRepository` que consulta `MediaStore`. **✅ hecho (lectura real)**
4. Introducir Room con `TrashEntry`, `CleanupStats`, `Achievement`; sembrar las seis metas. **✅ hecho** — entidades con campos publicos (sin depender de retencion de nombres de parametros del compilador) y `AppDatabase` como singleton.
5. Construir `SwipeDeckActivity` con gestos. **✅ hecho** — `onTouchListener` sobre `cardMedia` con traslacion/rotacion; se omitio `GalleryViewModel` por ahora (la Activity llama directo al repositorio en un `ExecutorService`) para no sumar la dependencia de `lifecycle-viewmodel` en esta pasada.
6. Implementar la pila de deshacer y conectar el swipe izquierdo a `TrashEntry`. **✅ hecho** — `undoStack` en memoria; el swipe izquierdo inserta en `TrashEntry` via `MediaRepository.moveToTrash`.
7. Construir `TrashActivity` con recuperar y vaciado masivo via `MediaStore.createDeleteRequest`. **hecho** - `TrashRepository` concentra las operaciones de Room; la Activity elimina las filas solo despues de la confirmacion del sistema.
8. Agregar controles de filtros y orden sobre la consulta del repositorio. **hecho** - `SwipeDeckActivity` construye `MediaQuery` desde un dialogo y recarga la cola.
9. Construir `DashboardActivity` sobre `StatsViewModel`. **hecho** - `DashboardActivity` muestra estadisticas, logros y almacenamiento local mediante `StatFs`.
10. Retirar el flujo legado (`PhotoGalleryManagerTest`, `PhotoItem`, `PhotoGalleryManager`, `HomeActivity` y `MainActivity`). **hecho** - el flujo activo quedó en `SwipeDeckActivity`, `TrashActivity` y `DashboardActivity`.

## 13. Estructura de paquetes propuesta

```
com.example.layouts
├── access/     Splash, Login, Register (sin cambios de fondo)
├── gallery/    SwipeDeckActivity, GalleryViewModel, MediaItem
├── trash/      TrashActivity, TrashViewModel, TrashEntry
├── stats/      DashboardActivity, StatsViewModel, CleanupStats, Achievement
├── data/       MediaRepository, StatsRepository, AchievementRepository, AppDatabase
└── filter/     FilterSheet, MediaQuery
```

## 14. Estrategia de pruebas

**Unitarias**: `MediaRepositoryTest`, `TrashRepositoryTest`, `MediaQueryTest`, `StatsRepositoryTest`, `AchievementRepositoryTest`, `DashboardSummaryTest`.

**Instrumentadas/manuales**: lectura real de `MediaStore` en dispositivo/emulador, flujo de permisos en API 30 y 33+, umbral y animación del swipe, diálogo de confirmación del sistema al vaciar papelera.
