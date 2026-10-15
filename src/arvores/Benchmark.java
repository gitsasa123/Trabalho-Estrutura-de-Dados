package arvores;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;

import arvores.ResultadoBenchmark.LinhaDesempenho;
import arvores.ResultadoBenchmark.PontoAltura;

/**
 * Benchmark das três árvores: altura × nº de elementos e desempenho (tempo, comparações e altura).
 * Roda pela linha de comando ({@link #main}) ou, com um {@link Ouvinte}, pela interface gráfica.
 */
public final class Benchmark {

    public static final long SEMENTE = 2026L;

    private static final int TAMANHO_AQUECIMENTO = 5000;
    private static final int MASCARA_ANDAMENTO = 1023; // avisa o andamento a cada 1024 operações

    /** Parâmetros do benchmark (os mesmos da linha de comando; 0 em N = todas as faixas). */
    public record Configuracao(Path csv, int nDesempenho, int repeticoes, int passo, int nAltura) {

        public static Configuracao padrao() {
            return new Configuracao(Path.of("data/faixas.csv"), 50_000, 5, 1000, 0);
        }
    }

    /** Quem acompanha a execução: recebe mensagens e andamento, e pode pedir o cancelamento. */
    public interface Ouvinte {

        void mensagem(String texto);

        /** Andamento geral, de 0 a 1 (aproximado: as etapas não levam o mesmo tempo). */
        default void progresso(double fracao) {
        }

        /** Se devolver {@code true}, o benchmark para e lança {@link CancellationException}. */
        default boolean cancelado() {
            return false;
        }
    }

    private record Dados(List<CarregadorFaixas.Registro> insercao, List<Integer> chavesBusca,
                         List<Integer> chavesInexistentes, List<Integer> chavesRemocao) {
    }

    private record Medida(long tempoNs, long comparacoes, int operacoes, int altura) {
    }

    /** Converte "tarefa X, Y% feita" em andamento geral e confere se pediram para cancelar. */
    private static final class Andamento {
        private final Ouvinte ouvinte;
        private final double totalTarefas;
        private double concluidas;

        Andamento(Ouvinte ouvinte, double totalTarefas) {
            this.ouvinte = ouvinte;
            this.totalTarefas = totalTarefas;
        }

        void dentroDaTarefa(double fracao) {
            conferirCancelamento();
            ouvinte.progresso(Math.min(1.0, (concluidas + fracao) / totalTarefas));
        }

        void tarefaConcluida() {
            concluidas++;
            ouvinte.progresso(Math.min(1.0, concluidas / totalTarefas));
        }

        void conferirCancelamento() {
            if (ouvinte.cancelado()) {
                throw new CancellationException("Benchmark cancelado");
            }
        }
    }

    private Benchmark() {
    }

    // ------------------------------------------------------------------ linha de comando

    public static void main(String[] args) throws IOException {
        Configuracao padrao = Configuracao.padrao();
        Configuracao cfg = new Configuracao(
                Path.of(args.length > 0 ? args[0] : padrao.csv().toString()),
                args.length > 1 ? Integer.parseInt(args[1]) : padrao.nDesempenho(),
                args.length > 2 ? Integer.parseInt(args[2]) : padrao.repeticoes(),
                args.length > 3 ? Integer.parseInt(args[3]) : padrao.passo(),
                args.length > 5 ? Integer.parseInt(args[5]) : padrao.nAltura());
        Path saida = Path.of(args.length > 4 ? args[4] : "results/tabelas");

        ResultadoBenchmark resultado = executar(cfg, System.out::println);
        resultado.gravar(saida);

        imprimirResumo(resultado);
        System.out.println("\nPronto. CSVs em " + saida.toAbsolutePath());
        System.out.println("Agora gere os gráficos: python scripts/gerar_graficos.py");
    }

    private static void imprimirResumo(ResultadoBenchmark resultado) {
        System.out.println("\n== Resumo (tempo médio em ms | comparações | altura) ==");
        for (LinhaDesempenho l : resultado.desempenho()) {
            System.out.printf(Locale.ROOT, "  %-11s %-12s %-18s %12.4f ms %14d comp. altura %d%n",
                    l.cenario(), l.arvore().nome(), l.operacao(), l.tempoMsMedio(), l.comparacoes(), l.altura());
        }
    }

