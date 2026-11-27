# F07 — Venta finalizada y stock pendiente (2.3 candidata)

## Regla confirmada

Una venta guardada es definitiva. Si después falla el descuento de stock, la app conserva la venta y la obligación de descontarlo; reintenta sin pedir repetir la venta ni mostrarla como fallida. Solo Premium y líneas PRODUCTO con control de stock. No cambia abonos, ventas manuales ni límites de modalidades.

## Implementación

1. Una transacción Room guarda recibo, líneas y movimientos pendientes identificados por recibo + línea. Si no se puede guardar la venta localmente, no se confirma.
2. Otra transacción por movimiento descuenta stock y marca el movimiento aplicado. Si falla cualquiera de ambos pasos, se revierte solo ese movimiento; el ticket sigue guardado.
3. Los movimientos aplicados se conservan para que repetir el trabajo no descuente dos veces. El procesador relee cada movimiento dentro de la transacción y filtra artículo por propietario.
4. Intenta aplicar inmediatamente en segundo plano. WorkManager reintenta sin requerir red; se reactiva al abrir la app y al autenticar, y dispone de tarea periódica. Android determina cuándo permite ejecutar esas tareas: no se promete un plazo exacto si la app está detenida o el sistema restringe su ejecución.
5. El artículo modificado queda pendiente de subir y una tarea con conexión reintenta su subida. Las subidas de artículos se serializan en el proceso y se reconoce solo el estado enviado: un descuento posterior sigue pendiente.

Ejemplo: stock 10, venta de 2 productos más una línea manual. Se guarda ticket + descuento pendiente de 2. Si falla la aplicación, sigue habiendo ticket y obligación. Al recuperarse queda stock 8. Reiniciar o repetir el procesador mantiene 8.

Si falta un artículo, su movimiento permanece pendiente, no se descuenta de otro propietario y no bloquea otros productos. No se crea otro ticket. El procesador no depende del Premium actual: termina las obligaciones adquiridas cuando se confirmó la venta.

## Datos y recuperación

Migración aditiva Room 18 → 19: añade stock_movements e índice, sin borrar tablas ni generar movimientos de ventas anteriores. No se recalculan ventas históricas porque podrían haberse descontado ya.

Respaldo del código estable: backup/v2.2-validada. Una copia del código no es una copia de la base de datos del teléfono. Probar primero en un emulador/copia con datos de prueba. Después de abrir Room 19, no instalar 2.2 sobre esa misma base: no hay migración inversa y se ha eliminado el borrado automático al bajar versión.

Si el móvil se destruye antes de enviar los datos locales al servidor, esos datos no pueden recuperarse. La cola es local, no un registro distribuido de movimientos en Firebase. Los conflictos entre dispositivos y la estrategia general de descarga/sincronización siguen pendientes de revisión independiente; este cambio no los declara resueltos.

## Verificación

- Ejecutado: scripts/check_stock_sql.py, cinco pruebas SQLite con SQL real del DAO/migración: fallo entre descuento y confirmación, reapertura, reintentos concurrentes, aislamiento por propietario, confirmación de subida antigua y migración aditiva.
- Ejecutado: scripts/check_translations.py y git diff --check.
- Añadido: StockMovementTest, pruebas instrumentadas Room de venta persistida ante fallo del planificador, fallo de confirmación, reapertura, concurrencia, duplicados, reglas Premium/control de stock y migración del conjunto de tablas 18.
- Pendiente: compilar y ejecutar pruebas Android. Este entorno no pudo descargar Gradle (Network is unreachable). Las pruebas SQLite no validan la integración Room/WorkManager/Firebase ni sustituyen las instrumentadas.

## Comprobación en Android Studio

Con emulador y datos de prueba, actualizar desde 2.2 a esta rama, sin desinstalar. Confirmar versión 2.3 y que conserva artículos y tickets. Ejecutar StockMovementTest desde androidTest. Después:

- Premium, artículo stock 10 con control activado: vender 2 terminando desde Manual; comprobar ticket y stock 8.
- Sin conexión, vender otras 2; comprobar ticket y stock 6, cerrar/reabrir y comprobar que sigue 6.
- Recuperar conexión y comprobar el artículo en Firebase; repetir apertura y verificar que no vuelve a descontar.
- Comprobar que las líneas manuales y los artículos sin control no alteran existencias.

F07 permanece pendiente de validación Android. No modificar otras reglas de negocio durante esta prueba.
