package com.phoneguard.ui.screens.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.phoneguard.R
import com.phoneguard.ui.components.BackTopBar
import com.phoneguard.ui.components.EmptyState
import com.phoneguard.ui.util.SettingsLauncher
import com.phoneguard.ui.util.findActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scans a QR code with CameraX and ML Kit (fully on the device) and returns its text.
 * The camera permission is explained before Android asks for it.
 */
@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = context.hasCameraPermission() }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.qr_title), onBack) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                cameraFailed -> EmptyState(
                    icon = Icons.Rounded.NoPhotography,
                    title = stringResource(R.string.qr_camera_error_title),
                    body = stringResource(R.string.qr_camera_error_body),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                    modifier = Modifier.align(Alignment.Center),
                )
                hasPermission -> {
                    QrCameraView(
                        onCode = onResult,
                        onError = { cameraFailed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                    ViewfinderOverlay(Modifier.fillMaxSize())
                    Text(
                        stringResource(R.string.qr_instruction),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(32.dp),
                    )
                }
                permanentlyDenied -> EmptyState(
                    icon = Icons.Rounded.CameraAlt,
                    title = stringResource(R.string.qr_permission_title),
                    body = stringResource(R.string.qr_permission_denied_body),
                    actionLabel = stringResource(R.string.action_open_settings),
                    onAction = { SettingsLauncher.openOwnAppSettings(context) },
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> EmptyState(
                    icon = Icons.Rounded.CameraAlt,
                    title = stringResource(R.string.qr_permission_title),
                    body = stringResource(R.string.qr_permission_body),
                    actionLabel = stringResource(R.string.action_allow_camera),
                    onAction = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun QrCameraView(
    onCode: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnError by rememberUpdatedState(onError)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val delivered = AtomicBoolean(false)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(
                        executor,
                        QrAnalyzer(scanner) { value ->
                            if (delivered.compareAndSet(false, true)) {
                                mainExecutor.execute { currentOnCode(value) }
                            }
                        },
                    )
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    currentOnError()
                }
            },
            mainExecutor,
        )

        onDispose {
            provider?.unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Sends each camera frame to ML Kit and reports the first QR code found. */
private class QrAnalyzer(
    private val scanner: BarcodeScanner,
    private val onFound: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @OptIn(markerClass = [ExperimentalGetImage::class])
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf { value -> value.isNotBlank() } }?.let(onFound)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

/** Dims everything except a rounded square, with an animated scan line. */
@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val frameColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "qrLine")
    val line by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Reverse),
        label = "line",
    )
    Canvas(modifier) {
        val side = size.minDimension * 0.68f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2.4f
        val corner = 28.dp.toPx()
        val window = Path().apply {
            addRoundRect(RoundRect(Rect(Offset(left, top), Size(side, side)), CornerRadius(corner, corner)))
        }
        clipPath(window, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.55f))
        }
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 4.dp.toPx()),
        )
        val y = top + side * line
        drawLine(
            color = frameColor.copy(alpha = 0.9f),
            start = Offset(left + 16.dp.toPx(), y),
            end = Offset(left + side - 16.dp.toPx(), y),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
