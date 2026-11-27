# Recibos — requisitos para la siguiente entrega

## Acuerdos confirmados

- Cada venta confirmada genera un recibo PDF y lo conserva, aunque el cliente no solicite una copia.
- Al finalizar la venta se pregunta si el cliente quiere recibir el recibo. No: conservar únicamente. Sí: conservar y enviar por el canal elegido, solicitando el destinatario.
- El recibo incluye los datos acordados: empresa y logotipo, número, fecha, conceptos, cantidades, precios, total y método de pago.
- Si falta conexión, conservar la solicitud de envío de forma persistente y realizar automáticamente el envío cuando vuelva la conectividad. El cierre de la aplicación no debe perder la solicitud.
- Una venta guardada no puede fallar ni repetirse por un error de generación o envío de recibos. Los reintentos conservan el identificador de recibo y deben evitar duplicados.
- Toda implementación se documentará dentro del código en castellano.

## Decisión pendiente de canal

El envío desatendido de correo requiere un servicio de envío autorizado. Compartir con la app de WhatsApp mediante Android exige interacción; el envío desatendido por WhatsApp requiere una integración autorizada con WhatsApp Business Platform y sus requisitos operativos. Pendiente acordar cómo ofrecer WhatsApp sin romper la condición de envío automático ni introducir servicios o costes no aprobados. No se ha implementado el envío en este paso.
