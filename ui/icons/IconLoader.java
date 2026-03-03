package main.ui.icons;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Icon loader for loading and scaling icons.
 * Supports loading from resources and creating icons from strings/text.
 */
public class IconLoader {

    private static final String ICON_PATH = "/ui/icons/";

    /**
     * Load an icon from resources.
     */
    public static ImageIcon load(String name, int size) {
        try {
            InputStream is = IconLoader.class.getResourceAsStream(ICON_PATH + name);
            if (is == null) {
                System.err.println("Icon not found: " + ICON_PATH + name);
                return createPlaceholderIcon(size);
            }
            BufferedImage img = javax.imageio.ImageIO.read(is);
            return scale(img, size);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + name + " - " + e.getMessage());
            return createPlaceholderIcon(size);
        }
    }

    /**
     * Scale an image to the specified size.
     */
    public static ImageIcon scale(Image img, int size) {
        if (img == null) return null;
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();
        return new ImageIcon(scaled);
    }

    /**
     * Create a text-based icon (useful for quick prototyping).
     */
    public static Icon fromText(String text, int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(color);
        Font font = new Font("Segoe UI", Font.PLAIN, size * 2 / 3);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics(font);
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();

        g.drawString(text, x, y);
        g.dispose();

        return new ImageIcon(img);
    }

    /**
     * Create a placeholder icon when loading fails.
     */
    public static ImageIcon createPlaceholderIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a circle with question mark
        g.setColor(new Color(200, 200, 200));
        g.fillOval(0, 0, size, size);

        g.setColor(Color.WHITE);
        Font font = new Font("Segoe UI", Font.BOLD, size * 2 / 3);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        String text = "?";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);

        g.dispose();
        return new ImageIcon(img);
    }

    /**
     * Create a colored circle icon.
     */
    public static Icon coloredCircle(Color color, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(1, 1, size - 2, size - 2);
        g.dispose();
        return new ImageIcon(img);
    }

    /**
     * Create an arrow icon.
     */
    public static Icon arrow(Direction direction, int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int center = size / 2;
        int pad = size / 4;

        switch (direction) {
            case UP:
                g.drawLine(center, size - pad, center, pad);
                g.drawLine(center, pad, pad, pad * 2);
                g.drawLine(center, pad, size - pad, pad * 2);
                break;
            case DOWN:
                g.drawLine(center, pad, center, size - pad);
                g.drawLine(center, size - pad, pad, size - pad * 2);
                g.drawLine(center, size - pad, size - pad, size - pad * 2);
                break;
            case LEFT:
                g.drawLine(size - pad, center, pad, center);
                g.drawLine(pad, center, pad * 2, pad);
                g.drawLine(pad, center, pad * 2, size - pad);
                break;
            case RIGHT:
                g.drawLine(pad, center, size - pad, center);
                g.drawLine(size - pad, center, size - pad * 2, pad);
                g.drawLine(size - pad, center, size - pad * 2, size - pad);
                break;
        }

        g.dispose();
        return new ImageIcon(img);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
