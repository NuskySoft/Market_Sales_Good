package es.nuskysoftware.marketsales.utils

internal object SpanishStrings {

    // ui/components/BarraAccionesVenta.kt
    // ui/components/BottomBarMercadillo.kt
    // ui/components/DialogoSeleccionMercadilloActivo.kt
    // ui/components/DialogSelectorColor.kt
    // (y el resto de módulos/pantallas que ya tenías)

    fun get(key: String): String = when (key) {
        // Pantalla principal
        "app_name" -> "Market Sales"
        "mercadillos" -> "Mercadillos"
        "configuracion" -> "Configuración"
        "articulos" -> "Artículos"
        "categorias" -> "Categorías"
        "inventario" -> "Inventario"
        "listados" -> "Listados"
        "iniciar_sesion" -> "Iniciar Sesión"
        "cerrar_sesion" -> "Cerrar Sesión"
        "salir" -> "Salir"
        "perfil" -> "Perfil"

        // Configuración
        "informacion_cuenta" -> "Información de Cuenta"
        "cambiar_contrasena" -> "Cambiar Contraseña"
        "tema" -> "Tema"
        "fuente" -> "Fuente"
        "idioma" -> "Idioma"
        "moneda" -> "Moneda"
        "version_premium" -> "PREMIUM"
        "version_free" -> "FREE"
        "promocion_premium" -> "¡Actualiza a Premium!"
        "desbloquea_funciones" -> "Desbloquea todas las funciones"

        // Tema
        "tema_claro" -> "Claro"
        "tema_oscuro" -> "Oscuro"

        // Idiomas
        "espanol" -> "Español"
        "ingles" -> "English"

        // Fuentes
        "montserrat" -> "Montserrat"
        "poppins" -> "Poppins"
        "roboto" -> "Roboto"

        // Monedas
        "euro" -> "€ Euro"
        "dolar" -> "$ Dólar"
        "libra" -> "£ Libra"
        "yen" -> "¥ Yen"

        // ✅ PANTALLA PERFIL - STRINGS COMPLETOS
        "modificar_informacion" -> "Modifica tu información personal"
        "informacion_personal" -> "Información Personal"
        "nombre_completo" -> "Nombre completo"
        "correo_electronico" -> "Correo electrónico"
        "actualizar_informacion" -> "Actualizar Información"
        "actualizando" -> "Actualizando..."
        "cambiar_contrasena_titulo" -> "Cambiar Contraseña"
        "cambiar_contrasena_descripcion" -> "Deja en blanco si no quieres cambiar la contraseña"
        "contrasena_actual" -> "Contraseña actual"
        "nueva_contrasena" -> "Nueva contraseña"
        "confirmar_nueva_contrasena" -> "Confirmar nueva contraseña"
        "cambiando" -> "Cambiando..."
        "cambiar_contrasena_btn" -> "Cambiar Contraseña"

        // Validaciones
        "nombre_vacio" -> "El nombre no puede estar vacío"
        "email_invalido" -> "Email inválido"
        "contrasena_actual_requerida" -> "Contraseña actual requerida"
        "contrasena_minimo_caracteres" -> "La contraseña debe tener al menos 6 caracteres"
        "contrasenas_no_coinciden" -> "Las contraseñas no coinciden"

        // Mensajes de éxito/error
        "informacion_actualizada" -> "✅ Información actualizada correctamente"
        "contrasena_actualizada" -> "✅ Contraseña actualizada correctamente"
        "error_actualizar_informacion" -> "❌ Error al actualizar la información"
        "error_cambiar_contrasena" -> "❌ Error al cambiar la contraseña"
        "error_generico" -> "❌ Error: {0}"

        // Botones y acciones
        "volver" -> "Volver"
        "ocultar_contrasena" -> "Ocultar contraseña"
        "mostrar_contrasena" -> "Mostrar contraseña"

        // Diálogos
        "cambiar" -> "Cambiar"
        "cancelar" -> "Cancelar"
        "guardar" -> "Guardar"
        "aceptar" -> "Aceptar"
        "eliminar" -> "Eliminar"
        "contrasenas_no_coinciden_dialogo" -> "Las contraseñas no coinciden"
        "contrasena_muy_corta" -> "La contraseña debe tener al menos 6 caracteres"

        // Mensajes
        "funcion_premium" -> "Esta función está disponible solo en la versión Premium"
        "error_actualizacion" -> "Error al actualizar la configuración"

        // ✅ NUEVOS STRINGS PARA CATEGORÍAS
        "add_categoria" -> "Añadir categoría"
        "nueva_categoria" -> "Nueva Categoría"
        "editar_categoria" -> "Editar Categoría"
        "nombre" -> "Nombre"
        "seleccionar_color" -> "Seleccionar color"
        "pulsa_crear_primera_categoria" -> "Pulsa + para crear tu primera categoría"
        "eliminar_categoria" -> "Eliminar Categoría"
        "confirmar_eliminar_categoria" -> "¿Seguro que deseas eliminar \"{nombre}\"?"
        "categoria_creada" -> "Categoría creada exitosamente"
        "categoria_actualizada" -> "Categoría actualizada exitosamente"
        "categoria_eliminada" -> "Categoría eliminada exitosamente"
        "error_crear_categoria" -> "Error al crear la categoría"
        "error_actualizar_categoria" -> "Error al actualizar la categoría"
        "error_eliminar_categoria" -> "Error al eliminar la categoría"
        "nombre_categoria_requerido" -> "El nombre de la categoría es requerido"
        "nombre_categoria_muy_corto" -> "El nombre debe tener al menos 2 caracteres"
        "nombre_categoria_muy_largo" -> "El nombre no puede tener más de 50 caracteres"
        "categoria_nombre_duplicado" -> "Ya existe una categoría con ese nombre"
        "color_invalido" -> "Color inválido"
        "sincronizacion_completada" -> "Sincronización completada"
        "error_sincronizacion" -> "Error en sincronización"

        // Artículos - Pantalla principal
        "add_articulo" -> "Añadir artículo"
        "nuevo_articulo" -> "Nuevo Artículo"
        "editar_articulo" -> "Editar Artículo"
        "pulsa_crear_primer_articulo" -> "Pulsa + para crear tu primer artículo"
        "eliminar_articulo" -> "Eliminar Artículo"
        "confirmar_eliminar_articulo" -> "¿Seguro que deseas eliminar \"{nombre}\"?"

        // Campos del formulario
        "precio_venta" -> "Precio de venta"
        "precio_coste" -> "Precio de coste"
        "stock" -> "Stock"
        "categoria" -> "Categoría"
        "seleccionar_categoria" -> "Seleccionar categoría"
        "sin_categorias" -> "No hay categorías disponibles"

        // Controles Premium
        "controlar_coste" -> "Controlar coste"
        "controlar_stock" -> "Controlar stock"
        "marcar_favorito" -> "Marcar como favorito"

        // Validaciones y mensajes
        "precio_venta_requerido" -> "El precio de venta es requerido"
        "precio_venta_invalido" -> "Precio de venta inválido"
        "categoria_requerida" -> "Selecciona una categoría"
        "articulo_creado" -> "Artículo creado exitosamente"
        "articulo_actualizado" -> "Artículo actualizado exitosamente"
        "articulo_eliminado" -> "Artículo eliminado exitosamente"
        "error_crear_articulo" -> "Error al crear el artículo"
        "error_actualizar_articulo" -> "Error al actualizar el artículo"
        "error_eliminar_articulo" -> "Error al eliminar el artículo"

        // Estados
        "sin_articulos" -> "No tienes artículos registrados"
        "cargando_articulos" -> "Cargando artículos..."
        "sincronizando_articulos" -> "Sincronizando artículos..."

        // Premium
        "solo_premium_stock" -> "Control de stock disponible solo en Premium"
        "solo_premium_coste" -> "Control de coste disponible solo en Premium"

        // ✅ AUTHENTICATION STRINGS PHASE 2
        "login_subtitle" -> "Inicia sesión en tu cuenta"
        "register_subtitle" -> "Crea tu nueva cuenta"
        "email" -> "Email"
        "email_placeholder" -> "tu@email.com"
        "email_invalid" -> "Email inválido"
        "password" -> "Contraseña"
        "password_placeholder" -> "Mínimo 6 caracteres"
        "password_invalid" -> "Mínimo 6 caracteres requeridos"
        "login_button" -> "Iniciar Sesión"
        "register_button" -> "Crear Cuenta"
        "loading" -> "Cargando..."
        "or" -> "o"
        "go_to_register" -> "¿No tienes cuenta? Crear cuenta"
        "go_to_login" -> "¿Ya tienes cuenta? Iniciar sesión"
        "register_benefits_title" -> "🚀 Beneficios de crear cuenta:"
        "benefit_1" -> "• Sincronización automática en todos tus dispositivos"
        "benefit_2" -> "• Configuración personalizada guardada"
        "benefit_3" -> "• Acceso a funciones Premium"
        "benefit_4" -> "• Respaldo seguro de tus datos"
        "welcome" -> "Bienvenido"
        "premium_user" -> "Usuario Premium"
        "free_user" -> "Usuario Gratuito"
        "not_authenticated" -> "No autenticado"
        "home" -> "Inicio"
        "markets" -> "Mercadillos"
        "add_market" -> "Añadir Mercadillo"
        "premium_features" -> "Funciones Premium"
        "upgrade_premium" -> "Actualizar a Premium"
        "about" -> "Acerca de"
        "logout" -> "Cerrar Sesión"
        "exit_app" -> "Salir de la App"
        "logout_title" -> "Cerrar Sesión"
        "logout_message" -> "¿Estás seguro de que deseas cerrar sesión?"
        "logout_confirm" -> "Cerrar Sesión"
        "exit_title" -> "Salir de la App"
        "exit_message" -> "¿Estás seguro de que deseas salir de Market Sales?"
        "exit_confirm" -> "Salir"
        "available_markets" -> "Mercadillos Disponibles"
        "premium_required" -> "Requiere Premium para acceso completo"
        "see_details" -> "Ver detalles"
        "edit" -> "Editar"
        "menu" -> "Menú"
        "premium" -> "Premium"
        "free" -> "Gratuito"

        // 🚀 GOOGLE AUTH V8 - NUEVOS STRINGS AGREGADOS
        "google_signin" -> "Continuar con Google"
        "google_register" -> "Registrarse con Google"
        "google_auth_error" -> "Error de autenticación con Google"
        "google_token_error" -> "Error obteniendo token de Google"
        "auth_provider_google" -> "Google"
        "auth_provider_email" -> "Email"
        "google_signin_success" -> "Autenticación con Google exitosa"
        "google_signin_failed" -> "Falló la autenticación con Google"

        // ✅ STRINGS PARA MERCADILLOS V11
        "add_mercadillo" -> "Añadir mercadillo"
        "nuevo_mercadillo" -> "Nuevo Mercadillo"
        "editar_mercadillo" -> "Editar Mercadillo"
        "ver_leyenda" -> "Ver leyenda"
        "leyenda_estados" -> "Leyenda de Estados"
        "entendido" -> "Entendido"
        "proximo_mercadillo" -> "Próximo Mercadillo"
        "sin_mercadillos" -> "No tienes mercadillos registrados"
        "pulsa_crear_primer_mercadillo" -> "Pulsa + para crear tu primer mercadillo"

        // Estados de mercadillos
        "estado_programado_parcial" -> "Programado parcialmente"
        "estado_programado_total" -> "Programado totalmente"
        "estado_en_curso" -> "En curso"
        "estado_pendiente_arqueo" -> "Terminado (pendiente arqueo)"
        "estado_pendiente_asignar_saldo" -> "Arqueo realizado (pendiente asignar saldo)"
        "estado_cerrado_completo" -> "Cerrado completamente"
        "estado_cancelado" -> "Cancelado"

        // Formulario de mercadillos
        "fecha_mercadillo" -> "Fecha del mercadillo"
        "lugar_mercadillo" -> "Lugar"
        "organizador_mercadillo" -> "Organizador"
        "hora_inicio" -> "Hora de inicio"
        "hora_fin" -> "Hora de fin"
        "es_gratis" -> "Es gratis"
        "importe_suscripcion" -> "Importe suscripción"
        "requiere_mesa" -> "Requiere mesa"
        "requiere_carpa" -> "Requiere carpa"
        "hay_punto_luz" -> "Hay punto de luz"

        // Validaciones mercadillos
        "fecha_requerida" -> "La fecha es requerida"
        "fecha_invalida" -> "Formato de fecha inválido (dd-MM-yyyy)"
        "lugar_requerido" -> "El lugar es requerido"
        "lugar_muy_corto" -> "El lugar debe tener al menos 3 caracteres"
        "lugar_muy_largo" -> "El lugar no puede tener más de 100 caracteres"
        "organizador_requerido" -> "El organizador es requerido"
        "organizador_muy_corto" -> "El organizador debe tener al menos 3 caracteres"
        "organizador_muy_largo" -> "El organizador no puede tener más de 100 caracteres"
        "hora_inicio_invalida" -> "Formato de hora de inicio inválido (HH:mm)"
        "hora_fin_invalida" -> "Formato de hora de fin inválido (HH:mm)"
        "horarios_invalidos" -> "La hora de inicio debe ser anterior a la hora de fin"
        "importe_negativo" -> "El importe no puede ser negativo"
        "importe_muy_alto" -> "El importe es demasiado alto"
        "mercadillo_existe_fecha_hora" -> "Ya existe un mercadillo en esa fecha y hora"

        // Mensajes mercadillos
        "mercadillo_creado" -> "Mercadillo creado exitosamente"
        "mercadillo_actualizado" -> "Mercadillo actualizado exitosamente"
        "mercadillo_eliminado" -> "Mercadillo eliminado exitosamente"
        "mercadillo_cancelado" -> "Mercadillo cancelado exitosamente"
        "error_crear_mercadillo" -> "Error creando mercadillo"
        "error_actualizar_mercadillo" -> "Error actualizando mercadillo"
        "error_eliminar_mercadillo" -> "Error eliminando mercadillo"
        "error_cancelar_mercadillo" -> "Error cancelando mercadillo"

        // Saldos y arqueos
        "saldo_inicial" -> "Saldo inicial"
        "saldo_final" -> "Saldo final"
        "asignar_saldo_inicial" -> "Asignar saldo inicial"
        "asignar_automaticamente" -> "Asignar automáticamente"
        "realizar_arqueo" -> "Realizar arqueo"
        "arqueo_caja" -> "Arqueo de caja"
        "saldo_asignado" -> "Saldo inicial asignado"
        "arqueo_realizado" -> "Arqueo de caja realizado exitosamente"
        "sin_saldo_anterior" -> "No hay mercadillos anteriores con saldo disponible"
        "saldo_negativo" -> "El saldo no puede ser negativo"
        "saldo_muy_alto" -> "El saldo es demasiado alto"

        // Confirmaciones
        "confirmar_eliminar_mercadillo" -> "¿Seguro que deseas eliminar este mercadillo?"
        "confirmar_cancelar_mercadillo" -> "¿Seguro que deseas cancelar este mercadillo?"
        "mercadillo_tiene_ventas" -> "No se puede cancelar: el mercadillo tiene ventas"
        "mercadillo_estado_no_valido" -> "No se puede cancelar: estado no válido"

        "informacion_basica" -> "Información Básica"
        "horarios" -> "Horarios"
        "configuracion_mercadillo" -> "Configuración"
        "crear_mercadillo" -> "Crear Mercadillo"
        "actualizar_mercadillo" -> "Actualizar Mercadillo"
        "guardando" -> "Guardando..."
        "saldo_inicial_ayuda" -> "Dinero disponible al inicio del mercadillo"
        "seleccionar_fecha" -> "Seleccionar fecha"
        "seleccionar_hora_inicio" -> "Seleccionar hora inicio"
        "seleccionar_hora_fin" -> "Seleccionar hora fin"

        // Validaciones específicas para el formulario
        "fecha_no_puede_estar_vacia" -> "La fecha no puede estar vacía"
        "lugar_no_puede_estar_vacio" -> "El lugar no puede estar vacío"
        "organizador_no_puede_estar_vacio" -> "El organizador no puede estar vacío"
        "formato_fecha_invalido" -> "Formato de fecha inválido (dd-MM-yyyy)"
        "formato_hora_inicio_invalido" -> "Formato de hora de inicio inválido (HH:mm)"
        "formato_hora_fin_invalido" -> "Formato de hora de fin inválido (HH:mm)"
        "hora_inicio_debe_ser_anterior" -> "La hora de inicio debe ser anterior a la hora de fin"
        "lugar_minimo_caracteres" -> "El lugar debe tener al menos 3 caracteres"
        "lugar_maximo_caracteres" -> "El lugar no puede tener más de 100 caracteres"
        "organizador_minimo_caracteres" -> "El organizador debe tener al menos 3 caracteres"
        "organizador_maximo_caracteres" -> "El organizador no puede tener más de 100 caracteres"
        "importe_no_puede_ser_negativo" -> "El importe no puede ser negativo"
        "importe_demasiado_alto" -> "El importe es demasiado alto"
        "saldo_inicial_no_puede_ser_negativo" -> "El saldo inicial no puede ser negativo"
        "saldo_inicial_demasiado_alto" -> "El saldo inicial es demasiado alto"

        "utilidades_premium_msg" -> "Estas opciones están disponibles solo para usuarios Premium"

        "seleccionar_hora" -> "Seleccionar hora"
        "opcional" -> "(opcional)"
        "lunes" -> "L"
        "martes" -> "M"
        "miercoles" -> "X"
        "jueves" -> "J"
        "viernes" -> "V"
        "sabado" -> "S"
        "domingo" -> "D"

        "utilidades" -> "Utilidades"
        "usuario" -> "Usuario"
        "usuario_invitado" -> "Usuario invitado"

        "realizar_cargo" -> "Realizar cargo"
        "carrito" -> "Carrito"

        // ui/components/BottomBarMercadillo.kt
        "cambiar_mercadillo_seleccionado" -> "Cambiar mercadillo seleccionado"
        "mercadillo_activo" -> "Mercadillo activo:"
        "ventas" -> "Ventas"
        "gastos" -> "Gastos"
        "resumen" -> "Resumen"

        // ui/components/DialogoSeleccionMercadilloActivo.kt
        "seleccionar_mercadillo_activo" -> "Seleccionar mercadillo activo"
        "varios_mercadillos_en_curso" -> "Hay varios mercadillos en curso. Elige uno para continuar:"

        // ui/components/DialogSelectorColor.kt
        "seleccionar_un_color" -> "Selecciona un color"
        "color_seleccionado" -> "Color seleccionado"

        "procesando" -> "Procesando…"

        // ui/components/dialogs/DialogoSeleccionMercadillo.kt
        "seleccionar_mercadillo" -> "Seleccionar mercadillo"

        // ui/components/gastos/BarraAccionesGasto.kt
        "cargar_gasto" -> "Cargar gasto"

        // ui/components/proximos/CardMercadillosProximos.kt
        "proximos_mercadillos" -> "Próximos mercadillos"

// ui/composables/gastos/PestanaGastosAutomaticas.kt
        "gastos_automaticos_proximamente" -> "Gastos automáticos — próximamente"

        // ui/composables/gastos/PestanaGastosManual.kt
        "anadir_gasto" -> "Añadir gasto"

// ui/composables/resumen/PestanaResumenGastos.kt
        "resumen_gastos" -> "Resumen de gastos"
        "total_gastos" -> "Total de gastos"

        // ui/composables/resumen/PestanaResumenVentas.kt
        "resumen_ventas" -> "Resumen de ventas"
        "total_ventas" -> "Total de ventas"
        "confirmar_abono" -> "Confirmar abono"
        "confirmar_abono_pregunta" -> "¿Seguro que quieres abonar esta línea?"
        "cantidad" -> "Cantidad"
        "total_linea" -> "Total línea"
        "si_abonar" -> "Sí, abonar"
        "precio_unitario" -> "PU:"
        "abonar" -> "Abonar"
        "abono_chip" -> "ABONO"

        // ui/composables/CampoDescripcion.kt
        "descripcion_label" -> "Descripción *"
        "descripcion_placeholder" -> "Ej: Llavero Delfín Madera"
        "descripcion_obligatoria" -> "La descripción es obligatoria"

        // ui/composables/CampoImporte.kt
        "importe" -> "Importe"

        // ui/composables/PestanaVentaManual.kt
        "anadir" -> "Añadir"
        "anadir_venta" -> "Añadir venta"

        // ui/composables/PestanaVentaProductos.kt
        "todos_los_productos" -> "Todos los productos"
        "buscar" -> "Buscar"
        "cerrar_busqueda" -> "Cerrar búsqueda"
        "buscar_placeholder" -> "Buscar..."
        "todas" -> "Todas"
        "inicia_sesion_ver_productos" -> "Inicia sesión para ver productos"
        "no_hay_productos" -> "No hay productos"

// ui/composables/TecladoNumerico.kt
        "borrar" -> "Borrar"

        // pantallas/arqueo/PantallaArqueo.kt
        "arqueo" -> "Arqueo"
        "cargando_datos_mercadillo" -> "Cargando datos de mercadillo…"
        "resumen_mercadillo" -> "Resumen del mercadillo"
        "bizum" -> "Bizum"
        "tarjeta" -> "Tarjeta"
        "efectivo" -> "Efectivo"
        "total" -> "Total"
        "acciones" -> "Acciones"
        "resultado_mercadillo" -> "Resultado del mercadillo"
        "asignar_saldo" -> "Asignar saldo"
        "cerrar" -> "Cerrar"
        "guion_largo" -> "—"

        // pantallas/arqueo/PantallaArqueoCaja.kt
        "ventas_en_efectivo" -> "Ventas en efectivo"
        "gastos_en_efectivo" -> "Gastos en efectivo"
        "resultado_arqueo" -> "Resultado del arqueo"
        "confirmar_arqueo" -> "Confirmar arqueo"
        "confirmar" -> "Confirmar"
        "confirmar_arqueo_aviso_unavez" -> "El arqueo solo se puede confirmar una vez"
        "confirmar_arqueo_aviso_guardado" -> "Se guardará el resultado del arqueo como saldo final de caja"
        "confirmar_arqueo_aviso_premium" -> "Los usuarios Premium podrán asignar ese saldo a un nuevo mercadillo o guardarlo para más adelante"
        "confirmar_arqueo_pregunta" -> "¿Estás seguro de querer confirmar el arqueo?"

        // pantallas/arqueo/PantallaAsignarSaldo.kt
        "opcion_solo_premium" -> "Esta opción es solo para usuarios Premium."
        "guardar_saldo" -> "Guardar saldo"
        "guardar_saldo_desc" -> "Conserva este saldo para asignarlo cuando crees un mercadillo."
        "retirar_efectivo" -> "Retirar efectivo"
        "anadir_efectivo" -> "Añadir efectivo"
        "importe_a_retirar" -> "Importe a retirar"
        "importe_a_anadir" -> "Importe a añadir"
        "selecciona_opcion_importe" -> "Selecciona una opción para introducir importe"

        // pantallas/arqueo/PantallaResultadoMercadillo.kt
        "ventas_por_metodo" -> "Ventas por método"
        "gastos_por_metodo" -> "Gastos por método"
        "total_gastos_metodos" -> "Total gastos (métodos)"
        "suscripcion" -> "Suscripción"


        // pantallas/gastos/PantallaGastos.kt
        "ingresar_gasto" -> "Ingresar gasto"
        "automaticos" -> "Automáticos"

        // pantallas/gastos/PantallaGastosCarrito.kt
        "gastos_carrito" -> "Carrito de gastos"

        "descripcion" -> "Descripción"

        // pantallas/login/LoginScreen.kt
        "contrasena" -> "Contraseña"
        "registrarse" -> "Registrarse"
        "o" -> "o"
        "continuar_google" -> "Continuar con Google"

        // pantallas/alta/PantallaAltaMercadillo.kt
        "alta_titulo_editar" -> "Editar Mercadillo"
        "alta_titulo_nuevo" -> "Nuevo Mercadillo"
        "lugar" -> "Lugar"
        "placeholder_lugar" -> "Ej: Plaza Mayor"
        "organizador" -> "Organizador"
        "placeholder_organizador" -> "Ej: Ayuntamiento"
        "aviso_saldo_en_curso" -> "Aviso: cambiar el saldo inicial de un mercadillo EN CURSO puede provocar descuadre."
        "borrar_mercadillo" -> "Borrar mercadillo"
        "actualizar" -> "Actualizar"
        "crear" -> "Crear"
        "saldo_pendiente_titulo" -> "Saldo pendiente"
        "saldo_pendiente_texto" -> "Tienes un saldo guardado pendiente de asignar. ¿Quieres asignarlo a este mercadillo?"
        "si" -> "Sí"
        "no" -> "No"
        "confirmar_cambio_saldo_titulo" -> "Confirmar cambio de saldo"
        "confirmar_cambio_saldo_linea1" -> "Has modificado el saldo inicial de un mercadillo EN CURSO. Esto puede provocar descuadre de caja."
        "confirmar_cambio_saldo_linea2" -> "¿Quieres guardar igualmente?"
        "guardar_igualmente" -> "Guardar igualmente"
        "confirmar_borrado_titulo" -> "Confirmar borrado"
        "confirmar_borrado_texto" -> "¿Estás seguro de querer borrar el mercadillo?"

        // pantallas/articulos/PantallaArticulos.kt
        "precio_invalido" -> "Precio de venta inválido"
        "selecciona_categoria" -> "Selecciona una categoría"
        "sin_categoria" -> "Sin categoría"
        "precio" -> "Precio"
        "coste" -> "Coste"
        "favorito_estrella" -> "⭐ Favorito"
        "editar" -> "Editar"


        "cambio_titulo" -> "Cambio"
        "entregado" -> "Entregado"
        "cambio" -> "Cambio"

        // utils/SpanishStrings.kt — añadir dentro de when(key)
        "solo_premium" -> "Solo Premium"
        "expandir" -> "Expandir"
        "version_invitado" -> "INVITADO"
        "app_version" -> "App {version}"
        "peso_argentino" -> "$ Peso Argentino"
        "peso_mexicano" -> "$ Peso Mexicano"
        "peso_colombiano" -> "$ Peso Colombiano"
        "sol_peruano" -> "S/ Sol Peruano"
        "peso_chileno" -> "$ Peso Chileno"
        "bolivar" -> "Bs Bolívar"
        "real_brasileno" -> "$ Real Brasileño"

        // utils/SpanishStrings.kt
        "recibo" -> "Recibo"
        "metodo_pago" -> "Método de pago: {metodo}"
        "finalizar_venta" -> "Finalizar venta"

        // utils/SpanishStrings.kt — nuevas
        "pendiente_desarrollo" -> "Pendiente de desarrollo"
        "muy_pronto_gestionar_stock" -> "Muy pronto podrás gestionar existencias y stock."

        "aqui_listados_informes" -> "Aquí vivirán los listados e informes."

        // utils/SpanishStrings.kt — nuevas
        "logout_success" -> "Sesión cerrada correctamente"
        "google_auth_unavailable" -> "Google Auth no disponible — Revisa la configuración"

        // utils/SpanishStrings.kt — nuevas
        "sesion_cerrada" -> "Sesión cerrada"
        "redirigiendo_al_inicio" -> "Redirigiendo al inicio…"

        "saldo_guardado" -> "Saldo guardado"
        "saldos_pendientes_asignar" -> "Saldo pendiente por asignar"

        "preparando_datos" -> "Preparando…"
        "comprobando_datos_locales" -> "Comprobando datos locales…"
        "descargando_firebase" -> "Descargando datos de Firebase…"
        "listo" -> "Listo."
        "error_inesperado" -> "Error inesperado"
        "no_se_pudieron_cargar_datos" -> "No se pudieron cargar los datos."
        "reintentar" -> "Reintentar"

        "nueva_venta" -> "Nueva venta"
        "venta_manual" -> "Venta manual"
        "productos" -> "Productos"

        "gestion_mercadillos" -> "Gestión de Mercadillos"

        "mercadillo_no_encontrado" -> "Mercadillo no encontrado"
        "error_cargando_datos" -> "Error cargando datos: {0}"

        "articulo_nombre_duplicado" -> "Ya existe un artículo con ese nombre"
        "articulo_creado_ok" -> "Artículo creado exitosamente"
        "error_creando_articulo" -> "Error creando artículo: {0}"
        "articulo_actualizado_ok" -> "Artículo actualizado exitosamente"
        "error_actualizando_articulo" -> "Error actualizando artículo"
        "error_actualizando_articulo_detalle" -> "Error actualizando artículo: {0}"
        "articulo_eliminado_ok" -> "Artículo eliminado exitosamente"
        "error_eliminando_articulo" -> "Error eliminando artículo"
        "error_eliminando_articulo_detalle" -> "Error eliminando artículo: {0}"
        "sync_completada" -> "Sincronización completada"
        "sync_error" -> "Error en sincronización"
        "sync_no_se_pudo_completar" -> "No se pudo completar la sincronización"
        "sync_error_detalle" -> "Error en sincronización: {0}"
        "nombre_min_caracteres" -> "El nombre debe tener al menos 2 caracteres"
        "nombre_max_caracteres" -> "El nombre no puede tener más de 100 caracteres"
        "precio_negativo" -> "El precio no puede ser negativo"
        "precio_demasiado_alto" -> "El precio es demasiado alto"
        "precio_coste_negativo" -> "El precio de coste no puede ser negativo"
        "precio_coste_demasiado_alto" -> "El precio de coste es demasiado alto"
        "stock_negativo" -> "El stock no puede ser negativo"
        "stock_demasiado_alto" -> "El stock es demasiado alto"

        "confirmar_guardar_saldo" -> "¿Estás seguro de querer guardar el saldo?"
        "guardar_saldo_uso" -> "Se podrá utilizar como saldo inicial al dar de alta un mercadillo."
        "guardar_saldo_reemplazar_aviso" -> "Ya hay un saldo inicial guardado. ¿Seguro que deseas reemplazarlo? Esta operación no se puede deshacer."
        "guardar_saldo_limite" -> "Si lo guardas, solo podrás asignarlo al crear un mercadillo nuevo."
        "confirmar_asignar_saldo" -> "¿Estás seguro de querer asignar el saldo inicial a este mercadillo?"
        "destino_con_saldo_pregunta" -> "El mercadillo seleccionado ya tiene saldo inicial. ¿Quieres reemplazar el saldo inicial?"
        "operacion_no_se_puede_deshacer" -> "Esta operación no se puede deshacer."

        "categoria_creada_ok" -> "Categoría creada exitosamente"
        "error_creando_categoria_detalle" -> "Error creando categoría: {0}"
        "categoria_actualizada_ok" -> "Categoría actualizada exitosamente"
        "error_actualizando_categoria" -> "Error actualizando categoría"
        "error_actualizando_categoria_detalle" -> "Error actualizando categoría: {0}"
        "categoria_eliminada_ok" -> "Categoría eliminada exitosamente"
        "error_eliminando_categoria" -> "Error eliminando categoría"
        "error_eliminando_categoria_detalle" -> "Error eliminando categoría: {0}"
        "error_en_sincronizacion" -> "Error en sincronización"
        "no_se_pudo_completar_sincronizacion" -> "No se pudo completar la sincronización"
        "error_en_sincronizacion_detalle" -> "Error en sincronización: {0}"
        "nombre_min_2" -> "El nombre debe tener al menos 2 caracteres"
        "nombre_max_50" -> "El nombre no puede tener más de 50 caracteres"

        "gastos_cargados" -> "Gastos cargados"
        "error_guardando_gastos" -> "Error guardando gastos"

// SpanishStrings.get(...)
        "mercadillo_creado_con_saldo" -> "Mercadillo creado con saldo inicial de {0}"
        "mercadillo_creado_ok" -> "Mercadillo creado exitosamente"
        "error_creando_mercadillo" -> "Error creando mercadillo: {0}"
        "error_cargando_mercadillo" -> "Error cargando mercadillo: {0}"
        "confirmar_cambio_saldo_en_curso" -> "Estás modificando el saldo inicial de un mercadillo en curso. Esto puede provocar descuadre de caja.\n\n¿Quieres guardar igualmente?"
        "mercadillo_actualizado_ok" -> "Mercadillo actualizado exitosamente"
        "error_actualizando_mercadillo" -> "Error actualizando mercadillo"
        "error_actualizando_mercadillo_detalle" -> "Error actualizando mercadillo: {0}"
        "error_eliminando_mercadillo" -> "Error eliminando mercadillo"
        "error_eliminando_mercadillo_detalle" -> "Error eliminando mercadillo: {0}"
        "no_hay_mercadillos_en_curso" -> "No hay mercadillos en curso"
        "sincronizacion_completada_sin_cambios" -> "Sincronización completada (sin cambios automáticos)"
        "error_sincronizacion_detalle" -> "Error en sincronización: {0}"
        "usuario_no_autenticado" -> "Usuario no autenticado"
        "estado_cambiado_a" -> "Estado cambiado a: {0}"
        "error_cambiando_estado" -> "Error cambiando estado"
        "error_cambiando_estado_detalle" -> "Error cambiando estado: {0}"
        "estado_mercadillo_no_valido" -> "Estado de mercadillo no válido"
        "no_modificar_estado" -> "No se puede modificar un mercadillo en estado: {0}"
        "no_modificar_fecha_en_curso" -> "No se puede modificar la fecha de un mercadillo en curso"
        "valid_fecha_vacia" -> "La fecha no puede estar vacía"
        "valid_formato_fecha_invalido" -> "Formato de fecha inválido (dd-MM-yyyy)"
        "valid_fecha_invalida" -> "Fecha inválida"
        "valid_fecha_pasada" -> "No se puede crear un mercadillo en una fecha anterior a hoy"
        "valid_error_disponibilidad_fecha" -> "Error validando disponibilidad de fecha"
        "free_limite_mercadillos_por_dia" -> "Los usuarios FREE solo pueden crear un mercadillo por día. Actualiza a Premium para crear múltiples mercadillos."
        "valid_lugar_vacio" -> "El lugar no puede estar vacío"
        "valid_lugar_min" -> "El lugar debe tener al menos 3 caracteres"
        "valid_lugar_max" -> "El lugar no puede tener más de 100 caracteres"
        "valid_organizador_vacio" -> "El organizador no puede estar vacío"
        "valid_organizador_min" -> "El organizador debe tener al menos 3 caracteres"
        "valid_organizador_max" -> "El organizador no puede tener más de 100 caracteres"
        "valid_hora_inicio_formato" -> "Formato de hora de inicio inválido (HH:mm)"
        "valid_hora_fin_formato" -> "Formato de hora de fin inválido (HH:mm)"
        "valid_horarios_invalidos" -> "Horarios inválidos"
        "valid_importe_negativo" -> "El importe no puede ser negativo"
        "valid_importe_alto" -> "El importe es demasiado alto"
        "valid_saldo_inicial_negativo" -> "El saldo inicial no puede ser negativo"
        "valid_saldo_inicial_alto" -> "El saldo inicial es demasiado alto"
        "valid_saldo_final_negativo" -> "El saldo final no puede ser negativo"
        "valid_saldo_final_alto" -> "El saldo final es demasiado alto"

        // SpanishStrings.get(...)
        "error_cargando_proximos_mercadillos" -> "Error cargando próximos mercadillos: {0}"

        "pu_label" -> "PU:"
        "metodo_efectivo" -> "Efectivo"
        "metodo_bizum" -> "Bizum"
        "metodo_tarjeta" -> "Tarjeta"

// SpanishStrings.get(...)
        "importe_mayor_cero" -> "El importe debe ser mayor que 0"
        "no_hay_lineas_vender" -> "No hay líneas para vender"
        "total_mayor_cero" -> "El total debe ser mayor que 0"
        "mercadillo_no_inicializado" -> "Mercadillo no inicializado"
        "error_guardar_venta" -> "Error al guardar la venta: {0}"

        "estado_debug_titulo" -> "🔧 Estado (DEBUG)"
        "estado_debug_aviso" -> "⚠️ Solo para debugging - Se eliminará en producción"
        "fecha" -> "Fecha"
        "formato_fecha_hint" -> "dd-MM-yyyy"
        "config_economica" -> "Configuración Económica"
        "mercadillo_gratuito" -> "Mercadillo gratuito"
        "importe_placeholder" -> "0,00"
        "config_logistica" -> "Configuración Logística"






        else -> key
    }
}