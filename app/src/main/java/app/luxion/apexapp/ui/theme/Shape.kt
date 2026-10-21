package app.luxion.apexapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Una sola decisión de radio: esquinas casi rectas en todo.
private val Radius = 4.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(Radius),
    small = RoundedCornerShape(Radius),
    medium = RoundedCornerShape(Radius),
    large = RoundedCornerShape(Radius),
    extraLarge = RoundedCornerShape(Radius),
)
