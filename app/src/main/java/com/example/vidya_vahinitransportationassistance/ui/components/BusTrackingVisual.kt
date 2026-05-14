package com.example.vidya_vahinitransportationassistance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.vidya_vahinitransportationassistance.data.MockData
import com.example.vidya_vahinitransportationassistance.data.model.RouteState

/**
 * A composable that visualizes the bus progress along a route.
 */
@Composable
fun BusTrackingVisual(
    routeState: RouteState,
    modifier: Modifier = Modifier
) {
    val stops = routeState.stops
    val currentStopIndex = routeState.currentStopIndex
    
    // Optimization: Use remember and derivedStateOf to prevent unnecessary calculations.
    // Each stop is exactly (1.0 / totalStops) apart.
    val targetProgress by remember(stops.size, currentStopIndex) {
        derivedStateOf {
            if (stops.isNotEmpty()) {
                (currentStopIndex + 1).toFloat() / stops.size
            } else {
                0f
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        label = "BusProgressAnimation"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val busIconSize = 32.dp
        val busIconSizePx = with(LocalDensity.current) { busIconSize.toPx() }

        // Draw the route line and stop circles
        // Responsiveness: Using size.width directly from Canvas scope ensures it works on all screens (720p/1080p)
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val width = size.width
            val centerY = size.height / 2
            
            // 1. Draw horizontal background line (Gray)
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 4.dp.toPx()
            )

            // 2. Draw horizontal progress line (Green)
            drawLine(
                color = Color.Green,
                start = Offset(0f, centerY),
                end = Offset(width * animatedProgress, centerY),
                strokeWidth = 4.dp.toPx()
            )

            // 3. Draw circles for each BusStop
            stops.forEachIndexed { index, _ ->
                val xPos = ((index + 1).toFloat() / stops.size) * width
                
                val circleColor = if (index <= currentStopIndex) Color.Green else Color.LightGray
                
                drawCircle(
                    color = circleColor,
                    radius = 8.dp.toPx(),
                    center = Offset(xPos, centerY)
                )
            }
        }

        // 4. Draw the bus icon moving to the current position
        val isCritical = routeState.status == "Critical"
        Icon(
            imageVector = if (isCritical) Icons.Filled.Warning else Icons.Filled.DirectionsBus,
            contentDescription = if (isCritical) "Warning Icon" else "Bus Icon",
            tint = if (isCritical) Color.Red else Color(0xFF1976D2),
            modifier = Modifier
                .size(busIconSize)
                .offset {
                    // Responsiveness: Offset calculated using constraints and animated progress
                    val xOffset = (animatedProgress * maxWidthPx - busIconSizePx / 2).toInt()
                    IntOffset(xOffset, -24.dp.toPx().toInt())
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BusTrackingVisualPreview() {
    BusTrackingVisual(
        // Fixed: Use getInitialRouteState() instead of the non-existent initialRouteState property.
        // This resolves the compilation error which was causing the NoClassDefFoundError in the preview.
        routeState = MockData.getInitialRouteState().copy(currentStopIndex = 2)
    )
}
