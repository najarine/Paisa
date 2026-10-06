package com.paisa.najarine.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paisa.najarine.R

@Composable
fun PaisaLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = 22.sp,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 1.5.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.paisa_logo),
            contentDescription = "Paisa Logo",
            modifier = Modifier.size(size)
        )
    }
}
