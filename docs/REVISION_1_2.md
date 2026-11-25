# Market Sales revisión integral

Funciones actuales trabajo pendiente y mejoras propuestas

Destinatario  NuskySoft
Fecha  25 de noviembre de 2026
Versión de partida  1.1
Entrega de traducciones  1.2

La aplicación ya ofrece un ciclo de trabajo de mercadillo: calendario, artículos y categorías, ventas manuales o por productos, registro de gastos, consulta de movimientos, arqueo y gestión de saldos. No todas las opciones visibles están terminadas: Inventario, Listados, gastos automáticos y envío de recibos tienen estructura o interfaz parcial, pero no completan la función que anuncian.

La prioridad recomendada es proteger la integridad de ventas y sincronización antes de añadir funciones nuevas. La revisión identifica riesgos concretos de identificadores, sincronización de gastos, separación de usuarios y migraciones. La entrega 1.2 corrige traducciones y añade controles de cobertura; los cambios contables y de almacenamiento propuestos no se aplican en esta entrega.

## Alcance y forma de interpretar los resultados

Base analizada: rama master de NuskySoft/Market_Sales_Good, commit 7e1d66fb186d902d66dca1521bd753993ed91e52. Se inventariaron los 140 archivos Kotlin de producción, los 3 archivos de pruebas existentes, la navegación, las entidades y consultas Room, los repositorios, los recursos relevantes, el manifiesto y Gradle. El anexo identifica cada módulo de producción, sus funciones declaradas y su huella de archivo.

La revisión es estática: lectura del código activo y contraste de referencias, búsquedas de rutas, funciones vacías, diccionarios y consultas. Se distinguen comportamientos implementados de propuestas y de riesgos deducidos del código. No equivale a haber ejecutado todas las líneas ni todas las combinaciones posibles de estados. Los bloques comentados se consideran historia del código, no funciones disponibles.

No se han consultado los datos reales del móvil ni la consola de Firebase. Tampoco se han comprobado reglas desplegadas de Firestore, índices remotos, configuración OAuth, compras ni políticas externas. La compilación Android no pudo ejecutarse aquí porque el entorno no puede descargar Gradle. La consulta de cancelados sí fue validada previamente por el usuario en su Android Studio.

## Contenido

Parte 1 Funciones actuales por módulo
Parte 2 Funciones incompletas y problemas encontrados
Parte 3 Mejoras nuevas y orden de trabajo
Traducciones y validación
Anexo Inventario de módulos

## Parte 1 Funciones actuales por módulo

### Inicio y navegación

Inicia la interfaz Compose con tema de la app, muestra una pantalla de presentación y abre Mercadillos. Declara las rutas de configuración, perfil, categorías, artículos, inventario, listados, utilidades, saldos, empresa, acceso y cierre de sesión. Las rutas con identificador abren edición, ventas, gastos, carrito, método de pago, cambio, recibo, resumen y arqueo.

Los contenedores de ventas y gastos cargan el mercadillo por identificador y comparten los modelos de carrito dentro del flujo. Se codifican parámetros de navegación con Uri.encode en varios pasos. La ruta de depuración se declara solo para compilaciones DEBUG. Hay un menú principal en uso y un MenuLateral alternativo que no debe confundirse con una segunda aplicación.

Código de referencia  MainActivity.kt y NavGraphArqueo.kt

### Calendario y ficha del mercadillo

Muestra un calendario mensual, permite cambiar de mes, representa estados con colores y muestra una leyenda. Agrupa los mercadillos por día y filtra por usuario. Cuando hay varios en una fecha, presenta un selector. Las fichas cerradas o pendientes de arqueo se dirigen al flujo de arqueo; las restantes, a la ficha del mercadillo.

La ficha guarda fecha, lugar, organizador, gratuidad, importe de inscripción, mesa, carpa, punto de luz, horas de inicio y fin y saldo inicial. Admite importes con coma o punto. Valida campos, horarios, fechas e importes. El saldo de un mercadillo en curso tiene confirmación específica si se modifica; su fecha queda bloqueada. La eliminación visible se restringe a estados programados. Los cancelados se consultan en modo de solo lectura desde la corrección ya validada.

Código de referencia  PantallaMercadillos.kt, MercadilloViewModel.kt, MercadillosManager.kt y MercadilloRepository.kt

### Estados y automatización

Define siete estados, colores, iconos, prioridades, descripciones y restricciones. El repositorio actual aplica una ventana de actividad desde el día del mercadillo hasta las cinco de la mañana del día siguiente, con zona Europe/Madrid. Al vencer la ventana pasa a pendiente de arqueo cuando hay líneas de venta o a cancelado cuando no las hay.

WorkManager solicita tareas diarias alrededor de las 00 y las 05 y también ejecuciones puntuales. Son trabajos programados, no una garantía de ejecución exacta al segundo. Existe además otra función de cálculo de estados en EstadosMercadillo; conviene unificar ambos criterios antes de ampliarlos. El gestor del calendario mantiene flujos y un refresco de minuto.

Código de referencia  EstadosMercadillo.kt, AutoEstadoScheduler.kt y AutoEstadoWorker.kt

### Categorías

Permite crear, editar y eliminar categorías, escoger color y validar nombres y colores. Asigna identificadores, conserva un orden y filtra las categorías por usuario. Presenta mensajes de validación, carga y resultado y un selector de color.

El repositorio guarda primero en Room y trata de sincronizar con Firestore. Usa banderas de sincronización y reintenta al recuperar conectividad. La eliminación física local y remota requiere una estrategia de borrado pendiente para no reaparecer tras una restauración si se elimina sin conexión.

Código de referencia  PantallaCategorias.kt, CategoriaViewModel.kt, CategoriaRepository.kt y CategoriaDao.kt

### Artículos y catálogo

Permite alta, edición y eliminación de artículos con nombre, categoría y precio de venta. Puede crear una categoría desde el formulario del artículo. Incluye favorito, controles opcionales de precio de coste y stock, y valida duplicados, precios, coste y existencias. Los controles de coste y stock se habilitan para Premium.

El modelo almacena fotoUri, pero no se ha localizado un flujo completo de selección, subida y visualización de fotos de producto. El catálogo en venta permite búsqueda y filtros por categoría. La pantalla Inventario independiente todavía no ofrece gestión real; el stock existente se gestiona desde artículos y ciertos movimientos de venta y abono.

Código de referencia  PantallaArticulos.kt, ArticuloViewModel.kt, ArticuloRepository.kt y ArticulosDao.kt

### Ventas manuales y por productos

La venta manual utiliza una descripción y un teclado de importes con dos decimales, borrado y doble cero. Añade líneas al ticket y calcula su total. La venta por productos está condicionada a Premium, ofrece categorías y búsqueda y agrupa cantidades al añadir el mismo producto.

El carrito permite eliminar líneas y ajustar cantidad o precio unitario. Guarda borradores en una caché en memoria por mercadillo. La confirmación genera cabecera y líneas en Room y después intenta enviarlas a Firestore. El coste y el stock son opcionales. El descuento de stock al cerrar la venta tiene una condición dependiente de la pestaña activa, por lo que un ticket mezclado requiere revisión.

Código de referencia  PantallaVentas.kt, PestanaVentaManual.kt, PestanaVentaProductos.kt y VentasViewModel.kt

### Método de pago cambio y recibo

