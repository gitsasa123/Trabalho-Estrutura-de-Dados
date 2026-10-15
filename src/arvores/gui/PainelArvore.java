package arvores.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import arvores.TipoArvore;
import arvores.gui.visual.Bolinha;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.Icones;
import arvores.gui.visual.LayoutQuebra;
import arvores.gui.visual.Paleta;

/** Uma árvore na tela: título, números (tamanho, altura, última operação), o desenho e uma legenda. */
@SuppressWarnings("serial")
final class PainelArvore extends Cartao {

    /** Etiqueta com cantos arredondados. */
    private static final class Chip extends JLabel {
        private Color fundo = Paleta.HOVER;

        Chip() {
            setOpaque(false);
            setFont(Tema.fonte(11.5f, Font.PLAIN));
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        }

        void definir(String texto, Color fundo, Color cor, String dica) {
            this.fundo = fundo;
            setText(texto);
            setForeground(cor);
            setToolTipText(dica);
            setVisible(texto != null && !texto.isEmpty());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fundo);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private final TipoArvore tipo;
    private final VisaoArvore visao;
    private final VisaoArvore.Ouvinte ouvinte;
    private final Chip chipTamanho = new Chip();
    private final Chip chipAltura = new Chip();
    private final Chip chipOperacao = new Chip();
    private final Chip chipValidade = new Chip();
    private final JTextArea rodape = new JTextArea(2, 10);
    private final JButton botaoRaiz = Estilo.texto(new JButton("Voltar à raiz", Icones.VOLTAR.icone(13)));
    private final JButton botaoExpandir = Estilo.icone(new JButton());
    private boolean expandido;

    PainelArvore(TipoArvore tipo, VisaoArvore.Ouvinte ouvinte, Runnable aoAlternarExpansao) {
        super(new BorderLayout());
        this.tipo = tipo;
        this.ouvinte = ouvinte;
        this.visao = new VisaoArvore(tipo, ouvinte);

        add(montarCabecalho(aoAlternarExpansao), BorderLayout.NORTH);
        add(visao, BorderLayout.CENTER);
        add(montarRodape(), BorderLayout.SOUTH);
        atualizarNumeros(0, 0);
        atualizarUltimaOperacao(null);
        atualizarValidade(null);
    }

    VisaoArvore visao() {
        return visao;
    }

    // ------------------------------------------------------------------ montagem

