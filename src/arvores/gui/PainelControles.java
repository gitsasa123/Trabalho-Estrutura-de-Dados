package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import arvores.Faixa;
import arvores.Ordem;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.Icones;
import arvores.gui.visual.Paleta;

/**
 * Os controles à esquerda: operações por chave, demonstrações, carga do dataset, percursos e opções
 * de exibição. Só valida o que foi digitado e repassa para {@link Acoes}; quem decide o que fazer
 * é o {@link PainelArvores}.
 */
@SuppressWarnings("serial")
final class PainelControles extends JPanel {

    enum Ordenacao {
        CRESCENTE("crescente"),
        DECRESCENTE("decrescente"),
        EMBARALHADA("embaralhada");

        final String rotulo;

        Ordenacao(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    enum Disposicao {
        COLUNAS("Lado a lado"),
        LINHAS("Empilhadas"),
        SO_BST("Só a BST"),
        SO_AVL("Só a AVL"),
        SO_RUBRO_NEGRA("Só a Rubro-Negra");

        final String rotulo;

        Disposicao(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    /** Opções de exibição escolhidas pelo usuário. */
    record Exibicao(Disposicao disposicao, boolean animar, double velocidade, boolean fatores, int niveis,
                    int porNivel) {
    }

    interface Acoes {

        void inserir(int chave, Faixa faixa);

        void buscar(int chave);

        void remover(int chave);

        void inserirSequencia(int de, int ate, Ordenacao ordem);

        void inserirAleatorias(int quantidade, int maximo);

        void esvaziar();

        void validar();

        void percorrer(Ordem ordem);

        /** {@code quantidade <= 0} = todas as faixas do arquivo. */
        void carregarDataset(Path csv, int quantidade, boolean embaralhar);

        void sortearChave();

        void exibicaoMudou();
    }

    private static final int MAXIMO_SEQUENCIA = 5000;

    private final Acoes acoes;

    private final JTextField campoChave = new JTextField(8);
    private final JTextField campoTitulo = new JTextField();
    private final JTextField campoArtista = new JTextField();
    private final JTextField campoGenero = new JTextField();
    private final JTextField campoOuvintes = new JTextField();
    private final JTextField campoDuracao = new JTextField();
    private final JTextArea mensagem = areaDeTexto(1);
    private final JButton botaoInserir = new JButton("Inserir");
    private final JButton botaoBuscar = new JButton("Buscar");
    private final JButton botaoRemover = new JButton("Remover");
    private Runnable ultimaAcao = this::pedirInserir;

    private final JSpinner sequenciaDe = spinner(1, -1_000_000, 1_000_000, 1);
    private final JSpinner sequenciaAte = spinner(15, -1_000_000, 1_000_000, 1);
    private final JComboBox<Ordenacao> sequenciaOrdem = new JComboBox<>(Ordenacao.values());
    private final JSpinner aleatoriasQuantidade = spinner(15, 1, MAXIMO_SEQUENCIA, 1);
    private final JSpinner aleatoriasMaximo = spinner(99, 1, 1_000_000, 1);

    private final JTextArea rotuloArquivo = areaDeTexto(1);
    private Path arquivoCsv;
    private final JSpinner quantidadeFaixas = spinner(20_000, 10, 1_000_000, 1000);
    private final JCheckBox todasAsFaixas = new JCheckBox("todas");
    private final JComboBox<String> ordemCarga = new JComboBox<>(new String[] {
        "embaralhada (caso médio)", "crescente (pior caso da BST)"});
    private final JButton botaoCarregar = new JButton("Carregar nas três árvores");
    private final JTextArea statusDataset = areaDeTexto(1);

    private final JComboBox<String> ordemPercurso = new JComboBox<>(new String[] {
        "Pré-ordem (raiz, esq., dir.)", "Em ordem (esq., raiz, dir.)", "Pós-ordem (esq., dir., raiz)"});

    private final JComboBox<Disposicao> disposicao = new JComboBox<>(Disposicao.values());
    private final JCheckBox animar = new JCheckBox("Animar as operações", true);
    private final JSlider velocidade = new JSlider(1, 8, 2);
    private final JCheckBox fatores = new JCheckBox("Mostrar fator de balanceamento (AVL)", true);
    private final JSpinner niveis = spinner(PedidoDesenho.NIVEIS_PADRAO, 2, 60, 1);
    private final JSpinner porNivel = spinner(PedidoDesenho.POR_NIVEL_PADRAO, 4, 256, 4);

    private final JButton[] botoesDeAcao;

    PainelControles(Acoes acoes) {
        this.acoes = acoes;
        setLayout(new BorderLayout());
        setBackground(Tema.FUNDO_PAINEL);

        ColunaRolavel coluna = new ColunaRolavel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setBackground(Tema.FUNDO_PAINEL);
        coluna.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));

        JButton botaoSortear = new JButton(Icones.DADO.icone(18));
        JButton botaoSequencia = new JButton("Inserir sequência", Icones.LISTA.icone(16));
        JButton botaoAleatorias = new JButton("Inserir aleatórias", Icones.EMBARALHAR.icone(16));
        JButton botaoEsvaziar = new JButton("Esvaziar as árvores", Icones.LIMPAR.icone(16));
        JButton botaoPercorrer = new JButton("Percorrer", Icones.ARVORE.icone(16));
        JButton botaoValidar = new JButton("Validar", Icones.CHECK_CIRCULO.icone(16));
        JButton botaoArquivo = new JButton("Escolher", Icones.PASTA.icone(16));
        botaoCarregar.setIcon(Icones.BAIXAR.icone(16));
        Estilo.primario(botaoCarregar);

        coluna.add(secaoOperacoes(botaoSortear));
        coluna.add(Box.createVerticalStrut(8));
        coluna.add(secaoDemonstracoes(botaoSequencia, botaoAleatorias, botaoEsvaziar));
        coluna.add(Box.createVerticalStrut(8));
        coluna.add(secaoDataset(botaoArquivo));
        coluna.add(Box.createVerticalStrut(8));
        coluna.add(secaoPercursos(botaoPercorrer, botaoValidar));
        coluna.add(Box.createVerticalStrut(8));
        coluna.add(secaoExibicao());
        coluna.add(Box.createVerticalGlue());

        JScrollPane rolagem = new JScrollPane(coluna, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(null);
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        rolagem.getViewport().setBackground(Tema.FUNDO_PAINEL);
        add(rolagem, BorderLayout.CENTER);
        setPreferredSize(new Dimension(350, 600));

        botoesDeAcao = new JButton[] {botaoInserir, botaoBuscar, botaoRemover, botaoSortear, botaoSequencia,
            botaoAleatorias, botaoEsvaziar, botaoCarregar, botaoPercorrer, botaoValidar, botaoArquivo};

        ligarEventos(botaoSortear, botaoSequencia, botaoAleatorias, botaoEsvaziar, botaoPercorrer, botaoValidar,
                botaoArquivo);
    }

    // ------------------------------------------------------------------ montagem

    private JPanel secaoOperacoes(JButton botaoSortear) {
        JPanel p = formulario();
        ((AbstractDocument) campoChave.getDocument()).setDocumentFilter(new FiltroInteiro(true));
        ((AbstractDocument) campoOuvintes.getDocument()).setDocumentFilter(new FiltroInteiro(false));
        ((AbstractDocument) campoDuracao.getDocument()).setDocumentFilter(new FiltroInteiro(false));
        campoChave.setToolTipText("track_id: um número inteiro (a chave das árvores)");
        campoChave.setFont(campoChave.getFont().deriveFont(Font.BOLD, 15f));
        campoChave.setMinimumSize(new Dimension(50, campoChave.getPreferredSize().height));
        // ao entrar no campo, seleciona tudo: digitar uma chave nova substitui a antiga
        campoChave.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(campoChave::selectAll);
            }
        });
        // o destaque vermelho do erro some assim que a pessoa volta a digitar
        campoChave.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                Estilo.erro(campoChave, false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                Estilo.erro(campoChave, false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                Estilo.erro(campoChave, false);
            }
        });

