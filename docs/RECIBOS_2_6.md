# Recibos — requisitos para la siguiente entrega

## Acuerdos confirmados

- Cada venta confirmada de Premium genera un recibo PDF y lo conserva, aunque el cliente no solicite una copia. Free y Demo conservan el registro de venta, no generan PDF.
- Al finalizar la venta Premium, si está activada la preferencia de solicitud, se pregunta si el cliente quiere recibir el recibo. No: conservar únicamente. Sí: conservar y enviar por el canal elegido, solicitando el destinatario.
- El recibo incluye los datos acordados: empresa y logotipo, número, fecha, conceptos, cantidades, precios, total y método de pago.
- Si falta conexión, conservar la solicitud de envío de forma persistente y realizar automáticamente el envío cuando vuelva la conectividad. El cierre de la aplicación no debe perder la solicitud.
- Una venta guardada no puede fallar ni repetirse por un error de generación o envío de recibos. Los reintentos conservan el identificador de recibo y deben evitar duplicados.
- Toda implementación se documentará dentro del código en castellano.

## Acuerdos adicionales confirmados — 27-11-2026

- El envío de recibos es exclusivo de Premium.
- En configuración habrá una opción «Solicitar siempre envío de recibo». Si está activada, preguntar al finalizar la transacción si el cliente quiere recibirlo; si está desactivada, no preguntar nunca. El recibo se conserva en ambos casos. Valor inicial desactivado; preferencia por dispositivo, confirmado.
- Si el cliente quiere recibir el recibo, ofrecer correo electrónico o WhatsApp. Si elige WhatsApp pero no puede enviarse en ese momento, pedir su correo para enviarlo después automáticamente por email. No automatizar WhatsApp ni contratar una API de WhatsApp Business.
- Para un correo solicitado sin conexión, guardar una solicitud persistente, asociada al recibo original, para enviarlo automáticamente cuando vuelva la red. El servicio autorizado de envío de email está por seleccionar.
- Confirmado: generar, consultar, enviar y reenviar PDF, datos de empresa y logotipo son funciones Premium. Free conserva el registro de venta y su consulta, sin PDF. Multiempresa Premium prevista para una fase final; no implementar ahora.
- Matriz Free/Premium cerrada y 2.6 aceptada por el usuario. Recibos se desarrollarán en 2.7.

## Preparación 2.7

La pantalla PantallaEnviarRecibo.kt existe, pero no implementa el envío real. Se debe guardar la venta antes de ofrecer envío; PDF y solicitudes de envío persistentes con independencia del resultado de la red. Preferencia por dispositivo, inicialmente desactivada, confirmada. Propuesta de envío automático: servicio de servidor autenticado, cola persistente y clave de idempotencia; credenciales de correo solo en el servidor, no en la aplicación. Confirmado: cada empresa Premium configura su propia cuenta de correo de envío (remitente propio). Como alternativa opcional de pago, Market Sales podrá proporcionar una dirección de envío a empresas que no dispongan de correo propio; suplemento, dominio, condiciones y límites pendientes de definir. No incluir ese servicio adicional en Premium por defecto ni activarlo en 2.7 sin contratarlo. Proveedor y mecanismo técnico de autenticación/envío pendientes; no enviar desde direcciones no autorizadas ni guardar credenciales en texto plano en el cliente. No se ha modificado aún código funcional.

## Configuración del envío — confirmada

Opción «Configuración de correo» en Utilidades, exclusiva de Premium. Datos de envío asociados a la empresa/cuenta, configurados una sola vez para todos los dispositivos, con prueba de conexión/envío a una dirección propia antes de habilitar el canal. Para SMTP: nombre del remitente, dirección, servidor, puerto, cifrado, usuario y contraseña de aplicación cuando proceda; para proveedores que lo requieran, autorización OAuth. Credenciales protegidas en servidor, sin mostrarlas en otros dispositivos; el usuario puede sustituirlas. Si no configura correo, generación, consulta y uso compartido del PDF por WhatsApp siguen disponibles, pero el envío automático por email no se ofrece como operativo.

## Preferencia de solicitud de envío — confirmada

«Solicitar siempre envío de recibo» se guarda por dispositivo, desactivada inicialmente; disponible para modificación solo en Premium. Activada: al finalizar una venta, preguntar si el cliente quiere recibir el recibo; desactivada: no preguntar, pero generar y conservar el PDF igualmente en Premium. La consulta y el envío voluntario posterior siguen disponibles para Premium.

## Conservación del PDF y acceso desde varios dispositivos — confirmado

