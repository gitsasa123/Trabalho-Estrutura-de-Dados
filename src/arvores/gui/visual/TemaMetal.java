package arvores.gui.visual;

import java.awt.Font;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/** As cores e fontes básicas que o Metal usa onde o {@link LookModerno} não define nada específico. */
final class TemaMetal extends DefaultMetalTheme {

    private static final ColorUIResource PRIMARIA_1 = new ColorUIResource(Paleta.FOCO);
    private static final ColorUIResource PRIMARIA_2 = new ColorUIResource(0xC6D6F7);
    private static final ColorUIResource PRIMARIA_3 = new ColorUIResource(0xD3E1FB);
    private static final ColorUIResource SECUNDARIA_1 = new ColorUIResource(Paleta.BORDA_FORTE);
    private static final ColorUIResource SECUNDARIA_2 = new ColorUIResource(Paleta.BORDA);
    private static final ColorUIResource SECUNDARIA_3 = new ColorUIResource(Paleta.FUNDO);
    private static final ColorUIResource TEXTO = new ColorUIResource(Paleta.TEXTO);

    private static final FontUIResource NORMAL = new FontUIResource(Paleta.fonte(13f, Font.PLAIN));
    private static final FontUIResource PEQUENA = new FontUIResource(Paleta.fonte(12f, Font.PLAIN));
    private static final FontUIResource FORTE = new FontUIResource(Paleta.fonte(13f, Font.BOLD));

    @Override
    public String getName() {
        return "Moderno";
    }

    @Override
    protected ColorUIResource getPrimary1() {
        return PRIMARIA_1;
    }

    @Override
    protected ColorUIResource getPrimary2() {
        return PRIMARIA_2;
    }

    @Override
    protected ColorUIResource getPrimary3() {
        return PRIMARIA_3;
    }

    @Override
    protected ColorUIResource getSecondary1() {
        return SECUNDARIA_1;
    }

    @Override
    protected ColorUIResource getSecondary2() {
        return SECUNDARIA_2;
    }

    @Override
    protected ColorUIResource getSecondary3() {
        return SECUNDARIA_3;
    }

    @Override
    protected ColorUIResource getBlack() {
        return TEXTO;
    }

    @Override
    public FontUIResource getControlTextFont() {
        return NORMAL;
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return NORMAL;
    }

    @Override
    public FontUIResource getUserTextFont() {
        return NORMAL;
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return NORMAL;
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return FORTE;
    }

    @Override
    public FontUIResource getSubTextFont() {
        return PEQUENA;
    }
}
