package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookEntity

@Composable
fun WoodenBookshelfGrid(
    books: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookRows = books.chunked(4)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2A170C),
                        Color(0xFF422615),
                        Color(0xFF2D190E)
                    )
                )
            )
    ) {
        // Outer Side Wood Framing Panels
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Wood Frame
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E1007), Color(0xFF3D2111), Color(0xFF2B160B))
                        )
                    )
            )

            // Middle Shelf Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(bookRows) { rowBooks ->
                    WoodenShelfRow(
                        books = rowBooks,
                        onOpenBook = onOpenBook,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            // Right Wood Frame & Metallic Scroll Indicator
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2B160B), Color(0xFF3D2111), Color(0xFF1E1007))
                        )
                    )
            )
        }
    }
}

@Composable
fun WoodenShelfRow(
    books: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onToggleFavorite: (BookEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Standing books on shelf
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until 4) {
                if (i < books.size) {
                    val book = books[i]
                    WoodenBookItem(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        onToggleFavorite = { onToggleFavorite(book) },
                        modifier = Modifier.width(80.dp)
                    )
                } else {
                    // Empty space holder to keep shelf proportions balanced
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }
        }

        // Realistic 3D Wooden Shelf Plank
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top bevel wood highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFD4A373))
            )

            // Main Wooden Shelf Plank
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5A2B),
                                Color(0xFF6B4220),
                                Color(0xFF4A2B14)
                            )
                        )
                    )
            )

            // Bottom Drop Shadow under shelf plank
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E0E05).copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun WoodenBookItem(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(book.coverColorHex)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(bottom = 2.dp)
            .clickable { onClick() }
            .testTag("wooden_book_${book.id}")
    ) {
        // Standing Book Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 6.dp, bottomEnd = 2.dp, bottomStart = 2.dp))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 6.dp, bottomEnd = 2.dp, bottomStart = 2.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(baseColor, baseColor.copy(alpha = 0.8f), Color(0xFF111827))
                    )
                )
        ) {
            if (!book.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay for title legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Book Spine Line (3D spine highlight on left edge)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Favorite star indicator
            if (book.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favori",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Title & Author text on cover
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
