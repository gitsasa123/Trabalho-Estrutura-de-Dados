package arvores.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import arvores.CarregadorFaixas;
import arvores.Faixa;
import arvores.gui.visual.LayoutQuebra;
import arvores.gui.visual.Paleta;
import arvores.gui.visual.Tabela;

/**
 * As faixas lidas do CSV, para escolher uma chave existente: duplo clique numa linha busca a chave
 * nas árvores. O filtro procura no título, no artista, no gênero e na chave.
 */
@SuppressWarnings("serial")
final class PainelFaixas extends JPanel {

    private static final String[] COLUNAS = {"track_id", "Título", "Artista", "Gênero", "Ouvintes", "Duração"};

    private final class Modelo extends AbstractTableModel {
        private List<CarregadorFaixas.Registro> visiveis = List.of();

        @Override
        public int getRowCount() {
            return visiveis.size();
        }

        @Override
        public int getColumnCount() {
            return COLUNAS.length;
        }

        @Override
        public String getColumnName(int c) {
            return COLUNAS[c];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return (c == 0 || c == 4) ? Long.class : (c == 5 ? Integer.class : String.class);
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            CarregadorFaixas.Registro r = visiveis.get(linha);
            Faixa f = r.dados();
            return switch (coluna) {
                case 0 -> (long) r.chave();
                case 1 -> f.titulo();
                case 2 -> f.artista();
                case 3 -> f.genero();
                case 4 -> f.ouvintes();
                default -> f.duracaoSeg();
            };
        }

        void definir(List<CarregadorFaixas.Registro> novas) {
            visiveis = novas;
            fireTableDataChanged();
        }
    }

    private final Modelo modelo = new Modelo();
    private final Tabela tabela = new Tabela(modelo);
    private final JTextField filtro = new JTextField(22);
    private final JLabel contagem = new JLabel(" ");
    private final Timer atrasoFiltro;
    private List<CarregadorFaixas.Registro> todas = List.of();

    PainelFaixas(IntConsumer aoEscolherChave) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel topo = new JPanel(new LayoutQuebra(FlowLayout.LEFT, 10, 4));
        topo.setOpaque(false);
        topo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel rotuloFiltro = new JLabel("Filtrar");
        rotuloFiltro.setFont(Tema.fonte(12f, Font.PLAIN));
        rotuloFiltro.setForeground(Tema.TEXTO_2);
        topo.add(rotuloFiltro);
        filtro.setToolTipText("Procura no título, artista, gênero e na chave");
        topo.add(filtro);
        contagem.setFont(Tema.fonte(12f, Font.PLAIN));
        contagem.setForeground(Tema.TEXTO_3);
        topo.add(contagem);
        JLabel dica = new JLabel("Duplo clique numa faixa para buscar a chave nas árvores.");
        dica.setFont(Tema.fonte(12f, Font.PLAIN));
        dica.setForeground(Tema.TEXTO_3);
        topo.add(dica);
        add(topo, BorderLayout.NORTH);

        tabela.setAutoCreateRowSorter(true);
        tabela.definirMensagemVazia("Carregue o dataset para ver as faixas aqui.");
        tabela.setDefaultRenderer(Long.class, numeros());
        tabela.setDefaultRenderer(Integer.class, duracao());
        int[] larguras = {80, 280, 200, 120, 90, 70};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int linha = tabela.getSelectedRow();
                    if (linha >= 0) {
                        long chave = (Long) tabela.getValueAt(linha, 0);
                        aoEscolherChave.accept((int) chave);
                    }
                }
            }
        });
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.LINHA));
        rolagem.getViewport().setBackground(Paleta.CARTAO);
        add(rolagem, BorderLayout.CENTER);

        atrasoFiltro = new Timer(250, e -> aplicarFiltro());
        atrasoFiltro.setRepeats(false);
        filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                atrasoFiltro.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atrasoFiltro.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atrasoFiltro.restart();
            }
        });
    }

    void definirFaixas(List<CarregadorFaixas.Registro> registros) {
        this.todas = registros;
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String texto = filtro.getText().trim().toLowerCase(Locale.ROOT);
        if (texto.isEmpty()) {
            modelo.definir(todas);
        } else {
            List<CarregadorFaixas.Registro> achadas = new ArrayList<>();
            for (CarregadorFaixas.Registro r : todas) {
                Faixa f = r.dados();
                if (Integer.toString(r.chave()).contains(texto)
                        || f.titulo().toLowerCase(Locale.ROOT).contains(texto)
                        || f.artista().toLowerCase(Locale.ROOT).contains(texto)
                        || f.genero().toLowerCase(Locale.ROOT).contains(texto)) {
                    achadas.add(r);
                }
            }
            modelo.definir(achadas);
        }
        contagem.setText(todas.isEmpty() ? "(nenhum dataset carregado)"
                : Formato.inteiro(modelo.getRowCount()) + " de " + Formato.inteiro(todas.size()) + " faixas");
    }

    private static DefaultTableCellRenderer numeros() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object valor) {
                setText(valor == null ? "" : Formato.inteiro((Long) valor));
            }
        };
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        return r;
    }

    private static DefaultTableCellRenderer duracao() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object valor) {
                setText(valor == null ? "" : Formato.duracao((Integer) valor));
            }
        };
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        return r;
    }
}
