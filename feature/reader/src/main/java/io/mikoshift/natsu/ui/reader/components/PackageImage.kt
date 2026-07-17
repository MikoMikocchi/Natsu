package io.mikoshift.natsu.ui.reader.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.mikoshift.natsu.feature.reader.R
import java.io.File

@Composable
fun PackageImage(
    assetPath: String?,
    alt: String?,
    modifier: Modifier = Modifier,
) {
    if (assetPath == null) return
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(assetPath))
            .build(),
        contentDescription = alt ?: stringResource(R.string.reader_image_alt),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentScale = ContentScale.FillWidth,
    )
}
