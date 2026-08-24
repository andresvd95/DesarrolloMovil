# SwipeClean - Limpieza de galeria por gestos

Aplicacion Android desarrollada en Java como parte de la asignatura de Desarrollo Movil. Permite a los usuarios autenticarse y gestionar una galeria de fotos desde la aplicacion.

Documentacion de producto y diseño tecnico del proyecto SwipeClean (MVP con gestos swipe, papelera, filtros, estadisticas y logros): [`docs/PRD.md`](docs/PRD.md) y [`docs/TDD.md`](docs/TDD.md).

## Estado de la migracion hacia SwipeClean

En construccion siguiendo el plan de migracion de `docs/TDD.md`:

- [x] Paso 1 — Permisos de medios (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`) en el manifest + solicitud en tiempo de ejecución.
- [x] Paso 2 — Modelo `MediaItem` (reemplaza a `PhotoItem`, con datos reales de archivo).
- [x] Paso 3 — `MediaRepository` con lectura real de `MediaStore` (`loadFromDevice`).
- [x] **Fase 1 (RF01 + RF02)** — `SwipeDeckActivity`: pide el permiso en tiempo de ejecución y muestra un archivo real de la galería a la vez. `LoginActivity` ahora navega aquí en vez de a `HomeActivity`.
- [x] Paso 4 — Room (`TrashEntry`, `CleanupStats`, `Achievement` + DAOs y `AppDatabase`).
- [x] **Fase 2 (RF03 + RF04)** — Gestos swipe reales sobre `cardMedia` (derecha = conservar, izquierda = papelera, arriba = después) con animación de traslación/rotación, cola de revisión (`pendingQueue`) y **deshacer** de la última acción (`buttonUndo`).
- [ ] Paso 7 — `TrashActivity` + vaciado real (RF05 completo: hoy el swipe izquierdo ya inserta en la papelera de Room, falta la pantalla para recuperar o vaciar definitivamente).
- [ ] Paso 8 — Filtros y orden (RF06, RF07).
- [ ] Paso 9 — `DashboardActivity` (RF08 completo, RF09, RF10). Ya existen `StatsRepository` y `AchievementRepository` con pruebas unitarias; falta la pantalla que los muestre.
- [ ] Paso 10 — Migrar/retirar `PhotoGalleryManagerTest`, `PhotoItem`, `PhotoGalleryManager` y `HomeActivity`, que quedaron reemplazados por el flujo real.

## Caracteristicas

- Pantalla de bienvenida (`Splash`)
- Registro e inicio de sesion con validacion basica
- Galeria de fotos con nombre y tamano por elemento
- Seleccion individual o masiva mediante checkboxes
- Confirmacion previa antes de eliminar fotos
- Actualizacion inmediata del contador despues del borrado

## Correccion aplicada

El problema original era que la pantalla eliminaba solo parte de la representacion visual y podia dejar elementos separados de la fila original. Ahora la galeria se maneja desde un estado central y la interfaz se reconstruye desde ese estado cada vez que cambia.

Cambios principales:

- Se creo `PhotoItem` como modelo de cada foto.
- Se creo `PhotoGalleryManager` para manejar seleccion y eliminacion.
- `HomeActivity` ahora renderiza la lista completa desde `PhotoGalleryManager`.
- Al eliminar fotos tambien desaparecen los divisores sobrantes.
- Se agrego una prueba unitaria para validar el borrado selectivo.

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

- [HomeActivity.java](/C:/Proyectos Android Studio/Actividad1/app/src/main/java/com/example/layouts/HomeActivity.java)
- [PhotoGalleryManager.java](/C:/Proyectos Android Studio/Actividad1/app/src/main/java/com/example/layouts/PhotoGalleryManager.java)
- [PhotoItem.java](/C:/Proyectos Android Studio/Actividad1/app/src/main/java/com/example/layouts/PhotoItem.java)
- [PhotoGalleryManagerTest.java](/C:/Proyectos Android Studio/Actividad1/app/src/test/java/com/example/layouts/PhotoGalleryManagerTest.java)

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

Prueba unitaria agregada:

```powershell
./gradlew.bat testDebugUnitTest --tests com.example.layouts.PhotoGalleryManagerTest
```

En este entorno la app compilo correctamente con `assembleDebug` y las fuentes de test tambien compilaron con `compileDebugUnitTestJavaWithJavac`. La ejecucion completa de `testDebugUnitTest` no pudo finalizar aqui por un fallo del worker de pruebas de Gradle al iniciar el proceso.

## Autor

Andres - Desarrollo Movil
