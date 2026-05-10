# Mi App - Gestión de Galería Fotográfica

Aplicación Android desarrollada en Java como parte de la asignatura de Desarrollo Móvil.
Permite a los usuarios autenticarse y gestionar una galería de fotos desde su dispositivo.

## Características

- Pantalla de bienvenida (Splash Screen)
- Registro e inicio de sesión con validación de campos
- Galería de fotos con nombre y tamaño de cada imagen
- Selección individual o masiva de fotos mediante checkboxes
- Botón "Seleccionar Todo" para marcar todas las fotos
- Eliminación de fotos seleccionadas con diálogo de confirmación

## Flujo de la aplicación
Splash Screen → Login → Registro (opcional) → Galería principal

## Tecnologías
| Herramienta | Detalle |
|---|---|
| Lenguaje | Java |
| Android SDK | API 24 – API 36 |
| Librerías | AndroidX, Material Design |
| Layouts | ConstraintLayout, LinearLayout dinámico |
| IDE | Android Studio |
## Estructura del proyecto
app/src/main/java/com/example/layouts/
├── SplashActivity.java # Pantalla de inicio
├── LoginActivity.java # Inicio de sesión
├── RegisterActivity.java # Registro de usuario
├── HomeActivity.java # Galería principal
└── MainActivity.java # Actividad base

## Requisitos
- Android 7.0 (API 24) o superior
- Android Studio Hedgehog o superior
## Instalación
1. Clona el repositorio
   ```bash
   git clone https://github.com/andresvd95/DesarrolloMovil.git
Abre el proyecto en Android Studio
Ejecuta en un emulador o dispositivo físico
Autor
Andrés — Desarrollo Móvil
