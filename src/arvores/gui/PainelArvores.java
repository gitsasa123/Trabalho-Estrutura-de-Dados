package arvores.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import arvores.Arvore;
import arvores.CarregadorFaixas;
import arvores.Faixa;
import arvores.Ordem;
import arvores.TipoArvore;
import arvores.gui.ModeloArvores.ResultadoArvore;
import arvores.gui.ModeloArvores.ResultadoOperacao;
import arvores.gui.VisaoArvore.TipoMarca;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;

/**
 * A aba "Árvores": os controles, os três desenhos e as tabelas. Faz o que o menu do programa antigo
 * fazia (buscar, inserir, remover, alturas, percursos, validar), mas mostrando as três árvores.
 *
 * <p>Cada operação é aplicada às três árvores de verdade; depois o painel monta, para cada uma, a
 * sequência "caminho da busca → mudança da forma" que a {@link VisaoArvore} anima.
 */
@SuppressWarnings("serial")
final class PainelArvores extends JPanel implements PainelControles.Acoes, VisaoArvore.Ouvinte {

    private static final long SEMENTE = 2026L;
    /** Até este nº de chaves, "inserir sequência" mostra uma de cada vez; acima disso, só o resultado. */
    private static final int LIMITE_ANIMADO = 60;
    private static final double ACELERACAO_SEQUENCIA = 2.2;

    /** O que cada árvore mostrava antes de uma operação: o caminho até a chave e o desenho com ele. */
    private record Previa(Caminho caminho, Desenho antes) {
    }

    private record Carregado(List<CarregadorFaixas.Registro> registros, CargaDataset.Resultado resultado) {
    }

    private final Barra barra;
    private final ModeloArvores modelo = new ModeloArvores();
    private final EnumMap<TipoArvore, PainelArvore> paineis = new EnumMap<>(TipoArvore.class);
    private final EnumMap<TipoArvore, Integer> focos = new EnumMap<>(TipoArvore.class);
    private final PainelControles controles;
    private final PainelRegistro registro = new PainelRegistro();
    private final PainelFaixa faixaEmDestaque = new PainelFaixa();
    private final PainelPercursos percursos = new PainelPercursos();
    private final PainelFaixas faixas;
    private final JTabbedPane abas = new JTabbedPane();
    private final JPanel areaArvores = new JPanel();
    private final JSplitPane divisor;
    private final Random sorteio = new Random();
    private final AtomicBoolean cancelarTarefa = new AtomicBoolean();

    private PainelControles.Exibicao exibicao;
    private boolean ocupado;

    PainelArvores(Barra barra) {
        this.barra = barra;
        setLayout(new BorderLayout());

        controles = new PainelControles(this);
        faixas = new PainelFaixas(chave -> {
            controles.definirChave(chave);
            buscar(chave);
        });
        for (TipoArvore t : TipoArvore.values()) {
            paineis.put(t, new PainelArvore(t, this, () -> alternarExpansao(t)));
        }

        abas.addTab("Registro de operações", registro);
        abas.addTab("Percursos", percursos);
        abas.addTab("Faixas carregadas", faixas);
        Estilo.compactas(abas);

        areaArvores.setOpaque(false);
        JPanel topo = new JPanel(new BorderLayout(0, 10));
        topo.setOpaque(false);
        topo.add(areaArvores, BorderLayout.CENTER);
        topo.add(faixaEmDestaque, BorderLayout.SOUTH);
        Cartao cartaoAbas = new Cartao(new BorderLayout());
        cartaoAbas.add(abas, BorderLayout.CENTER);
        divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topo, cartaoAbas);
        divisor.setResizeWeight(0.75);
        divisor.setBorder(null);
        divisor.setContinuousLayout(true);

