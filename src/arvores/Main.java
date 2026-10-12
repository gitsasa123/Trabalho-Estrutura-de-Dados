package arvores;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Demonstração ao vivo: carrega faixas do FMA nas três árvores e deixa buscar, inserir, remover
 * e comparar, mostrando quantas comparações cada árvore precisou.
 *
 * Uso:  java -cp out arvores.Main [csv] [N] [ordenado|embaralhado]
 *   csv   (padrão: data/faixas.csv)
 *   N     quantas faixas carregar (padrão: 20000)
 *   modo  ordem de inserção na carga (padrão: embaralhado).
 *         Com "ordenado" a BST vira uma lista: ótimo para mostrar o pior caso (use N pequeno, ex.: 20000).
 */
public final class Main {

    private static final String[] NOMES = {"BST", "AVL", "Rubro-Negra"};

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "data/faixas.csv");
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        boolean embaralhar = !(args.length > 2 && args[2].equalsIgnoreCase("ordenado"));

        System.out.println("Carregando até " + n + " faixas de " + csv + " ("
                + (embaralhar ? "ordem aleatória" : "ordem crescente de track_id") + ") ...");
        List<CarregadorFaixas.Registro> registros = CarregadorFaixas.carregar(csv, n, embaralhar, 2026L);
        if (registros.isEmpty()) {
            System.out.println("Nenhuma faixa carregada. Rode antes scripts/preparar_dados.py.");
            return;
        }

        List<Arvore<Integer, Faixa>> arvores = List.of(new BST<>(), new AVL<>(), new RubroNegra<>());
        for (int i = 0; i < arvores.size(); i++) {
            Arvore<Integer, Faixa> a = arvores.get(i);
            long inicio = System.nanoTime();
            for (CarregadorFaixas.Registro r : registros) {
                a.inserir(r.chave(), r.dados());
            }
            System.out.printf(Locale.ROOT, "  %-12s carregada em %7.1f ms | comparações: %,d%n", NOMES[i],
                    (System.nanoTime() - inicio) / 1e6, a.contadorComparacoes());
            a.zerarContador();
        }
        mostrarAlturas(arvores);

        int exemplo = registros.get(0).chave();
        Scanner entrada = new Scanner(System.in);
        while (true) {
            System.out.println("\n===== MENU =====");
            System.out.println("1) Buscar faixa por track_id   (ex.: " + exemplo + ")");
            System.out.println("2) Inserir faixa");
            System.out.println("3) Remover faixa por track_id");
            System.out.println("4) Mostrar altura e tamanho das 3 árvores");
            System.out.println("5) Percursos (primeiras 10 chaves) de uma árvore");
            System.out.println("6) Validar as 3 árvores (propriedades de BST / AVL / Rubro-Negra)");
            System.out.println("0) Sair");
            System.out.print("> ");
            if (!entrada.hasNextLine()) {
                return;
            }
            String opcao = entrada.nextLine().trim();
            try {
                switch (opcao) {
                    case "1" -> buscar(arvores, lerInt(entrada, "track_id: "));
                    case "2" -> inserir(arvores, entrada);
                    case "3" -> remover(arvores, lerInt(entrada, "track_id: "));
                    case "4" -> mostrarAlturas(arvores);
                    case "5" -> percursos(arvores, entrada);
                    case "6" -> validar(arvores);
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido: digite um número inteiro.");
            }
        }
    }

    private static int lerInt(Scanner entrada, String pergunta) {
        System.out.print(pergunta);
        return Integer.parseInt(entrada.nextLine().trim());
    }

    private static void buscar(List<Arvore<Integer, Faixa>> arvores, int chave) {
        Faixa achada = null;
        for (int i = 0; i < arvores.size(); i++) {
            Arvore<Integer, Faixa> a = arvores.get(i);
            a.zerarContador();
            Faixa f = a.buscar(chave);
            System.out.printf(Locale.ROOT, "  %-12s %s em %,d comparações%n", NOMES[i],
                    f == null ? "não encontrou" : "encontrou   ", a.contadorComparacoes());
            if (f != null) {
                achada = f;
            }
        }
        if (achada != null) {
            System.out.println("  -> " + achada.titulo() + " | " + achada.artista() + " | " + achada.genero()
                    + " | " + achada.ouvintes() + " ouvintes | " + achada.duracaoSeg() + " s");
        }
    }

    private static void inserir(List<Arvore<Integer, Faixa>> arvores, Scanner entrada) {
        int chave = lerInt(entrada, "novo track_id: ");
        System.out.print("título: ");
        String titulo = entrada.nextLine().trim();
        System.out.print("artista: ");
        String artista = entrada.nextLine().trim();
        Faixa f = new Faixa(titulo, artista, "Desconhecido", 0, 0);
        for (int i = 0; i < arvores.size(); i++) {
            Arvore<Integer, Faixa> a = arvores.get(i);
            a.zerarContador();
            a.inserir(chave, f);
            System.out.printf(Locale.ROOT, "  %-12s inserida com %,d comparações (altura agora %d)%n",
                    NOMES[i], a.contadorComparacoes(), a.altura());
        }
    }

    private static void remover(List<Arvore<Integer, Faixa>> arvores, int chave) {
        for (int i = 0; i < arvores.size(); i++) {
            Arvore<Integer, Faixa> a = arvores.get(i);
            a.zerarContador();
            boolean ok = a.remover(chave);
            System.out.printf(Locale.ROOT, "  %-12s %s, %,d comparações (altura agora %d)%n", NOMES[i],
                    ok ? "removeu" : "não achou", a.contadorComparacoes(), a.altura());
        }
    }

    private static void mostrarAlturas(List<Arvore<Integer, Faixa>> arvores) {
        System.out.println("  Árvore       tamanho   altura");
        for (int i = 0; i < arvores.size(); i++) {
            Arvore<Integer, Faixa> a = arvores.get(i);
            System.out.printf(Locale.ROOT, "  %-12s %,7d %,8d%n", NOMES[i], a.tamanho(), a.altura());
        }
    }

    private static void percursos(List<Arvore<Integer, Faixa>> arvores, Scanner entrada) {
        int escolha = lerInt(entrada, "árvore (1=BST, 2=AVL, 3=Rubro-Negra): ");
        if (escolha < 1 || escolha > 3) {
            System.out.println("Escolha 1, 2 ou 3.");
            return;
        }
        Arvore<Integer, Faixa> a = arvores.get(escolha - 1);
        for (Ordem ordem : Ordem.values()) {
            List<Integer> chaves = a.percorrer(ordem);
            System.out.println("  " + ordem + ": " + chaves.subList(0, Math.min(10, chaves.size())) + " ...");
        }
    }

    private static void validar(List<Arvore<Integer, Faixa>> arvores) {
        for (int i = 0; i < arvores.size(); i++) {
            System.out.printf("  %-12s %s%n", NOMES[i], arvores.get(i).validar() ? "válida" : "INVÁLIDA");
        }
    }
}
