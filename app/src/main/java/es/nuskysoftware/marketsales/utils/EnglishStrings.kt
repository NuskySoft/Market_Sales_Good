// app/src/main/java/es/nuskysoftware/marketsales/utils/EnglishStrings.kt
package es.nuskysoftware.marketsales.utils

internal object EnglishStrings {

    // ui/components/BarraAccionesVenta.kt
    // ui/components/BottomBarMercadillo.kt
    // ui/components/DialogoSeleccionMercadilloActivo.kt
    // ui/components/DialogSelectorColor.kt
    // (y el resto de módulos/pantallas que ya tenías)

    fun get(key: String): String = when (key) {
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
        "utilidades_premium_msg" -> "These options are available only for Premium users"

        "seleccionar_hora" -> "Select time"
        "opcional" -> "(optional)"
        "lunes" -> "M"
        "martes" -> "T"
        "miercoles" -> "W"
        "jueves" -> "T"
        "viernes" -> "F"
        "sabado" -> "S"
        "domingo" -> "S"

        "utilidades" -> "Utilities"
        "usuario" -> "User"
        "usuario_invitado" -> "Guest user"

        "realizar_cargo" -> "Checkout"
        "carrito" -> "Cart"

        // ui/components/BottomBarMercadillo.kt
        "cambiar_mercadillo_seleccionado" -> "Change selected market"
        "mercadillo_activo" -> "Active market:"
        "ventas" -> "Sales"
        "gastos" -> "Expenses"
        "resumen" -> "Summary"

        // ui/components/DialogoSeleccionMercadilloActivo.kt
        "seleccionar_mercadillo_activo" -> "Select active market"
        "varios_mercadillos_en_curso" -> "There are multiple markets in progress. Choose one to continue:"

        // ui/components/DialogSelectorColor.kt
        "seleccionar_un_color" -> "Select a color"
        "color_seleccionado" -> "Selected color"

        // ui/components/DownloadProgressBar.kt
        "procesando" -> "Processing…"

        // ui/components/dialogs/DialogoSeleccionMercadillo.kt
        "seleccionar_mercadillo" -> "Select market"

        // ui/components/gastos/BarraAccionesGasto.kt
        "cargar_gasto" -> "Add expense"

        // ui/components/proximos/CardMercadillosProximos.kt
        "proximos_mercadillos" -> "Upcoming markets"

        // ui/composables/gastos/PestanaGastosAutomaticas.kt
        "gastos_automaticos_proximamente" -> "Automatic expenses — coming soon"

        // ui/composables/gastos/PestanaGastosManual.kt
        "anadir_gasto" -> "Add expense"

        // ui/composables/resumen/PestanaResumenGastos.kt
        "resumen_gastos" -> "Expense summary"
        "total_gastos" -> "Total expenses"

        // ui/composables/resumen/PestanaResumenVentas.kt
        "resumen_ventas" -> "Sales summary"
        "total_ventas" -> "Total sales"
        "confirmar_abono" -> "Confirm refund"
        "confirmar_abono_pregunta" -> "Are you sure you want to refund this line?"
        "cantidad" -> "Quantity"
        "total_linea" -> "Line total"
        "si_abonar" -> "Yes, refund"
        "precio_unitario" -> "Unit price:"
        "abonar" -> "Refund"
        "abono_chip" -> "REFUND"

        // ui/composables/CampoDescripcion.kt
        "descripcion_label" -> "Description *"
        "descripcion_placeholder" -> "Eg: Wooden Dolphin Keyring"
        "descripcion_obligatoria" -> "Description is required"

        // ui/composables/CampoImporte.kt
        "importe" -> "Amount"

        // ui/composables/PestanaVentaManual.kt
        "anadir" -> "Add"
        "anadir_venta" -> "Add sale"

        // ui/composables/PestanaVentaProductos.kt
        "todos_los_productos" -> "All products"
        "buscar" -> "Search"
        "cerrar_busqueda" -> "Close search"
        "buscar_placeholder" -> "Search..."
        "todas" -> "All"
        "inicia_sesion_ver_productos" -> "Log in to view products"
        "no_hay_productos" -> "No products"

        // ui/composables/TecladoNumerico.kt
        "borrar" -> "Delete"

        // screens/arqueo/PantallaArqueo.kt
        "arqueo" -> "Cash Count"
        "cargando_datos_mercadillo" -> "Loading market data…"
        "resumen_mercadillo" -> "Market summary"
        "bizum" -> "Bizum"
        "tarjeta" -> "Card"
        "efectivo" -> "Cash"
        "total" -> "Total"
        "acciones" -> "Actions"
        "resultado_mercadillo" -> "Market result"
        "asignar_saldo" -> "Assign balance"
        "cerrar" -> "Close"
        "guion_largo" -> "—"

        // screens/arqueo/PantallaArqueoCaja.kt
        "ventas_en_efectivo" -> "Cash sales"
        "gastos_en_efectivo" -> "Cash expenses"
        "resultado_arqueo" -> "Cash count result"
        "confirmar_arqueo" -> "Confirm cash count"
        "confirmar" -> "Confirm"
        "confirmar_arqueo_aviso_unavez" -> "The cash count can only be confirmed once"
        "confirmar_arqueo_aviso_guardado" -> "The result will be saved as the final cash balance"
        "confirmar_arqueo_aviso_premium" -> "Premium users can assign that balance to a new market or save it for later"
        "confirmar_arqueo_pregunta" -> "Are you sure you want to confirm the cash count?"

        // screens/arqueo/PantallaAsignarSaldo.kt
        "opcion_solo_premium" -> "This option is only for Premium users."
        "guardar_saldo" -> "Save balance"
        "guardar_saldo_desc" -> "Keep this balance to assign it when you create a market."
        "retirar_efectivo" -> "Withdraw cash"
        "anadir_efectivo" -> "Add cash"
        "importe_a_retirar" -> "Amount to withdraw"
        "importe_a_anadir" -> "Amount to add"
        "selecciona_opcion_importe" -> "Select an option to enter an amount"

        // screens/arqueo/PantallaResultadoMercadillo.kt
        "ventas_por_metodo" -> "Sales by method"
        "gastos_por_metodo" -> "Expenses by method"
        "total_gastos_metodos" -> "Total expenses (methods)"
        "suscripcion" -> "Subscription"


        // screens/gastos/PantallaGastos.kt
        "ingresar_gasto" -> "Enter expense"
        "automaticos" -> "Automatic"

        // screens/gastos/PantallaGastosCarrito.kt
        "gastos_carrito" -> "Expense cart"

        "descripcion" -> "Description"


        // screens/login/LoginScreen.kt

        "contrasena" -> "Password"
        "registrarse" -> "Sign up"
        "o" -> "or"
        "continuar_google" -> "Continue with Google"

        // screens/alta/PantallaAltaMercadillo.kt
        "alta_titulo_editar" -> "Edit Market Day"
        "alta_titulo_nuevo" -> "New Market Day"
        "lugar" -> "Place"
        "placeholder_lugar" -> "e.g., Main Square"
        "organizador" -> "Organizer"
        "placeholder_organizador" -> "e.g., City Council"
        "aviso_saldo_en_curso" -> "Warning: changing the initial cash of a market day IN PROGRESS may cause a mismatch."
        "borrar_mercadillo" -> "Delete market day"
        "actualizar" -> "Update"
        "crear" -> "Create"
        "saldo_pendiente_titulo" -> "Pending balance"
        "saldo_pendiente_texto" -> "You have a saved balance pending assignment. Do you want to assign it to this market day?"
        "si" -> "Yes"
        "no" -> "No"
        "confirmar_cambio_saldo_titulo" -> "Confirm cash change"
        "confirmar_cambio_saldo_linea1" -> "You changed the initial cash of a market day IN PROGRESS. This can cause a cash mismatch."
        "confirmar_cambio_saldo_linea2" -> "Do you want to save anyway?"
        "guardar_igualmente" -> "Save anyway"
        "confirmar_borrado_titulo" -> "Confirm deletion"
        "confirmar_borrado_texto" -> "Are you sure you want to delete the market day?"

        // screens/articles/PantallaArticulos.kt
        "precio_invalido" -> "Invalid sale price"
        "selecciona_categoria" -> "Select a category"
        "sin_categoria" -> "No category"
        "precio" -> "Price"
        "coste" -> "Cost"
        "favorito_estrella" -> "⭐ Favorite"
        "editar" -> "Edit"

        "cambio_titulo" -> "Change"
        "entregado" -> "Given"
        "cambio" -> "Change due"

// utils/EnglishStrings.kt — añadir dentro de when(key)
        "solo_premium" -> "Premium only"
        "expandir" -> "Expand"
        "version_invitado" -> "GUEST"
        "app_version" -> "App {version}"
        "peso_argentino" -> "$ Argentine Peso"
        "peso_mexicano" -> "$ Mexican Peso"
        "peso_colombiano" -> "$ Colombian Peso"
        "sol_peruano" -> "S/ Peruvian Sol"
        "peso_chileno" -> "$ Chilean Peso"
        "bolivar" -> "Bs Venezuelan Bolívar"
        "real_brasileno" -> "$ Brazilian Real"

        // utils/EnglishStrings.kt
        "recibo" -> "Receipt"
        "metodo_pago" -> "Payment method: {metodo}"
        "finalizar_venta" -> "Finalize sale"

        // utils/EnglishStrings.kt — nuevas
        "pendiente_desarrollo" -> "Under development"
        "muy_pronto_gestionar_stock" -> "Soon you'll be able to manage inventory and stock."

        // utils/EnglishStrings.kt — nuevas
        "aqui_listados_informes" -> "Reports and listings will live here."

        // utils/EnglishStrings.kt — nuevas
        "logout_success" -> "Signed out successfully"
        "google_auth_unavailable" -> "Google Auth unavailable — Check your configuration"

        "saldo_guardado" -> "Saved balance"
        "saldos_pendientes_asignar" -> "Pending balance to assign"

        "preparando_datos" -> "Preparing…"
        "comprobando_datos_locales" -> "Checking local data…"
        "descargando_firebase" -> "Downloading data from Firebase…"
        "listo" -> "Done."
        "error_inesperado" -> "Unexpected error"
        "no_se_pudieron_cargar_datos" -> "Could not load data."
        "reintentar" -> "Retry"

        "nueva_venta" -> "New sale"
        "venta_manual" -> "Manual sale"
        "productos" -> "Products"

        "gestion_mercadillos" -> "Market management"

        "mercadillo_no_encontrado" -> "Market not found"
        "error_cargando_datos" -> "Error loading data: {0}"

        "articulo_nombre_duplicado" -> "An item with that name already exists"
        "articulo_creado_ok" -> "Item created successfully"
        "error_creando_articulo" -> "Error creating item: {0}"
        "articulo_actualizado_ok" -> "Item updated successfully"
        "error_actualizando_articulo" -> "Error updating item"
        "error_actualizando_articulo_detalle" -> "Error updating item: {0}"
        "articulo_eliminado_ok" -> "Item deleted successfully"
        "error_eliminando_articulo" -> "Error deleting item"
        "error_eliminando_articulo_detalle" -> "Error deleting item: {0}"
        "sync_completada" -> "Sync completed"
        "sync_error" -> "Sync error"
        "sync_no_se_pudo_completar" -> "Could not complete synchronization"
        "sync_error_detalle" -> "Sync error: {0}"
        "nombre_min_caracteres" -> "Name must be at least 2 characters"
        "nombre_max_caracteres" -> "Name cannot exceed 100 characters"
        "precio_negativo" -> "Price cannot be negative"
        "precio_demasiado_alto" -> "Price is too high"
        "precio_coste_negativo" -> "Cost price cannot be negative"
        "precio_coste_demasiado_alto" -> "Cost price is too high"
        "stock_negativo" -> "Stock cannot be negative"
        "stock_demasiado_alto" -> "Stock is too high"

        "confirmar_guardar_saldo" -> "Are you sure you want to save the balance?"
        "guardar_saldo_uso" -> "It can be used as the opening balance when creating a new market."
        "guardar_saldo_reemplazar_aviso" -> "An opening balance is already saved. Are you sure you want to replace it? This operation cannot be undone."
        "guardar_saldo_limite" -> "If you save it, you will only be able to assign it when creating a new market."
        "confirmar_asignar_saldo" -> "Are you sure you want to assign the opening balance to this market?"
        "destino_con_saldo_pregunta" -> "The selected market already has an opening balance. Do you want to replace it?"
        "operacion_no_se_puede_deshacer" -> "This operation cannot be undone."

        "categoria_creada_ok" -> "Category created successfully"
        "error_creando_categoria_detalle" -> "Error creating category: {0}"
        "categoria_actualizada_ok" -> "Category updated successfully"
        "error_actualizando_categoria" -> "Error updating category"
        "error_actualizando_categoria_detalle" -> "Error updating category: {0}"
        "categoria_eliminada_ok" -> "Category deleted successfully"
        "error_eliminando_categoria" -> "Error deleting category"
        "error_eliminando_categoria_detalle" -> "Error deleting category: {0}"
        "error_en_sincronizacion" -> "Sync error"
        "no_se_pudo_completar_sincronizacion" -> "Could not complete sync"
        "error_en_sincronizacion_detalle" -> "Sync error: {0}"
        "nombre_min_2" -> "Name must be at least 2 characters"
        "nombre_max_50" -> "Name cannot be longer than 50 characters"

        "gastos_cargados" -> "Expenses saved"
        "error_guardando_gastos" -> "Error saving expenses"

// EnglishStrings.get(...)
        "mercadillo_creado_con_saldo" -> "Market created with starting balance of {0}"
        "mercadillo_creado_ok" -> "Market created successfully"
        "error_creando_mercadillo" -> "Error creating market: {0}"
        "error_cargando_mercadillo" -> "Error loading market: {0}"
        "confirmar_cambio_saldo_en_curso" -> "You are modifying the starting balance of a market in progress. This may cause a cash mismatch.\n\nDo you want to save anyway?"
        "mercadillo_actualizado_ok" -> "Market updated successfully"
        "error_actualizando_mercadillo" -> "Error updating market"
        "error_eliminando_mercadillo" -> "Error deleting market"
        "error_eliminando_mercadillo_detalle" -> "Error deleting market: {0}"
        "no_hay_mercadillos_en_curso" -> "There are no markets in progress"
        "sincronizacion_completada_sin_cambios" -> "Sync completed (no automatic changes)"
        "error_sincronizacion_detalle" -> "Sync error: {0}"
        "usuario_no_autenticado" -> "User not authenticated"
        "estado_cambiado_a" -> "State changed to: {0}"
        "error_cambiando_estado" -> "Error changing state"
        "error_cambiando_estado_detalle" -> "Error changing state: {0}"
        "estado_mercadillo_no_valido" -> "Invalid market state"
        "no_modificar_estado" -> "You cannot modify a market in state: {0}"
        "no_modificar_fecha_en_curso" -> "You cannot change the date of a market in progress"
        "valid_fecha_vacia" -> "Date cannot be empty"
        "valid_formato_fecha_invalido" -> "Invalid date format (dd-MM-yyyy)"
        "valid_fecha_invalida" -> "Invalid date"
        "valid_fecha_pasada" -> "You cannot create a market on a date earlier than today"
        "valid_error_disponibilidad_fecha" -> "Error validating date availability"
        "free_limite_mercadillos_por_dia" -> "FREE users can only create one market per day. Upgrade to Premium to create multiple markets."
        "valid_lugar_vacio" -> "Location cannot be empty"
        "valid_lugar_min" -> "Location must be at least 3 characters"
        "valid_lugar_max" -> "Location cannot be more than 100 characters"
        "valid_organizador_vacio" -> "Organizer cannot be empty"
        "valid_organizador_min" -> "Organizer must be at least 3 characters"
        "valid_organizador_max" -> "Organizer cannot be more than 100 characters"
        "valid_hora_inicio_formato" -> "Invalid start time format (HH:mm)"
        "valid_hora_fin_formato" -> "Invalid end time format (HH:mm)"
        "valid_horarios_invalidos" -> "Invalid times"
        "valid_importe_negativo" -> "Amount cannot be negative"
        "valid_importe_alto" -> "Amount is too high"
        "valid_saldo_inicial_negativo" -> "Starting balance cannot be negative"
        "valid_saldo_inicial_alto" -> "Starting balance is too high"
        "valid_saldo_final_negativo" -> "Final balance cannot be negative"
        "valid_saldo_final_alto" -> "Final balance is too high"

        "error_cargando_proximos_mercadillos" -> "Error loading upcoming markets: {0}"

        "pu_label" -> "Unit:"
        "metodo_efectivo" -> "Cash"
        "metodo_bizum" -> "Bizum"
        "metodo_tarjeta" -> "Card"

        // EnglishStrings.get(...)
        "importe_mayor_cero" -> "Amount must be greater than 0"
        "no_hay_lineas_vender" -> "No line items to sell"
        "total_mayor_cero" -> "Total must be greater than 0"
        "mercadillo_no_inicializado" -> "Market not initialized"
        "error_guardar_venta" -> "Error saving sale: {0}"

        "estado_debug_titulo" -> "🔧 State (DEBUG)"
        "estado_debug_aviso" -> "⚠️ Debug only — Will be removed in production"
        "fecha" -> "Date"
        "formato_fecha_hint" -> "dd-MM-yyyy"
        "config_economica" -> "Financial settings"
        "mercadillo_gratuito" -> "Free market"

        "importe_placeholder" -> "0.00"
        "config_logistica" -> "Logistics settings"


        else -> key
    }
}
