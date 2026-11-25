package com.example.tracey.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.tracey.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.Locale
import kotlin.math.abs

private data class ParsedReceiptResult(
    val merchant: String,
    val amount: Double?,
    val category: String,
    val confidenceNote: String
)

private data class OcrRowLine(
    val text: String,
    val centerY: Float,
    val height: Float,
    val leftX: Float,
    val rightX: Float
)

@Composable
fun ReceiptScannerDialog(
    wallets: List<String>,
    initialWallet: String,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        title: String,
        amount: Double,
        type: String,
        wallet: String,
        category: String,
        status: String,
        dateMillis: Long,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Take a clear photo or pick a receipt from Gallery.") }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var selectedWallet by remember { mutableStateOf(initialWallet) }
    var category by remember { mutableStateOf("Food") }

    fun applyParsedResult(parsed: ParsedReceiptResult) {
        if (parsed.merchant.isNotBlank()) title = parsed.merchant
        if (parsed.amount != null && parsed.amount > 0.0) {
            amountText = String.format(Locale.US, "%.2f", parsed.amount)
            scanStatus = parsed.confidenceNote
        } else {
            scanStatus = "Could not detect total clearly—please verify amount below."
        }
        category = parsed.category
        isScanning = false
    }

    fun processImageUri(uri: Uri) {
        isScanning = true
        scanStatus = "Reading receipt rows & verifying total..."
        previewBitmap = loadScaledPreviewBitmap(context, uri)

        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    applyParsedResult(parseReceiptPrecisely(visionText))
                }
                .addOnFailureListener {
                    isScanning = false
                    scanStatus = "Could not read receipt. Try better lighting or enter manually."
                }
        } catch (e: Exception) {
            isScanning = false
            scanStatus = "Unable to load receipt image."
        }
    }

    fun processBitmapFallback(bitmap: Bitmap) {
        isScanning = true
        scanStatus = "Reading receipt..."
        previewBitmap = bitmap
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                applyParsedResult(parseReceiptPrecisely(visionText))
            }
            .addOnFailureListener {
                isScanning = false
                scanStatus = "Could not read receipt. Try better lighting."
            }
    }

    val fullResCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    val fallbackCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) processBitmapFallback(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) processImageUri(uri)
    }

    fun startCameraIntent() {
        val uri = runCatching {
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val photoFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        }.getOrNull()

        if (uri != null) {
            pendingCameraUri = uri
            try {
                fullResCameraLauncher.launch(uri)
            } catch (_: SecurityException) {
                scanStatus = "Camera permission denied. Enable Camera in App Settings or use Gallery."
            } catch (_: Exception) {
                fallbackCameraLauncher.launch(null)
            }
        } else {
            fallbackCameraLauncher.launch(null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCameraIntent()
        } else {
            scanStatus = "Camera permission is required to capture receipts."
        }
    }

    fun launchHighResCamera() {
        val manifestRequestsCamera = runCatching {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            info.requestedPermissions?.contains(Manifest.permission.CAMERA) == true
        }.getOrDefault(false)

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (manifestRequestsCamera && !hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCameraIntent()
        }
    }

    val parsedAmount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PureWhiteSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = CharcoalPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Receipt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { launchHighResCamera() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera")
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Scanned Receipt",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PersianMistBackground
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(scanStatus, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Expense", "Income").forEach { itemType ->
                        val active = type == itemType
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) CharcoalPrimary else PersianCoatGrey,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = itemType }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = itemType,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else DeepGraphite
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Merchant / Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Extracted Amount (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Target Wallet", style = MaterialTheme.typography.labelMedium, color = MutedFurGrey)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEach { w ->
                        val selected = selectedWallet == w
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) CharcoalPrimary else PureWhiteSurface,
                            border = BorderStroke(1.dp, if (selected) CharcoalPrimary else SilverBorder),
                            modifier = Modifier.clickable { selectedWallet = w }
                        ) {
                            Text(
                                text = w,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else DeepGraphite
                            )
                        }
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelMedium, color = MutedFurGrey)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cats = if (type == "Expense") {
                        listOf("Food", "Transport", "Bills", "School", "Shopping", "Other")
                    } else {
                        listOf("Allowance", "Salary", "Freelance", "Gift", "Other")
                    }
                    cats.forEach { cat ->
                        val selected = category == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) CharcoalPrimary else PureWhiteSurface,
                            border = BorderStroke(1.dp, if (selected) CharcoalPrimary else SilverBorder),
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else DeepGraphite
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MutedFurGrey) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = parsedAmount > 0.0,
                        onClick = {
                            if (parsedAmount > 0.0) {
                                onSaveTransaction(
                                    title.ifBlank { "Scanned Receipt" },
                                    parsedAmount,
                                    type,
                                    selectedWallet,
                                    category,
                                    "Paid",
                                    System.currentTimeMillis(),
                                    "Scanned Receipt"
                                )
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                    ) {
                        Text("Save to Wallet")
                    }
                }
            }
        }
    }
}

private fun loadScaledPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull()
}

