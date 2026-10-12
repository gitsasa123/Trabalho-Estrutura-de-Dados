package arvores;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.function.Supplier;

/**
 * Mede o desempenho das três árvores com os dados reais do FMA e grava CSVs em results/tabelas.
 * Os gráficos são gerados depois por scripts/gerar_graficos.py a partir desses CSVs.
 *
 * Uso:  java -cp out arvores.Benchmark [csv] [N] [repeticoes] [passo] [pastaSaida] [nAltura]
 *   csv         dados tratados                                  (padrão: data/faixas.csv)
 *   N           nº de faixas no experimento de desempenho       (padrão: 50000; 0 = todas)
 *   repeticoes  quantas vezes cada medição é repetida           (padrão: 5)
 *   passo       a cada quantas inserções medir a altura         (padrão: 1000)
 *   pastaSaida  onde gravar os CSVs                             (padrão: results/tabelas)
 *   nAltura     nº de faixas no experimento de altura           (padrão: 0 = todas)
 *
 * Dois experimentos:
 *   1) ALTURA x ELEMENTOS: insere o dataset aos poucos e mede a altura a cada 'passo' inserções.
 *   2) DESEMPENHO: para cada operação mede tempo, nº de comparações e altura.
 * Cada experimento roda em dois cenários:
 *   ORDENADO    - chaves em ordem crescente de track_id (pior caso da BST)
 *   EMBARALHADO - mesmas chaves em ordem aleatória, com semente fixa (caso médio)
 */
public final class Benchmark {

    private static final long SEMENTE = 2026L;

    private enum Cenario { ORDENADO, EMBARALHADO }

    private enum Operacao { INSERCAO, BUSCA_EXISTENTE, BUSCA_INEXISTENTE, REMOCAO }

    private record Fabrica(String nome, Supplier<Arvore<Integer, Faixa>> criar) {
    }

    /** Tudo que uma execução precisa, montado uma vez por cenário para ser IGUAL nas 3 árvores. */
    private record Dados(List<CarregadorFaixas.Registro> insercao, List<Integer> chavesBusca,
                         List<Integer> chavesInexistentes, List<Integer> chavesRemocao) {
    }

    /** Resultado de uma operação em uma execução. */
    private record Medida(long tempoNs, long comparacoes, int operacoes, int altura) {
    }

