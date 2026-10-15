package arvores.gui.visual;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * Atalhos para escolher o jeito de desenhar de cada componente. O visual vem do {@link LookModerno};
 * aqui só se diz qual variação usar.
 */
public final class Estilo {

    /** Propriedade que pinta um campo de texto como "com erro". */
    static final String ERRO = "arvores.campo.erro";
    /** Propriedade que deixa as abas menores (para abas dentro de um painel). */
    static final String ABAS_COMPACTAS = "arvores.abas.compactas";

    private Estilo() {
    }

    /** Ação principal da tela: botão escuro. */
    public static <B extends AbstractButton> B primario(B botao) {
        return tipo(botao, BotaoUI.Tipo.PRIMARIO);
    }

    /** Ação destrutiva: contorno e texto vermelhos. */
    public static <B extends AbstractButton> B perigo(B botao) {
        return tipo(botao, BotaoUI.Tipo.PERIGO);
    }

    /** Ação discreta: só o texto, o fundo aparece ao passar o mouse. */
    public static <B extends AbstractButton> B texto(B botao) {
        return tipo(botao, BotaoUI.Tipo.TEXTO);
    }

    /** Botão quadradinho só com um ícone. */
    public static <B extends AbstractButton> B icone(B botao) {
        return tipo(botao, BotaoUI.Tipo.ICONE);
    }

    private static <B extends AbstractButton> B tipo(B botao, BotaoUI.Tipo tipo) {
        botao.putClientProperty(BotaoUI.CHAVE_TIPO, tipo);
        botao.repaint();
        return botao;
    }

    /** Marca (ou desmarca) um campo de texto como inválido: contorno e fundo avermelhados. */
    public static void erro(JComponent campo, boolean comErro) {
        campo.putClientProperty(ERRO, comErro ? Boolean.TRUE : null);
        campo.repaint();
    }

    /** Abas menores, para as que ficam dentro de um painel. */
    public static JTabbedPane compactas(JTabbedPane abas) {
        abas.putClientProperty(ABAS_COMPACTAS, Boolean.TRUE);
        abas.updateUI();
        return abas;
    }
}
