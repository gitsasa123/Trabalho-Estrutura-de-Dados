package arvores.gui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Posiciona os nós de um desenho no estilo de Reingold–Tilford: cada subárvore é desenhada o mais
 * junto possível das vizinhas (sem nunca encostar) e o pai fica centralizado sobre os filhos.
 * Um nó com um filho só o desloca meia unidade para o lado, para dar para ver se ele é o esquerdo
 * ou o direito (assim uma BST em ordem crescente vira uma escadinha).
 */
final class LayoutArvore {

    /** Distância mínima entre os centros de dois nós vizinhos do mesmo nível. */
    static final double SEPARACAO = 1.0;

    private LayoutArvore() {
    }

    /** Menor e maior x de cada nível de uma subárvore, relativos à raiz dela. */
    private record Contorno(double[] min, double[] max) {
        int niveis() {
            return min.length;
        }
    }

    /** Preenche x e y de todos os nós (raiz em x = 0) e devolve {xMin, xMax, yMax}. */
    static double[] posicionar(NoDesenho raiz) {
        if (raiz == null) {
            return new double[] {0, 0, 0};
        }
        calcular(raiz);

        double xMin = 0;
        double xMax = 0;
        double yMax = 0;
        raiz.x = 0;
        raiz.y = 0;
        // desce pelos nós sem recursão: a pilha guarda os pais já posicionados
        Deque<NoDesenho> pilha = new ArrayDeque<>();
        pilha.push(raiz);
        while (!pilha.isEmpty()) {
            NoDesenho pai = pilha.pop();
            xMin = Math.min(xMin, pai.x);
            xMax = Math.max(xMax, pai.x);
            yMax = Math.max(yMax, pai.y);
            for (NoDesenho filho : new NoDesenho[] {pai.esq, pai.dir}) {
                if (filho != null) {
                    filho.x = pai.x + filho.deslocamento;
                    filho.y = pai.y + 1;
                    pilha.push(filho);
                }
            }
        }
        return new double[] {xMin, xMax, yMax};
    }

    /** Calcula o deslocamento de cada filho em relação ao pai; devolve o contorno da subárvore. */
    private static Contorno calcular(NoDesenho n) {
        if (n.esq == null && n.dir == null) {
            return new Contorno(new double[] {0}, new double[] {0});
        }
        if (n.esq == null || n.dir == null) {
            NoDesenho filho = (n.esq != null) ? n.esq : n.dir;
            Contorno c = calcular(filho);
            // elisão: linha reta; senão, meia unidade para o lado do filho
            double dx = (n.tipo == NoDesenho.Tipo.ELISAO) ? 0 : (filho == n.esq ? -SEPARACAO / 2 : SEPARACAO / 2);
            filho.deslocamento = dx;
            return juntar(c, dx, null, 0);
        }

        Contorno ce = calcular(n.esq);
        Contorno cd = calcular(n.dir);
        double distancia = SEPARACAO;
        int comuns = Math.min(ce.niveis(), cd.niveis());
        for (int nivel = 0; nivel < comuns; nivel++) {
            distancia = Math.max(distancia, ce.max()[nivel] - cd.min()[nivel] + SEPARACAO);
        }
        n.esq.deslocamento = -distancia / 2;
        n.dir.deslocamento = distancia / 2;
        return juntar(ce, -distancia / 2, cd, distancia / 2);
    }

    /** Contorno do pai a partir do(s) contorno(s) do(s) filho(s) já deslocados. */
    private static Contorno juntar(Contorno a, double dxA, Contorno b, double dxB) {
        int niveis = 1 + Math.max(a.niveis(), (b == null) ? 0 : b.niveis());
        double[] min = new double[niveis];
        double[] max = new double[niveis];
        min[0] = 0;
        max[0] = 0;
        for (int i = 1; i < niveis; i++) {
            double menor = Double.POSITIVE_INFINITY;
            double maior = Double.NEGATIVE_INFINITY;
            if (i - 1 < a.niveis()) {
                menor = Math.min(menor, a.min()[i - 1] + dxA);
                maior = Math.max(maior, a.max()[i - 1] + dxA);
            }
            if (b != null && i - 1 < b.niveis()) {
                menor = Math.min(menor, b.min()[i - 1] + dxB);
                maior = Math.max(maior, b.max()[i - 1] + dxB);
            }
            min[i] = menor;
            max[i] = maior;
        }
        return new Contorno(min, max);
    }
}