Permite indicar efectivo, Bizum o tarjeta. En efectivo abre un cálculo de entregado y cambio. Bizum y tarjeta se registran como métodos declarados; el código revisado no realiza un cobro con banco, TPV o pasarela de Bizum.

La última pantalla presenta el total y el método y permite finalizar la venta. Conserva variables y callbacks para correo y WhatsApp, pero no muestra un envío completo ni genera un documento de recibo. No debe considerarse implantado el envío de recibos al cliente.

Código de referencia  PantallaMetodoPago.kt, PantallaCambio.kt, PantallaEnviarRecibo.kt y MainActivity.kt

### Gastos

Permite introducir descripción e importe con teclado, añadir varias líneas al carrito, editar o borrar líneas y confirmar su registro. Clasifica el pago en efectivo, Bizum o tarjeta. Para efectivo pregunta si el gasto se descuenta de la caja del mercadillo.

La entidad incluye descontarDeCaja. El arqueo consulta específicamente los gastos en efectivo que salen de caja; el resultado económico incluye los gastos de todos los métodos. Guardar un gasto actualiza el total local del mercadillo dentro de una transacción Room. Hay una pestaña de gastos automáticos, pero únicamente anuncia su futura disponibilidad.

Código de referencia  PantallaGastos.kt, GastosViewModel.kt, PantallaGastosCarrito.kt y GastosRepository.kt

### Resumen de ventas gastos y abonos

Presenta pestañas con las líneas de venta, gastos y totales del mercadillo. Asocia métodos a recibos y distingue visualmente los abonos. La interfaz permite abonar una línea completa una sola vez y, en artículos con control de stock y usuario Premium, reponer la cantidad.

El abono activo de la interfaz inserta una línea negativa asociada al recibo original. Hay también un crearAbono en el repositorio que crea otro recibo con método ABONO; son caminos distintos que conviene consolidar. Desde el arqueo se oculta el botón de abono según la ruta de procedencia. El resumen permite finalizar un mercadillo en curso con confirmación.

Código de referencia  PantallaResumenMercadillo.kt, PestanaResumenVentas.kt y PestanaResumenGastos.kt

### Arqueo de caja y resultado económico

La ficha de arqueo muestra datos y totales por método y enlaza a resumen, conteo de caja, resultado y asignación de saldo. En caja se introducen totales de billetes y monedas, se suman y se comparan con el efectivo esperado: saldo inicial más ventas en efectivo menos gastos en efectivo descontables de caja.

Muestra si la caja cuadra, sobra o falta. La confirmación persiste el conteo y cambia el estado; Premium puede pasar a asignación de saldo y Free a cierre completo. El resultado económico suma ventas de efectivo, Bizum y tarjeta y resta gastos de esos métodos más la inscripción. No debe confundirse este resultado con el dinero físico de caja.

Código de referencia  PantallaArqueo.kt, PantallaArqueoCaja.kt, PantallaResultadoMercadillo.kt y ArqueoViewModel.kt

### Asignación y conservación de saldos

El flujo Premium permite guardar un saldo para más adelante o asignarlo a un mercadillo programado. Contempla retirar o añadir efectivo antes de confirmar y advierte cuando ya existe un saldo guardado o un destino con saldo. Al completar, cierra el mercadillo de origen.

La utilidad de saldos pendientes muestra el saldo y los datos de origen en modo de consulta. El repositorio trabaja con el último saldo no consumido del usuario y puede reemplazarlo. La sincronización de consumo, sustitución y recuperación tiene huecos que se detallan en la parte 2; por ello esta función existe, pero su continuidad entre dispositivos no está demostrada.

Código de referencia  PantallaAsignarSaldo.kt, AsignarSaldoViewModel.kt, SaldoGuardadoRepository.kt y PantallaSaldosPendientes.kt

### Datos de empresa y utilidades

Utilidades presenta acceso a saldos pendientes y datos de empresa. Para Free muestra un aviso y deshabilita esas opciones. Empresa permite guardar o actualizar NIF o DNI, nombre comercial, razón social, dirección, población, código postal, provincia y país.

Los datos se almacenan en la entidad empresa y se envían al documento empresa/datos de Firestore. No existe un enlace completo con emisión de recibos. La falta de propietario en ese documento y en la entidad exige revisar el aislamiento de cuentas antes de usarlo como servicio multiusuario.

Código de referencia  PantallaUtilidades.kt, PantallaDatosEmpresa.kt y EmpresaRepository.kt

### Acceso registro perfil y salida

Ofrece registro e inicio de sesión por correo y contraseña y acceso con Google mediante token. Conserva el usuario autenticado de Firebase, su nombre, correo, foto y condición Premium. El cierre de sesión vuelve a usuario_default y reinicia el estado visible de cuenta.

Perfil permite modificar nombre y correo y cambiar contraseña con reautenticación por correo y contraseña actual. Muestra validaciones, mensajes de éxito y error y opciones de privacidad de anuncios. El cambio de contraseña para cuentas exclusivamente Google necesita tratamiento específico. No se localizó una pantalla completa de recuperar contraseña ni un flujo de verificación de correo.

Código de referencia  PantallaLogin.kt, PantallaPerfil.kt, AuthViewModel.kt, AuthRepository.kt y UserRepository.kt

### Configuración y versiones

Permite elegir español o inglés, Montserrat Poppins o Roboto, tema claro u oscuro y moneda. Idioma, fuente y moneda están condicionados a Premium en la interfaz; el tema está disponible con una política más amplia. Muestra cuenta, plan y versión real del APK mediante BuildConfig.VERSION_NAME.

La versión sigue MAYOR.MENOR; 1.2 corresponde a esta entrega y versionCode pasa a 3. La base de datos mantiene su propia versión 18. La configuración se almacena localmente y se sincroniza al documento global configuraciones/configuracion_global; esta decisión puede hacer que diferentes usuarios compartan preferencias remotas y debe revisarse.

Código de referencia  PantallaConfiguracion.kt, ConfigurationManager.kt y ConfiguracionRepository.kt

### Sincronización y uso sin conexión

La app conserva datos en Room y usa Firestore para copia o sincronización. Las entidades principales llevan propietario, versión, fecha de modificación y bandera de sincronización. Varios repositorios observan la conectividad y reintentan pendientes. El acceso realiza comprobaciones locales y remotas y presenta progreso.

El camino principal permite recuperación inicial con Premium; si hay datos locales sube cambios y usa recuentos para decidir si falta información remota. Existen varios bootstrap y serializadores con reglas diferentes. No se ha demostrado una sincronización completa de conflictos, borrados, empresa y saldos; no debe presentarse como copia integral verificada.

Código de referencia  AuthRepository.kt, FullBootstrapper.kt, DefaultBootstrapper.kt y los repositorios de cada entidad

### Publicidad y privacidad

Integra consentimiento UMP, opciones de privacidad, banners e intersticiales. Comprueba consentimiento y Premium para servir anuncios; al pasar a Premium descarta el intersticial precargado. El controlador guarda contadores diarios en preferencias y fija un máximo de un intersticial al día en el código actual.

Dos módulos importan BuildConfig de Firebase en lugar del de la aplicación, por lo que la selección de comportamiento DEBUG debe revisarse. La configuración real de anuncios, formularios y consentimiento depende de los servicios desplegados, que no se han inspeccionado.

Código de referencia  AdsConsentManager.kt, AdsInterstitialController.kt, AdsBottomBar.kt y BannerAd.kt

