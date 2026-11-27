# Recibos — requisitos para la siguiente entrega

## Acuerdos confirmados

- Cada venta confirmada genera un recibo PDF y lo conserva, aunque el cliente no solicite una copia.
- Al finalizar la venta se pregunta si el cliente quiere recibir el recibo. No: conservar únicamente. Sí: conservar y enviar por el canal elegido, solicitando el destinatario.
- El recibo incluye los datos acordados: empresa y logotipo, número, fecha, conceptos, cantidades, precios, total y método de pago.
- Si falta conexión, conservar la solicitud de envío de forma persistente y realizar automáticamente el envío cuando vuelva la conectividad. El cierre de la aplicación no debe perder la solicitud.
- Una venta guardada no puede fallar ni repetirse por un error de generación o envío de recibos. Los reintentos conservan el identificador de recibo y deben evitar duplicados.
- Toda implementación se documentará dentro del código en castellano.

## Acuerdos adicionales confirmados — 27-11-2026

- El envío de recibos es exclusivo de Premium.
- En configuración habrá una opción «Solicitar siempre envío de recibo». Si está activada, preguntar al finalizar la transacción si el cliente quiere recibirlo; si está desactivada, no preguntar nunca. El recibo se conserva en ambos casos. El valor inicial y el alcance cuenta/dispositivo se concretarán antes de implementar.
- Si el cliente quiere recibir el recibo, ofrecer correo electrónico o WhatsApp. Si elige WhatsApp pero no puede enviarse en ese momento, pedir su correo para enviarlo después automáticamente por email. No automatizar WhatsApp ni contratar una API de WhatsApp Business.
- Para un correo solicitado sin conexión, guardar una solicitud persistente, asociada al recibo original, para enviarlo automáticamente cuando vuelva la red. El servicio autorizado de envío de email está por seleccionar.
- La generación/consulta del PDF en Free y Demo, separada del envío Premium, se revisará en la definición de modalidades. No confundir la conservación del registro de venta (común) con el derecho a generar/compartir un archivo PDF.
- Se pausa la implementación de recibos hasta cerrar la matriz Free/Premium de funciones actuales y futuras. No modificar código funcional en esta fase.