    private JPanel montarCabecalho(Runnable aoAlternarExpansao) {
        JPanel cabecalho = new JPanel(new GridBagLayout());
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.LINHA),
                BorderFactory.createEmptyBorder(11, 14, 10, 8)));

        JLabel titulo = new JLabel(tipo.nome(), new Bolinha(Tema.corDaArvore(tipo), 16), JLabel.LEADING);
        titulo.setIconTextGap(8);
        titulo.setFont(Tema.fonte(15f, Font.BOLD));
        titulo.setForeground(Tema.TEXTO);
        titulo.setToolTipText(tipo.nomeCompleto());
        // em cartões estreitos o nome encolhe (com "...") em vez de empurrar os botões para fora
        titulo.setMinimumSize(new Dimension(24, titulo.getPreferredSize().height));

        JButton botaoAjustar = botaoIcone("Ajustar o desenho à janela (duplo clique no fundo)", Icones.AJUSTAR);
        botaoAjustar.addActionListener(e -> visao.ajustar());
        botaoExpandir.setIcon(Icones.EXPANDIR.icone(18));
        configurarBotaoIcone(botaoExpandir, "Mostrar só esta árvore");
        botaoExpandir.addActionListener(e -> aoAlternarExpansao.run());

        JPanel botoes = new JPanel(new GridLayout(1, 2, 2, 0));
        botoes.setOpaque(false);
        botoes.add(botaoAjustar);
        botoes.add(botaoExpandir);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        cabecalho.add(titulo, c);
        c.gridx = 1;
        c.weightx = 0;
        cabecalho.add(botoes, c);

        JPanel chips = new JPanel(new LayoutQuebra(FlowLayout.LEFT, 5, 4));
        chips.setOpaque(false);
        chips.add(chipTamanho);
        chips.add(chipAltura);
        chips.add(chipOperacao);
        chips.add(chipValidade);
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.gridy = 1;
        cc.gridwidth = 2;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(6, 0, 0, 0);
        cabecalho.add(chips, cc);
        return cabecalho;
    }

    private JPanel montarRodape() {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.LINHA),
                BorderFactory.createEmptyBorder(6, 14, 6, 8)));
        rodape.setFont(Tema.fonte(11.5f, Font.PLAIN));
        rodape.setForeground(Tema.TEXTO_3);
        rodape.setEditable(false);
        rodape.setFocusable(false);
        rodape.setOpaque(false);
        rodape.setLineWrap(true);
        rodape.setWrapStyleWord(true);
        rodape.setBorder(null);
        p.add(rodape, BorderLayout.CENTER);
        botaoRaiz.setFont(Tema.fonte(12f, Font.BOLD));
        botaoRaiz.setMargin(new Insets(4, 8, 4, 8));
        botaoRaiz.setIconTextGap(5);
        botaoRaiz.setFocusable(false);
        botaoRaiz.setVisible(false);
        botaoRaiz.addActionListener(e -> ouvinte.focoSolicitado(tipo, null));
        p.add(botaoRaiz, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(10, 48));
        return p;
    }

    private static JButton botaoIcone(String dica, Icones icone) {
        JButton b = Estilo.icone(new JButton(icone.icone(18)));
        configurarBotaoIcone(b, dica);
        return b;
    }

    private static void configurarBotaoIcone(JButton b, String dica) {
        b.setToolTipText(dica);
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(32, 32));
    }

    // ------------------------------------------------------------------ atualização

    void atualizarNumeros(int tamanho, int altura) {
        chipTamanho.definir("n = " + Formato.inteiro(tamanho), Paleta.HOVER, Tema.TEXTO,
                "Número de chaves na árvore");
        int minima = (tamanho == 0) ? 0 : 32 - Integer.numberOfLeadingZeros(tamanho);
        boolean longa = tamanho > 0 && altura > 2 * minima + 2;
        chipAltura.definir("altura " + Formato.inteiro(altura), longa ? Paleta.PERIGO_FUNDO : Paleta.HOVER,
                longa ? Paleta.PERIGO_ESCURO : Tema.TEXTO,
                "<html>Níveis do caminho mais longo da raiz até uma folha.<br>"
                        + "Com " + Formato.inteiro(tamanho) + " nós, o mínimo possível seria " + minima + ".</html>");
    }

    /** Ex.: "última operação: 17 comparações". {@code null} apaga. */
    void atualizarUltimaOperacao(String texto) {
        if (texto == null) {
            chipOperacao.definir("", Paleta.HOVER, Tema.TEXTO, null);
            return;
        }
        chipOperacao.definir(texto, Tema.clarear(Tema.corDaArvore(tipo), 0.85), Tema.escurecer(Tema.corDaArvore(tipo), 0.55),
                "Comparações entre chaves feitas na última operação por esta árvore");
    }

    /** {@code null} = ainda não verificada (o chip some). */
    void atualizarValidade(Boolean valida) {
        if (valida == null) {
            chipValidade.definir("", Paleta.HOVER, Tema.TEXTO, null);
        } else if (valida) {
            chipValidade.definir("válida", Paleta.SUCESSO_FUNDO, Paleta.SUCESSO,
                    "A última validação confirmou todas as propriedades da " + tipo.nome());
        } else {
            chipValidade.definir("INVÁLIDA", Paleta.PERIGO_FUNDO, Paleta.PERIGO_ESCURO,
                    "A validação encontrou uma propriedade violada!");
        }
    }

    /** Legenda e aviso de resumo embaixo do desenho. */
    void atualizarRodape(Desenho d, int tamanhoTotal, Integer chaveFoco) {
        boolean emFoco = d.focoAplicado;
        botaoRaiz.setVisible(emFoco);
        String texto;
        if (emFoco) {
            texto = "Mostrando só a subárvore da chave " + chaveFoco + " (" + Formato.inteiro(d.nosNoFoco) + " nós).";
        } else if (d.resumido) {
            texto = "Resumida: " + Formato.inteiro(d.nosVisiveis()) + " de " + Formato.inteiro(tamanhoTotal)
                    + " nós. Cada triângulo é uma subárvore escondida (clique para abrir).";
        } else if (tamanhoTotal == 0) {
            texto = " ";
        } else {
            texto = switch (tipo) {
                case BST -> "Menores à esquerda, maiores à direita. Não se rebalanceia sozinha.";
                case AVL -> "O selo é o fator de balanceamento (altura da esquerda − da direita): sempre −1, 0 ou +1.";
                case RUBRO_NEGRA -> "Raiz preta; vermelho não tem filho vermelho; todo caminho tem o mesmo nº de pretos.";
            };
        }
        rodape.setText(texto);
        rodape.setToolTipText(texto);
    }

    void definirExpandido(boolean expandido) {
        this.expandido = expandido;
        botaoExpandir.setToolTipText(expandido ? "Voltar a mostrar as três árvores" : "Mostrar só esta árvore");
        botaoExpandir.setIcon((expandido ? Icones.REDUZIR : Icones.EXPANDIR).icone(18));
    }

    boolean expandido() {
        return expandido;
    }
}
