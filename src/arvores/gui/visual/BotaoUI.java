package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Botões chatos e arredondados, com estado ao passar o mouse e ao apertar. O jeito de desenhar vem de
 * {@link Estilo}: padrão (branco com contorno), primário (escuro), perigo (vermelho), texto (sem fundo)
 * e ícone.
 */
public final class BotaoUI extends BasicButtonUI {

    /** Os jeitos de desenhar um botão. */
    public enum Tipo {
        PADRAO,
        PRIMARIO,
        PERIGO,
        TEXTO,
        ICONE
    }

    static final String CHAVE_TIPO = "arvores.botao.tipo";
    static final String CHAVE_FOCO_POR_MOUSE = "arvores.botao.focoPorMouse";
    private static final double RAIO = 9;

    private static final BotaoUI INSTANCIA = new BotaoUI();

    /** Quem clica com o mouse não precisa ver o anel de foco; só quem navega pelo teclado. */
    static final MouseListener OUVINTE_MOUSE = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            ((JComponent) e.getSource()).putClientProperty(CHAVE_FOCO_POR_MOUSE, Boolean.TRUE);
        }
    };

    static final FocusListener OUVINTE_FOCO = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            ((JComponent) e.getSource()).putClientProperty(CHAVE_FOCO_POR_MOUSE, null);
        }
    };

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
        b.addMouseListener(OUVINTE_MOUSE);
        b.addFocusListener(OUVINTE_FOCO);
    }

    @Override
    protected void uninstallListeners(AbstractButton b) {
        b.removeMouseListener(OUVINTE_MOUSE);
        b.removeFocusListener(OUVINTE_FOCO);
        super.uninstallListeners(b);
    }

    // ------------------------------------------------------------------ estilo

    static Tipo tipoDe(AbstractButton b) {
        Object p = b.getClientProperty(CHAVE_TIPO);
        if (p instanceof Tipo t) {
            return t;
        }
        // o botão "padrão" de um diálogo (o que o Enter aciona) ganha destaque
        if (b instanceof JButton jb && jb.isDefaultButton()) {
            return Tipo.PRIMARIO;
        }
        return Tipo.PADRAO;
    }

    /** Cor do texto (e dos ícones) do botão no estado atual. */
    public static Color corDoTexto(AbstractButton b) {
        Tipo tipo = tipoDe(b);
        if (!b.isEnabled()) {
            return tipo == Tipo.PRIMARIO ? Color.WHITE : Paleta.TEXTO_DESABILITADO;
        }
        Color propria = b.getForeground();
        if (propria != null && !(propria instanceof UIResource)) {
            return propria;
        }
        ButtonModel m = b.getModel();
        return switch (tipo) {
            case PRIMARIO -> Color.WHITE;
            case PERIGO -> Paleta.PERIGO;
            case TEXTO, ICONE -> (m.isRollover() || m.isPressed() || m.isSelected()) ? Paleta.TEXTO : Paleta.TEXTO_2;
            case PADRAO -> Paleta.TEXTO;
        };
    }

    // ------------------------------------------------------------------ pintura

    @Override
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            if (b.isContentAreaFilled()) {
                desenharFundo(g, b);
            }
            super.paint(g, c);
        } finally {
            g.dispose();
        }
    }

    private void desenharFundo(Graphics2D g, AbstractButton b) {
        Tipo tipo = tipoDe(b);
        ButtonModel m = b.getModel();
        boolean ativo = b.isEnabled();
        boolean apertado = ativo && m.isPressed() && m.isArmed();
        boolean sobre = ativo && m.isRollover();
        boolean marcado = ativo && m.isSelected();
        int w = b.getWidth();
        int h = b.getHeight();
        double raio = (tipo == Tipo.ICONE) ? 8 : RAIO;
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, raio * 2, raio * 2);

        Color fundo;
        Color contorno = null;
        switch (tipo) {
            case PRIMARIO -> {
                fundo = !ativo ? Paleta.PRIMARIO_DESABILITADO
                        : apertado ? Paleta.PRIMARIO_PRESSIONADO : sobre ? Paleta.PRIMARIO_HOVER : Paleta.PRIMARIO;
            }
            case PERIGO -> {
                fundo = !ativo ? Paleta.FUNDO_DESABILITADO
                        : apertado ? Paleta.misturar(Paleta.PERIGO_FUNDO, Paleta.PERIGO_BORDA, 0.5)
                        : sobre ? Paleta.PERIGO_FUNDO : Paleta.CARTAO;
                contorno = ativo ? Paleta.PERIGO_BORDA : Paleta.BORDA;
            }
            case TEXTO, ICONE -> {
                fundo = apertado ? Paleta.PRESSIONADO : (sobre || marcado) ? Paleta.HOVER : null;
                if (marcado && !sobre) {
                    fundo = Paleta.SELECAO;
                }
            }
            default -> {
                fundo = !ativo ? Paleta.FUNDO_DESABILITADO
                        : apertado ? Paleta.PRESSIONADO : (sobre || marcado) ? Paleta.HOVER : Paleta.CARTAO;
                contorno = !ativo ? Paleta.BORDA : sobre ? Paleta.misturar(Paleta.BORDA_FORTE, Paleta.TEXTO_3, 0.35)
                        : Paleta.BORDA_FORTE;
            }
        }
        if (fundo != null) {
            g.setColor(fundo);
            g.fill(forma);
        }
        if (contorno != null && b.isBorderPainted()) {
            g.setColor(contorno);
            g.draw(forma);
        }
        if (ativo && b.isFocusPainted() && b.hasFocus()
                && !Boolean.TRUE.equals(b.getClientProperty(CHAVE_FOCO_POR_MOUSE))) {
            g.setColor(Paleta.comAlfa(Paleta.FOCO, 0.85));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, raio * 2 - 2, raio * 2 - 2));
        }
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        g.setFont(b.getFont());
        g.setColor(corDoTexto(b));
        int linhaBase = textRect.y + g.getFontMetrics().getAscent();
        g.drawString(text, textRect.x + getTextShiftOffset(), linhaBase + getTextShiftOffset());
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect,
                              Rectangle iconRect) {
        // o anel de foco é desenhado junto com o fundo
    }
}
