package arvores.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import arvores.Arvore;
import arvores.CarregadorFaixas;
import arvores.Faixa;
import arvores.Ordem;
import arvores.TipoArvore;

/**
 * Os dados do programa: as três árvores (sempre com as mesmas chaves) e as operações aplicadas às
 * três de uma vez, no mesmo estilo do menu antigo: cada árvore informa quantas comparações fez.
 * Não depende de Swing.
 */
final class ModeloArvores {

    enum TipoOperacao {
        INSERIR("Inserir"),
        BUSCAR("Buscar"),
        REMOVER("Remover");

        final String rotulo;

        TipoOperacao(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    /**
     * O que uma árvore fez numa operação.
     *
     * @param sucesso       inserir: a chave era nova (false = substituiu os dados); buscar: achou;
     *                      remover: a chave existia
     * @param dados         buscar: os dados achados (ou null)
     * @param caminhoAntes  caminho da busca até a chave, antes da operação
     * @param caminhoDepois caminho até a chave depois de inserir (vazio nos outros casos)
     */
    record ResultadoArvore(TipoArvore tipo, boolean sucesso, Faixa dados, long comparacoes, int tamanho,
                           int altura, Caminho caminhoAntes, Caminho caminhoDepois) {
    }

    record ResultadoOperacao(TipoOperacao operacao, int chave, List<ResultadoArvore> arvores) {

        ResultadoArvore de(TipoArvore tipo) {
            return arvores.get(tipo.ordinal());
        }

        /** Dados da faixa achada por qualquer das árvores (null se nenhuma achou). */
        Faixa dados() {
            for (ResultadoArvore r : arvores) {
                if (r.dados() != null) {
                    return r.dados();
                }
            }
            return null;
        }
    }

    private final EnumMap<TipoArvore, Arvore<Integer, Faixa>> arvores = new EnumMap<>(TipoArvore.class);
    private List<CarregadorFaixas.Registro> dataset = List.of();

    ModeloArvores() {
        esvaziar();
    }

    Arvore<Integer, Faixa> arvore(TipoArvore tipo) {
        return arvores.get(tipo);
    }

    /** Faixas lidas do último CSV carregado (na ordem em que foram inseridas). */
    List<CarregadorFaixas.Registro> dataset() {
        return dataset;
    }

    int tamanho() {
        return arvores.get(TipoArvore.BST).tamanho();
    }

    void esvaziar() {
        for (TipoArvore t : TipoArvore.values()) {
            arvores.put(t, t.criar());
        }
        dataset = List.of();
    }

    void substituir(Map<TipoArvore, Arvore<Integer, Faixa>> novas, List<CarregadorFaixas.Registro> registros) {
        for (TipoArvore t : TipoArvore.values()) {
            arvores.put(t, novas.get(t));
        }
        dataset = registros;
    }

    ResultadoOperacao inserir(int chave, Faixa faixa) {
        List<ResultadoArvore> resultados = new ArrayList<>();
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = arvores.get(t);
            Caminho antes = Caminho.ate(a.raizVisivel(), chave);
            a.zerarContador();
            a.inserir(chave, faixa);
            long comparacoes = a.contadorComparacoes();
            Caminho depois = Caminho.ate(a.raizVisivel(), chave);
            resultados.add(new ResultadoArvore(t, !antes.encontrou(), null, comparacoes, a.tamanho(), a.altura(),
                    antes, depois));
        }
        return new ResultadoOperacao(TipoOperacao.INSERIR, chave, resultados);
    }

    /**
     * Insere nas três árvores só somando as comparações em {@code comparacoes} (uma posição por árvore).
     * Não calcula caminhos nem alturas, que custam O(n) cada: para muitas chaves seria lento demais.
     */
    void inserirSemDetalhes(int chave, Faixa faixa, long[] comparacoes) {
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = arvores.get(t);
            a.zerarContador();
            a.inserir(chave, faixa);
            comparacoes[t.ordinal()] += a.contadorComparacoes();
        }
    }

    ResultadoOperacao buscar(int chave) {
        List<ResultadoArvore> resultados = new ArrayList<>();
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = arvores.get(t);
            Caminho caminho = Caminho.ate(a.raizVisivel(), chave);
            a.zerarContador();
            Faixa dados = a.buscar(chave);
            resultados.add(new ResultadoArvore(t, dados != null, dados, a.contadorComparacoes(), a.tamanho(),
                    a.altura(), caminho, Caminho.VAZIO));
        }
        return new ResultadoOperacao(TipoOperacao.BUSCAR, chave, resultados);
    }

    ResultadoOperacao remover(int chave) {
        List<ResultadoArvore> resultados = new ArrayList<>();
        for (TipoArvore t : TipoArvore.values()) {
            Arvore<Integer, Faixa> a = arvores.get(t);
            Caminho antes = Caminho.ate(a.raizVisivel(), chave);
            a.zerarContador();
            boolean removeu = a.remover(chave);
            resultados.add(new ResultadoArvore(t, removeu, null, a.contadorComparacoes(), a.tamanho(), a.altura(),
                    antes, Caminho.VAZIO));
        }
        return new ResultadoOperacao(TipoOperacao.REMOVER, chave, resultados);
    }

    Map<TipoArvore, Boolean> validar() {
        EnumMap<TipoArvore, Boolean> resultado = new EnumMap<>(TipoArvore.class);
        for (TipoArvore t : TipoArvore.values()) {
            resultado.put(t, arvores.get(t).validar());
        }
        return resultado;
    }

    Map<TipoArvore, List<Integer>> percorrer(Ordem ordem) {
        EnumMap<TipoArvore, List<Integer>> resultado = new EnumMap<>(TipoArvore.class);
        for (TipoArvore t : TipoArvore.values()) {
            resultado.put(t, arvores.get(t).percorrer(ordem));
        }
        return resultado;
    }
}