        linha(p, 0, "Chave (track_id)", comBotao(campoChave, botaoSortear));
        botaoSortear.setToolTipText("Sortear: preenche a chave com uma faixa que existe nas árvores");
        botaoSortear.setPreferredSize(new Dimension(38, 38));
        botaoSortear.setMargin(new Insets(0, 0, 0, 0));

        JPanel botoes = new JPanel(new GridLayout(1, 3, 6, 0));
        botoes.setOpaque(false);
        botaoInserir.setMnemonic(KeyEvent.VK_I);
        botaoBuscar.setMnemonic(KeyEvent.VK_B);
        botaoRemover.setMnemonic(KeyEvent.VK_R);
        botaoInserir.setToolTipText("Insere a chave nas três árvores (Alt+I)");
        botaoBuscar.setToolTipText("Busca a chave nas três árvores e mostra o caminho (Alt+B)");
        botaoRemover.setToolTipText("Remove a chave das três árvores (Alt+R)");
        botaoInserir.setIcon(Icones.MAIS.icone(15));
        botaoBuscar.setIcon(Icones.LUPA.icone(15));
        botaoRemover.setIcon(Icones.LIXEIRA.icone(15));
        Estilo.primario(botaoInserir);
        Estilo.perigo(botaoRemover);
        for (JButton b : new JButton[] {botaoInserir, botaoBuscar, botaoRemover}) {
            b.setMargin(new Insets(8, 6, 8, 6));
            b.setIconTextGap(6);
            botoes.add(b);
        }
        GridBagConstraints c = restricao(0, 1, 2);
        c.insets = new Insets(10, 0, 2, 0);
        p.add(botoes, c);

