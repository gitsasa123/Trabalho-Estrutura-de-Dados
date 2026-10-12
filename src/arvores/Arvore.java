package arvores;

import java.util.List;

/**
 * Interface comum às três árvores (BST, AVL e Rubro-Negra).
 * O benchmark e os testes só conhecem esta interface.
 *
 * Convenções do grupo (valem para as 3 árvores):
 *  - Chave repetida em inserir(): os dados antigos são SUBSTITUÍDOS e o tamanho não muda.
 *  - altura(): número de nós do caminho mais longo da raiz até uma folha
 *    (árvore vazia = 0, um único nó = 1).
 *  - contadorComparacoes(): conta cada comparação entre chaves (compareTo)
 *    feita em inserir, buscar e remover.
 */
public interface Arvore<K extends Comparable<K>, V> {

    /** Insere (chave, dados). Se a chave já existe, substitui os dados. */
    void inserir(K chave, V dados);

    /** Retorna os dados da chave, ou null se a chave não existe. */
    V buscar(K chave);

    /** Remove a chave. Retorna true se ela existia. Nas balanceadas, deve rebalancear. */
    boolean remover(K chave);

    /** Altura da árvore (veja convenção acima). */
    int altura();

    /** Lista de chaves no percurso pedido (PRE, EM ou POS). */
    List<K> percorrer(Ordem ordem);

    /** Total de comparações entre chaves desde a última zerada. */
    long contadorComparacoes();

    /** Zera o contador (use entre as medições). */
    void zerarContador();

    /** Quantidade de chaves armazenadas. */
    int tamanho();

    /**
     * Verifica as propriedades da estrutura (ordem das chaves e, nas balanceadas,
     * fator de balanceamento / regras da rubro-negra). Usado nos testes.
     */
    boolean validar();
}
