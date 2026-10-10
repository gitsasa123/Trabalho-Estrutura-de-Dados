package arvores;

/** Ordem de percurso da árvore. */
public enum Ordem {
    PRE,  // raiz, esquerda, direita
    EM,   // esquerda, raiz, direita (chaves em ordem crescente)
    POS   // esquerda, direita, raiz
}
