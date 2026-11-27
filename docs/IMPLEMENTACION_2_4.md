# Market Sales 2.4 — candidata conjunta para compilar y probar

Fecha: 27-11-2026. Rama: `fix/v2.4-correcciones-auditoria`.
Base: candidata 2.3 `3bf79db995e14f34bf3f282feba33ef3d4e0e2ce`.
Respaldo de código conservado: `backup/v2.3-antes-2.4`. No se ha modificado master.

Esta entrega reúne los cambios de los apartados acordados; **no equivale a una versión validada en Android**. No marcar el checklist como solucionado hasta pasar las pruebas funcionales. No introduce la integración con WooCommerce ni la leyenda de colores pendiente.

## Cambios de código

| Hallazgo | Cambio |
| --- | --- |
| F02 | Gastos identificados por mercadillo y línea; identificador único de línea entre dispositivos. Confirmación por dueño, clave y snapshot original. |
| F04 | Empresa con propietario y clave por cuenta. Flujo reactivo al cambiar de sesión. Se retira el documento global de empresa. |
| F05 | Room 19 → 20 aditivo para stock, fechas y saldos; reconstrucción de empresa por cuenta. Se conserva una tabla privada `empresa_legacy` como respaldo local, fuera de la sincronización. Si no hay dueño identificable en la configuración anterior, no se atribuyen esos datos a otra cuenta. |
| F06 | Un motor común lee pendientes persistidos en Room y compara revisiones mediante transacciones de Firebase. Conserva fecha original; empate estable. No utiliza conteos como detector de modificaciones. Trabajo persistente con red y reintentos, además del primer plano. |
| F08 | Firma release desde `keystore.properties`, excluido de Git. Usar la misma clave anterior. |
| F09 | Bajas lógicas sincronizadas. Categoría automática protegida por propietario y fuera del límite Demo. Confirmar borrado de categoría mueve sus artículos a Sin categorizar, sin borrar historial. |
| F10 | Varios saldos simultáneos. Alta no sustituye anteriores. Asignación por identificador y destino; alta de mercadillo y consumo del saldo en una transacción. Lista con selección de saldo, asignación y borrado confirmado. No se añade arbitraje excepcional de doble asignación offline. |
| F13 | Fecha de calendario comparable en `fechaEpochDay` (día numérico), conservando `fecha` dd-MM-yyyy por compatibilidad de interfaz. Instantes existentes siguen en milisegundos. Consultas por fecha ordenadas cronológicamente; importación normaliza la representación visible. |
| F14 | Abono pide efectivo/Bizum/tarjeta. Nuevo recibo negativo con su método real, ligado a la línea original. Ticket, línea y obligación de stock se guardan juntos. Confirmación duplicada local rechazada. No añade abonos parciales. |
| F15 | Permiso persistente de restauración por cuenta; los datos remotos reservados no se mezclan con una instalación vacía gratuita. Primera oportunidad y segunda antes del primer dato nuevo, seguidas de confirmación destructiva adicional. Borrado con generación de cuenta, persistencia y reintento. Copias antiguas de esta versión no pueden resubir una generación retirada. No borra autenticación. |
| F16 | BuildConfig de Market Sales para distinguir anuncios de prueba y producción. |
| F17 | Mínimo candidato API 26, recálculo al abrir/reanudar y cada minuto visible, tareas diarias y subida separada. Conserva ventanas 00:00/05:00 Madrid y reglas acordadas. |
| F18 | Guardado local fallido no anuncia éxito; una venta ya confirmada permanece finalizada. La subida queda separada y pendiente de reintentar. Se retiran guardados directos de abonos que perdían relación con caja. |
| F19 | Consultas sensibles con propietario, flujos de cuenta, empresa por UID, comprobación de dueño antes de fusionar datos y antes de escribir en la nube. Un callback de conectividad por proceso. Preferencias visuales solo en el dispositivo. |
| F20 | Operaciones monetarias decimales, límites de entrada a dos decimales y normalización al persistir. Sumatorios SQL en céntimos. Conserva formato de MonedaUtils. |

### Stock entre dispositivos

El stock no se resuelve eligiendo el último número recibido: se conserva una base de inventario y se incorporan los movimientos únicos de todas las ventas/abonos pertinentes. Los movimientos anteriores de 2.3 se consideran ya incorporados al punto inicial de la migración; no se vuelven a descontar ni se reconstruyen ventas históricas. Las ediciones explícitas de inventario actualizan la base. Un descuento pendiente no cambia la fecha de edición del catálogo. El procesamiento local conserva descuento y confirmación en una sola transacción.

### Restauración Premium y límites de esta candidata

La activación comercial/compra de Premium no estaba desarrollada en el proyecto. No se inventa una compra ni se concede Premium por pulsar recuperar: el diálogo informa de ello y conserva la reserva. El motor consulta el estado Premium real de `usuarios/{uid}` y restaura cuando esa cuenta ya está activada. El método previo de activación de desarrollo se conserva; **la contratación comercial sigue pendiente como función**, no se anuncia como implementada.

