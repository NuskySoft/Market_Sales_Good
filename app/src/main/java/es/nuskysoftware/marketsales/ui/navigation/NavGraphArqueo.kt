// app/src/main/java/es/nuskysoftware/marketsales/ui/navigation/ArqueoNavGraph.kt
/**
 * Graph de navegación para Arqueo:
 *  - arqueo/{mercadilloId}  → PantallaArqueo (hub)
 *  - arqueo/caja/{mercadilloId} → PantallaArqueoCaja
 *  - arqueo/resultado/{mercadilloId} → PantallaResultadoMercadillo
 *  - arqueo/asignar-saldo/{mercadilloId} → PantallaAsignarSaldo
 */
package es.nuskysoftware.marketsales.ui.navigation

import androidx.navigation.compose.composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import es.nuskysoftware.marketsales.ui.pantallas.arqueo.PantallaArqueo
import es.nuskysoftware.marketsales.ui.pantallas.arqueo.PantallaArqueoCaja
import es.nuskysoftware.marketsales.ui.pantallas.arqueo.PantallaResultadoMercadillo
import es.nuskysoftware.marketsales.ui.pantallas.arqueo.PantallaAsignarSaldo

fun NavGraphBuilder.addArqueoGraph(navController: NavController) {
    composable(
        route = "arqueo/{mercadilloId}",
        arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
    ) { backStack ->
        val id = backStack.arguments?.getString("mercadilloId") ?: return@composable
        PantallaArqueo(navController, id)
    }

    composable(
        route = "arqueo/caja/{mercadilloId}",
        arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
    ) { backStack ->
        val id = backStack.arguments?.getString("mercadilloId") ?: return@composable
        PantallaArqueoCaja(navController, id)
    }

    composable(
        route = "arqueo/resultado/{mercadilloId}",
        arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
    ) { backStack ->
        val id = backStack.arguments?.getString("mercadilloId") ?: return@composable
        PantallaResultadoMercadillo(navController, id)
    }

    composable(
        route = "arqueo/asignar-saldo/{mercadilloId}",
        arguments = listOf(navArgument("mercadilloId") { type = NavType.StringType })
    ) { backStack ->
        val id = backStack.arguments?.getString("mercadilloId") ?: return@composable
        PantallaAsignarSaldo(navController, id)
    }
}

