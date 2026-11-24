package com.dking.crocapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card heading with a small numbered circle: ① Files, ② Code, …
 * Makes the Send / Receive flow readable at a glance.
 */
@Composable
fun StepTitle(
    number: Int,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = trailing
            )
        }
    }
}

/**
 * Big, friendly tap target used when nothing is picked yet
 * ("Tap to pick files"). Replaces a small button + "No files selected".
 */
@Composable
fun PickTarget(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}




/**
 * Large thin summary for picked files: "03 FILES   2.4 GB".
 * [size] is a formatted size such as "2.4 GB"; the number and unit are styled apart.
 */
@Composable
fun BigCount(count: Int, size: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val num = size.substringBefore(' ')
    val unit = size.substringAfter(' ', "")
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = count.toString().padStart(2, '0'),
                fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                fontSize = 56.sp, letterSpacing = (-2).sp, lineHeight = 56.sp,
                color = cs.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = androidx.compose.ui.res.pluralStringResource(com.dking.crocapp.R.plurals.big_files, count).uppercase(),
                fontFamily = com.dking.crocapp.ui.theme.MonoFamily, fontSize = 11.sp, letterSpacing = 3.sp,
                color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = num,
                fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                fontSize = 36.sp, letterSpacing = (-1).sp, lineHeight = 36.sp,
                color = cs.onSurface, modifier = Modifier.padding(bottom = 2.dp)
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = unit.uppercase(),
                    fontFamily = com.dking.crocapp.ui.theme.MonoFamily, fontSize = 11.sp, letterSpacing = 3.sp,
                    color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))
    }
}


/** The numbered ring used by step titles ("01"), on its own. */
@Composable
fun StepNumber(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = com.dking.crocapp.ui.theme.MonoFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
