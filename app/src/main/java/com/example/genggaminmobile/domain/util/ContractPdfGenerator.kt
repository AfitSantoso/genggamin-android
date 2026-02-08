package com.example.genggaminmobile.domain.util

import android.content.Context
import android.graphics.Bitmap
import com.example.genggaminmobile.R
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data class containing all information needed to generate a loan contract PDF
 */
data class ContractData(
    val customerName: String,
    val customerNik: String,
    val customerAddress: String,
    val amount: Long,
    val tenor: Int,
    val interestRate: Double,
    val monthlyInstallment: Long,
    val totalRepayment: Long,
    val purpose: String,
    val signatureBitmap: Bitmap,
)

/**
 * Utility class for generating loan contract PDFs with iText7
 */
object ContractPdfGenerator {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

    private val primaryColor = DeviceRgb(25, 118, 210) // Material Blue

    /**
     * Generate a loan contract PDF file
     * @param context Android context for accessing resources
     * @param contractData All contract information
     * @return Generated PDF file in cache directory
     */
    fun generateContractPdf(context: Context, contractData: ContractData): File {
        val contractNumber = "LOAN/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}/${UUID.randomUUID().toString().take(8).uppercase()}"
        val outputFile = File(context.cacheDir, "contract_${System.currentTimeMillis()}.pdf")

        val writer = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument, PageSize.A4).apply {
            setMargins(50f, 50f, 50f, 50f)
        }

        try {
            // Title
            addTitle(document, context.getString(R.string.contract_title))
            addSubtitle(document, context.getString(R.string.contract_number_prefix) + contractNumber)
            addSpacer(document)

            // Parties Section
            addSectionTitle(document, context.getString(R.string.contract_parties_title))
            addPartiesInfo(document, context, contractData)
            addSpacer(document)

            // Loan Terms Section
            addSectionTitle(document, context.getString(R.string.contract_terms_title))
            addLoanTermsTable(document, context, contractData)
            addSpacer(document)

            // Obligations Section
            addSectionTitle(document, context.getString(R.string.contract_obligations_title))
            addObligations(document, context)
            addSpacer(document)

            // Penalty Section
            addSectionTitle(document, context.getString(R.string.contract_penalty_title))
            addParagraph(document, context.getString(R.string.contract_penalty_text))
            addSpacer(document)

            // Default Section
            addSectionTitle(document, context.getString(R.string.contract_default_title))
            addParagraph(document, context.getString(R.string.contract_default_text))
            addSpacer(document)

            // Closing Section
            addSectionTitle(document, context.getString(R.string.contract_closing_title))
            addParagraph(document, context.getString(R.string.contract_closing_text))
            addSpacer(document)

            // Date and Place
            val currentDate = dateFormatter.format(Date())
            addParagraph(document, context.getString(R.string.contract_date_place, currentDate))
            addSpacer(document)

            // Signatures
            addSignatures(document, context, contractData)
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun addTitle(document: Document, text: String) {
        document.add(
            Paragraph(text)
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(primaryColor),
        )
    }

    private fun addSubtitle(document: Document, text: String) {
        document.add(
            Paragraph(text)
                .setFontSize(11f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY),
        )
    }

    private fun addSectionTitle(document: Document, text: String) {
        document.add(
            Paragraph(text)
                .setFontSize(12f)
                .setBold()
                .setFontColor(primaryColor)
                .setMarginTop(10f),
        )
    }

    private fun addParagraph(document: Document, text: String) {
        document.add(
            Paragraph(text)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.JUSTIFIED),
        )
    }

    private fun addSpacer(document: Document) {
        document.add(Paragraph("\n").setFontSize(6f))
    }

