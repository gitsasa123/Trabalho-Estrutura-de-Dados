package arvores.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import arvores.TipoArvore;
import arvores.gui.visual.Bolinha;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.LookModerno;
import arvores.gui.visual.Paleta;

/**
 * Janela do programa: abas "Árvores" (as três árvores desenhadas e interativas), "Benchmark" e "Testes",
 * barra de status com o andamento das tarefas demoradas.
 */
@SuppressWarnings("serial")
public final class JanelaPrincipal extends JFrame implements Barra {

    private static final String TITULO = "Árvores de busca: BST, AVL e Rubro-Negra";

    private static final Icon PONTO_LIVRE = new Bolinha(Tema.ACHADO, 14);
    private static final Icon PONTO_OCUPADO = new Bolinha(Tema.VISITA, 14);

    private final JLabel textoStatus = new JLabel("Pronto.", PONTO_LIVRE, JLabel.LEADING);
    private final JProgressBar progresso = new JProgressBar(0, 1000);
    private final JButton botaoCancelar = new JButton("Cancelar");
    private final JTabbedPane abas = new JTabbedPane();
    private final PainelArvores painelArvores;
    private final PainelBenchmark painelBenchmark;
    private Runnable aoCancelar;
    private boolean tarefaAtiva;

    private JanelaPrincipal() {
        super(TITULO);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        painelArvores = new PainelArvores(this);
        painelBenchmark = new PainelBenchmark(this, () -> painelArvores.controles().arquivoCsv());

        abas.addTab("Árvores", painelArvores);
        abas.addTab("Benchmark", painelBenchmark);
        abas.addTab("Testes", new PainelTestes(this));

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.add(abas, BorderLayout.CENTER);
        conteudo.add(montarBarraDeStatus(), BorderLayout.SOUTH);
        setContentPane(conteudo);
        setJMenuBar(montarMenu());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }
        });
        Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setIconImages(List.of(icone(32), icone(64)));
        setSize(Math.min(1440, tela.width - 40), Math.min(1000, tela.height - 40));
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
    }

    /** Ícone do programa: uma raiz e dois filhos, nas cores das três árvores. */
    private static Image icone(int tamanho) {
        BufferedImage img = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double t = tamanho;
        double r = t * 0.17;
        double[][] centros = {{t * 0.5, t * 0.22}, {t * 0.24, t * 0.74}, {t * 0.76, t * 0.74}};
        g.setColor(Tema.TEXTO_3);
        g.setStroke(new BasicStroke((float) (t * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(centros[0][0], centros[0][1], centros[1][0], centros[1][1]));
        g.draw(new Line2D.Double(centros[0][0], centros[0][1], centros[2][0], centros[2][1]));
        TipoArvore[] tipos = {TipoArvore.RUBRO_NEGRA, TipoArvore.BST, TipoArvore.AVL};
        for (int i = 0; i < 3; i++) {
            g.setColor(Tema.corDaArvore(tipos[i]));
            g.fill(new Ellipse2D.Double(centros[i][0] - r, centros[i][1] - r, 2 * r, 2 * r));
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------ abrir o programa

    /**
     * Abre a janela. Argumentos opcionais, como no programa antigo: {@code [csv] [quantidade] [ordenado|embaralhado]}.
     * Com argumentos, já carrega as faixas.
     */
    public static void abrir(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configurarAparencia();
            JanelaPrincipal janela = new JanelaPrincipal();
            Path csvPadrao = (args.length > 0) ? Path.of(args[0]) : localizarCsvPadrao();
            janela.painelArvores.controles().definirArquivoCsv(csvPadrao);
            janela.setVisible(true);
            janela.painelArvores.ajustarDivisor();
            if (args.length > 0) {
                int quantidade = 20_000;
                if (args.length > 1) {
                    try {
                        quantidade = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(janela, "Quantidade inválida: " + args[1], "Argumentos",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
                boolean embaralhar = !(args.length > 2 && args[2].equalsIgnoreCase("ordenado"));
                janela.painelArvores.carregarAoAbrir(csvPadrao, quantidade, embaralhar);
            }
        });
    }

    /** O CSV padrão: data/faixas.csv, procurado na pasta atual e perto do programa. */
    static Path localizarCsvPadrao() {
        return Arquivos.localizar("data/faixas.csv");
    }

    private static void configurarAparencia() {
        LookModerno.instalar();
        ToolTipManager.sharedInstance().setInitialDelay(300);
        ToolTipManager.sharedInstance().setDismissDelay(15_000);
    }

    // ------------------------------------------------------------------ menu e barra de status

    private JMenuBar montarMenu() {
        JMenuBar barraMenu = new JMenuBar();

        JMenu arquivo = new JMenu("Arquivo");
        arquivo.setMnemonic(KeyEvent.VK_A);
        arquivo.setDisplayedMnemonicIndex(-1);   // Alt+letra continua valendo, mas sem o sublinhado antiquado
        JMenuItem abrir = new JMenuItem("Carregar dataset (CSV)...");
        abrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        abrir.addActionListener(e -> {
            abas.setSelectedComponent(painelArvores);
            painelArvores.controles().escolherECarregar();
        });
        JMenuItem sair = new JMenuItem("Sair");
        sair.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        sair.addActionListener(e -> sair());
        arquivo.add(abrir);
        arquivo.addSeparator();
        arquivo.add(sair);

        JMenu arvores = new JMenu("Árvores");
        arvores.setMnemonic(KeyEvent.VK_V);
        arvores.setDisplayedMnemonicIndex(-1);
        JMenuItem esvaziar = new JMenuItem("Esvaziar as três árvores");
        esvaziar.addActionListener(e -> painelArvores.esvaziar());
        JMenuItem validar = new JMenuItem("Validar as três árvores");
        validar.addActionListener(e -> painelArvores.validar());
        arvores.add(esvaziar);
        arvores.add(validar);

        JMenu ajuda = new JMenu("Ajuda");
        ajuda.setMnemonic(KeyEvent.VK_U);
        ajuda.setDisplayedMnemonicIndex(-1);
        JMenuItem comoUsar = new JMenuItem("Como usar");
        comoUsar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        comoUsar.addActionListener(e -> mostrarAjuda());
        JMenuItem sobre = new JMenuItem("Sobre");
        sobre.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "<html><b>" + TITULO + "</b><br>Projeto de Estrutura de Dados (IFC São Bento do Sul).<br><br>"
                        + "Compara uma árvore binária de busca, uma AVL e uma Rubro-Negra<br>"
                        + "com faixas reais do Free Music Archive.</html>", "Sobre", JOptionPane.INFORMATION_MESSAGE));
        ajuda.add(comoUsar);
        ajuda.add(sobre);

        barraMenu.add(arquivo);
        barraMenu.add(arvores);
        barraMenu.add(ajuda);
        return barraMenu;
    }

    private void mostrarAjuda() {
        JOptionPane.showMessageDialog(this, "<html><body style='width:430px'>"
                + "<h3>Como usar</h3>"
                + "<b>Operações:</b> digite uma chave e use <i>Inserir</i>, <i>Buscar</i> ou <i>Remover</i>. "
                + "A operação é aplicada às <b>três árvores ao mesmo tempo</b> e o histórico mostra quantas "
                + "comparações cada uma fez (em verde, a que fez menos).<br><br>"
                + "<b>Demonstrações:</b> insira 1 a 31 em ordem crescente para ver a BST virar uma lista enquanto "
                + "a AVL e a Rubro-Negra se rearranjam (rotações e mudanças de cor).<br><br>"
                + "<b>Dataset:</b> carregue as faixas do FMA nas três árvores (milhares de chaves). Árvores grandes "
                + "são resumidas: cada triângulo guarda uma subárvore escondida; clique nele para abri-la.<br><br>"
                + "<b>Desenho:</b> arraste para mover, use a rodinha do mouse para dar zoom, duplo clique no fundo "
                + "para enquadrar, clique num nó para ver a faixa, botão direito para mais opções.<br><br>"
                + "<b>Benchmark e Testes:</b> nas outras abas, o benchmark do projeto e os testes automáticos."
                + "</body></html>", "Como usar", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel montarBarraDeStatus() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(Paleta.CARTAO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Paleta.BORDA),
                BorderFactory.createEmptyBorder(6, 16, 6, 12)));
        textoStatus.setFont(Tema.fonte(12.5f, Font.PLAIN));
        textoStatus.setForeground(Tema.TEXTO_2);
        textoStatus.setIconTextGap(9);
        p.add(textoStatus, BorderLayout.CENTER);

        JPanel direita = new JPanel(new BorderLayout(10, 0));
        direita.setOpaque(false);
        progresso.setPreferredSize(new Dimension(220, 8));
        progresso.setVisible(false);
        Estilo.texto(botaoCancelar);
        botaoCancelar.setVisible(false);
        botaoCancelar.setMargin(new Insets(3, 12, 3, 12));
        botaoCancelar.addActionListener(e -> {
            if (aoCancelar != null) {
                aoCancelar.run();
                textoStatus.setText("Cancelando...");
            }
        });
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(progresso);
        direita.add(centro, BorderLayout.CENTER);
        direita.add(botaoCancelar, BorderLayout.EAST);
        p.add(direita, BorderLayout.EAST);
        return p;
    }

    private void sair() {
        painelArvores.cancelarTudo();
        painelBenchmark.cancelarTudo();
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ Barra

    @Override
    public void mensagem(String texto) {
        textoStatus.setText(texto);
    }

    @Override
    public void iniciarTarefa(String texto, Runnable cancelar) {
        tarefaAtiva = true;
        textoStatus.setIcon(PONTO_OCUPADO);
        textoStatus.setText(texto);
        aoCancelar = cancelar;
        progresso.setIndeterminate(true);
        progresso.setVisible(true);
        botaoCancelar.setVisible(cancelar != null);
    }

    @Override
    public boolean ocupada() {
        return tarefaAtiva;
    }

    @Override
    public void progresso(double fracao, String texto) {
        if (fracao < 0) {
            progresso.setIndeterminate(true);
        } else {
            progresso.setIndeterminate(false);
            progresso.setValue((int) Math.round(Math.min(1, fracao) * 1000));
        }
        textoStatus.setText(texto);
    }

    @Override
    public void terminarTarefa() {
        tarefaAtiva = false;
        textoStatus.setIcon(PONTO_LIVRE);
        progresso.setVisible(false);
        botaoCancelar.setVisible(false);
        aoCancelar = null;
    }
}
