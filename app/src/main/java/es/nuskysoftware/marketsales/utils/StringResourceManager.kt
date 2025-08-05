package es.nuskysoftware.marketsales.utils

import java.util.Locale

/**
 * Gestor de recursos de string que permite cambio dinámico de idioma
 * V8 - Con strings para Google Auth
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

        // Diálogos
        "cambiar" -> "Cambiar"
        "cancelar" -> "Cancelar"
        "guardar" -> "Guardar"
        "aceptar" -> "Aceptar"
        "nueva_contrasena" -> "Nueva contraseña"
        "confirmar_contrasena" -> "Confirmar contraseña"
        "contrasenas_no_coinciden" -> "Las contraseñas no coinciden"
        "contrasena_muy_corta" -> "La contraseña debe tener al menos 6 caracteres"

        // Mensajes
        "funcion_premium" -> "Esta función está disponible solo en la versión Premium"
        "contrasena_actualizada" -> "Contraseña actualizada correctamente"
        "error_actualizacion" -> "Error al actualizar la configuración"

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

        // Dialogs
        "cambiar" -> "Change"
        "cancelar" -> "Cancel"
        "guardar" -> "Save"
        "aceptar" -> "Accept"
        "nueva_contrasena" -> "New password"
        "confirmar_contrasena" -> "Confirm password"
        "contrasenas_no_coinciden" -> "Passwords don't match"
        "contrasena_muy_corta" -> "Password must be at least 6 characters"

        // Messages
        "funcion_premium" -> "This feature is available only in Premium version"
        "contrasena_actualizada" -> "Password updated successfully"
        "error_actualizacion" -> "Error updating configuration"

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

        else -> key
    }
}

//// app/src/main/java/es/nuskysoftware/marketsales/utils/StringResourceManager.kt
//package es.nuskysoftware.marketsales.utils
//
//import java.util.Locale
//
///**
// * Gestor de recursos de string que permite cambio dinámico de idioma
// */
//object StringResourceManager {
//
//    /**
//     * Obtiene un string localizado según el idioma configurado
//     */
//    fun getString(key: String, language: String = "es"): String {
//        return when (language) {
//            "en" -> getEnglishString(key)
//            "es" -> getSpanishString(key)
//            else -> getSpanishString(key)
//        }
//    }
//
//    /**
//     * Strings en español
//     */
//    private fun getSpanishString(key: String): String = when (key) {
//        // Pantalla principal
//        "app_name" -> "Market Sales"
//        "mercadillos" -> "Mercadillos"
//        "configuracion" -> "Configuración"
//        "articulos" -> "Artículos"
//        "categorias" -> "Categorías"
//        "inventario" -> "Inventario"
//        "listados" -> "Listados"
//        "iniciar_sesion" -> "Iniciar Serión"
//        "cerrar_sesion" -> "Cerrar Sesión"
//        "salir" -> "Salir"
//
//        // Configuración
//        "informacion_cuenta" -> "Información de Cuenta"
//        "cambiar_contrasena" -> "Cambiar Contraseña"
//        "tema" -> "Tema"
//        "fuente" -> "Fuente"
//        "idioma" -> "Idioma"
//        "moneda" -> "Moneda"
//        "version_premium" -> "PREMIUM"
//        "version_free" -> "FREE"
//        "promocion_premium" -> "¡Actualiza a Premium!"
//        "desbloquea_funciones" -> "Desbloquea todas las funciones"
//
//        // Tema
//        "tema_claro" -> "Claro"
//        "tema_oscuro" -> "Oscuro"
//
//        // Idiomas
//        "espanol" -> "Español"
//        "ingles" -> "English"
//
//        // Fuentes
//        "montserrat" -> "Montserrat"
//        "poppins" -> "Poppins"
//        "roboto" -> "Roboto"
//
//        // Monedas
//        "euro" -> "€ Euro"
//        "dolar" -> "$ Dólar"
//        "libra" -> "£ Libra"
//        "yen" -> "¥ Yen"
//
//        // Diálogos
//        "cambiar" -> "Cambiar"
//        "cancelar" -> "Cancelar"
//        "guardar" -> "Guardar"
//        "aceptar" -> "Aceptar"
//        "nueva_contrasena" -> "Nueva contraseña"
//        "confirmar_contrasena" -> "Confirmar contraseña"
//        "contrasenas_no_coinciden" -> "Las contraseñas no coinciden"
//        "contrasena_muy_corta" -> "La contraseña debe tener al menos 6 caracteres"
//
//        // Mensajes
//        "funcion_premium" -> "Esta función está disponible solo en la versión Premium"
//        "contrasena_actualizada" -> "Contraseña actualizada correctamente"
//        "error_actualizacion" -> "Error al actualizar la configuración"
//
//        // ✅ AGREGADO: STRINGS DE AUTENTICACIÓN FASE 2
//        "login_subtitle" -> "Inicia sesión en tu cuenta"
//        "register_subtitle" -> "Crea tu nueva cuenta"
//        "email" -> "Email"
//        "email_placeholder" -> "tu@email.com"
//        "email_invalid" -> "Email inválido"
//        "password" -> "Contraseña"
//        "password_placeholder" -> "Mínimo 6 caracteres"
//        "password_invalid" -> "Mínimo 6 caracteres requeridos"
//        "login_button" -> "Iniciar Sesión"
//        "register_button" -> "Crear Cuenta"
//        "loading" -> "Cargando..."
//        "or" -> "o"
//        "go_to_register" -> "¿No tienes cuenta? Crear cuenta"
//        "go_to_login" -> "¿Ya tienes cuenta? Iniciar sesión"
//        "register_benefits_title" -> "🚀 Beneficios de crear cuenta:"
//        "benefit_1" -> "• Sincronización automática en todos tus dispositivos"
//        "benefit_2" -> "• Configuración personalizada guardada"
//        "benefit_3" -> "• Acceso a funciones Premium"
//        "benefit_4" -> "• Respaldo seguro de tus datos"
//        "welcome" -> "Bienvenido"
//        "premium_user" -> "Usuario Premium"
//        "free_user" -> "Usuario Gratuito"
//        "not_authenticated" -> "No autenticado"
//        "home" -> "Inicio"
//        "markets" -> "Mercadillos"
//        "add_market" -> "Añadir Mercadillo"
//        "premium_features" -> "Funciones Premium"
//        "upgrade_premium" -> "Actualizar a Premium"
//        "about" -> "Acerca de"
//        "logout" -> "Cerrar Sesión"
//        "exit_app" -> "Salir de la App"
//        "logout_title" -> "Cerrar Sesión"
//        "logout_message" -> "¿Estás seguro de que deseas cerrar sesión?"
//        "logout_confirm" -> "Cerrar Sesión"
//        "exit_title" -> "Salir de la App"
//        "exit_message" -> "¿Estás seguro de que deseas salir de Market Sales?"
//        "exit_confirm" -> "Salir"
//        "available_markets" -> "Mercadillos Disponibles"
//        "premium_required" -> "Requiere Premium para acceso completo"
//        "see_details" -> "Ver detalles"
//        "edit" -> "Editar"
//        "menu" -> "Menú"
//        "premium" -> "Premium"
//        "free" -> "Gratuito"
//
//        else -> key
//    }
//
//    /**
//     * Strings en inglés
//     */
//    private fun getEnglishString(key: String): String = when (key) {
//        // Main screen
//        "app_name" -> "Market Sales"
//        "mercadillos" -> "Markets"
//        "configuracion" -> "Settings"
//        "articulos" -> "Articles"
//        "categorias" -> "Categories"
//        "inventario" -> "Inventory"
//        "listados" -> "Reports"
//        "iniciar_sesion" -> "Login"
//        "cerrar_sesion" -> "Logout"
//        "salir" -> "Exit"
//
//        // Settings
//        "informacion_cuenta" -> "Account Information"
//        "cambiar_contrasena" -> "Change Password"
//        "tema" -> "Theme"
//        "fuente" -> "Font"
//        "idioma" -> "Language"
//        "moneda" -> "Currency"
//        "version_premium" -> "PREMIUM"
//        "version_free" -> "FREE"
//        "promocion_premium" -> "Upgrade to Premium!"
//        "desbloquea_funciones" -> "Unlock all features"
//
//        // Theme
//        "tema_claro" -> "Light"
//        "tema_oscuro" -> "Dark"
//
//        // Languages
//        "espanol" -> "Español"
//        "ingles" -> "English"
//
//        // Fonts
//        "montserrat" -> "Montserrat"
//        "poppins" -> "Poppins"
//        "roboto" -> "Roboto"
//
//        // Currencies
//        "euro" -> "€ Euro"
//        "dolar" -> "$ Dollar"
//        "libra" -> "£ Pound"
//        "yen" -> "¥ Yen"
//
//        // Dialogs
//        "cambiar" -> "Change"
//        "cancelar" -> "Cancel"
//        "guardar" -> "Save"
//        "aceptar" -> "Accept"
//        "nueva_contrasena" -> "New password"
//        "confirmar_contrasena" -> "Confirm password"
//        "contrasenas_no_coinciden" -> "Passwords don't match"
//        "contrasena_muy_corta" -> "Password must be at least 6 characters"
//
//        // Messages
//        "funcion_premium" -> "This feature is available only in Premium version"
//        "contrasena_actualizada" -> "Password updated successfully"
//        "error_actualizacion" -> "Error updating configuration"
//
//        // ✅ AGREGADO: AUTHENTICATION STRINGS PHASE 2
//        "login_subtitle" -> "Sign in to your account"
//        "register_subtitle" -> "Create your new account"
//        "email" -> "Email"
//        "email_placeholder" -> "your@email.com"
//        "email_invalid" -> "Invalid email"
//        "password" -> "Password"
//        "password_placeholder" -> "Minimum 6 characters"
//        "password_invalid" -> "Minimum 6 characters required"
//        "login_button" -> "Sign In"
//        "register_button" -> "Create Account"
//        "loading" -> "Loading..."
//        "or" -> "or"
//        "go_to_register" -> "Don't have an account? Create account"
//        "go_to_login" -> "Already have an account? Sign in"
//        "register_benefits_title" -> "🚀 Account benefits:"
//        "benefit_1" -> "• Automatic sync across all your devices"
//        "benefit_2" -> "• Personalized configuration saved"
//        "benefit_3" -> "• Access to Premium features"
//        "benefit_4" -> "• Secure data backup"
//        "welcome" -> "Welcome"
//        "premium_user" -> "Premium User"
//        "free_user" -> "Free User"
//        "not_authenticated" -> "Not authenticated"
//        "home" -> "Home"
//        "markets" -> "Markets"
//        "add_market" -> "Add Market"
//        "premium_features" -> "Premium Features"
//        "upgrade_premium" -> "Upgrade to Premium"
//        "about" -> "About"
//        "logout" -> "Sign Out"
//        "exit_app" -> "Exit App"
//        "logout_title" -> "Sign Out"
//        "logout_message" -> "Are you sure you want to sign out?"
//        "logout_confirm" -> "Sign Out"
//        "exit_title" -> "Exit App"
//        "exit_message" -> "Are you sure you want to exit Market Sales?"
//        "exit_confirm" -> "Exit"
//        "available_markets" -> "Available Markets"
//        "premium_required" -> "Requires Premium for full access"
//        "see_details" -> "See details"
//        "edit" -> "Edit"
//        "menu" -> "Menu"
//        "premium" -> "Premium"
//        "free" -> "Free"
//
//        else -> key
//    }
//}