package arvores.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import arvores.TipoArvore;

/** Peças comuns dos gráficos: escala logarítmica, rótulos, texturas e marcadores. */
final class Graficos {

    static final Font FONTE_TITULO = Tema.fonte(16f, Font.BOLD);
    static final Font FONTE_SUBTITULO = Tema.fonte(11.5f, Font.PLAIN);
    static final Font FONTE_EIXO = Tema.fonte(10.5f, Font.PLAIN);
    static final Font FONTE_PAINEL = Tema.fonte(12f, Font.BOLD);
    static final Font FONTE_VALOR = Tema.fonte(9.5f, Font.PLAIN);

    private Graficos() {
    }

    static void qualidade(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ------------------------------------------------------------------ escala logarítmica

    /** Mapeia valores positivos para a altura na tela, em escala logarítmica. */
    record EscalaLog(double minimo, double maximo, double yBaixo, double yAlto) {

        double y(double valor) {
            double v = Math.max(valor, minimo);
            double t = (Math.log10(v) - Math.log10(minimo)) / (Math.log10(maximo) - Math.log10(minimo));
            return yBaixo - t * (yBaixo - yAlto);
        }

        /** Potências de 10 dentro do intervalo. */
        List<Double> graduacoes() {
            List<Double> lista = new ArrayList<>();
            for (double v = Math.pow(10, Math.ceil(Math.log10(minimo) - 1e-9)); v <= maximo * 1.0001; v *= 10) {
                lista.add(v);
            }
            return lista;
        }
    }

    /**
     * Limites que cobrem todos os valores positivos: embaixo, a potência de 10 logo abaixo do menor
     * valor; em cima, uma folga para os rótulos das barras (sem arredondar, para não sobrar espaço vazio).
     */
    static double[] limitesLog(double menorPositivo, double maior) {
        double lo = Math.pow(10, Math.floor(Math.log10(Math.max(menorPositivo / 2, 1e-3))));
        double hi = Math.max(maior * 3.2, lo * 10);
        return new double[] {lo, hi};
    }

    /** Rótulo de eixo: 0,1 | 1 | 100 | 1.000 | 10 mil | 1 mi | 1 bi. */
    static String rotuloEixo(double v) {
        if (v == 0) {
            return "0";
        }
        if (v >= 1e9) {
            return Formato.decimal(v / 1e9, 0) + " bi";
        }
        if (v >= 1e6) {
            return Formato.decimal(v / 1e6, 0) + " mi";
        }
        if (v >= 1e4) {
            return Formato.decimal(v / 1e3, 0) + " mil";
        }
        return v < 1 ? Formato.decimal(v, v >= 0.1 ? 1 : v >= 0.01 ? 2 : 3) : Formato.inteiro(Math.round(v));
    }

    /** Rótulo sobre a barra de tempo: 1,2 | 45 | 1.234. */
    static String rotuloTempo(double ms) {
        return ms >= 10 ? Formato.decimal(ms, 0) : ms >= 1 ? Formato.decimal(ms, 1) : ms >= 0.1 ? Formato.decimal(ms, 2)
                : Formato.decimal(ms, 3);
    }

    // ------------------------------------------------------------------ desenho de texto

    static void texto(Graphics2D g, String texto, double x, double y, Font fonte, Color cor) {
        g.setFont(fonte);
        g.setColor(cor);
        g.drawString(texto, (float) x, (float) y);
    }

    static void textoCentrado(Graphics2D g, String texto, double cx, double y, Font fonte, Color cor) {
        g.setFont(fonte);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(cor);
        g.drawString(texto, (float) (cx - fm.stringWidth(texto) / 2.0), (float) y);
    }

    static void textoADireita(Graphics2D g, String texto, double xDireita, double y, Font fonte, Color cor) {
        g.setFont(fonte);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(cor);
        g.drawString(texto, (float) (xDireita - fm.stringWidth(texto)), (float) y);
    }

    static void textoGirado(Graphics2D g, String texto, double x, double yCentro, Font fonte, Color cor) {
        AffineTransform antes = g.getTransform();
        g.translate(x, yCentro);
        g.rotate(-Math.PI / 2);
        textoCentrado(g, texto, 0, 0, fonte, cor);
        g.setTransform(antes);
    }

    // ------------------------------------------------------------------ texturas e marcadores

    /** Textura de cada árvore nas barras: lisa, listras diagonais, xadrez de linhas (para não depender só da cor). */
    static void preencherBarra(Graphics2D g, Shape forma, TipoArvore tipo) {
        Color cor = Tema.corDaArvore(tipo);
        g.setColor(cor);
        g.fill(forma);
        if (tipo == TipoArvore.BST) {
            return;
        }
        int lado = 8;
        BufferedImage tile = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D t = tile.createGraphics();
        t.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        t.setColor(new Color(255, 255, 255, 120));
        t.setStroke(new BasicStroke(1.4f));
        t.drawLine(-1, lado + 1, lado + 1, -1);
        t.drawLine(-1, 1, 1, -1);
        t.drawLine(lado - 1, lado + 1, lado + 1, lado - 1);
        if (tipo == TipoArvore.RUBRO_NEGRA) {
            t.drawLine(-1, -1, lado + 1, lado + 1);
        }
        t.dispose();
        g.setPaint(new TexturePaint(tile, new Rectangle2D.Double(0, 0, lado, lado)));
        g.fill(forma);
    }

    /** Marcador da linha de cada árvore: círculo, quadrado, triângulo. */
    static void marcador(Graphics2D g, TipoArvore tipo, double x, double y, double tamanho) {
        double r = tamanho / 2;
        Shape forma = switch (tipo) {
            case BST -> new Ellipse2D.Double(x - r, y - r, tamanho, tamanho);
            case AVL -> new Rectangle2D.Double(x - r, y - r, tamanho, tamanho);
            case RUBRO_NEGRA -> {
                Path2D.Double t = new Path2D.Double();
                t.moveTo(x, y - r * 1.15);
                t.lineTo(x + r * 1.1, y + r * 0.9);
                t.lineTo(x - r * 1.1, y + r * 0.9);
                t.closePath();
                yield t;
            }
        };
        g.setColor(Tema.corDaArvore(tipo));
        g.fill(forma);
        g.setColor(Tema.FUNDO_DESENHO);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(forma);
    }

    /** Legenda horizontal com os nomes das árvores, alinhada à direita em {@code xDireita}. */
    static void legendaArvores(Graphics2D g, double xDireita, double y, boolean comReferencia) {
        g.setFont(FONTE_EIXO);
        FontMetrics fm = g.getFontMetrics();
        List<String> nomes = new ArrayList<>();
        for (TipoArvore t : TipoArvore.values()) {
            nomes.add(t.nome());
        }
        if (comReferencia) {
            nomes.add("log2(n+1)");
        }
        double largura = 0;
        for (String n : nomes) {
            largura += 22 + fm.stringWidth(n) + 14;
        }
        double x = xDireita - largura;
        for (int i = 0; i < nomes.size(); i++) {
            if (i < TipoArvore.values().length) {
                TipoArvore t = TipoArvore.values()[i];
                preencherBarra(g, new Rectangle2D.Double(x, y - 10, 13, 13), t);
            } else {
                g.setColor(Tema.TEXTO_2);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 3f}, 0f));
                g.drawLine((int) x - 1, (int) y - 4, (int) x + 14, (int) y - 4);
            }
            texto(g, nomes.get(i), x + 20, y, FONTE_EIXO, Tema.TEXTO);
            x += 22 + fm.stringWidth(nomes.get(i)) + 14;
        }
    }

    static AlphaComposite alfa(float a) {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
    }
}
