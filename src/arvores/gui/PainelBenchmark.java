package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import arvores.Benchmark;
import arvores.Cenario;
import arvores.Operacao;
import arvores.ResultadoBenchmark;
import arvores.ResultadoBenchmark.LinhaDesempenho;
import arvores.TipoArvore;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.Icones;
import arvores.gui.visual.LayoutQuebra;
import arvores.gui.visual.Paleta;
import arvores.gui.visual.Tabela;

/**
 * A aba "Benchmark": roda o benchmark do projeto (ou abre resultados já salvos), mostra os gráficos e as
 * tabelas e exporta os CSVs e as imagens. É o mesmo experimento da linha de comando
 * ({@code java -cp out arvores.Benchmark}), com o andamento na tela.
 */
@SuppressWarnings("serial")
final class PainelBenchmark extends JPanel {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Barra barra;
    private final Supplier<Path> arquivoCsv;

    private final JSpinner campoN = spinner(50_000, 100, 1_000_000, 5000);
    private final JSpinner campoRepeticoes = spinner(5, 1, 30, 1);
    private final JSpinner campoPasso = spinner(1000, 10, 100_000, 100);
    private final JSpinner campoNAltura = spinner(0, 0, 1_000_000, 5000);
    private final JButton botaoRodar = Estilo.primario(new JButton("Rodar o benchmark", Icones.TOCAR.icone(14)));
    private final JButton botaoRapido = new JButton("Teste rápido");
    private final JButton botaoAbrir = new JButton("Abrir resultados salvos", Icones.PASTA.icone(16));
    private final JButton botaoCsv = new JButton("Exportar CSVs", Icones.BAIXAR.icone(16));
    private final JButton botaoImagens = new JButton("Salvar gráficos (PNG)", Icones.BAIXAR.icone(16));
    private final JLabel origem = new JLabel(" ");
    private final JLabel ambiente = new JLabel(" ");

    private final GraficoBarras graficoTempo = new GraficoBarras(
            "Desempenho: tempo de cada operação",
            r -> "N = " + Formato.inteiro(r.nDesempenho())
                    + " faixas do FMA · média de várias repetições (barras de erro = desvio padrão) · escala log",
            "Tempo total da operação (ms), escala log",
            LinhaDesempenho::tempoMsMedio, LinhaDesempenho::tempoMsDesvio,
            Graficos::rotuloTempo, v -> "Tempo médio: " + Formato.tempoMs(v));
    private final GraficoBarras graficoComparacoes = new GraficoBarras(
            "Desempenho: número de comparações entre chaves",
            r -> "N = " + Formato.inteiro(r.nDesempenho()) + " faixas do FMA · total de comparações em cada operação · escala log",
            "Comparações (total), escala log",
            l -> (double) l.comparacoes(), l -> 0,
            Formato::compacto, v -> Formato.inteiro((long) v) + " comparações");
    private final GraficoAltura graficoAltura = new GraficoAltura();
    private final JPanel painelTabelas = new JPanel();
    private final JTextArea registro = new JTextArea();
    private final JTabbedPane abas = new JTabbedPane();

    private final AtomicBoolean cancelar = new AtomicBoolean();
    private ResultadoBenchmark resultado;
    private boolean ocupado;

    PainelBenchmark(Barra barra, Supplier<Path> arquivoCsv) {
        this.barra = barra;
        this.arquivoCsv = arquivoCsv;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(montarTopo(), BorderLayout.NORTH);

        painelTabelas.setLayout(new BoxLayout(painelTabelas, BoxLayout.Y_AXIS));
        painelTabelas.setBackground(Paleta.CARTAO);
        painelTabelas.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        registro.setEditable(false);
        registro.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        registro.setForeground(Tema.TEXTO);
        registro.setMargin(new Insets(12, 16, 12, 16));

        abas.addTab("Tempo", graficoTempo);
        abas.addTab("Comparações", graficoComparacoes);
        abas.addTab("Altura", graficoAltura);
        abas.addTab("Tabelas", new JScrollPane(painelTabelas));
        abas.addTab("Registro da execução", new JScrollPane(registro));
        Estilo.compactas(abas);
        Cartao cartaoAbas = new Cartao(new BorderLayout());
        cartaoAbas.add(abas, BorderLayout.CENTER);
        add(cartaoAbas, BorderLayout.CENTER);

        mostrar(null, "Nenhum resultado carregado. Rode o benchmark ou abra os resultados salvos.");
        carregarResultadosSalvosDoProjeto();
    }

