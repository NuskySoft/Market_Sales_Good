package es.nuskysoftware.marketsales.ui.composables

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 🛒 ICONO CARRITO PERSONALIZADO (más grande y visible)
val IconoCarritoCustom: ImageVector
    get() {
        return ImageVector.Builder(
            name = "shopping_cart_custom",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                // Carrito más grueso y visible
                moveTo(7f, 18f)
                curveTo(5.9f, 18f, 5f, 18.9f, 5f, 20f)
                curveTo(5f, 21.1f, 5.9f, 22f, 7f, 22f)
                curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
                curveTo(9f, 18.9f, 8.1f, 18f, 7f, 18f)
                close()

                moveTo(1f, 2f)
                verticalLineTo(4f)
                horizontalLineTo(3f)
                lineTo(6.6f, 11.59f)
                lineTo(5.25f, 14.04f)
                curveTo(5.09f, 14.32f, 5f, 14.65f, 5f, 15f)
                curveTo(5f, 16.1f, 5.9f, 17f, 7f, 17f)
                horizontalLineTo(19f)
                verticalLineTo(15f)
                horizontalLineTo(7.42f)
                curveTo(7.28f, 15f, 7.17f, 14.89f, 7.17f, 14.75f)
                lineTo(7.2f, 14.63f)
                lineTo(8.1f, 13f)
                horizontalLineTo(15.55f)
                curveTo(16.3f, 13f, 16.96f, 12.59f, 17.3f, 11.97f)
                lineTo(20.88f, 5f)
                horizontalLineTo(5.21f)
                lineTo(4.27f, 2f)
                horizontalLineTo(1f)
                close()

                moveTo(17f, 18f)
                curveTo(15.9f, 18f, 15f, 18.9f, 15f, 20f)
                curveTo(15f, 21.1f, 15.9f, 22f, 17f, 22f)
                curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
                curveTo(19f, 18.9f, 18.1f, 18f, 17f, 18f)
                close()
            }
        }.build()
    }

@Composable
fun IconoCarritoGrande(
    modifier: Modifier = Modifier,
    tint: Color = Color.Black
) {
    Icon(
        imageVector = IconoCarritoCustom,
        contentDescription = "Carrito de compras",
        modifier = modifier,
        tint = tint
    )
}