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

- PDF: conservar el registro de venta en todas las modalidades. Determinar si generar/consultar PDF es común o exclusivo de Premium. Enviar PDF por correo/WhatsApp es Premium, confirmado. Ajuste de preguntar siempre y alternativa correo si WhatsApp no puede enviar: confirmados (ver RECIBOS_2_6.md).
- Inventario: Premium, propuesto, coherente con el stock y costes actuales. Definir entradas/ajustes, valoración e historial.
- Listados e informes: propuesta Free = resúmenes básicos ya existentes; Premium = informes agregados, estadísticas, comparativas y exportaciones. Concretar el alcance de cada informe.
- Contratar/cancelar Premium: acceso a contratación para Free; ventajas activas tras suscripción válida. Definir precios, periodo, caducidad y política de descenso.
- Leyenda del calendario: común, propuesta (ya existe un diálogo accesible desde calendario).
- Integración WooCommerce: Premium, propuesta; no implementada.
- Usuarios colaboradores/roles de empresa: antigua propuesta sin confirmar; decidir si entra en el producto futuro.

## Decisiones de la revisión

### 1. Venta mediante catálogo

- Confirmado: Free podrá vender desde el catálogo, con un máximo de 10 unidades vendidas mediante catálogo por mercadillo, no 10 referencias distintas. Cuenta la cantidad vendida aunque se repita la misma referencia; ejemplo: vender 10 raciones de churros agota el límite aunque exista un único artículo de catálogo utilizado.
- Premium: sin ese límite.
- Las ventas manuales no están sujetas a este límite de catálogo.
- Confirmado: no se impone ningún límite adicional de referencias distintas por ticket. Se pueden combinar varios artículos y conceptos manuales en una misma venta, siempre que las unidades de catálogo acumuladas en el mercadillo no superen 10. Esta decisión sustituye la propuesta anterior de una sola referencia por ticket.
- Precisar al implementar el tratamiento de abonos y el aviso al alcanzar el límite. No alterar ventas confirmadas.

### 2. Actualización manual / sincronización

Confirmado: el gesto de deslizar hacia abajo en el calendario estará disponible tanto en Free como en Premium. Ambas modalidades mantienen sincronización automática y actualización manual. La restauración de datos previos en instalación vacía sigue siendo exclusiva de Premium.

### 3. Saldos guardados y asignación

Confirmado: Free y Premium permiten introducir saldo inicial manualmente, realizar arqueo, consultar saldo final y cerrar mercadillo. Guardar saldo para otro mercadillo, asignarlo y gestionar saldos pendientes son exclusivos de Premium.

### 4. Configuración visual, idioma y moneda

Confirmado: todas las opciones de configuración actuales (tema claro/oscuro, idioma, tipografía y moneda) estarán disponibles tanto en Free como en Premium. Por ahora la única opción exclusiva de Premium en configuración será «Solicitar siempre envío de recibo». Free no podrá enviar recibos. Demo conserva configuración de solo lectura.

Requisito futuro confirmado: arquitectura de idiomas ampliable mediante módulos independientes descargables y de pago, adquiribles por separado del plan Premium. Una vez adquirido e instalado un idioma debe aparecer entre las opciones de selección y poder utilizarse sin conexión. Ejemplo orientativo: francés por 0,50 EUR; precio, idiomas incluidos de serie, alcance de compras/restauración y mecanismo de distribución pendientes de concretar. No implementar todavía ni asumir que el precio propuesto es definitivo. Preparar la futura gestión de traducciones para evitar depender de una nueva versión de la app por cada idioma.

### 5. Siguiente cuestión: recibos PDF y datos de empresa

Envío y preferencia de solicitud Premium confirmados. Propuesta pendiente: mantener registro y consulta de ventas en Free; generación y consulta del recibo PDF también en Free, sin envío integrado, y datos de empresa/personalización comercial en Premium. Confirmar si el documento PDF debe ser exclusivo de Premium o común a ambas modalidades.

No se han modificado permisos ni código funcional. La matriz se revisará con el usuario, anotando decisiones antes de pasar a la implementación.
