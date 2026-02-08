package com.example.genggaminmobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.genggaminmobile.R

/**
 * Data class representing a single point in the signature path
 */
data class SignaturePoint(
    val x: Float,
    val y: Float,
    val isStartOfStroke: Boolean = false,
)

/**
 * A composable that provides a signature drawing pad with smooth drawing capability.
 * This component completely prevents parent scroll interference during signature drawing
 * by consuming all pointer events within the signature area.
 *
 * @param modifier Modifier for styling the component
 * @param strokeColor Color of the signature stroke
 * @param strokeWidth Width of the signature stroke
 * @param backgroundColor Background color of the signature pad
 * @param onSignatureChanged Callback invoked when the signature changes, provides whether there's content
 * @param signaturePathState External state for managing signature points
 */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 4f,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onSignatureChanged: (hasSignature: Boolean) -> Unit = {},
    signaturePathState: MutableState<List<SignaturePoint>> = remember { mutableStateOf(emptyList()) },
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val signaturePath = signaturePathState.value
    val hasSignature = signaturePath.isNotEmpty()

    // Get the current view to request disallow intercept touch event
    val view = LocalView.current

    LaunchedEffect(hasSignature) {
        onSignatureChanged(hasSignature)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = if (hasSignature) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Wait for initial touch down
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // CRITICAL: Request parent to NOT intercept touch events
                        // This MUST be called on the Android View level to prevent scroll
                        view.parent?.requestDisallowInterceptTouchEvent(true)

                        // Consume the down event
                        down.consume()

                        // Add the starting point
                        val startPoint = SignaturePoint(
                            x = down.position.x,
                            y = down.position.y,
                            isStartOfStroke = true,
                        )
                        signaturePathState.value = signaturePathState.value + startPoint

                        // Track all subsequent moves until release
                        do {
                            val event = awaitPointerEvent()

                            // Keep requesting parent to not intercept
                            view.parent?.requestDisallowInterceptTouchEvent(true)

                            event.changes.forEach { change ->
                                // Consume ALL changes to prevent any scroll
                                change.consume()

                                if (event.type == PointerEventType.Move) {
                                    val movePoint = SignaturePoint(
                                        x = change.position.x,
                                        y = change.position.y,
                                        isStartOfStroke = false,
                                    )
                                    signaturePathState.value = signaturePathState.value + movePoint
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // Release - allow parent to intercept again
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                },
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())

            // Draw the signature path
            if (signaturePath.isNotEmpty()) {
                val path = Path()
                signaturePath.forEachIndexed { index, point ->
                    if (point.isStartOfStroke) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        // Placeholder text when no signature
        if (!hasSignature) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.contract_signature_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Composable that wraps SignaturePad with a clear button
 */
@Composable
fun SignaturePadWithControls(
    modifier: Modifier = Modifier,
    signaturePathState: MutableState<List<SignaturePoint>>,
    onSignatureChanged: (hasSignature: Boolean) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tanda Tangan Digital",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            if (signaturePathState.value.isNotEmpty()) {
                TextButton(
                    onClick = { signaturePathState.value = emptyList() },
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.contract_clear_signature),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SignaturePad(
            signaturePathState = signaturePathState,
            onSignatureChanged = onSignatureChanged,
        )
    }
}

/**
 * Convert the signature path to a Bitmap
 */
fun signaturePathToBitmap(
    signaturePath: List<SignaturePoint>,
    width: Int = 600,
    height: Int = 300,
    strokeColor: Int = android.graphics.Color.BLACK,
    strokeWidth: Float = 4f,
    backgroundColor: Int = android.graphics.Color.WHITE,
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Fill background
    canvas.drawColor(backgroundColor)

    if (signaturePath.isEmpty()) {
        return bitmap
    }

    // Scale factors to fit the signature in the bitmap
    val minX = signaturePath.minOfOrNull { it.x } ?: 0f
    val maxX = signaturePath.maxOfOrNull { it.x } ?: 0f
    val minY = signaturePath.minOfOrNull { it.y } ?: 0f
    val maxY = signaturePath.maxOfOrNull { it.y } ?: 0f

    val signatureWidth = maxX - minX
    val signatureHeight = maxY - minY

    // Add padding
    val padding = 40f
    val availableWidth = width - (2 * padding)
    val availableHeight = height - (2 * padding)

    val scaleX = if (signatureWidth > 0) availableWidth / signatureWidth else 1f
    val scaleY = if (signatureHeight > 0) availableHeight / signatureHeight else 1f
    val scale = minOf(scaleX, scaleY)

    val offsetX = padding + (availableWidth - signatureWidth * scale) / 2 - minX * scale
    val offsetY = padding + (availableHeight - signatureHeight * scale) / 2 - minY * scale

    val paint = android.graphics.Paint().apply {
        color = strokeColor
        this.strokeWidth = strokeWidth * scale
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }

    val path = android.graphics.Path()
    signaturePath.forEach { point ->
        val x = point.x * scale + offsetX
        val y = point.y * scale + offsetY

        if (point.isStartOfStroke) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    canvas.drawPath(path, paint)
    return bitmap
}
