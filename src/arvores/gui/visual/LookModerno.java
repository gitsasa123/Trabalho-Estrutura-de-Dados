package arvores.gui.visual;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * O visual do programa: um aspecto próprio (feito sobre o Metal, que já traz os componentes menos usados)
 * com superfícies claras, cantos arredondados, botões chapados e barras de rolagem finas.
 * Para usar: {@link #instalar()} antes de criar qualquer janela.
 */
@SuppressWarnings("serial")
public final class LookModerno extends MetalLookAndFeel {

    /** Liga o visual moderno; se não der, o Java continua com o visual que já estava. */
    public static void instalar() {
        MetalLookAndFeel.setCurrentTheme(new TemaMetal());
        try {
            UIManager.setLookAndFeel(new LookModerno());
        } catch (UnsupportedLookAndFeelException e) {
            // continua com o visual padrão do Java
        }
    }

    @Override
    public String getName() {
        return "Moderno";
    }

    @Override
    public String getID() {
        return "Moderno";
    }

    @Override
    public String getDescription() {
        return "Visual claro e arredondado do programa de árvores";
    }

    @Override
    protected void initClassDefaults(UIDefaults tabela) {
        super.initClassDefaults(tabela);
        tabela.putDefaults(new Object[] {
            "ButtonUI", BotaoUI.class.getName(),
            "ToggleButtonUI", BotaoUI.class.getName(),
            "CheckBoxUI", CaixaUI.class.getName(),
            "TabbedPaneUI", AbasUI.class.getName(),
            "ScrollBarUI", RolagemUI.class.getName(),
            "TextFieldUI", CampoUI.class.getName(),
            "FormattedTextFieldUI", CampoUI.Formatado.class.getName(),
            "SpinnerUI", GiradorUI.class.getName(),
            "ComboBoxUI", ComboUI.class.getName(),
            "SliderUI", DeslizanteUI.class.getName(),
            "ProgressBarUI", ProgressoUI.class.getName(),
            "SplitPaneUI", DivisorUI.class.getName(),
            "MenuUI", MenuUI.class.getName(),
            "MenuItemUI", MenuItemUI.class.getName(),
            "PopupMenuSeparatorUI", SeparadorUI.class.getName(),
        });
    }

    @Override
    protected void initComponentDefaults(UIDefaults tabela) {
        super.initComponentDefaults(tabela);

        ColorUIResource texto = cor(Paleta.TEXTO);
        ColorUIResource texto2 = cor(Paleta.TEXTO_2);
        ColorUIResource desabilitado = cor(Paleta.TEXTO_DESABILITADO);
        ColorUIResource branco = cor(Paleta.CARTAO);
        ColorUIResource fundo = cor(Paleta.FUNDO);
        ColorUIResource fundoDesabilitado = cor(Paleta.FUNDO_DESABILITADO);
        ColorUIResource selecao = cor(Paleta.SELECAO);
        ColorUIResource selecaoTexto = cor(new Color(0xD3E1FB));
        ColorUIResource semCor = new ColorUIResource(new Color(0, 0, 0, 0));
        FontUIResource normal = new FontUIResource(Paleta.fonte(13f, Font.PLAIN));
        FontUIResource pequena = new FontUIResource(Paleta.fonte(12f, Font.PLAIN));
        BorderUIResource.EmptyBorderUIResource semBorda = new BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0);
        InsetsUIResource nenhuma = new InsetsUIResource(0, 0, 0, 0);

        tabela.putDefaults(new Object[] {
            // painéis, rótulos e dicas
            "Panel.background", fundo,
            "Panel.foreground", texto,
            "Label.foreground", texto,
            "Label.disabledForeground", desabilitado,
            "ToolTip.background", cor(Paleta.TEXTO),
            "ToolTip.foreground", cor(Color.WHITE),
            "ToolTip.border", new BorderUIResource.EmptyBorderUIResource(6, 10, 6, 10),
            "ToolTip.font", pequena,
            "ToolTip.backgroundInactive", cor(Paleta.TEXTO),
            "ToolTip.foregroundInactive", cor(Color.WHITE),
            "ToolTip.borderInactive", new BorderUIResource.EmptyBorderUIResource(6, 10, 6, 10),

            // botões e caixas de marcar
            "Button.border", new BasicBorders.MarginBorder(),
            "Button.margin", new InsetsUIResource(8, 14, 8, 14),
            "Button.font", new FontUIResource(Paleta.fonteMedia(13f)),
            "Button.background", branco,
            "Button.foreground", texto,
            "Button.disabledText", desabilitado,
            "Button.rollover", Boolean.TRUE,
            "Button.focus", semCor,
            "Button.textShiftOffset", 0,
            "CheckBox.icon", CaixaUI.ICONE,
            "CheckBox.border", new BasicBorders.MarginBorder(),
            "CheckBox.margin", new InsetsUIResource(2, 0, 2, 0),
            "CheckBox.textIconGap", 8,
            "CheckBox.font", normal,
            "CheckBox.foreground", texto,
            "CheckBox.focus", semCor,

            // campos de texto
            "TextField.border", CampoUI.BORDA,
            "TextField.margin", nenhuma,
            "TextField.background", branco,
            "TextField.foreground", texto,
            "TextField.caretForeground", texto,
            "TextField.selectionBackground", selecaoTexto,
            "TextField.selectionForeground", texto,
            "TextField.inactiveForeground", desabilitado,
            "TextField.inactiveBackground", fundoDesabilitado,
            "TextField.disabledBackground", fundoDesabilitado,
            "FormattedTextField.border", CampoUI.BORDA,
            "FormattedTextField.margin", nenhuma,
            "FormattedTextField.background", branco,
            "FormattedTextField.foreground", texto,
            "FormattedTextField.caretForeground", texto,
            "FormattedTextField.selectionBackground", selecaoTexto,
            "FormattedTextField.selectionForeground", texto,
            "FormattedTextField.inactiveForeground", desabilitado,
            "FormattedTextField.inactiveBackground", fundoDesabilitado,
            "FormattedTextField.disabledBackground", fundoDesabilitado,
            "TextArea.background", branco,
            "TextArea.foreground", texto,
            "TextArea.caretForeground", texto,
            "TextArea.selectionBackground", selecaoTexto,
            "TextArea.selectionForeground", texto,
            "TextArea.inactiveForeground", texto,

            // seleção de números e de opções
            "Spinner.border", new BorderUIResource.EmptyBorderUIResource(7, 10, 7, 0),
            "Spinner.arrowButtonInsets", new InsetsUIResource(2, 0, 2, 3),
            "Spinner.arrowButtonBorder", semBorda,
            "Spinner.editorBorderPainted", Boolean.FALSE,
            "Spinner.background", branco,
            "Spinner.foreground", texto,
            "ComboBox.border", new BorderUIResource.EmptyBorderUIResource(1, 1, 1, 1),
            "ComboBox.background", branco,
            "ComboBox.foreground", texto,
            "ComboBox.selectionBackground", selecao,
            "ComboBox.selectionForeground", texto,
            "ComboBox.disabledBackground", fundoDesabilitado,
            "ComboBox.disabledForeground", desabilitado,
            "List.background", branco,
            "List.foreground", texto,
            "List.selectionBackground", selecao,
            "List.selectionForeground", texto,
            "List.focusCellHighlightBorder", semBorda,

            // rolagem, divisórias, abas, progresso
            "ScrollPane.border", semBorda,
            "ScrollPane.background", fundo,
            "Viewport.background", fundo,
            "ScrollBar.width", 12,
            "ScrollBar.minimumThumbSize", new DimensionUIResource(28, 28),
            "ScrollBar.maximumThumbSize", new DimensionUIResource(4096, 4096),
            "SplitPane.border", semBorda,
            "SplitPane.dividerSize", 10,
            "SplitPane.background", fundo,
            "SplitPaneDivider.border", semBorda,
            "TabbedPane.font", new FontUIResource(Paleta.fonteMedia(13.5f)),
            "TabbedPane.foreground", texto2,
            "TabbedPane.selectedForeground", texto,
            "TabbedPane.background", fundo,
            "TabbedPane.focus", semCor,
            "TabbedPane.textIconGap", 6,
            "TabbedPane.tabRunOverlay", 0,
            "TabbedPane.tabsOverlapBorder", Boolean.FALSE,
            "TabbedPane.contentOpaque", Boolean.FALSE,
            "TabbedPane.tabsOpaque", Boolean.FALSE,
            "ProgressBar.border", semBorda,
            "ProgressBar.repaintInterval", 16,
            "ProgressBar.cycleTime", 1600,
            "ProgressBar.background", cor(Paleta.BORDA),
            "ProgressBar.foreground", cor(Paleta.PRIMARIO),

            // tabelas
            "Table.background", branco,
            "Table.foreground", texto,
            "Table.gridColor", cor(Paleta.BORDA),
            "Table.selectionBackground", selecao,
            "Table.selectionForeground", texto,
            "Table.focusCellHighlightBorder", new BorderUIResource.EmptyBorderUIResource(0, 12, 0, 12),
            "Table.cellNoFocusBorder", new BorderUIResource.EmptyBorderUIResource(0, 12, 0, 12),
            "Table.scrollPaneBorder", semBorda,
            "TableHeader.background", branco,
            "TableHeader.foreground", texto2,
            "TableHeader.font", new FontUIResource(Paleta.fonte(12f, Font.BOLD)),
            "TableHeader.cellBorder", semBorda,

            // menus
            "MenuBar.background", branco,
            "MenuBar.foreground", texto,
            "MenuBar.border", new BorderUIResource.EmptyBorderUIResource(2, 8, 2, 8),
            "Menu.background", branco,
            "Menu.foreground", texto,
            "Menu.selectionBackground", cor(Paleta.HOVER),
            "Menu.selectionForeground", texto,
            "Menu.border", new BasicBorders.MarginBorder(),
            "Menu.margin", new InsetsUIResource(6, 12, 6, 12),
            "Menu.font", normal,
            "PopupMenu.background", branco,
            "PopupMenu.foreground", texto,
            "PopupMenu.border", new BorderUIResource(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Paleta.BORDA_FORTE), BorderFactory.createEmptyBorder(4, 4, 4, 4))),
            "Separator.foreground", cor(Paleta.BORDA),
            "Separator.background", branco,

            // seletor de arquivos
            "FileView.directoryIcon", Icones.PASTA.icone(18, new Color(0x6F8FD8)),
            "FileView.fileIcon", Icones.ARQUIVO.icone(18, Paleta.TEXTO_3),
            "FileView.computerIcon", Icones.COMPUTADOR.icone(18, Paleta.TEXTO_3),
            "FileView.hardDriveIcon", Icones.DISCO.icone(18, Paleta.TEXTO_3),
            "FileView.floppyDriveIcon", Icones.DISCO.icone(18, Paleta.TEXTO_3),
            "FileChooser.upFolderIcon", Icones.SUBIR.icone(18),
            "FileChooser.newFolderIcon", Icones.PASTA_MAIS.icone(18),
            "FileChooser.homeFolderIcon", Icones.CASA.icone(18),
            "FileChooser.detailsViewIcon", Icones.LISTA.icone(18),
            "FileChooser.listViewIcon", Icones.GRADE.icone(18),

            // caixas de mensagem
            "OptionPane.background", fundo,
            "OptionPane.messageForeground", texto,
            "OptionPane.messageFont", normal,
            "OptionPane.buttonFont", new FontUIResource(Paleta.fonteMedia(13f)),
            "OptionPane.border", new BorderUIResource.EmptyBorderUIResource(20, 22, 14, 22),
            "OptionPane.buttonAreaBorder", new BorderUIResource.EmptyBorderUIResource(16, 0, 0, 0),
            "OptionPane.setButtonMargin", Boolean.FALSE,
            "OptionPane.informationIcon", Icones.INFO.icone(40),
            "OptionPane.warningIcon", Icones.ALERTA.icone(40),
            "OptionPane.errorIcon", Icones.ERRO.icone(40),
            "OptionPane.questionIcon", Icones.PERGUNTA.icone(40),
        });

        for (String tipo : new String[] {"MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            tabela.putDefaults(new Object[] {
                tipo + ".background", branco,
                tipo + ".foreground", texto,
                tipo + ".selectionBackground", cor(Paleta.HOVER),
                tipo + ".selectionForeground", texto,
                tipo + ".disabledForeground", desabilitado,
                tipo + ".acceleratorForeground", cor(Paleta.TEXTO_3),
                tipo + ".acceleratorSelectionForeground", cor(Paleta.TEXTO_3),
                tipo + ".acceleratorFont", pequena,
                tipo + ".border", new BasicBorders.MarginBorder(),
                tipo + ".margin", new InsetsUIResource(7, 14, 7, 14),
                tipo + ".font", normal,
            });
        }
    }

    private static ColorUIResource cor(Color c) {
        return new ColorUIResource(c);
    }
}
