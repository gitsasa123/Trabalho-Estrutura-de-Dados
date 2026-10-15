package arvores.gui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import arvores.Cenario;
import arvores.Operacao;
import arvores.ResultadoBenchmark;
import arvores.ResultadoBenchmark.LinhaDesempenho;
import arvores.TipoArvore;

/**
 * Barras agrupadas (uma barra por árvore, um grupo por operação) em escala logarítmica, um painel
 * por cenário. Serve para o tempo e para as comparações: é o mesmo gráfico dos PNGs do projeto.
 */
@SuppressWarnings("serial")
final class GraficoBarras extends JComponent {

    private record Alvo(Rectangle2D area, String dica) {
    }

    private final String titulo;
    private final String rotuloY;
    private final ToDoubleFunction<LinhaDesempenho> valor;
    private final ToDoubleFunction<LinhaDesempenho> erro;
    private final DoubleFunction<String> rotuloBarra;
    private final DoubleFunction<String> dica;
    private final Function<ResultadoBenchmark, String> subtitulo;

    private ResultadoBenchmark resultado;
    private List<Alvo> alvos = List.of();

    GraficoBarras(String titulo, Function<ResultadoBenchmark, String> subtitulo, String rotuloY,
                  ToDoubleFunction<LinhaDesempenho> valor, ToDoubleFunction<LinhaDesempenho> erro,
                  DoubleFunction<String> rotuloBarra, DoubleFunction<String> dica) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.rotuloY = rotuloY;
        this.valor = valor;
        this.erro = erro;
        this.rotuloBarra = rotuloBarra;
        this.dica = dica;
        setOpaque(true);
        setBackground(Tema.FUNDO_DESENHO);
        setPreferredSize(new Dimension(900, 420));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    void definir(ResultadoBenchmark r) {
        this.resultado = r;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        List<Alvo> novos = new ArrayList<>();
        desenhar(g, getWidth(), getHeight(), novos);
        g.dispose();
        alvos = novos;   // só o desenho da tela define onde ficam as dicas (o do PNG tem outras medidas)
    }

    /** Desenha em qualquer tamanho para gravar o PNG. */
    void desenhar(Graphics2D g, int w, int h) {
        desenhar(g, w, h, new ArrayList<>());
    }

    private void desenhar(Graphics2D g, int w, int h, List<Alvo> destino) {
        Graficos.qualidade(g);
        g.setColor(Tema.FUNDO_DESENHO);
        g.fillRect(0, 0, w, h);
        if (resultado == null || resultado.desempenho().isEmpty()) {
            Graficos.textoCentrado(g, "Sem resultados para mostrar. Rode o benchmark ou abra resultados salvos.",
                    w / 2.0, h / 2.0, Graficos.FONTE_SUBTITULO, Tema.TEXTO_3);
            return;
        }

        Graficos.texto(g, titulo, 18, 30, Graficos.FONTE_TITULO, Tema.TEXTO);
        Graficos.texto(g, subtitulo.apply(resultado), 18, 50, Graficos.FONTE_SUBTITULO, Tema.TEXTO_2);
        Graficos.legendaArvores(g, w - 18, 30, false);

        double menor = Double.MAX_VALUE;
        double maior = 0;
        for (LinhaDesempenho l : resultado.desempenho()) {
            double v = valor.applyAsDouble(l);
            if (v > 0) {
                menor = Math.min(menor, v);
                maior = Math.max(maior, v + erro.applyAsDouble(l));
            }
        }
        double[] limites = Graficos.limitesLog(menor == Double.MAX_VALUE ? 1 : menor, Math.max(maior, 10));

        int topo = 70;
        int entre = 28;
        double larguraPainel = (w - 18 * 2 - entre) / 2.0;
        Cenario[] cenarios = Cenario.values();
        for (int i = 0; i < cenarios.length; i++) {
            desenharPainel(g, cenarios[i], 18 + i * (larguraPainel + entre), topo, larguraPainel, h - topo - 10,
                    limites, i == 0, destino);
        }
    }

