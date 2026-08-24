# SwipeClean — Product Requirements Document

Documento de producto v1.0 · Proyecto Integrado 1 — Desarrollo Móvil · Android/Java

## 1. Resumen ejecutivo

SwipeClean convierte la limpieza de galería en una decisión rápida por gesto: deslizar a la derecha conserva, a la izquierda envía a la papelera, hacia arriba lo deja pendiente para después. El progreso se mide con estadísticas y logros que motivan seguir liberando espacio.

El repositorio implementa hoy la capa de acceso (splash, login, registro) y un primer módulo de galería con selección múltiple por checkbox y borrado con confirmación, construido sobre datos de ejemplo, no sobre el almacenamiento real del dispositivo. Ese código es la base sobre la que se construye este PRD.

## 2. El problema

Las galerías se llenan de forma constante con fotos, videos, capturas y multimedia de mensajería. Se agrava porque se guardan varias fotos de una misma escena para elegir la mejor, y revisar cientos o miles de archivos uno por uno es tedioso, así que casi nadie lo hace.

- **Acumulación constante**: el contenido multimedia crece cada día.
- **Muchas fotos, una escena**: se guardan variantes en vez de una sola.
- **Revisión manual lenta**: depurar archivo por archivo se abandona rápido.

## 3. Contexto y stakeholders

- **Dónde**: Android, uso diario, dispositivos que capturan multimedia de alta calidad.
- **Para quién**: público joven, usuarios frecuentes de cámara/redes/mensajería.
- **Cómo**: 100% local, sin cuenta ni servidores externos.
- **Distribución**: académica, vía GitHub, sin publicación obligatoria en tiendas.
- **Stakeholders**: usuario final · desarrollador · docente · institución · usuarios de prueba.

## 4. La solución

Un gesto, una decisión, con el archivo a pantalla completa:

| Gesto | Acción | Efecto |
|---|---|---|
| → Derecha | Conservar | El archivo se queda en la galería |
| ← Izquierda | A la papelera | Recuperable hasta el vaciado |
| ↑ Arriba | Revisar después | Pasa a una cola para otra sesión |

El botón **deshacer** y la **papelera temporal** evitan borrados accidentales.

## 5. Gamificación

Estadísticas (eliminados, revisados, espacio liberado), logros por metas y un dashboard de progreso.

Metas de logros: **10 · 50 · 100 · 200 · 500 · 1.000** archivos.

## 6. Objetivos

**General**: desarrollar un prototipo Android que permita revisar, organizar y eliminar fotos y videos con gestos swipe, con estadísticas y logros que motiven liberar espacio de forma sencilla y entretenida.

**Específicos**:
- Interfaz por gestos intuitiva sobre el contenido real de la galería.
- Filtrar y ordenar el contenido antes de revisarlo.
- Papelera temporal recuperable, sin borrados definitivos accidentales.
- Módulo de estadísticas de limpieza y espacio recuperado.
- Sistema de logros ligado a metas de archivos procesados.
- Procesamiento 100% local, sin cuentas ni sincronización.

## 7. Alcance del MVP

**Incluye**: revisión individual de fotos y videos · gestos swipe + deshacer · papelera temporal con confirmación · filtros (tipo, capturas, tamaño, fecha, álbum) · orden (aleatorio, tamaño, fecha, tipo) · estadísticas y espacio liberado · logros y dashboard · funciona local, sin cuenta.

**No incluye (por ahora)**: iOS/web · cuentas y sincronización real · nube/Drive/Photos/iCloud · IA de fotos similares · otros tipos de archivo · monetización · publicación obligatoria en Play Store.

## 8. Punto de partida: qué ya existe

| Pieza | Qué hace hoy | Qué se reutiliza para el MVP |
|---|---|---|
| `SplashActivity` | Bienvenida con retraso fijo | Se mantiene tal cual |
| `LoginActivity` / `RegisterActivity` | Validación de campos vacíos, sin persistencia | Capa de acceso simulada del prototipo |
| `PhotoItem` | Nombre, tamaño, color de miniatura inventados | Base para `MediaItem` |
| `PhotoGalleryManager` | Lista en memoria, selección múltiple, borrado permanente | Base conceptual del repositorio de medios y papelera |
| `HomeActivity` | Lista con checkboxes, confirmación antes de borrar | El patrón "confirmar antes de borrar" y "re-render desde estado" se conservan |

