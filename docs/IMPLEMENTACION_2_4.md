# Implementación 2.4 — en desarrollo

Base: candidata 2.3, commit `3bf79db995e14f34bf3f282feba33ef3d4e0e2ce`. Respaldo verificado `backup/v2.3-antes-2.4`. Rama `fix/v2.4-correcciones-auditoria`. No modificar master ni respaldos. No incrementar la versión hasta completar el lote.

## Primer bloque (F08, F16)
- F08: la firma release lee `keystore.properties` local, excluido de Git. Copiar `keystore.properties.example` a `keystore.properties` y rellenar con la ruta y credenciales de la misma clave anterior. No generar otra clave ni publicar ese fichero. La compilación debug no requiere estos datos. Para release configurar los cuatro valores y verificar firma; no distribuir una compilación sin firmar. No se modifica el historial.
- F16: AdsConsentManager y AdsInterstitialController usan BuildConfig de Market Sales. Los recursos debug/release de anuncios ya estaban separados y se conservan. No se cambian las condiciones de modalidades.

Comprobación estática de cambios realizada; compilación Gradle y pruebas Android pendientes. Verificar debug con IDs de prueba, release con IDs de producción sin generar impresiones de prueba reales, y firma con la clave existente. No instalar sobre los datos reales sin respaldo de Room.

## Segundo bloque (F02)
- Las dos rutas activas de subida de gastos usan GastoSync: clave mercadillo-línea, todos los campos y confirmación por propietario, clave, versión y fecha original. Room asigna el número de línea dentro de la transacción de alta.
- La descarga reconoce identificadores antiguos idLineaGasto, descarta claves vacías y agrupa duplicados de la misma clave. Si existe el documento canónico se prefiere frente a la copia antigua incompleta; no se borran documentos remotos antiguos. Los campos que nunca se subieron no se pueden recuperar de esa copia. La reconciliación de datos locales/remotos y reintentos persistentes generales siguen pendientes de F06/F18.
- Pendiente compilar y ejecutar prueba de dos mercadillos con línea 0001, reintento sin conexión y descarga de documentos antiguos.

## Tercer bloque (F17)
- minSdk 26 como candidato, pendiente confirmar mediante Gradle/manifest merge de todas las dependencias.
- Motor local compartido de estados automáticos, transacción Room, 00:00 y 05:00 Europe/Madrid. Conserva estados 4–7 y no repite cambios ya realizados. La reanudación y el cambio de cuenta en primer plano recalculan de inmediato; comprobación cada minuto mientras la actividad está visible. WorkManager diario sin permisos extra, sin garantía de puntualidad exacta.
- Subida separada con red y reintento; pendiente integrar con la sincronización general F06/F18 y validar en Android el cambio de día, las 05:00, reinicio y ausencia de conexión.

## Pendientes
F04, F05, F06, F09, F10, F13, F14, F15, F18, F19 y F20; regresión F01/F03/F07/F11/F12. Mantener las decisiones del documento de continuidad 2.4. F08/F16 quedan pendientes de validación real.
