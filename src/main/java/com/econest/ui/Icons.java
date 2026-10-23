package com.econest.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Image;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads images and icons from the classpath, scales them and caches the
 * result.
 *
 * <p><b>Nothing here ever throws.</b> If a file is missing, unreadable or the
 * SVG library is not on the classpath, every method returns {@code null} and
 * the caller simply skips the image. That is deliberate: the app must run
 * from a clean checkout before anyone has dropped the artwork in.</p>
 *
 * <pre>
 *   Icon nav  = Icons.icon("inventory", 18, Theme.TEXT_ON_DARK); // SVG, tinted
 *   Icon logo = Icons.image("logo.png", 160, 48);                // PNG, scaled
 *
 *   if (logo != null) {
 *       pnl.add(new JLabel(logo));
 *   }
 * </pre>
 *
 * <h2>Where files live</h2>
 * <ul>
 *   <li>{@code src/main/resources/images/} - photos and the logo (PNG)</li>
 *   <li>{@code src/main/resources/images/icons/} - line icons (SVG)</li>
 * </ul>
 * A name with no extension is looked up as {@code .svg} first, then
 * {@code .png}, in {@code icons/} first and then {@code images/}.
 * See {@code images/README.txt} for the expected file names.
 */
public final class Icons {

    /** Classpath folder for photos and the logo. */
    private static final String IMAGES_DIR = "/images/";

    /** Classpath folder for the line icons. */
    private static final String ICONS_DIR = "/images/icons/";

    /** Extensions tried, in order, when a name has none. */
    private static final String[] EXTENSIONS = {".svg", ".png"};

    /** Cache of everything already loaded, keyed by name + size + tint. */
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    /** Marks "we looked and there was nothing", so we only look once. */
    private static final Icon MISSING = new ImageIcon();

    private Icons() {
        // Utility class.
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * A square icon at the given size, in its own colours.
     *
     * @param name file name, with or without extension ("inventory", "logo.png")
     * @param size width and height in pixels
     * @return the icon, or null when the file is not on the classpath
     */
    public static Icon icon(String name, int size) {
        return icon(name, size, null);
    }

    /**
     * A square icon at the given size, recoloured.
     *
     * <p>Tinting only applies to SVGs - a PNG is returned as-is, so use SVGs
     * for anything that has to sit on both the dark sidebar and a light
     * card.</p>
     *
     * @param name file name, with or without extension
     * @param size width and height in pixels
     * @param tint colour to paint the SVG in, or null to keep its own colours
     * @return the icon, or null when the file is not on the classpath
     */
    public static Icon icon(String name, int size, Color tint) {
        String key = name + "|" + size + "|" + size + "|"
                + (tint == null ? "none" : tint.getRGB());
        Icon cached = CACHE.computeIfAbsent(key, k -> {
            Icon loaded = load(name, size, size, tint);
            return loaded == null ? MISSING : loaded;
        });
        return cached == MISSING ? null : cached;
    }

    /**
     * A bitmap scaled to fit inside a box, keeping its aspect ratio.
     *
     * @param name   file name, e.g. "logo.png"
     * @param maxWidth  the widest it may be
     * @param maxHeight the tallest it may be
     * @return the scaled icon, or null when the file is not on the classpath
     */
    public static Icon image(String name, int maxWidth, int maxHeight) {
        String key = name + "|fit" + maxWidth + "x" + maxHeight;
        Icon cached = CACHE.computeIfAbsent(key, k -> {
            Icon loaded = load(name, maxWidth, maxHeight, null);
            return loaded == null ? MISSING : loaded;
        });
        return cached == MISSING ? null : cached;
    }

    /** True when the named file is actually on the classpath. */
    public static boolean exists(String name) {
        return resolve(name) != null;
    }

    /** Empties the cache. Only useful if artwork is replaced while running. */
    public static void clearCache() {
        CACHE.clear();
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    /**
     * Does the real work behind the public methods. Catches {@link Throwable}
     * rather than {@link Exception} on purpose: a missing SVG library shows up
     * as {@code NoClassDefFoundError}, and that must not take the UI down
     * either.
     */
    private static Icon load(String name, int width, int height, Color tint) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Resolved resolved = resolve(name);
        if (resolved == null) {
            return null;
        }
        try {
            return resolved.path.toLowerCase().endsWith(".svg")
                    ? loadSvg(resolved, width, height, tint)
                    : loadBitmap(resolved, width, height);
        } catch (Throwable t) {
            // Corrupt file, unsupported format, missing SVG library - the UI
            // carries on without the image.
            System.err.println("Icons: could not load " + resolved.path + " (" + t + ")");
            return null;
        }
    }

    /** Renders an SVG at the requested size, optionally recoloured. */
    private static Icon loadSvg(Resolved resolved, int width, int height, Color tint) {
        FlatSVGIcon svg = new FlatSVGIcon(resolved.path.substring(1), width, height);
        if (tint != null) {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(colour -> tint));
        }
        // Touch the icon so a broken file fails here, inside the try block,
        // instead of later on the painting thread.
        if (svg.getIconWidth() <= 0) {
            return null;
        }
        return svg;
    }

    /** Loads a bitmap and scales it to fit the box, keeping its proportions. */
    private static Icon loadBitmap(Resolved resolved, int maxWidth, int maxHeight) {
        ImageIcon original = new ImageIcon(resolved.url);
        int w = original.getIconWidth();
        int h = original.getIconHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        double scale = Math.min(maxWidth / (double) w, maxHeight / (double) h);
        int targetW = Math.max(1, (int) Math.round(w * scale));
        int targetH = Math.max(1, (int) Math.round(h * scale));
        Image scaled = original.getImage()
                .getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // ---------------------------------------------------------------------
    // Name resolution
    // ---------------------------------------------------------------------

    /** A file that was found: its classpath path and its URL. */
    private record Resolved(String path, URL url) {
    }

    /**
     * Finds the file for a name. A name with an extension is looked up as
     * given; a bare name is tried as {@code .svg} then {@code .png}. Both the
     * {@code icons/} and the {@code images/} folder are searched, icons first.
     */
    private static Resolved resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleaned = name.startsWith("/") ? name.substring(1) : name;

        if (cleaned.contains(".")) {
            return firstMatch(ICONS_DIR + cleaned, IMAGES_DIR + cleaned);
        }
        for (String extension : EXTENSIONS) {
            Resolved hit = firstMatch(
                    ICONS_DIR + cleaned + extension,
                    IMAGES_DIR + cleaned + extension);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /** Returns the first of these classpath paths that actually exists. */
    private static Resolved firstMatch(String... paths) {
        for (String path : paths) {
            URL url = Icons.class.getResource(path);
            if (url != null) {
                return new Resolved(path, url);
            }
        }
        return null;
    }
}
