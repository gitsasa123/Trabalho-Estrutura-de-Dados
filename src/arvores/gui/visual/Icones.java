package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * Ícones desenhados em código (vetoriais): ficam nítidos em qualquer tamanho e tomam a cor do texto
 * do componente onde estão. Todos foram pensados numa grade de 24 × 24.
 */
public enum Icones {
    MAIS,
    LUPA,
    LIXEIRA,
    DADO,
    BAIXAR,
    TOCAR,
    CHECK,
    CHECK_CIRCULO,
    PASTA,
    LISTA,
    EMBARALHAR,
    LIMPAR,
    ARVORE,
    AJUSTAR,
    EXPANDIR,
    REDUZIR,
    VOLTAR,
    NOTA,
    ARQUIVO,
    CASA,
    PASTA_MAIS,
    SUBIR,
    GRADE,
    COMPUTADOR,
    DISCO,
    CHEVRON_DIREITA,
    CHEVRON_BAIXO,
    CHEVRON_CIMA,
    INFO,
    ALERTA,
    ERRO,
    PERGUNTA;

    private static final Color AZUL = new Color(0x3F6FD8);
    private static final Color AMBAR = new Color(0xE69B00);

    /** Ícone que usa a cor do texto do componente (botão, rótulo...) onde for colocado. */
    public Icon icone(int tamanho) {
        return new Vetor(this, tamanho, null);
    }

    /** Ícone de uma cor fixa. */
    public Icon icone(int tamanho, Color cor) {
        return new Vetor(this, tamanho, cor);
    }

    /**
     * Desenha o ícone com o canto de cima à esquerda em (x, y), ocupando {@code tamanho} × {@code tamanho}.
     * Nos ícones coloridos (aviso, erro...), {@code cor == null} usa a cor própria deles.
     */
    public void pintar(Graphics2D g0, double x, double y, double tamanho, Color cor) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            double escala = tamanho / 24.0;
            g.translate(x, y);
            g.scale(escala, escala);
            double pixels = Math.max(1.3, tamanho / 11.0);
            g.setStroke(new BasicStroke((float) (pixels / escala), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(cor == null ? Paleta.TEXTO_2 : cor);
            desenhar(g, cor);
        } finally {
            g.dispose();
        }
    }

