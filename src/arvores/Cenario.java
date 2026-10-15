package arvores;

/** Ordem em que as chaves são inseridas nos experimentos (as mesmas para as três árvores). */
public enum Cenario {
    ORDENADO("Inserção em ordem crescente de track_id (pior caso da BST)"),
    EMBARALHADO("Inserção em ordem aleatória (caso médio)");

    private final String descricao;

    Cenario(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
