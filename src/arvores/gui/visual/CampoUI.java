package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicFormattedTextFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;

/**
 * Campos de texto de cantos arredondados, com anel azul quando têm o foco e vermelho quando marcados
 * com {@link Estilo#erro}. A mesma pintura serve para os campos formatados (os dos spinners).
 */
public final class CampoUI extends BasicTextFieldUI {

    /** Margem interna: o texto fica afastado do contorno. */
    static final AbstractBorder BORDA = new Margem();

    private static final FocusListener REPINTAR = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            ((Component) e.getSource()).repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            ((Component) e.getSource()).repaint();
        }
    };

    public static ComponentUI createUI(JComponent c) {
        return new CampoUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
        c.addFocusListener(REPINTAR);
    }

    @Override
    public void uninstallUI(JComponent c) {
        c.removeFocusListener(REPINTAR);
        super.uninstallUI(c);
    }

    @Override
    protected void paintSafely(Graphics g) {
        pintarCaixa(g, getComponent());
        super.paintSafely(g);
    }

    /** O mesmo visual para {@code JFormattedTextField}. */
    public static final class Formatado extends BasicFormattedTextFieldUI {

        public static ComponentUI createUI(JComponent c) {
            return new Formatado();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            c.setOpaque(false);
            c.addFocusListener(REPINTAR);
        }

        @Override
        public void uninstallUI(JComponent c) {
            c.removeFocusListener(REPINTAR);
            super.uninstallUI(c);
        }

        @Override
        protected void paintSafely(Graphics g) {
            pintarCaixa(g, getComponent());
            super.paintSafely(g);
        }
    }

    /** Fundo arredondado e contorno do campo (os campos dentro de um spinner ficam sem caixa própria). */
    private static void pintarCaixa(Graphics g0, JTextComponent c) {
        if ("Spinner.formattedTextField".equals(c.getName())) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            int w = c.getWidth();
            int h = c.getHeight();
            boolean erro = Boolean.TRUE.equals(c.getClientProperty(Estilo.ERRO));
            boolean ativo = c.isEnabled() && c.isEditable();
            Color fundo = erro ? Paleta.PERIGO_FUNDO : ativo ? c.getBackground() : Paleta.FUNDO_DESABILITADO;
            RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 16, 16);
            g.setColor(fundo);
            g.fill(forma);
            if (c.hasFocus() && ativo) {
                Color destaque = erro ? Paleta.PERIGO : Paleta.FOCO;
                g.setColor(Paleta.comAlfa(destaque, 0.28));
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 15, 15));
                g.setColor(destaque);
                g.setStroke(new BasicStroke(1f));
                g.draw(forma);
            } else {
                g.setColor(erro ? Paleta.PERIGO_BORDA : ativo ? Paleta.BORDA_FORTE : Paleta.BORDA);
                g.draw(forma);
            }
        } finally {
            g.dispose();
        }
    }

    /** Folga entre o contorno e o texto, mais a margem pedida no próprio campo. */
    @SuppressWarnings("serial")
    private static final class Margem extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets m = (c instanceof JTextComponent t && t.getMargin() != null) ? t.getMargin() : new Insets(0, 0, 0, 0);
            insets.set(7 + m.top, 10 + m.left, 7 + m.bottom, 10 + m.right);
            return insets;
        }
    }
}
