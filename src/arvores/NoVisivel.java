package arvores;

import java.util.OptionalInt;

/**
 * Visão somente leitura de um nó, para quem precisa desenhar ou inspecionar a estrutura
 * (a interface gráfica). Os nós de verdade continuam privados em cada árvore.
 *
 * <p>Os nós são os da própria árvore: depois de uma inserção ou remoção eles podem ter mudado de
 * lugar ou de conteúdo. Quem quiser guardar algo deve copiar (por exemplo, a chave) em vez de
 * guardar o nó.
 */
public interface NoVisivel<K, V> {

    K chave();

    V dados();

    /** Filho esquerdo, ou {@code null} se não existe. */
    NoVisivel<K, V> esquerdo();

    /** Filho direito, ou {@code null} se não existe. */
    NoVisivel<K, V> direito();

    /** Cor do nó (só na Rubro-Negra). */
    default Cor cor() {
        return Cor.NENHUMA;
    }

    /** Fator de balanceamento (altura da esquerda − altura da direita), se a árvore o guarda (AVL). */
    default OptionalInt fatorBalanceamento() {
        return OptionalInt.empty();
    }
}