## 9. Requisitos funcionales

| ID | Requisito | Estado | Brecha |
|---|---|---|---|
| RF01 | Acceso a galería: permisos de fotos y videos | ✅ Cumple | `SwipeDeckActivity` solicita permiso en tiempo de ejecución y `MediaRepository` lee `MediaStore` real |
| RF02 | Visualización uno a uno | ✅ Cumple | `SwipeDeckActivity` muestra un `MediaItem` a la vez (navegación Anterior/Siguiente, gestos pendientes en Fase 2) |
| RF03 | Gestos: conservar/papelera/después | ❌ No cumple | Interacción es checkbox + botón, sin swipe |
| RF04 | Deshacer | ❌ No cumple | No existe pila de acciones |
| RF05 | Papelera: recuperar o eliminar definitivo | ⚠️ Parcial | Borrado directo con confirmación, sin estado recuperable |
| RF06 | Filtrado | ❌ No cumple | No existe |
| RF07 | Ordenamiento | ❌ No cumple | No existe |
| RF08 | Estadísticas | ⚠️ Parcial | Solo contador de fotos restantes |
| RF09 | Logros | ❌ No cumple | No existe |
| RF10 | Dashboard | ❌ No cumple | No existe |

## 10. Requisitos no funcionales

| Atributo | Criterio | Estado |
|---|---|---|
| Usabilidad | Interfaz simple, sin tecnicismos | ⚠️ Parcial |
| Rendimiento | Carga fluida por gestos | ❌ Sin validar |
| Privacidad | Todo local, sin servidores | ✅ Cumple |
| Seguridad | Permisos mínimos + confirmación al borrar | ⚠️ Parcial |
| Compatibilidad | Android + Android Studio | ✅ Cumple |
| Confiabilidad | Sin borrado por gesto accidental | ❌ Sin validar |
| Sin conexión | Funciones clave offline | ✅ Cumple |
| Mantenibilidad | Código modular y documentado | ✅ Cumple |

## 11. Metodología de levantamiento de requisitos

1. **Entrevistas**: charlas semiestructuradas con jóvenes — descubren el porqué y necesidades ocultas.
2. **Encuestas**: preguntas cerradas a más usuarios — validan y priorizan a mayor escala.
3. **Prototipado**: pruebas del flujo con usuarios reales — comprueban que el swipe es intuitivo.

## 12. Ruta hacia el MVP

- **Fase 0**: Splash, login, registro, galería de ejemplo con checkbox.
- **Fase 1 (hecho)**: Acceso real a galería y visualización uno a uno → RF01, RF02.
- **Fase 2**: Gestos y deshacer → RF03, RF04.
- **Fase 3**: Papelera recuperable → RF05.
- **Fase 4**: Filtros y orden → RF06, RF07.
- **Fase 5**: Estadísticas, logros y dashboard → RF08, RF09, RF10.

## 13. Riesgos y supuestos

- Permisos de medios distintos en Android 13+ (`READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`) vs. versiones anteriores.
- Borrado real de archivos ajenos a la app requiere confirmación del sistema desde Android 11 (`RecoverableSecurityException` / `createDeleteRequest`).
- Cargar miles de miniaturas sin paginación puede afectar el rendimiento.
- El calendario académico limita la profundidad de filtros/logros; cada fase debe ser demostrable por separado.

## 14. Conclusiones

El problema real es la fricción de limpiar, no solo el espacio ocupado. El swipe convierte esa fricción en una decisión rápida y entretenida. El código existente aporta una capa de acceso funcional y un patrón de estado ya validado con pruebas unitarias; el desarrollo del producto SwipeClean en sí todavía está en construcción, guiado por este documento y su TDD asociado.
