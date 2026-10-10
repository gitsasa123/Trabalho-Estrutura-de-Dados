package arvores;

/** Dados guardados em cada nó: uma faixa do Free Music Archive (a chave é o track_id). */
public record Faixa(String titulo, String artista, String genero, long ouvintes, int duracaoSeg) {
}
