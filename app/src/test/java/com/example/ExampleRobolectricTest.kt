package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FrequenciaPlano
import com.example.data.model.Ordem
import com.example.data.model.Pagamento
import com.example.data.model.Plano
import com.example.data.model.StatusOrdem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jakero Tudo", appName)
    }

    @Test
    fun `ordem calculations and balance logic`() {
        val ordem = Ordem(
            id = 1L,
            num = 42,
            data = LocalDate.now(),
            cliente = "Maria Silva",
            telefone = "85999998888",
            servico = "Confecção de Cortinas",
            quantidade = 2.0,
            valorUnitario = 150.0,
            entrada = 100.0,
            desconto = 20.0,
            pagamentos = listOf(
                Pagamento(id = "p1", valor = 80.0, data = LocalDate.now())
            ),
            status = StatusOrdem.ANDAMENTO
        )

        // Valor: (2.0 * 150.0) - 20.0 = 280.0
        assertEquals(280.0, ordem.valor, 0.001)
        // Total Pago: entrada (100.0) + pagamentos (80.0) = 180.0
        assertEquals(180.0, ordem.totalPago, 0.001)
        // Falta: 280.0 - 180.0 = 100.0
        assertEquals(100.0, ordem.falta, 0.001)
        assertFalse(ordem.quitada)
        assertEquals("0042", ordem.numeroFormatado)
    }

    @Test
    fun `ordem parcelamento calculation`() {
        val ordem = Ordem(
            id = 2L,
            num = 10,
            data = LocalDate.now(),
            cliente = "João Pereira",
            telefone = "85988887777",
            servico = "Reforma Sofá",
            quantidade = 1.0,
            valorUnitario = 600.0,
            entrada = 150.0,
            desconto = 0.0,
            plano = Plano(
                parcelas = 3,
                primeiroVencimento = LocalDate.of(2026, 10, 1),
                frequencia = FrequenciaPlano.MENSAL
            )
        )

        // Saldo a parcelar: 600.0 - 150.0 = 450.0
        // 3 parcelas de 150.0 cada
        val parcelas = ordem.parcelasCalculadas()
        assertEquals(3, parcelas.size)
        assertEquals(150.0, parcelas[0].valor, 0.001)
        assertEquals(150.0, parcelas[1].valor, 0.001)
        assertEquals(150.0, parcelas[2].valor, 0.001)
    }
}
