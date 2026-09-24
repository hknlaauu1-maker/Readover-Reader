package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ads.AdManager
import com.example.util.i18n.AppLanguage
import com.example.util.i18n.LocalAppLanguage
import com.example.util.i18n.appString

@Composable
fun PremiumPurchaseDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val priceText = if (currentLang == AppLanguage.TURKISH) "1 Dolar ($1.00)" else "$1.00"
    val titleText = if (currentLang == AppLanguage.TURKISH) "Readover Pro Sürümüne Yükselt" else "Upgrade to Readover Pro"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24), // Vibrant gold/yellow to match branding
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appString("premium_lifetime_title"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appString("premium_lifetime_desc"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                FeatureCheckItem(title = appString("premium_feature_1"))
                FeatureCheckItem(title = appString("premium_feature_2"))
                FeatureCheckItem(title = appString("premium_feature_3"))
                FeatureCheckItem(title = appString("premium_feature_4"))
                FeatureCheckItem(title = appString("premium_feature_5"))

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = appString("premium_license_type"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = appString("premium_no_sub"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AdManager.setPremiumUnlocked(context, true)
                    val msg = if (currentLang == AppLanguage.TURKISH) 
                        "Tebrikler! Readover Pro sürümüne yükseltildiniz. Reklamlar kaldırıldı."
                    else 
                        "Congratulations! Upgraded to Readover Pro version. Ads removed."
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBBF24),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_unlock_premium")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(appString("premium_btn_purchase"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appString("close"))
            }
        }
    )
}

@Composable
private fun FeatureCheckItem(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun KvkkAndLegalNoticeDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val githubUrl = "https://github.com/hknlaauu1-maker/Readover-Reader"

    val dialogTitle = if (currentLang == AppLanguage.TURKISH) {
        "Kullanıcı Sözleşmesi (EULA) & Gizlilik"
    } else {
        "End User License Agreement (EULA) & Privacy"
    }

    val understoodText = if (currentLang == AppLanguage.TURKISH) "Okudum, Kabul Ediyorum" else appString("understood")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 1: İçerik ve Telif Hakkı Sorumluluğu
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.TURKISH) "1. İçerik ve Telif Hakkı Sorumluluğu" else "1. Content & Copyright Responsibility",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.TURKISH) {
                                "• Kullanıcı Tarafından Yüklenen İçerik: Uygulama, kullanıcıların kendi e-kitap dosyalarını (EPUB, PDF, MOBI vb.) cihaza yükleyerek okumasına olanak tanıyan bir araçtır. Uygulamaya yüklenen, açılan veya saklanan tüm içeriklerin yasal sorumluluğu ve telif haklarına uygunluğu tamamen kullanıcıya aittir.\n\n" +
                                "• Korsan ve Yasa Dışı İçerik Yasağı: Kullanıcı, telif hakkı sahibinin izni olmadan yasa dışı yollarla elde edilmiş (korsan) materyalleri uygulamada açmayacağını kabul eder. Telif hakkı ihlallerinden doğabilecek tüm hukuki ve cezai sorumluluk kullanıcıya aittir; geliştirici bu konuda hiçbir sorumluluk kabul etmez."
                            } else {
                                "• User Uploaded Content: The app is a reader utility allowing users to import and read their own e-book files (EPUB, PDF, MOBI, etc.). All legal responsibility and copyright compliance of any loaded or opened files belong solely to the user.\n\n" +
                                "• Anti-Piracy Agreement: The user agrees not to open any materials obtained unlawfully (pirated) without copyright holder permission. Full legal and criminal liability from any infringements belongs to the user; the developer accepts zero liability."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }

                // SECTION 2: Hizmetin Sınırları ve Sorumluluk Reddi (Disclaimer)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.TURKISH) "2. Hizmet Sınırları & Sorumluluk Reddi" else "2. Limitations & Disclaimer",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.TURKISH) {
                                "• Dosya ve Veri Kaybı: Uygulama; teknik aksaklıklar, cihaz çökmeleri veya güncellemeler nedeniyle okuma geçmişinin, yer işaretlerinin (bookmark), notların veya yüklenen kitapların kaybolmayacağını garanti etmez. Kullanıcı, önemli verilerini yedeklemekle kendisi yükümlüdür.\n\n" +
                                "• Hizmetin \"Olduğu Gibi\" Sunulması: Uygulama 'mevcut haliyle' (As-Is) sunulmaktadır. Geliştirici, uygulamanın tüm e-kitap formatlarını kusursuz açacağını, yazı tiplerini veya sayfa düzenlerini her cihazda hatasız göstereceğini garanti etmez."
                            } else {
                                "• Data and File Loss: The app does not guarantee that reading progress, bookmarks, notes, or uploaded files will not be lost due to technical issues, device failures, or updates. Backing up files and progress remains the user's sole responsibility.\n\n" +
                                "• \"As-Is\" Disclaimer: The app is provided on an 'as-is' and 'as-available' basis. The developer does not warrant that all e-book formats will open flawlessly, or that font rendering and layouts will display error-free on every device."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }

                // SECTION 3: Cihaz İzinleri ve Veri Gizliliği
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.TURKISH) "3. Cihaz İzinleri ve Veri Gizliliği" else "3. Device Permissions & Privacy",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF059669)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.TURKISH) {
                                "• Depolama Erişimi: Uygulamanın cihazdaki e-kitap dosyalarını tespit edebilmesi ve açabilmesi için kullanıcının dosya/depolama erişim izni vermesi gerekmektedir. Bu izin sadece kitap dosyalarının yerel olarak okunması amacıyla kullanılır.\n\n" +
                                "• Kişisel Veriler: %100 Sıfır Veri Toplama Politikası uygulanmaktadır. Okuma alışkanlıklarınız, altı çizilen kelimeleriniz veya istatistikleriniz kesinlikle sunucularımıza veya üçüncü taraflara gönderilmez; her şey cihazınızda yerel olarak saklanır."
                            } else {
                                "• Storage Access: The app requires local storage/file access permission to find and open e-book documents. This permission is used exclusively for locating and reading files locally on your device.\n\n" +
                                "• Personal Data: 100% Zero Data Collection Policy. Your reading habits, highlighted words, and annotations remain strictly on your local device and are never sent to external servers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }

                // SECTION 4: Fikri Mülkiyet (Uygulamanın Kendisi)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.TURKISH) "4. Fikri Mülkiyet ve Açık Kaynak" else "4. Intellectual Property & Open Source",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentLang == AppLanguage.TURKISH) {
                                "• Tasarım ve Marka: Uygulamanın arayüz tasarımı, görsel tasarımı, logoları ve özel yazı tipleri geliştiriciye aittir.\n\n" +
                                "• Açık Kaynak Kodları: Uygulamamız açık kaynaklı Librera tabanlıdır. Kullanıcılar, bu açık kaynak kodlarını lisans kurallarına (GPL) uygun şekilde kullanabilir, değiştirebilir ve yeni bir uygulama geliştirebilir."
                            } else {
                                "• Branding & Interface: The application's interface design, assets, logos, and specialized fonts belong to the developer.\n\n" +
                                "• Open Source Core: Our app is built upon the open-source Librera Reader core. Users may copy, modify, and build their own apps using these open-source resources according to GPL licensing requirements."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub Repository", githubUrl))
                                        Toast.makeText(context, "Copied GitHub URL!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = githubUrl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(understoodText)
            }
        }
    )
}
