package arvores.gui.visual;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Cores e fontes do visual moderno do programa: superfícies claras e neutras, texto escuro, e botões
 * primários quase pretos (assim nenhuma cor da interface briga com as cores das árvores).
 */
public final class Paleta {

    // superfícies
    public static final Color FUNDO = new Color(0xF4F5F7);
    public static final Color CARTAO = Color.WHITE;
    public static final Color FUNDO_DESABILITADO = new Color(0xF6F7F9);
    public static final Color BORDA = new Color(0xE3E5EA);
    public static final Color BORDA_FORTE = new Color(0xCBD0D9);
    public static final Color HOVER = new Color(0xF0F2F6);
    public static final Color PRESSIONADO = new Color(0xE5E8EE);
    public static final Color SELECAO = new Color(0xE7EDFB);
    public static final Color PONTOS = new Color(0xE8EAF0);

    // texto
    public static final Color TEXTO = new Color(0x1B1F29);
    public static final Color TEXTO_2 = new Color(0x596072);
    public static final Color TEXTO_3 = new Color(0x8B92A3);
    public static final Color TEXTO_DESABILITADO = new Color(0xB3B9C5);

    // ações
    public static final Color PRIMARIO = new Color(0x1B1F29);
    public static final Color PRIMARIO_HOVER = new Color(0x343C50);
    public static final Color PRIMARIO_PRESSIONADO = new Color(0x0B0E15);
    public static final Color PRIMARIO_DESABILITADO = new Color(0xC3C8D3);
    public static final Color FOCO = new Color(0x5B8DEF);
    public static final Color PERIGO = new Color(0xC62828);
    public static final Color PERIGO_BORDA = new Color(0xEBB9B9);
    public static final Color PERIGO_FUNDO = new Color(0xFDEDED);
    public static final Color PERIGO_ESCURO = new Color(0x8E1B13);
    public static final Color SUCESSO = new Color(0x1D6B3C);
    public static final Color SUCESSO_FUNDO = new Color(0xDDF3E5);

    private static final String[] PREFERIDAS = {
        "Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans", "Ubuntu", "Cantarell",
        "DejaVu Sans"};

    /** A primeira fonte da lista que existe neste computador (senão, a padrão do Java). */
    public static final String FAMILIA = escolherFamilia();

    /** A variante "semibold" da fonte, quando existe (fica mais elegante que o negrito comum); senão, null. */
    private static final String FAMILIA_FORTE = escolherFamiliaForte();

    /** A variante "medium" (peso intermediário), quando existe; senão, null. */
    private static final String FAMILIA_MEDIA = existe(FAMILIA + " Medium") ? FAMILIA + " Medium" : null;

    private static final Map<Integer, Font> CACHE = new HashMap<>();

    private Paleta() {
    }

    private static String escolherFamilia() {
        for (String f : PREFERIDAS) {
            if (existe(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    private static String escolherFamiliaForte() {
        for (String sufixo : new String[] {" SemiBold", " Semibold"}) {
            if (existe(FAMILIA + sufixo)) {
                return FAMILIA + sufixo;
            }
        }
        return null;
    }

    private static boolean existe(String familia) {
        return new Font(familia, Font.PLAIN, 12).getFamily().equalsIgnoreCase(familia);
    }

    /** Fonte da interface; {@code Font.BOLD} dá a variante semibold, se a fonte tiver uma. */
    public static synchronized Font fonte(float tamanho, int estilo) {
        int chave = Math.round(tamanho * 10) * 4 + (estilo & 3);
        return CACHE.computeIfAbsent(chave, k -> {
            if ((estilo & Font.BOLD) != 0 && FAMILIA_FORTE != null) {
                return new Font(FAMILIA_FORTE, estilo & ~Font.BOLD, 1).deriveFont(tamanho);
            }
            return new Font(FAMILIA, estilo, 1).deriveFont(tamanho);
        });
    }

    /** Fonte de peso intermediário (botões, abas): a variante "medium" se existir, senão a normal. */
    public static synchronized Font fonteMedia(float tamanho) {
        int chave = -Math.round(tamanho * 10);
        return CACHE.computeIfAbsent(chave, k ->
                new Font(FAMILIA_MEDIA != null ? FAMILIA_MEDIA : FAMILIA, Font.PLAIN, 1).deriveFont(tamanho));
    }

    /** Liga o antisserrilhado de formas e de texto. */
    public static void qualidade(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color comAlfa(Color cor, double alfa) {
        int a = (int) Math.round(255 * Math.max(0, Math.min(1, alfa)));
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), a);
    }

    public static Color misturar(Color a, Color b, double t) {
        double u = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * u),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * u),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * u));
    }
}