### Tema componentes y utilidades internas

Define paletas clara y oscura, tipografías, formas, barras de acciones, tarjetas, selectores, diálogo de color, calendario, teclado y botones con contadores. Incluye conversión de colores, formato monetario, validaciones de correo y teléfono y una protección para evitar dobles navegaciones hacia atrás.

Existen componentes o funciones alternativos y nombres de archivos que no coinciden con su contenido, como ResumenVentasViewModel.kt, que contiene un composable. La caché de carrito no es una persistencia en disco. Los datos de usuario y los identificadores técnicos no se traducen, solo sus etiquetas visibles.

Código de referencia  ui/theme, ui/components y utils

### Estados del mercadillo

| Código | Estado | Comportamiento principal |
| --- | --- | --- |
| 1 | Programado parcial | Creado sin saldo inicial |
| 2 | Programado total | Saldo inicial informado |
| 3 | En curso | Operaciones de ventas y gastos |
| 4 | Pendiente de arqueo | Cerrar y comprobar caja |
| 5 | Pendiente de asignar saldo | Decidir destino del efectivo |
| 6 | Cerrado completo | Consulta de arqueo y resultado |
| 7 | Cancelado | Consulta de la ficha sin edición |

### Datos que conserva la aplicación

| Tabla | Contenido | Propietario en el modelo |
| --- | --- | --- |
| usuarios | Cuenta, nombre, correo, foto y Premium | uid |
| configuracion | Idioma, tema, fuente, moneda y sesión local | Registro de configuración global |
| categorias | Nombre, color y orden | userId |
| articulos | Nombre, categoría, precio, coste, stock, favorito y fotoUri | userId |
| mercadillos | Fecha, lugar, logística, estado, saldos y totales | userId |
| recibos | Cabecera de venta, total, método y estado | idUsuario |
| lineas_venta | Productos o conceptos, cantidades y subtotales, vínculo de abono | idUsuario |
| LineasGastos | Conceptos, importes, método y descuento de caja | idUsuario |
| saldos_guardados | Origen y efectivo pendiente de asignación | idUsuario |
| empresa | Identificación y domicilio comercial | No incluye uid |

Los nombres y campos son los encontrados en las entidades y DAOs. El registro global de configuración debe distinguirse de los datos financieros vinculados a un usuario. La lista de tablas de una instalación antigua puede variar si sus migraciones no se han completado.

## Parte 2 Funciones incompletas y problemas encontrados

Esta parte separa opciones que ya existen parcialmente de defectos o riesgos de la implementación actual. Las evidencias son estáticas salvo los pequeños experimentos SQLite descritos. Ninguna propuesta de esta parte se aplica silenciosamente a la lógica financiera en la entrega 1.2.

### Funciones que están a medio implementar

#### Inventario

La ruta y la pantalla existen, pero el contenido central es pendiente_desarrollo y muy_pronto_gestionar_stock. Completar movimientos de stock, ajustes y consulta de existencias, reutilizando los campos del artículo.

Referencia  PantallaInventario.kt

#### Listados e informes

La pantalla anuncia desarrollo futuro y no contiene consultas o exportaciones completas. Definir informes por fechas, mercadillo y producto, y después exportación.

Referencia  PantallaListados.kt

#### Gastos automáticos

La pestaña solo muestra gastos_automaticos_proximamente. Definir plantillas, periodicidad, confirmación y prevención de duplicados.

Referencia  PestanaGastosAutomaticas.kt

#### Envío de recibos

Hay TipoEnvio, correo, teléfono, focos y callback onEnviar, pero faltan campos y acción completa de envío; el flujo visible finaliza la venta. Generar recibo, previsualizarlo y abrir un envío solicitado por el usuario.

Referencia  PantallaEnviarRecibo.kt y MainActivity.kt

#### Suscripción Premium

Se navega a suscripcion sin destino declarado. La promoción de Configuración no contiene un proceso completo de compra. Existen funciones locales para activar Premium, sin integración de facturación encontrada. Definir alta de plan y verificación fiable de derechos antes de publicitar una suscripción de pago.

Referencia  PantallaSplashDescarga.kt y Configuración

#### Fotos del catálogo

Se conserva fotoUri y se transmite como texto, pero no se encontró un ciclo completo de selección, carga y recuperación de imágenes. Decidir almacenamiento de imágenes y acceso en otros dispositivos.

Referencia  ArticuloEntity.kt y repositorios

#### Recuperación de saldos y empresa

Hay guardado local y algunas subidas, pero el RemoteBundle principal no incluye empresa ni saldos. Completar subida, descarga, consumo y borrado de ambas entidades.

Referencia  SaldoGuardadoRepository.kt, EmpresaRepository.kt y bootstrap

#### Sincronización manual de ventas

El método tiene una corrutina con un try vacío; no hace el trabajo que su nombre indica. Conectarlo a un servicio único con resultado y reintentos.

Referencia  VentasViewModel.sincronizarPendientes

#### Funciones heredadas de cuenta

Hay métodos Deprecated vacíos para preferencias antiguas; inviteUserToCompany devuelve false. Retirarlos o diseñar una función nueva explícita. Los comentarios indican que multiusuario se eliminó; no es un compromiso de producto futuro.

Referencia  UserRepository.kt y ConfiguracionRepository.kt

#### Persistencia del carrito

Guarda borradores en ConcurrentHashMap, solo mientras viva el proceso. Persistir en Room si se quiere recuperar tras cierre, reinicio o muerte del proceso.

Referencia  CarritCache.kt

### Hallazgos priorizados que requieren corrección

#### F01 Recibos con identificadores repetibles

Prioridad Alta. VentasRepository.guardarVenta y crearAbono construyen idRecibo como RC más HHmmss e idioma. RecibosDao inserta con REPLACE y ReciboEntity usa ese campo como clave primaria. Dos operaciones del mismo idioma en el mismo segundo, incluso en días o usuarios distintos, pueden compartir identificador.

Acción recomendada Usar UUID o un identificador globalmente único; mantener una numeración visible separada. Probar doble pulsación y dos dispositivos. No se ha constatado pérdida de recibos reales.

#### F02 Gastos con dos identificadores remotos incompatibles

Prioridad Alta. GastosRepository.pushUno escribe idMercadillo-numeroLinea; AuthRepository.pushLocalPendings usa solo numeroLinea. Además, su toMap no conserva todos los campos que consume el lector de gastos. El DAO marcarComoSincronizado también actualiza solo por numeroLinea.

Acción recomendada Unificar serializador y clave compuesta y actualizar por mercadillo y línea. Probar dos mercadillos con línea 0001 y comparar Room y Firestore.

#### F03 El recuento de mercadillos consulta categorías

Prioridad Alta. MercadilloDao.contarPorUsuario ejecuta SELECT COUNT(*) FROM categorias en lugar de mercadillos. AuthRepository utiliza este valor para decidir si debe descargar información.

Acción recomendada Corregir la tabla en una entrega específica y probar recuentos distintos entre categorías y mercadillos. El error de consulta se reproduce con SQLite.

#### F04 Separación insuficiente en empresa y configuración

Prioridad Alta. EmpresaRepository escribe empresa/datos sin propietario y ConfiguracionRepository escribe configuraciones/configuracion_global. EmpresaEntity no contiene uid.

Acción recomendada Definir si son datos globales intencionales o particulares y separar por uid cuando corresponda. Las reglas remotas no están en el material revisado, por lo que no puede afirmarse quién tiene acceso efectivo.

