package arvores.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

import arvores.Cenario;
import arvores.ResultadoBenchmark;
import arvores.ResultadoBenchmark.PontoAltura;
import arvores.TipoArvore;

/**
 * Altura de cada árvore conforme as chaves são inseridas, um painel por cenário. No cenário em
 * ordem crescente a escala é logarítmica (a BST chega a 100 mil níveis). A linha tracejada é a
 * altura mínima possível, log2(n+1).
 */
@SuppressWarnings("serial")
final class GraficoAltura extends JComponent {

    private ResultadoBenchmark resultado;

    GraficoAltura() {
        setOpaque(true);
        setBackground(Tema.FUNDO_DESENHO);
        setPreferredSize(new Dimension(900, 420));
    }

    void definir(ResultadoBenchmark r) {
        this.resultado = r;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        desenhar(g, getWidth(), getHeight());
        g.dispose();
    }

    void desenhar(Graphics2D g, int w, int h) {
        Graficos.qualidade(g);
        g.setColor(Tema.FUNDO_DESENHO);
        g.fillRect(0, 0, w, h);
        if (resultado == null || resultado.alturas().isEmpty()) {
            Graficos.textoCentrado(g, "Sem resultados para mostrar. Rode o benchmark ou abra resultados salvos.",
                    w / 2.0, h / 2.0, Graficos.FONTE_SUBTITULO, Tema.TEXTO_3);
            return;
        }
        int nMax = 0;
        for (PontoAltura p : resultado.alturas()) {
            nMax = Math.max(nMax, p.elementos());
        }
        Graficos.texto(g, "Balanceamento: altura × elementos inseridos", 18, 30, Graficos.FONTE_TITULO, Tema.TEXTO);
        Graficos.texto(g, "Altura medida durante a inserção incremental de até " + Formato.inteiro(nMax)
                + " faixas do FMA", 18, 50, Graficos.FONTE_SUBTITULO, Tema.TEXTO_2);
        Graficos.legendaArvores(g, w - 18, 30, true);

        int topo = 70;
        int entre = 20;
        double larguraPainel = (w - 18 * 2 - entre) / 2.0;
        Cenario[] cenarios = Cenario.values();
        for (int i = 0; i < cenarios.length; i++) {
            desenharPainel(g, cenarios[i], 18 + i * (larguraPainel + entre), topo, larguraPainel, h - topo - 10, nMax);
        }
    }

    private void desenharPainel(Graphics2D g, Cenario cenario, double x0, double y0, double largura, double altura,
                                int nMax) {
        double esq = 58;
        double dir = 112;
        double cima = 28;
        double baixo = 40;
        double px0 = x0 + esq;
        double px1 = x0 + largura - dir;
        double py0 = y0 + cima;
        double py1 = y0 + altura - baixo;
        boolean log = cenario == Cenario.ORDENADO;

        int alturaMax = 1;
        for (TipoArvore t : TipoArvore.values()) {
            for (PontoAltura p : resultado.serieAltura(cenario, t)) {
                alturaMax = Math.max(alturaMax, p.altura());
            }
        }
        Graficos.EscalaLog escalaLog = log ? new Graficos.EscalaLog(1, Math.max(10, alturaMax * 1.8), py1, py0) : null;
        double yMaxLinear = Math.max(4, alturaMax * 1.2);

        Graficos.texto(g, cenario.descricao(), x0, y0 + 14, Graficos.FONTE_PAINEL, Tema.TEXTO);

        // grade e rótulos do eixo y
        List<Double> graduacoes = new ArrayList<>();
        if (log) {
            graduacoes.addAll(escalaLog.graduacoes());
        } else {
            double passo = passoAgradavel(yMaxLinear / 5);
            for (double v = 0; v <= yMaxLinear; v += passo) {
                graduacoes.add(v);
            }
        }
        for (double v : graduacoes) {
            double y = log ? escalaLog.y(v) : py1 - v / yMaxLinear * (py1 - py0);
            g.setColor(Tema.LINHA);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(px0, y, px1, y));
            Graficos.textoADireita(g, Graficos.rotuloEixo(v), px0 - 6, y + 4, Graficos.FONTE_EIXO, Tema.TEXTO_2);
        }
        g.setColor(Tema.LINHA_FORTE);
        g.draw(new Line2D.Double(px0, py1, px1, py1));
        Graficos.textoGirado(g, log ? "Altura (nº de níveis), escala log" : "Altura (nº de níveis)", x0 + 10,
                (py0 + py1) / 2, Graficos.FONTE_EIXO, Tema.TEXTO_2);

