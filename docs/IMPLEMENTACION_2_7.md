# Market Sales 2.7 — entrega candidata

## Estado y alcance
- Base: 2.6 aceptada por el usuario; respaldo `backup/v2.6-validada` (9da155341cafc474d73e357669c4dd1c9964d1ee). Rama `feature/v2.7-recibos-pdf`; no integrar en master hasta aceptación.
- versionName 2.7, versionCode 11, Room 22. Migración aditiva 21→22: indicador `recibos.pdfOriginal`, tablas locales `receipt_documents` y `receipt_mail`. Mantener una copia de la base de 2.6 para volver a esa versión; no hacer downgrade sobre Room 22.
- Se ha escrito el código de los módulos, no se ha compilado ni ejecutado la app Android en este entorno. No presentar esta candidata como lista para producción.

## Implementación
- VentasRepository guarda la instantánea original Premium en la transacción de recibo/líneas/obligación de stock. La generación real de PDF y red son posteriores e independientes. Los abonos conservan referencia original. No se generan PDF retroactivos de Free/Demo/anteriores. El indicador de elegibilidad se sincroniza como un dato pequeño del recibo, no se añaden colecciones de archivos/secretos.
- ReceiptFiles usa Android PdfDocument A4, tabla, paginación, datos de empresa, logotipo opcional, moneda y método de pago originales, escritura atómica. Visor PdfRenderer interno. Los originales existentes no se sobrescriben. Si un original se pierde, se intenta recuperar la copia; si queda la instantánea local, se puede recrear el mismo contenido (no se garantiza identidad binaria del PDF recreado).
- ReceiptSettings conserva preferencia por cuenta/dispositivo, carpeta SAF y credenciales AES-GCM con Android Keystore. Los parámetros SMTP y la autorización se configuran en cada dispositivo; no se sincronizan secretos. Logotipo local por dispositivo, sin coste de alojamiento. Copias Android excluyen archivos PDF y preferencias de recibos/secretos; una identidad local no respaldada impide que una cola restaurada por Android se envíe desde otro móvil.
- ReceiptPdfWorker, ReceiptCopyWorker y ReceiptWorker separan generación, copia y correo. La cola guarda destinatario, cuenta autorizada, dispositivo y estado. Transiciones condicionales, detección de solicitudes activas, recuperación de SENDING como UNCERTAIN. Reintento explícito de incertidumbre. No confundir aceptación SMTP/Graph con entrega. Las solicitudes ya autorizadas pueden completarse tras pasar a Free; no se crean nuevas en Free. La ejecución depende de restricciones Android, red, sesión activa y políticas del proveedor.
- SMTP con TLS obligatorio y verificación de identidad, JavaMail Android 1.6.8. No se ha configurado ni enviado ningún correo real. Gmail con contraseña de aplicación cuando la cuenta la permite; no se implementa OAuth Google en esta candidata. Otros proveedores SMTP deben admitir TLS y autenticación compatible.
- WhatsApp normal y Business se abren mediante ACTION_SEND y PDF compartido de solo lectura; el usuario elige destinatario y confirma en WhatsApp. No se afirma entrega ni se automatiza WhatsApp. Si no hay conexión o no se puede abrir WhatsApp, se ofrece correo si está configurado.
- Copia a carpeta SAF voluntaria y estado de pendientes; exportación y recuperación manual mediante selector de archivos para proveedores sin selección persistente de carpetas. El acceso a Drive/Dropbox/OneDrive depende del proveedor instalado; no equivale a integraciones API directas. La copia aceptada por el proveedor local no demuestra subida a nube. Si una copia externa distinta tiene el mismo nombre no se sobrescribe; resolver en la carpeta elegida. La recuperación manual valida nombre ligado a cuenta/recibo, cabecera y estructura PDF; no es verificación criptográfica de autenticidad de una copia externa. El usuario controla esas copias.
- UI reutiliza Material3 y tema, texto es/en en nuevos componentes, acciones en Configuración, Utilidades y detalle de ventas. Algunos mensajes técnicos de cola se guardan en castellano. Aviso de datos de empresa en calendario al primer acceso Premium de ese dispositivo y Utilidades, no durante venta.
- No se contratan Firebase Storage, Cloud Functions ni servidor de correo. El suplemento de correo Market Sales sigue fuera de 2.7.

## Activación Outlook/Hotmail (requisito pendiente externo)
La implementación usa autorización de dispositivo OAuth2 pública y Microsoft Graph Mail.Send delegado, sin secreto de cliente. No se ha creado un registro Microsoft Entra. Para habilitar:
1. Registrar una aplicación en Microsoft Entra que admita cuentas personales Microsoft y, si se desea, organizativas.
2. Habilitar flujos de cliente público; configurar permisos delegados Microsoft Graph Mail.Send y User.Read; autorización offline_access solicitada en el código.
3. Copiar el Application (client) ID público a las propiedades Gradle de desarrollo: `MS_MAIL_CLIENT_ID=<UUID>`. No añadir client secret. Si no se configura, la UI informa de que Outlook está pendiente.
4. Compilar, autorizar desde la app y comprobar envío de prueba a la cuenta propia; probar renovación de token y revocación. Ver https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code y https://learn.microsoft.com/en-us/graph/api/user-sendmail?view=graph-rest-1.0.
Graph acepta 202 como aceptación de solicitud, no entrega final. Esta ruta directa limita adjuntos a 2.5 MB para evitar los requisitos de subida de adjuntos grandes; usar exportación/WhatsApp para mayores, o ampliar después. No se han comprobado políticas de tenants ni compatibilidad real de cuentas.

## Validación efectuada
- 16 comprobaciones SQLite ejecutadas sobre las sentencias de migración y consultas de cola copiadas del código: conservación de total histórico y pdfOriginal=false, separación de instantáneas y copia, aislamiento por cuenta/dispositivo, detección de solicitudes activas sin distinguir mayúsculas de email, compare-and-set, recuperación a UNCERTAIN, reintento explícito, SENT no reintentable. Resultado: 16/16.
- Tres pruebas instrumentadas añadidas en Receipt27Test: migración mínima SQLite, DAO y estados con Room, PDF multipágina inmutable. NO ejecutadas.
- Dependencias publicadas JavaMail Android 1.6.8 verificadas en Maven Central; resolución real Gradle pendiente. El entorno no dispone de Android SDK ni Gradle y no tiene acceso a Maven desde el contenedor. No se ha ejecutado assembleDebug, KSP, pruebas Android ni probado Firebase/correos/destinos SAF.

## Prueba de aceptación recomendada
1. Copia de los datos locales de 2.6, checkout de rama 2.7, Gradle sync y compilación debug. Verificar Room 21→22 con datos reales.
2. Free/Demo: venta habitual sin pregunta ni PDF; Premium: venta con y sin solicitud, abono, existencia de PDF, logotipo, cambios de datos sin modificar original, varias páginas, reinicio de app.
3. Gmail con contraseña de aplicación y otro SMTP propio: envío de prueba a uno mismo, correo de recibo en línea, cola sin conexión y reanudación, cancelación, credenciales incorrectas. No usar clientes reales para pruebas.
4. WhatsApp instalado y no instalado, conexión/no conexión; comprobar que vuelve a la venta sin duplicados.
5. Carpeta local SAF y proveedor de nube disponible; copia automática, exportación manual, recuperación desde otro dispositivo Premium y permisos revocados.
6. Cambio de cuenta, descenso Premium→Free, solicitudes pendientes autorizadas, reactivación y stock/ventas de 2.6. Outlook solo tras registro descrito arriba.
