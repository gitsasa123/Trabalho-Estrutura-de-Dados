package arvores.gui.visual;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Tabela no visual do programa: linhas finas, cabeçalho discreto, seleção suave. Quando está vazia,
 * mostra uma mensagem no lugar das linhas.
 */
@SuppressWarnings("serial")
public final class Tabela extends JTable {

    private String mensagemVazia;

    public Tabela(TableModel modelo) {
        super(modelo);
        setFillsViewportHeight(true);
        setShowGrid(false);
        setShowHorizontalLines(true);
        setGridColor(Paleta.BORDA);
        setIntercellSpacing(new Dimension(0, 1));
        setRowHeight(30);
        setBackground(Paleta.CARTAO);
        setForeground(Paleta.TEXTO);
        setSelectionBackground(Paleta.SELECAO);
        setSelectionForeground(Paleta.TEXTO);
        setFocusable(true);
        JTableHeader cabecalho = getTableHeader();
        cabecalho.setReorderingAllowed(false);
        cabecalho.setDefaultRenderer(new Cabecalho());
        cabecalho.setPreferredSize(new Dimension(10, 32));
        cabecalho.setBackground(Paleta.CARTAO);
    }

    /** Garante que cada coluna seja larga o bastante para o seu título aparecer inteiro. */
    public void ajustarLarguraDosTitulos() {
        JTableHeader cabecalho = getTableHeader();
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn coluna = getColumnModel().getColumn(i);
            Component titulo = cabecalho.getDefaultRenderer()
                    .getTableCellRendererComponent(this, coluna.getHeaderValue(), false, false, -1, i);
            coluna.setPreferredWidth(Math.max(coluna.getPreferredWidth(), titulo.getPreferredSize().width + 6));
        }
    }

    /** Texto mostrado no meio da tabela enquanto ela não tem nenhuma linha. */
    public void definirMensagemVazia(String texto) {
        this.mensagemVazia = texto;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (getRowCount() == 0 && mensagemVazia != null) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                Paleta.qualidade(g);
                Rectangle area = getVisibleRect();
                g.setFont(Paleta.fonte(13f, Font.PLAIN));
                g.setColor(Paleta.TEXTO_3);
                FontMetrics fm = g.getFontMetrics();
                int x = area.x + (area.width - fm.stringWidth(mensagemVazia)) / 2;
                int y = area.y + (area.height + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(mensagemVazia, Math.max(area.x + 8, x), y);
            } finally {
                g.dispose();
            }
        }
    }

    /** Cabeçalho: texto pequeno e esmaecido, com uma linha embaixo. */
    private static final class Cabecalho extends DefaultTableCellRenderer {

        Cabecalho() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                       boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, false, false, linha, coluna);
            setFont(Paleta.fonte(12f, Font.BOLD));
            setForeground(Paleta.TEXTO_2);
            setBackground(Paleta.CARTAO);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Paleta.BORDA_FORTE),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            // colunas de números (alinhadas à direita nas células) têm o título à direita também
            TableCellRenderer celula = tabela.getColumnModel().getColumn(coluna).getCellRenderer();
            if (celula == null) {
                celula = tabela.getDefaultRenderer(tabela.getColumnClass(coluna));
            }
            boolean aDireita = celula instanceof JLabel rotulo && rotulo.getHorizontalAlignment() == SwingConstants.RIGHT;
            setHorizontalAlignment(aDireita ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setHorizontalTextPosition(SwingConstants.LEADING);
            setIconTextGap(4);
            setIcon(setinha(tabela, coluna));
            return this;
        }
    }

    /** Pequena seta ao lado do título da coluna pela qual a tabela está ordenada. */
    private static Icon setinha(JTable tabela, int coluna) {
        RowSorter<?> ordenador = tabela.getRowSorter();
        if (ordenador == null) {
            return null;
        }
        List<? extends RowSorter.SortKey> chaves = ordenador.getSortKeys();
        if (chaves.isEmpty() || chaves.get(0).getColumn() != tabela.convertColumnIndexToModel(coluna)) {
            return null;
        }
        SortOrder ordem = chaves.get(0).getSortOrder();
        if (ordem == SortOrder.UNSORTED) {
            return null;
        }
        return (ordem == SortOrder.ASCENDING ? Icones.CHEVRON_CIMA : Icones.CHEVRON_BAIXO).icone(12, Paleta.TEXTO_2);
    }
}