#### F05 Cadena de migraciones incompleta

Prioridad Alta. AppDatabase solo registra 16 a 17 y 17 a 18. Migrations.kt conserva migraciones antiguas no integradas, algunas anidadas dentro de otra. exportSchema está desactivado y se permite migración destructiva al bajar versión.

Acción recomendada Completar las rutas necesarias, exportar esquemas y probar actualizaciones sobre copias de bases antiguas antes de distribuir APK. No usar reinstalación como solución si se quieren conservar datos.

#### F06 Sincronización decidida por cantidades de registros

Prioridad Alta. handlePostLoginDataSync descarga cuando el recuento remoto supera al local. Igual número de registros no implica igual contenido: un cambio de precio, nombre, saldo o estado puede no generar descarga en ese camino.

Acción recomendada Comparar versiones o marcas de cambio y establecer resolución de conflictos. Probar cambios de un mismo registro en dos dispositivos sin variar el número de filas.

#### F07 Guardado de venta sin una transacción local integral

Prioridad Alta. VentasRepository inserta recibo y líneas en llamadas separadas. Después VentasViewModel cambia stock con operaciones independientes. Un fallo intermedio puede dejar parte de la operación guardada.

Acción recomendada Guardar cabecera, líneas y ajuste de stock en una transacción local; sincronizar mediante una cola. Probar fallos en cada paso y dobles confirmaciones.

#### F08 Claves de firma y credenciales en el historial y Gradle

Prioridad Alta. Se retiró keystore/release.jks de la versión actual, pero permanece en el historial anterior. app/build.gradle.kts conserva secretos de firma en texto y una ruta absoluta del equipo de desarrollo.

Acción recomendada Sacar secretos del código, cargar propiedades locales o variables del entorno y decidir tratamiento del historial y de la clave según el uso real de la app. No se reproducen aquí contraseñas ni bytes de la clave.

#### F09 Riesgo de recuperación incompleta de datos borrados

Prioridad Media alta. Varias descargas filtran por activo o activa y algunas eliminaciones son físicas y no mantienen una operación pendiente de borrado. Un dispositivo con datos antiguos puede conservar o restaurar filas que se eliminaron en otro.

Acción recomendada Usar tombstones o un registro de borrados sincronizable y probar borrado sin conexión seguido de restauración.

#### F10 Saldo consumido no sincronizado de forma fiable

Prioridad Media alta. SaldoGuardadoRepository.sincronizar recibe id pero lee el último saldo no consumido. Tras marcarConsumido, ese saldo ya no satisface el filtro. ReemplazarGuardado borra el anterior localmente, sin una retirada remota equivalente en ese camino.

Acción recomendada Consultar por id y sincronizar el estado consumido. Añadir restauración de saldos y reintentos de borrado.

#### F11 Stock dependiente de la pestaña al confirmar

Prioridad Media alta. VentasViewModel solo aplica descuento de stock si la pestaña activa es PRODUCTOS. Un ticket con productos y conceptos manuales puede finalizarse desde MANUAL.

Acción recomendada Procesar las líneas PRODUCTO de todo ticket, independientemente de la pestaña. Probar ticket mixto, cambio de pestaña y abono.

#### F12 Comprobación del invitado con efecto destructivo

Prioridad Media alta. AuthRepository.shouldShowDefaultUserWarning consulta datos remotos de usuario_default y puede llamar deactivateAllUserData, marcando datos como inactivos. Es más que una comprobación de aviso.

Acción recomendada Separar consulta de aviso y limpieza, definir identificador de invitado por instalación y comprobar que no afecta a datos ajenos. La posibilidad de escritura depende también de las reglas remotas no revisadas.

#### F13 Fechas almacenadas como texto no ordenable cronológicamente

Prioridad Media. Se usan cadenas dd-MM-yyyy en comparaciones SQL, MAX u ORDER BY y en algunas ordenaciones Kotlin por it.fecha. El orden alfabético puede poner diciembre antes de enero o el día 02 de otro mes antes del día 25 actual.

Acción recomendada Persistir epoch o fecha ISO, o convertir antes de ordenar en todas las consultas relevantes. Probar cruce de mes y de año.

#### F14 Dos caminos de abono con reglas distintas

Prioridad Media. La interfaz de resumen inserta una línea negativa en el recibo original; VentasRepository.crearAbono crea un recibo nuevo con método ABONO. Los cálculos por método se apoyan en el método de la cabecera.

Acción recomendada Elegir un único servicio de abono, conservar el método de devolución y verificar stock, totales, sincronización e idempotencia.

#### F15 Rutas y mensajes que anuncian acciones no completadas

Prioridad Media. La pantalla de recuperación Free ofrece suscripcion sin ruta declarada. Su botón de borrar crea un UserRepository pero no realiza la eliminación en el bloque revisado, aunque el texto la anuncia.

Acción recomendada No anunciar borrado o compra hasta que el flujo esté implementado y probado. En 1.2 solo se traduce el texto existente, sin cambiar el comportamiento.

#### F16 Anuncios con BuildConfig de una dependencia

Prioridad Media. AdsConsentManager y AdsInterstitialController importan com.google.firebase.BuildConfig. El modo de prueba debe corresponder a la variante de Market Sales.

Acción recomendada Usar el BuildConfig de la app y verificar anuncios de prueba y reales sin generar impresiones de producción durante desarrollo.

#### F17 Compatibilidad Android y tareas de fondo

Prioridad Media. minSdk es 24, pero hay uso de java.time y funciones anotadas RequiresApi O. Una anotación no sustituye un control de versión ni una configuración de compatibilidad. WorkManager no ofrece puntualidad exacta.

Acción recomendada Elegir mínimo compatible o desugaring y probar API 24 25 y 26. Probar reapertura tras medianoche y a las cinco con la app cerrada.

#### F18 Errores ocultos y falso éxito parcial

Prioridad Media. Hay catch vacíos, repositorios que devuelven false y funciones que continúan pese a una sincronización parcial. AuthViewModel.updateUserProfile puede devolver éxito aunque un repositorio inferior haya capturado su error.

Acción recomendada Separar éxito local, pendiente de nube y fallo; registrar el detalle técnico y dar un mensaje claro al usuario.

#### F19 Gestión de recursos y cuentas

Prioridad Media. Varios repositorios crean CoroutineScope y observadores de red propios sin un cierre explícito. AuthRepository registra un listener de autenticación por instancia. ArticuloRepository y CategoriaRepository capturan el usuario al crear ciertos flujos.

Acción recomendada Centralizar repositorios, cancelar scopes y listeners según ciclo de vida y probar cambio de cuenta sin reiniciar la app.

#### F20 Precisión monetaria y formatos

Prioridad Media. Importes financieros usan Double y distintos formateadores, varios con coma o Locale español fijo. El idioma y el símbolo no siempre se gestionan por separado.

Acción recomendada Usar céntimos enteros o decimal controlado y un formato coherente. Probar importes límite, redondeo, moneda y cambio de idioma.

No se propone resolver todos estos hallazgos en un único cambio: identificadores, migraciones y sincronización requieren pruebas de recuperación y datos de muestra controlados. La revisión indica dónde intervenir; no certifica que los escenarios de riesgo hayan ocurrido ya en instalaciones reales.

## Parte 3 Mejoras nuevas y orden de trabajo