    // ------------------------------------------------------------------ montagem

    private JPanel montarTopo() {
        JPanel parametros = new JPanel(new LayoutQuebra(FlowLayout.LEFT, 18, 6));
        parametros.setOpaque(false);
        parametros.add(campoRotulado("Faixas no desempenho", campoN));
        parametros.add(campoRotulado("Repetições", campoRepeticoes));
        parametros.add(campoRotulado("Altura medida a cada (inserções)", campoPasso));
        parametros.add(campoRotulado("Faixas no experimento de altura (0 = todas)", campoNAltura));
        campoN.setToolTipText("Nº de faixas usadas nas medidas de tempo e comparações");
        campoNAltura.setToolTipText("Nº de faixas no experimento de altura; 0 usa todas as faixas do arquivo");

        JPanel botoes = new JPanel(new LayoutQuebra(FlowLayout.LEFT, 8, 6));
        botoes.setOpaque(false);
        botaoRodar.setToolTipText("Com os valores padrão leva alguns minutos: a BST com chaves em ordem crescente é O(n²)");
        botaoRapido.setToolTipText("10.000 faixas, 2 repetições: termina em poucos segundos");
        botoes.add(botaoRodar);
        botoes.add(botaoRapido);
        botoes.add(Box.createHorizontalStrut(14));
        botoes.add(botaoAbrir);
        botoes.add(botaoCsv);
        botoes.add(botaoImagens);

        origem.setFont(Tema.fonte(12.5f, Font.BOLD));
        origem.setForeground(Tema.TEXTO);
        ambiente.setFont(Tema.fonte(11.5f, Font.PLAIN));
        ambiente.setForeground(Tema.TEXTO_2);
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        textos.add(origem, BorderLayout.NORTH);
        textos.add(ambiente, BorderLayout.CENTER);

        JPanel pilha = new JPanel();
        pilha.setOpaque(false);
        pilha.setLayout(new BoxLayout(pilha, BoxLayout.Y_AXIS));
        for (JPanel p : new JPanel[] {parametros, botoes, textos}) {
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            pilha.add(p);
        }
        Cartao topo = new Cartao(new BorderLayout()).comMargem(14, 16, 12, 16);
        topo.add(pilha, BorderLayout.CENTER);

        botaoRodar.addActionListener(e -> rodar(configuracaoDosCampos()));
        botaoRapido.addActionListener(e -> {
            campoN.setValue(10_000);
            campoRepeticoes.setValue(2);
            campoPasso.setValue(500);
            campoNAltura.setValue(20_000);
            rodar(configuracaoDosCampos());
        });
        botaoAbrir.addActionListener(e -> abrirResultadosSalvos());
        botaoCsv.addActionListener(e -> exportarCsvs());
        botaoImagens.addActionListener(e -> salvarImagens());
        return topo;
    }

    /** Um campo numérico com o nome em cima. */
    private static JPanel campoRotulado(String rotulo, JSpinner campo) {
        JLabel nome = new JLabel(rotulo);
        nome.setFont(Tema.fonte(12f, Font.PLAIN));
        nome.setForeground(Tema.TEXTO_2);
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linha.setOpaque(false);
        linha.add(campo);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(nome, BorderLayout.NORTH);
        p.add(linha, BorderLayout.CENTER);
        return p;
    }

