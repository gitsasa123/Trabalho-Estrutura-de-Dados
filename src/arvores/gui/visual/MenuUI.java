package arvores.gui.visual;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

/** Menus (da barra e submenus) com o mesmo destaque arredondado dos itens. */
public final class MenuUI extends BasicMenuUI {

    public static ComponentUI createUI(JComponent c) {
        return new MenuUI();
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem item, Color corDeSelecao) {
        MenuItemUI.destacar(g, item);
    }
}
