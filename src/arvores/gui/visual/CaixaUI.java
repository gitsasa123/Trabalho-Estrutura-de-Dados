package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicCheckBoxUI;

/** Caixas de marcar: quadradinho arredondado que fica escuro, com um "check" branco, quando marcado. */
public final class CaixaUI extends BasicCheckBoxUI {

    private static final CaixaUI INSTANCIA = new CaixaUI();

    /** O quadradinho; é o ícone padrão de {@code CheckBox.icon}. */
    static final Icon ICONE = new Quadrado();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCIA;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
    }

    @Override
    protected void installListeners(AbstractButton b) {
        super.installListeners(b);
        b.addMouseListener(BotaoUI.OUVINTE_MOUSE);
        b.addFocusListener(BotaoUI.OUVINTE_FOCO);
    }

    @Override
    protected void uninstallListeners(AbstractButton b) {
        b.removeMouseListener(BotaoUI.OUVINTE_MOUSE);
        b.removeFocusListener(BotaoUI.OUVINTE_FOCO);
        super.uninstallListeners(b);
    }

    @Override
    public synchronized void paint(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            super.paint(g, c);
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        g.setFont(b.getFont());
        g.setColor(b.isEnabled() ? b.getForeground() : Paleta.TEXTO_DESABILITADO);
        g.drawString(text, textRect.x, textRect.y + g.getFontMetrics().getAscent());
    }

    @Override
    protected void paintFocus(Graphics g, Rectangle textRect, Dimension size) {
        // o anel de foco é desenhado em volta do quadradinho
    }

    private static final class Quadrado implements Icon, UIResource {

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean ativo = b.isEnabled();
            boolean marcado = m.isSelected();
            boolean sobre = ativo && m.isRollover();
            boolean apertado = ativo && m.isPressed() && m.isArmed();
            Graphics2D g = (Graphics2D) g0.create();
            try {
                Paleta.qualidade(g);
                g.translate(x, y);
                RoundRectangle2D caixa = new RoundRectangle2D.Double(0.5, 0.5, 15, 15, 8, 8);
                if (ativo && b.isFocusPainted() && b.hasFocus()
                        && !Boolean.TRUE.equals(b.getClientProperty(BotaoUI.CHAVE_FOCO_POR_MOUSE))) {
                    g.setColor(Paleta.comAlfa(Paleta.FOCO, 0.35));
                    g.setStroke(new BasicStroke(3f));
                    g.draw(new RoundRectangle2D.Double(-0.5, -0.5, 17, 17, 10, 10));
                    g.setStroke(new BasicStroke(1f));
                }
                if (marcado) {
                    g.setColor(!ativo ? Paleta.PRIMARIO_DESABILITADO
                            : apertado ? Paleta.PRIMARIO_PRESSIONADO : sobre ? Paleta.PRIMARIO_HOVER : Paleta.PRIMARIO);
                    g.fill(caixa);
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D.Double check = new Path2D.Double();
                    check.moveTo(4.4, 8.3);
                    check.lineTo(7, 10.9);
                    check.lineTo(11.7, 5.4);
                    g.draw(check);
                } else {
                    g.setColor(!ativo ? Paleta.FUNDO_DESABILITADO
                            : apertado ? Paleta.PRESSIONADO : sobre ? Paleta.HOVER : Paleta.CARTAO);
                    g.fill(caixa);
                    g.setColor(!ativo ? Paleta.BORDA : sobre ? Paleta.TEXTO_3 : Paleta.BORDA_FORTE);
                    g.draw(caixa);
                }
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