Las siguientes propuestas no se consideran implementadas por el mero hecho de existir un campo o un texto en el código. Son decisiones de producto para futuras entregas, separadas de los defectos descritos y de los módulos parciales.

#### Copias verificables y recuperación

Exportar una copia cifrada o protegida con control de versión y fecha, verificar su integridad y ofrecer una restauración de prueba. Mostrar qué entidades se incluyen y qué datos aún no han subido.

#### Historial de movimientos y cambios

Registrar quién creó o modificó una venta, gasto, saldo o mercadillo, cuándo y por qué. Mantener trazabilidad de abonos y ajustes, evitando editar operaciones financieras sin rastro.

#### Panel de sincronización

Mostrar por entidad registros pendientes, último envío correcto y errores recuperables. Ofrecer reintentar y un diagnóstico que no incluya contraseñas ni datos sensibles innecesarios.

#### Estadísticas útiles para mercadillos

Comparar ventas, gastos, resultado, ticket medio, mejores productos y evolución por evento y fecha. Separar beneficio estimado de flujo de caja y explicar el tratamiento del coste.

#### Inventario por movimientos

Definir entradas, ventas, devoluciones, mermas y ajustes con fecha y motivo. Añadir avisos de existencias bajas y evitar stock negativo según una política explícita.

#### Importación y exportación

Añadir CSV o Excel para artículos y listados con previsualización, validación y reversión. No se detectó un flujo completo existente de importación o exportación para el usuario.

#### Recibos completos

Diseñar una plantilla con empresa, número visible, líneas, métodos, totales y devoluciones. Exportar o compartir a petición del usuario. Decidir aparte los requisitos de facturación que sean aplicables al uso comercial.

#### Gestión segura de cuenta

Recuperar contraseña, verificar correo, diferenciar cuentas Google y correo y ofrecer un proceso claro de eliminación o exportación de cuenta. Verificar derechos Premium en un servicio fiable si se comercializa.

#### Accesibilidad y uso con pantalla grande

Revisar TalkBack, contraste, tamaños de texto y objetivos táctiles. El manifiesto fija orientación vertical; diseñar tablet o paisaje solo si aporta valor al puesto de venta.

#### Automatización probada y recordatorios

Añadir recordatorios de mercadillo y pendientes de arqueo con permisos y horarios configurables. Definir un único motor de estados con pruebas de cambio de fecha, zona horaria y horario de verano.

#### Borradores persistentes y protección de cobro

Recuperar tickets tras cierre de proceso, impedir dobles confirmaciones y mostrar claramente venta registrada o pendiente. Mantener el carrito separado por cuenta y mercadillo.

#### Calidad técnica continua

Compilar y probar cada cambio en CI, ejecutar pruebas Room, traducciones, migraciones y cálculos de caja. Guardar esquemas de base de datos y generar APK de prueba con secretos fuera del repositorio.

### Secuencia recomendada de entregas

| Orden | Objetivo | Criterio para darlo por terminado |
| --- | --- | --- |
| 1 | Integridad de datos | Recibos únicos, claves de gasto coherentes y pruebas de venta abono y caja |
| 2 | Recuperación y separación de cuentas | Copia completa, migraciones probadas y datos privados por usuario |
| 3 | Sincronización fiable | Altas cambios borrados y saldos iguales en dos dispositivos |
| 4 | Funciones parciales | Terminar una por entrega: recibos, inventario o listados |
| 5 | Funciones nuevas | Elegir según utilidad real en mercadillo y esfuerzo de mantenimiento |

Cada entrega pequeña aumenta la versión menor. Un salto a 2.0 se reserva para una ampliación o cambio grande acordado. No se propone saltar a 2.0 solo por documentar problemas o por acumular correcciones pequeñas.

## Traducciones corregidas en la versión 1.2

Antes de esta entrega, SpanishStrings contenía 537 claves y EnglishStrings 534 en el análisis de ramas literales. Ahora ambos contienen 598 claves, sin duplicados ni valores vacíos y con los mismos parámetros de sustitución. Esto supone 61 claves añadidas en español y 64 en inglés, además de correcciones de llamadas y de dos avisos que estaban en inglés dentro del diccionario español.

Se completan los textos que faltaban en Utilidades y Perfil, datos de empresa, elección de método de pago, creación de categoría, sí y no y formato de hora. Se corrigen los argumentos de idioma de MenuLateral, etiquetas de contraseña, carrito y retorno, mensajes propios de inicio de sesión, progreso de sincronización y pantalla de recuperación. Las descripciones visibles de estados usan el idioma seleccionado; los códigos guardados en la base no cambian.

No se traducen nombres, lugares, descripciones de producto ni otros datos escritos por el usuario. Tampoco se modifican claves técnicas de Firestore, rutas o métodos de pago almacenados. Los mensajes técnicos que devuelven Firebase o Android pueden seguir en el idioma que proporcione el SDK; una futura capa de errores por código permitiría traducirlos sin mostrar detalles internos.

Se añade scripts/check_translations.py para verificar pares de claves, duplicados, cadenas vacías, parámetros y argumentos literales de idioma. La ejecución local ha comprobado 654 llamadas con clave literal. No garantiza por sí sola todas las claves construidas dinámicamente, la naturalidad de cada frase ni el ajuste visual de textos largos; esos casos necesitan pruebas de interfaz.

## Validación y pruebas pendientes

| Comprobación | Resultado |
| --- | --- |
| Análisis de ambos diccionarios | 598 claves por idioma y sin diferencias de cobertura |
| Parámetros como {0} y {version} | Mismos parámetros en ambos idiomas |
| Llamadas literales a traducciones | 654 comprobadas sin clave faltante |
| Formato del cambio | git diff --check sin errores |
| Pruebas unitarias de traducciones | Añadidas; pendientes de ejecución en Android Studio |
| Compilación y pruebas Android | No ejecutadas por bloqueo de descarga de Gradle |
| Datos reales y configuración Firebase | No accedidos ni modificados |
| Corrección de cancelados anterior | Validada por el usuario |

Prueba manual de aceptación: actualizar con Ctrl T, sincronizar y compilar. Con una cuenta que pueda cambiar idioma, recorrer Utilidades, Perfil, Empresa, método de pago, calendario y acceso primero en español y luego en inglés. Confirmar que no aparecen claves como datos_empresa ni opciones_de_privacidad. Probar errores y estados de carga y comprobar que los textos no se recortan.

Ejecutar TranslationsTest y las pruebas Room en Android Studio. Para los hallazgos de datos, usar copias y cuentas de prueba: dos mercadillos con el mismo número de línea de gasto; dos recibos con la misma hora; distintos recuentos de categorías y mercadillos; cambios remotos sin nuevas filas; venta mixta de artículos y conceptos; abono de artículo con stock; cierre Free y Premium; reinstalación con copia y cambio de usuario.

## Anexo Inventario de módulos y estructura de datos

Se relacionan todos los archivos Kotlin de producción de la base revisada. La huella SHA 256 abreviada identifica sus bytes en la versión 1.1; no es una medida de cobertura de pruebas. En los archivos modificados para 1.2 la huella posterior cambia. Los nombres de función describen superficies de código, no garantizan que estén accesibles desde la navegación.

### Inicio

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| MainActivity.kt | onCreate, NavigationSystem, PantallaVentasWrapper, PantallaGastosWrapper, tryGetBackStackEntrySafe, pingFirestoreProd | 54f61b0fca |

