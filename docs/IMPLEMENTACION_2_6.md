# Market Sales 2.6 — adaptación de funcionalidades existentes

## Alcance y respaldo

Base: 2.5, commit e8f081c372845177540a5c1cedddb49920d4e22d. Respaldo de código: backup/v2.5-validada. Rama de entrega: feature/v2.6-modalidades. VersionName 2.6, versionCode 10, Room 21.

Aplicación de los acuerdos de docs/MATRIZ_FREE_PREMIUM.md. Esta entrega no incorpora facturación ni caducidad comercial automática: aplica el plan existente de la cuenta. Tampoco crea el módulo de recibos PDF, inventario, informes, paquetes de idiomas ni conectores.

## Comportamiento

- Free admite 10 unidades positivas de catálogo por mercadillo. Los abonos no devuelven cupo, los conceptos manuales no consumen cupo. Se comprueba antes del cobro y dentro de la transacción de confirmación, sin modificar ventas confirmadas.
- Free admite 50 mercadillos por año de celebración; cuentan las bajas y cancelaciones conservadas. Uno activo por día. Se valida alta y cambio de fecha. Editar en la misma fecha no consume otro cupo; cambiar de año comprueba el cupo de destino.
- Premium permite alta rápida desde un día vacío del calendario con la fecha preseleccionada. Free recibe aviso y conserva el alta normal.
- Ajustes básicos y actualización manual para ambas cuentas registradas. Demo permanece local y con configuración de solo lectura.
- Inventario, datos de empresa y gestión/transferencia de saldos reservados a Premium, incluidos controles de acceso a rutas y repositorios. El arqueo es común y Free puede cerrar sin transferir saldo.
- La edición Free conserva los campos de coste y existencias del registro persistido. No se pierde el historial ni se borran datos de empresa o saldos al bajar de plan.
- Tras sincronización completa, se retiran lógicamente los mercadillos futuros duplicados posteriores al primero cuando se conoce el orden de creación, están planificados y no tienen operaciones ni saldos vinculados. Si llegan operaciones posteriores de otro dispositivo, se recupera su visibilidad. Se preservan históricos y mercadillos activos con operaciones.
- Los anuncios a pantalla completa se limitan a rutas no críticas y esperan a que esté determinado el plan.

## Migración y límites técnicos

Room 20→21 incorpora creadoEn (nullable) y bajaPorPlan (false por defecto). No se inventan fechas: si un grupo antiguo no tiene orden de creación fiable, se conserva íntegro. El respaldo Git conserva código, no la base del teléfono. Copiar la base Room 20 antes de instalar; no hacer downgrade de esquema sobre la base migrada.

Los límites son transaccionales en cada dispositivo. Dos dispositivos desconectados pueden superar simultáneamente un cupo; la conciliación no anula ventas confirmadas. Tras recibir los datos, se aplica el límite sobre lo acumulado. La retirada automática de duplicados depende de sincronización completa para proteger operaciones aún no recibidas. El cambio de año del mercadillo mueve su registro al año de destino. El reinicio destructivo de cuenta, ya existente, es un flujo independiente.

## Verificación

Ejecutadas fuera de Android: consultas SQLite copiadas literalmente de los nuevos DAO y los dos ALTER TABLE de la migración, sobre tablas de prueba. Resultados correctos: cupo anual, cambio de año, cancelados/bajas, edición, propietario, unidades positivas confirmadas, abonos, manuales, borradores, operaciones vinculadas y bajas por plan. Compilado y ejecutado AppModePolicy aislado con 14 comprobaciones de Kotlin. Se añadieron pruebas JUnit de política en el repositorio. No equivale a compilar Room/KSP/Compose ni a ejecutar la aplicación.

Pendiente: Gradle/Android SDK no disponibles en el entorno. Validar assembleDebug, migración real desde 2.5, Demo, ventas 9+1 y rechazo de la unidad 11, ventas manuales y abonos, alta 50/51 y cambio de año, alta rápida Premium, ajustes, publicidad, arqueo tras descenso, conservación de costes/existencias, restauración Premium y sincronización Firebase/multidispositivo. El fixture histórico de StockMovementTest que simula Room 18 a partir de una base moderna sigue pendiente de revisión; no se ha validado esa prueba.
