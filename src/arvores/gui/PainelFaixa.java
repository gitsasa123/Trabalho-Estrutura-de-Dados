package arvores.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;

import arvores.Faixa;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Icones;

/** Faixa de música em destaque: a que foi achada numa busca ou em que se clicou no desenho. */
@SuppressWarnings("serial")
final class PainelFaixa extends Cartao {

    private static final String DICA = "Clique num nó ou busque uma chave para ver a faixa guardada nele.";

    private final JLabel chave = new JLabel();
    private final JLabel titulo = new JLabel();
    private final JLabel detalhes = new JLabel();

    PainelFaixa() {
        super(new BorderLayout(10, 0));
        comMargem(9, 14, 9, 14);

        chave.setFont(Tema.fonte(13f, Font.BOLD));
        chave.setForeground(Tema.TEXTO_2);
        chave.setIconTextGap(7);
        titulo.setFont(Tema.fonte(13.5f, Font.BOLD));
        titulo.setForeground(Tema.TEXTO);
        detalhes.setFont(Tema.fonte(12.5f, Font.PLAIN));
        detalhes.setForeground(Tema.TEXTO_2);

        JPanel texto = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        texto.setOpaque(false);
        texto.add(chave);
        texto.add(titulo);
        texto.add(detalhes);
        add(texto, BorderLayout.CENTER);
        limpar();
    }

    void mostrar(int chaveDaFaixa, Faixa f) {
        chave.setIcon(Icones.NOTA.icone(16, Tema.TEXTO_3));
        chave.setText("chave " + chaveDaFaixa);
        titulo.setText(f.titulo());
        detalhes.setText(f.artista() + "  ·  " + f.genero() + "  ·  " + Formato.inteiro(f.ouvintes()) + " ouvintes  ·  "
                + Formato.duracao(f.duracaoSeg()));
        setToolTipText(null);
    }

    void limpar() {
        chave.setIcon(Icones.NOTA.icone(16, Tema.TEXTO_3));
        chave.setText("");
        titulo.setText("");
        detalhes.setText(DICA);
        setToolTipText(null);
    }
}
