package arvores.gui.visual;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * Como o {@link FlowLayout}, mas calcula a altura de todas as linhas quando os componentes não cabem
 * numa só (o FlowLayout comum "esquece" as linhas de baixo e elas ficam cortadas).
 */
public final class LayoutQuebra extends FlowLayout {

    private static final long serialVersionUID = 1L;

    public LayoutQuebra(int alinhamento, int espacoH, int espacoV) {
        super(alinhamento, espacoH, espacoV);
    }

    @Override
    public Dimension preferredLayoutSize(Container alvo) {
        return medir(alvo, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container alvo) {
        Dimension minimo = medir(alvo, false);
        minimo.width -= getHgap() + 1;
        return minimo;
    }

    private Dimension medir(Container alvo, boolean preferido) {
        synchronized (alvo.getTreeLock()) {
            // a largura que vale é a do primeiro ancestral que já tem tamanho
            Container c = alvo;
            while (c.getWidth() == 0 && c.getParent() != null) {
                c = c.getParent();
            }
            int larguraAlvo = c.getWidth() == 0 ? Integer.MAX_VALUE : c.getWidth();
            Insets margens = alvo.getInsets();
            int folgaH = margens.left + margens.right + getHgap() * 2;
            int larguraMaxima = larguraAlvo == Integer.MAX_VALUE ? larguraAlvo : larguraAlvo - folgaH;

            Dimension total = new Dimension(0, 0);
            int larguraLinha = 0;
            int alturaLinha = 0;
            for (int i = 0; i < alvo.getComponentCount(); i++) {
                Component filho = alvo.getComponent(i);
                if (!filho.isVisible()) {
                    continue;
                }
                Dimension d = preferido ? filho.getPreferredSize() : filho.getMinimumSize();
                if (larguraLinha > 0 && larguraLinha + getHgap() + d.width > larguraMaxima) {
                    acrescentar(total, larguraLinha, alturaLinha);
                    larguraLinha = 0;
                    alturaLinha = 0;
                }
                larguraLinha += (larguraLinha == 0 ? 0 : getHgap()) + d.width;
                alturaLinha = Math.max(alturaLinha, d.height);
            }
            acrescentar(total, larguraLinha, alturaLinha);
            total.width += folgaH;
            total.height += margens.top + margens.bottom + getVgap() * 2;
            return total;
        }
    }

    private void acrescentar(Dimension total, int larguraLinha, int alturaLinha) {
        total.width = Math.max(total.width, larguraLinha);
        if (total.height > 0) {
            total.height += getVgap();
        }
        total.height += alturaLinha;
    }
}
