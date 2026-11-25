# Market Sales

## Versiones de la aplicación

- Usar dos números: MAYOR.MENOR (por ejemplo, 1.1, 1.2, 2.0).
- Cada entrega con correcciones o cambios pequeños aumenta MENOR en una unidad. Después de 1.9 sigue 1.10.
- Una entrega con cambios grandes aumenta MAYOR y reinicia MENOR a 0; no cambiar de nivel por acumular cambios pequeños.
- Aumentar versionCode en una unidad por cada entrega, también cuando cambia MAYOR.
- La fuente de la versión es defaultConfig en app/build.gradle.kts. La interfaz debe obtenerla de BuildConfig.VERSION_NAME, sin números duplicados.
- Registrar cada entrega en CHANGELOG.md con versión, fecha y cambios concretos.
- La versión 1.1 incluye la consulta de mercadillos cancelados validada por el usuario y la implantación de este sistema de versiones.
- La versión de Room es independiente: solo cambiarla cuando cambie el esquema de base de datos.
