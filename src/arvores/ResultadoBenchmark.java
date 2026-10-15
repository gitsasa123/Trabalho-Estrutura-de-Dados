package arvores;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Tudo o que o benchmark mede, mais a leitura e a gravação dos CSVs (o formato é o que
 * {@code scripts/gerar_graficos.py} espera).
 *
 * @param desempenho tempo, comparações e altura de cada operação, árvore e cenário
 * @param alturas    altura de cada árvore a cada {@code passo} inserções
 * @param ambiente   linhas "chave: valor" descrevendo a máquina e os parâmetros da execução
 */
public record ResultadoBenchmark(List<LinhaDesempenho> desempenho, List<PontoAltura> alturas,
                                 List<String> ambiente) {

    public static final String ARQUIVO_DESEMPENHO = "desempenho.csv";
    public static final String ARQUIVO_ALTURA = "altura_incremental.csv";
    public static final String ARQUIVO_AMBIENTE = "ambiente.txt";

    private static final String CABECALHO_DESEMPENHO = "cenario,arvore,n,operacao,operacoes,"
            + "tempo_ms_medio,tempo_ms_desvio,comparacoes,comparacoes_por_operacao,altura";
    private static final String CABECALHO_ALTURA = "cenario,arvore,elementos,altura";

    /** Uma operação (inserção, busca...) de uma árvore em um cenário; os tempos são em ms. */
    public record LinhaDesempenho(Cenario cenario, TipoArvore arvore, int n, Operacao operacao, int operacoes,
                                  double tempoMsMedio, double tempoMsDesvio, long comparacoes,
                                  double comparacoesPorOperacao, int altura) {
    }

    /** Altura da árvore depois de inserir {@code elementos} chaves. */
    public record PontoAltura(Cenario cenario, TipoArvore arvore, int elementos, int altura) {
    }

    public Optional<LinhaDesempenho> linha(Cenario cenario, TipoArvore arvore, Operacao operacao) {
        return desempenho.stream()
                .filter(l -> l.cenario() == cenario && l.arvore() == arvore && l.operacao() == operacao)
                .findFirst();
    }

    /** Pontos de altura de uma árvore em um cenário, em ordem crescente de elementos. */
    public List<PontoAltura> serieAltura(Cenario cenario, TipoArvore arvore) {
        return alturas.stream()
                .filter(p -> p.cenario() == cenario && p.arvore() == arvore)
                .sorted(Comparator.comparingInt(PontoAltura::elementos))
                .toList();
    }

    /** Altura depois da última inserção medida. */
    public OptionalInt alturaFinal(Cenario cenario, TipoArvore arvore) {
        List<PontoAltura> serie = serieAltura(cenario, arvore);
        return serie.isEmpty() ? OptionalInt.empty() : OptionalInt.of(serie.get(serie.size() - 1).altura());
    }

    /** Nº de faixas do experimento de desempenho (0 se não há medidas). */
    public int nDesempenho() {
        return desempenho.isEmpty() ? 0 : desempenho.get(0).n();
    }

    public boolean vazio() {
        return desempenho.isEmpty() && alturas.isEmpty();
    }

    // ------------------------------------------------------------------ escrita

    public String csvDesempenho() {
        StringBuilder csv = new StringBuilder(CABECALHO_DESEMPENHO).append('\n');
        for (LinhaDesempenho l : desempenho) {
            csv.append(String.format(Locale.ROOT, "%s,%s,%d,%s,%d,%.4f,%.4f,%d,%.2f,%d\n",
                    l.cenario(), l.arvore().nome(), l.n(), l.operacao(), l.operacoes(),
                    l.tempoMsMedio(), l.tempoMsDesvio(), l.comparacoes(), l.comparacoesPorOperacao(), l.altura()));
        }
        return csv.toString();
    }

    public String csvAltura() {
        StringBuilder csv = new StringBuilder(CABECALHO_ALTURA).append('\n');
        for (PontoAltura p : alturas) {
            csv.append(String.format(Locale.ROOT, "%s,%s,%d,%d\n",
                    p.cenario(), p.arvore().nome(), p.elementos(), p.altura()));
        }
        return csv.toString();
    }

    /** Grava {@code desempenho.csv}, {@code altura_incremental.csv} e {@code ambiente.txt} na pasta. */
    public void gravar(Path pasta) throws IOException {
        Files.createDirectories(pasta);
        Files.writeString(pasta.resolve(ARQUIVO_DESEMPENHO), csvDesempenho(), StandardCharsets.UTF_8);
        Files.writeString(pasta.resolve(ARQUIVO_ALTURA), csvAltura(), StandardCharsets.UTF_8);
        if (!ambiente.isEmpty()) {
            Files.writeString(pasta.resolve(ARQUIVO_AMBIENTE), String.join("\n", ambiente) + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    // ------------------------------------------------------------------ leitura

    /** Lê os CSVs gravados por {@link #gravar} (o {@code ambiente.txt} é opcional). */
    public static ResultadoBenchmark ler(Path pasta) throws IOException {
        List<LinhaDesempenho> desempenho = lerDesempenho(pasta.resolve(ARQUIVO_DESEMPENHO));
        List<PontoAltura> alturas = lerAltura(pasta.resolve(ARQUIVO_ALTURA));
        Path arquivoAmbiente = pasta.resolve(ARQUIVO_AMBIENTE);
        List<String> ambiente = Files.exists(arquivoAmbiente)
                ? Files.readAllLines(arquivoAmbiente, StandardCharsets.UTF_8).stream()
                        .filter(l -> !l.isBlank()).toList()
                : List.of();
        return new ResultadoBenchmark(desempenho, alturas, ambiente);
    }

    private static List<LinhaDesempenho> lerDesempenho(Path arquivo) throws IOException {
        List<LinhaDesempenho> linhas = new ArrayList<>();
        List<String[]> campos = lerCampos(arquivo, CABECALHO_DESEMPENHO, 10);
        for (int i = 0; i < campos.size(); i++) {
            String[] c = campos.get(i);
            try {
                linhas.add(new LinhaDesempenho(Cenario.valueOf(c[0]), TipoArvore.porNome(c[1]),
                        Integer.parseInt(c[2]), Operacao.valueOf(c[3]), Integer.parseInt(c[4]),
                        Double.parseDouble(c[5]), Double.parseDouble(c[6]), Long.parseLong(c[7]),
                        Double.parseDouble(c[8]), Integer.parseInt(c[9])));
            } catch (IllegalArgumentException e) {
                throw new IOException(arquivo + ", linha " + (i + 2) + ": " + e.getMessage(), e);
            }
        }
        return linhas;
    }

    private static List<PontoAltura> lerAltura(Path arquivo) throws IOException {
        List<PontoAltura> pontos = new ArrayList<>();
        List<String[]> campos = lerCampos(arquivo, CABECALHO_ALTURA, 4);
        for (int i = 0; i < campos.size(); i++) {
            String[] c = campos.get(i);
            try {
                pontos.add(new PontoAltura(Cenario.valueOf(c[0]), TipoArvore.porNome(c[1]),
                        Integer.parseInt(c[2]), Integer.parseInt(c[3])));
            } catch (IllegalArgumentException e) {
                throw new IOException(arquivo + ", linha " + (i + 2) + ": " + e.getMessage(), e);
            }
        }
        return pontos;
    }

    /** Lê as linhas de dados (sem o cabeçalho) já separadas por vírgula, conferindo o cabeçalho. */
    private static List<String[]> lerCampos(Path arquivo, String cabecalho, int colunas) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (!linhas.isEmpty() && linhas.get(0).startsWith("\uFEFF")) {
            linhas.set(0, linhas.get(0).substring(1));   // CSV salvo pelo Excel/Bloco de Notas começa com BOM
        }
        if (linhas.isEmpty() || !linhas.get(0).trim().equals(cabecalho)) {
            throw new IOException(arquivo + ": cabeçalho inesperado (esperado: " + cabecalho + ")");
        }
        List<String[]> resultado = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            if (linhas.get(i).isBlank()) {
                continue;
            }
            String[] c = linhas.get(i).trim().split(",", -1);
            if (c.length != colunas) {
                throw new IOException(arquivo + ", linha " + (i + 1) + ": esperado " + colunas
                        + " colunas, encontrado " + c.length);
            }
            resultado.add(c);
        }
        return resultado;
    }
}
