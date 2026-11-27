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

## Modalidades (acuerdos confirmados para 2.0)

- Seguir docs/MODALIDADES_2_0.md. Mantener Demo, gratuito y Premium como estados diferentes.
- Demo: local únicamente, 1 mercadillo, 1 categoría, 1 artículo y 10 tickets; no añadir contadores históricos.
- Demo permite ventas en cualquier estado, gastos y arqueo; no abonos, asignación de saldo, borrado del mercadillo, Utilidades, Perfil ni cambios de configuración.
- Aviso Demo fijo y acceso independiente a registro/login. Nunca transferir datos Demo a una cuenta, tampoco mediante Premium.
- Mantener datos al ascender gratuito a Premium, conservar sesión y guardar local antes de sincronizar. Restauración en instalación vacía exclusiva de Premium.
- No marcar como validadas las pruebas Android hasta que se hayan ejecutado. No tocar las ramas backup/.

## Correcciones y filosofía de negocio

- Antes de cambiar una regla de negocio: explicar comportamiento actual, ejemplo y propuesta; confirmar la intención con el usuario. No tratar todos los hallazgos de la auditoría como defectos demostrados.
- Regla F11 confirmada: en cuenta Premium, descontar las líneas PRODUCTO con artículo que controle stock, independientemente de la pestaña seleccionada. Los conceptos manuales no descuentan existencias. No ampliar el control de stock a gratuito o Demo.

- Regla F07 confirmada: una venta confirmada nunca se convierte en fallida por stock o red. Guardar la obligación de stock junto al ticket y aplicar descuento + confirmación en otra transacción, con reintentos persistentes y sin dobles descuentos. No recalcular ventas antiguas ni cambiar abonos.

## Lote 2.4 autorizado

- Las correcciones de esta ronda se entregan juntas como 2.4; no aumentar versión por cada hallazgo.
- Decisiones y comprobaciones actuales en docs/IMPLEMENTACION_2_4.md. No dar por validadas compilación, pruebas Android o reglas Firebase pendientes.
- Demo dispone de categoría protegida Sin categorizar adicional a su única categoría propia. No integrar WooCommerce ni leyenda de estados en este lote.
- Al comparar ediciones, conservar el lastModified del dispositivo de origen; movimientos de stock independientes se suman por identificador, no se descartan por fecha.
- Las versiones del esquema Room son independientes: esta candidata usa 20. Respaldo 2.3 conserva código, no datos del dispositivo; no hacer downgrade sobre Room 20.
