# Matriz Free / Premium — revisión de producto

Base: código 2.5, 27-11-2026. La Demo conserva las reglas ya cerradas; no se reabre su diseño. Se distingue comportamiento actual de propuestas nuevas. No se programarán cambios de permisos hasta aprobar la matriz.

## Funciones actuales (revisión estática)

| Función | Free actual | Premium actual |
|---|---|---|
| Registro, perfil, sesión persistente y operación offline | Sí | Sí |
| Calendario y gestión de mercadillos | Un mercadillo por fecha | Varios por fecha |
| Categorías y artículos | Sí; sin límite general detectado fuera de Demo | Sí |
| Ventas manuales | Sí | Sí |
| Ventas seleccionando productos del catálogo | No; pestaña bloqueada | Sí |
| Control de coste y stock de artículos | No | Sí |
| Gastos, abonos y arqueo | Sí | Sí |
| Gestión de saldos guardados y asignación a mercadillos | Bloqueada en las pantallas; algunos repositorios solo comprueban que no sea Demo | Sí |
| Datos de empresa | Bloqueados en menú de Utilidades | Sí |
| Consulta de resúmenes de mercadillos | Sí | Sí |
| Sincronización automática de cambios locales | Sí | Sí |
| Forzar sincronización en calendario | No | Sí |
| Restaurar datos previos en instalación vacía | No | Sí |
| Tema claro/oscuro | Sí | Sí |
| Cambiar idioma, fuente, moneda | No | Sí |
| Publicidad, con consentimiento | Sí | No |

## Funciones futuras — propuestas no aprobadas salvo lo indicado

- PDF: generación, consulta, personalización y envío exclusivos de Premium; registro y consulta de ventas comunes. Confirmado (ver RECIBOS_2_6.md).
- Inventario: exclusivo de Premium, confirmado; incluye entradas y ajustes, historial, valoración y avisos de stock mínimo.
- Listados e informes: distribución confirmada. Free conserva listados y resúmenes individuales; Premium añade informes agregados, estadísticas, comparativas y exportaciones.
- Contratar/cancelar Premium: modelo comercial confirmado en el apartado 16; precios definitivos pendientes.
- Leyenda del calendario: común a Free y Premium, ya implementada y confirmada por el usuario. Retirada de tareas pendientes; no modificar.
- Integraciones con cualquier plataforma de venta online: exclusivamente Premium, confirmado; no implementadas.
- Usuarios colaboradores/roles de empresa: exclusivo de Premium, confirmado; fase final junto con multiempresa.

## Decisiones de la revisión

### 1. Venta mediante catálogo

- Confirmado: Free podrá vender desde el catálogo, con un máximo de 10 unidades vendidas mediante catálogo por mercadillo, no 10 referencias distintas. Cuenta la cantidad vendida aunque se repita la misma referencia; ejemplo: vender 10 raciones de churros agota el límite aunque exista un único artículo de catálogo utilizado.
- Premium: sin ese límite.
- Las ventas manuales no están sujetas a este límite de catálogo.
- Confirmado: no se impone ningún límite adicional de referencias distintas por ticket. Se pueden combinar varios artículos y conceptos manuales en una misma venta, siempre que las unidades de catálogo acumuladas en el mercadillo no superen 10. Esta decisión sustituye la propuesta anterior de una sola referencia por ticket.
- Abonos y aviso de límite: reglas confirmadas en el apartado 15. No alterar ventas confirmadas.

### 2. Actualización manual / sincronización

Confirmado: el gesto de deslizar hacia abajo en el calendario estará disponible tanto en Free como en Premium. Ambas modalidades mantienen sincronización automática y actualización manual. La restauración de datos previos en instalación vacía sigue siendo exclusiva de Premium.

### 3. Saldos guardados y asignación

Confirmado: Free y Premium permiten introducir saldo inicial manualmente, realizar arqueo, consultar saldo final y cerrar mercadillo. Guardar saldo para otro mercadillo, asignarlo y gestionar saldos pendientes son exclusivos de Premium.

### 4. Configuración visual, idioma y moneda

Confirmado: todas las opciones de configuración actuales (tema claro/oscuro, idioma, tipografía y moneda) estarán disponibles tanto en Free como en Premium. Por ahora la única opción exclusiva de Premium en configuración será «Solicitar siempre envío de recibo». Free no podrá enviar recibos. Demo conserva configuración de solo lectura.

