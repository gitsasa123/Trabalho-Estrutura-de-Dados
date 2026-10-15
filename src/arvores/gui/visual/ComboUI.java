package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/** Caixa de seleção no visual dos campos de texto, com uma lista suspensa limpa. */
public final class ComboUI extends BasicComboBoxUI {

    public static ComponentUI createUI(JComponent c) {
        return new ComboUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        squareButton = false;
    }

    @Override
    protected JButton createArrowButton() {
        return new Seta();
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new Item();
    }

    @Override
    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox) {
            @Override
            protected void configurePopup() {
                super.configurePopup();
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Paleta.BORDA_FORTE),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
                setOpaque(true);
                setBackground(Paleta.CARTAO);
            }

            @Override
            protected JScrollPane createScroller() {
                JScrollPane rolagem = super.createScroller();
                rolagem.setBorder(null);
                rolagem.setOpaque(false);
                rolagem.getViewport().setOpaque(false);
                return rolagem;
            }
        };
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            int w = c.getWidth();
            int h = c.getHeight();
            boolean ativo = c.isEnabled();
            boolean foco = ativo && (comboBox.hasFocus() || comboBox.isPopupVisible());
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
        super.paint(g0, c);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // o fundo já foi desenhado em paint
    }

    @Override
    public void paintCurrentValue(Graphics g0, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer<? super Object> renderizador = comboBox.getRenderer();
        Component c = renderizador.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
        c.setFont(comboBox.getFont());
        c.setForeground(comboBox.isEnabled() ? Paleta.TEXTO : Paleta.TEXTO_DESABILITADO);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, false);
        } finally {
            g.dispose();
        }
    }

    /** Uma linha da lista, e também o valor mostrado na caixa fechada. */
    @SuppressWarnings("serial")
    private static final class Item extends DefaultListCellRenderer {

        private boolean destacado;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean selecionado,
                                                      boolean foco) {
            super.getListCellRendererComponent(lista, valor, indice, false, false);
            boolean naCaixa = indice < 0;
            destacado = !naCaixa && selecionado;
            setOpaque(false);
            setForeground(Paleta.TEXTO);
            setBorder(BorderFactory.createEmptyBorder(7, naCaixa ? 10 : 12, 7, naCaixa ? 2 : 12));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (destacado) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    Paleta.qualidade(g);
                    g.setColor(Paleta.SELECAO);
                    g.fill(new RoundRectangle2D.Double(0, 1, getWidth(), getHeight() - 2, 8, 8));
                } finally {
                    g.dispose();
                }
            }
            super.paintComponent(g0);
        }
    }

    /** A setinha da direita. */
    @SuppressWarnings("serial")
    private static final class Seta extends JButton {

        Seta() {
            setFocusable(false);
            setBorder(null);
            setOpaque(false);
            setContentAreaFilled(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(28, 20);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                Paleta.qualidade(g);
                Color cor = isEnabled() ? Paleta.TEXTO_2 : Paleta.TEXTO_DESABILITADO;
                Icones.CHEVRON_BAIXO.pintar(g, (getWidth() - 10) / 2.0 - 1, (getHeight() - 10) / 2.0, 10, cor);
            } finally {
                g.dispose();
            }
        }
    }
}
