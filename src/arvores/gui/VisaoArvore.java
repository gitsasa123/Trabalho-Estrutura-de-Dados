package arvores.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import arvores.Cor;
import arvores.Faixa;
import arvores.TipoArvore;

/**
 * Desenha uma árvore (BST, AVL ou Rubro-Negra) e anima as mudanças: quando uma inserção ou
 * remoção rearranja a árvore, os nós deslizam até a nova posição, então as rotações aparecem.
 * Também mostra o caminho de uma busca passo a passo.
 *
 * <p>Mouse: arrastar move, a rodinha dá zoom, clique seleciona um nó, clique num triângulo abre a
 * subárvore escondida, duplo clique no fundo ajusta à janela e o botão direito abre um menu.
 * Tudo roda na thread de eventos do Swing.
 */
@SuppressWarnings("serial")
final class VisaoArvore extends JComponent {

    enum TipoMarca { VISITANDO, VISITADO, ENCONTRADO, NAO_ENCONTRADO, INSERIDO, REMOVENDO, PERCURSO }

    private record Marca(TipoMarca tipo, int numero, long inicioNs) {
    }

    /** Quem trata os pedidos feitos com o mouse (o painel que coordena as três árvores). */
    interface Ouvinte {

        void noSelecionado(TipoArvore tipo, NoDesenho no);

        /** {@code chave == null} pede para voltar à raiz da árvore. */
        void focoSolicitado(TipoArvore tipo, Integer chave);

        void buscaSolicitada(int chave);

        void remocaoSolicitada(int chave);
    }

    /** Um elemento como aparece neste quadro: posição em pixels lógicos (antes do zoom) e transparência. */
    private static final class Elemento {
        final NoDesenho no;
        double x;
        double y;
        double alfa = 1;
        Color preenchimento;

        Elemento(NoDesenho no) {
            this.no = no;
        }
    }

    private record Aresta(String pai, String filho, double x1, double y1, double x2, double y2, double alfa,
                          NoDesenho.Tipo tipoFilho) {
    }

    private static final Font FONTE_NO = Tema.fonte(12f, Font.BOLD);
    private static final Font FONTE_PEQUENA = Tema.fonte(9.5f, Font.BOLD);
    private static final Font FONTE_ROTULO = Tema.fonte(10.5f, Font.PLAIN);
    private static final double RAIO_MINIMO = 16;
    private static final double ESCALA_MINIMA = 0.03;
    private static final double ESCALA_MAXIMA = 3.5;
    private static final double ESCALA_AJUSTE_MAXIMA = 1.25;
    private static final int MARGEM = 26;
    private static final long DURACAO_TRANSICAO_MS = 600;

    private final TipoArvore tipo;
    private final Ouvinte ouvinte;
    private final LinhaDoTempo linha = new LinhaDoTempo(this::repaint);

    private Desenho atual = Desenho.VAZIO;
    private Desenho origem;
    private Desenho destino;
    private double progresso;
    private final Map<String, Marca> marcas = new HashMap<>();
    private String idSelecionado;
    private String idSobMouse;
    private String mensagem;

    private boolean mostrarFatores = true;
    private boolean animar = true;
    private double velocidade = 1.0;

    private double raio = RAIO_MINIMO;
    private double espacoX = 2 * RAIO_MINIMO + 14;
    private double espacoY = 2 * RAIO_MINIMO + 30;

    private double escala = 1;
    private double tx;
    private double ty;
    private boolean ajusteAutomatico = true;
    private double escalaIni;
    private double txIni;
    private double tyIni;
    private double escalaFim;
    private double txFim;
    private double tyFim;

    private int arrasteX;
    private int arrasteY;
    private boolean arrastando;