    private void desenharPainel(Graphics2D g, Cenario cenario, double x0, double y0, double largura, double altura,
                                double[] limites, boolean comEixoY, List<Alvo> destino) {
        double esq = 58;
        double dir = 6;
        double cima = 28;
        double baixo = 46;
        double px0 = x0 + esq;
        double px1 = x0 + largura - dir;
        double py0 = y0 + cima;
        double py1 = y0 + altura - baixo;
        Graficos.EscalaLog escala = new Graficos.EscalaLog(limites[0], limites[1], py1, py0);

        Graficos.texto(g, cenario.descricao(), x0, y0 + 14, Graficos.FONTE_PAINEL, Tema.TEXTO);

        for (double v : escala.graduacoes()) {
            double y = escala.y(v);
            g.setColor(Tema.LINHA);
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(px0, y, px1, y));
            Graficos.textoADireita(g, Graficos.rotuloEixo(v), px0 - 6, y + 4, Graficos.FONTE_EIXO, Tema.TEXTO_2);
        }
        g.setColor(Tema.LINHA_FORTE);
        g.draw(new Line2D.Double(px0, py1, px1, py1));
        if (comEixoY) {
            Graficos.textoGirado(g, rotuloY, x0 + 10, (py0 + py1) / 2, Graficos.FONTE_EIXO, Tema.TEXTO_2);
        }

        Operacao[] operacoes = Operacao.values();
        TipoArvore[] arvores = TipoArvore.values();
        double grupo = (px1 - px0) / operacoes.length;
        double larguraBarra = Math.min(46, grupo / (arvores.length + 1.2));
        for (int o = 0; o < operacoes.length; o++) {
            double centro = px0 + grupo * (o + 0.5);
            String[] linhas = quebrar(operacoes[o].descricao());
            Graficos.textoCentrado(g, linhas[0], centro, py1 + 16, Graficos.FONTE_EIXO, Tema.TEXTO_2);
            if (!linhas[1].isEmpty()) {
                Graficos.textoCentrado(g, linhas[1], centro, py1 + 30, Graficos.FONTE_EIXO, Tema.TEXTO_3);
            }
            for (int a = 0; a < arvores.length; a++) {
                Optional<LinhaDesempenho> linha = resultado.linha(cenario, arvores[a], operacoes[o]);
                if (linha.isEmpty()) {
                    continue;
                }
                double v = valor.applyAsDouble(linha.get());
                double e = erro.applyAsDouble(linha.get());
                double bx = centro + (a - (arvores.length - 1) / 2.0) * larguraBarra - larguraBarra * 0.45;
                double yTopo = escala.y(v);
                Rectangle2D.Double barra = new Rectangle2D.Double(bx, yTopo, larguraBarra * 0.9, Math.max(1, py1 - yTopo));
                Graficos.preencherBarra(g, barra, arvores[a]);

                double yRotulo = yTopo;
                if (e > 0) {
                    double yCima = escala.y(v + e);
                    double yBaixo = escala.y(Math.max(v - e, limites[0]));
                    double cx = bx + larguraBarra * 0.45;
                    g.setColor(Tema.TEXTO_2);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(new Line2D.Double(cx, yCima, cx, yBaixo));
                    g.draw(new Line2D.Double(cx - 3, yCima, cx + 3, yCima));
                    g.draw(new Line2D.Double(cx - 3, yBaixo, cx + 3, yBaixo));
                    yRotulo = yCima;
                }
                if (larguraBarra >= 24) {
                    Graficos.textoCentrado(g, rotuloBarra.apply(v), bx + larguraBarra * 0.45, yRotulo - 4,
                            Graficos.FONTE_VALOR, Tema.TEXTO_2);
                }
                destino.add(new Alvo(new Rectangle2D.Double(bx, Math.min(yRotulo, yTopo) - 12, larguraBarra * 0.9,
                        py1 - Math.min(yRotulo, yTopo) + 12),
                        "<html><b>" + arvores[a].nome() + "</b> · " + operacoes[o].descricao() + "<br>" + dica.apply(v)
                                + (e > 0 ? " (± " + Formato.decimal(e, 1) + ")" : "") + "</html>"));
            }
        }
    }

    /** "Busca (chave existente)" → {"Busca", "(chave existente)"}. */
    private static String[] quebrar(String descricao) {
        int i = descricao.indexOf(" (");
        return i < 0 ? new String[] {descricao, ""} : new String[] {descricao.substring(0, i), descricao.substring(i + 1)};
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (Alvo a : alvos) {
            if (a.area().contains(e.getX(), e.getY())) {
                return a.dica();
            }
        }
        return null;
    }
}
