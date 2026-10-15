package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import arvores.Arvore;
import arvores.Benchmark;
import arvores.CarregadorFaixas;
import arvores.Cenario;
import arvores.Cor;
import arvores.Faixa;
import arvores.Operacao;
import arvores.Ordem;
import arvores.ResultadoBenchmark;
import arvores.TipoArvore;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.LookModerno;
import arvores.gui.visual.Tabela;

/**
 * Testes da parte da interface que não é Swing (o retrato das árvores, o modelo e a carga do
 * dataset), sem abrir nenhuma janela:
 * <pre>java -cp out arvores.gui.TesteGui</pre>
 */
public final class TesteGui {

    private static final double FOLGA = 1e-9;

    private static PrintStream saida = System.out;
    private static long verificacoes = 0;

    private TesteGui() {
    }

    private static void confere(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHOU: " + mensagem);
        }
    }

    public static void main(String[] args) {
        executar(System.out);
    }

    /** Roda os testes escrevendo em {@code destino}; devolve o nº de verificações que passaram. */
    public static synchronized long executar(PrintStream destino) {
        saida = destino;
        verificacoes = 0;
        for (TipoArvore tipo : TipoArvore.values()) {
            saida.println("== desenho: " + tipo.nome() + " ==");
            testeArvorePequena(tipo);
            testeInvarianteDeContagem(tipo);
            testeCaminhoEmArvoreBalanceada(tipo);
            testeFantasma(tipo);
            testeFoco(tipo);
            testeCadeia(tipo);
        }
        testeLayoutCompacto();
        testeCaminhoLongoEmCadeia();
        testeVazia();
        saida.println("== modelo, carga e benchmark ==");
        testeModelo();
        testeCarga();
        testeBenchmark();
        saida.println("== visual dos componentes ==");
        testeVisual();
        saida.println("\nOK: " + verificacoes + " verificações da interface passaram.");
        return verificacoes;
    }

    // ------------------------------------------------------------------ utilidades

    private static Arvore<Integer, Faixa> montar(TipoArvore tipo, List<Integer> chaves) {
        Arvore<Integer, Faixa> a = tipo.criar();
        for (int k : chaves) {
            a.inserir(k, faixa(k));
        }
        return a;
    }

    private static Faixa faixa(int k) {
        return new Faixa("Faixa " + k, "Artista", "Rock", k, 100);
    }

    private static List<Integer> embaralhadas(int n, long semente) {
        List<Integer> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            l.add(i);
        }
        Collections.shuffle(l, new Random(semente));
        return l;
    }

    private static Desenho desenhar(Arvore<Integer, Faixa> a, PedidoDesenho pedido) {
        return ConstrutorDesenho.construir(a.raizVisivel(), pedido);
    }

    private static long ocultos(Desenho d) {
        long total = 0;
        for (NoDesenho n : d.nos) {
            if (n.tipo == NoDesenho.Tipo.MARCADOR) {
                total += n.ocultos;
            }
        }
        return total;
    }

    private static int contar(Desenho d, NoDesenho.Tipo tipo) {
        int total = 0;
        for (NoDesenho n : d.nos) {
            if (n.tipo == tipo) {
                total++;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ testes

    /** Árvore que cabe inteira: tudo aparece, na forma certa, e o layout respeita as regras. */
    private static void testeArvorePequena(TipoArvore tipo) {
        for (int n : new int[] {1, 2, 3, 7, 15, 40}) {
            Arvore<Integer, Faixa> a = montar(tipo, embaralhadas(n, 7 + n));
            Desenho d = desenhar(a, PedidoDesenho.padrao());
            String quem = tipo.nome() + " n=" + n;

            confere(d.nosVisiveis() == n && !d.resumido, quem + ": todos os nós aparecem");
            confere(d.porId.size() == d.nos.size(), quem + ": ids únicos");
            confere(d.raiz.chave == a.raizVisivel().chave() && d.raiz.x == 0 && d.raiz.y == 0, quem + ": raiz em 0,0");
            confere(d.yMax + 1 == a.altura(), quem + ": nº de níveis desenhados = altura");

            List<Integer> preDesenho = new ArrayList<>();
            percorrerPre(d.raiz, preDesenho);
            confere(preDesenho.equals(a.percorrer(Ordem.PRE)), quem + ": mesma forma (pré-ordem)");

            for (NoDesenho no : d.nos) {
                confere(no.dados != null && no.dados.equals(faixa(no.chave)), quem + ": dados do nó " + no.chave);
                if (tipo == TipoArvore.RUBRO_NEGRA) {
                    confere(no.cor != Cor.NENHUMA, quem + ": cor presente");
                } else {
                    confere(no.cor == Cor.NENHUMA, quem + ": sem cor");
                }
                confere(no.temFator == (tipo == TipoArvore.AVL), quem + ": fator só na AVL");
                if (no.esq != null && no.dir != null) {
                    confere(Math.abs(no.x - (no.esq.x + no.dir.x) / 2) < FOLGA, quem + ": pai centrado nos filhos");
                    confere(no.esq.x < no.x && no.x < no.dir.x, quem + ": esquerdo à esquerda, direito à direita");
                } else if (no.esq != null) {
                    confere(Math.abs(no.esq.x - (no.x - 0.5)) < FOLGA, quem + ": filho único esquerdo desloca -0,5");
                } else if (no.dir != null) {
                    confere(Math.abs(no.dir.x - (no.x + 0.5)) < FOLGA, quem + ": filho único direito desloca +0,5");
                }
            }
            conferirSeparacaoPorNivel(d, quem);
        }
    }

    private static void percorrerPre(NoDesenho raiz, List<Integer> saidaChaves) {
        Deque<NoDesenho> pilha = new ArrayDeque<>();
        pilha.push(raiz);
        while (!pilha.isEmpty()) {
            NoDesenho n = pilha.pop();
            saidaChaves.add(n.chave);
            if (n.dir != null) {
                pilha.push(n.dir);
            }
            if (n.esq != null) {
                pilha.push(n.esq);
            }
        }
    }

    /** Dois elementos do mesmo nível nunca ficam mais perto que SEPARACAO, e a ordem das chaves é mantida. */
    private static void conferirSeparacaoPorNivel(Desenho d, String quem) {
        List<NoDesenho> ordenados = new ArrayList<>(d.nos);
        ordenados.sort((p, q) -> p.y != q.y ? Double.compare(p.y, q.y) : Double.compare(p.x, q.x));
        for (int i = 1; i < ordenados.size(); i++) {
            NoDesenho antes = ordenados.get(i - 1);
            NoDesenho depois = ordenados.get(i);
            if (antes.y == depois.y) {
                confere(depois.x - antes.x >= LayoutArvore.SEPARACAO - FOLGA, quem + ": nós do mesmo nível separados");
                if (antes.ehNo() && depois.ehNo()) {
                    confere(antes.chave < depois.chave, quem + ": ordem das chaves no nível");
                }
            }
        }
    }

    /** Sem caminho nem foco, cada nó da árvore ou aparece ou é contado em exatamente um marcador. */
    private static void testeInvarianteDeContagem(TipoArvore tipo) {
        Random rnd = new Random(99);
        for (int rodada = 0; rodada < 60; rodada++) {
            int n = 1 + rnd.nextInt(700);
            Arvore<Integer, Faixa> a = montar(tipo, embaralhadas(n, rodada));
            int niveis = 1 + rnd.nextInt(12);
            int largura = 2 + rnd.nextInt(40);
            Desenho d = desenhar(a, new PedidoDesenho(niveis, largura, null, List.of(), null));
            confere(d.nosVisiveis() + ocultos(d) == n,
                    tipo.nome() + ": visíveis + escondidos = tamanho (n=" + n + ", níveis=" + niveis
                            + ", largura=" + largura + ")");
            boolean temMarcador = contar(d, NoDesenho.Tipo.MARCADOR) > 0;
            confere(d.yMax + 1 <= niveis + (temMarcador ? 1 : 0),
                    tipo.nome() + ": respeita o limite de níveis (os marcadores ficam um nível abaixo)");
            confere(d.porId.size() == d.nos.size(), tipo.nome() + ": ids únicos");
            conferirSeparacaoPorNivel(d, tipo.nome() + " rodada " + rodada);
        }
    }

    /** Em 100 mil nós, o caminho até uma chave aparece inteiro no meio do resumo. */
    private static void testeCaminhoEmArvoreBalanceada(TipoArvore tipo) {
        int n = 100_000;
        Arvore<Integer, Faixa> a = montar(tipo, embaralhadas(n, 5));
        int alvo = 77_777;
        Caminho c = Caminho.ate(a.raizVisivel(), alvo);
        confere(c.encontrou() && c.ultima() == alvo, tipo.nome() + ": caminho achou a chave");

        PedidoDesenho base = PedidoDesenho.padrao();
        Desenho sem = desenhar(a, base);
        Desenho com = desenhar(a, base.comCaminho(c.chaves(), null));
        confere(!sem.porId.containsKey(NoDesenho.idDoNo(alvo)) || c.chaves().size() <= 6,
                tipo.nome() + ": sem o caminho, uma chave funda não aparece");
        for (int chave : c.chaves()) {
            confere(com.porId.containsKey(NoDesenho.idDoNo(chave)) && com.porId.get(NoDesenho.idDoNo(chave)).ehNo(),
                    tipo.nome() + ": nó " + chave + " do caminho aparece");
        }
        confere(contar(com, NoDesenho.Tipo.ELISAO) == 0, tipo.nome() + ": caminho curto não tem elisão");
        confere(com.nosVisiveis() + ocultos(com) == n,
                tipo.nome() + ": com caminho, visíveis + escondidos = tamanho");
        confere(com.nos.size() < 400, tipo.nome() + ": o desenho continua pequeno (" + com.nos.size() + ")");
    }

    private static void testeFantasma(TipoArvore tipo) {
        Arvore<Integer, Faixa> a = montar(tipo, List.of(10, 20, 30, 40, 50, 60, 70));
        for (int ausente : new int[] {5, 15, 25, 35, 45, 55, 65, 75}) {
            Caminho c = Caminho.ate(a.raizVisivel(), ausente);
            confere(!c.encontrou(), tipo.nome() + ": " + ausente + " não existe");
            Desenho d = desenhar(a, PedidoDesenho.padrao().comCaminho(c.chaves(), ausente));
            NoDesenho fantasma = d.porId.get(NoDesenho.idDoNo(ausente));
            confere(fantasma != null && fantasma.tipo == NoDesenho.Tipo.FANTASMA && contar(d, NoDesenho.Tipo.FANTASMA) == 1,
                    tipo.nome() + ": um fantasma para " + ausente);
            NoDesenho pai = d.porId.get(NoDesenho.idDoNo(c.ultima()));
            confere(pai != null && (pai.esq == fantasma || pai.dir == fantasma),
                    tipo.nome() + ": fantasma de " + ausente + " pendurado no último nó visitado");
            confere((ausente < pai.chave) == (pai.esq == fantasma), tipo.nome() + ": fantasma do lado certo");
        }
        Caminho existente = Caminho.ate(a.raizVisivel(), 30);
        Desenho d = desenhar(a, PedidoDesenho.padrao().comCaminho(existente.chaves(), null));
        confere(contar(d, NoDesenho.Tipo.FANTASMA) == 0, tipo.nome() + ": chave existente não tem fantasma");
    }

    private static void testeFoco(TipoArvore tipo) {
        int n = 3000;
        Arvore<Integer, Faixa> a = montar(tipo, embaralhadas(n, 11));
        Caminho c = Caminho.ate(a.raizVisivel(), 1234);
        int foco = c.chaves().get(Math.min(3, c.chaves().size() - 1));

        PedidoDesenho pedido = PedidoDesenho.padrao().comFoco(foco).comLimites(8, 16);
        Desenho d = desenhar(a, pedido);
        confere(d.focoAplicado && d.raiz.chave == foco, tipo.nome() + ": desenho começa na chave em foco");
        confere(d.nosNoFoco > 0 && d.nosNoFoco < n, tipo.nome() + ": subárvore em foco é parte da árvore");
        confere(d.nosVisiveis() + ocultos(d) == d.nosNoFoco, tipo.nome() + ": contagem dentro do foco");

        Desenho comCaminho = desenhar(a, pedido.comCaminho(c.chaves(), null));
        confere(comCaminho.porId.containsKey(NoDesenho.idDoNo(1234)), tipo.nome() + ": caminho recortado pelo foco");

        Desenho semFoco = desenhar(a, PedidoDesenho.padrao().comFoco(-12345));
        confere(!semFoco.focoAplicado && semFoco.raiz.chave == a.raizVisivel().chave(),
                tipo.nome() + ": foco inexistente desenha a árvore inteira");
    }

    /** Chaves em ordem crescente: a BST vira lista (100 mil níveis), as outras ficam baixas. */
    private static void testeCadeia(TipoArvore tipo) {
        int n = 20_000;
        List<Integer> ordem = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            ordem.add(i);
        }
        Arvore<Integer, Faixa> a = montar(tipo, ordem);
        Desenho d = desenhar(a, PedidoDesenho.padrao());
        confere(d.nosVisiveis() + ocultos(d) == n, tipo.nome() + ": cadeia de " + n + " nós contada por inteiro");
        if (tipo == TipoArvore.BST) {
            confere(d.nosVisiveis() == PedidoDesenho.NIVEIS_PADRAO && contar(d, NoDesenho.Tipo.MARCADOR) == 1,
                    "BST em cadeia: só os primeiros níveis e um marcador");
            confere(ocultos(d) == n - PedidoDesenho.NIVEIS_PADRAO, "BST em cadeia: marcador conta o resto");
        }
    }

    private static void testeCaminhoLongoEmCadeia() {
        int n = 20_000;
        List<Integer> ordem = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            ordem.add(i);
        }
        Arvore<Integer, Faixa> a = montar(TipoArvore.BST, ordem);
        int alvo = 15_000;
        Caminho c = Caminho.ate(a.raizVisivel(), alvo);
        confere(c.chaves().size() == alvo, "BST em cadeia: caminho tem " + alvo + " nós");

        PedidoDesenho pedido = PedidoDesenho.padrao().comCaminho(c.chaves(), null);
        Desenho d = desenhar(a, pedido);
        int cabeca = PedidoDesenho.NIVEIS_PADRAO / 2;
        int cauda = PedidoDesenho.NIVEIS_PADRAO - cabeca;
        NoDesenho elisao = null;
        for (NoDesenho no : d.nos) {
            if (no.tipo == NoDesenho.Tipo.ELISAO) {
                elisao = no;
            }
        }
        confere(elisao != null && contar(d, NoDesenho.Tipo.ELISAO) == 1, "caminho longo tem uma elisão");
        confere(elisao.ocultos == alvo - cabeca - cauda, "elisão conta os níveis omitidos: " + elisao.ocultos);
        confere(d.porId.containsKey(NoDesenho.idDoNo(1)) && d.porId.containsKey(NoDesenho.idDoNo(cabeca)),
                "começo do caminho aparece");
        confere(!d.porId.containsKey(NoDesenho.idDoNo(cabeca + 1)), "miolo do caminho não aparece");
        confere(d.porId.containsKey(NoDesenho.idDoNo(alvo)) && d.porId.containsKey(NoDesenho.idDoNo(alvo - cauda + 1)),
                "fim do caminho aparece, com a chave buscada");
        confere(d.nos.size() <= cabeca + cauda + 4, "desenho do caminho longo continua pequeno (" + d.nos.size() + ")");
        confere(elisao.dir != null && elisao.x == elisao.dir.x, "a elisão fica na vertical");

        Set<String> ids = new HashSet<>();
        for (NoDesenho no : d.nos) {
            confere(ids.add(no.id), "ids únicos no caminho longo");
        }
    }

    private static void testeLayoutCompacto() {
        List<Integer> um31 = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            um31.add(i);
        }
        Desenho perfeita = desenhar(montar(TipoArvore.AVL, um31), PedidoDesenho.padrao());
        confere(perfeita.nosVisiveis() == 31 && perfeita.yMax == 4, "AVL de 1..31 é perfeita (5 níveis)");
        confere(Math.abs(perfeita.largura() - 15) < FOLGA, "árvore perfeita de 31 nós ocupa 15 unidades: " + perfeita.largura());

        List<Integer> dez = um31.subList(0, 10);
        Desenho escada = desenhar(montar(TipoArvore.BST, dez), PedidoDesenho.padrao());
        confere(Math.abs(escada.largura() - 4.5) < FOLGA && escada.yMax == 9, "BST 1..10 vira uma escada de meia unidade");

        Desenho a = desenhar(montar(TipoArvore.RUBRO_NEGRA, embaralhadas(60, 3)), PedidoDesenho.padrao());
        Desenho b = desenhar(montar(TipoArvore.RUBRO_NEGRA, embaralhadas(60, 3)), PedidoDesenho.padrao());
        confere(a.nos.size() == b.nos.size(), "layout determinístico (tamanho)");
        for (int i = 0; i < a.nos.size(); i++) {
            confere(a.nos.get(i).id.equals(b.nos.get(i).id) && a.nos.get(i).x == b.nos.get(i).x
                    && a.nos.get(i).y == b.nos.get(i).y, "layout determinístico (posição)");
        }
    }

    private static void testeVazia() {
        Desenho d = ConstrutorDesenho.construir(null, PedidoDesenho.padrao());
        confere(d.vazio() && d.nos.isEmpty() && d.largura() == 0, "árvore vazia → desenho vazio");
    }

    // ------------------------------------------------------------------ modelo e carga

    /** As três árvores sempre concordam, e os números que o modelo devolve batem com os da árvore. */
    private static void testeModelo() {
        ModeloArvores m = new ModeloArvores();
        TreeMap<Integer, Faixa> gabarito = new TreeMap<>();
        Random rnd = new Random(2026);
        for (int i = 0; i < 4000; i++) {
            int chave = rnd.nextInt(300);
            int op = rnd.nextInt(10);
            ModeloArvores.ResultadoOperacao r;
            if (op < 5) {
                Faixa nova = new Faixa("t" + i, "a", "g", i, 1);
                boolean novo = !gabarito.containsKey(chave);
                gabarito.put(chave, nova);
                r = m.inserir(chave, nova);
                for (ModeloArvores.ResultadoArvore a : r.arvores()) {
                    confere(a.sucesso() == novo, a.tipo() + ": inserir devolve se a chave era nova");
                    confere(a.caminhoDepois().encontrou() && a.caminhoDepois().ultima() == chave,
                            a.tipo() + ": depois de inserir, o caminho termina na chave");
                }
            } else if (op < 8) {
                boolean existia = gabarito.remove(chave) != null;
                r = m.remover(chave);
                for (ModeloArvores.ResultadoArvore a : r.arvores()) {
                    confere(a.sucesso() == existia, a.tipo() + ": remover devolve se a chave existia");
                }
            } else {
                Faixa esperada = gabarito.get(chave);
                r = m.buscar(chave);
                for (ModeloArvores.ResultadoArvore a : r.arvores()) {
                    confere(a.sucesso() == (esperada != null) && Objects.equals(a.dados(), esperada),
                            a.tipo() + ": buscar devolve os dados certos");
                }
                confere(Objects.equals(r.dados(), esperada), "dados() do resultado da busca");
            }
            for (ModeloArvores.ResultadoArvore a : r.arvores()) {
                confere(a.tamanho() == gabarito.size(), a.tipo() + ": tamanho depois da operação");
                // o caminho calculado "por fora" tem que ter o tamanho exato das comparações da própria árvore
                confere(a.comparacoes() == a.caminhoAntes().chaves().size(),
                        a.tipo() + ": " + r.operacao() + " " + chave + ": " + a.comparacoes()
                                + " comparações x caminho de " + a.caminhoAntes().chaves().size());
                confere(a.altura() == m.arvore(a.tipo()).altura(), a.tipo() + ": altura devolvida");
            }
            if (i % 100 == 0) {
                confere(!m.validar().containsValue(false), "as três árvores continuam válidas");
                for (var e : m.percorrer(Ordem.EM).entrySet()) {
                    confere(e.getValue().equals(new ArrayList<>(gabarito.keySet())), e.getKey() + ": em-ordem do modelo");
                }
            }
        }
        m.esvaziar();
        confere(m.tamanho() == 0 && m.dataset().isEmpty(), "esvaziar");

        // a inserção em massa (sem caminhos nem alturas) tem que dar o mesmo resultado que a detalhada
        ModeloArvores detalhado = new ModeloArvores();
        ModeloArvores direto = new ModeloArvores();
        long[] totaisDetalhados = new long[3];
        long[] totaisDiretos = new long[3];
        Random sorteio = new Random(7);
        for (int i = 0; i < 2000; i++) {
            int chave = sorteio.nextInt(700) - 100;
            for (ModeloArvores.ResultadoArvore ra : detalhado.inserir(chave, faixa(chave)).arvores()) {
                totaisDetalhados[ra.tipo().ordinal()] += ra.comparacoes();
            }
            direto.inserirSemDetalhes(chave, faixa(chave), totaisDiretos);
        }
        confere(Arrays.equals(totaisDetalhados, totaisDiretos), "inserção em massa soma as mesmas comparações");
        for (TipoArvore t : TipoArvore.values()) {
            confere(direto.arvore(t).percorrer(Ordem.PRE).equals(detalhado.arvore(t).percorrer(Ordem.PRE)),
                    t.nome() + ": inserção em massa dá a mesma forma");
        }
        confere(!direto.validar().containsValue(false), "árvores válidas depois da inserção em massa");
    }

    private static void testeCarga() {
        List<CarregadorFaixas.Registro> registros = new ArrayList<>();
        for (int k : embaralhadas(5000, 8)) {
            registros.add(new CarregadorFaixas.Registro(k, faixa(k)));
        }
        double[] maior = {0};
        CargaDataset.Resultado r;
        try {
            r = CargaDataset.executar(registros, new CargaDataset.Ouvinte() {
                @Override
                public void andamento(TipoArvore tipo, double fracao) {
                    synchronized (maior) {
                        maior[0] = Math.max(maior[0], fracao);
                    }
                }

                @Override
                public boolean cancelado() {
                    return false;
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError("carga falhou", e);
        }
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = r.arvores().get(t);
            confere(a.tamanho() == 5000 && a.validar(), t + ": árvore carregada completa e válida");
            confere(r.comparacoes().get(t) > 0 && r.nanos().get(t) > 0, t + ": carga mediu comparações e tempo");
            confere(a.contadorComparacoes() == 0, t + ": contador zerado depois da carga");
        }
        confere(maior[0] == 1.0, "andamento chegou a 100%");

        try {
            CargaDataset.executar(registros, new CargaDataset.Ouvinte() {
                @Override
                public void andamento(TipoArvore tipo, double fracao) {
                }

                @Override
                public boolean cancelado() {
                    return true;
                }
            });
            confere(false, "carga cancelada deveria lançar CancellationException");
        } catch (CancellationException esperado) {
            // era isso mesmo que devia acontecer
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError("cancelamento deveria ser CancellationException", e);
        }
    }

    /** O motor do benchmark: andamento, resultado completo, CSV de ida e volta e cancelamento. */
    private static void testeBenchmark() {
        Path csv = null;
        Path pasta = null;
        try {
            csv = Files.createTempFile("faixas-teste", ".csv");
            StringBuilder dados = new StringBuilder("chave,titulo,artista,genero,ouvintes,duracao\n");
            for (int k = 1; k <= 3000; k++) {
                dados.append(k * 3).append(",Faixa ").append(k).append(",Artista,Rock,").append(k).append(",120\n");
            }
            Files.writeString(csv, dados.toString());
            Benchmark.Configuracao cfg = new Benchmark.Configuracao(csv, 2000, 2, 500, 2000);

            double[] andamento = {0, 0};   // último valor visto, maior valor visto
            boolean[] regrediu = {false};
            ResultadoBenchmark r = Benchmark.executar(cfg, new Benchmark.Ouvinte() {
                @Override
                public void mensagem(String texto) {
                }

                @Override
                public void progresso(double fracao) {
                    regrediu[0] |= fracao + 1e-9 < andamento[0] || fracao < 0 || fracao > 1;
                    andamento[0] = fracao;
                    andamento[1] = Math.max(andamento[1], fracao);
                }
            });
            confere(!regrediu[0] && andamento[1] > 0.99, "o andamento só cresce e chega ao fim");
            confere(r.desempenho().size() == Cenario.values().length * TipoArvore.values().length * Operacao.values().length,
                    "uma linha de desempenho por cenário, árvore e operação");
            confere(r.nDesempenho() == 2000, "N do desempenho");
            confere(r.alturaFinal(Cenario.ORDENADO, TipoArvore.BST).getAsInt() == 2000, "a BST em ordem crescente tem altura n");
            confere(r.alturaFinal(Cenario.ORDENADO, TipoArvore.AVL).getAsInt() <= 15, "a AVL continua baixa");
            confere(r.serieAltura(Cenario.EMBARALHADO, TipoArvore.RUBRO_NEGRA).size() == 4, "um ponto de altura a cada 500 inserções");
            confere(r.linha(Cenario.ORDENADO, TipoArvore.BST, Operacao.INSERCAO).get().comparacoes() == 2000L * 1999 / 2,
                    "a BST ordenada faz n(n-1)/2 comparações ao inserir");

            pasta = Files.createTempDirectory("resultados-teste");
            r.gravar(pasta);
            ResultadoBenchmark lido = ResultadoBenchmark.ler(pasta);
            confere(lido.csvDesempenho().equals(r.csvDesempenho()) && lido.csvAltura().equals(r.csvAltura()),
                    "ler o que foi gravado devolve os mesmos CSVs");
            confere(lido.ambiente().equals(r.ambiente()), "o ambiente também vai e volta");

            int[] chamadas = {0};
            try {
                Benchmark.executar(cfg, new Benchmark.Ouvinte() {
                    @Override
                    public void mensagem(String texto) {
                    }

                    @Override
                    public boolean cancelado() {
                        return ++chamadas[0] > 3;
                    }
                });
                confere(false, "benchmark cancelado deveria lançar CancellationException");
            } catch (CancellationException esperado) {
                // era isso mesmo que devia acontecer
            }
        } catch (IOException e) {
            throw new AssertionError("benchmark falhou", e);
        } finally {
            apagar(csv);
            if (pasta != null) {
                for (String nome : new String[] {ResultadoBenchmark.ARQUIVO_DESEMPENHO,
                    ResultadoBenchmark.ARQUIVO_ALTURA, ResultadoBenchmark.ARQUIVO_AMBIENTE}) {
                    apagar(pasta.resolve(nome));
                }
                apagar(pasta);
            }
        }
    }

    private static void apagar(Path caminho) {
        try {
            if (caminho != null) {
                Files.deleteIfExists(caminho);
            }
        } catch (IOException e) {
            // arquivo temporário: se não der para apagar, o sistema limpa depois
        }
    }

    // ------------------------------------------------------------------ visual dos componentes

    /** Pinta os componentes do visual novo fora da tela (na thread da interface) e confere alguns pixels. */
    private static void testeVisual() {
        try {
            SwingUtilities.invokeAndWait(TesteGui::conferirVisual);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof AssertionError falha) {
                throw falha;
            }
            throw new AssertionError("o visual falhou: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("teste do visual interrompido", e);
        }
    }

    private static void conferirVisual() {
        if (!(UIManager.getLookAndFeel() instanceof LookModerno)) {
            LookModerno.instalar();
        }

        JButton primario = Estilo.primario(new JButton(" "));
        BufferedImage img = pintar(primario, 100, 32);
        confere(brilho(img.getRGB(50, 5)) < 90, "botão primário é escuro");
        confere(brilho(pintar(new JButton(" "), 100, 32).getRGB(50, 4)) > 240, "botão padrão é claro");
        JButton perigo = Estilo.perigo(new JButton(" "));
        perigo.setEnabled(false);
        confere(brilho(pintar(perigo, 100, 32).getRGB(50, 4)) > 230, "botão desligado fica claro");

        JTextField erro = new JTextField();
        Estilo.erro(erro, true);
        Color c = new Color(pintar(erro, 120, 34).getRGB(20, 17));
        confere(c.getRed() > c.getGreen() + 8, "campo com erro fica avermelhado");
        Estilo.erro(erro, false);
        Color normal = new Color(pintar(erro, 120, 34).getRGB(20, 17));
        confere(Math.abs(normal.getRed() - normal.getGreen()) < 4, "campo sem erro volta ao branco");

        confere(brilho(pintar(new JCheckBox("", true), 20, 20).getRGB(2, 8)) < 90, "caixa marcada é escura");
        confere(brilho(pintar(new JCheckBox("", false), 20, 20).getRGB(8, 8)) > 230, "caixa desmarcada é clara");

        JProgressBar barra = new JProgressBar(0, 100);
        barra.setValue(50);
        BufferedImage progresso = pintar(barra, 200, 8);
        confere(brilho(progresso.getRGB(20, 4)) < 90, "progresso: parte cheia é escura");
        confere(brilho(progresso.getRGB(180, 4)) > 200, "progresso: parte vazia é clara");

        Cartao cartao = new Cartao(new BorderLayout());
        BufferedImage cartaoImg = pintar(cartao, 100, 60);
        confere(cartaoImg.getRGB(0, 0) == MARCA, "cartão não pinta fora dos cantos arredondados");
        confere(brilho(cartaoImg.getRGB(50, 30)) > 250, "cartão é branco por dentro");

        Tabela vazia = new Tabela(new DefaultTableModel(new Object[] {"a"}, 0));
        vazia.definirMensagemVazia("nada ainda");
        BufferedImage tabelaImg = pintar(vazia, 240, 120);
        boolean achouTexto = false;
        for (int x = 40; x < 200 && !achouTexto; x++) {
            for (int y = 40; y < 80; y++) {
                if (brilho(tabelaImg.getRGB(x, y)) < 200) {
                    achouTexto = true;
                    break;
                }
            }
        }
        confere(achouTexto, "tabela vazia mostra a mensagem");

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Um", new JPanel());
        abas.addTab("Dois", new JPanel());
        pintar(abas, 300, 120);
        JTabbedPane abasCompactas = Estilo.compactas(new JTabbedPane());
        abasCompactas.addTab("Um", new JPanel());
        pintar(abasCompactas, 300, 120);

        JSpinner girador = new JSpinner();
        pintar(girador, 100, 34);
        pintar(new JComboBox<>(new String[] {"a", "b"}), 140, 34);
        pintar(new JScrollBar(), 12, 120);
        pintar(new JSlider(1, 8, 4), 160, 24);
        pintar(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), new JPanel()), 200, 160);
        JMenuBar barraMenu = new JMenuBar();
        JMenu menu = new JMenu("Arquivo");
        menu.add(new JMenuItem("Abrir"));
        barraMenu.add(menu);
        pintar(barraMenu, 200, 30);
    }

    /** Cor de fundo usada para ver o que o componente não pintou. */
    private static final int MARCA = 0xFFFF00FF;

    private static BufferedImage pintar(JComponent componente, int largura, int altura) {
        componente.setSize(largura, altura);
        organizar(componente);
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(MARCA));
        g.fillRect(0, 0, largura, altura);
        componente.paint(g);
        g.dispose();
        confere(true, componente.getClass().getSimpleName() + " pinta sem erro");
        return img;
    }

    private static void organizar(Component c) {
        if (c instanceof Container pai) {
            pai.doLayout();
            for (Component filho : pai.getComponents()) {
                organizar(filho);
            }
        }
    }

    private static int brilho(int argb) {
        Color c = new Color(argb);
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
    }
}
