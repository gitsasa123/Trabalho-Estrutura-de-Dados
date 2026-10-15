package arvores.gui;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Procura os arquivos do projeto (data/, results/) de onde o programa foi aberto. */
final class Arquivos {

    private Arquivos() {
    }

    /**
     * Procura {@code relativo} (por exemplo "data/faixas.csv") na pasta atual e nas pastas perto do
     * programa, para funcionar também ao abrir o .jar com duplo clique. Se não achar, devolve o
     * caminho relativo à pasta atual.
     */
    static Path localizar(String relativo) {
        List<Path> candidatos = new ArrayList<>();
        candidatos.add(Path.of(relativo));
        try {
            Path local = Path.of(Arquivos.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path pasta = Files.isDirectory(local) ? local : local.getParent();
            for (int i = 0; i < 3 && pasta != null; i++) {
                candidatos.add(pasta.resolve(relativo));
                pasta = pasta.getParent();
            }
        } catch (URISyntaxException | SecurityException | NullPointerException | IllegalArgumentException e) {
            // sem localização do programa: fica só a pasta atual
        }
        for (Path c : candidatos) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }
        return candidatos.get(0);
    }
}