    private static final List<Fabrica> ARVORES = List.of(
            new Fabrica("BST", BST::new),
            new Fabrica("AVL", AVL::new),
            new Fabrica("Rubro-Negra", RubroNegra::new));

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "data/faixas.csv");
        int nDesempenho = args.length > 1 ? Integer.parseInt(args[1]) : 50_000;
        int repeticoes = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        int passo = args.length > 3 ? Integer.parseInt(args[3]) : 1000;
        Path saida = Path.of(args.length > 4 ? args[4] : "results/tabelas");
        int nAltura = args.length > 5 ? Integer.parseInt(args[5]) : 0;

        System.out.println("Lendo " + csv + " ...");
        List<CarregadorFaixas.Registro> todos = CarregadorFaixas.carregar(csv, 0, false, SEMENTE);
        todos.sort(Comparator.comparingInt(CarregadorFaixas.Registro::chave));
        for (int i = 1; i < todos.size(); i++) {
            if (todos.get(i).chave() == todos.get(i - 1).chave()) {
                throw new IllegalStateException("Chave repetida no dataset: " + todos.get(i).chave());
            }
        }
        System.out.println(todos.size() + " faixas, track_id de " + todos.get(0).chave()
                + " a " + todos.get(todos.size() - 1).chave());
        if (todos.size() < 10_000) {
            System.out.println("ATENÇÃO: o enunciado exige no mínimo 10.000 registros!");
        }

        Files.createDirectories(saida);
        gravarAmbiente(saida.resolve("ambiente.txt"), csv, todos.size(), nDesempenho, repeticoes, passo);

        List<CarregadorFaixas.Registro> baseAltura = recorte(todos, nAltura);
        experimentoAltura(baseAltura, passo, saida.resolve("altura_incremental.csv"));

        List<CarregadorFaixas.Registro> baseDesempenho = recorte(todos, nDesempenho);
        experimentoDesempenho(baseDesempenho, repeticoes, saida.resolve("desempenho.csv"));

        System.out.println("\nPronto. CSVs em " + saida.toAbsolutePath());
        System.out.println("Agora gere os gráficos: python scripts/gerar_graficos.py");
    }

    private static List<CarregadorFaixas.Registro> recorte(List<CarregadorFaixas.Registro> todos, int n) {
        int tam = (n <= 0 || n > todos.size()) ? todos.size() : n;
        return new ArrayList<>(todos.subList(0, tam));
    }

    // ===================================================================== experimento 1: altura

    private static void experimentoAltura(List<CarregadorFaixas.Registro> base, int passo, Path arquivo)
            throws IOException {
        System.out.println("\n== Experimento 1: altura x nº de elementos (" + base.size()
                + " faixas, a cada " + passo + " inserções) ==");
        StringBuilder csv = new StringBuilder("cenario,arvore,elementos,altura\n");

        for (Cenario cenario : Cenario.values()) {
            List<CarregadorFaixas.Registro> ordem = new ArrayList<>(base);
            if (cenario == Cenario.EMBARALHADO) {
                Collections.shuffle(ordem, new Random(SEMENTE));
            }
            for (Fabrica f : ARVORES) {
                long inicio = System.nanoTime();
                Arvore<Integer, Faixa> arvore = f.criar().get();
                int inseridos = 0;
                for (CarregadorFaixas.Registro r : ordem) {
                    arvore.inserir(r.chave(), r.dados());
                    inseridos++;
                    if (inseridos % passo == 0 || inseridos == ordem.size()) {
                        csv.append(String.format(Locale.ROOT, "%s,%s,%d,%d%n",
                                cenario, f.nome(), inseridos, arvore.altura()));
                    }
                }
                System.out.printf(Locale.ROOT, "  %-11s %-12s altura final %6d  (%.1f s)%n",
                        cenario, f.nome(), arvore.altura(), (System.nanoTime() - inicio) / 1e9);
            }
        }
        Files.writeString(arquivo, csv.toString(), StandardCharsets.UTF_8);
    }

    // ===================================================================== experimento 2: desempenho

    private static void experimentoDesempenho(List<CarregadorFaixas.Registro> base, int repeticoes, Path arquivo)
            throws IOException {
        int n = base.size();
        System.out.println("\n== Experimento 2: desempenho (" + n + " faixas, " + repeticoes
                + " repetições por medição) ==");

        // aquecimento: deixa o JIT do Java compilar o código antes de medir (resultado descartado)
        System.out.println("  aquecendo a JVM ...");
        List<CarregadorFaixas.Registro> pequeno = recorte(base, 5000);
        for (Cenario cenario : Cenario.values()) {
            Dados d = montarDados(pequeno, cenario);
            for (Fabrica f : ARVORES) {
                executar(f, d);
            }
        }

        StringBuilder csv = new StringBuilder("cenario,arvore,n,operacao,operacoes,"
                + "tempo_ms_medio,tempo_ms_desvio,comparacoes,comparacoes_por_operacao,altura\n");

        for (Cenario cenario : Cenario.values()) {
            Dados dados = montarDados(base, cenario);
            for (Fabrica f : ARVORES) {
                long inicio = System.nanoTime();
                Medida[][] medidas = new Medida[repeticoes][];
                for (int rep = 0; rep < repeticoes; rep++) {
                    System.gc();
                    medidas[rep] = executar(f, dados);
                }
                for (Operacao op : Operacao.values()) {
                    double[] tempos = new double[repeticoes];
                    for (int rep = 0; rep < repeticoes; rep++) {
                        tempos[rep] = medidas[rep][op.ordinal()].tempoNs() / 1e6;
                    }
                    Medida m = medidas[0][op.ordinal()]; // comparações e altura não variam entre repetições
                    csv.append(String.format(Locale.ROOT, "%s,%s,%d,%s,%d,%.4f,%.4f,%d,%.2f,%d%n",
                            cenario, f.nome(), n, op, m.operacoes(), media(tempos), desvio(tempos),
                            m.comparacoes(), (double) m.comparacoes() / m.operacoes(), m.altura()));
                }
                System.out.printf(Locale.ROOT, "  %-11s %-12s ok (%.1f s)%n",
                        cenario, f.nome(), (System.nanoTime() - inicio) / 1e9);
            }
        }
        Files.writeString(arquivo, csv.toString(), StandardCharsets.UTF_8);
        imprimirResumo(csv.toString());
    }

    /** Monta as listas de chaves de um cenário. A mesma semente garante listas idênticas para as 3 árvores. */
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

        // chaves que NÃO estão na árvore: sorteadas entre a menor chave e a maior + n
        int min = chaves.get(0);
        int max = chaves.get(n - 1);
        Random rnd = new Random(SEMENTE + 2);
        List<Integer> inexistentes = new ArrayList<>(n);
        while (inexistentes.size() < n) {
            int k = min + rnd.nextInt(max - min + n + 1);
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

    /** Uma execução completa: inserir tudo, buscar existentes, buscar inexistentes, remover metade. */
    private static Medida[] executar(Fabrica f, Dados d) {
        Arvore<Integer, Faixa> a = f.criar().get();
        Medida[] r = new Medida[Operacao.values().length];

        // --- inserção
        a.zerarContador();
        long t0 = System.nanoTime();
        for (CarregadorFaixas.Registro x : d.insercao()) {
            a.inserir(x.chave(), x.dados());
        }
        long t1 = System.nanoTime();
        r[Operacao.INSERCAO.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.insercao().size(), a.altura());
        exigir(a.tamanho() == d.insercao().size(), f.nome() + ": tamanho errado após inserir");

        // --- busca de chaves que existem
        a.zerarContador();
        int achou = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesBusca()) {
            if (a.buscar(k) != null) {
                achou++;
            }
        }
        t1 = System.nanoTime();
        r[Operacao.BUSCA_EXISTENTE.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesBusca().size(), a.altura());
        exigir(achou == d.chavesBusca().size(), f.nome() + ": busca não encontrou chave existente");

        // --- busca de chaves que NÃO existem
        a.zerarContador();
        achou = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesInexistentes()) {
            if (a.buscar(k) != null) {
                achou++;
            }
        }
        t1 = System.nanoTime();
        r[Operacao.BUSCA_INEXISTENTE.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesInexistentes().size(), a.altura());
        exigir(achou == 0, f.nome() + ": busca encontrou chave que não existe");

        // --- remoção de metade das chaves
        a.zerarContador();
        int removidos = 0;
        t0 = System.nanoTime();
        for (int k : d.chavesRemocao()) {
            if (a.remover(k)) {
                removidos++;
            }
        }
        t1 = System.nanoTime();
        r[Operacao.REMOCAO.ordinal()] = new Medida(t1 - t0, a.contadorComparacoes(), d.chavesRemocao().size(), a.altura());
        exigir(removidos == d.chavesRemocao().size(), f.nome() + ": remoção falhou");
        exigir(a.tamanho() == d.insercao().size() - removidos, f.nome() + ": tamanho errado após remover");
        exigir(a.validar(), f.nome() + ": árvore inválida após o benchmark");
        return r;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException("Benchmark inconsistente: " + mensagem);
        }
    }

    // ===================================================================== utilidades

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

    private static void imprimirResumo(String csv) {
        System.out.println("\n== Resumo (tempo médio em ms | comparações | altura) ==");
        String[] linhas = csv.split("\n");
        for (int i = 1; i < linhas.length; i++) {
            String[] c = linhas[i].split(",");
            System.out.printf(Locale.ROOT, "  %-11s %-12s %-18s %12s ms %14s comp. altura %s%n",
                    c[0], c[1], c[3], c[5], c[7], c[9]);
        }
    }

    private static void gravarAmbiente(Path arquivo, Path csv, int total, int nDesempenho, int repeticoes,
                                       int passo) throws IOException {
        String texto = "data/hora: " + LocalDateTime.now() + "\n"
                + "java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")\n"
                + "sistema: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "\n"
                + "processadores logicos: " + Runtime.getRuntime().availableProcessors() + "\n"
                + "memoria maxima da JVM (MB): " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "\n"
                + "dataset: " + csv + " (" + total + " faixas)\n"
                + "N do experimento de desempenho: " + nDesempenho + "\n"
                + "repeticoes: " + repeticoes + "\n"
                + "passo da altura: " + passo + "\n"
                + "semente: " + SEMENTE + "\n";
        Files.writeString(arquivo, texto, StandardCharsets.UTF_8);
    }
}
