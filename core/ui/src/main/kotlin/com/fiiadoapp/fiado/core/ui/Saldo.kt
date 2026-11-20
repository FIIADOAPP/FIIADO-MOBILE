package com.fiiadoapp.fiado.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.fiiadoapp.fiado.domain.Dinero

/** "Debe S/ 12.50", "A favor S/ 3.00" o "Al día". Lo que se debe va en coral; lo demás, en verde. */
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
    estilo: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = textoDeSaldo(saldo),
        modifier = modifier,
        style = estilo,
        fontWeight = if (saldo.esPositivo) FontWeight.SemiBold else FontWeight.Medium,
        color =
            when {
                saldo.esPositivo -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            },
    )
}