        GridBagConstraints cm = restricao(0, 2, 2);
        cm.insets = new Insets(4, 0, 2, 0);
        p.add(mensagem, cm);

        JPanel dados = formulario();
        linha(dados, 0, "Título", campoTitulo);
        linha(dados, 1, "Artista", campoArtista);
        linha(dados, 2, "Gênero", campoGenero);
        linha(dados, 3, "Ouvintes", campoOuvintes);
        linha(dados, 4, "Duração (s)", campoDuracao);
        GridBagConstraints co = restricao(0, 3, 2);
        co.insets = new Insets(4, 0, 0, 0);
        p.add(new Secao("Dados da faixa (opcional)", dados, false, true), co);

        return new Secao("Operações", p, true, false);
    }

    private JPanel secaoDemonstracoes(JButton botaoSequencia, JButton botaoAleatorias, JButton botaoEsvaziar) {
        JPanel p = formulario();
        linha(p, 0, "Sequência", doisValores(sequenciaDe, "a", sequenciaAte));
        linha(p, 1, "em ordem", sequenciaOrdem);
        GridBagConstraints c = restricao(0, 2, 2);
        c.insets = new Insets(6, 0, 8, 0);
        p.add(botaoSequencia, c);
        botaoSequencia.setToolTipText("Insere de, de+1, ..., até nas três árvores. Em ordem crescente a BST vira uma lista.");

        linha(p, 3, "Aleatórias", doisValores(aleatoriasQuantidade, "de 1 a", aleatoriasMaximo));
        GridBagConstraints c2 = restricao(0, 4, 2);
        c2.insets = new Insets(6, 0, 8, 0);
        p.add(botaoAleatorias, c2);
        botaoAleatorias.setToolTipText("Insere N chaves sorteadas, todas diferentes (o caso médio da BST)");

        p.add(botaoEsvaziar, restricao(0, 5, 2));
        return new Secao("Demonstrações", p, true, false);
    }

    private JPanel secaoDataset(JButton botaoArquivo) {
        JPanel p = formulario();
        JPanel arquivo = new JPanel(new BorderLayout(6, 0));
        arquivo.setOpaque(false);
        JPanel nomeDoArquivo = new JPanel(new GridBagLayout());
        nomeDoArquivo.setOpaque(false);
        nomeDoArquivo.add(rotuloArquivo, restricao(0, 0, 1));
        arquivo.add(nomeDoArquivo, BorderLayout.CENTER);
        arquivo.add(botaoArquivo, BorderLayout.EAST);
        GridBagConstraints c1 = restricao(0, 0, 2);
        c1.insets = new Insets(0, 0, 4, 0);
        p.add(arquivo, c1);

        JPanel quantidade = new JPanel(new BorderLayout(8, 0));
        quantidade.setOpaque(false);
        quantidade.add(quantidadeFaixas, BorderLayout.CENTER);
        quantidade.add(todasAsFaixas, BorderLayout.EAST);
        todasAsFaixas.setOpaque(false);
        todasAsFaixas.addActionListener(e -> quantidadeFaixas.setEnabled(!todasAsFaixas.isSelected()));
        linha(p, 1, "Faixas", quantidade);
        linha(p, 2, "Ordem", ordemCarga);
        GridBagConstraints c4 = restricao(0, 3, 2);
        c4.insets = new Insets(8, 0, 0, 0);
        p.add(botaoCarregar, c4);
        botaoCarregar.setToolTipText("Substitui o conteúdo das três árvores pelas faixas do arquivo");
        GridBagConstraints c5 = restricao(0, 4, 2);
        c5.insets = new Insets(4, 0, 0, 0);
        p.add(statusDataset, c5);
        return new Secao("Dataset (Free Music Archive)", p, true, false);
    }

    private JPanel secaoPercursos(JButton botaoPercorrer, JButton botaoValidar) {
        JPanel p = formulario();
        ordemPercurso.setSelectedIndex(1);
        linha(p, 0, "Ordem", ordemPercurso);
        JPanel botoes = new JPanel(new GridLayout(1, 2, 6, 0));
        botoes.setOpaque(false);
        botoes.add(botaoPercorrer);
        botoes.add(botaoValidar);
        GridBagConstraints c = restricao(0, 1, 2);
        c.insets = new Insets(8, 0, 0, 0);
        p.add(botoes, c);
        botaoPercorrer.setToolTipText("Numera os nós na ordem do percurso e lista as chaves na aba Percursos");
        botaoValidar.setToolTipText("Validar as três árvores: confere as propriedades de cada uma (ordem das chaves, balanceamento, cores)");
        return new Secao("Percursos e verificação", p, true, false);
    }

    private JPanel secaoExibicao() {
        JPanel p = formulario();
        linha(p, 0, "Disposição", disposicao);
        animar.setOpaque(false);
        fatores.setOpaque(false);
        p.add(animar, restricao(0, 1, 2));
        velocidade.setOpaque(false);
        velocidade.setMajorTickSpacing(1);
        velocidade.setSnapToTicks(true);
        velocidade.setToolTipText("Velocidade das animações");
        linha(p, 2, "Velocidade", velocidade);
        p.add(fatores, restricao(0, 3, 2));
        linha(p, 4, "Máx. de níveis", niveis);
        linha(p, 5, "Máx. por nível", porNivel);
        niveis.setToolTipText("Árvores grandes são resumidas: níveis abaixo deste viram triângulos");
        porNivel.setToolTipText("Se um nível tiver mais nós que isto, ele e os de baixo viram triângulos");
        return new Secao("Exibição", p, false, false);
    }

    // ------------------------------------------------------------------ eventos

    private void ligarEventos(JButton botaoSortear, JButton botaoSequencia, JButton botaoAleatorias,
                              JButton botaoEsvaziar, JButton botaoPercorrer, JButton botaoValidar,
                              JButton botaoArquivo) {
        botaoInserir.addActionListener(e -> {
            ultimaAcao = this::pedirInserir;
            pedirInserir();
        });
        botaoBuscar.addActionListener(e -> {
            ultimaAcao = this::pedirBuscar;
            pedirBuscar();
        });
        botaoRemover.addActionListener(e -> {
            ultimaAcao = this::pedirRemover;
            pedirRemover();
        });
        campoChave.addActionListener(e -> ultimaAcao.run());
        botaoSortear.addActionListener(e -> acoes.sortearChave());

        botaoSequencia.addActionListener(e -> {
            int de = (Integer) sequenciaDe.getValue();
            int ate = (Integer) sequenciaAte.getValue();
            if (Math.abs((long) ate - de) >= MAXIMO_SEQUENCIA) {
                mostrarMensagem("A sequência pode ter no máximo " + Formato.inteiro(MAXIMO_SEQUENCIA) + " chaves.", true);
                return;
            }
            acoes.inserirSequencia(de, ate, (Ordenacao) sequenciaOrdem.getSelectedItem());
        });
        botaoAleatorias.addActionListener(e -> acoes.inserirAleatorias(
                (Integer) aleatoriasQuantidade.getValue(), (Integer) aleatoriasMaximo.getValue()));
        botaoEsvaziar.addActionListener(e -> acoes.esvaziar());
        botaoPercorrer.addActionListener(e -> acoes.percorrer(
                switch (ordemPercurso.getSelectedIndex()) {
                    case 0 -> Ordem.PRE;
                    case 2 -> Ordem.POS;
                    default -> Ordem.EM;
                }));
        botaoValidar.addActionListener(e -> acoes.validar());

        botaoArquivo.addActionListener(e -> escolherArquivo());
        botaoCarregar.addActionListener(e -> {
            if (arquivoCsv == null || !Files.isRegularFile(arquivoCsv)) {
                mostrarMensagem("Escolha o arquivo CSV das faixas (data/faixas.csv).", true);
                escolherArquivo();
                return;
            }
            acoes.carregarDataset(arquivoCsv, todasAsFaixas.isSelected() ? 0 : (Integer) quantidadeFaixas.getValue(),
                    ordemCarga.getSelectedIndex() == 0);
        });

        Runnable mudou = acoes::exibicaoMudou;
        disposicao.addActionListener(e -> mudou.run());
        animar.addActionListener(e -> mudou.run());
        fatores.addActionListener(e -> mudou.run());
        velocidade.addChangeListener(e -> {
            if (!velocidade.getValueIsAdjusting()) {
                mudou.run();
            }
        });
        niveis.addChangeListener(e -> mudou.run());
        porNivel.addChangeListener(e -> mudou.run());
    }

    private void pedirInserir() {
        Integer chave = lerChave();
        if (chave == null) {
            return;
        }
        String titulo = campoTitulo.getText().trim();
        String artista = campoArtista.getText().trim();
        String genero = campoGenero.getText().trim();
        long ouvintes = campoOuvintes.getText().isBlank() ? 0 : numero(campoOuvintes.getText());
        int duracao = (int) Math.min(Integer.MAX_VALUE, campoDuracao.getText().isBlank() ? 0 : numero(campoDuracao.getText()));
        Faixa faixa = new Faixa(titulo.isEmpty() ? "Faixa " + chave : titulo,
                artista.isEmpty() ? "Artista desconhecido" : artista,
                genero.isEmpty() ? "Desconhecido" : genero, ouvintes, duracao);
        acoes.inserir(chave, faixa);
    }

    private void pedirBuscar() {
        Integer chave = lerChave();
        if (chave != null) {
            acoes.buscar(chave);
        }
    }

    private void pedirRemover() {
        Integer chave = lerChave();
        if (chave != null) {
            acoes.remover(chave);
        }
    }

    private static long numero(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /** Lê a chave do campo; se não for um inteiro válido, avisa ao lado do campo e devolve null. */
    private Integer lerChave() {
        String texto = campoChave.getText().trim();
        try {
            int chave = Integer.parseInt(texto);
            Estilo.erro(campoChave, false);
            return chave;
        } catch (NumberFormatException e) {
            Estilo.erro(campoChave, true);
            mostrarMensagem(texto.isEmpty() ? "Digite uma chave (número inteiro)."
                    : "\"" + texto + "\" não é um número inteiro válido.", true);
            campoChave.requestFocusInWindow();
            return null;
        }
    }

    /** Mostra o seletor de arquivos; devolve false se a pessoa cancelou. */
    private boolean escolherArquivo() {
        File pasta = (arquivoCsv != null && arquivoCsv.toAbsolutePath().getParent() != null)
                ? arquivoCsv.toAbsolutePath().getParent().toFile() : new File(".");
        JFileChooser escolhedor = new JFileChooser(pasta);
        escolhedor.setDialogTitle("Escolher o CSV das faixas");
        escolhedor.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));
        if (escolhedor.showOpenDialog(SwingUtilities.getWindowAncestor(this)) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        definirArquivoCsv(escolhedor.getSelectedFile().toPath());
        return true;
    }

    // ------------------------------------------------------------------ interface com o painel principal

    void definirArquivoCsv(Path caminho) {
        this.arquivoCsv = caminho;
        String nome = (caminho == null) ? "(nenhum arquivo escolhido)" : caminho.toString();
        // só a pasta e o nome do arquivo (o caminho inteiro fica na dica)
        String curto = nome;
        if (caminho != null && caminho.getFileName() != null) {
            Path pasta = caminho.toAbsolutePath().getParent();
            curto = (pasta != null && pasta.getFileName() != null ? pasta.getFileName() + "/" : "") + caminho.getFileName();
        }
        rotuloArquivo.setText(curto);
        rotuloArquivo.setToolTipText(nome);
    }

    Path arquivoCsv() {
        return arquivoCsv;
    }

    void definirChave(int chave) {
        campoChave.setText(Integer.toString(chave));
        Estilo.erro(campoChave, false);
    }

    void mostrarMensagem(String texto, boolean erro) {
        ajustarLinhas(mensagem, texto);
        mensagem.setText(texto);
        mensagem.setToolTipText(texto.isBlank() ? null : texto);
        mensagem.setForeground(erro ? Paleta.PERIGO : Tema.TEXTO_2);
    }

    void definirStatusDataset(String texto) {
        ajustarLinhas(statusDataset, texto.trim());
        statusDataset.setText(texto.trim());
        statusDataset.setToolTipText(texto.isBlank() ? null : texto);
    }

    /** Desliga os botões de ação enquanto uma tarefa demorada (carga do dataset) roda. */
    void definirOcupado(boolean ocupado) {
        for (JButton b : botoesDeAcao) {
            b.setEnabled(!ocupado);
        }
        campoChave.setEnabled(!ocupado);
        if (!ocupado && campoChave.isShowing()) {
            // o botão que estava com o foco foi desligado; o foco volta para onde se digita a próxima chave
            campoChave.requestFocusInWindow();
        }
    }

    Exibicao exibicao() {
        return new Exibicao((Disposicao) disposicao.getSelectedItem(), animar.isSelected(),
                velocidade.getValue() / 2.0, fatores.isSelected(), (Integer) niveis.getValue(),
                (Integer) porNivel.getValue());
    }

    void definirDisposicao(Disposicao d) {
        disposicao.setSelectedItem(d);
    }

    /** Preenche a quantidade e a ordem da carga (0 faixas = todas). */
    void definirParametrosCarga(int quantidade, boolean embaralhar) {
        todasAsFaixas.setSelected(quantidade <= 0);
        quantidadeFaixas.setEnabled(quantidade > 0);
        if (quantidade > 0) {
            quantidadeFaixas.setValue(Math.max(10, quantidade));
        }
        ordemCarga.setSelectedIndex(embaralhar ? 0 : 1);
    }

    /** Escolhe um CSV e já carrega nas três árvores (menu Arquivo). */
    void escolherECarregar() {
        if (escolherArquivo() && arquivoCsv != null && Files.isRegularFile(arquivoCsv)) {
            botaoCarregar.doClick();
        }
    }

    // ------------------------------------------------------------------ componentes auxiliares

    /**
     * Reserva linhas para o texto (a área quebra sozinha, mas só calcula a altura certa depois de ter largura):
     * uma linha cabe uns 44 caracteres.
     */
    private static void ajustarLinhas(JTextArea area, String texto) {
        int linhas = Math.max(1, (texto.length() + 43) / 44);
        if (area.getRows() != linhas) {
            area.setRows(linhas);
            area.revalidate();
        }
    }

    /** Texto de poucas linhas que quebra sozinho e nunca aumenta a largura do painel. */
    private static JTextArea areaDeTexto(int linhas) {
        JTextArea a = new JTextArea(linhas, 12);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(null);
        a.setFont(Tema.fonte(11.5f, Font.PLAIN));
        a.setForeground(Tema.TEXTO_2);
        return a;
    }

    private static JSpinner spinner(int valor, int minimo, int maximo, int passo) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, minimo, maximo, passo));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(s, "#");
        s.setEditor(editor);
        editor.getTextField().setColumns(4);
        return s;
    }

    /** "[ valor ] texto [ valor ]" numa linha só, repartindo a largura. */
    private static JPanel doisValores(JSpinner a, String texto, JSpinner b) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(a, c);
        JLabel t = new JLabel(texto);
        t.setForeground(Tema.TEXTO_2);
        t.setFont(Tema.fonte(12f, Font.PLAIN));
        GridBagConstraints ct = new GridBagConstraints();
        ct.insets = new Insets(0, 6, 0, 6);
        p.add(t, ct);
        p.add(b, c);
        return p;
    }

    private static JPanel formulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    private static GridBagConstraints restricao(int x, int y, int largura) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = largura;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static void linha(JPanel p, int y, String rotulo, Component campo) {
        JLabel l = new JLabel(rotulo);
        l.setForeground(Tema.TEXTO_2);
        l.setFont(Tema.fonte(12f, Font.PLAIN));
        GridBagConstraints cl = new GridBagConstraints();
        cl.gridx = 0;
        cl.gridy = y;
        cl.anchor = GridBagConstraints.WEST;
        cl.insets = new Insets(1, 0, 1, 8);
        p.add(l, cl);
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 1;
        cc.gridy = y;
        cc.weightx = 1;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.anchor = GridBagConstraints.WEST;
        cc.insets = new Insets(1, 0, 1, 0);
        p.add(campo, cc);
    }

    private static JPanel comBotao(JTextField campo, JButton botao) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.add(campo, BorderLayout.CENTER);
        p.add(botao, BorderLayout.EAST);
        return p;
    }

    /** Só deixa digitar números inteiros (com sinal se {@code comSinal}). */
    private static final class FiltroInteiro extends DocumentFilter {
        private final boolean comSinal;

        FiltroInteiro(boolean comSinal) {
            this.comSinal = comSinal;
        }

        private boolean valido(String texto) {
            return texto.matches(comSinal ? "-?\\d{0,10}" : "\\d{0,18}");
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet a) throws BadLocationException {
            replace(fb, offset, 0, texto, a);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int tamanho, String texto, AttributeSet a)
                throws BadLocationException {
            String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String novo = atual.substring(0, offset) + (texto == null ? "" : texto) + atual.substring(offset + tamanho);
            if (valido(novo)) {
                super.replace(fb, offset, tamanho, texto, a);
            }
        }
    }

    /** Coluna de seções que ocupa exatamente a largura da área visível (só rola na vertical). */
    private static final class ColunaRolavel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visivel, int orientacao, int direcao) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visivel, int orientacao, int direcao) {
            return Math.max(40, visivel.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Bloco com título que abre e fecha ao clicar: um cartão branco, ou só o título quando "discreta". */
    private static final class Secao extends Cartao {
        private final JPanel conteudo;
        private final JButton cabecalho;
        private final String titulo;
        private boolean aberta;

        Secao(String titulo, JPanel conteudo, boolean aberta, boolean discreta) {
            super(new BorderLayout());
            this.titulo = titulo;
            this.conteudo = conteudo;
            this.aberta = aberta;
            if (discreta) {
                transparente();
            }
            setBorder(BorderFactory.createEmptyBorder(discreta ? 0 : 1, 0, discreta ? 0 : 1, 0));

            cabecalho = Estilo.texto(new JButton(titulo));
            cabecalho.setHorizontalAlignment(SwingConstants.LEFT);
            cabecalho.setFont(Tema.fonte(discreta ? 12f : 13f, Font.BOLD));
            cabecalho.setForeground(discreta ? Tema.TEXTO_2 : Tema.TEXTO);
            cabecalho.setMargin(new Insets(discreta ? 3 : 6, 8, discreta ? 3 : 6, 8));
            cabecalho.setIconTextGap(7);
            cabecalho.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cabecalho.addActionListener(e -> alternar());
            JPanel moldura = new JPanel(new BorderLayout());
            moldura.setOpaque(false);
            moldura.setBorder(BorderFactory.createEmptyBorder(discreta ? 0 : 5, discreta ? 0 : 6, 0, discreta ? 0 : 6));
            moldura.add(cabecalho, BorderLayout.CENTER);
            add(moldura, BorderLayout.NORTH);

            conteudo.setBorder(BorderFactory.createEmptyBorder(discreta ? 2 : 3, discreta ? 8 : 14, discreta ? 4 : 12,
                    discreta ? 0 : 14));
            conteudo.setOpaque(false);
            add(conteudo, BorderLayout.CENTER);
            atualizar();
        }

        private void alternar() {
            aberta = !aberta;
            atualizar();
            revalidate();
            repaint();
        }

        private void atualizar() {
            conteudo.setVisible(aberta);
            cabecalho.setIcon((aberta ? Icones.CHEVRON_BAIXO : Icones.CHEVRON_DIREITA).icone(14, Tema.TEXTO_3));
            cabecalho.setToolTipText(aberta ? "Recolher \"" + titulo + "\"" : "Expandir \"" + titulo + "\"");
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