    private static JSpinner spinner(int valor, int minimo, int maximo, int passo) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, minimo, maximo, passo));
        s.setEditor(new JSpinner.NumberEditor(s, "#"));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(7);
        return s;
    }

    private Benchmark.Configuracao configuracaoDosCampos() {
        return new Benchmark.Configuracao(arquivoCsv.get(), (Integer) campoN.getValue(),
                (Integer) campoRepeticoes.getValue(), (Integer) campoPasso.getValue(), (Integer) campoNAltura.getValue());
    }

    // ------------------------------------------------------------------ resultados salvos

    private void carregarResultadosSalvosDoProjeto() {
        Path pasta = Arquivos.localizar("results/tabelas");
        if (Files.isRegularFile(pasta.resolve(ResultadoBenchmark.ARQUIVO_DESEMPENHO))) {
            try {
                mostrar(ResultadoBenchmark.ler(pasta), "Resultados salvos do projeto (" + pasta + ")");
            } catch (IOException e) {
                origem.setText("Não foi possível ler os resultados salvos: " + e.getMessage());
            }
        }
    }

    private void abrirResultadosSalvos() {
        JFileChooser escolhedor = new JFileChooser(Arquivos.localizar("results/tabelas").toFile());
        escolhedor.setDialogTitle("Pasta com desempenho.csv e altura_incremental.csv");
        escolhedor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (escolhedor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path pasta = escolhedor.getSelectedFile().toPath();
        try {
            mostrar(ResultadoBenchmark.ler(pasta), "Resultados salvos (" + pasta + ")");
            barra.mensagem("Resultados abertos de " + pasta);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível abrir os resultados:\n" + e.getMessage(),
                    "Abrir resultados", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------ execução

    private void rodar(Benchmark.Configuracao cfg) {
        if (ocupado) {
            return;
        }
        if (barra.ocupada()) {
            barra.mensagem("Espere a tarefa em andamento terminar: ela atrapalharia as medidas do benchmark.");
            return;
        }
        if (cfg.csv() == null || !Files.isRegularFile(cfg.csv())) {
            JOptionPane.showMessageDialog(this, "Escolha o arquivo CSV das faixas na aba Árvores (seção Dataset).",
                    "Benchmark", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ocupado = true;
        cancelar.set(false);
        definirControlesAtivos(false);
        registro.setText("");
        abas.setSelectedIndex(4);
        barra.iniciarTarefa("Rodando o benchmark...", () -> cancelar.set(true));
        long inicio = System.nanoTime();

        new SwingWorker<ResultadoBenchmark, Object[]>() {
            @Override
            protected ResultadoBenchmark doInBackground() throws Exception {
                return Benchmark.executar(cfg, new Benchmark.Ouvinte() {
                    @Override
                    public void mensagem(String texto) {
                        publish(new Object[] {texto, null});
                    }

                    @Override
                    public void progresso(double fracao) {
                        publish(new Object[] {null, fracao});
                    }

                    @Override
                    public boolean cancelado() {
                        return cancelar.get();
                    }
                });
            }

            @Override
            protected void process(List<Object[]> blocos) {
                for (Object[] b : blocos) {
                    if (b[0] != null) {
                        registro.append(b[0] + "\n");
                        registro.setCaretPosition(registro.getDocument().getLength());
                    }
                    if (b[1] != null) {
                        double f = (Double) b[1];
                        double segundos = (System.nanoTime() - inicio) / 1e9;
                        barra.progresso(f, "Rodando o benchmark... " + (int) (f * 100) + "%  ("
                                + Formato.decimal(segundos, 0) + " s)");
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    ResultadoBenchmark r = get();
                    double segundos = (System.nanoTime() - inicio) / 1e9;
                    mostrar(r, "Resultados desta execução (" + Formato.decimal(segundos, 1) + " s)");
                    registro.append("\nPronto em " + Formato.decimal(segundos, 1) + " s.\n");
                    abas.setSelectedIndex(0);
                    barra.mensagem("Benchmark concluído em " + Formato.decimal(segundos, 1) + " s.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof CancellationException) {
                        registro.append("\nBenchmark cancelado.\n");
                        barra.mensagem("Benchmark cancelado.");
                    } else {
                        registro.append("\nERRO: " + causa + "\n");
                        barra.mensagem("O benchmark falhou: " + causa.getMessage());
                        JOptionPane.showMessageDialog(PainelBenchmark.this, "O benchmark falhou:\n" + causa.getMessage(),
                                "Benchmark", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    ocupado = false;
                    definirControlesAtivos(true);
                    barra.terminarTarefa();
                }
            }
        }.execute();
    }

    void cancelarTudo() {
        cancelar.set(true);
    }

    private void definirControlesAtivos(boolean ativos) {
        for (Component c : new Component[] {campoN, campoRepeticoes, campoPasso, campoNAltura, botaoRodar, botaoRapido,
            botaoAbrir, botaoCsv, botaoImagens}) {
            c.setEnabled(ativos);
        }
    }

    // ------------------------------------------------------------------ mostrar

    private void mostrar(ResultadoBenchmark r, String descricao) {
        this.resultado = r;
        origem.setText(descricao);
        ambiente.setText(r == null ? " " : resumoAmbiente(r.ambiente()));
        ambiente.setToolTipText(r == null || r.ambiente().isEmpty() ? null
                : "<html>" + String.join("<br>", r.ambiente()) + "</html>");
        graficoTempo.definir(r);
        graficoComparacoes.definir(r);
        graficoAltura.definir(r);
        reconstruirTabelas();
        botaoCsv.setEnabled(r != null && !ocupado);
        botaoImagens.setEnabled(r != null && !ocupado);
    }

    private static String resumoAmbiente(List<String> linhas) {
        if (linhas.isEmpty()) {
            return " ";
        }
        StringBuilder sb = new StringBuilder();
        for (String l : linhas) {
            int i = l.indexOf(':');
            if (i < 0) {
                continue;
            }
            String chave = l.substring(0, i).trim();
            String valor = l.substring(i + 1).trim();
            switch (chave) {
                case "data/hora" -> {
                    try {
                        sb.append(DATA_HORA.format(LocalDateTime.parse(valor))).append(" · ");
                    } catch (DateTimeParseException e) {
                        sb.append(valor).append(" · ");
                    }
                }
                case "sistema" -> sb.append(valor).append(" · ");
                case "java" -> sb.append("Java ").append(valor.split(" ")[0]).append(" · ");
                case "processadores logicos" -> sb.append(valor).append(" processadores · ");
                case "repeticoes" -> sb.append(valor).append(" repetições");
                default -> { }
            }
        }
        return sb.toString().replaceAll(" · $", "");
    }

    // ------------------------------------------------------------------ tabelas

    private void reconstruirTabelas() {
        painelTabelas.removeAll();
        if (resultado == null || resultado.desempenho().isEmpty()) {
            JLabel vazio = new JLabel("Sem resultados para mostrar.");
            vazio.setForeground(Tema.TEXTO_3);
            painelTabelas.add(vazio);
        } else {
            for (Cenario cenario : Cenario.values()) {
                painelTabelas.add(titulo(cenario.descricao() + " — N = " + Formato.inteiro(resultado.nDesempenho())));
                painelTabelas.add(tabelaDeDesempenho(cenario));
                painelTabelas.add(Box.createVerticalStrut(18));
            }
            painelTabelas.add(titulo("Altura final (depois de inserir todas as faixas do experimento de altura)"));
            painelTabelas.add(tabelaDeAlturasFinais());
        }
        painelTabelas.revalidate();
        painelTabelas.repaint();
    }

    private static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(13.5f, Font.BOLD));
        l.setForeground(Tema.TEXTO);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JPanel tabelaDeDesempenho(Cenario cenario) {
        String[] colunas = {"Estrutura", "Inserção (ms)", "Busca existente (ms)", "Busca inexistente (ms)",
            "Remoção (ms)", "Comparações (inserção)", "Altura após inserir", "Altura após remover"};
        DefaultTableModel modelo = somenteLeitura(colunas);
        for (TipoArvore t : TipoArvore.values()) {
            modelo.addRow(new Object[] {
                t.nome(),
                tempo(cenario, t, Operacao.INSERCAO), tempo(cenario, t, Operacao.BUSCA_EXISTENTE),
                tempo(cenario, t, Operacao.BUSCA_INEXISTENTE), tempo(cenario, t, Operacao.REMOCAO),
                resultado.linha(cenario, t, Operacao.INSERCAO).map(l -> Formato.inteiro(l.comparacoes())).orElse("-"),
                resultado.linha(cenario, t, Operacao.INSERCAO).map(l -> Formato.inteiro(l.altura())).orElse("-"),
                resultado.linha(cenario, t, Operacao.REMOCAO).map(l -> Formato.inteiro(l.altura())).orElse("-")});
        }
        return envolver(modelo);
    }

    private String tempo(Cenario cenario, TipoArvore tipo, Operacao op) {
        return resultado.linha(cenario, tipo, op).map(l -> Formato.decimal(l.tempoMsMedio(), 1)).orElse("-");
    }

    private JPanel tabelaDeAlturasFinais() {
        String[] colunas = new String[1 + TipoArvore.values().length];
        colunas[0] = "Cenário";
        for (TipoArvore t : TipoArvore.values()) {
            colunas[1 + t.ordinal()] = t.nome();
        }
        DefaultTableModel modelo = somenteLeitura(colunas);
        for (Cenario c : Cenario.values()) {
            Object[] linha = new Object[colunas.length];
            linha[0] = c.descricao();
            for (TipoArvore t : TipoArvore.values()) {
                linha[1 + t.ordinal()] = resultado.alturaFinal(c, t).isPresent()
                        ? Formato.inteiro(resultado.alturaFinal(c, t).getAsInt()) : "-";
            }
            modelo.addRow(linha);
        }
        return envolver(modelo);
    }

    private static DefaultTableModel somenteLeitura(String[] colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    /** Tabela de só leitura com o cabeçalho, números à direita. */
    private static JPanel envolver(DefaultTableModel modelo) {
        Tabela tabela = new Tabela(modelo);
        tabela.setFocusable(false);
        DefaultTableCellRenderer direita = new DefaultTableCellRenderer();
        direita.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 1; c < tabela.getColumnCount(); c++) {
            tabela.getColumnModel().getColumn(c).setCellRenderer(direita);
        }
        tabela.getColumnModel().getColumn(0).setPreferredWidth(220);
        tabela.ajustarLarguraDosTitulos();
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(tabela.getTableHeader(), BorderLayout.NORTH);
        p.add(tabela, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32 + 31 * modelo.getRowCount() + 2));
        return p;
    }

    // ------------------------------------------------------------------ exportar

    private Path escolherPasta(String titulo, String sugestao) {
        JFileChooser escolhedor = new JFileChooser(Arquivos.localizar(sugestao).toFile());
        escolhedor.setDialogTitle(titulo);
        escolhedor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (escolhedor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return escolhedor.getSelectedFile().toPath();
    }

    /** Pergunta antes de sobrescrever (os resultados do projeto estão no Git e não devem ser trocados sem querer). */
    private boolean confirmarSubstituicao(Path pasta, String... nomes) {
        StringBuilder existentes = new StringBuilder();
        for (String nome : nomes) {
            if (Files.exists(pasta.resolve(nome))) {
                existentes.append("\n  • ").append(nome);
            }
        }
        if (existentes.length() == 0) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this, "Estes arquivos já existem em " + pasta + " e serão substituídos:"
                + existentes + "\n\nContinuar?", "Substituir arquivos", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void exportarCsvs() {
        if (resultado == null) {
            return;
        }
        Path pasta = escolherPasta("Pasta onde gravar os CSVs", "results/tabelas");
        if (pasta == null || !confirmarSubstituicao(pasta, ResultadoBenchmark.ARQUIVO_DESEMPENHO,
                ResultadoBenchmark.ARQUIVO_ALTURA, ResultadoBenchmark.ARQUIVO_AMBIENTE)) {
            return;
        }
        try {
            resultado.gravar(pasta);
            barra.mensagem("CSVs gravados em " + pasta + " (use python scripts/gerar_graficos.py para gerar os PNGs do README).");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível gravar:\n" + e.getMessage(), "Exportar CSVs",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void salvarImagens() {
        if (resultado == null) {
            return;
        }
        Path pasta = escolherPasta("Pasta onde gravar os gráficos", "results/graficos");
        if (pasta == null || !confirmarSubstituicao(pasta, "desempenho_tempo.png", "desempenho_comparacoes.png", "altura.png")) {
            return;
        }
        try {
            Files.createDirectories(pasta);
            gravar(pasta.resolve("desempenho_tempo.png"), graficoTempo::desenhar);
            gravar(pasta.resolve("desempenho_comparacoes.png"), graficoComparacoes::desenhar);
            gravar(pasta.resolve("altura.png"), graficoAltura::desenhar);
            barra.mensagem("Gráficos gravados em " + pasta);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível gravar:\n" + e.getMessage(), "Salvar gráficos",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private interface Desenhista {
        void desenhar(Graphics2D g, int largura, int altura);
    }

    private static void gravar(Path arquivo, Desenhista desenhista) throws IOException {
        BufferedImage imagem = new BufferedImage(1500, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            desenhista.desenhar(g, imagem.getWidth(), imagem.getHeight());
        } finally {
            g.dispose();
        }
        ImageIO.write(imagem, "png", new File(arquivo.toString()));
    }
}