        // eixo x
        for (int i = 0; i <= 4; i++) {
            double n = nMax * i / 4.0;
            double x = px0 + (px1 - px0) * i / 4.0;
            Graficos.textoCentrado(g, Formato.inteiro(Math.round(n)), x, py1 + 16, Graficos.FONTE_EIXO, Tema.TEXTO_2);
        }
        Graficos.textoCentrado(g, "Elementos inseridos", (px0 + px1) / 2, py1 + 32, Graficos.FONTE_EIXO, Tema.TEXTO_3);

        // altura mínima possível (pontos espaçados em escala log: a curva sobe rápido no começo)
        Path2D.Double referencia = new Path2D.Double();
        for (int i = 0; i <= 200; i++) {
            double n = Math.max(1, Math.pow(Math.max(nMax, 1), i / 200.0));
            double v = Math.log(n + 1) / Math.log(2);
            double x = px0 + (px1 - px0) * (nMax == 0 ? 0 : n / nMax);
            double y = log ? escalaLog.y(Math.max(1, v)) : py1 - v / yMaxLinear * (py1 - py0);
            if (i == 0) {
                referencia.moveTo(x, y);
            } else {
                referencia.lineTo(x, y);
            }
        }
        g.setColor(Tema.TEXTO_2);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {5f, 4f}, 0f));
        g.draw(referencia);

        // linhas das árvores, com marcadores e rótulo no fim
        List<double[]> fins = new ArrayList<>();
        for (TipoArvore t : TipoArvore.values()) {
            List<PontoAltura> serie = resultado.serieAltura(cenario, t);
            if (serie.isEmpty()) {
                continue;
            }
            Path2D.Double linha = new Path2D.Double();
            double[] xs = new double[serie.size()];
            double[] ys = new double[serie.size()];
            for (int i = 0; i < serie.size(); i++) {
                PontoAltura p = serie.get(i);
                xs[i] = px0 + (px1 - px0) * (nMax == 0 ? 0 : (double) p.elementos() / nMax);
                ys[i] = log ? escalaLog.y(Math.max(1, p.altura())) : py1 - p.altura() / yMaxLinear * (py1 - py0);
                if (i == 0) {
                    linha.moveTo(xs[i], ys[i]);
                } else {
                    linha.lineTo(xs[i], ys[i]);
                }
            }
            g.setColor(Tema.corDaArvore(t));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linha);
            int passoMarcador = Math.max(1, serie.size() / 7);
            for (int i = passoMarcador / 2; i < serie.size(); i += passoMarcador) {
                Graficos.marcador(g, t, xs[i], ys[i], 7);
            }
            int ultimo = serie.size() - 1;
            Graficos.marcador(g, t, xs[ultimo], ys[ultimo], 7);
            fins.add(new double[] {t.ordinal(), ys[ultimo], serie.get(ultimo).altura()});
        }

        // rótulos finais, afastados para não ficarem um em cima do outro
        fins.sort((a, b) -> Double.compare(a[1], b[1]));
        double anterior = -1e9;
        for (double[] f : fins) {
            double y = Math.max(f[1], anterior + 15);
            anterior = y;
            TipoArvore t = TipoArvore.values()[(int) f[0]];
            Graficos.marcador(g, t, px1 + 12, y, 7);
            Graficos.texto(g, t.nome() + ": " + Formato.inteiro((long) f[2]), px1 + 22, y + 4, Graficos.FONTE_EIXO,
                    Tema.TEXTO);
        }
    }

    private static double passoAgradavel(double bruto) {
        double base = Math.pow(10, Math.floor(Math.log10(bruto)));
        for (double m : new double[] {1, 2, 5, 10}) {
            if (m * base >= bruto) {
                return m * base;
            }
        }
        return 10 * base;
    }
}
