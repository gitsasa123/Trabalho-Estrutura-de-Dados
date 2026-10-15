package arvores;

import java.util.List;

/**
 * Contrato comum da BST, da AVL e da Rubro-Negra, para que os mesmos testes e experimentos
 * rodem em qualquer uma delas.
 *
 * <p>Convenções (iguais nas três árvores):
 * <ul>
 *   <li>chave repetida em {@link #inserir}: os dados antigos são substituídos e o tamanho não muda;</li>
 *   <li>altura: nº de nós do caminho mais longo da raiz até uma folha (vazia = 0, um nó = 1);</li>
 *   <li>comparações: cada {@code compareTo} entre chaves feito em inserir, buscar e remover
 *       incrementa o contador.</li>
 * </ul>
 */
public interface Arvore<K extends Comparable<K>, V> {

    void inserir(K chave, V dados);

    /** Devolve os dados da chave, ou {@code null} se ela não existe. */
    V buscar(K chave);

    /** Devolve {@code true} se a chave existia (e foi removida). */
    boolean remover(K chave);

    int altura();

    List<K> percorrer(Ordem ordem);

    long contadorComparacoes();

    void zerarContador();

    int tamanho();

    /** Confere as propriedades da estrutura (ordem das chaves, balanceamento, cores...). */
    boolean validar();

    /** Raiz para desenhar ou inspecionar a árvore (somente leitura), ou {@code null} se está vazia. */
    NoVisivel<K, V> raizVisivel();
}
