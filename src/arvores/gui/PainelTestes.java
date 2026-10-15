package arvores.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import arvores.TesteArvore;
import arvores.gui.visual.Cartao;
import arvores.gui.visual.Estilo;
import arvores.gui.visual.Icones;
import arvores.gui.visual.Paleta;

/** Roda os testes automáticos do projeto e mostra o andamento ao vivo. */
@SuppressWarnings("serial")
final class PainelTestes extends JPanel {

    /** Entrega cada linha escrita pelos testes assim que ela termina. */
    private static final class LinhaPorLinha extends OutputStream {
        private final ByteArrayOutputStream linha = new ByteArrayOutputStream();
        private final Consumer<String> aoCompletar;

        LinhaPorLinha(Consumer<String> aoCompletar) {
            this.aoCompletar = aoCompletar;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                aoCompletar.accept(linha.toString(StandardCharsets.UTF_8));
                linha.reset();
            } else {
                linha.write(b);
            }
        }
    }

    private final Barra barra;
    private final JTextArea saida = new JTextArea();
    private final JButton botaoRodar = Estilo.primario(new JButton("Executar os testes", Icones.TOCAR.icone(14)));
    private final JLabel resumo = new JLabel(" ");

    PainelTestes(Barra barra) {
        this.barra = barra;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Testes automáticos");
        titulo.setFont(Tema.fonte(15f, Font.BOLD));
        titulo.setForeground(Tema.TEXTO);
        JTextArea explicacao = new JTextArea("Compara cada árvore com o TreeMap do Java em milhares de operações aleatórias "
                + "e confere a validade depois de cada uma; também testa o que a interface desenha. Leva alguns segundos.");
        explicacao.setEditable(false);
        explicacao.setFocusable(false);
        explicacao.setOpaque(false);
        explicacao.setLineWrap(true);
        explicacao.setWrapStyleWord(true);
        explicacao.setBorder(null);
        explicacao.setFont(Tema.fonte(12.5f, Font.PLAIN));
        explicacao.setForeground(Tema.TEXTO_2);
        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(explicacao, BorderLayout.CENTER);

        resumo.setFont(Tema.fonte(13f, Font.BOLD));
        botaoRodar.addActionListener(e -> executar());
        JPanel acao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        acao.setOpaque(false);
        acao.add(resumo);
        acao.add(botaoRodar);

        Cartao topo = new Cartao(new BorderLayout(20, 0)).comMargem(14, 18, 14, 18);
        topo.add(textos, BorderLayout.CENTER);
        topo.add(acao, BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        saida.setEditable(false);
        saida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        saida.setForeground(Tema.TEXTO);
        saida.setMargin(new Insets(12, 16, 12, 16));
        saida.setText("Clique em \"Executar os testes\".\n");
        JScrollPane rolagem = new JScrollPane(saida);
        rolagem.setBorder(null);
        rolagem.getViewport().setBackground(Paleta.CARTAO);
        Cartao cartaoSaida = new Cartao(new BorderLayout());
        cartaoSaida.add(rolagem, BorderLayout.CENTER);
        add(cartaoSaida, BorderLayout.CENTER);
    }

    private void executar() {
        if (barra.ocupada()) {
            barra.mensagem("Espere a tarefa em andamento terminar antes de rodar os testes.");
            return;
        }
        botaoRodar.setEnabled(false);
        saida.setText("");
        resumo.setText("Executando...");
        resumo.setForeground(Tema.TEXTO_2);
        barra.iniciarTarefa("Executando os testes automáticos...", null);

        new SwingWorker<Long, String>() {
            @Override
            protected Long doInBackground() {
                PrintStream destino = new PrintStream(new LinhaPorLinha(this::publish), true, StandardCharsets.UTF_8);
                long verificacoesDasArvores = TesteArvore.executar(destino);
                long verificacoesDaInterface = TesteGui.executar(destino);
                return verificacoesDasArvores + verificacoesDaInterface;
            }

            @Override
            protected void process(List<String> linhas) {
                for (String l : linhas) {
                    saida.append(l + "\n");
                }
                saida.setCaretPosition(saida.getDocument().getLength());
            }

            @Override
            protected void done() {
                botaoRodar.setEnabled(true);
                barra.terminarTarefa();
                try {
                    String texto = "Tudo certo: " + Formato.inteiro(get()) + " verificações passaram.";
                    resumo.setText(texto);
                    resumo.setForeground(Paleta.SUCESSO);
                    barra.mensagem("Testes automáticos: " + texto);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    resumo.setText("FALHOU: " + causa.getMessage());
                    resumo.setForeground(Paleta.PERIGO);
                    saida.append("\n" + causa + "\n");
                    barra.mensagem("Um teste falhou: " + causa.getMessage());
                }
            }
        }.execute();
    }
}
