package arvores.gui;

import java.util.Locale;

/** Formatação de números e tempos no padrão brasileiro (1.234,5), igual à dos gráficos em Python. */
final class Formato {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formato() {
    }

    /** 1234567 → "1.234.567". */
    static String inteiro(long valor) {
        return String.format(PT_BR, "%,d", valor);
    }

    /** 1234.5 → "1.234,5" (com o nº de casas pedido). */
    static String decimal(double valor, int casas) {
        return String.format(PT_BR, "%,." + casas + "f", valor);
    }

    /** Versão curta para rótulos apertados: 734 mil, 1,3 mi, 1,25 bi. */
    static String compacto(double valor) {
        if (valor >= 1e9) {
            return decimal(valor / 1e9, 2) + " bi";
        }
        if (valor >= 1e6) {
            return decimal(valor / 1e6, 1) + " mi";
        }
        if (valor >= 1e3) {
            return decimal(valor / 1e3, 0) + " mil";
        }
        return decimal(valor, 0);
    }

    /** Tempo em milissegundos; passa para segundos acima de 10 s. */
    static String tempoMs(double ms) {
        if (ms >= 10_000) {
            return decimal(ms / 1000, 1) + " s";
        }
        if (ms >= 100) {
            return decimal(ms, 0) + " ms";
        }
        return decimal(ms, ms >= 10 ? 1 : 2) + " ms";
    }

    /** 168 → "2:48". */
    static String duracao(int segundos) {
        int s = Math.max(0, segundos);
        return (s / 60) + ":" + String.format(PT_BR, "%02d", s % 60);
    }
}