private fun parseReceiptPrecisely(visionText: Text): ParsedReceiptResult {
    val rows = visionText.textBlocks
        .flatMap { it.lines }
        .mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            OcrRowLine(
                text = line.text.trim(),
                centerY = box.exactCenterY(),
                height = box.height().toFloat().coerceAtLeast(16f),
                leftX = box.left.toFloat(),
                rightX = box.right.toFloat()
            )
        }
        .sortedBy { it.centerY }

    val maxRowY = rows.maxOfOrNull { it.centerY } ?: 1000f
    val moneyRegex = Regex("""(?:₱|PHP\s*)?(\d{1,3}(?:,\d{3})*\.\d{2}|\d+\.\d{2})(?=[VvTtZzEe\s)*]|$)""")

    fun extractNumbers(text: String): List<Double> {
        return moneyRegex.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0.0 && it < 1_000_000.0 }
            .toList()
    }

    fun findAmountAlignedWith(anchor: OcrRowLine): Double? {
        val inlineNums = extractNumbers(anchor.text)
        if (inlineNums.isNotEmpty()) return inlineNums.last()

        val verticalTolerance = maxOf(anchor.height * 1.8f, maxRowY * 0.015f, 36f)
        val sameRowCandidates = rows.filter { candidate ->
            candidate !== anchor &&
                    abs(candidate.centerY - anchor.centerY) <= verticalTolerance &&
                    candidate.rightX >= anchor.leftX
        }.sortedByDescending { it.rightX }

        for (candidate in sameRowCandidates) {
            val nums = extractNumbers(candidate.text)
            if (nums.isNotEmpty()) return nums.last()
        }
        return null
    }

    val knownMerchants = listOf(
        "7-ELEVEN" to "Food",
        "JOLLIBEE" to "Food",
        "MCDONALD" to "Food",
        "CHOWKING" to "Food",
        "MANG INASAL" to "Food",
        "GREENWICH" to "Food",
        "STARBUCKS" to "Food",
        "ALFAMART" to "Food",
        "MINISTOP" to "Food",
        "UNCLE JOHN" to "Food",
        "PUREGOLD" to "Food",
        "SM SUPERMARKET" to "Food",
        "WATSONS" to "Shopping",
        "MERCURY DRUG" to "Shopping",
        "SM STORE" to "Shopping",
        "ROBINSONS" to "Shopping",
        "GRAB" to "Transport",
        "SHELL" to "Transport",
        "PETRON" to "Transport",
        "CALTEX" to "Transport",
        "MERALCO" to "Bills",
        "PLDT" to "Bills",
        "GLOBE" to "Bills",
        "SMART" to "Bills"
    )

    val fullUpper = visionText.text.uppercase(Locale.US)
    val matchedMerchant = knownMerchants.firstOrNull { fullUpper.contains(it.first) }

    val merchantName = matchedMerchant?.first ?: rows.firstOrNull { row ->
        val upper = row.text.uppercase(Locale.US)
        row.text.length in 3..34 &&
                row.text.any { it.isLetter() } &&
                !upper.contains("CORP") &&
                !upper.contains("OPERATED BY") &&
                !upper.contains("VAT") &&
                !upper.contains("TIN") &&
                !upper.contains("RECEIPT") &&
                !upper.contains("INVOICE") &&
                !upper.contains("PERMIT") &&
                !upper.contains("TEL")
    }?.text ?: "Scanned Receipt"

    val totalKeywordRegex = Regex(
        """\b(?:grand\s*total|amount\s*due|total\s*due|balance\s*due|total\s*amount|net\s*amount|total\s*sale|total\s*php|total\s*peso|total)\b""",
        RegexOption.IGNORE_CASE
    )
    val excludedTotalLabels = listOf(
        "subtotal", "sub-total", "sub total", "vat", "tax", "zero_rated",
        "exempt", "discount", "change", "cash", "savings"
    )

    var detectedTotal: Double? = null
    var note = "Receipt scanned! Review and save to your wallet."

    val totalAnchor = rows.lastOrNull { row ->
        val lower = row.text.lowercase(Locale.US)
        totalKeywordRegex.containsMatchIn(lower) && excludedTotalLabels.none { lower.contains(it) }
    }
    if (totalAnchor != null) {
        detectedTotal = findAmountAlignedWith(totalAnchor)
        if (detectedTotal != null) {
            note = "Total matched from receipt! Review and save to your wallet."
        }
    }

    val cashAnchor = rows.firstOrNull { row ->
        val lower = row.text.lowercase(Locale.US)
        (lower.contains("cash") || lower.contains("tendered")) && !lower.contains("gcash")
    }
    val changeAnchor = rows.firstOrNull { row ->
        row.text.lowercase(Locale.US).contains("change")
    }

    val cashVal = cashAnchor?.let { findAmountAlignedWith(it) }
    val changeVal = changeAnchor?.let { findAmountAlignedWith(it) }

    if (cashVal != null && changeVal != null && cashVal > changeVal) {
        val computedFromCash = cashVal - changeVal
        if (detectedTotal == null || abs(detectedTotal - computedFromCash) > 0.05) {
            detectedTotal = computedFromCash
            note = "Verified via Cash (₱${String.format(Locale.US, "%.2f", cashVal)}) − Change (₱${String.format(Locale.US, "%.2f", changeVal)})."
        }
    }

    if (detectedTotal == null) {
        val ignoreRowKeywords = listOf(
            "cash", "change", "tendered", "vat", "tax", "zero_rated",
            "exempt", "tin", "min", "sn#", "rcpt", "permit", "accr", "tel"
        )
        detectedTotal = rows
            .filter { row ->
                val lower = row.text.lowercase(Locale.US)
                ignoreRowKeywords.none { lower.contains(it) }
            }
            .flatMap { extractNumbers(it.text) }
            .maxOrNull()
    }

    val detectedCategory = matchedMerchant?.second ?: when {
        listOf("coffee", "cafe", "burger", "chicken", "rice", "meal", "snack", "drinks", "ice").any {
            fullUpper.contains(it.uppercase(Locale.US))
        } -> "Food"
        listOf("fare", "fuel", "gas", "parking", "toll").any {
            fullUpper.contains(it.uppercase(Locale.US))
        } -> "Transport"
        else -> "Food"
    }

    return ParsedReceiptResult(
        merchant = merchantName,
        amount = detectedTotal,
        category = detectedCategory,
        confidenceNote = note
    )
}