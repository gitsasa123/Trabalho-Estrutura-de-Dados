package arvores.gui.visual;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;

/** Ícone de uma bolinha colorida com um halo suave: a "etiqueta" de cor de uma árvore ou de um estado. */
public final class Bolinha implements Icon {

    private final Color cor;
    private final int tamanho;

    public Bolinha(Color cor, int tamanho) {
        this.cor = cor;
        this.tamanho = tamanho;
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            g.setColor(Paleta.comAlfa(cor, 0.22));
            g.fill(new Ellipse2D.Double(x, y, tamanho, tamanho));
            double miolo = tamanho * 0.56;
            g.setColor(cor);
            g.fill(new Ellipse2D.Double(x + (tamanho - miolo) / 2, y + (tamanho - miolo) / 2, miolo, miolo));
        } finally {
            g.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return tamanho;
    }

    @Override
    public int getIconHeight() {
        return tamanho;
    }
}
