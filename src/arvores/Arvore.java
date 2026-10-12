package arvores;

import java.util.List;


public interface Arvore<K extends Comparable<K>, V> {

    void inserir(K chave, V dados);

    V buscar(K chave);

    boolean remover(K chave);

    int altura();

    List<K> percorrer(Ordem ordem);

    long contadorComparacoes();

    void zerarContador();

    int tamanho();

   
    boolean validar();
}