    // ------------------------------------------------------------------ execução

    /**
     * Roda os dois experimentos e devolve as medidas (não grava nada em disco).
     *
     * @throws CancellationException se o {@code ouvinte} pedir o cancelamento
     */
    public static ResultadoBenchmark executar(Configuracao cfg, Ouvinte ouvinte) throws IOException {
        if (cfg.repeticoes() < 1 || cfg.passo() < 1) {
            throw new IllegalArgumentException("repetições e passo devem ser pelo menos 1");
        }
        ouvinte.mensagem("Lendo " + cfg.csv() + " ...");
        List<CarregadorFaixas.Registro> todos = CarregadorFaixas.carregar(cfg.csv(), 0, false, SEMENTE);
        todos.sort(Comparator.comparingInt(CarregadorFaixas.Registro::chave));
        for (int i = 1; i < todos.size(); i++) {
            if (todos.get(i).chave() == todos.get(i - 1).chave()) {
                throw new IllegalStateException("Chave repetida no dataset: " + todos.get(i).chave());
            }
        }
        if (todos.size() < 2) {
            throw new IllegalStateException("O dataset precisa de pelo menos 2 faixas");
        }
        ouvinte.mensagem(todos.size() + " faixas, track_id de " + todos.get(0).chave()
                + " a " + todos.get(todos.size() - 1).chave());
        if (todos.size() < 10_000) {
            ouvinte.mensagem("ATENÇÃO: o enunciado exige no mínimo 10.000 registros!");
        }

        List<String> ambiente = descreverAmbiente(cfg, todos.size());
        int tarefas = TipoArvore.values().length * Cenario.values().length * (1 + cfg.repeticoes());
        Andamento andamento = new Andamento(ouvinte, tarefas);

        List<PontoAltura> alturas = experimentoAltura(recorte(todos, cfg.nAltura()), cfg.passo(), ouvinte, andamento);
        List<LinhaDesempenho> desempenho = experimentoDesempenho(recorte(todos, cfg.nDesempenho()),
                cfg.repeticoes(), ouvinte, andamento);
        return new ResultadoBenchmark(desempenho, alturas, ambiente);
    }

    private static List<CarregadorFaixas.Registro> recorte(List<CarregadorFaixas.Registro> todos, int n) {
        int tam = (n <= 0 || n > todos.size()) ? todos.size() : n;
        return new ArrayList<>(todos.subList(0, tam));
    }

