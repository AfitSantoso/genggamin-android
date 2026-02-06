package com.example.genggaminmobile.ui.features.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class FAQItem(
    val question: String,
    val answer: String,
)

data class FAQCategory(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val items: List<FAQItem>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    val categories = remember { getFAQCategories() }

    Scaffold(
        topBar = {
            HelpTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Section
            item {
                HelpHeaderSection()
            }

            // Quick Contact Section
            item {
                QuickContactSection()
            }

            // FAQ Categories
            itemsIndexed(categories) { index, category ->
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + expandVertically(),
                ) {
                    FAQCategoryCard(category = category)
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HelpTopBar(onBack: () -> Unit) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                )
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pusat Bantuan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "Temukan jawaban untuk pertanyaanmu",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpHeaderSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ContactSupport,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Hai, ada yang bisa kami bantu?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih kategori di bawah untuk menemukan jawaban dari pertanyaan yang sering diajukan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickContactSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Butuh bantuan lebih lanjut?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickContactButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Call,
                    label = "Telepon",
                    subtitle = "021-1234567",
                    color = Color(0xFF4CAF50),
                    onClick = { /* Handle phone call */ },
                )
                QuickContactButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Email,
                    label = "Email",
                    subtitle = "cs@genggamin.id",
                    color = Color(0xFF2196F3),
                    onClick = { /* Handle email */ },
                )
            }
        }
    }
}

@Composable
private fun QuickContactButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FAQCategoryCard(category: FAQCategory) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rotation",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else 1.dp,
        ),
    ) {
        Column {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            category.backgroundColor,
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        tint = category.iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${category.items.size} pertanyaan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Tutup" else "Buka",
                    modifier = Modifier.rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // FAQ Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    category.items.forEachIndexed { index, item ->
                        FAQItemCard(item = item)
                        if (index < category.items.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FAQItemCard(item: FAQItem) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "faq_rotation",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = if (isExpanded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = item.answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Justify,
                        )
                    }
                }
            }
        }
    }
}

