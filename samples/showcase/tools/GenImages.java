import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Generates the test images of the showcase (dev time tool, run with "java tools/GenImages.java <dir>").
 */
public class GenImages {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();

        BufferedImage pattern = pattern(256, 256, true);
        ImageIO.write(pattern, "png", new File(dir, "pattern.png"));
        BufferedImage opaque = pattern(256, 256, false);
        ImageIO.write(opaque, "bmp", new File(dir, "pattern.bmp"));
        ImageIO.write(opaque, "gif", new File(dir, "pattern.gif"));
        ImageIO.write(photo(480, 320), "jpg", new File(dir, "photo.jpg"));
        ImageIO.write(icon(64), "png", new File(dir, "icon.png"));
        ImageIO.write(icon(16), "png", new File(dir, "icon-16.png"));
        ImageIO.write(texture(256), "png", new File(dir, "texture.png"));
        ImageIO.write(tile(32), "png", new File(dir, "tile.png"));
        animatedGif(new File(dir, "animated.gif"));
    }

    static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static BufferedImage pattern(int w, int h, boolean alpha) {
        BufferedImage image = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = alpha ? 255 - (int) (Math.hypot(x - w / 2.0, y - h / 2.0) * 1.2) : 255;
                a = Math.max(40, Math.min(255, a));
                image.setRGB(x, y, (a << 24) | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | (255 - x * 255 / w));
            }
        }
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(6));
        g.drawOval(40, 40, w - 80, h - 80);
        g.setColor(new Color(20, 20, 20));
        g.fillRect(w / 2 - 30, h / 2 - 30, 60, 60);
        g.setColor(Color.YELLOW);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g.drawString("FX", w / 2 - 20, h / 2 + 10);
        g.dispose();
        return image;
    }

    static BufferedImage photo(int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image);
        g.setPaint(new GradientPaint(0, 0, new Color(90, 150, 230), 0, h * 0.6f, new Color(250, 220, 170)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 200, 60));
        g.fillOval(w - 140, 40, 80, 80);
        g.setColor(new Color(60, 110, 60));
        int[] xs = { 0, 90, 170, 260, 340, 420, w, w, 0 };
        int[] ys = { 220, 150, 200, 130, 190, 160, 210, h, h };
        g.fillPolygon(xs, ys, xs.length);
        g.setColor(new Color(40, 80, 140));
        g.fillRect(0, h - 60, w, 60);
        g.dispose();
        return image;
    }

    static BufferedImage icon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(image);
        g.setColor(new Color(66, 133, 244));
        g.fillRoundRect(0, 0, size, size, size / 4, size / 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 2));
        g.drawString("Q", size / 4, size * 2 / 3 + size / 12);
        g.dispose();
        return image;
    }

    static BufferedImage texture(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image);
        int cell = size / 8;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                g.setColor((x + y) % 2 == 0 ? new Color(230, 120, 40) : new Color(40, 90, 200));
                g.fillRect(x * cell, y * cell, cell, cell);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size / 5));
        g.drawString("3D", size / 3, size / 2 + size / 12);
        g.dispose();
        return image;
    }

    static BufferedImage tile(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(image);
        g.setColor(new Color(0, 150, 136, 200));
        g.fillRect(0, 0, size / 2, size / 2);
        g.fillRect(size / 2, size / 2, size / 2, size / 2);
        g.setColor(new Color(255, 193, 7, 200));
        g.fillOval(size / 2 + 2, 2, size / 2 - 4, size / 2 - 4);
        g.dispose();
        return image;
    }

    static void animatedGif(File file) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        ImageWriter writer = writers.next();
        Color[] colors = { new Color(220, 50, 50), new Color(50, 170, 70), new Color(50, 90, 220), new Color(240, 180, 30) };
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < colors.length; i++) {
                BufferedImage frame = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = graphics(frame);
                g.setColor(colors[i]);
                g.fillRect(0, 0, 96, 96);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
                g.drawString(String.valueOf(i + 1), 34, 64);
                g.dispose();

                ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(frame);
                IIOMetadata metadata = writer.getDefaultImageMetadata(type, null);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", "50");
                control.setAttribute("transparentColorIndex", "0");
                if (i == 0) {
                    IIOMetadataNode apps = child(root, "ApplicationExtensions");
                    IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                    app.setAttribute("applicationID", "NETSCAPE");
                    app.setAttribute("authenticationCode", "2.0");
                    app.setUserObject(new byte[] { 1, 0, 0 });
                    apps.appendChild(app);
                }
                metadata.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(frame, null, metadata), null);
            }
            writer.endWriteSequence();
        }
    }

    static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
