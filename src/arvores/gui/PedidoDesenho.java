package arvores.gui;

import java.util.List;

/**
 * O que desenhar de uma árvore.
 *
 * @param maxNiveis     máximo de níveis mostrados de cima para baixo (o resto vira marcador)
 * @param maxPorNivel   se um nível tiver mais nós que isto, ele e os de baixo viram marcadores
 * @param chaveFoco     se não for {@code null}, desenha só a subárvore dessa chave
 * @param caminho       chaves de um caminho a mostrar inteiro, mesmo abaixo do limite de níveis
 * @param chaveFantasma se não for {@code null}, mostra um nó vazio onde essa chave ficaria
 *                      (no fim do caminho)
 */
record PedidoDesenho(int maxNiveis, int maxPorNivel, Integer chaveFoco, List<Integer> caminho,
                     Integer chaveFantasma) {

    static final int NIVEIS_PADRAO = 30;
    static final int POR_NIVEL_PADRAO = 32;

    PedidoDesenho {
        caminho = (caminho == null) ? List.of() : caminho;
    }

    static PedidoDesenho padrao() {
        return new PedidoDesenho(NIVEIS_PADRAO, POR_NIVEL_PADRAO, null, List.of(), null);
    }

    PedidoDesenho comLimites(int niveis, int porNivel) {
        return new PedidoDesenho(niveis, porNivel, chaveFoco, caminho, chaveFantasma);
    }

    PedidoDesenho comFoco(Integer foco) {
        return new PedidoDesenho(maxNiveis, maxPorNivel, foco, caminho, chaveFantasma);
    }

    PedidoDesenho comCaminho(List<Integer> novoCaminho, Integer fantasma) {
        return new PedidoDesenho(maxNiveis, maxPorNivel, chaveFoco, novoCaminho, fantasma);
    }

    PedidoDesenho semCaminho() {
        return comCaminho(List.of(), null);
    }
}
