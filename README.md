# SwipeClean - Limpieza de galeria por gestos

Aplicacion Android en Java para autenticar usuarios y gestionar una galeria de fotos desde el dispositivo.

Documentacion de producto y diseño tecnico del proyecto SwipeClean (MVP con gestos swipe, papelera, filtros, estadisticas y logros): [`docs/PRD.md`](docs/PRD.md) y [`docs/TDD.md`](docs/TDD.md).

## Estado de la migracion hacia SwipeClean

Migracion principal completada siguiendo el plan de `docs/TDD.md`:

- [x] Paso 1 — Permisos de medios (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`) en el manifest + solicitud en tiempo de ejecución.
- [x] Paso 2 — Modelo `MediaItem` (reemplaza a `PhotoItem`, con datos reales de archivo).
- [x] Paso 3 — `MediaRepository` con lectura real de `MediaStore` (`loadFromDevice`).
- [x] **Fase 1 (RF01 + RF02)** — `SwipeDeckActivity`: pide el permiso en tiempo de ejecución y muestra un archivo real de la galería a la vez. `LoginActivity` ahora navega directamente a `SwipeDeckActivity`.
- [x] Paso 4 — Room (`TrashEntry`, `CleanupStats`, `Achievement` + DAOs y `AppDatabase`).
- [x] **Fase 2 (RF03 + RF04)** — Gestos swipe reales sobre `cardMedia` (derecha = conservar, izquierda = papelera, arriba = después) con animación de traslación/rotación, cola de revisión (`pendingQueue`) y **deshacer** de la última acción (`buttonUndo`).
- [x] Paso 7 — `TrashActivity` + vaciado real (RF05): recuperar entradas desde Room y solicitar el borrado definitivo con confirmación nativa de `MediaStore`.
- [x] Paso 8 — Filtros y orden (RF06, RF07): diálogo en `SwipeDeckActivity` conectado a `MediaQuery`.
- [x] Paso 9 — `DashboardActivity` (RF08, RF09, RF10): estadísticas, logros y almacenamiento del dispositivo.
- [x] Paso 10 — Retirado el flujo legado de `PhotoGalleryManager`, `PhotoItem`, `HomeActivity` y `MainActivity`; el flujo activo usa `SwipeDeckActivity`, Room y MediaStore.

## Caracteristicas

- Pantalla de bienvenida, registro e inicio de sesion
- Revision individual de fotos y videos con gestos swipe
- Deshacer y papelera temporal recuperable
- Filtros y ordenamiento por tipo, capturas, album, tamano y fecha
- Estadisticas, logros y dashboard de almacenamiento

## Arquitectura actual

El flujo activo usa `MediaStore` para leer medios reales, `MediaQuery` para filtros y orden, Room para papelera/estadisticas/logros y Activities separadas para swipe, papelera y dashboard.

## Flujo de la aplicacion

`Splash` -> `Login` -> `Registro` (opcional) -> `Galeria principal`

## Tecnologias

| Herramienta | Detalle |
|---|---|
| Lenguaje | Java |
| Android SDK | API 24 a API 36 |
| Librerias | AndroidX, Material Design |
| UI | XML + `LinearLayout` dinamico |
| IDE recomendado | Android Studio |

## Estructura principal

- `SwipeDeckActivity` — revision por gestos y filtros.
- `TrashActivity` — recuperacion y vaciado confirmado.
- `DashboardActivity` — estadisticas, logros y almacenamiento.
- `MediaRepository` / `MediaQuery` — acceso y consulta de medios.
- `TrashRepository`, `StatsRepository`, `AchievementRepository` — persistencia y progreso.

## Requisitos

- Android 7.0 (API 24) o superior
- Android Studio reciente con soporte para AGP 9.x

## Ejecucion

1. Clonar el repositorio:

```bash
git clone https://github.com/andresvd95/DesarrolloMovil.git
```

2. Abrir el proyecto en Android Studio.
3. Ejecutar en un emulador o dispositivo fisico.

Compilacion por consola:

```powershell
./gradlew.bat assembleDebug
```

## Pruebas

Pruebas unitarias principales: `MediaRepositoryTest`, `MediaQueryTest`, `TrashRepositoryTest`, `StatsRepositoryTest`, `AchievementRepositoryTest` y `DashboardSummaryTest`.

La validación estática de XML, referencias y formato pasa correctamente. La ejecución de Gradle queda pendiente: el wrapper 9.3.1 se bloquea durante `CommandLineTaskParser.parseTasks` incluso con un JDK configurado.

## Autor

Andres VD
