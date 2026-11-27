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
- En configuración habrá una opción «Solicitar siempre envío de recibo». Si está activada, preguntar al finalizar la transacción si el cliente quiere recibirlo; si está desactivada, no preguntar nunca. El recibo se conserva en ambos casos. El valor inicial y el alcance cuenta/dispositivo se concretarán antes de implementar.
- Si el cliente quiere recibir el recibo, ofrecer correo electrónico o WhatsApp. Si elige WhatsApp pero no puede enviarse en ese momento, pedir su correo para enviarlo después automáticamente por email. No automatizar WhatsApp ni contratar una API de WhatsApp Business.
- Para un correo solicitado sin conexión, guardar una solicitud persistente, asociada al recibo original, para enviarlo automáticamente cuando vuelva la red. El servicio autorizado de envío de email está por seleccionar.
- Confirmado: generar, consultar, enviar y reenviar PDF, datos de empresa y logotipo son funciones Premium. Free conserva el registro de venta y su consulta, sin PDF. Multiempresa Premium prevista para una fase final; no implementar ahora.
- Matriz Free/Premium cerrada y 2.6 aceptada por el usuario. Recibos se desarrollarán en 2.7.

## Preparación 2.7

La pantalla PantallaEnviarRecibo.kt existe, pero no implementa el envío real. Se debe guardar la venta antes de ofrecer envío; PDF y solicitudes de envío persistentes con independencia del resultado de la red. Preferencia por dispositivo, inicialmente desactivada, propuesta pendiente de aprobación. Propuesta de envío automático: servicio de servidor autenticado, cola persistente y clave de idempotencia; credenciales de correo solo en el servidor, no en la aplicación. Confirmado: cada empresa Premium configura su propia cuenta de correo de envío (remitente propio). Como alternativa opcional de pago, Market Sales podrá proporcionar una dirección de envío a empresas que no dispongan de correo propio; suplemento, dominio, condiciones y límites pendientes de definir. No incluir ese servicio adicional en Premium por defecto ni activarlo en 2.7 sin contratarlo. Proveedor y mecanismo técnico de autenticación/envío pendientes; no enviar desde direcciones no autorizadas ni guardar credenciales en texto plano en el cliente. No se ha modificado aún código funcional.

## Siguiente cuestión: configuración del envío

Propuesta: datos de envío asociados a la empresa/cuenta, configurados una sola vez, con prueba de conexión/envío a una dirección propia antes de habilitar el canal. Para SMTP, servidor, puerto, cifrado, usuario y contraseña de aplicación cuando proceda; para proveedores que lo requieran, autorización OAuth. Credenciales protegidas en servidor, sin mostrarlas en otros dispositivos; el usuario puede sustituirlas. Si no configura correo, WhatsApp sigue disponible, pero el envío automático por email no se ofrece como operativo. Pendiente de confirmación.