    VisaoArvore(TipoArvore tipo, Ouvinte ouvinte) {
        this.tipo = tipo;
        this.ouvinte = ouvinte;
        setOpaque(true);
        setBackground(Tema.FUNDO_DESENHO);
        setPreferredSize(new Dimension(320, 300));
        setMinimumSize(new Dimension(160, 120));
        ToolTipManager.sharedInstance().registerComponent(this);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                arrasteX = e.getX();
                arrasteY = e.getY();
                arrastando = false;
                if (e.isPopupTrigger()) {
                    menuDeContexto(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) && !SwingUtilities.isMiddleMouseButton(e)) {
                    return;
                }
                int dx = e.getX() - arrasteX;
                int dy = e.getY() - arrasteY;
                if (!arrastando && Math.abs(dx) + Math.abs(dy) < 4) {
                    return;
                }
                arrastando = true;
                congelarCamera();
                tx += dx;
                ty += dy;
                arrasteX = e.getX();
                arrasteY = e.getY();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menuDeContexto(e);
                } else if (arrastando) {
                    setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (arrastando || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                Elemento sob = elementoEm(e.getX(), e.getY());
                if (e.getClickCount() >= 2) {
                    if (sob == null) {
                        ajustar();
                    } else if (sob.no.tipo == NoDesenho.Tipo.NO) {
                        ouvinte.focoSolicitado(tipo, sob.no.chave);
                    }
                    return;
                }
                if (sob == null) {
                    return;
                }
                if (sob.no.tipo == NoDesenho.Tipo.MARCADOR) {
                    ouvinte.focoSolicitado(tipo, sob.no.chave);
                } else if (sob.no.tipo == NoDesenho.Tipo.NO) {
                    idSelecionado = sob.no.id;
                    repaint();
                    ouvinte.noSelecionado(tipo, sob.no);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Elemento sob = elementoEm(e.getX(), e.getY());
                String id = (sob == null) ? null : sob.no.id;
                if (!Objects.equals(id, idSobMouse)) {
                    idSobMouse = id;
                    boolean clicavel = sob != null
                            && (sob.no.tipo == NoDesenho.Tipo.NO || sob.no.tipo == NoDesenho.Tipo.MARCADOR);
                    setCursor(clicavel ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (idSobMouse != null) {
                    idSobMouse = null;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                congelarCamera();
                double fator = Math.pow(1.13, -e.getPreciseWheelRotation());
                double nova = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, escala * fator));
                double razao = nova / escala;
                tx = e.getX() - (e.getX() - tx) * razao;
                ty = e.getY() - (e.getY() - ty) * razao;
                escala = nova;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (ajusteAutomatico && destino == null) {
                    aplicarAjuste(atual);
                    repaint();
                }
            }
        });
    }

    // ------------------------------------------------------------------ configuração

    void configurar(boolean animar, double velocidade, boolean mostrarFatores) {
        this.animar = animar;
        this.velocidade = Math.max(0.1, velocidade);
        this.mostrarFatores = mostrarFatores;
        repaint();
    }

    /** Texto no meio da tela por cima do desenho (por exemplo, enquanto o dataset carrega); null remove. */
    void definirMensagem(String texto) {
        this.mensagem = texto;
        repaint();
    }

    /** Volta a enquadrar a árvore inteira na janela. */
    void ajustar() {
        ajusteAutomatico = true;
        aplicarAjuste(destino != null ? destino : atual);
        repaint();
    }

    Desenho desenhoAtual() {
        return destino != null ? destino : atual;
    }

    void selecionar(String id) {
        idSelecionado = id;
        repaint();
    }

    /** Termina na hora qualquer animação em andamento. */
    void terminarAnimacao() {
        linha.pular();
    }

    boolean animando() {
        return linha.ativa();
    }

    // ------------------------------------------------------------------ sequências de animação

    /** Monta uma sequência de etapas (transição, visita de caminho, marcas...) e a executa de uma vez. */
    final class Sequencia {
        private final List<LinhaDoTempo.Etapa> etapas = new ArrayList<>();
        private final double aceleracao;

        Sequencia(double aceleracao) {
            this.aceleracao = aceleracao;
        }

        /** Anima até este desenho (os nós deslizam; novos aparecem; removidos somem). */
        Sequencia mostrar(Desenho alvo) {
            etapas.add(new Transicao(alvo, aceleracao));
            return this;
        }

        /** Acende o caminho nó por nó. {@code chaves} são as chaves visitadas, da raiz para baixo. */
        Sequencia visitar(List<Integer> chaves) {
            etapas.add(new Visita(chaves, aceleracao));
            return this;
        }

        Sequencia marcar(String id, TipoMarca marca) {
            etapas.add(new Marcar(id, marca));
            return this;
        }

        /** Numera os nós na ordem dada (percurso), um de cada vez. */
        Sequencia percurso(List<String> ids) {
            etapas.add(new Percurso(ids, aceleracao));
            return this;
        }

        Sequencia pausa(long ms) {
            etapas.add(() -> (long) (ms / (velocidade * aceleracao)));
            return this;
        }

        /** Executa depois que a animação acabar (fora do tratamento da animação, para poder iniciar outra). */
        Sequencia aoTerminar(Runnable acao) {
            etapas.add(new LinhaDoTempo.Etapa() {
                @Override
                public long duracaoMs() {
                    return 0;
                }

                @Override
                public void aoFinalizar() {
                    SwingUtilities.invokeLater(acao);
                }
            });
            return this;
        }

        void executar() {
            linha.pular();      // termina a anterior primeiro: as marcas dela não podem sobrar
            marcas.clear();
            linha.executar(etapas, animar);
        }
    }

    Sequencia sequencia() {
        return new Sequencia(1.0);
    }

    /** Sequência mais rápida (ou mais lenta, se {@code aceleracao < 1}) que o normal. */
    Sequencia sequencia(double aceleracao) {
        return new Sequencia(aceleracao);
    }

    /** Atalho: mostra o desenho (animado se as animações estiverem ligadas), apagando as marcas. */
    void mostrar(Desenho d) {
        sequencia().mostrar(d).executar();
    }

    /** Troca o desenho na hora, sem animar. */
    void mostrarSemAnimar(Desenho d) {
        boolean antes = animar;
        animar = false;
        mostrar(d);
        animar = antes;
    }

