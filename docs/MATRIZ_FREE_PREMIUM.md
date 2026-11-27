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
- Propuesta adicional aún por cerrar: una referencia distinta de catálogo por ticket en Free. El límite de 10 unidades se aplica al conjunto de tickets del mercadillo.
- Precisar al implementar el tratamiento de abonos y el aviso al alcanzar el límite. No alterar ventas confirmadas.

### 2. Actualización manual / sincronización

El gesto de deslizar hacia abajo en el calendario solicita una sincronización inmediata y actualmente está restringido a Premium. Se propone permitirlo también en Free, sin alterar la sincronización automática ni la restauración de una instalación vacía exclusiva de Premium. Pendiente de confirmación.

No se han modificado permisos ni código funcional. La matriz se revisará con el usuario, anotando decisiones antes de pasar a la implementación.
