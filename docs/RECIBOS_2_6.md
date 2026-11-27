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

## Siguiente cuestión: conservación del PDF y acceso desde varios dispositivos

Propuesta pendiente: generar y conservar cada PDF de forma duradera en el dispositivo donde se confirma la venta; sincronizar una copia privada con la cuenta cuando haya conexión, para consultar y reenviar desde otro dispositivo Premium. Mantener el PDF original inalterado aunque cambien posteriormente el precio de los artículos, los datos de empresa o el logotipo. Usar el identificador de recibo existente para vincular archivo y venta, sin crear otro ticket por reenvío. Si falla la generación, conservar los datos originales necesarios para reintentar sin repetir la venta. No eliminar los PDF por caducar Premium; bloquear acceso hasta reactivación, respetando las reglas de recuperación ya acordadas. No se ha modificado código funcional.