Requisito futuro confirmado: arquitectura de idiomas ampliable mediante módulos independientes descargables y de pago, adquiribles por separado del plan Premium. Una vez adquirido e instalado un idioma debe aparecer entre las opciones de selección y poder utilizarse sin conexión. Ejemplo orientativo: francés por 0,50 EUR; precio, idiomas incluidos de serie, alcance de compras/restauración y mecanismo de distribución pendientes de concretar. No implementar todavía ni asumir que el precio propuesto es definitivo. Preparar la futura gestión de traducciones para evitar depender de una nueva versión de la app por cada idioma.

### 5. Recibos PDF y datos de empresa

Confirmado: guardar y consultar ventas en Free y Premium. Generar, consultar, enviar y reenviar PDF, configurar datos de empresa y añadir logotipo son exclusivos de Premium. Se mantienen los acuerdos de RECIBOS_2_6.md.

### 6. Multiempresa futura

Confirmado: Premium podrá gestionar varias empresas desde una misma cuenta en el futuro. Es una de las últimas funcionalidades que se implementarán. No confundir multiempresa con usuarios colaboradores de una empresa (pendiente de decidir). Mantener abierta la evolución hacia aislamiento de datos por empresa sin introducir ahora cambios de esquema ni lógica multiempresa. Número de empresas, permisos, precios y migración pendientes de concretar.

### 7. Inventario

Confirmado: Free y Premium permiten crear categorías y artículos sin límite general (salvo Demo), establecer precios de venta y marcar favoritos. Free puede vender hasta 10 unidades de catálogo por mercadillo; Premium sin ese límite. Control de coste, existencias, descuento automático, entradas de mercancía, ajustes, historial de movimientos, valoración económica y avisos de stock mínimo son exclusivos de Premium.

### 8. Listados, estadísticas e informes

Confirmado: Free y Premium pueden consultar los resúmenes individuales de cada mercadillo, sus ventas, gastos, arqueos y totales, así como listados básicos de mercadillos. Premium exclusivamente: informes por intervalo de fechas, estadísticas mensuales/anuales, comparativas entre mercadillos, artículos más vendidos, análisis agregado por métodos de pago, rentabilidad de artículos, evolución de ingresos/gastos, gráficos y exportaciones PDF/CSV. Se mantienen los desgloses básicos de cada mercadillo ya existentes.

### 9. Límites de mercadillos y alta rápida desde calendario

Confirmado: Free puede crear un mercadillo por fecha y un máximo de 50 mercadillos por año; Premium puede crear varios por fecha y no tiene límite anual. No se añade límite mensual ni histórico. Calendario, leyenda de estados, planificación, consulta de historial y gestión de cancelados comunes.
- Alta rápida Premium: al pulsar un día sin mercadillo programado se abre directamente la pantalla de alta de mercadillos con la fecha seleccionada ya introducida. No se crea el mercadillo hasta confirmar el formulario.
- Free: al pulsar ese día se muestra un aviso indicando que el alta directa desde el calendario es exclusiva de Premium; puede seguir utilizando la opción habitual de alta, dentro de sus límites.
- Confirmado: el límite anual se computa por año natural de la fecha de celebración del mercadillo, no por fecha de alta. Los cancelados cuentan y borrar un mercadillo no libera cupo. Cada 1 de enero empieza un nuevo cupo de 50 para el nuevo año; planificar fechas futuras consume el cupo del año correspondiente. Se mantiene la consulta del historial de años anteriores.

### 10. Publicidad

Confirmado: Demo y Free pueden mostrar banners e intersticiales cuando proceda según consentimiento, conexión y límites de frecuencia; Premium queda libre de publicidad. Los intersticiales se reservan a transiciones no críticas y nunca deben interrumpir una venta, cobro, confirmación ni registro de gastos. Principio de producto confirmado: no introducir restricciones ni molestias que provoquen el abandono de la aplicación; incentivar la compra mediante ventajas reales de Premium.

### 11. Integración con tiendas online

Confirmado: cualquier integración con plataformas de venta online será exclusiva de Premium (WooCommerce y cualquier otra futura plataforma). Free conserva catálogo independiente y ventas manuales/limitadas de catálogo. Reglas de importación y sincronización se definirán en su fase de desarrollo.
- Requisito futuro específico: conector privado para la nueva tienda propia de Tokamadera, independiente de WordPress/WooCommerce. No mostrar ni permitir el uso de este conector al resto de usuarios; definir autorización y configuración en su fase de desarrollo. No implementar ahora.