### ads

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| AdsBottomBar.kt | AdsBottomBar | d6a1aee02f |
| AdsConsentManager.kt | init, requestConsentAndShowFormIfRequired, showPrivacyOptions, refresh, reset, loadAndMaybeShowForm, buildRequestParameters | dec446b70d |
| AdsInterstitialController.kt | adUnitId, preload, onAdLoaded, onAdFailedToLoad, maybeShow, dropPreloaded, attachCallbacks, onAdShowedFullScreenContent, onAdDismissedFullScreenContent y 5 funciones adicionales | 7cde4d3420 |
| BannerAd.kt | BannerAdBottom | b690f559c6 |

### data  local  cache

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| CarritCache.kt | save, load, clear | 1db3544a99 |

### data  local  dao

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ArticulosDao.kt | upsertAll, borrarPorUsuario, contarPorUsuario, upsert, existeAlgunoDeUsuario, insertArticulo, insertOrUpdate, updateArticulo, deleteArticulo y 14 funciones adicionales | 81b91d8b15 |
| CategoriaDao.kt | upsert, existeAlgunoDeUsuario, insertCategoria, insertOrUpdate, updateCategoria, deleteCategoria, upsertAll, borrarPorUsuario, contarPorUsuario y 12 funciones adicionales | f3620b2455 |
| ConfiguracionDao.kt | existeConfiguracion, getConfiguracion, getConfiguracionSync, getConfiguracionFlow, insertarConfiguracion, insertOrUpdate, actualizarConfiguracion, eliminarTodasLasConfiguraciones, updateMoneda y 19 funciones adicionales | 38d421a600 |
| EmpresaDao.kt | getEmpresa, getEmpresaFlow, insertOrUpdate, updateSincronizado, getEmpresaPendiente | cee01eb3a1 |
| LineasGastosDao.kt | upsertAll, insertarLinea, insertarLineas, borrarPorMercadillo, borrarPorUsuario, observarGastosPorMercadillo, getTotalGastosMercadillo, getTotalGastosPorMetodo, getTotalGastosEfectivoDeCaja y 7 funciones adicionales | 26abbcc463 |
| LineasVentaDao.kt | upsertAll, contarPorUsuario, borrarPorUsuario, observarLineasPorRecibo, observarLineasPorMercadillo, upsert, existeAlgunoDeUsuario, obtenerLineasPorRecibo, obtenerLineasPorMercadillo y 12 funciones adicionales | 89630466ac |
| MercadilloDao.kt | upsertAll, borrarPorUsuario, upsert, existeAlgunoDeUsuario, insertMercadillo, insertOrUpdate, updateMercadillo, deleteMercadillo, desactivarMercadillo y 38 funciones adicionales | 343ec25ccd |
| RecibosDao.kt | upsertAll, borrarPorUsuario, contarPorUsuario, upsert, existeAlgunoDeUsuario, obtenerRecibosPorMercadillo, obtenerReciboPorId, insertarRecibo, actualizarRecibo y 7 funciones adicionales | 45aac498c8 |
| SaldoGuardadoDao.kt | upsert, upsertAll, insert, update, delete, deleteById, borrarPorUsuario, getAllByUser, getUltimoNoConsumido y 5 funciones adicionales | aed4884a38 |
| UserDao.kt | existeUsuario, isPremium, getUserById, getUserByIdSync, getUserByIdFlow, insertUser, insertOrUpdate, updateUser, deleteUser y 16 funciones adicionales | 8247029e1c |

### data  local  database

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| AppDatabase.kt | configuracionDao, mercadilloDao, userDao, categoriaDao, articuloDao, recibosDao, lineasVentaDao, lineasGastosDao, saldoGuardadoDao y 5 funciones adicionales | 1670e89eef |
| Migrations.kt | migrate | 37690f93e8 |

### data  local  entity

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ArticuloEntity.kt | Modelo y campos idArticulo, userId, nombre, idCategoria, precioVenta, precioCoste, stock, controlarStock | 5c3e715c23 |
| CategoriaEntity.kt | Modelo y campos idCategoria, userId, nombre, colorHex, orden, activa, version, lastModified | db3dae195f |
| ConfiguracionEntity.kt | Modelo y campos id, moneda, idioma, fuente, temaOscuro, usuarioLogueado, numeroVersion, ultimoDispositivo | 9f3d0bf70b |
| EmpresaEntity.kt | Modelo y campos id, nif, nombre, razonSocial, direccion, poblacion, codigoPostal, provincia | 53e07051b1 |
| LineaGastoEntity.kt | Modelo y campos idMercadillo, idUsuario, numeroLinea, descripcion, importe, fechaHora, formaPago, descontarDeCaja | 786bba0865 |
| LineasVentasEntity.kt | Modelo y campos idLinea, idRecibo, idMercadillo, idUsuario, numeroLinea, tipoLinea, descripcion, idProducto | f10b4bd8e7 |
| MercadilloEntity.kt | Modelo y campos idMercadillo, userId, fecha, lugar, organizador, esGratis, importeSuscripcion, requiereMesa | 9107906d92 |
| ReciboEntity.kt | Modelo y campos idRecibo, idMercadillo, idUsuario, fechaHora, metodoPago, totalTicket, estado, version | 68ba308aa7 |
| SaldoGuardadoEntity.kt | Modelo y campos idRegistro, idUsuario, idMercadilloOrigen, fechaMercadillo, lugarMercadillo, organizadorMercadillo, horaInicioMercadillo, saldoInicialGuardado | 14183412cd |
| UserEntity.kt | Modelo y campos uid, email, displayName, photoUrl, esPremium, version, lastModified, fechaCreacion | 7049c43b55 |

### data  repository

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ArticuloRepository.kt | getArticulosUsuarioActual, getArticulosByCategoria, crearArticulo, actualizarArticulo, eliminarArticulo, getArticuloById, getHybridArticulos, sincronizarArticuloConFirebase, eliminarArticuloDeFirebase y 5 funciones adicionales | 448c6cc380 |
| AuthRepository.kt | text, setProgress, colCategorias, colArticulos, colMercadillos, colRecibos, colLineasVenta, colLineasGastos, testConnection y 26 funciones adicionales | 8c0d30251a |
| CategoriaRepository.kt | getCategoriasUsuarioActual, crearCategoria, actualizarCategoria, eliminarCategoria, getCategoriaById, getHybridCategorias, sincronizarCategoriaConFirebase, eliminarCategoriaDeFirebase, sincronizarCategoriasNoSincronizadas y 3 funciones adicionales | 99646c694b |
| ConfiguracionRepository.kt | getConfiguracion, crearConfiguracionPorDefecto, updateIdioma, updateTemaOscuro, updateFuente, updateMoneda, setUsuarioLogueado, getUsuarioLogueado, isUsuarioDefault y 11 funciones adicionales | 12c0965a50 |
| EmpresaRepository.kt | getEmpresaFlow, guardarOActualizar, sincronizarConFirebase, sincronizarPendiente | 16b381d0cb |
| GastosRepository.kt | observarGastos, guardarGasto, pushUno, pushPendientes | ca4b6b0e64 |
| MercadilloRepository.kt | getMercadillosUsuarioActual, getMercadillosPorMes, crearMercadillo, actualizarMercadillo, eliminarMercadillo, desactivarMercadillo, getMercadilloById, asignarSaldoInicial, asignarSaldoInicialAutomatico y 22 funciones adicionales | 1d328cd21e |
| SaldoGuardadoRepository.kt | getUltimoNoConsumido, reemplazarGuardado, marcarConsumido, borrarGuardado, sincronizar | c4f2ae098d |
| UserRepository.kt | getOrCreateUser, getUserById, updateUserPremium, updateUserEmail, updateUserDisplayName, updateUserPhotoUrl, getUserByEmail, getAllPremiumUsers, getAllFreeUsers y 18 funciones adicionales | 136a43ec47 |
| VentasRepository.kt | guardarVenta, nextSeq, obtenerVentasMercadillo, obtenerLineasVenta, crearAbono, sincronizarPendientes | 92ae4e0c13 |