    private fun addPartiesInfo(document: Document, context: Context, contractData: ContractData) {
        // First Party
        document.add(
            Paragraph(context.getString(R.string.contract_party_first))
                .setFontSize(10f)
                .setBold(),
        )
        document.add(
            Paragraph("Nama: ${context.getString(R.string.contract_party_first_name)}")
                .setFontSize(10f)
                .setMarginLeft(20f),
        )
        document.add(
            Paragraph("Alamat: ${context.getString(R.string.contract_party_first_address)}")
                .setFontSize(10f)
                .setMarginLeft(20f),
        )

        addSpacer(document)

        // Second Party (Customer)
        document.add(
            Paragraph(context.getString(R.string.contract_party_second))
                .setFontSize(10f)
                .setBold(),
        )
        document.add(
            Paragraph("Nama: ${contractData.customerName}")
                .setFontSize(10f)
                .setMarginLeft(20f),
        )
        document.add(
            Paragraph("NIK: ${contractData.customerNik}")
                .setFontSize(10f)
                .setMarginLeft(20f),
        )
        document.add(
            Paragraph("Alamat: ${contractData.customerAddress}")
                .setFontSize(10f)
                .setMarginLeft(20f),
        )
    }

    private fun addLoanTermsTable(document: Document, context: Context, contractData: ContractData) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
            .useAllAvailableWidth()
            .setMarginTop(10f)

        // Helper function to add rows
        fun addRow(label: String, value: String) {
            table.addCell(
                Cell().add(Paragraph(label).setFontSize(10f).setBold())
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5f),
            )
            table.addCell(
                Cell().add(Paragraph(": $value").setFontSize(10f))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5f),
            )
        }

        addRow(context.getString(R.string.contract_term_amount), currencyFormatter.format(contractData.amount))
        addRow(context.getString(R.string.contract_term_tenor), context.getString(R.string.contract_term_tenor_months, contractData.tenor))
        addRow(context.getString(R.string.contract_term_interest), context.getString(R.string.contract_term_interest_percent, contractData.interestRate))
        addRow(context.getString(R.string.contract_term_installment), currencyFormatter.format(contractData.monthlyInstallment))
        addRow(context.getString(R.string.contract_term_total), currencyFormatter.format(contractData.totalRepayment))
        addRow(context.getString(R.string.contract_term_purpose), contractData.purpose)

        document.add(table)
    }

    private fun addObligations(document: Document, context: Context) {
        val dueDate = "5" // Default due date
        document.add(
            Paragraph(context.getString(R.string.contract_obligation_1, dueDate))
                .setFontSize(10f)
                .setMarginLeft(10f),
        )
        document.add(
            Paragraph(context.getString(R.string.contract_obligation_2))
                .setFontSize(10f)
                .setMarginLeft(10f),
        )
        document.add(
            Paragraph(context.getString(R.string.contract_obligation_3))
                .setFontSize(10f)
                .setMarginLeft(10f),
        )
        document.add(
            Paragraph(context.getString(R.string.contract_obligation_4))
                .setFontSize(10f)
                .setMarginLeft(10f),
        )
    }

    private fun addSignatures(document: Document, context: Context, contractData: ContractData) {
        val signatureTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .useAllAvailableWidth()

        // First Party Signature
        val firstPartyCell = Cell()
            .add(
                Paragraph(context.getString(R.string.contract_signature_first_party))
                    .setFontSize(10f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER),
            )
            .add(Paragraph("\n\n\n").setFontSize(10f))
            .add(
                Paragraph(context.getString(R.string.contract_signature_company))
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER),
            )
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)

        // Second Party Signature with customer's signature image
        val signatureBytes = bitmapToByteArray(contractData.signatureBitmap)
        val signatureImage = Image(ImageDataFactory.create(signatureBytes))
            .scaleToFit(150f, 80f)
            .setHorizontalAlignment(HorizontalAlignment.CENTER)

        val secondPartyCell = Cell()
            .add(
                Paragraph(context.getString(R.string.contract_signature_second_party))
                    .setFontSize(10f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER),
            )
            .add(signatureImage)
            .add(
                Paragraph("(${contractData.customerName})")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER),
            )
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)

        signatureTable.addCell(firstPartyCell)
        signatureTable.addCell(secondPartyCell)

        document.add(signatureTable)
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