    private final class Transicao implements LinhaDoTempo.Etapa {
        private final Desenho alvo;
        private final double aceleracao;
        private boolean imediata;

        Transicao(Desenho alvo, double aceleracao) {
            this.alvo = alvo;
            this.aceleracao = aceleracao;
        }

        @Override
        public void aoIniciar() {
            imediata = mesmoLayout(atual, alvo);
            origem = atual;
            destino = alvo;
            progresso = 0;
            recalcularGeometria(origem, destino);
            if (ajusteAutomatico && getWidth() > 0) {
                escalaIni = escala;
                txIni = tx;
                tyIni = ty;
                double[] fim = calcularAjuste(alvo);
                escalaFim = fim[0];
                txFim = fim[1];
                tyFim = fim[2];
            }
        }

        @Override
        public long duracaoMs() {
            return imediata ? 0 : (long) (DURACAO_TRANSICAO_MS / (velocidade * aceleracao));
        }

        @Override
        public void aoQuadro(double t) {
            progresso = suavizar(t);
            if (ajusteAutomatico && getWidth() > 0) {
                escala = escalaIni + (escalaFim - escalaIni) * progresso;
                tx = txIni + (txFim - txIni) * progresso;
                ty = tyIni + (tyFim - tyIni) * progresso;
            }
        }

        @Override
        public void aoFinalizar() {
            atual = alvo;
            origem = null;
            destino = null;
            progresso = 1;
            recalcularGeometria(atual);
            if (ajusteAutomatico) {
                aplicarAjuste(atual);
            }
        }
    }

    private final class Visita implements LinhaDoTempo.Etapa {
        private final List<Integer> chaves;
        private final double aceleracao;
        private final List<String> ids = new ArrayList<>();
        private int ultimo = -1;

        Visita(List<Integer> chaves, double aceleracao) {
            this.chaves = chaves;
            this.aceleracao = aceleracao;
        }

