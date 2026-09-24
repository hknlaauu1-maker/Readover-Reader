package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BookEntity
import kotlin.math.abs

/**
 * Modern Dense 5-Column Book Cover Grid matching the user's reference design.
 * Features rounded rectangular artistic book covers on a sleek dark slate background,
 * with clean titles underneath.
 */
@Composable
fun WoodenBookshelfGrid(
    books: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF19202A))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("modern_bookshelf_grid")
        ) {
            items(books, key = { it.id }) { book ->
                ModernDenseBookCard(
                    book = book,
                    onClick = { onOpenBook(book.id) },
                    onToggleFavorite = { onToggleFavorite(book) }
                )
            }
        }
    }
}

@Composable
fun ModernDenseBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coverPalette = remember(book.id, book.title) {
        getProceduralPalette(book.id, book.title)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_item_${book.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Book Cover Artwork Box (Aspect ratio approx 1:1.52)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            coverPalette.gradientStart,
                            coverPalette.gradientMiddle,
                            coverPalette.gradientEnd
                        )
                    )
                )
        ) {
            // Always render procedural artistic cover as base layer (guarantees book is never blank/empty)
            ProceduralArtisticCover(
                book = book,
                palette = coverPalette
            )

            // If an open source / embedded cover image is available, load it on top with crossfade
            if (!book.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(book.coverImageUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle inner shadow overlay for realism
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
            }

            // Left spine highlight
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Favorite star badge
            if (book.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favori",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Reading progress bar if started
            if (book.progressPercent > 0.02f) {
                LinearProgressIndicator(
                    progress = { book.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Color(0xFF38BDF8),
                    trackColor = Color.Black.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title underneath cover
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                lineHeight = 12.sp
            ),
            color = Color(0xFFE2E8F0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Renders an artistic procedural cover for books that don't have an online image URL,
 * styled to look like published classic / sci-fi / fantasy book jackets.
 */
@Composable
private fun ProceduralArtisticCover(
    book: BookEntity,
    palette: CoverPalette
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = palette.accentGold.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        // Inner ornate border frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.75.dp,
                    color = palette.accentGold.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Author text
            Text(
                text = (book.author.ifBlank { "KLASİK" }).uppercase(),
                color = palette.accentGold.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Central emblem / title banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (palette.emblemType) {
                        0 -> Icons.Default.AutoStories
                        1 -> Icons.AutoMirrored.Filled.MenuBook
                        else -> Icons.Default.Bookmark
                    },
                    contentDescription = null,
                    tint = palette.accentGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = book.title.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                        lineHeight = 10.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom format badge or decorative line
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(1.dp)
                    .background(palette.accentGold.copy(alpha = 0.6f))
            )
        }
    }
}

private data class CoverPalette(
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    val accentGold: Color,
    val emblemType: Int
)

private fun getProceduralPalette(bookId: Long, title: String): CoverPalette {
    val hash = abs((bookId.toString() + title).hashCode())
    val styleIndex = hash % 8
    val emblem = (hash / 8) % 3

    return when (styleIndex) {
        0 -> CoverPalette(
            gradientStart = Color(0xFF78350F), // Sand Dune Warm Amber
            gradientMiddle = Color(0xFF451A03),
            gradientEnd = Color(0xFF1C1917),
            accentGold = Color(0xFFFDE68A),
            emblemType = emblem
        )
        1 -> CoverPalette(
            gradientStart = Color(0xFF1E3A8A), // Indigo Celestial
            gradientMiddle = Color(0xFF0F172A),
            gradientEnd = Color(0xFF020617),
            accentGold = Color(0xFF93C5FD),
            emblemType = emblem
        )
        2 -> CoverPalette(
            gradientStart = Color(0xFF881337), // Crimson Velvet
            gradientMiddle = Color(0xFF4C0519),
            gradientEnd = Color(0xFF1F1015),
            accentGold = Color(0xFFFECDD3),
            emblemType = emblem
        )
        3 -> CoverPalette(
            gradientStart = Color(0xFF065F46), // Emerald Forest
            gradientMiddle = Color(0xFF064E3B),
            gradientEnd = Color(0xFF022C22),
            accentGold = Color(0xFFA7F3D0),
            emblemType = emblem
        )
        4 -> CoverPalette(
            gradientStart = Color(0xFF581C87), // Mystical Royal Purple
            gradientMiddle = Color(0xFF3B0764),
            gradientEnd = Color(0xFF19062B),
            accentGold = Color(0xFFE9D5FF),
            emblemType = emblem
        )
        5 -> CoverPalette(
            gradientStart = Color(0xFF334155), // Slate Charcoal
            gradientMiddle = Color(0xFF1E293B),
            gradientEnd = Color(0xFF0F172A),
            accentGold = Color(0xFFCBD5E1),
            emblemType = emblem
        )
        6 -> CoverPalette(
            gradientStart = Color(0xFF9A3412), // Terracotta Ancient
            gradientMiddle = Color(0xFF7C2D12),
            gradientEnd = Color(0xFF2E1007),
            accentGold = Color(0xFFFED7AA),
            emblemType = emblem
        )
        else -> CoverPalette(
            gradientStart = Color(0xFF14532D), // Deep Dark Bronze-Green
            gradientMiddle = Color(0xFF163820),
            gradientEnd = Color(0xFF0A180E),
            accentGold = Color(0xFFBBF7D0),
            emblemType = emblem
        )
    }
}
