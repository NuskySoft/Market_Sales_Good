# Historial de versiones

## 2.3 — 2026-11-25

- F07: guarda movimientos pendientes de stock en la misma transacción que el ticket y sus líneas. Un fallo posterior no invalida la venta.
- Aplica cada descuento y su confirmación en una transacción independiente; reintenta en segundo plano y al abrir la app, también sin conexión.
- Serializa subidas de artículos y confirma únicamente el estado enviado para no perder descuentos que entren durante una subida.
- Room 18 → 19: añade la tabla de movimientos sin borrar datos ni reprocesar ventas antiguas. No admite volver a una base de datos anterior borrándola.
- Incluye pruebas instrumentadas de recuperación, concurrencia y migración; pendientes de ejecutar en Android.
- F11 de la versión 2.2 validada por el usuario.


## 2.2 — 2026-11-25

- F11: al confirmar una venta Premium, descuenta los productos con control de stock independientemente de la pestaña seleccionada.
- Mantiene las reglas de Premium, artículo con control de stock y conceptos manuales sin movimiento de existencias.
- Cambio de negocio confirmado por el usuario. Prueba manual pendiente: stock 10, vender 2 y finalizar desde Manual o Productos debe dejar 8.
- No modifica todavía la transacción integral de stock (F07), la lógica de abonos ni el esquema Room.


## 2.1 — 2026-11-25

- F03: corrige contarPorUsuario de MercadilloDao para contar mercadillos del propietario, en lugar de categorías.
- Añade prueba Room con cantidades distintas de categorías y mercadillos y aislamiento entre propietarios.
- La Demo 2.0 ha sido probada por el usuario e incorporada a master; respaldo backup/v2.0-validada.
- Room permanece en 18; no se cambia el esquema ni se modifican datos existentes.


## 2.0 — 2026-11-25

- Separa Demo local, cuenta gratuita y Premium; conserva cuentas y sesión persistente.
- Demo: 1 mercadillo, 1 categoría, 1 artículo y 10 tickets; permite ventas en cualquier estado, gastos y arqueo.
- Bloquea abonos, borrado del mercadillo, asignación de saldo, Perfil, Utilidades y cambios de configuración en Demo.
- Añade aviso fijo y acceso a registro/login; confirma que los datos Demo no se trasladan a las cuentas.
- Excluye Demo de sincronización y recuperación, y conserva el derecho Premium de restauración en instalaciones vacías.
- Guarda límite de tickets, cabecera y líneas en una transacción local, con identificadores únicos y subida en segundo plano.
- Room continúa en versión 18; no se eliminan ni reasignan datos de cuentas.


## 1.2 — 2026-11-25

- Completa claves de traducción en español e inglés para Utilidades, Perfil, empresa y pagos.
- Aplica el idioma seleccionado en el menú lateral, mensajes de acceso y sincronización, descripciones de estados y etiquetas de accesibilidad.
- Corrige dos avisos en inglés dentro del diccionario español.
- Añade comprobación automática de claves, duplicados, parámetros y argumentos de idioma.
- Incorpora revisión funcional y técnica de módulos, funciones incompletas y mejoras propuestas.


## 1.1 — 2026-11-25

- Permite consultar desde el calendario los datos de mercadillos cancelados en modo de solo lectura. Corrección validada por el usuario en Android Studio.
- Configuración muestra la versión real de la aplicación mediante BuildConfig.VERSION_NAME.
- Se establece la numeración MAYOR.MENOR y el incremento de versionCode por entrega.

## 1.0 — Versión anterior

- Versión de partida anterior a la implantación de este historial.
