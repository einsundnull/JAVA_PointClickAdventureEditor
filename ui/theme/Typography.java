package main.ui.theme;

import java.awt.Font;

/**
 * Typography system for consistent font usage across the application.
 */
public class Typography {

    private final String primaryFontFamily;
    private final String monoFontFamily;
    private final float baseSize;

    private final Font fontXS;
    private final Font fontSM;
    private final Font fontBase;
    private final Font fontMD;
    private final Font fontLG;
    private final Font fontXL;
    private final Font font2X;

    private final Font fontSemiboldSM;
    private final Font fontSemiboldBase;
    private final Font fontSemiboldMD;
    private final Font fontSemiboldLG;

    private final Font fontMonoSM;
    private final Font fontMonoBase;

    /**
     * Create typography with system defaults.
     */
    public Typography() {
        this("Segoe UI", "JetBrains Mono", 12f);
    }

    /**
     * Create custom typography.
     *
     * @param primaryFontFamily Main font family
     * @param monoFontFamily Monospace font family
     * @param baseSize Base font size in points
     */
    public Typography(String primaryFontFamily, String monoFontFamily, float baseSize) {
        this.primaryFontFamily = primaryFontFamily;
        this.monoFontFamily = monoFontFamily;
        this.baseSize = baseSize;

        // Calculate sizes based on baseSize
        float xs = baseSize - 2f;
        float sm = baseSize - 1f;
        float md = baseSize + 1f;
        float lg = baseSize + 3f;
        float xl = baseSize + 6f;
        float xx = baseSize + 10f;

        // Regular weights
        this.fontXS = new Font(primaryFontFamily, Font.PLAIN, (int) xs);
        this.fontSM = new Font(primaryFontFamily, Font.PLAIN, (int) sm);
        this.fontBase = new Font(primaryFontFamily, Font.PLAIN, (int) baseSize);
        this.fontMD = new Font(primaryFontFamily, Font.PLAIN, (int) md);
        this.fontLG = new Font(primaryFontFamily, Font.PLAIN, (int) lg);
        this.fontXL = new Font(primaryFontFamily, Font.PLAIN, (int) xl);
        this.font2X = new Font(primaryFontFamily, Font.PLAIN, (int) xx);

        // Semibold weights (Font.BOLD = 700)
        this.fontSemiboldSM = new Font(primaryFontFamily, Font.BOLD, (int) sm);
        this.fontSemiboldBase = new Font(primaryFontFamily, Font.BOLD, (int) baseSize);
        this.fontSemiboldMD = new Font(primaryFontFamily, Font.BOLD, (int) md);
        this.fontSemiboldLG = new Font(primaryFontFamily, Font.BOLD, (int) lg);

        // Monospace fonts
        this.fontMonoSM = new Font(monoFontFamily, Font.PLAIN, (int) sm);
        this.fontMonoBase = new Font(monoFontFamily, Font.PLAIN, (int) baseSize);
    }

    // === REGULAR ===

    /** Extra small - 10px: Tiny labels, metadata */
    public Font xs() { return fontXS; }

    /** Small - 11px: Body text, list items, button text */
    public Font sm() { return fontSM; }

    /** Base - 12px: Default text */
    public Font base() { return fontBase; }

    /** Medium - 13px: Subtitles, section headers */
    public Font md() { return fontMD; }

    /** Large - 15px: Titles */
    public Font lg() { return fontLG; }

    /** Extra Large - 18px: Main headers */
    public Font xl() { return fontXL; }

    /** 2X - 22px: Dialog titles */
    public Font x2() { return font2X; }

    // === SEMIBOLD ===

    /** Semibold Small - 11px: Button text, important labels */
    public Font semiboldSm() { return fontSemiboldSM; }

    /** Semibold Base - 12px: Default headings */
    public Font semiboldBase() { return fontSemiboldBase; }

    /** Semibold Medium - 13px: Section titles */
    public Font semiboldMd() { return fontSemiboldMD; }

    /** Semibold Large - 15px: Card titles */
    public Font semiboldLg() { return fontSemiboldLG; }

    // === MONOSPACE ===

    /** Monospace Small - 11px: Code snippets, log output */
    public Font monoSm() { return fontMonoSM; }

    /** Monospace Base - 12px: Default monospace */
    public Font monoBase() { return fontMonoBase; }

    // === GETTERS ===

    public String getPrimaryFontFamily() { return primaryFontFamily; }
    public String getMonoFontFamily() { return monoFontFamily; }
    public float getBaseSize() { return baseSize; }
}