### ui  components

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| BarraAccionesVenta.kt | BarraAccionesVenta | 00faf11212 |
| BotonCarritoVenta.kt | BotonCarritoVentas | 5ee1d34a49 |
| BottomBarMercadillo.kt | BottomBarMercadillo, BottomBarButton | 5afa851266 |
| DialogSelectoColor.kt | DialogSelectorColor | 556ed06c4a |
| DialogoSeleccionMercadilloActivo.kt | DialogoSeleccionMercadilloActivo | 956fc9213f |
| DownloadProgressBar.kt | DownloadProgressBar | 5c8b61fafa |
| IconoCarritoPersonalizado.kt | IconoCarritoGrande | d1fa369bbb |
| MenuHamburguesa.kt | MenuHamburguesa, MenuOption | 90574d4460 |
| MenuLateral.kt | MenuLateral, MenuLateralItem | f68b704fc5 |

### ui  components  calendario

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| CalendarioGrid.kt | CalendarioGrid, DiaCalendario | 0a140f7ebd |

### ui  components  dialogs

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| DialogoSeleccionMercadillo.kt | DialogoSeleccionMercadillo | 75521ef3d9 |

### ui  components  gastos

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| BarraAccionesGasto.kt | BarraAccionesGasto | 40d0f1daf8 |

### ui  components  leyenda

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| LeyendaEstadosDialog.kt | LeyendaColoresDialog | 42cb7a8ab6 |

### ui  components  mercadillos

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ProximosMercadillosSection.kt | ProximosMercadillosSection, navegarSegunEstado | 9c3a7725a2 |

### ui  components  proximos

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| CardMercadillosProximos.kt | CardMercadillosProximos, MercadilloProximoItem | b1bd166398 |

### ui  components  ventas

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| BarraAccionesVenta.kt | BarraAccionesVenta, BotonCarritoVentas | c823f07924 |

### ui  composables

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| CampoDescripcion.kt | CampoDescripcion | 31cf16f336 |
| CampoImporte.kt | CampoImporte | b30e72c4df |
| PestanaVentaManual.kt | PestanaVentaManual | fe40950b1b |
| PestanaVentaProductos.kt | PestanaVentaProductos, CategoriaChip, ProductoCardCuadrada, parseHexColor | 25acb2537d |
| TecladoNumerico.kt | TecladoNumerico, Numero, TecladoNumericoPago | 85fb1baba0 |

### ui  composables  gastos

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| PestanaGastosAutomaticas.kt | PestanaGastosAutomaticas | 4398254b1a |
| PestanaGastosManual.kt | PestanaGastosManual | 9a03f49d67 |

### ui  composables  resumen

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| PestanaResumenGastos.kt | PestanaResumenGastos, GastoResumenRow, MetodoPagoIcon | b7bf79580f |
| PestanaResumenVentas.kt | PestanaResumenVentas, LineaResumenCard, AbonoResumenCard, MetodoPagoIcon, siguienteIdLinea | 84a4057ecf |

### ui  navigation

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| NavGraphArqueo.kt | addArqueoGraph | 5025a33ded |

### ui  pantallas

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| LoginSCreen.kt | LoginScreen | fa8c3aad2d |
| PantallSplash.kt | PantallaSplash | 11818d6693 |
| PantallVentasCarrito.kt | PantallaVentasCarrito | 6bea18ae89 |
| PantallaAltaMercadillos.kt | PantallaAltaMercadillo, parseDoubleFlexible, guardar | 83170e7f72 |
| PantallaArticulos.kt | PantallaArticulos, limpiarFormulario, ArticuloCard | 68ef455237 |
| PantallaCambio.kt | PantallaCambio, parseDouble, TextoImporteGrande | 6055b022a0 |
| PantallaCategorias.kt | PantallaCategorias, CategoriaCard, ColorPickerDialog | 97fe82d2f2 |
| PantallaConfiguracion.kt | PantallaConfiguracion, OpcionConfiguracionV10, TarjetaInformacionCuenta, TarjetaPromocionPremium | 86cc90bada |
| PantallaDatosEmpresa.kt | PantallaDatosEmpresa | ab63966ee3 |
| PantallaDebugDatos.kt | PantallaDebugDatos, cambiarActivo | 32fbf62dae |
| PantallaEnviarRecibo.kt | PantallaEnviarRecibo | ecbee74a3c |
| PantallaInventario.kt | PantallaInventario | fabfc71941 |
| PantallaListados.kt | PantallaListados | d6cef20a3a |
| PantallaLogin.kt | PantallaLogin, handleAuthAction, handleGoogleSignIn | f24e49dbbd |
| PantallaLogoutSplash.kt | PantallaLogoutSplash | 1e52b2135b |
| PantallaMercadillos.kt | PantallaMercadillos, languageToLocale | f0aa722894 |
| PantallaMetodoPago.kt | PantallaMetodoPago, MetodoPagoButton | 980d914b5d |
| PantallaPerfil.kt | PantallaPerfil, updateBasicInfo, updatePassword, BotonOpcionesPrivacidad | 7fbd99c079 |
| PantallaResumenMercadillo.kt | PantallaResumenVentas | c50fc767c8 |
| PantallaSaldosPendientes.kt | PantallaSaldosPendientes, ReadOnlyItem | fb4fa83116 |
| PantallaSplashDescarga.kt | PantallaSplashDescarga | 9d46fb381d |
| PantallaUtilidades.kt | PantallaUtilidades, OpcionUtilidad | 5256c8d648 |
| PantallaVentas.kt | PantallaVentas | 7a0d625e95 |

### ui  pantallas  arqueo

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| PantallaArqueo.kt | PantallaArqueo, InfoRow, MetricCard, EstadoChip | 39ae373fcb |
| PantallaArqueoCaja.kt | PantallaArqueoCaja, KeyValueLine, sanitizeImporteInput, parseImporte | 496cb4fa7b |
| PantallaAsignarSaldo.kt | PantallaAsignarSaldo, IntermediaContenido, recordarImporteDesdeDigits | 689629146d |
| PantallaResultadoMercadillo.kt | PantallaResultadoMercadillo, KeyValueLine | 75a9edf46c |

### ui  pantallas  gastos

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| PantallaGastos.kt | PantallaGastos, fechaCorta | c3082de954 |
| PantallaGastosCarrito.kt | PantallaGastosCarrito, BottomSummaryBar | 018f6eb86a |

### ui  theme

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| Color.kt | Paleta formas o constantes compartidas | c3d7b64607 |
| LocalConfiguration.kt | getConfigurationManager | 17cf457901 |
| Shape.kt | Paleta formas o constantes compartidas | be01785f97 |
| Theme.kt | MarketSalesTheme, getTypographyForFont | 0b3084752c |
| Type.kt | createTypography | edae95a7a0 |

