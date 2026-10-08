import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        Path arquivo = Path.of(args.length > 0 ? args[0] : "genres.csv");

        // 1) Ler o dataset (pula o cabeçalho)
        List<Genero> dataset = Files.readAllLines(arquivo).stream()
                .skip(1)
                .filter(l -> !l.isBlank())
                .map(Genero::deLinhaCsv)
                .collect(Collectors.toList());
        System.out.println("Gêneros lidos: " + dataset.size());

        // 2) Árvore ordenada por TÍTULO (ordem alfabética, sem diferenciar maiúsculas)
        ArvoreBinaria<Genero> porTitulo = new ArvoreBinaria<>(
                Comparator.comparing(Genero::titulo, String.CASE_INSENSITIVE_ORDER));
        porTitulo.carregarDataset(dataset);

        System.out.println("\n== Árvore por título ==");
        System.out.println("Nós: " + porTitulo.tamanho() + " | Altura: " + porTitulo.altura());
        porTitulo.imprimir(3);

        Genero jazz = porTitulo.buscar(new Genero(0, 0, 0, "jazz"));
        System.out.println("\nBusca 'jazz': " + jazz);

        System.out.println("\nPrimeiros 10 em ordem alfabética:");
        porTitulo.emOrdem().stream().limit(10).forEach(g -> System.out.println("  " + g));

        // 3) Árvore ordenada por NÚMERO DE FAIXAS (desempate pelo id, pois há valores repetidos)
        ArvoreBinaria<Genero> porFaixas = new ArvoreBinaria<>(
                Comparator.comparingInt(Genero::numFaixas).thenComparingInt(Genero::id));
        porFaixas.carregarDataset(dataset);

        List<Genero> emOrdem = porFaixas.emOrdem();
        System.out.println("\n== Top 5 gêneros com mais faixas ==");
        for (int i = emOrdem.size() - 1; i >= emOrdem.size() - 5; i--) {
            System.out.println("  " + emOrdem.get(i));
        }
    }
}
