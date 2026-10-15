package arvores;

import java.awt.GraphicsEnvironment;

import arvores.gui.JanelaPrincipal;

/**
 * Abre a interface gráfica com as três árvores.
 *
 * <p>Argumentos opcionais (os mesmos do programa antigo): {@code [csv] [quantidade] [ordenado|embaralhado]}.
 * Com eles, o programa já abre carregando as faixas.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // texto suavizado também no Linux (em outros sistemas já é assim); tem que valer antes de qualquer janela
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Este programa precisa de uma tela (interface gráfica).");
            System.err.println("Sem tela, use: java -cp out arvores.Benchmark  ou  java -cp out arvores.TesteArvore");
            System.exit(1);
        }
        JanelaPrincipal.abrir(args);
    }
}
