package es.nuskysoftware.marketsales.utils

import java.util.Locale

/**
 * Gestor de recursos de string que permite cambio dinámico de idioma
 * V11 - Con strings completos para PantallaPerfil + PantallaCategorias
 */
object StringResourceManager {

    fun getString(key: String, language: String = "es"): String {
        return when (language) {
            "en" -> getEnglishString(key)
            "es" -> getSpanishString(key)
            else -> getSpanishString(key)
        }
    }

    /**
     * Strings en español
     */
    private fun getSpanishString(key: String): String = when (key) {
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
        "articulos" -> "Artículos"
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

        "seleccionar_hora" -> "Seleccionar hora"
        "opcional" -> "(opcional)"
        "lunes" -> "L"
        "martes" -> "M"
        "miercoles" -> "X"
        "jueves" -> "J"
        "viernes" -> "V"
        "sabado" -> "S"
        "domingo" -> "D"

        else -> key
    }

    /**
     * Strings en inglés
     */
    private fun getEnglishString(key: String): String = when (key) {
        // Main screen
        "app_name" -> "Market Sales"
        "mercadillos" -> "Markets"
        "configuracion" -> "Settings"
        "articulos" -> "Articles"
        "categorias" -> "Categories"
        "inventario" -> "Inventory"
        "listados" -> "Reports"
        "iniciar_sesion" -> "Login"
        "cerrar_sesion" -> "Logout"
        "salir" -> "Exit"
        "perfil" -> "Profile"

        // Settings
        "informacion_cuenta" -> "Account Information"
        "cambiar_contrasena" -> "Change Password"
        "tema" -> "Theme"
        "fuente" -> "Font"
        "idioma" -> "Language"
        "moneda" -> "Currency"
        "version_premium" -> "PREMIUM"
        "version_free" -> "FREE"
        "promocion_premium" -> "Upgrade to Premium!"
        "desbloquea_funciones" -> "Unlock all features"

        // Theme
        "tema_claro" -> "Light"
        "tema_oscuro" -> "Dark"

        // Languages
        "espanol" -> "Español"
        "ingles" -> "English"

        // Fonts
        "montserrat" -> "Montserrat"
        "poppins" -> "Poppins"
        "roboto" -> "Roboto"

        // Currencies
        "euro" -> "€ Euro"
        "dolar" -> "$ Dollar"
        "libra" -> "£ Pound"
        "yen" -> "¥ Yen"

        // ✅ PROFILE SCREEN - COMPLETE STRINGS
        "modificar_informacion" -> "Change your personal information"
        "informacion_personal" -> "Personal Information"
        "nombre_completo" -> "Full name"
        "correo_electronico" -> "Email address"
        "actualizar_informacion" -> "Update Information"
        "actualizando" -> "Updating..."
        "cambiar_contrasena_titulo" -> "Change Password"
        "cambiar_contrasena_descripcion" -> "Leave blank if you don't want to change the password"
        "contrasena_actual" -> "Current password"
        "nueva_contrasena" -> "New password"
        "confirmar_nueva_contrasena" -> "Confirm new password"
        "cambiando" -> "Changing..."
        "cambiar_contrasena_btn" -> "Change Password"

        // Validations
        "nombre_vacio" -> "Name cannot be empty"
        "email_invalido" -> "Invalid email"
        "contrasena_actual_requerida" -> "Current password required"
        "contrasena_minimo_caracteres" -> "Password must be at least 6 characters"
        "contrasenas_no_coinciden" -> "Passwords don't match"

        // Success/Error messages
        "informacion_actualizada" -> "✅ Information updated successfully"
        "contrasena_actualizada" -> "✅ Password updated successfully"
        "error_actualizar_informacion" -> "❌ Error updating information"
        "error_cambiar_contrasena" -> "❌ Error changing password"
        "error_generico" -> "❌ Error: {0}"

        // Buttons and actions
        "volver" -> "Back"
        "ocultar_contrasena" -> "Hide password"
        "mostrar_contrasena" -> "Show password"

        // Dialogs
        "cambiar" -> "Change"
        "cancelar" -> "Cancel"
        "guardar" -> "Save"
        "aceptar" -> "Accept"
        "eliminar" -> "Delete"
        "contrasenas_no_coinciden_dialogo" -> "Passwords don't match"
        "contrasena_muy_corta" -> "Password must be at least 6 characters"

        // Messages
        "funcion_premium" -> "This feature is available only in Premium version"
        "error_actualizacion" -> "Error updating configuration"

        // ✅ NEW STRINGS FOR CATEGORIES
        "add_categoria" -> "Add category"
        "nueva_categoria" -> "New Category"
        "editar_categoria" -> "Edit Category"
        "nombre" -> "Name"
        "seleccionar_color" -> "Select color"
        "pulsa_crear_primera_categoria" -> "Tap + to create your first category"
        "eliminar_categoria" -> "Delete Category"
        "confirmar_eliminar_categoria" -> "Are you sure you want to delete \"{nombre}\"?"
        "categoria_creada" -> "Category created successfully"
        "categoria_actualizada" -> "Category updated successfully"
        "categoria_eliminada" -> "Category deleted successfully"
        "error_crear_categoria" -> "Error creating category"
        "error_actualizar_categoria" -> "Error updating category"
        "error_eliminar_categoria" -> "Error deleting category"
        "nombre_categoria_requerido" -> "Category name is required"
        "nombre_categoria_muy_corto" -> "Name must be at least 2 characters"
        "nombre_categoria_muy_largo" -> "Name cannot be more than 50 characters"
        "categoria_nombre_duplicado" -> "A category with that name already exists"
        "color_invalido" -> "Invalid color"
        "sincronizacion_completada" -> "Synchronization completed"
        "error_sincronizacion" -> "Synchronization error"

        // Articles - Main screen
        "articulos" -> "Articles"
        "add_articulo" -> "Add article"
        "nuevo_articulo" -> "New Article"
        "editar_articulo" -> "Edit Article"
        "pulsa_crear_primer_articulo" -> "Tap + to create your first article"
        "eliminar_articulo" -> "Delete Article"
        "confirmar_eliminar_articulo" -> "Are you sure you want to delete \"{nombre}\"?"

        // Form fields
        "precio_venta" -> "Sale price"
        "precio_coste" -> "Cost price"
        "stock" -> "Stock"
        "categoria" -> "Category"
        "seleccionar_categoria" -> "Select category"
        "sin_categorias" -> "No categories available"

        // Premium controls
        "controlar_coste" -> "Control cost"
        "controlar_stock" -> "Control stock"
        "marcar_favorito" -> "Mark as favorite"

        // Validations and messages
        "precio_venta_requerido" -> "Sale price is required"
        "precio_venta_invalido" -> "Invalid sale price"
        "categoria_requerida" -> "Select a category"
        "articulo_creado" -> "Article created successfully"
        "articulo_actualizado" -> "Article updated successfully"
        "articulo_eliminado" -> "Article deleted successfully"
        "error_crear_articulo" -> "Error creating article"
        "error_actualizar_articulo" -> "Error updating article"
        "error_eliminar_articulo" -> "Error deleting article"

        // States
        "sin_articulos" -> "You have no registered articles"
        "cargando_articulos" -> "Loading articles..."
        "sincronizando_articulos" -> "Syncing articles..."

        // Premium
        "solo_premium_stock" -> "Stock control available only in Premium"
        "solo_premium_coste" -> "Cost control available only in Premium"


        // ✅ AUTHENTICATION STRINGS PHASE 2
        "login_subtitle" -> "Sign in to your account"
        "register_subtitle" -> "Create your new account"
        "email" -> "Email"
        "email_placeholder" -> "your@email.com"
        "email_invalid" -> "Invalid email"
        "password" -> "Password"
        "password_placeholder" -> "Minimum 6 characters"
        "password_invalid" -> "Minimum 6 characters required"
        "login_button" -> "Sign In"
        "register_button" -> "Create Account"
        "loading" -> "Loading..."
        "or" -> "or"
        "go_to_register" -> "Don't have an account? Create account"
        "go_to_login" -> "Already have an account? Sign in"
        "register_benefits_title" -> "🚀 Account benefits:"
        "benefit_1" -> "• Automatic sync across all your devices"
        "benefit_2" -> "• Personalized configuration saved"
        "benefit_3" -> "• Access to Premium features"
        "benefit_4" -> "• Secure data backup"
        "welcome" -> "Welcome"
        "premium_user" -> "Premium User"
        "free_user" -> "Free User"
        "not_authenticated" -> "Not authenticated"
        "home" -> "Home"
        "markets" -> "Markets"
        "add_market" -> "Add Market"
        "premium_features" -> "Premium Features"
        "upgrade_premium" -> "Upgrade to Premium"
        "about" -> "About"
        "logout" -> "Sign Out"
        "exit_app" -> "Exit App"
        "logout_title" -> "Sign Out"
        "logout_message" -> "Are you sure you want to sign out?"
        "logout_confirm" -> "Sign Out"
        "exit_title" -> "Exit App"
        "exit_message" -> "Are you sure you want to exit Market Sales?"
        "exit_confirm" -> "Exit"
        "available_markets" -> "Available Markets"
        "premium_required" -> "Requires Premium for full access"
        "see_details" -> "See details"
        "edit" -> "Edit"
        "menu" -> "Menu"
        "premium" -> "Premium"
        "free" -> "Free"

        // 🚀 GOOGLE AUTH V8 - NEW ENGLISH STRINGS
        "google_signin" -> "Continue with Google"
        "google_register" -> "Sign up with Google"
        "google_auth_error" -> "Google authentication error"
        "google_token_error" -> "Error getting Google token"
        "auth_provider_google" -> "Google"
        "auth_provider_email" -> "Email"
        "google_signin_success" -> "Google authentication successful"
        "google_signin_failed" -> "Google authentication failed"

        // ✅ STRINGS PARA MERCADILLOS V11
        "add_mercadillo" -> "Add market"
        "nuevo_mercadillo" -> "New Market"
        "editar_mercadillo" -> "Edit Market"
        "ver_leyenda" -> "View legend"
        "leyenda_estados" -> "Status Legend"
        "entendido" -> "Understood"
        "proximo_mercadillo" -> "Next Market"
        "sin_mercadillos" -> "You have no registered markets"
        "pulsa_crear_primer_mercadillo" -> "Tap + to create your first market"

        // Market states
        "estado_programado_parcial" -> "Partially scheduled"
        "estado_programado_total" -> "Fully scheduled"
        "estado_en_curso" -> "In progress"
        "estado_pendiente_arqueo" -> "Finished (pending cash count)"
        "estado_pendiente_asignar_saldo" -> "Cash counted (pending balance assignment)"
        "estado_cerrado_completo" -> "Completely closed"
        "estado_cancelado" -> "Cancelled"

        // Market form
        "fecha_mercadillo" -> "Market date"
        "lugar_mercadillo" -> "Location"
        "organizador_mercadillo" -> "Organizer"
        "hora_inicio" -> "Start time"
        "hora_fin" -> "End time"
        "es_gratis" -> "Is free"
        "importe_suscripcion" -> "Subscription amount"
        "requiere_mesa" -> "Requires table"
        "requiere_carpa" -> "Requires tent"
        "hay_punto_luz" -> "Has power outlet"

        // Market validations
        "fecha_requerida" -> "Date is required"
        "fecha_invalida" -> "Invalid date format (dd-MM-yyyy)"
        "lugar_requerido" -> "Location is required"
        "lugar_muy_corto" -> "Location must be at least 3 characters"
        "lugar_muy_largo" -> "Location cannot be more than 100 characters"
        "organizador_requerido" -> "Organizer is required"
        "organizador_muy_corto" -> "Organizer must be at least 3 characters"
        "organizador_muy_largo" -> "Organizer cannot be more than 100 characters"
        "hora_inicio_invalida" -> "Invalid start time format (HH:mm)"
        "hora_fin_invalida" -> "Invalid end time format (HH:mm)"
        "horarios_invalidos" -> "Start time must be before end time"
        "importe_negativo" -> "Amount cannot be negative"
        "importe_muy_alto" -> "Amount is too high"
        "mercadillo_existe_fecha_hora" -> "A market already exists at that date and time"

        // Market messages
        "mercadillo_creado" -> "Market created successfully"
        "mercadillo_actualizado" -> "Market updated successfully"
        "mercadillo_eliminado" -> "Market deleted successfully"
        "mercadillo_cancelado" -> "Market cancelled successfully"
        "error_crear_mercadillo" -> "Error creating market"
        "error_actualizar_mercadillo" -> "Error updating market"
        "error_eliminar_mercadillo" -> "Error deleting market"
        "error_cancelar_mercadillo" -> "Error cancelling market"

        // Balances and cash counts
        "saldo_inicial" -> "Initial balance"
        "saldo_final" -> "Final balance"
        "asignar_saldo_inicial" -> "Assign initial balance"
        "asignar_automaticamente" -> "Assign automatically"
        "realizar_arqueo" -> "Perform cash count"
        "arqueo_caja" -> "Cash count"
        "saldo_asignado" -> "Initial balance assigned"
        "arqueo_realizado" -> "Cash count completed successfully"
        "sin_saldo_anterior" -> "No previous markets with available balance"
        "saldo_negativo" -> "Balance cannot be negative"
        "saldo_muy_alto" -> "Balance is too high"

        // Confirmations
        "confirmar_eliminar_mercadillo" -> "Are you sure you want to delete this market?"
        "confirmar_cancelar_mercadillo" -> "Are you sure you want to cancel this market?"
        "mercadillo_tiene_ventas" -> "Cannot cancel: market has sales"
        "mercadillo_estado_no_valido" -> "Cannot cancel: invalid status"

        "informacion_basica" -> "Basic Information"
        "horarios" -> "Schedule"
        "configuracion_mercadillo" -> "Market Configuration"
        "crear_mercadillo" -> "Create Market"
        "actualizar_mercadillo" -> "Update Market"
        "guardando" -> "Saving..."
        "saldo_inicial_ayuda" -> "Money available at the start of the market"
        "seleccionar_fecha" -> "Select date"
        "seleccionar_hora_inicio" -> "Select start time"
        "seleccionar_hora_fin" -> "Select end time"

        // English validations
        "fecha_no_puede_estar_vacia" -> "Date cannot be empty"
        "lugar_no_puede_estar_vacio" -> "Location cannot be empty"
        "organizador_no_puede_estar_vacio" -> "Organizer cannot be empty"
        "formato_fecha_invalido" -> "Invalid date format (dd-MM-yyyy)"
        "formato_hora_inicio_invalido" -> "Invalid start time format (HH:mm)"
        "formato_hora_fin_invalido" -> "Invalid end time format (HH:mm)"
        "hora_inicio_debe_ser_anterior" -> "Start time must be before end time"
        "lugar_minimo_caracteres" -> "Location must have at least 3 characters"
        "lugar_maximo_caracteres" -> "Location cannot have more than 100 characters"
        "organizador_minimo_caracteres" -> "Organizer must have at least 3 characters"
        "organizador_maximo_caracteres" -> "Organizer cannot have more than 100 characters"
        "importe_no_puede_ser_negativo" -> "Amount cannot be negative"
        "importe_demasiado_alto" -> "Amount is too high"
        "saldo_inicial_no_puede_ser_negativo" -> "Initial balance cannot be negative"
        "saldo_inicial_demasiado_alto" -> "Initial balance is too high"

        "seleccionar_hora" -> "Select time"
        "opcional" -> "(optional)"
        "lunes" -> "M"
        "martes" -> "T"
        "miercoles" -> "W"
        "jueves" -> "T"
        "viernes" -> "F"
        "sabado" -> "S"
        "domingo" -> "S"

        else -> key
    }
}