Generar y conservar cada PDF de forma duradera en el dispositivo donde se confirma la venta, incluso sin conexión; sincronizar una copia privada con Firebase cuando haya conexión, para consultar y reenviar desde otro dispositivo Premium de la misma cuenta. Mantener el PDF original inalterado aunque cambien posteriormente el precio de los artículos, los datos de empresa o el logotipo. Usar el identificador de recibo existente para vincular archivo y venta, sin crear otro ticket por reenvío. Si falla la generación, conservar los datos originales necesarios para reintentar sin repetir la venta. No eliminar los PDF por caducar Premium; bloquear acceso hasta reactivación, respetando las reglas de recuperación ya acordadas.

## Datos de empresa incompletos — confirmado

No bloquear nunca una venta por falta de datos de empresa o logotipo. El logotipo es opcional; si falta, generar el PDF sin él. Si faltan datos de empresa, generar el recibo de venta con los datos disponibles, sin inventarlos ni presentarlo como factura fiscal. Al activar Premium, informar al usuario de que debe completar los datos de empresa para enviar recibos personalizados y ofrecer acceso a Datos de empresa. Si siguen incompletos, mantener un aviso no intrusivo en Utilidades/Datos de empresa. Nunca mostrar un aviso de datos de empresa incompletos al finalizar una venta. Los cambios de datos solo afectan a nuevos recibos; mantener inalterados los PDF ya generados.

## Formato de los recibos PDF — confirmado

Diseño estándar, limpio y profesional, A4 con paginación cuando haya muchos conceptos. Logotipo opcional, datos de empresa disponibles, identificador del recibo, fecha y hora, mercadillo, conceptos, cantidades, precios unitarios, importes, total, moneda y método de pago. Pie de agradecimiento e identificación de Market Sales. Los recibos de abono/devolución llevan encabezado RECIBO DE ABONO, importes negativos y referencia a la venta original. Sin editor de plantillas ni personalización de colores o tipografías en 2.7. No presentar el recibo como factura fiscal.

## Infraestructura de envío — confirmada

Market Sales no debe asumir costes por los correos que envían los usuarios desde sus propias cuentas. No contratar ni activar Cloud Functions ni un servicio de correo de pago para este fin sin aprobación.

Envío directo desde Android mediante SMTP con TLS y autenticación autorizada por el proveedor (contraseña de aplicación u OAuth cuando proceda). WorkManager y una cola persistente local permitirán reintentar al recuperar conectividad, sujeto a las restricciones de Android. No depender de abrir Gmail ni de un servidor intermediario de Market Sales. Mantener las credenciales cifradas con Android Keystore y separadas por cuenta/dispositivo; nunca enviarlas a Firebase en texto plano ni sincronizar secretos sin un mecanismo seguro. Este acuerdo sustituye el anterior de credenciales centralizadas: se pueden compartir los parámetros no secretos, pero cada dispositivo autoriza su acceso al correo. Si el proveedor no admite el envío directo, no prometer compatibilidad. Un resultado SMTP incierto no se reintenta a ciegas; el envío confirmado por el servidor no garantiza entrega en bandeja de entrada. El dispositivo debe estar operativo para enviar.

## Almacenamiento PDF — nueva restricción económica confirmada

El usuario no desea asumir costes de alojamiento de documentos de sus clientes. No subir PDF ni logotipos a un servicio de almacenamiento facturado a Market Sales sin aprobación expresa. Se mantiene la generación y conservación duradera local de los PDF originales. Se revisa el acuerdo anterior de sincronizar los archivos PDF en Firebase: Firebase sigue sincronizando los datos de negocio según las reglas existentes, pero no se presupone el almacenamiento de los archivos PDF. 

### Propuesta pendiente de confirmación

Guardar por defecto los PDF en almacenamiento privado local separado por cuenta. Ofrecer al usuario Premium una opción voluntaria de copia/exportación a una carpeta elegida por él, incluyendo proveedores de nube que estén disponibles mediante el selector de archivos de Android. La cuota y cualquier coste de la nube corresponden al usuario. Mantener el original local; no prometer sincronización automática ni acceso desde otros dispositivos hasta definir la integración correspondiente. Proponer copia automática a la carpeta elegida cuando sea técnicamente viable, con reintento persistente, sin depender de un servidor intermediario de Market Sales. Advertir en configuración (no durante la venta) del riesgo de pérdida de PDF al desinstalar o perder el dispositivo si no existe copia externa. Respetar el acceso Premium dentro de la app; las copias exportadas por el usuario quedan bajo su control. No se ha modificado código funcional.