        /** Só aqui se sabe quais nós o desenho mostra: a transição anterior da sequência já terminou. */
        @Override
        public void aoIniciar() {
            ids.clear();
            ultimo = -1;
            boolean elisaoJaIncluida = false;
            for (int chave : chaves) {
                String id = NoDesenho.idDoNo(chave);
                if (atual.porId.containsKey(id)) {
                    ids.add(id);
                } else if (!elisaoJaIncluida) {
                    // nó omitido no miolo de um caminho longo: aparece como o "⋮" da elisão, uma vez só
                    for (NoDesenho n : atual.nos) {
                        if (n.tipo == NoDesenho.Tipo.ELISAO) {
                            ids.add(n.id);
                            elisaoJaIncluida = true;
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public long duracaoMs() {
            if (ids.isEmpty()) {
                return 0;
            }
            long total = Math.max(Math.min(ids.size() * 280L, 2600L), ids.size() * 45L);
            return (long) (total / (velocidade * aceleracao));
        }

        @Override
        public void aoQuadro(double t) {
            aplicar(Math.min(ids.size() - 1, (int) (t * ids.size())));
        }

        @Override
        public void aoFinalizar() {
            aplicar(ids.size() - 1);
        }

        /** Os nós antes de {@code indice} ficam "visitados"; o nó {@code indice} é o que está sendo visitado. */
        private void aplicar(int indice) {
            if (indice <= ultimo) {
                return;
            }
            long agora = System.nanoTime();
            for (int i = Math.max(ultimo, 0); i < indice; i++) {
                marcas.put(ids.get(i), new Marca(TipoMarca.VISITADO, 0, agora));
            }
            marcas.put(ids.get(indice), new Marca(TipoMarca.VISITANDO, 0, agora));
            ultimo = indice;
        }
    }

    private final class Marcar implements LinhaDoTempo.Etapa {
        private final String id;
        private final TipoMarca marca;

        Marcar(String id, TipoMarca marca) {
            this.id = id;
            this.marca = marca;
        }

        @Override
        public long duracaoMs() {
            return 0;
        }

        @Override
        public void aoFinalizar() {
            marcas.put(id, new Marca(marca, 0, System.nanoTime()));
            linha.manterQuadros(700);
        }
    }

    private final class Percurso implements LinhaDoTempo.Etapa {
        private final List<String> ids;
        private final double aceleracao;
        private int ultimo = -1;

        Percurso(List<String> ids, double aceleracao) {
            this.ids = ids;
            this.aceleracao = aceleracao;
        }

        @Override
        public long duracaoMs() {
            if (ids.isEmpty()) {
                return 0;
            }
            long total = Math.max(Math.min(ids.size() * 240L, 4500L), ids.size() * 40L);
            return (long) (total / (velocidade * aceleracao));
        }

        @Override
        public void aoQuadro(double t) {
            aplicar(Math.min(ids.size() - 1, (int) (t * ids.size())));
        }

        @Override
        public void aoFinalizar() {
            aplicar(ids.size() - 1);
        }

        private void aplicar(int indice) {
            long agora = System.nanoTime();
            for (int i = ultimo + 1; i <= indice; i++) {
                marcas.put(ids.get(i), new Marca(TipoMarca.PERCURSO, i + 1, agora));
            }
            ultimo = Math.max(ultimo, indice);
        }
    }

    /** Largura de traço que nunca fica mais fina que ~1 pixel na tela, por menor que seja o zoom. */
    private float traco(double largura) {
        return (float) Math.max(largura, 1.1 / escala);
    }

    private static double suavizar(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /** Dois desenhos "iguais" não precisam de animação (mesmos elementos nos mesmos lugares). */
    private static boolean mesmoLayout(Desenho a, Desenho b) {
        if (a.nos.size() != b.nos.size()) {
            return false;
        }
        for (NoDesenho n : b.nos) {
            NoDesenho m = a.porId.get(n.id);
            if (m == null || m.tipo != n.tipo || m.x != n.x || m.y != n.y || m.cor != n.cor
                    || m.fator != n.fator || m.ocultos != n.ocultos) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ câmera

    private void congelarCamera() {
        ajusteAutomatico = false;
    }

    private void recalcularGeometria(Desenho... desenhos) {
        FontMetrics fm = getFontMetrics(FONTE_NO);
        int maior = 0;
        for (Desenho d : desenhos) {
            if (d == null) {
                continue;
            }
            for (NoDesenho n : d.nos) {
                if (n.tipo == NoDesenho.Tipo.NO || n.tipo == NoDesenho.Tipo.FANTASMA) {
                    maior = Math.max(maior, fm.stringWidth(Integer.toString(n.chave)));
                }
            }
        }
        raio = Math.max(RAIO_MINIMO, maior / 2.0 + 7);
        espacoX = 2 * raio + 14;
        espacoY = 2 * raio + 30;
    }

    /** Escala e deslocamento que enquadram o desenho inteiro na janela. */
    private double[] calcularAjuste(Desenho d) {
        double w = Math.max(1, getWidth());
        double h = Math.max(1, getHeight());
        if (d.vazio()) {
            return new double[] {1, w / 2, h / 2};
        }
        double xMin = d.xMin * espacoX - raio - MARGEM;
        double xMax = d.xMax * espacoX + raio + MARGEM;
        double yMin = -raio - MARGEM;
        double yMax = d.yMax * espacoY + raio + MARGEM + 14;
        double larguraLogica = xMax - xMin;
        double alturaLogica = yMax - yMin;
        double s = Math.min(w / larguraLogica, h / alturaLogica);
        s = Math.max(ESCALA_MINIMA, Math.min(ESCALA_AJUSTE_MAXIMA, s));
        return new double[] {s, (w - larguraLogica * s) / 2 - xMin * s, (h - alturaLogica * s) / 2 - yMin * s};
    }

    private void aplicarAjuste(Desenho d) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        recalcularGeometria(d);
        double[] a = calcularAjuste(d);
        escala = a[0];
        tx = a[1];
        ty = a[2];
    }

    // ------------------------------------------------------------------ elementos do quadro

    /** Monta o que aparece neste quadro (posições interpoladas durante uma transição). */
    private Map<String, Elemento> elementosDoQuadro() {
        Map<String, Elemento> mapa = new LinkedHashMap<>();
        if (destino == null) {
            for (NoDesenho n : atual.nos) {
                Elemento e = new Elemento(n);
                e.x = n.x * espacoX;
                e.y = n.y * espacoY;
                e.preenchimento = corDoNo(n);
                mapa.put(n.id, e);
            }
            return mapa;
        }
        for (NoDesenho n : origem.nos) {
            if (destino.porId.containsKey(n.id)) {
                continue;
            }
            Elemento e = new Elemento(n);   // sai: fica parado e some
            e.x = n.x * espacoX;
            e.y = n.y * espacoY;
            e.alfa = 1 - progresso;
            e.preenchimento = corDoNo(n);
            mapa.put(n.id, e);
        }
        for (NoDesenho n : destino.nos) {
            Elemento e = new Elemento(n);
            NoDesenho antes = origem.porId.get(n.id);
            if (antes == null) {            // entra: já na posição final, aparecendo
                e.x = n.x * espacoX;
                e.y = n.y * espacoY;
                e.alfa = progresso;
                e.preenchimento = corDoNo(n);
            } else {                        // continua: desliza
                e.x = (antes.x + (n.x - antes.x) * progresso) * espacoX;
                e.y = (antes.y + (n.y - antes.y) * progresso) * espacoY;
                e.preenchimento = Tema.misturar(corDoNo(antes), corDoNo(n), progresso);
            }
            mapa.put(n.id, e);
        }
        return mapa;
    }

    private List<Aresta> arestasDoQuadro(Map<String, Elemento> elementos) {
        Map<String, Aresta> arestas = new LinkedHashMap<>();
        if (destino == null) {
            coletarArestas(atual, elementos, arestas, 1, null);
        } else {
            coletarArestas(origem, elementos, arestas, 1 - progresso, destino);
            coletarArestas(destino, elementos, arestas, progresso, origem);
        }
        return new ArrayList<>(arestas.values());
    }

    /**
     * Junta as arestas de um desenho. Se a mesma aresta existe também no outro desenho
     * ({@code outro}), ela fica sempre inteira; senão aparece ou some conforme {@code alfa}.
     */
    private void coletarArestas(Desenho d, Map<String, Elemento> elementos, Map<String, Aresta> destinoMapa,
                                double alfa, Desenho outro) {
        for (NoDesenho pai : d.nos) {
            for (NoDesenho filho : new NoDesenho[] {pai.esq, pai.dir}) {
                if (filho == null) {
                    continue;
                }
                Elemento ep = elementos.get(pai.id);
                Elemento ef = elementos.get(filho.id);
                if (ep == null || ef == null) {
                    continue;
                }
                boolean nosDois = outro != null && existeAresta(outro, pai.id, filho.id);
                double a = (nosDois ? 1 : alfa) * Math.min(ep.alfa, ef.alfa);
                destinoMapa.put(pai.id + ">" + filho.id,
                        new Aresta(pai.id, filho.id, ep.x, ep.y, ef.x, ef.y, a, filho.tipo));
            }
        }
    }

    private static boolean existeAresta(Desenho d, String idPai, String idFilho) {
        NoDesenho p = d.porId.get(idPai);
        return p != null && ((p.esq != null && p.esq.id.equals(idFilho)) || (p.dir != null && p.dir.id.equals(idFilho)));
    }

    private Color corDoNo(NoDesenho n) {
        if (n.cor == Cor.VERMELHO) {
            return Tema.NO_VERMELHO;
        }
        if (n.cor == Cor.PRETO) {
            return Tema.NO_PRETO;
        }
        return Tema.clarear(Tema.corDaArvore(tipo), 0.82);
    }

    // ------------------------------------------------------------------ pintura

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            desenharGrade(g);

            boolean vazio = destino == null ? atual.vazio() : (origem.vazio() && destino.vazio());
            if (vazio) {
                desenharVazio(g);
            } else {
                g.translate(tx, ty);
                g.scale(escala, escala);
                Map<String, Elemento> elementos = elementosDoQuadro();
                List<Aresta> arestas = arestasDoQuadro(elementos);
                desenharArestas(g, arestas);
                for (Elemento e : elementos.values()) {
                    desenharElemento(g, e);
                }
                for (Elemento e : elementos.values()) {
                    desenharMarca(g, e);
                }
            }
            g.setTransform(new AffineTransform());
            if (mensagem != null) {
                desenharMensagem(g);
            }
        } finally {
            g.dispose();
        }
    }

    /** Pontinhos de fundo, como num caderno: acompanham o arrasto e o zoom do desenho. */
    private void desenharGrade(Graphics2D g) {
        double passo = Math.max(14, Math.min(56, 28 * escala));
        double x0 = ((tx % passo) + passo) % passo;
        double y0 = ((ty % passo) + passo) % passo;
        int colunas = (int) Math.ceil((getWidth() - x0) / passo);
        int linhas = (int) Math.ceil((getHeight() - y0) / passo);
        if ((long) colunas * linhas > 12_000) {
            return;
        }
        g.setColor(Tema.PONTOS);
        for (int i = 0; i < colunas; i++) {
            for (int j = 0; j < linhas; j++) {
                g.fill(new Ellipse2D.Double(x0 + i * passo - 1.1, y0 + j * passo - 1.1, 2.2, 2.2));
            }
        }
    }

    private void desenharVazio(Graphics2D g) {
        g.setFont(Tema.fonte(14f, Font.BOLD));
        centralizar(g, "Árvore vazia", getWidth() / 2.0, getHeight() / 2.0 - 8, Tema.TEXTO_2);
        g.setFont(Tema.fonte(11.5f, Font.PLAIN));
        centralizar(g, "Insira uma chave ou carregue o dataset", getWidth() / 2.0, getHeight() / 2.0 + 14, Tema.TEXTO_3);
    }

    private void desenharMensagem(Graphics2D g) {
        g.setColor(Tema.comAlfa(Tema.FUNDO_DESENHO, 0.82));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(Tema.fonte(14f, Font.BOLD));
        centralizar(g, mensagem, getWidth() / 2.0, getHeight() / 2.0, Tema.TEXTO_2);
    }

    private void centralizar(Graphics2D g, String texto, double cx, double cy, Color cor) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(cor);
        g.drawString(texto, (float) (cx - fm.stringWidth(texto) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private boolean emDestaque(String id) {
        Marca m = marcas.get(id);
        return m != null && (m.tipo == TipoMarca.VISITANDO || m.tipo == TipoMarca.VISITADO
                || m.tipo == TipoMarca.ENCONTRADO || m.tipo == TipoMarca.NAO_ENCONTRADO
                || m.tipo == TipoMarca.INSERIDO || m.tipo == TipoMarca.REMOVENDO);
    }

    private void desenharArestas(Graphics2D g, List<Aresta> arestas) {
        for (Aresta a : arestas) {
            if (a.alfa() <= 0.01) {
                continue;
            }
            boolean caminho = emDestaque(a.filho()) && emDestaque(a.pai());
            Marca mPai = marcas.get(a.pai());
            caminho = caminho && mPai != null && (mPai.tipo == TipoMarca.VISITADO || mPai.tipo == TipoMarca.VISITANDO);
            Color cor = caminho ? Tema.VISITA : Tema.ARESTA;
            float largura = traco(caminho ? 3.2 : 1.6);
            float[] tracejado = switch (a.tipoFilho()) {
                case MARCADOR -> new float[] {5f, 4f};
                case ELISAO -> new float[] {2f, 4f};
                case FANTASMA -> new float[] {5f, 4f};
                case NO -> null;
            };
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, a.alfa())));
            g.setColor(cor);
            g.setStroke(new BasicStroke(largura, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, tracejado, 0f));
            g.draw(new Line2D.Double(a.x1(), a.y1(), a.x2(), a.y2()));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void desenharElemento(Graphics2D g, Elemento e) {
        if (e.alfa <= 0.01) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, e.alfa)));
        double detalhe = escala * raio;
        switch (e.no.tipo) {
            case NO -> desenharNo(g, e, detalhe);
            case FANTASMA -> desenharFantasma(g, e, detalhe);
            case MARCADOR -> desenharMarcador(g, e, detalhe);
            case ELISAO -> desenharElisao(g, e, detalhe);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void desenharNo(Graphics2D g, Elemento e, double detalhe) {
        Shape circulo = new Ellipse2D.Double(e.x - raio, e.y - raio, 2 * raio, 2 * raio);
        if (detalhe > 4) {
            g.setColor(new Color(0, 0, 0, 34));
            g.fill(new Ellipse2D.Double(e.x - raio + 1, e.y - raio + 2.5, 2 * raio, 2 * raio));
        }
        g.setColor(e.preenchimento);
        g.fill(circulo);
        boolean rubroNegra = e.no.cor != Cor.NENHUMA;
        Color borda = rubroNegra ? Tema.escurecer(e.preenchimento, 0.25) : Tema.corDaArvore(tipo);
        if (e.no.id.equals(idSobMouse)) {
            borda = Tema.SELECAO;
        }
        g.setColor(borda);
        g.setStroke(new BasicStroke(traco(rubroNegra ? 1.4 : 2.2)));
        g.draw(circulo);

        if (detalhe >= 6) {
            g.setFont(FONTE_NO);
            centralizar(g, Integer.toString(e.no.chave), e.x, e.y, rubroNegra ? Color.WHITE : Tema.TEXTO);
        }
        if (mostrarFatores && e.no.temFator && detalhe >= 9) {
            desenharFator(g, e);
        }
    }

    private void desenharFator(Graphics2D g, Elemento e) {
        int f = e.no.fator;
        String texto = f > 0 ? "+" + f : (f < 0 ? "−" + (-f) : "0");
        g.setFont(FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(texto) + 7;
        double h = fm.getAscent() + 2;
        double bx = e.x + raio * 0.55;
        double by = e.y - raio - 5;
        Color fundo = (f == 0) ? new Color(0xEEF0F4) : new Color(0xFFE9B0);
        g.setColor(fundo);
        g.fill(new RoundRectangle2D.Double(bx, by, w, h, 6, 6));
        g.setColor(f == 0 ? Tema.TEXTO_3 : new Color(0x7A4E00));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new RoundRectangle2D.Double(bx, by, w, h, 6, 6));
        g.drawString(texto, (float) (bx + 3.5), (float) (by + h - 3));
    }

    private void desenharFantasma(Graphics2D g, Elemento e, double detalhe) {
        Shape circulo = new Ellipse2D.Double(e.x - raio, e.y - raio, 2 * raio, 2 * raio);
        g.setColor(Tema.FUNDO_DESENHO);
        g.fill(circulo);
        g.setColor(Tema.TEXTO_3);
        g.setStroke(new BasicStroke(traco(1.8), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f));
        g.draw(circulo);
        if (detalhe >= 6) {
            g.setFont(FONTE_NO);
            centralizar(g, Integer.toString(e.no.chave), e.x, e.y, Tema.TEXTO_3);
        }
    }

    private void desenharMarcador(Graphics2D g, Elemento e, double detalhe) {
        Color cor = Tema.corDaArvore(tipo);
        Path2D.Double tri = new Path2D.Double();
        tri.moveTo(e.x, e.y - raio * 0.85);
        tri.lineTo(e.x - raio * 1.05, e.y + raio * 0.85);
        tri.lineTo(e.x + raio * 1.05, e.y + raio * 0.85);
        tri.closePath();
        g.setColor(Tema.clarear(cor, 0.88));
        g.fill(tri);
        g.setColor(e.no.id.equals(idSobMouse) ? Tema.SELECAO : cor);
        g.setStroke(new BasicStroke(traco(1.6)));
        g.draw(tri);
        if (detalhe >= 5) {
            g.setFont(FONTE_ROTULO);
            centralizar(g, Formato.compacto(e.no.ocultos), e.x, e.y + raio * 0.85 + 11, Tema.TEXTO_2);
        }
    }

    private void desenharElisao(Graphics2D g, Elemento e, double detalhe) {
        RoundRectangle2D.Double pilula = new RoundRectangle2D.Double(e.x - raio, e.y - raio * 0.55, 2 * raio, raio * 1.1, raio, raio);
        g.setColor(Tema.FUNDO_PAINEL);
        g.fill(pilula);
        g.setColor(Tema.TEXTO_3);
        g.setStroke(new BasicStroke(traco(1.4)));
        g.draw(pilula);
        g.setColor(Tema.TEXTO_2);
        for (int i = -1; i <= 1; i++) {
            g.fill(new Ellipse2D.Double(e.x + i * raio * 0.5 - 1.7, e.y - 1.7, 3.4, 3.4));
        }
        if (detalhe >= 5) {
            g.setFont(FONTE_ROTULO);
            centralizar(g, Formato.inteiro(e.no.ocultos) + " níveis", e.x, e.y + raio * 0.55 + 11, Tema.TEXTO_2);
        }
    }

    /** Anéis e emblemas dos estados (visitado, achado, inserido...) e da seleção. */
    private void desenharMarca(Graphics2D g, Elemento e) {
        if (e.alfa <= 0.01 || (e.no.tipo != NoDesenho.Tipo.NO && e.no.tipo != NoDesenho.Tipo.FANTASMA
                && e.no.tipo != NoDesenho.Tipo.ELISAO)) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, e.alfa)));
        Marca m = marcas.get(e.no.id);
        if (m != null) {
            double pulso = pulso(m);
            switch (m.tipo) {
                case VISITANDO -> anel(g, e, Tema.VISITA, 4.5 + pulso, 3.6f, true);
                case VISITADO -> anel(g, e, Tema.comAlfa(Tema.VISITA, 0.85), 3.5, 2.4f, false);
                case ENCONTRADO -> {
                    anel(g, e, Tema.ACHADO, 5 + pulso, 3.8f, true);
                    emblema(g, e, Tema.ACHADO, 'v');
                }
                case NAO_ENCONTRADO -> {
                    anel(g, e, Tema.NAO_ACHADO, 5 + pulso, 3.2f, true);
                    emblema(g, e, Tema.NAO_ACHADO, 'x');
                }
                case INSERIDO -> {
                    anel(g, e, Tema.INSERIDO, 5 + pulso, 3.8f, true);
                    emblema(g, e, Tema.INSERIDO, '+');
                }
                case REMOVENDO -> {
                    anel(g, e, Tema.NAO_ACHADO, 5 + pulso, 3.8f, true);
                    emblema(g, e, Tema.NAO_ACHADO, 'x');
                }
                case PERCURSO -> {
                    anel(g, e, Tema.PERCURSO, 4 + pulso, 3f, false);
                    numero(g, e, m.numero);
                }
            }
        }
        if (e.no.id.equals(idSelecionado) && e.no.tipo == NoDesenho.Tipo.NO) {
            g.setColor(Tema.SELECAO);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
            double r = raio + 8.5;
            g.draw(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** Aumenta o anel por um instante quando o estado acaba de acontecer. */
    private static double pulso(Marca m) {
        double dt = (System.nanoTime() - m.inicioNs) / 1e6;
        if (dt < 0 || dt > 500) {
            return 0;
        }
        return 5 * Math.sin(Math.PI * dt / 500);
    }

    private void anel(Graphics2D g, Elemento e, Color cor, double folga, float largura, boolean brilho) {
        double r = raio + folga;
        if (brilho) {
            g.setColor(Tema.comAlfa(cor, 0.22));
            g.setStroke(new BasicStroke(traco(largura + 7f)));
            g.draw(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
        }
        g.setColor(cor);
        g.setStroke(new BasicStroke(traco(largura)));
        g.draw(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
    }

    /** Distintivo no canto de cima à esquerda: v = achado, x = não achado/removendo, + = inserido. */
    private void emblema(Graphics2D g, Elemento e, Color cor, char forma) {
        double d = 15;
        double bx = e.x - raio - 4;
        double by = e.y - raio - 8;
        g.setColor(cor);
        g.fill(new Ellipse2D.Double(bx, by, d, d));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double cx = bx + d / 2;
        double cy = by + d / 2;
        switch (forma) {
            case 'v' -> {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(cx - 3.6, cy + 0.2);
                p.lineTo(cx - 1.1, cy + 2.8);
                p.lineTo(cx + 3.8, cy - 2.8);
                g.draw(p);
            }
            case 'x' -> {
                g.draw(new Line2D.Double(cx - 3, cy - 3, cx + 3, cy + 3));
                g.draw(new Line2D.Double(cx - 3, cy + 3, cx + 3, cy - 3));
            }
            default -> {
                g.draw(new Line2D.Double(cx - 3.4, cy, cx + 3.4, cy));
                g.draw(new Line2D.Double(cx, cy - 3.4, cx, cy + 3.4));
            }
        }
    }

    private void numero(Graphics2D g, Elemento e, int n) {
        String texto = Integer.toString(n);
        g.setFont(FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        double d = Math.max(16, fm.stringWidth(texto) + 8);
        double bx = e.x - raio - 6;
        double by = e.y - raio - 9;
        g.setColor(Tema.PERCURSO);
        g.fill(new RoundRectangle2D.Double(bx, by, d, 16, 16, 16));
        centralizar(g, texto, bx + d / 2, by + 8, Color.WHITE);
    }

    // ------------------------------------------------------------------ mouse: o que está debaixo do cursor

    /** Elemento sob o ponto (em pixels da janela), ou null. Usa as posições do quadro atual. */
    private Elemento elementoEm(int px, int py) {
        if ((destino == null ? atual : destino).vazio()) {
            return null;
        }
        double lx = (px - tx) / escala;
        double ly = (py - ty) / escala;
        Elemento melhor = null;
        double menor = Double.MAX_VALUE;
        for (Elemento e : elementosDoQuadro().values()) {
            if (e.alfa < 0.5 || e.no.tipo == NoDesenho.Tipo.FANTASMA) {
                continue;
            }
            double dx = lx - e.x;
            double dy = ly - e.y;
            double alcance = (e.no.tipo == NoDesenho.Tipo.NO) ? raio + 2 : raio * 1.15;
            double d2 = dx * dx + dy * dy;
            if (d2 <= alcance * alcance && d2 < menor) {
                menor = d2;
                melhor = e;
            }
        }
        return melhor;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Elemento e = elementoEm(event.getX(), event.getY());
        if (e == null) {
            return null;
        }
        NoDesenho n = e.no;
        switch (n.tipo) {
            case MARCADOR:
                return "<html><b>" + Formato.inteiro(n.ocultos) + " nós escondidos</b> nesta subárvore"
                        + "<br>(raiz: chave " + n.chave + ")<br><i>Clique para abrir só esta subárvore</i></html>";
            case ELISAO:
                return "<html>" + Formato.inteiro(n.ocultos) + " níveis do caminho foram omitidos</html>";
            case NO: {
                StringBuilder sb = new StringBuilder("<html><b>Chave ").append(n.chave).append("</b>");
                if (n.dados != null) {
                    Faixa f = n.dados;
                    sb.append("<br>").append(escapar(f.titulo())).append("<br>").append(escapar(f.artista()))
                            .append(" · ").append(escapar(f.genero()))
                            .append("<br>").append(Formato.inteiro(f.ouvintes())).append(" ouvintes · ")
                            .append(Formato.duracao(f.duracaoSeg()));
                }
                if (n.cor != Cor.NENHUMA) {
                    sb.append("<br>cor: ").append(n.cor == Cor.VERMELHO ? "vermelho" : "preto");
                }
                if (n.temFator) {
                    sb.append("<br>fator de balanceamento: ").append(n.fator > 0 ? "+" + n.fator : n.fator);
                }
                return sb.append("</html>").toString();
            }
            default:
                return null;
        }
    }

    private static String escapar(String texto) {
        String t = texto.length() > 60 ? texto.substring(0, 57) + "..." : texto;
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void menuDeContexto(MouseEvent e) {
        Elemento sob = elementoEm(e.getX(), e.getY());
        JPopupMenu menu = new JPopupMenu();
        if (sob != null && sob.no.tipo == NoDesenho.Tipo.NO) {
            int chave = sob.no.chave;
            JMenuItem buscar = new JMenuItem("Buscar a chave " + chave + " nas três árvores");
            buscar.addActionListener(x -> ouvinte.buscaSolicitada(chave));
            JMenuItem remover = new JMenuItem("Remover a chave " + chave + " das três árvores");
            remover.addActionListener(x -> ouvinte.remocaoSolicitada(chave));
            JMenuItem focar = new JMenuItem("Mostrar só esta subárvore");
            focar.addActionListener(x -> ouvinte.focoSolicitado(tipo, chave));
            menu.add(buscar);
            menu.add(remover);
            menu.addSeparator();
            menu.add(focar);
        } else if (sob != null && sob.no.tipo == NoDesenho.Tipo.MARCADOR) {
            int chave = sob.no.chave;
            JMenuItem abrir = new JMenuItem("Abrir esta subárvore (" + Formato.inteiro(sob.no.ocultos) + " nós)");
            abrir.addActionListener(x -> ouvinte.focoSolicitado(tipo, chave));
            menu.add(abrir);
        }
        JMenuItem ajustar = new JMenuItem("Ajustar à janela");
        ajustar.addActionListener(x -> ajustar());
        menu.add(ajustar);
        JMenuItem raizItem = new JMenuItem("Voltar à raiz da árvore");
        raizItem.addActionListener(x -> ouvinte.focoSolicitado(tipo, null));
        menu.add(raizItem);
        menu.show(this, e.getX(), e.getY());
    }
}
