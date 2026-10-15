package arvores;

/** Operações medidas em cada cenário do benchmark. */
public enum Operacao {
    INSERCAO("Inserção"),
    BUSCA_EXISTENTE("Busca (chave existente)"),
    BUSCA_INEXISTENTE("Busca (chave inexistente)"),
    REMOCAO("Remoção (metade das chaves)");

    private final String descricao;

    Operacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
