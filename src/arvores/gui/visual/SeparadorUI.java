package arvores.gui.visual;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;

/** Linha fina que separa grupos de itens num menu. */
public final class SeparadorUI extends BasicPopupMenuSeparatorUI {

    public static ComponentUI createUI(JComponent c) {
        return new SeparadorUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        g.setColor(Paleta.BORDA);
        g.fillRect(8, c.getHeight() / 2, Math.max(0, c.getWidth() - 16), 1);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(0, 9);
    }
}
