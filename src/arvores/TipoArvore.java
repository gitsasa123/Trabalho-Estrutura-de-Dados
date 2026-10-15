package arvores;

import java.util.function.Supplier;

/**
 * As três árvores do projeto. O {@link #nome()} é o mesmo usado nos CSVs do benchmark, nos
 * gráficos e no README.
 */
public enum TipoArvore {
    BST("BST", "Árvore Binária de Busca", () -> new BST<>()),
    AVL("AVL", "Árvore AVL", () -> new AVL<>()),
    RUBRO_NEGRA("Rubro-Negra", "Árvore Rubro-Negra", () -> new RubroNegra<>());

    private final String nome;
    private final String nomeCompleto;
    private final Supplier<Arvore<Integer, Faixa>> fabrica;

    TipoArvore(String nome, String nomeCompleto, Supplier<Arvore<Integer, Faixa>> fabrica) {
        this.nome = nome;
        this.nomeCompleto = nomeCompleto;
        this.fabrica = fabrica;
    }

    /** Nome curto: "BST", "AVL" ou "Rubro-Negra". */
    public String nome() {
        return nome;
    }

    public String nomeCompleto() {
        return nomeCompleto;
    }

    /** Cria uma árvore vazia desse tipo, com chave inteira (track_id) e {@link Faixa} como dados. */
    public Arvore<Integer, Faixa> criar() {
        return fabrica.get();
    }

    /** Procura pelo {@link #nome()} (usado ao ler os CSVs do benchmark). */
    public static TipoArvore porNome(String nome) {
        for (TipoArvore t : values()) {
            if (t.nome.equals(nome)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Árvore desconhecida: " + nome);
    }

    @Override
    public String toString() {
        return nome;
    }
}
