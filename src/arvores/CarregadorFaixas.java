package arvores;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lê o CSV limpo (data/faixas.csv) gerado por scripts/preparar_dados.py.
 * Formato: chave,titulo,artista,genero,ouvintes,duracao  (uma faixa por linha).
 */
public final class CarregadorFaixas {

    public record Registro(int chave, Faixa dados) {
    }

    private CarregadorFaixas() {
    }

    /**
     * @param limite    quantas faixas ler (as primeiras, na ordem do arquivo); 0 ou negativo = todas
     * @param embaralhar true = ordem aleatória (caso médio); false = ordem do arquivo,
     *                   que é crescente por track_id (pior caso da BST)
     * @param seed      semente do embaralhamento (fixa = resultados reproduzíveis)
     */
    public static List<Registro> carregar(Path caminho, int limite, boolean embaralhar, long seed)
            throws IOException {
        List<Registro> registros = new ArrayList<>();
        try (BufferedReader leitor = Files.newBufferedReader(caminho, StandardCharsets.UTF_8)) {
            leitor.readLine(); // cabeçalho
            String linha;
            while ((limite <= 0 || registros.size() < limite) && (linha = leitor.readLine()) != null) {
                if (linha.isBlank()) {
                    continue;
                }
                List<String> c = dividirLinhaCsv(linha);
                if (c.size() < 6) {
                    throw new IOException("Linha com campos faltando: " + linha);
                }
                Faixa faixa = new Faixa(c.get(1), c.get(2), c.get(3),
                        Long.parseLong(c.get(4)), Integer.parseInt(c.get(5)));
                registros.add(new Registro(Integer.parseInt(c.get(0)), faixa));
            }
        }
        if (embaralhar) {
            Collections.shuffle(registros, new Random(seed));
        }
        return registros;
    }

    /** Divide uma linha CSV respeitando aspas ("a,b" é um campo só; "" dentro de aspas vira "). */
    static List<String> dividirLinhaCsv(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char ch = linha.charAt(i);
            if (entreAspas) {
                if (ch == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(ch);
                }
            } else if (ch == '"') {
                entreAspas = true;
            } else if (ch == ',') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}