### 12. Usuarios colaboradores

Confirmado: Premium permitirá invitar a otros usuarios con cuentas individuales a colaborar en una misma empresa, sin compartir credenciales, y gestionar permisos por rol. Se implementará en una de las últimas fases junto con multiempresa. Límites, roles y coste por definir entonces. El uso de una misma cuenta en varios dispositivos sigue disponible para ambas modalidades y no constituye colaboración entre cuentas.

### 13. Descenso de Premium a Free

Confirmado: al terminar la vigencia Premium, pasar a Free conservando ventas, gastos, categorías, artículos, datos de empresa, costes, stock y PDF históricos. Costes, control de stock, consulta de PDF y estadísticas avanzadas se deshabilitan; se activa publicidad. Los recibos ya solicitados y pendientes de envío se completan, sin nuevos envíos. Los idiomas adquiridos separadamente se mantienen. Al reactivar Premium se recupera el acceso a funciones y datos conservados; puede ser necesario ajustar stock por ventas durante el periodo Free.
- Mercadillos ya programados: se mantienen aunque superen 50 al año; las nuevas altas se sujetan a los límites Free. Excepción confirmada: si hay dos o más mercadillos programados para una misma fecha, se conserva el creado primero y se eliminan de la planificación los creados después. No aplicar esta excepción a mercadillos históricos. Salvedad confirmada: solo se dan de baja los mercadillos futuros posteriores que no tengan operaciones registradas; se conservan los mercadillos con ventas o gastos, y todos los históricos, aunque coincidan en fecha. Si caduca Premium durante dos mercadillos simultáneos con operaciones, ambos se conservan. No borrar datos históricos asociados.


### 14. Mercadillos duplicados al descender de plan

Confirmada la salvedad de conservación indicada en el apartado 13. No introducir borrado irreversible de operaciones ni modificación de estados definitivos.

### 15. Abonos y límite de ventas de catálogo Free

Confirmado: mantener abonos y devoluciones en Free y Premium. El límite Free de 10 unidades vendidas mediante catálogo por mercadillo se computa sobre las ventas positivas confirmadas, sin recuperar cupo al abonar una unidad; no contar borradores o ventas no confirmadas. Permitir siempre la devolución de ventas válidas sin consumir cupo. Informar de las unidades restantes antes de confirmar la venta; si excede el cupo, permitir ajustar las líneas de catálogo o continuar mediante conceptos manuales, sin rechazar una venta manual por el límite. Confirmado. No modificar código funcional todavía.

### 16. Contratación Premium e idiomas de pago

Confirmado: suscripción mensual renovable, suscripción anual renovable abonada de una sola vez con descuento respecto a doce mensualidades, y prueba gratuita de 15 días una sola vez por cuenta registrada. Sin permanencia obligatoria; cancelar la renovación conserva Premium hasta el final de la vigencia correspondiente. Reactivar recupera las funcionalidades y datos conservados según los apartados 13 y 14. Los módulos de idiomas se adquieren por pago único, independiente de la suscripción.

Ejemplo orientativo aprobado como ilustración del descuento anual: si la mensualidad fuera 10 EUR, la anualidad podría ser 100 EUR en vez de 120 EUR. No son precios definitivos ni un porcentaje de descuento obligatorio. Precios, condiciones de activación de la prueba e implementación de facturación por concretar en la fase comercial. No activar cobros en esta fase.

## Cierre de la revisión general de modalidades

Las 16 cuestiones están acordadas. Las tablas de comportamiento actual describen la base 2.5, no el resultado de la futura implementación. Aplicar las decisiones confirmadas en los apartados anteriores como especificación de producto.

Propuesta de orden: primero adaptar las funciones existentes y límites Free/Premium (incluido el alta rápida desde calendario), sin introducir aún facturación ni recibos nuevos; después recibos PDF, inventario, informes, contratación y módulos de idiomas. Las integraciones online quedan para una fase posterior; multiempresa y colaboradores para las últimas fases. Concretar los detalles de cada módulo cuando llegue su turno. Actualizar comentarios internos del código con cada modificación.

En esta revisión solo se ha actualizado documentación; todavía no se han modificado permisos ni código funcional.
