package arvores.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.swing.Timer;

/**
 * Executa etapas de animação uma depois da outra, com um {@link Timer} do Swing (tudo roda na
 * thread de eventos). Uma nova sequência termina a anterior na hora, pulando direto para o estado
 * final, então a tela nunca fica "atrasada" em relação aos dados.
 */
final class LinhaDoTempo {

    interface Etapa {

        /** Duração em ms (0 = instantânea). É lida logo depois de {@link #aoIniciar()}. */
        long duracaoMs();

        default void aoIniciar() {
        }

        /** Chamada a cada quadro com t de 0 a 1 (sem suavização). */
        default void aoQuadro(double t) {
        }

        /** Deixa o estado final pronto; também é chamada quando a animação é pulada. */
        default void aoFinalizar() {
        }
    }

    private final Deque<Etapa> fila = new ArrayDeque<>();
    private final Runnable repintar;
    private final Timer relogio;
    private Etapa atual;
    private long inicioNs;
    private long duracaoAtualNs;
    private long efeitosAteNs;

    LinhaDoTempo(Runnable repintar) {
        this.repintar = repintar;
        this.relogio = new Timer(16, e -> tique());
        this.relogio.setCoalesce(true);
    }

    boolean ativa() {
        return atual != null || !fila.isEmpty();
    }

    /** Mantém os quadros rodando por mais um tempo (para efeitos que dependem do relógio). */
    void manterQuadros(long ms) {
        efeitosAteNs = Math.max(efeitosAteNs, System.nanoTime() + ms * 1_000_000L);
        relogio.start();
    }

    /** Termina o que estava rodando e começa estas etapas (ou aplica tudo de uma vez, sem animar). */
    void executar(List<Etapa> etapas, boolean animar) {
        pular();
        fila.addAll(etapas);
        if (!animar) {
            pular();
            return;
        }
        proxima();
        if (ativa()) {
            relogio.start();
        }
    }

    /** Vai direto para o fim de tudo o que estava na fila. */
    void pular() {
        if (atual != null) {
            Etapa e = atual;
            atual = null;
            e.aoFinalizar();
        }
        while (!fila.isEmpty()) {
            Etapa e = fila.poll();
            e.aoIniciar();
            e.aoFinalizar();
        }
        pararSeOcioso();
        repintar.run();
    }

    private void proxima() {
        while (atual == null && !fila.isEmpty()) {
            Etapa e = fila.poll();
            e.aoIniciar();
            long ms = e.duracaoMs();
            if (ms <= 0) {
                e.aoFinalizar();
                continue;
            }
            atual = e;
            inicioNs = System.nanoTime();
            duracaoAtualNs = ms * 1_000_000L;
        }
    }

    private void tique() {
        long agora = System.nanoTime();
        if (atual != null) {
            double t = (double) (agora - inicioNs) / duracaoAtualNs;
            if (t >= 1) {
                Etapa e = atual;
                atual = null;
                e.aoFinalizar();
                proxima();
            } else {
                atual.aoQuadro(t);
            }
        }
        repintar.run();
        pararSeOcioso();
    }

    private void pararSeOcioso() {
        if (atual == null && fila.isEmpty() && System.nanoTime() >= efeitosAteNs) {
            relogio.stop();
        }
    }
}