private fun getFAQCategories(): List<FAQCategory> {
    return listOf(
        FAQCategory(
            title = "Pendaftaran & Akun",
            icon = Icons.Default.Person,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
            items = listOf(
                FAQItem(
                    question = "Bagaimana cara ganti nomor HP?",
                    answer = "Untuk mengganti nomor HP, silakan buka menu Profil > Edit Profil > Ubah Nomor HP.",
                ),
                FAQItem(
                    question = "Kenapa KTP saya ditolak?",
                    answer = "KTP dapat ditolak karena beberapa alasan: (1) Foto buram atau tidak jelas, (2) KTP sudah kadaluarsa, (3) Data tidak sesuai dengan yang tertera, (4) Foto KTP terpotong atau tidak lengkap. Silakan upload ulang dengan foto yang lebih jelas dan pastikan seluruh data KTP terlihat.",
                ),
                FAQItem(
                    question = "Bagaimana cara hapus akun?",
                    answer = "Untuk menghapus akun, silakan hubungi Customer Service kami melalui telepon di 021-1234567 atau email ke cs@genggamin.id. Proses penghapusan akun membutuhkan waktu 3-5 hari kerja. Pastikan tidak ada pinjaman aktif sebelum mengajukan penghapusan akun.",
                ),
                FAQItem(
                    question = "Apakah data saya aman?",
                    answer = "Ya, keamanan data Anda adalah prioritas kami. Semua data dienkripsi menggunakan teknologi SSL 256-bit dan disimpan di server yang aman. Kami juga terdaftar dan diawasi oleh OJK (Otoritas Jasa Keuangan) sehingga mengikuti standar keamanan yang ketat.",
                ),
                FAQItem(
                    question = "Bagaimana cara reset password?",
                    answer = "Klik 'Lupa Password' di halaman login, masukkan email yang terdaftar, lalu ikuti petunjuk yang dikirim ke email Anda. Link reset password berlaku selama 24 jam. Jika tidak menerima email, periksa folder spam atau coba lagi.",
                ),
            ),
        ),
        FAQCategory(
            title = "Pengajuan Pinjaman",
            icon = Icons.Default.MonetizationOn,
            iconColor = Color(0xFF2196F3),
            backgroundColor = Color(0xFF2196F3).copy(alpha = 0.15f),
            items = listOf(
                FAQItem(
                    question = "Apa saja syarat untuk mengajukan pinjaman?",
                    answer = "Syarat pengajuan pinjaman:\n• WNI dengan usia 18-55 tahun\n• Memiliki KTP yang masih berlaku\n• Memiliki pekerjaan/penghasilan tetap\n• Tidak memiliki tunggakan di pinjaman lain\n• Nomor HP aktif yang terdaftar\n• Rekening bank atas nama sendiri",
                ),
                FAQItem(
                    question = "Berapa lama proses persetujuan pinjaman?",
                    answer = "Proses persetujuan pinjaman biasanya 10 menit. Tahapannya:\n1. Verifikasi Data (2 menit)\n2. Analisis & Persetujuan (5 menit)\n3. Pencairan Dana (5 menit)\n\nAnda akan mendapat notifikasi di setiap tahapan proses.",
                ),
                FAQItem(
                    question = "Apa arti status pengajuan saya?",
                    answer = "• SUBMITTED: Pengajuan terkirim, menunggu review\n• UNDER_REVIEW: Sedang diverifikasi tim kami\n• APPROVED: Disetujui, dana akan segera dicairkan\n• REJECTED: Ditolak (akan ada penjelasan alasan)\n• DISBURSED: Dana sudah dicairkan ke rekening Anda\n• PENDING (Offline): Tersimpan di perangkat, akan terkirim saat online",
                ),
                FAQItem(
                    question = "Berapa maksimal pinjaman yang bisa saya ajukan?",
                    answer = "Batas pinjaman ditentukan berdasarkan profil dan penghasilan Anda. Umumnya berkisar dari Rp 1.000.000 hingga Rp 50.000.000. Semakin lengkap profil dan semakin baik riwayat pembayaran, semakin tinggi limit yang bisa didapatkan.",
                ),
                FAQItem(
                    question = "Bisakah saya mengajukan pinjaman lebih dari satu?",
                    answer = "Anda bisa memiliki lebih dari satu pinjaman aktif dalam satu waktu.",
                ),
            ),
        ),
        FAQCategory(
            title = "Pembayaran",
            icon = Icons.Default.Payment,
            iconColor = Color(0xFFFF9800),
            backgroundColor = Color(0xFFFF9800).copy(alpha = 0.15f),
            items = listOf(
                FAQItem(
                    question = "Bagaimana cara bayar lewat ATM?",
                    answer = "Langkah pembayaran via ATM:\n1. Pilih menu Transfer\n2. Pilih ke Bank [Nama Bank]\n3. Masukkan nomor Virtual Account yang tertera di tagihan\n4. Konfirmasi nominal pembayaran\n5. Simpan bukti transfer\n\nPembayaran akan diproses otomatis dalam 1x24 jam.",
                ),
                FAQItem(
                    question = "Bagaimana cara bayar lewat Virtual Account (VA)?",
                    answer = "Pembayaran via VA:\n1. Buka aplikasi mobile banking/internet banking\n2. Pilih menu Transfer > Virtual Account\n3. Masukkan nomor VA dari aplikasi Genggamin\n4. Pastikan nominal sudah sesuai\n5. Konfirmasi dan selesaikan pembayaran\n\nVA berlaku 24 jam dan nominal sudah termasuk cicilan + bunga.",
                ),
                FAQItem(
                    question = "Bagaimana cara bayar lewat Alfamart/Indomaret?",
                    answer = "Langkah pembayaran di minimarket:\n1. Buka aplikasi dan ambil kode pembayaran\n2. Kunjungi Alfamart/Indomaret terdekat\n3. Tunjukkan kode pembayaran ke kasir\n4. Bayar sesuai nominal tagihan + biaya admin\n5. Simpan struk sebagai bukti pembayaran\n\nBiaya admin: Rp 2.500 - Rp 5.000.",
                ),
                FAQItem(
                    question = "Bagaimana cara konfirmasi pembayaran?",
                    answer = "Pembayaran via VA/Transfer biasanya dikonfirmasi otomatis. Jika dalam 2x24 jam status belum berubah:\n1. Buka menu Riwayat Pinjaman\n2. Pilih pinjaman yang sudah dibayar\n3. Klik 'Konfirmasi Pembayaran'\n4. Upload bukti transfer/struk\n5. Tim kami akan memverifikasi dalam 1x24 jam.",
                ),
                FAQItem(
                    question = "Sudah bayar tapi tagihan masih muncul, bagaimana?",
                    answer = "Jika tagihan masih muncul setelah pembayaran:\n1. Pastikan pembayaran berhasil (cek mutasi rekening)\n2. Tunggu 1x24 jam untuk proses settlement\n3. Jika masih muncul, hubungi CS dengan melampirkan bukti transfer\n\nPenyebab umum: pembayaran diluar jam operasional bank atau nominal tidak sesuai dengan tagihan.",
                ),
                FAQItem(
                    question = "Apa yang terjadi jika terlambat bayar?",
                    answer = "Keterlambatan pembayaran akan dikenakan:\n• Denda keterlambatan per hari\n• Dapat mempengaruhi skor kredit Anda\n• Limit pinjaman berikutnya bisa dikurangi\n\nJika mengalami kesulitan, segera hubungi CS kami untuk mendiskusikan solusi pembayaran.",
                ),
            ),
        ),
    )
}
