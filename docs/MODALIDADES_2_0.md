# Market Sales 2.0 — Demo, gratuito y Premium

Estado: implementación candidata. Pendiente de compilación y pruebas Android. Base: d4a5497b93d6672dc2068acdb8f28741eea804ec (versión 1.2).

## Requisitos confirmados

- Tres modalidades: Demo sin registro, gratuito registrado y Premium.
- Demo local desde el primer uso, sin necesidad de conexión ni llamadas de sincronización de datos de negocio.
- Etiqueta «Versión Demo» fija sobre todas las pantallas, fuera del contenido desplazable, con accesos independientes a crear cuenta e iniciar sesión.
- Máximo de un mercadillo, una categoría, un artículo y diez tickets confirmados. Cada ticket puede tener varias líneas. No se añaden contadores históricos.
- Ventas en cualquier estado del mercadillo solo en Demo. Gastos y arqueo disponibles. No se permite asignar saldo ni hacer abonos.
- No se permite borrar/desactivar el mercadillo Demo. Utilidades y Perfil inaccesibles; configuración consultable sin modificaciones, incluido el tema.
- Al alcanzar un límite se explica y se ofrece crear una cuenta gratuita.
- Nueva cuenta por correo/contraseña o Google: empieza sin datos de Demo. Se avisa antes; no existe transferencia de Demo, tampoco pagando Premium.
- Una cuenta existente conserva sus propios datos. Gratuito a Premium mantiene usuario y datos.
- Cierre explícito de sesión: vuelve a Demo con sus datos y límites previos. No se añaden caducidades ni cierres por falta de conexión. Las sesiones se conservan con Firebase Auth; una revocación externa puede exigir reautenticación.
- Restauración de datos de negocio desde nube en una instalación vacía: exclusiva de Premium. Gratuito puede usar datos locales y subir pendientes.
- Los datos del antiguo usuario invitado no se migran ni se mezclan con la nueva Demo. Se mantienen intactos en disco; la Demo usa otro identificador local.

## Implementación

- AppModePolicy define las tres modalidades y límites; DemoAccess aplica identidad, permisos de sincronización y avisos.
- La Demo utiliza demo_local_v2 en las tablas existentes. El aislamiento es por propietario dentro de Room, no una nueva base de datos. No cambia el esquema: Room sigue en 18.
- Se recrea la navegación al cambiar propietario. Los borradores de gastos compartidos por actividad utilizan una clave por propietario.
- Se comprueban los límites dentro de transacciones Room. El límite de ventas cuenta recibos, no líneas. Cabecera y líneas se guardan juntas y se devuelve el resultado antes de esperar a la red.
- Se sustituyen identificadores de recibos basados en hora por UUID para evitar sustituciones que también permitirían eludir el recuento.
- Se bloquean las dos vías de abono y las rutas de asignación de saldo en Demo. Las cuentas mantienen sus reglas anteriores.
- Las subidas y descargas de categorías, artículos, mercadillos, ventas, gastos y configuración no admiten el propietario Demo. Se retira el bootstrap remoto del invitado.
- Se conservan restricciones existentes: gratuito un mercadillo por fecha; Premium permite varios. Idioma/fuente/moneda avanzados y control de stock/coste conservan sus restricciones previas. Los anuncios mantienen sus condiciones existentes.

## Verificación realizada en el entorno de desarrollo

- Comprobador de traducciones: 610 claves por idioma; 667 llamadas literales comprobadas.
- git diff --check.
- Experimento SQLite con las consultas reales de recuento: separa invitado antiguo, Demo y cuenta; diez recibos permanecen contados tras cerrar y reabrir la base.
- Comprobación léxica de delimitadores en archivos Kotlin modificados. No equivale a compilar.
- Añadidos seis tests JUnit de política y dos tests instrumentados Room: límites concurrentes, diez tickets con veinte líneas, reapertura de repositorio, aislamiento y límites de catálogo/mercadillo.
- No se han ejecutado tests Kotlin ni Android: el wrapper no puede descargar Gradle 9.5.0 (Network is unreachable). No hay APK compilado por este entorno.

## Aceptación en Android Studio

1. En un emulador sin datos, activar modo avión ANTES del primer arranque. La Demo debe abrir sin pedir cuenta ni descargar datos.
2. Crear un mercadillo, una categoría y un artículo. Intentar el segundo de cada tipo: aviso y enlace a registro. No guardar el segundo.
3. Confirmar diez ventas, con varias líneas por ticket. Intentar la undécima y pulsar rápidamente confirmar: deben existir exactamente diez recibos y todas sus líneas.
4. Cerrar por completo y reabrir. Los datos y límites Demo deben seguir presentes.
5. Probar ventas con mercadillo futuro, cancelado y cerrado. La excepción no debe aplicarse a cuentas registradas.
6. Registrar gastos y completar el arqueo. No poder asignar saldo ni borrar el mercadillo.
7. Intentar abono desde resumen: aviso de función exclusiva para registrados, sin crear líneas negativas.
8. Comprobar aviso Demo fijo en calendario, ventas, gastos, configuración y acceso/registro; desplazar contenido y abrir teclado.
9. Utilidades y Perfil no accesibles en Demo. Ningún ajuste de configuración modificable, tampoco tema.
10. Con conexión, crear una cuenta nueva por correo y probar otra por Google. Aviso previo; cuenta sin datos Demo. Nunca mezclar con datos de una cuenta ya existente.
11. Cerrar y reabrir la app autenticada, también en modo avión: sesión y datos locales conservados. Cerrar sesión expresamente: Demo anterior intacta; volver a entrar: datos de la cuenta.
12. Cuenta gratuita vacía con datos remotos: sin restauración automática. Cuenta Premium: restauración permitida. Ascenso gratuito a Premium: mismo UID y datos existentes.
13. En Firebase, comprobar que no se crean documentos cuyo propietario sea demo_local_v2. Las reglas de servidor no se han modificado ni verificado aquí.
14. Comprobar regresiones de cancelados, configuración gratuita/Premium, varios mercadillos por fecha en Premium y venta normal con/sin conexión.

## Respaldo y límites de esta entrega

- backup/2026-11-25-v1.2-antes-demo: d4a5497b93d6672dc2068acdb8f28741eea804ec.
- backup/v1.1-cancelados-validada: 7e1d66fb186d902d66dca1521bd753993ed91e52.
- No se eliminan datos de cuentas ni se migra el esquema Room. Las ramas de respaldo no contienen la base de datos del teléfono, las reglas de Firebase ni la clave de firma privada local.
- La lista de 20 hallazgos de la revisión 1.2 sigue siendo el registro de trabajo. Esta entrega no certifica resueltos todos los problemas de sincronización, migraciones, stock o abonos de cuentas registradas. La transacción de venta no incluye todavía el ajuste de stock Premium (F07/F11 siguen requiriendo trabajo).
- La contratación Premium real y la ruta de suscripción incompleta ya existían como trabajo pendiente; no se implementa facturación en esta entrega.
