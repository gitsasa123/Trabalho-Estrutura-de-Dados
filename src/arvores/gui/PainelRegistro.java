package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import arvores.TipoArvore;
import arvores.gui.visual.Paleta;
import arvores.gui.visual.Tabela;

/**
 * Histórico das operações: uma linha por operação, com o que cada árvore fez (comparações e altura).
 * A árvore que fez menos comparações fica em destaque, que é o ponto da comparação entre elas.
 */
@SuppressWarnings("serial")
final class PainelRegistro extends JPanel {

    /** Uma linha do histórico; {@code comparacoes} (pode ser null) serve para destacar a menor. */
    record Linha(int numero, String operacao, String resultado, String[] celulas, long[] comparacoes) {
    }

    private static final class Modelo extends AbstractTableModel {
        private final List<Linha> linhas = new ArrayList<>();

        @Override
        public int getRowCount() {
            return linhas.size();
        }

        @Override
        public int getColumnCount() {
            return 3 + TipoArvore.values().length;
        }

        @Override
        public String getColumnName(int coluna) {
            return switch (coluna) {
                case 0 -> "#";
                case 1 -> "Operação";
                case 2 -> "Resultado";
                default -> TipoArvore.values()[coluna - 3].nome();
            };
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            Linha l = linhas.get(linha);
            return switch (coluna) {
                case 0 -> l.numero();
                case 1 -> l.operacao();
                case 2 -> l.resultado();
                default -> l.celulas()[coluna - 3];
            };
        }

        void adicionar(Linha l) {
            linhas.add(l);
            fireTableRowsInserted(linhas.size() - 1, linhas.size() - 1);
        }

        void limpar() {
            linhas.clear();
            fireTableDataChanged();
        }
    }

    private final Modelo modelo = new Modelo();
    private final Tabela tabela = new Tabela(modelo);
    private int contador;

    PainelRegistro() {
        setLayout(new BorderLayout());
        setOpaque(false);
        tabela.definirMensagemVazia("As operações aparecem aqui, com as comparações feitas por cada árvore.");
        tabela.getColumnModel().getColumn(0).setPreferredWidth(36);
        tabela.getColumnModel().getColumn(0).setMaxWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(290);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(180);
        for (int c = 3; c < modelo.getColumnCount(); c++) {
            tabela.getColumnModel().getColumn(c).setPreferredWidth(230);
        }
        tabela.setDefaultRenderer(Object.class, new Renderizador());

        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getViewport().setBackground(Paleta.CARTAO);
        add(rolagem, BorderLayout.CENTER);
    }

    void adicionar(String operacao, String resultado, String[] celulas, long[] comparacoes) {
        contador++;
        modelo.adicionar(new Linha(contador, operacao, resultado, celulas, comparacoes));
        int ultima = modelo.getRowCount() - 1;
        SwingUtilities.invokeLater(() -> tabela.scrollRectToVisible(tabela.getCellRect(ultima, 0, true)));
    }

    void limpar() {
        contador = 0;
        modelo.limpar();
    }

    private final class Renderizador extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionada, boolean foco,
                                                       int linha, int coluna) {
            super.getTableCellRendererComponent(t, valor, selecionada, false, linha, coluna);
            setHorizontalAlignment(SwingConstants.LEFT);
            setToolTipText(valor == null ? null : valor.toString());
            setFont(Tema.fonte(13f, Font.PLAIN));
            setForeground(coluna == 0 ? Tema.TEXTO_3 : Tema.TEXTO);
            if (coluna >= 3) {
                Linha l = modelo.linhas.get(linha);
                long[] c = l.comparacoes();
                if (c != null && menor(c, coluna - 3)) {
                    setFont(Tema.fonte(13f, Font.BOLD));
                    setForeground(Paleta.SUCESSO);
                }
            }
            return this;
        }

        /** A árvore {@code indice} fez menos comparações que alguma outra (e ninguém fez menos que ela). */
        private boolean menor(long[] c, int indice) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (long v : c) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            return min < max && c[indice] == min;
        }
    }
}
