package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import arvores.Ordem;
import arvores.TipoArvore;
import arvores.gui.visual.Bolinha;
import arvores.gui.visual.Paleta;

/** Lista as chaves de cada árvore na ordem de um percurso (pré, em ou pós-ordem). */
@SuppressWarnings("serial")
final class PainelPercursos extends JPanel {

    private static final int MAXIMO_MOSTRADO = 600;

    private final JLabel titulo = new JLabel();
    private final EnumMap<TipoArvore, JTextArea> areas = new EnumMap<>(TipoArvore.class);

    PainelPercursos() {
        setLayout(new BorderLayout());
        setOpaque(false);
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        titulo.setFont(Tema.fonte(12.5f, Font.PLAIN));
        titulo.setForeground(Tema.TEXTO_2);
        add(titulo, BorderLayout.NORTH);

        JPanel colunas = new JPanel(new GridLayout(1, 3, 0, 0));
        colunas.setOpaque(false);
        colunas.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.LINHA));
        for (TipoArvore t : TipoArvore.values()) {
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.setForeground(Tema.TEXTO);
            area.setMargin(new Insets(8, 14, 10, 14));
            areas.put(t, area);

            JLabel nome = new JLabel(t.nome(), new Bolinha(Tema.corDaArvore(t), 14), JLabel.LEADING);
            nome.setIconTextGap(7);
            nome.setFont(Tema.fonte(12.5f, Font.BOLD));
            nome.setForeground(Tema.TEXTO);
            nome.setBorder(BorderFactory.createEmptyBorder(9, 14, 3, 14));

            JScrollPane rolagem = new JScrollPane(area);
            rolagem.setBorder(null);
            rolagem.getViewport().setBackground(Paleta.CARTAO);

            JPanel coluna = new JPanel(new BorderLayout());
            coluna.setOpaque(false);
            if (t.ordinal() > 0) {
                coluna.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Tema.LINHA));
            }
            coluna.add(nome, BorderLayout.NORTH);
            coluna.add(rolagem, BorderLayout.CENTER);
            colunas.add(coluna);
        }
        add(colunas, BorderLayout.CENTER);
        limpar();
    }

    void limpar() {
        titulo.setText("Escolha uma ordem no painel da esquerda e clique em Percorrer.");
        for (JTextArea a : areas.values()) {
            a.setText("");
        }
    }

    void mostrar(Ordem ordem, Map<TipoArvore, List<Integer>> listas) {
        String descricao = switch (ordem) {
            case PRE -> "Pré-ordem: raiz, esquerda, direita.";
            case EM -> "Em ordem: esquerda, raiz, direita. As chaves saem em ordem crescente, nas três árvores.";
            case POS -> "Pós-ordem: esquerda, direita, raiz.";
        };
        titulo.setText(descricao + (ordem == Ordem.EM ? "" : " A forma da árvore muda a ordem."));
        for (TipoArvore t : TipoArvore.values()) {
            List<Integer> chaves = listas.get(t);
            StringBuilder sb = new StringBuilder();
            int mostrados = Math.min(chaves.size(), MAXIMO_MOSTRADO);
            for (int i = 0; i < mostrados; i++) {
                sb.append(i == 0 ? "" : ", ").append(chaves.get(i));
            }
            if (chaves.size() > mostrados) {
                sb.append("\n\n… e mais ").append(Formato.inteiro(chaves.size() - mostrados)).append(" chaves (")
                        .append(Formato.inteiro(chaves.size())).append(" no total).");
            } else if (chaves.isEmpty()) {
                sb.append("(árvore vazia)");
            }
            JTextArea area = areas.get(t);
            area.setText(sb.toString());
            area.setCaretPosition(0);
        }
    }
}
