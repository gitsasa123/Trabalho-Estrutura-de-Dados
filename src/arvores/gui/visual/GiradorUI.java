package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

/** Caixa de números com duas setinhas, no mesmo visual dos campos de texto. */
public final class GiradorUI extends BasicSpinnerUI {

    private final FocusListener repintar = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            spinner.repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            spinner.repaint();
        }
    };

    public static ComponentUI createUI(JComponent c) {
        return new GiradorUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        spinner.setOpaque(false);
        ajustarEditor(null, spinner.getEditor());
    }

    @Override
    protected void replaceEditor(JComponent antigo, JComponent novo) {
        super.replaceEditor(antigo, novo);
        ajustarEditor(antigo, novo);
    }

    /** O editor fica transparente (quem desenha a caixa é o spinner) e avisa quando ganha ou perde o foco. */
    private void ajustarEditor(JComponent antigo, JComponent novo) {
        if (antigo instanceof JSpinner.DefaultEditor e) {
            e.getTextField().removeFocusListener(repintar);
        }
        if (novo != null) {
            novo.setOpaque(false);
        }
        if (novo instanceof JSpinner.DefaultEditor e) {
            e.getTextField().addFocusListener(repintar);
        }
    }

    @Override
    protected Component createNextButton() {
        return trocar(super.createNextButton(), true);
    }

    @Override
    protected Component createPreviousButton() {
        return trocar(super.createPreviousButton(), false);
    }

    /** Troca a seta padrão por uma nossa, mantendo o que faz o botão funcionar (clique e repetição). */
    private static Component trocar(Component padrao, boolean paraCima) {
        Seta nova = new Seta(paraCima);
        nova.setName(padrao.getName());
        for (MouseListener l : padrao.getMouseListeners()) {
            nova.addMouseListener(l);
        }
        if (padrao instanceof JButton b) {
            for (ActionListener l : b.getActionListeners()) {
                nova.addActionListener(l);
            }
        }
        return nova;
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            int w = c.getWidth();
            int h = c.getHeight();
            boolean ativo = c.isEnabled();
            boolean foco = ativo && spinner.getEditor() instanceof JSpinner.DefaultEditor e
                    && e.getTextField().hasFocus();
            RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 16, 16);
            g.setColor(ativo ? Paleta.CARTAO : Paleta.FUNDO_DESABILITADO);
            g.fill(forma);
            if (foco) {
                g.setColor(Paleta.comAlfa(Paleta.FOCO, 0.28));
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 15, 15));
                g.setColor(Paleta.FOCO);
                g.setStroke(new BasicStroke(1f));
            } else {
                g.setColor(ativo ? Paleta.BORDA_FORTE : Paleta.BORDA);
            }
            g.draw(forma);
        } finally {
            g.dispose();
        }
    }

    /** Uma das duas setinhas à direita. */
    @SuppressWarnings("serial")
    private static final class Seta extends JButton {
        private final boolean paraCima;

        Seta(boolean paraCima) {
            this.paraCima = paraCima;
            setFocusable(false);
            setRequestFocusEnabled(false);
            setBorder(null);
            setOpaque(false);
            setContentAreaFilled(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(24, 10);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                Paleta.qualidade(g);
                boolean ativo = isEnabled();
                if (ativo && getModel().isRollover()) {
                    g.setColor(getModel().isPressed() ? Paleta.PRESSIONADO : Paleta.HOVER);
                    double folgaCima = paraCima ? 0 : 1;
                    double folgaBaixo = paraCima ? 1 : 0;
                    g.fill(new RoundRectangle2D.Double(2, folgaCima, getWidth() - 4, getHeight() - folgaCima - folgaBaixo, 6, 6));
                }
                Color cor = ativo ? Paleta.TEXTO_2 : Paleta.TEXTO_DESABILITADO;
                Icones icone = paraCima ? Icones.CHEVRON_CIMA : Icones.CHEVRON_BAIXO;
                icone.pintar(g, (getWidth() - 9) / 2.0, (getHeight() - 9) / 2.0 + (paraCima ? 0.5 : -0.5), 9, cor);
            } finally {
                g.dispose();
            }
        }
    }
}