    private static List<String> descreverAmbiente(Configuracao cfg, int total) {
        Runtime jvm = Runtime.getRuntime();
        return List.of(
                "data/hora: " + LocalDateTime.now(),
                "java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")",
                "sistema: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                "processadores logicos: " + jvm.availableProcessors(),
                "memoria maxima da JVM (MB): " + jvm.maxMemory() / (1024 * 1024),
                "dataset: " + cfg.csv() + " (" + total + " faixas)",
                "N do experimento de desempenho: " + cfg.nDesempenho(),
                "repeticoes: " + cfg.repeticoes(),
                "passo da altura: " + cfg.passo(),
                "semente: " + SEMENTE);
    }

    // ------------------------------------------------------------------ experimento 1: altura

    private static List<PontoAltura> experimentoAltura(List<CarregadorFaixas.Registro> base, int passo,
                                                       Ouvinte ouvinte, Andamento andamento) {
        ouvinte.mensagem("\n== Experimento 1: altura x nº de elementos (" + base.size()
                + " faixas, a cada " + passo + " inserções) ==");
        List<PontoAltura> pontos = new ArrayList<>();

        for (Cenario cenario : Cenario.values()) {
            List<CarregadorFaixas.Registro> ordem = new ArrayList<>(base);
            if (cenario == Cenario.EMBARALHADO) {
                Collections.shuffle(ordem, new Random(SEMENTE));
            }
            for (TipoArvore tipo : TipoArvore.values()) {
                long inicio = System.nanoTime();
                Arvore<Integer, Faixa> arvore = tipo.criar();
                int inseridos = 0;
                for (CarregadorFaixas.Registro r : ordem) {
                    arvore.inserir(r.chave(), r.dados());
                    inseridos++;
                    if ((inseridos & MASCARA_ANDAMENTO) == 0) {
                        andamento.dentroDaTarefa((double) inseridos / ordem.size());
                    }
                    if (inseridos % passo == 0 || inseridos == ordem.size()) {
                        pontos.add(new PontoAltura(cenario, tipo, inseridos, arvore.altura()));
                    }
                }
                andamento.tarefaConcluida();
                ouvinte.mensagem(String.format(Locale.ROOT, "  %-11s %-12s altura final %6d  (%.1f s)",
                        cenario, tipo.nome(), arvore.altura(), (System.nanoTime() - inicio) / 1e9));
            }
        }
        return pontos;
    }

    // ------------------------------------------------------------------ experimento 2: desempenho

    private static List<LinhaDesempenho> experimentoDesempenho(List<CarregadorFaixas.Registro> base,
                                                               int repeticoes, Ouvinte ouvinte,
                                                               Andamento andamento) {
        int n = base.size();
        if (n < 2) {
            throw new IllegalArgumentException("O experimento de desempenho precisa de pelo menos 2 faixas");
        }
        ouvinte.mensagem("\n== Experimento 2: desempenho (" + n + " faixas, " + repeticoes
                + " repetições por medição) ==");

        ouvinte.mensagem("  aquecendo a JVM ...");
        List<CarregadorFaixas.Registro> pequeno = recorte(base, TAMANHO_AQUECIMENTO);
        for (Cenario cenario : Cenario.values()) {
            Dados d = montarDados(pequeno, cenario);
            for (TipoArvore tipo : TipoArvore.values()) {
                andamento.conferirCancelamento();
                executar(tipo, d, null);
            }
        }

        List<LinhaDesempenho> linhas = new ArrayList<>();
        for (Cenario cenario : Cenario.values()) {
            Dados dados = montarDados(base, cenario);
            for (TipoArvore tipo : TipoArvore.values()) {
                long inicio = System.nanoTime();
                Medida[][] medidas = new Medida[repeticoes][];
                for (int rep = 0; rep < repeticoes; rep++) {
                    andamento.conferirCancelamento();
                    System.gc();
                    medidas[rep] = executar(tipo, dados, andamento);
                    andamento.tarefaConcluida();
                }
                for (Operacao op : Operacao.values()) {
                    double[] tempos = new double[repeticoes];
                    for (int rep = 0; rep < repeticoes; rep++) {
                        tempos[rep] = medidas[rep][op.ordinal()].tempoNs() / 1e6;
                    }
                    Medida m = medidas[0][op.ordinal()]; // comparações e altura não variam entre repetições
                    linhas.add(new LinhaDesempenho(cenario, tipo, n, op, m.operacoes(), media(tempos),
                            desvio(tempos), m.comparacoes(), (double) m.comparacoes() / m.operacoes(), m.altura()));
                }
                ouvinte.mensagem(String.format(Locale.ROOT, "  %-11s %-12s ok (%.1f s)",
                        cenario, tipo.nome(), (System.nanoTime() - inicio) / 1e9));
            }
        }
        return linhas;
    }

    private static Dados montarDados(List<CarregadorFaixas.Registro> base, Cenario cenario) {
        int n = base.size();
        List<CarregadorFaixas.Registro> insercao = new ArrayList<>(base);
        if (cenario == Cenario.EMBARALHADO) {
            Collections.shuffle(insercao, new Random(SEMENTE));
        }

        List<Integer> chaves = new ArrayList<>(n);
        Set<Integer> existentes = new HashSet<>();
        for (CarregadorFaixas.Registro r : base) {
            chaves.add(r.chave());
            existentes.add(r.chave());
        }

        List<Integer> chavesBusca = new ArrayList<>(chaves);
        Collections.shuffle(chavesBusca, new Random(SEMENTE + 1));

        int min = chaves.get(0);
        int max = chaves.get(n - 1);
        Random rnd = new Random(SEMENTE + 2);
        List<Integer> inexistentes = new ArrayList<>(n);
        long amplitude = (long) max - min + n + 1;
        while (inexistentes.size() < n) {
            long candidato = (amplitude <= Integer.MAX_VALUE) ? min + rnd.nextInt((int) amplitude)
                    : min + rnd.nextLong(amplitude);
            if (candidato > Integer.MAX_VALUE) {
                candidato -= amplitude;   // chaves perto do limite de int: usa o lado de baixo
            }
            if (candidato < Integer.MIN_VALUE || candidato > Integer.MAX_VALUE) {
                continue;
            }
            int k = (int) candidato;
            if (!existentes.contains(k)) {
                inexistentes.add(k);
            }
        }

        // remove metade das chaves, em ordem aleatória
        List<Integer> chavesRemocao = new ArrayList<>(chaves);
        Collections.shuffle(chavesRemocao, new Random(SEMENTE + 3));
        chavesRemocao = new ArrayList<>(chavesRemocao.subList(0, n / 2));

        return new Dados(insercao, chavesBusca, inexistentes, chavesRemocao);
    }

    /**
     * Uma execução completa: inserir tudo, buscar existentes, buscar inexistentes, remover metade.
     * O {@code andamento} pode ser {@code null} (aquecimento da JVM).
     */
    private static Medida[] executar(TipoArvore tipo, Dados d, Andamento andamento) {
        Arvore<Integer, Faixa> a = tipo.criar();
        String nome = tipo.nome();
        Medida[] r = new Medida[Operacao.values().length];

        a.zerarContador();
        int feitas = 0;
        long t0 = System.nanoTime();
        for (CarregadorFaixas.Registro x : d.insercao()) {
            a.inserir(x.chave(), x.dados());
            if ((++feitas & MASCARA_ANDAMENTO) == 0) {
                avisar(andamento, Operacao.INSERCAO, (double) feitas / d.insercao().size());
            }
        }
        long t1 = System.nanoTime();
        r[Operacao.INSERCAO.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.insercao().size(), a.altura());
        exigir(a.tamanho() == d.insercao().size(), nome + ": tamanho errado após inserir");

        a.zerarContador();
        int achou = 0;
        feitas = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesBusca()) {
            if (a.buscar(k) != null) {
                achou++;
            }
            if ((++feitas & MASCARA_ANDAMENTO) == 0) {
                avisar(andamento, Operacao.BUSCA_EXISTENTE, (double) feitas / d.chavesBusca().size());
            }
        }
        t1 = System.nanoTime();
        r[Operacao.BUSCA_EXISTENTE.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesBusca().size(), a.altura());
        exigir(achou == d.chavesBusca().size(), nome + ": busca não encontrou chave existente");

        a.zerarContador();
        achou = 0;
        feitas = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesInexistentes()) {
            if (a.buscar(k) != null) {
                achou++;
            }
            if ((++feitas & MASCARA_ANDAMENTO) == 0) {
                avisar(andamento, Operacao.BUSCA_INEXISTENTE, (double) feitas / d.chavesInexistentes().size());
            }
        }
        t1 = System.nanoTime();
        r[Operacao.BUSCA_INEXISTENTE.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesInexistentes().size(), a.altura());
        exigir(achou == 0, nome + ": busca encontrou chave que não existe");

        a.zerarContador();
        int removidos = 0;
        feitas = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesRemocao()) {
            if (a.remover(k)) {
                removidos++;
            }
            if ((++feitas & MASCARA_ANDAMENTO) == 0) {
                avisar(andamento, Operacao.REMOCAO, (double) feitas / d.chavesRemocao().size());
            }
        }
        t1 = System.nanoTime();
        r[Operacao.REMOCAO.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesRemocao().size(), a.altura());
        exigir(removidos == d.chavesRemocao().size(), nome + ": remoção falhou");
        exigir(a.tamanho() == d.insercao().size() - removidos, nome + ": tamanho errado após remover");
        exigir(a.validar(), nome + ": árvore inválida após o benchmark");
        return r;
    }

    /** As quatro operações de uma execução dividem igualmente o andamento da tarefa. */
    private static void avisar(Andamento andamento, Operacao operacao, double fracaoDaOperacao) {
        if (andamento != null) {
            andamento.dentroDaTarefa((operacao.ordinal() + fracaoDaOperacao) / Operacao.values().length);
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException("Benchmark inconsistente: " + mensagem);
        }
    }

    private static double media(double[] v) {
        double soma = 0;
        for (double x : v) {
            soma += x;
        }
        return soma / v.length;
    }

    private static double desvio(double[] v) {
        if (v.length < 2) {
            return 0;
        }
        double m = media(v);
        double soma = 0;
        for (double x : v) {
            soma += (x - m) * (x - m);
        }
        return Math.sqrt(soma / (v.length - 1));
    }
}