        JPanel direita = new JPanel(new BorderLayout());
        direita.setOpaque(false);
        direita.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));
        direita.add(divisor, BorderLayout.CENTER);
        add(controles, BorderLayout.WEST);
        add(direita, BorderLayout.CENTER);

        exibicaoMudou();   // aplica a disposição e faz o primeiro desenho
    }

    PainelControles controles() {
        return controles;
    }

    /** Cancela qualquer tarefa em andamento (ao fechar a janela). */
    void cancelarTudo() {
        cancelarTarefa.set(true);
    }

    // ------------------------------------------------------------------ exibição

    private VisaoArvore visao(TipoArvore tipo) {
        return paineis.get(tipo).visao();
    }

    @Override
    public void exibicaoMudou() {
        PainelControles.Exibicao nova = controles.exibicao();
        boolean limites = exibicao == null || nova.niveis() != exibicao.niveis() || nova.porNivel() != exibicao.porNivel();
        exibicao = nova;
        for (TipoArvore t : TipoArvore.values()) {
            visao(t).configurar(nova.animar(), nova.velocidade(), nova.fatores());
        }
        aplicarDisposicao(nova.disposicao());
        if (limites) {
            redesenharTodas(false);
        }
    }

    private void aplicarDisposicao(PainelControles.Disposicao d) {
        TipoArvore unica = switch (d) {
            case SO_BST -> TipoArvore.BST;
            case SO_AVL -> TipoArvore.AVL;
            case SO_RUBRO_NEGRA -> TipoArvore.RUBRO_NEGRA;
            default -> null;
        };
        areaArvores.removeAll();
        if (unica != null) {
            areaArvores.setLayout(new BorderLayout());
            areaArvores.add(paineis.get(unica), BorderLayout.CENTER);
        } else {
            areaArvores.setLayout(d == PainelControles.Disposicao.COLUNAS ? new GridLayout(1, 3, 12, 0) : new GridLayout(3, 1, 0, 12));
            for (TipoArvore t : TipoArvore.values()) {
                areaArvores.add(paineis.get(t));
            }
        }
        for (TipoArvore t : TipoArvore.values()) {
            paineis.get(t).definirExpandido(t == unica);
        }
        areaArvores.revalidate();
        areaArvores.repaint();
    }

    private void alternarExpansao(TipoArvore tipo) {
        PainelControles.Disposicao atual = controles.exibicao().disposicao();
        PainelControles.Disposicao unica = switch (tipo) {
            case BST -> PainelControles.Disposicao.SO_BST;
            case AVL -> PainelControles.Disposicao.SO_AVL;
            case RUBRO_NEGRA -> PainelControles.Disposicao.SO_RUBRO_NEGRA;
        };
        controles.definirDisposicao(atual == unica ? PainelControles.Disposicao.COLUNAS : unica);
    }

    // ------------------------------------------------------------------ desenhos

    private PedidoDesenho pedidoBase(TipoArvore tipo) {
        return new PedidoDesenho(exibicao.niveis(), exibicao.porNivel(), focos.get(tipo), List.of(), null);
    }

    /** Desenho da árvore (com o foco atual); {@code caminho} e {@code fantasma} pedem o caminho de uma busca. */
    private Desenho desenhoComCaminho(TipoArvore tipo, List<Integer> caminho, Integer fantasma) {
        Desenho d = ConstrutorDesenho.construir(modelo.arvore(tipo).raizVisivel(),
                pedidoBase(tipo).comCaminho(caminho, fantasma));
        if (focos.get(tipo) != null && !d.focoAplicado) {
            focos.remove(tipo); // a chave em foco não existe mais: volta a mostrar a árvore inteira
        }
        return d;
    }

    private Desenho desenhoNormal(TipoArvore tipo) {
        return desenhoComCaminho(tipo, List.of(), null);
    }

    private void redesenharTodas(boolean animar) {
        for (TipoArvore t : TipoArvore.values()) {
            Desenho d = desenhoNormal(t);
            if (animar) {
                visao(t).mostrar(d);
            } else {
                visao(t).mostrarSemAnimar(d);
            }
            paineis.get(t).atualizarRodape(d, modelo.arvore(t).tamanho(), focos.get(t));
        }
        atualizarNumeros();
    }

    private void atualizarNumeros() {
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = modelo.arvore(t);
            paineis.get(t).atualizarNumeros(a.tamanho(), a.altura());
        }
    }

    private void finalizarAnimacoes() {
        for (TipoArvore t : TipoArvore.values()) {
            visao(t).terminarAnimacao();
        }
    }

    // ------------------------------------------------------------------ operações por chave

    private EnumMap<TipoArvore, Previa> previas(int chave) {
        EnumMap<TipoArvore, Previa> previas = new EnumMap<>(TipoArvore.class);
        for (TipoArvore t : TipoArvore.values()) {
            Caminho c = Caminho.ate(modelo.arvore(t).raizVisivel(), chave);
            previas.put(t, new Previa(c, desenhoComCaminho(t, c.chaves(), c.encontrou() ? null : chave)));
        }
        return previas;
    }

    @Override
    public void inserir(int chave, Faixa faixa) {
        if (ocupado) {
            return;
        }
        finalizarAnimacoes();
        EnumMap<TipoArvore, Previa> previas = previas(chave);
        ResultadoOperacao r = modelo.inserir(chave, faixa);
        String id = NoDesenho.idDoNo(chave);
        for (TipoArvore t : TipoArvore.values()) {
            ResultadoArvore ra = r.de(t);
            Previa p = previas.get(t);
            Desenho depois = desenhoComCaminho(t, ra.caminhoDepois().chaves(), null);
            VisaoArvore.Sequencia s = visao(t).sequencia();
            if (!p.caminho().vazio()) {
                s.mostrar(p.antes()).visitar(p.caminho().chaves());
                if (!ra.sucesso()) {
                    s.marcar(id, TipoMarca.ENCONTRADO);
                }
                s.pausa(150);
            }
            s.mostrar(depois).marcar(id, ra.sucesso() ? TipoMarca.INSERIDO : TipoMarca.ENCONTRADO).executar();
            paineis.get(t).atualizarRodape(depois, ra.tamanho(), focos.get(t));
        }
        boolean nova = r.de(TipoArvore.BST).sucesso();
        faixaEmDestaque.mostrar(chave, faixa);
        concluirOperacao(r, nova ? "inserida" : "já existia: dados substituídos",
                "Chave " + chave + (nova ? " inserida." : " já existia: os dados foram substituídos."));
    }

    @Override
    public void buscar(int chave) {
        if (ocupado) {
            return;
        }
        finalizarAnimacoes();
        EnumMap<TipoArvore, Previa> previas = previas(chave);
        ResultadoOperacao r = modelo.buscar(chave);
        String id = NoDesenho.idDoNo(chave);
        for (TipoArvore t : TipoArvore.values()) {
            ResultadoArvore ra = r.de(t);
            Previa p = previas.get(t);
            VisaoArvore.Sequencia s = visao(t).sequencia().mostrar(p.antes());
            if (!p.caminho().vazio()) {
                s.visitar(p.caminho().chaves());
            }
            if (ra.sucesso()) {
                s.marcar(id, TipoMarca.ENCONTRADO);
            } else if (!p.caminho().vazio()) {
                s.marcar(id, TipoMarca.NAO_ENCONTRADO);
            }
            s.executar();
            paineis.get(t).atualizarRodape(p.antes(), ra.tamanho(), focos.get(t));
        }
        Faixa f = r.dados();
        if (f != null) {
            faixaEmDestaque.mostrar(chave, f);
            concluirOperacao(r, "encontrada: " + f.titulo(),
                    "Chave " + chave + " encontrada: " + f.titulo() + " — " + f.artista() + ".");
        } else {
            faixaEmDestaque.limpar();
            concluirOperacao(r, "não existe", "Chave " + chave + " não existe nas árvores.");
        }
    }

    @Override
    public void remover(int chave) {
        if (ocupado) {
            return;
        }
        finalizarAnimacoes();
        EnumMap<TipoArvore, Previa> previas = previas(chave);
        ResultadoOperacao r = modelo.remover(chave);
        String id = NoDesenho.idDoNo(chave);
        for (TipoArvore t : TipoArvore.values()) {
            ResultadoArvore ra = r.de(t);
            Previa p = previas.get(t);
            Desenho depois = desenhoNormal(t);
            VisaoArvore.Sequencia s = visao(t).sequencia().mostrar(p.antes());
            if (!p.caminho().vazio()) {
                s.visitar(p.caminho().chaves());
            }
            if (ra.sucesso()) {
                s.marcar(id, TipoMarca.REMOVENDO).pausa(350).mostrar(depois);
            } else if (!p.caminho().vazio()) {
                s.marcar(id, TipoMarca.NAO_ENCONTRADO);
            }
            s.executar();
            paineis.get(t).atualizarRodape(ra.sucesso() ? depois : p.antes(), ra.tamanho(), focos.get(t));
        }
        faixaEmDestaque.limpar();
        boolean existia = r.de(TipoArvore.BST).sucesso();
        concluirOperacao(r, existia ? "removida" : "não existia",
                existia ? "Chave " + chave + " removida." : "Chave " + chave + " não existe: nada foi removido.");
    }

    /** Atualiza os números nos painéis, o histórico e as mensagens depois de uma operação por chave. */
    private void concluirOperacao(ResultadoOperacao r, String resultado, String mensagem) {
        String[] celulas = new String[3];
        long[] comparacoes = new long[3];
        StringBuilder resumo = new StringBuilder();
        for (TipoArvore t : TipoArvore.values()) {
            ResultadoArvore ra = r.de(t);
            celulas[t.ordinal()] = Formato.inteiro(ra.comparacoes()) + " comp. · alt. " + Formato.inteiro(ra.altura());
            comparacoes[t.ordinal()] = ra.comparacoes();
            paineis.get(t).atualizarNumeros(ra.tamanho(), ra.altura());
            paineis.get(t).atualizarUltimaOperacao(r.operacao().rotulo.toLowerCase() + ": "
                    + Formato.inteiro(ra.comparacoes()) + " comp.");
            paineis.get(t).atualizarValidade(null);
            resumo.append(resumo.length() == 0 ? "" : " · ").append(t.nome()).append(' ')
                    .append(Formato.inteiro(ra.comparacoes()));
        }
        registro.adicionar(r.operacao().rotulo + " " + r.chave(), resultado, celulas, comparacoes);
        controles.mostrarMensagem(mensagem + " Comparações: " + resumo + ".", false);
        barra.mensagem(mensagem + " Comparações: " + resumo + ".");
    }

    // ------------------------------------------------------------------ várias chaves de uma vez

    @Override
    public void inserirSequencia(int de, int ate, PainelControles.Ordenacao ordem) {
        int menor = Math.min(de, ate);
        int maior = Math.max(de, ate);
        List<Integer> chaves = new ArrayList<>();
        for (int k = menor; k <= maior; k++) {
            chaves.add(k);
        }
        switch (ordem) {
            case DECRESCENTE -> Collections.reverse(chaves);
            case EMBARALHADA -> Collections.shuffle(chaves, new Random(SEMENTE));
            default -> { }
        }
        inserirVarias("Sequência " + Formato.inteiro(menor) + " a " + Formato.inteiro(maior) + " (" + ordem.rotulo + ")",
                chaves);
    }

    @Override
    public void inserirAleatorias(int quantidade, int maximo) {
        int n = Math.min(quantidade, maximo);
        Set<Integer> sorteadas = new LinkedHashSet<>();
        while (sorteadas.size() < n) {
            sorteadas.add(1 + sorteio.nextInt(maximo));
        }
        inserirVarias(n + " chaves aleatórias de 1 a " + Formato.inteiro(maximo), new ArrayList<>(sorteadas));
    }

    private static Faixa faixaDeDemonstracao(int chave) {
        return new Faixa("Faixa " + chave, "Demonstração", "Desconhecido", 0, 0);
    }

    private void inserirVarias(String descricao, List<Integer> chaves) {
        if (ocupado || chaves.isEmpty()) {
            return;
        }
        if (avisarSeHaOutraTarefa()) {
            return;
        }
        boolean passoAPasso = exibicao.animar() && chaves.size() <= LIMITE_ANIMADO;
        finalizarAnimacoes();
        long[] totais = new long[3];
        if (!passoAPasso) {
            for (int k : chaves) {
                modelo.inserirSemDetalhes(k, faixaDeDemonstracao(k), totais);
            }
            faixaEmDestaque.limpar();
            redesenharTodas(exibicao.animar());
            concluirVarias(descricao, chaves.size(), totais);
            return;
        }

        // uma chave de cada vez, esperando as três animações terminarem antes da próxima
        cancelarTarefa.set(false);
        definirOcupado(true);
        barra.iniciarTarefa("Inserindo " + chaves.size() + " chaves...", () -> cancelarTarefa.set(true));
        Iterator<Integer> proximas = chaves.iterator();
        int[] feitas = {0};
        Runnable[] passo = new Runnable[1];
        passo[0] = () -> {
            if (cancelarTarefa.get() || !proximas.hasNext()) {
                finalizarAnimacoes();
                definirOcupado(false);
                barra.terminarTarefa();
                concluirVarias(descricao, feitas[0], totais);
                return;
            }
            int chave = proximas.next();
            ResultadoOperacao r = modelo.inserir(chave, faixaDeDemonstracao(chave));
            somar(totais, r);
            feitas[0]++;
            barra.progresso((double) feitas[0] / chaves.size(), "Inserindo " + feitas[0] + " de " + chaves.size());
            AtomicInteger faltam = new AtomicInteger(TipoArvore.values().length);
            String id = NoDesenho.idDoNo(chave);
            for (TipoArvore t : TipoArvore.values()) {
                ResultadoArvore ra = r.de(t);
                Desenho depois = desenhoComCaminho(t, ra.caminhoDepois().chaves(), null);
                visao(t).sequencia(ACELERACAO_SEQUENCIA).mostrar(depois)
                        .marcar(id, ra.sucesso() ? TipoMarca.INSERIDO : TipoMarca.ENCONTRADO)
                        .aoTerminar(() -> {
                            if (faltam.decrementAndGet() == 0) {
                                passo[0].run();
                            }
                        }).executar();
                paineis.get(t).atualizarRodape(depois, ra.tamanho(), focos.get(t));
                paineis.get(t).atualizarNumeros(ra.tamanho(), ra.altura());
            }
        };
        passo[0].run();
    }

    private static void somar(long[] totais, ResultadoOperacao r) {
        for (TipoArvore t : TipoArvore.values()) {
            totais[t.ordinal()] += r.de(t).comparacoes();
        }
    }

    private void concluirVarias(String descricao, int inseridas, long[] totais) {
        String[] celulas = new String[3];
        StringBuilder resumo = new StringBuilder();
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = modelo.arvore(t);
            int altura = a.altura();   // custa O(n): uma vez por árvore
            celulas[t.ordinal()] = Formato.inteiro(totais[t.ordinal()]) + " comp. · alt. " + Formato.inteiro(altura);
            paineis.get(t).atualizarNumeros(a.tamanho(), altura);
            paineis.get(t).atualizarUltimaOperacao(Formato.inteiro(totais[t.ordinal()]) + " comp. no total");
            paineis.get(t).atualizarValidade(null);
            resumo.append(resumo.length() == 0 ? "" : " · ").append(t.nome()).append(" altura ")
                    .append(Formato.inteiro(altura));
        }
        registro.adicionar("Inserir: " + descricao, Formato.inteiro(inseridas) + " chaves", celulas, totais.clone());
        String texto = Formato.inteiro(inseridas) + " chaves inseridas. Alturas: " + resumo + ".";
        controles.mostrarMensagem(texto, false);
        barra.mensagem(texto);
    }

    @Override
    public void esvaziar() {
        if (ocupado) {
            return;
        }
        finalizarAnimacoes();
        modelo.esvaziar();
        focos.clear();
        faixaEmDestaque.limpar();
        faixas.definirFaixas(List.of());
        controles.definirStatusDataset(" ");
        redesenharTodas(exibicao.animar());
        for (PainelArvore p : paineis.values()) {
            p.atualizarUltimaOperacao(null);
            p.atualizarValidade(null);
        }
        registro.adicionar("Esvaziar as árvores", "vazias", new String[] {"-", "-", "-"}, null);
        controles.mostrarMensagem("Árvores esvaziadas.", false);
        barra.mensagem("Árvores esvaziadas.");
    }

    // ------------------------------------------------------------------ percursos, validação, sorteio

    @Override
    public void percorrer(Ordem ordem) {
        if (ocupado) {
            return;
        }
        finalizarAnimacoes();
        Map<TipoArvore, List<Integer>> listas = modelo.percorrer(ordem);
        percursos.mostrar(ordem, listas);
        abas.setSelectedComponent(percursos);

        String[] celulas = new String[3];
        for (TipoArvore t : TipoArvore.values()) {
            List<Integer> chaves = listas.get(t);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(8, chaves.size()); i++) {
                sb.append(i == 0 ? "" : ", ").append(chaves.get(i));
            }
            celulas[t.ordinal()] = chaves.isEmpty() ? "(vazia)" : sb + (chaves.size() > 8 ? " ..." : "");
            visao(t).sequencia().percurso(idsEmOrdem(visao(t).desenhoAtual(), ordem)).executar();
        }
        String nome = switch (ordem) {
            case PRE -> "Pré-ordem";
            case EM -> "Em ordem";
            case POS -> "Pós-ordem";
        };
        registro.adicionar("Percurso: " + nome, "primeiras chaves de cada árvore", celulas, null);
        String texto = nome + ": os números nos nós mostram a ordem de visita.";
        controles.mostrarMensagem(texto, false);
        barra.mensagem(texto);
    }

    /** Ids dos nós do desenho, na ordem do percurso (só os nós de verdade, sem marcadores). */
    static List<String> idsEmOrdem(Desenho d, Ordem ordem) {
        List<String> ids = new ArrayList<>();
        if (d.vazio()) {
            return ids;
        }
        Deque<NoDesenho> pilha = new ArrayDeque<>();
        switch (ordem) {
            case PRE -> {
                pilha.push(d.raiz);
                while (!pilha.isEmpty()) {
                    NoDesenho n = pilha.pop();
                    if (n.ehNo()) {
                        ids.add(n.id);
                    }
                    if (n.dir != null) {
                        pilha.push(n.dir);
                    }
                    if (n.esq != null) {
                        pilha.push(n.esq);
                    }
                }
            }
            case EM -> {
                NoDesenho atual = d.raiz;
                while (atual != null || !pilha.isEmpty()) {
                    while (atual != null) {
                        pilha.push(atual);
                        atual = atual.esq;
                    }
                    atual = pilha.pop();
                    if (atual.ehNo()) {
                        ids.add(atual.id);
                    }
                    atual = atual.dir;
                }
            }
            case POS -> {
                pilha.push(d.raiz);
                while (!pilha.isEmpty()) {
                    NoDesenho n = pilha.pop();
                    if (n.ehNo()) {
                        ids.add(n.id);
                    }
                    if (n.esq != null) {
                        pilha.push(n.esq);
                    }
                    if (n.dir != null) {
                        pilha.push(n.dir);
                    }
                }
                Collections.reverse(ids);
            }
        }
        return ids;
    }

    @Override
    public void validar() {
        if (ocupado) {
            return;
        }
        Map<TipoArvore, Boolean> r = modelo.validar();
        String[] celulas = new String[3];
        boolean todas = true;
        for (TipoArvore t : TipoArvore.values()) {
            boolean ok = r.get(t);
            todas &= ok;
            celulas[t.ordinal()] = ok ? "válida" : "INVÁLIDA";
            paineis.get(t).atualizarValidade(ok);
        }
        registro.adicionar("Validar as árvores", todas ? "todas válidas" : "ALGUMA INVÁLIDA", celulas, null);
        String texto = todas ? "As três árvores estão válidas (ordem das chaves, balanceamento e cores conferidos)."
                : "Atenção: alguma árvore violou suas propriedades!";
        controles.mostrarMensagem(texto, !todas);
        barra.mensagem(texto);
    }

    @Override
    public void sortearChave() {
        Arvore<Integer, Faixa> bst = modelo.arvore(TipoArvore.BST);
        int chave;
        if (bst.tamanho() == 0) {
            chave = 1 + sorteio.nextInt(99);
        } else {
            List<Integer> existentes = bst.percorrer(Ordem.EM);
            chave = existentes.get(sorteio.nextInt(existentes.size()));
        }
        controles.definirChave(chave);
        controles.mostrarMensagem("Chave " + chave + (bst.tamanho() == 0 ? " sorteada." : " sorteada entre as existentes."), false);
    }

    // ------------------------------------------------------------------ mouse nos desenhos

    @Override
    public void noSelecionado(TipoArvore tipo, NoDesenho no) {
        controles.definirChave(no.chave);
        if (no.dados != null) {
            faixaEmDestaque.mostrar(no.chave, no.dados);
        }
        for (TipoArvore t : TipoArvore.values()) {
            visao(t).selecionar(NoDesenho.idDoNo(no.chave));
        }
    }

    @Override
    public void focoSolicitado(TipoArvore tipo, Integer chave) {
        if (ocupado) {
            return;
        }
        if (chave == null) {
            focos.remove(tipo);
        } else {
            focos.put(tipo, chave);
        }
        Desenho d = desenhoNormal(tipo);
        visao(tipo).mostrar(d);
        paineis.get(tipo).atualizarRodape(d, modelo.arvore(tipo).tamanho(), focos.get(tipo));
    }

    @Override
    public void buscaSolicitada(int chave) {
        controles.definirChave(chave);
        buscar(chave);
    }

    @Override
    public void remocaoSolicitada(int chave) {
        controles.definirChave(chave);
        remover(chave);
    }

    // ------------------------------------------------------------------ carga do dataset

    private void definirOcupado(boolean valor) {
        ocupado = valor;
        controles.definirOcupado(valor);
    }

    /** Outra aba (o benchmark, por exemplo) está ocupada: não começa nada pesado agora. */
    private boolean avisarSeHaOutraTarefa() {
        if (!barra.ocupada()) {
            return false;
        }
        String texto = "Espere a tarefa em andamento (veja a barra de status) terminar.";
        controles.mostrarMensagem(texto, true);
        barra.mensagem(texto);
        return true;
    }

    @Override
    public void carregarDataset(Path csv, int quantidade, boolean embaralhar) {
        if (ocupado || avisarSeHaOutraTarefa()) {
            return;
        }
        finalizarAnimacoes();
        cancelarTarefa.set(false);
        definirOcupado(true);
        barra.iniciarTarefa("Carregando faixas de " + csv.getFileName() + "...", () -> cancelarTarefa.set(true));
        for (PainelArvore p : paineis.values()) {
            p.visao().definirMensagem("Carregando...");
        }
        double[] porArvore = new double[TipoArvore.values().length];
        long inicio = System.nanoTime();

        new SwingWorker<Carregado, double[]>() {
            @Override
            protected Carregado doInBackground() throws Exception {
                List<CarregadorFaixas.Registro> registros = CarregadorFaixas.carregar(csv, quantidade, embaralhar, SEMENTE);
                if (registros.isEmpty()) {
                    throw new IOException("O arquivo não tem nenhuma faixa.");
                }
                publish(new double[] {-1, registros.size()});
                CargaDataset.Resultado resultado = CargaDataset.executar(registros, new CargaDataset.Ouvinte() {
                    @Override
                    public void andamento(TipoArvore tipo, double fracao) {
                        publish(new double[] {tipo.ordinal(), fracao});
                    }

                    @Override
                    public boolean cancelado() {
                        return cancelarTarefa.get();
                    }
                });
                return new Carregado(registros, resultado);
            }

            @Override
            protected void process(List<double[]> blocos) {
                for (double[] b : blocos) {
                    if (b[0] < 0) {
                        barra.progresso(0, "Inserindo " + Formato.inteiro((long) b[1]) + " faixas nas três árvores...");
                    } else {
                        porArvore[(int) b[0]] = b[1];
                    }
                }
                double soma = 0;
                StringBuilder detalhe = new StringBuilder();
                for (TipoArvore t : TipoArvore.values()) {
                    soma += porArvore[t.ordinal()];
                    detalhe.append(detalhe.length() == 0 ? "" : " · ").append(t.nome()).append(' ')
                            .append((int) (porArvore[t.ordinal()] * 100)).append('%');
                }
                barra.progresso(soma / porArvore.length, detalhe.toString());
            }

            @Override
            protected void done() {
                String falha = null;
                try {
                    Carregado c = get();
                    aplicarCarga(csv, c, embaralhar, (System.nanoTime() - inicio) / 1e9);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof CancellationException) {
                        barra.mensagem("Carga cancelada: as árvores continuam como estavam.");
                        controles.mostrarMensagem("Carga cancelada.", false);
                    } else {
                        String texto = (causa instanceof IOException)
                                ? "Não foi possível ler o arquivo: " + causa.getMessage()
                                : "Erro ao carregar: " + causa;
                        controles.mostrarMensagem(texto, true);
                        barra.mensagem(texto);
                        falha = texto;
                    }
                } finally {
                    for (PainelArvore p : paineis.values()) {
                        p.visao().definirMensagem(null);
                    }
                    definirOcupado(false);
                    barra.terminarTarefa();
                }
                if (falha != null) {
                    // só agora, com a interface liberada: o diálogo é modal e a barra não pode ficar "carregando"
                    JOptionPane.showMessageDialog(PainelArvores.this, falha, "Carregar dataset", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void aplicarCarga(Path csv, Carregado c, boolean embaralhar, double segundos) {
        modelo.substituir(c.resultado().arvores(), c.registros());
        focos.clear();
        faixaEmDestaque.limpar();
        faixas.definirFaixas(c.registros());
        redesenharTodas(false);

        String[] celulas = new String[3];
        long[] comparacoes = new long[3];
        for (TipoArvore t : TipoArvore.values()) {
            long comp = c.resultado().comparacoes().get(t);
            celulas[t.ordinal()] = Formato.tempoMs(c.resultado().nanos().get(t) / 1e6) + " · "
                    + Formato.compacto(comp) + " comp. · alt. " + Formato.inteiro(modelo.arvore(t).altura());
            comparacoes[t.ordinal()] = comp;
            paineis.get(t).atualizarUltimaOperacao("carga: " + Formato.compacto(comp) + " comp.");
            paineis.get(t).atualizarValidade(null);
        }
        int n = c.registros().size();
        String ordem = embaralhar ? "embaralhadas" : "em ordem crescente";
        registro.adicionar("Carregar " + Formato.inteiro(n) + " faixas (" + ordem + ")", "tempo · comparações · altura",
                celulas, comparacoes);
        String texto = Formato.inteiro(n) + " faixas de " + csv.getFileName() + " carregadas em "
                + Formato.decimal(segundos, 1) + " s.";
        controles.definirStatusDataset(texto);
        controles.mostrarMensagem("Dataset carregado.", false);
        barra.mensagem(texto);
        abas.setSelectedComponent(registro);
    }

    /** Usado ao abrir o programa com argumentos de linha de comando (csv, quantidade, ordem). */
    void carregarAoAbrir(Path csv, int quantidade, boolean embaralhar) {
        controles.definirArquivoCsv(csv);
        controles.definirParametrosCarga(quantidade, embaralhar);
        SwingUtilities.invokeLater(() -> carregarDataset(csv, quantidade, embaralhar));
    }

    /** Dá espaço aos desenhos na primeira vez que a janela aparece. */
    void ajustarDivisor() {
        divisor.setDividerLocation(0.72);
    }
}
