package com.fiiadoapp.fiado.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.fiiadoapp.fiado.domain.Dinero

/** "Debe S/ 12.50", "A favor S/ 3.00" o "Al día". */
@Composable
fun textoDeSaldo(saldo: Dinero): String =
    when {
        saldo.esPositivo -> stringResource(R.string.saldo_debe, saldo.formatear())
        saldo.esNegativo -> stringResource(R.string.saldo_a_favor, (-saldo).formatear())
        else -> stringResource(R.string.saldo_al_dia)
    }

@Composable
fun SaldoTexto(
    saldo: Dinero,
    modifier: Modifier = Modifier,
) {
    Text(
        text = textoDeSaldo(saldo),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (saldo.esPositivo) FontWeight.Bold else FontWeight.Normal,
        color =
            when {
                saldo.esPositivo -> MaterialTheme.colorScheme.error
                saldo.esNegativo -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}
