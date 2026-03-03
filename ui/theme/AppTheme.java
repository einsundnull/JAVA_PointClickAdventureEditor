package main.ui.theme;

/**
 * Application theme definition.
 * Contains all design tokens for a complete theme.
 */
public class AppTheme {

    private final String name;
    private final ColorPalette colors;
    private final Typography typography;

    private final boolean isDark;

    /**
     * Light theme.
     */
    public static AppTheme light() {
        return new AppTheme("Light", ColorPalette.light(), new Typography(), false);
    }

    /**
     * Dark theme.
     */
    public static AppTheme dark() {
        return new AppTheme("Dark", ColorPalette.dark(), new Typography(), true);
    }

    /**
     * Custom theme.
     */
    public AppTheme(String name, ColorPalette colors, Typography typography, boolean isDark) {
        this.name = name;
        this.colors = colors;
        this.typography = typography;
        this.isDark = isDark;
    }

    public String getName() { return name; }
    public ColorPalette colors() { return colors; }
    public Typography typography() { return typography; }

    /**
     * Returns the Spacing utility class (which contains only static constants).
     */
    public Class<Spacing> spacing() { return Spacing.class; }

    public boolean isDark() { return isDark; }

    @Override
    public String toString() {
        return name + " Theme";
    }
}
