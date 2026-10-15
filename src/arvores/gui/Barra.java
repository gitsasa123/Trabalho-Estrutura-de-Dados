package arvores.gui;

/** Onde os painéis mostram mensagens e o andamento de tarefas demoradas (a barra de status da janela). */
interface Barra {

    void mensagem(String texto);

    /**
     * Começa uma tarefa demorada; {@code cancelar} é chamado se o usuário apertar Cancelar
     * ({@code null} = a tarefa não pode ser cancelada, e o botão não aparece).
     */
    void iniciarTarefa(String texto, Runnable cancelar);

    /** Há uma tarefa demorada rodando? Duas ao mesmo tempo atrapalhariam as medidas do benchmark. */
    boolean ocupada();

    /** Andamento de 0 a 1 (negativo = indeterminado) e o texto que o acompanha. */
    void progresso(double fracao, String texto);

    void terminarTarefa();
}