### ui  viewmodel

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ArqueoViewModel.kt | cargar, confirmarArqueoCaja, fmtMoneda, create | 109826d94c |
| ArticuloViewModel.kt | crearArticulo, actualizarArticulo, eliminarArticulo, getArticuloById, getArticulosByCategoria, searchArticulos, forzarSincronizacion, limpiarMensajes, limpiarError y 5 funciones adicionales | c80c5a69d5 |
| ArtriculoViewModelFactory.kt | create | ce6fda4033 |
| AsignarSaldoViewModel.kt | cargarDatos, safeParseDate, saldoFinalActual, saldoFinalActualFmt, saldoFinalAjustado, saldoFinalAjustadoFmt, fmtMoneda, importeActual, abrirIntermediaGuardar y 12 funciones adicionales | 595b7d023d |
| AuthViewModel.kt | testAuthConnection, registerWithEmail, loginWithEmail, signInWithGoogle, logout, isUserAuthenticated, clearAuthError, updateUserProfile, updatePassword y 2 funciones adicionales | c37f124515 |
| AuthViewModelFactory.kt | create | a7d300b0e9 |
| CategoriaViewModel.kt | tr, crearCategoria, actualizarCategoria, eliminarCategoria, getCategoriaById, forzarSincronizacion, limpiarMensajes, limpiarError, limpiarMensaje y 2 funciones adicionales | 216818e93f |
| CategoriaViewModelFactory.kt | create | 9400f89336 |
| ConfiguracionViewModel.kt | actualizarPassword, actualizarTema, actualizarFuente, actualizarIdioma, actualizarMoneda, actualizarVersion, actualizarEmail, sincronizar | 7a6319cd76 |
| ConfiguracionViewModelFactory.kt | create | 407d7d00e4 |
| EmpresaViewModel.kt | guardarOActualizar | 48e95b1175 |
| EmpresaViewModelFactory.kt | create | 7a5c420aa9 |
| GastosViewModel.kt | configurarDescontarDeCajaParaEfectivo, actualizarDescripcion, onDigitoPresionado, onDobleDecimalPresionado, onBorrarDigito, renderImporte, obtenerImporteComoDouble, añadirGastoManual, editarDescripcionLinea y 5 funciones adicionales | ae738018de |
| GastosViewModelFactory.kt | create | 42ec3c6641 |
| MercadilloViewModel.kt | tr, tr1, crearMercadillo, cargarMercadillo, actualizarMercadillo, confirmarCambioSaldoEnCurso, limpiarMercadilloParaEditar, borrarMercadillo, seleccionarMercadilloActivo y 32 funciones adicionales | 7d5aa55d35 |
| MercadilloViewModelFactory.kt | create | 4a5649d14a |
| ProximosMercadillosViewModel.kt | tr, tr1, cargar, create | 97e4c1e941 |
| ResumenVentasViewModel.kt | LineaVentaResumenCard, MetodoPagoIcon | 3b753c3e76 |
| SaldoPendienteViewModel.kt | create | eb60eaa42e |
| VentasViewModel.kt | t, inicializar, persistirSiProcede, actualizarState, cambiarPestana, onDigitoPresionado, onDobleDecimalPresionado, onBorrarDigito, calcularNuevoImporteTPV y 16 funciones adicionales | d8942b7f5f |
| VentasViewModelFactory.kt | create | 47e1a413d6 |

### utils

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| ActivityFinder.kt | findActivity | adc827a877 |
| ColorUtils.kt | generarColorAleatorioPastel, toHexString, hexStringToColor, isValidHexColor, getContrastColor, obtenerAleatorio | 530730316b |
| ConfigurationManager.kt | togglePremiumForDevelopment, setPremiumForDevelopment, markPremiumUnknown, markPremiumKnown, updateUserConfigurationProtected, getCurrentUserId, getIdioma, getFuente, getModoOscuro y 29 funciones adicionales | 4dd1002c1d |
| ConnectivityObserver.kt | onAvailable, onLost, isCurrentlyConnected | 6928a00416 |
| DefaultBootstrapper.kt | runIfNeeded | 11f8390d6a |
| EnglishStrings.kt | get | a056a29e31 |
| EstadosMercadillo.kt | fromCodigo, getEstadoInicial, obtenerDescripcionKey, obtenerDescripcion, obtenerColor, obtenerColorTexto, obtenerTodosLosEstados, obtenerIcono, requiereAtencion y 4 funciones adicionales | 21e17ccbe6 |
| FooterMarca.kt | FooterMarca | 474770f203 |
| FullBootstrapper.kt | runIfNeeded | 848a095acb |
| GoogleAuthHelper.kt | getGoogleSignInClient, getLastSignedInAccount, signOut, revokeAccess | 2e15a010fc |
| MercadillosFilters.kt | soloDelUsuario | 7f04386440 |
| MercadillosManager.kt | navegarMesAnterior, navegarMesSiguiente, moveMonth, refreshNow, diaDelMes, truncToDay, safeParse, syncSuave, launchInIO | 9450c9d467 |
| MercadillosValidator.kt | puedeCrearConFecha, parseLocalDate, tryParse | 8f30aa71f6 |
| MonedaUtils.kt | obtenerSimboloMoneda, formatearImporte | f6bd3a833e |
| NavControllerExtensions.kt | safePopBackStack | 2af3bc4de5 |
| SpanishStrings.kt | get | 3dfad32422 |
| StringResourceManager.kt | getString | d75558f14b |
| ValidationUtils.kt | esEmailValido, esNumeroWhatsappValido, prefijoPorDefecto | f8b2445711 |

### utils  ui  alta

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| AltaMercadilloComponents.kt | CampoEstadoDebug, t, CampoFecha, CampoTexto, ConfiguracionEconomica, ConfiguracionLogistica, ConfiguracionHorarios, CampoSaldoInicial, DatePickerDialog y 6 funciones adicionales | e2134bc201 |

### work

| Archivo | Funciones o contenido | SHA 256 |
| --- | --- | --- |
| AutoEstadoScheduler.kt | scheduleDaily, cancelDaily, runOnceNow, periodicAt | 7ed1ebee9d |
| AutoEstadoWorker.kt | doWork | 961844cd94 |

### Pruebas y recursos complementarios

Pruebas de partida: ExampleUnitTest.kt, ExampleInstrumentedTest.kt y data/MercadilloCanceladoTest.kt. En 1.2 se añade utils/TranslationsTest.kt y el comprobador Python de traducciones. Los dos primeros tests son ejemplos mínimos y no constituyen una batería funcional de ventas o migraciones.

También se inspeccionaron AndroidManifest.xml, build.gradle.kts raíz y de app, settings.gradle.kts, gradle.properties, gradle/libs.versions.toml, el wrapper de Gradle, strings.xml, recursos de anuncios de debug y release y reglas XML de copia y FileProvider. Los iconos, fuentes e imágenes son recursos, no módulos con lógica propia. No se compilaron ni verificaron visualmente todas sus variantes.

Fuentes de la revisión: https://github.com/NuskySoft/Market_Sales_Good/tree/7e1d66fb186d902d66dca1521bd753993ed91e52 y cambios de la entrega 1.2 en su rama de revisión. Las evidencias se localizan por el archivo y la función indicados en cada hallazgo.