    private void desenhar(Graphics2D g, Color cor) {
        switch (this) {
            case MAIS -> {
                linha(g, 12, 5, 12, 19);
                linha(g, 5, 12, 19, 12);
            }
            case LUPA -> {
                g.draw(new Ellipse2D.Double(4.5, 4.5, 12, 12));
                linha(g, 15, 15, 19.5, 19.5);
            }
            case LIXEIRA -> {
                linha(g, 4.5, 6.5, 19.5, 6.5);
                aberta(g, 9.5, 6.5, 9.5, 4.5, 14.5, 4.5, 14.5, 6.5);
                aberta(g, 6.5, 6.5, 7.5, 19.5, 16.5, 19.5, 17.5, 6.5);
                linha(g, 10, 10.5, 10, 16);
                linha(g, 14, 10.5, 14, 16);
            }
            case DADO -> {
                g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 6, 6));
                for (double[] p : new double[][] {{8.6, 8.6}, {15.4, 8.6}, {12, 12}, {8.6, 15.4}, {15.4, 15.4}}) {
                    g.fill(new Ellipse2D.Double(p[0] - 1.3, p[1] - 1.3, 2.6, 2.6));
                }
            }
            case BAIXAR -> {
                linha(g, 12, 4, 12, 15);
                aberta(g, 7, 10.5, 12, 15.5, 17, 10.5);
                aberta(g, 5, 19.5, 19, 19.5);
            }
            case TOCAR -> {
                Path2D.Double triangulo = new Path2D.Double();
                triangulo.moveTo(8.5, 5.5);
                triangulo.lineTo(18.5, 12);
                triangulo.lineTo(8.5, 18.5);
                triangulo.closePath();
                g.fill(triangulo);
                g.draw(triangulo);
            }
            case CHECK -> aberta(g, 5, 12.5, 10, 17.5, 19, 7.5);
            case CHECK_CIRCULO -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                aberta(g, 8, 12.3, 11, 15.3, 16.2, 9);
            }
            case PASTA -> {
                g.draw(new RoundRectangle2D.Double(3.5, 8.5, 17, 11, 3, 3));
                aberta(g, 3.5, 9, 3.5, 6.5, 9, 6.5, 11.2, 8.5);
            }
            case LISTA -> {
                for (double y : new double[] {7, 12, 17}) {
                    g.fill(new Ellipse2D.Double(3.7, y - 1.3, 2.6, 2.6));
                    linha(g, 9.5, y, 20, y);
                }
            }
            case EMBARALHAR -> {
                Path2D.Double a = new Path2D.Double();
                a.moveTo(4, 7);
                a.lineTo(8, 7);
                a.curveTo(11.5, 7, 12.5, 17, 16, 17);
                a.lineTo(19, 17);
                g.draw(a);
                Path2D.Double b = new Path2D.Double();
                b.moveTo(4, 17);
                b.lineTo(8, 17);
                b.curveTo(11.5, 17, 12.5, 7, 16, 7);
                b.lineTo(19, 7);
                g.draw(b);
                aberta(g, 17, 4.5, 19.8, 7, 17, 9.5);
                aberta(g, 17, 14.5, 19.8, 17, 17, 19.5);
            }
            case LIMPAR -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                linha(g, 9, 9, 15, 15);
                linha(g, 15, 9, 9, 15);
            }
            case ARVORE -> {
                linha(g, 10.7, 8.8, 7.3, 15.2);
                linha(g, 13.3, 8.8, 16.7, 15.2);
                g.draw(new Ellipse2D.Double(9.4, 3.4, 5.2, 5.2));
                g.draw(new Ellipse2D.Double(3.4, 15.4, 5.2, 5.2));
                g.draw(new Ellipse2D.Double(15.4, 15.4, 5.2, 5.2));
            }
            case AJUSTAR -> {
                aberta(g, 4, 10, 4, 4, 10, 4);
                aberta(g, 14, 4, 20, 4, 20, 10);
                aberta(g, 20, 14, 20, 20, 14, 20);
                aberta(g, 10, 20, 4, 20, 4, 14);
            }
            case EXPANDIR -> {
                linha(g, 5.5, 18.5, 18.5, 5.5);
                aberta(g, 13, 5, 19, 5, 19, 11);
                aberta(g, 11, 19, 5, 19, 5, 13);
            }
            case REDUZIR -> {
                aberta(g, 4, 14, 10, 14, 10, 20);
                aberta(g, 20, 10, 14, 10, 14, 4);
                linha(g, 14, 10, 20.5, 3.5);
                linha(g, 10, 14, 3.5, 20.5);
            }
            case VOLTAR -> {
                aberta(g, 10, 6, 4, 12, 10, 18);
                linha(g, 4, 12, 20, 12);
            }
            case ARQUIVO -> {
                fechada(g, 6.5, 3.5, 13.5, 3.5, 18.5, 8.5, 18.5, 20.5, 6.5, 20.5);
                aberta(g, 13.5, 3.5, 13.5, 8.5, 18.5, 8.5);
            }
            case CASA -> {
                aberta(g, 3.5, 11.5, 12, 4, 20.5, 11.5);
                aberta(g, 6, 10, 6, 20, 18, 20, 18, 10);
                aberta(g, 10, 20, 10, 14.5, 14, 14.5, 14, 20);
            }
            case PASTA_MAIS -> {
                g.draw(new RoundRectangle2D.Double(3.5, 8.5, 17, 11, 3, 3));
                aberta(g, 3.5, 9, 3.5, 6.5, 9, 6.5, 11.2, 8.5);
                linha(g, 12, 11.8, 12, 16.4);
                linha(g, 9.7, 14.1, 14.3, 14.1);
            }
            case SUBIR -> {
                linha(g, 12, 19, 12, 5.5);
                aberta(g, 6.5, 11, 12, 5.5, 17.5, 11);
            }
            case GRADE -> {
                for (double gx : new double[] {4, 13.5}) {
                    for (double gy : new double[] {4, 13.5}) {
                        g.draw(new RoundRectangle2D.Double(gx, gy, 6.5, 6.5, 2.2, 2.2));
                    }
                }
            }
            case COMPUTADOR -> {
                g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 11, 2.4, 2.4));
                linha(g, 12, 16, 12, 19.5);
                linha(g, 8, 19.5, 16, 19.5);
            }
            case DISCO -> {
                g.draw(new RoundRectangle2D.Double(3.5, 8, 17, 9, 2.6, 2.6));
                linha(g, 6.8, 12.5, 11.5, 12.5);
                g.fill(new Ellipse2D.Double(15.4, 11.4, 2.2, 2.2));
            }
            case NOTA -> {
                g.fill(new Ellipse2D.Double(4.8, 14.6, 6.6, 5.4));
                linha(g, 10.9, 17, 10.9, 4.8);
                Path2D.Double bandeira = new Path2D.Double();
                bandeira.moveTo(10.9, 4.8);
                bandeira.curveTo(12.6, 7.4, 17.6, 7.2, 17.6, 12.2);
                g.draw(bandeira);
            }
            case CHEVRON_DIREITA -> aberta(g, 9, 6, 15, 12, 9, 18);
            case CHEVRON_BAIXO -> aberta(g, 6, 9, 12, 15, 18, 9);
            case CHEVRON_CIMA -> aberta(g, 6, 15, 12, 9, 18, 15);
            case INFO -> {
                g.setColor(cor == null ? AZUL : cor);
                g.fill(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(10.7, 6.5, 2.6, 2.6));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                linha(g, 12, 11.4, 12, 17);
            }
            case ALERTA -> {
                g.setColor(cor == null ? AMBAR : cor);
                Path2D.Double triangulo = new Path2D.Double();
                triangulo.moveTo(12, 4);
                triangulo.lineTo(21, 19.5);
                triangulo.lineTo(3, 19.5);
                triangulo.closePath();
                g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(triangulo);
                g.draw(triangulo);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(10.8, 15.1, 2.4, 2.4));
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                linha(g, 12, 9.3, 12, 13);
            }
            case ERRO -> {
                g.setColor(cor == null ? Paleta.PERIGO : cor);
                g.fill(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                linha(g, 8.6, 8.6, 15.4, 15.4);
                linha(g, 15.4, 8.6, 8.6, 15.4);
            }
            case PERGUNTA -> {
                g.setColor(cor == null ? AZUL : cor);
                g.fill(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setColor(Color.WHITE);
                g.setFont(Paleta.fonte(15f, Font.BOLD));
                FontMetrics fm = g.getFontMetrics();
                double largura = fm.stringWidth("?");
                g.drawString("?", (float) (12 - largura / 2), (float) (12 + (fm.getAscent() - fm.getDescent()) / 2.0));
            }
        }
    }

    private static void linha(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /** Linha quebrada aberta passando pelos pontos (x1, y1, x2, y2, ...). */
    private static void aberta(Graphics2D g, double... pontos) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(pontos[0], pontos[1]);
        for (int i = 2; i < pontos.length; i += 2) {
            p.lineTo(pontos[i], pontos[i + 1]);
        }
        g.draw(p);
    }

    /** Polígono fechado passando pelos pontos (x1, y1, x2, y2, ...). */
    private static void fechada(Graphics2D g, double... pontos) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(pontos[0], pontos[1]);
        for (int i = 2; i < pontos.length; i += 2) {
            p.lineTo(pontos[i], pontos[i + 1]);
        }
        p.closePath();
        g.draw(p);
    }

    /** Ícones coloridos (aviso, erro...) não mudam de cor com o componente. */
    private boolean colorido() {
        return this == INFO || this == ALERTA || this == ERRO || this == PERGUNTA;
    }

    private static final class Vetor implements Icon, UIResource {
        private final Icones forma;
        private final int tamanho;
        private final Color fixa;

        Vetor(Icones forma, int tamanho, Color fixa) {
            this.forma = forma;
            this.tamanho = tamanho;
            this.fixa = fixa;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color cor = fixa;
            if (cor == null && !forma.colorido()) {
                cor = (c instanceof AbstractButton b) ? BotaoUI.corDoTexto(b)
                        : (c != null ? c.getForeground() : Paleta.TEXTO_2);
            }
            forma.pintar((Graphics2D) g, x, y, tamanho, cor);
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
}