El control de generaciones requiere permisos de Firebase para `account_data_state/{uid}` y `stock_movements` del propietario, además de las colecciones existentes. No se han leído ni desplegado reglas del proyecto Firebase desde este entorno. La prueba de dos dispositivos debe verificar permisos y el bloqueo de resubidas anteriores; compilar no comprueba las reglas del servidor. Si se pretende admitir clientes antiguos que ignoren generaciones, hay que impedir sus escrituras mediante reglas de servidor antes de distribuir esta versión. No se ha modificado la configuración Firebase ni borrado datos reales.

## Comprobaciones ejecutadas

- `python3 scripts/check_release24_sql.py`: 183 consultas reales de DAO preparadas sobre las columnas de las entidades; orden cronológico, propietarios, confirmación de snapshot de gastos, stock aditivo, múltiples saldos, SQL real de migración y guardas monetarias: pasan.
- `python3 scripts/check_stock_sql.py`: cinco pruebas SQLite de fallo/recuperación, concurrencia, dueño, snapshot y conservación de datos: pasan. La expectativa de versión de catálogo se adapta a que un movimiento de stock ya no modifica su fecha de edición.
- Pruebas Kotlin añadidas: fechas y precisión monetaria (`Release24PolicyTest`); pendientes de ejecutar con Gradle.
- Se mantienen las pruebas instrumentadas anteriores, ajustando dueño explícito y migraciones donde corresponde. Pendientes en dispositivo/emulador.
- Intento `bash gradlew :app:compileDebugKotlin --console=plain`: bloqueado al descargar Gradle (`Network is unreachable`). No se ha comprobado compilación, manifest merge, firma release ni comportamiento real Android/Firebase.

## Cómo probar

1. En Android Studio, Fetch y Checkout de `fix/v2.4-correcciones-auditoria`; actualizar esa rama. Verificar `versionName = 2.4`, `versionCode = 8`.
2. Compilar primero debug. Ejecutar `testDebugUnitTest` y las pruebas instrumentadas. Para release, copiar `keystore.properties.example` a `keystore.properties` y completar las cuatro propiedades privadas usando la clave existente.
3. Antes de instalar sobre la base real: conservar una copia de sus datos Room. La rama backup es una copia del código, no del contenido del móvil. No volver a Room 19 sobre una base ya migrada a 20.
4. Probar Demo offline: categoría protegida más una propia, 1 artículo, 1 mercadillo y 10 ventas; excepción de ventas por estado y restricciones existentes.
5. Probar dos cuentas: empresa separada, listados, borradores y pendientes sin cruzar datos. Preferencias visuales por dispositivo.
6. Sin red, crear ventas/gastos y editar catálogo; cerrar proceso y volver a abrir. Recuperar conexión y comprobar que se sube sin pedir repetir los registros. Dos dispositivos: edición más reciente y ventas de 2 + 3 desde stock 10 → 5.
7. Borrar artículos y categorías offline: confirmar traslado, mantener tickets y comprobar que no reaparecen al reconectar.
8. Crear dos saldos, asignar uno y comprobar que queda el otro; borrar pendiente con cancelar/confirmar. Fallar el alta de mercadillo y comprobar que no consume saldo.
9. Cobrar por Bizum y devolver por efectivo: conservar cobro y abono por métodos diferentes; comprobar caja, totales y stock una sola vez.
10. Instalación vacía gratuita con datos remotos: rechazar primera oportunidad conserva reserva; primer dato vuelve a preguntar; cancelar no borra; confirmar deja tarea persistente. Activar Premium antes de confirmar conserva y recupera. Verificar interrupción/reintento del borrado y generación en otro dispositivo.
11. Fechas entre meses y años, importes con coma/punto y dos decimales; cruzar medianoche y las 05:00 con app abierta, cerrada y reanudada.
12. Anuncios debug/release y compilación con firma habitual; comprobar de nuevo F01/F03/F07/F11/F12.

Estado final de este documento: **cambios preparados para revisión/compilación; validación Android y Firebase pendiente**.

## Corrección de compilación — 27-11-2026

El registro de Android Studio alcanza KSP y detecta `Unused parameter: now` en StockMovementDao.applyStock. Se retira el argumento obsoleto del DAO y de StockMovementProcessor. No se cambia la lógica de negocio ni la versión 2.4. Se amplía la comprobación de SQL para contrastar todos los parámetros de las consultas Room con los argumentos de sus métodos; pasa. Las pruebas SQLite de stock siguen pasando. Pendiente repetir assembleDebug en Android Studio.

### Segundo registro de compilación

Room/KSP supera el error anterior. El compilador Kotlin detecta una salida `return@launch` desde el bloque no inline de la transacción en AsignarSaldoViewModel. Se sustituye por validación del destino dentro de la transacción: si falta, se revierte y se muestra el error local, sin anunciar guardado correcto. Se conserva la cancelación de la corrutina. Revisadas las salidas etiquetadas de los bloques modificados. Compilación completa pendiente de repetir en Android Studio.
