import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * Compares two snapshot runs (see scripts/snapshot.sh) : images pixel by pixel, checks value by value, and errors.
 * <p>
 * usage: java tools/Compare.java comparison/jvm comparison/native comparison/diff [tolerance]
 * <p>
 * Writes summary.txt, index.html and diff images into the output directory. Exit code 0 when both runs match and
 * none reported errors, 1 otherwise.
 * <p>
 * Images whose differences are at most {@link #NOISE_MAX_DELTA} per channel on less than {@link #NOISE_MAX_RATIO} of
 * the pixels are reported as NOISE : the same variations exist between two JVM runs using different execution modes
 * (e.g. JIT vs -Xint), they come from floating point evaluation, not from the native image.
 * <p>
 * The images and check values of a page flagged {@code runtimeDependent} in the reports (a page showing where the JVM
 * and a native image legitimately differ) are reported as EXPECTED when they differ : they are not mismatches, but
 * failed checks and errors of such a page still are.
 * <p>
 * Where an image differs is in summary.txt, readable without the images : the bounding box {@code (x,y) wxh} of the
 * differing pixels and the regions they form (see {@link Regions}). Images of different sizes (SIZE) are compared on
 * their common area, from the top left corner.
 */
public class Compare {

    static final int NOISE_MAX_DELTA = 2;
    static final double NOISE_MAX_RATIO = 0.005;
    /** The side of the grid cells of the regions, in pixels (see {@link Regions}) */
    static final int REGION_CELL = 16;
    static final int MAX_REGIONS = 3;

    /**
     * An image compared : {@code differing}, {@code total} and {@code where} are about the common area of images of
     * different sizes.
     */
    record ImageResult(String file, String status, long differing, long total, int maxDelta, String diffFile,
            String where) {
    }

    public static void main(String[] args) throws Exception {
        Path a = Path.of(args[0]);
        Path b = Path.of(args[1]);
        Path out = Path.of(args[2]);
        int tolerance = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        Files.createDirectories(out);

        // Reports
        Map<String, Object> reportA = readReport(a);
        Map<String, Object> reportB = readReport(b);
        Map<String, Map<String, Object>> pagesA = pages(reportA);
        Map<String, Map<String, Object>> pagesB = pages(reportB);
        TreeSet<String> pageIds = new TreeSet<>(pagesA.keySet());
        pageIds.addAll(pagesB.keySet());
        Set<String> runtimeDependent = new TreeSet<>();
        for (Map<String, Map<String, Object>> pages : List.of(pagesA, pagesB)) {
            pages.forEach((id, page) -> {
                if (Boolean.TRUE.equals(page.get("runtimeDependent"))) {
                    runtimeDependent.add(id);
                }
            });
        }

        // Images
        TreeSet<String> names = new TreeSet<>();
        names.addAll(pngs(a));
        names.addAll(pngs(b));
        List<ImageResult> images = new ArrayList<>();
        for (String name : names) {
            ImageResult r = compareImage(a.resolve(name), b.resolve(name), out, name, tolerance);
            if (!r.status.equals("IDENTICAL") && !r.status.equals("NOISE") && runtimeDependent.contains(pageId(name))) {
                r = new ImageResult(r.file, "EXPECTED", r.differing, r.total, r.maxDelta, r.diffFile, r.where);
            }
            images.add(r);
        }

        List<String> lines = new ArrayList<>();
        int mismatches = 0;
        int errorPages = 0;
        int sameErrorPages = 0;
        Set<String> expectedPages = new TreeSet<>();

        lines.add("A = " + a + " (" + reportA.getOrDefault("runtime", "?") + ")");
        lines.add("B = " + b + " (" + reportB.getOrDefault("runtime", "?") + ")");
        for (String key : List.of("javafxVersion", "javaVersion", "screen", "pipeline", "conditionalFeatures")) {
            if (!String.valueOf(reportA.get(key)).equals(String.valueOf(reportB.get(key)))) {
                lines.add("ENV DIFF " + key + ": A=" + reportA.get(key) + " | B=" + reportB.get(key));
                mismatches++;
            }
        }
        lines.add("");
        lines.add("== Images (tolerance " + tolerance + " ; box (x,y) wxh : the bounding box of the differing pixels ; "
                + "region : the differing pixels of touching cells (sides or corners) of a " + REGION_CELL
                + " px grid)");
        for (ImageResult r : images) {
            if (r.status.equals("EXPECTED")) {
                expectedPages.add(pageId(r.file));
            } else if (!r.status.equals("IDENTICAL") && !r.status.equals("NOISE")) {
                mismatches++;
            }
            lines.add(String.format("%-10s %-60s %s", r.status, r.file, describe(r)));
        }

        lines.add("");
        lines.add("== Checks and errors");
        Map<String, List<String>> pageNotes = new LinkedHashMap<>();
        for (String id : pageIds) {
            List<String> notes = new ArrayList<>();
            Map<String, Object> pa = pagesA.get(id);
            Map<String, Object> pb = pagesB.get(id);
            if (pa == null || pb == null) {
                notes.add("page only in " + (pa == null ? "B" : "A"));
            } else {
                Map<String, String> ca = checks(pa);
                Map<String, String> cb = checks(pb);
                TreeSet<String> checkNames = new TreeSet<>(ca.keySet());
                checkNames.addAll(cb.keySet());
                for (String c : checkNames) {
                    if (!String.valueOf(ca.get(c)).equals(String.valueOf(cb.get(c)))) {
                        if (runtimeDependent.contains(id)) {
                            notes.add("expected: check '" + c + "': A=" + ca.get(c) + " | B=" + cb.get(c));
                            expectedPages.add(id);
                        } else {
                            notes.add("check '" + c + "': A=" + ca.get(c) + " | B=" + cb.get(c));
                        }
                    }
                }
                if (!String.valueOf(pa.get("extras")).equals(String.valueOf(pb.get("extras")))) {
                    notes.add("extras: A=" + pa.get("extras") + " | B=" + pb.get("extras"));
                }
            }
            List<?> errorsA = pa != null && pa.get("errors") instanceof List<?> l ? l : List.of();
            List<?> errorsB = pb != null && pb.get("errors") instanceof List<?> l ? l : List.of();
            boolean sameErrors = !errorsA.isEmpty() && String.valueOf(errorsA).equals(String.valueOf(errorsB));
            if (sameErrors) {
                // not a difference between the two runs (e.g. a JavaFX bug or a missing device on this machine)
                errorsA.forEach(e -> notes.add("same error in both: " + e));
            } else {
                errorsA.forEach(e -> notes.add("error in A: " + e));
                errorsB.forEach(e -> notes.add("error in B: " + e));
            }
            if (!notes.isEmpty()) {
                if (notes.stream().anyMatch(n -> n.startsWith("error"))) {
                    errorPages++;
                }
                if (sameErrors) {
                    sameErrorPages++;
                }
                if (notes.stream().anyMatch(n -> !n.startsWith("error") && !n.startsWith("same error")
                        && !n.startsWith("expected"))) {
                    mismatches++;
                }
                pageNotes.put(id, notes);
                lines.add(id);
                notes.forEach(n -> lines.add("    " + n));
            }
        }
        for (var entry : Map.of("A", reportA, "B", reportB).entrySet()) {
            Object other = entry.getValue().get("uncaughtOutsidePages");
            if (other instanceof Map<?, ?> map && !map.isEmpty()) {
                lines.add("uncaught outside pages in " + entry.getKey() + ": " + map);
                errorPages++;
            }
        }

        long identical = images.stream().filter(r -> r.status.equals("IDENTICAL")).count();
        long noise = images.stream().filter(r -> r.status.equals("NOISE")).count();
        String verdict = mismatches == 0 && errorPages == 0 ? "MATCH" : "MISMATCH";
        lines.add(0, String.format("%s : %d/%d images identical, %d floating point noise, %d mismatches, %d pages with errors, "
                + "%d pages with the same errors in both runs, %d runtime dependent pages with EXPECTED differences %s",
                verdict, identical, images.size(), noise, mismatches, errorPages, sameErrorPages, expectedPages.size(),
                expectedPages));
        Files.write(out.resolve("summary.txt"), lines, StandardCharsets.UTF_8);
        writeHtml(out, a, b, reportA, reportB, images, pageNotes, lines.getFirst());
        lines.forEach(System.out::println);
        System.exit(verdict.equals("MATCH") ? 0 : 1);
    }

    /**
     * The id of the page an image belongs to : {@code <page>.png}, {@code <page>--<extra>.png}, and the size suffix
     * of SIZE results.
     */
    static String pageId(String image) {
        String name = image.contains(" (") ? image.substring(0, image.indexOf(" (")) : image;
        name = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
        return name.contains("--") ? name.substring(0, name.indexOf("--")) : name;
    }

    static List<String> pngs(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png")).toList();
        }
    }

    /**
     * The differing pixels of an image and where they are, e.g.
     * {@code 44 px (0.003%), max delta 194, box (1210,12) 180x40, 1 region}, on the common area of images of different
     * sizes.
     */
    static String describe(ImageResult r) {
        String pixels = r.differing == 0 ? "" : String.format("%d px (%.3f%%), max delta %d, %s", r.differing,
                100.0 * r.differing / r.total, r.maxDelta, r.where);
        if (r.file.contains(" (") && r.total > 0) {
            return "common area " + (r.differing == 0 ? "identical" : ": " + pixels);
        }
        return pixels;
    }

    static ImageResult compareImage(Path fa, Path fb, Path out, String name, int tolerance) throws IOException {
        if (!Files.exists(fa)) {
            return new ImageResult(name, "ONLY_B", 0, 0, 0, null, "");
        }
        if (!Files.exists(fb)) {
            return new ImageResult(name, "ONLY_A", 0, 0, 0, null, "");
        }
        BufferedImage ia = ImageIO.read(fa.toFile());
        BufferedImage ib = ImageIO.read(fb.toFile());
        boolean sameSize = ia.getWidth() == ib.getWidth() && ia.getHeight() == ib.getHeight();
        String file = sameSize ? name
                : name + " (" + ia.getWidth() + "x" + ia.getHeight() + " vs " + ib.getWidth() + "x" + ib.getHeight()
                        + ")";
        // images of different sizes : their common area, from the top left corner
        int w = Math.min(ia.getWidth(), ib.getWidth());
        int h = Math.min(ia.getHeight(), ib.getHeight());
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Regions regions = new Regions(w, h);
        long differing = 0;
        int maxDelta = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = ia.getRGB(x, y);
                int pb = ib.getRGB(x, y);
                int delta = 0;
                for (int shift = 0; shift <= 24; shift += 8) {
                    delta = Math.max(delta, Math.abs(((pa >> shift) & 0xFF) - ((pb >> shift) & 0xFF)));
                }
                int gray = (((pa >> 16) & 0xFF) + ((pa >> 8) & 0xFF) + (pa & 0xFF)) / 3;
                gray = 200 + gray * 55 / 255;
                if (delta > tolerance) {
                    differing++;
                    maxDelta = Math.max(maxDelta, delta);
                    regions.add(x, y);
                    int red = 128 + Math.min(127, delta);
                    diff.setRGB(x, y, (red << 16));
                } else {
                    diff.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        if (differing == 0) {
            return new ImageResult(file, sameSize ? "IDENTICAL" : "SIZE", 0, (long) w * h, 0, null, "");
        }
        String diffFile = "diff-" + name;
        ImageIO.write(diff, "png", out.resolve(diffFile).toFile());
        boolean noise = maxDelta <= NOISE_MAX_DELTA && differing < NOISE_MAX_RATIO * w * h;
        String status = !sameSize ? "SIZE" : noise ? "NOISE" : "DIFFERENT";
        return new ImageResult(file, status, differing, (long) w * h, maxDelta, diffFile, regions.describe());
    }

    /**
     * Where the differing pixels of an image are : their bounding box and the regions they form. The image is divided
     * in square cells of {@link #REGION_CELL} pixels : the differing pixels of touching cells (sides or corners) belong
     * to the same region. Pixels at most {@link #REGION_CELL} pixels apart (horizontally and vertically) always do,
     * pixels less than twice as far apart do when their cells touch.
     */
    static final class Regions {
        private final int columns;
        private final int rows;
        // per cell : the number of differing pixels and their bounding box
        private final int[] count;
        private final int[] minX;
        private final int[] minY;
        private final int[] maxX;
        private final int[] maxY;

        Regions(int width, int height) {
            columns = (width + REGION_CELL - 1) / REGION_CELL;
            rows = (height + REGION_CELL - 1) / REGION_CELL;
            count = new int[columns * rows];
            minX = new int[count.length];
            minY = new int[count.length];
            maxX = new int[count.length];
            maxY = new int[count.length];
        }

        void add(int x, int y) {
            int cell = (y / REGION_CELL) * columns + x / REGION_CELL;
            if (count[cell]++ == 0) {
                minX[cell] = maxX[cell] = x;
                minY[cell] = maxY[cell] = y;
            } else {
                minX[cell] = Math.min(minX[cell], x);
                minY[cell] = Math.min(minY[cell], y);
                maxX[cell] = Math.max(maxX[cell], x);
                maxY[cell] = Math.max(maxY[cell], y);
            }
        }

        /**
         * {@code box (x,y) wxh, n regions : (x,y) wxh p px ; ...} with the {@link #MAX_REGIONS} largest regions, empty
         * without differing pixels.
         */
        String describe() {
            // the regions (x0, y0, x1, y1, pixels) : the touching cells with differing pixels, by a depth first search
            List<int[]> regions = new ArrayList<>();
            boolean[] seen = new boolean[count.length];
            int[] stack = new int[count.length];
            for (int start = 0; start < count.length; start++) {
                if (count[start] == 0 || seen[start]) {
                    continue;
                }
                int[] region = { Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1, 0 };
                regions.add(region);
                int top = 0;
                stack[top++] = start;
                seen[start] = true;
                while (top > 0) {
                    int cell = stack[--top];
                    region[0] = Math.min(region[0], minX[cell]);
                    region[1] = Math.min(region[1], minY[cell]);
                    region[2] = Math.max(region[2], maxX[cell]);
                    region[3] = Math.max(region[3], maxY[cell]);
                    region[4] += count[cell];
                    int column = cell % columns;
                    int row = cell / columns;
                    for (int r = Math.max(0, row - 1); r <= Math.min(rows - 1, row + 1); r++) {
                        for (int c = Math.max(0, column - 1); c <= Math.min(columns - 1, column + 1); c++) {
                            int next = r * columns + c;
                            if (count[next] > 0 && !seen[next]) {
                                seen[next] = true;
                                stack[top++] = next;
                            }
                        }
                    }
                }
            }
            if (regions.isEmpty()) {
                return "";
            }
            int[] all = { Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1, 0 };
            for (int[] region : regions) {
                all[0] = Math.min(all[0], region[0]);
                all[1] = Math.min(all[1], region[1]);
                all[2] = Math.max(all[2], region[2]);
                all[3] = Math.max(all[3], region[3]);
            }
            StringBuilder where = new StringBuilder("box ").append(box(all)).append(", ").append(regions.size())
                    .append(regions.size() == 1 ? " region" : " regions");
            if (regions.size() > 1) {
                // the largest first, then from the top left (a stable sort of the regions found row by row)
                regions.sort((a, b) -> Integer.compare(b[4], a[4]));
                where.append(regions.size() > MAX_REGIONS ? ", the " + MAX_REGIONS + " largest : " : " : ");
                for (int i = 0; i < Math.min(MAX_REGIONS, regions.size()); i++) {
                    where.append(i > 0 ? " ; " : "").append(box(regions.get(i))).append(' ').append(regions.get(i)[4])
                            .append(" px");
                }
            }
            return where.toString();
        }

        static String box(int[] region) {
            return "(" + region[0] + "," + region[1] + ") " + (region[2] - region[0] + 1) + "x"
                    + (region[3] - region[1] + 1);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readReport(Path dir) throws IOException {
        Path file = dir.resolve("report.json");
        if (!Files.exists(file)) {
            return Map.of();
        }
        return (Map<String, Object>) new JsonParser(Files.readString(file)).parse();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> pages(Map<String, Object> report) {
        Map<String, Map<String, Object>> pages = new LinkedHashMap<>();
        Object list = report.get("pages");
        if (list instanceof List<?> l) {
            for (Object p : l) {
                Map<String, Object> page = (Map<String, Object>) p;
                pages.put(String.valueOf(page.get("id")), page);
            }
        }
        return pages;
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> checks(Map<String, Object> page) {
        Map<String, String> checks = new LinkedHashMap<>();
        Object list = page.get("checks");
        if (list instanceof List<?> l) {
            for (Object c : l) {
                Map<String, Object> check = (Map<String, Object>) c;
                checks.put(String.valueOf(check.get("name")), check.get("value") + " [" + check.get("ok") + "]");
            }
        }
        return checks;
    }

    static void writeHtml(Path out, Path a, Path b, Map<String, Object> ra, Map<String, Object> rb, List<ImageResult> images,
            Map<String, List<String>> pageNotes, String verdict) throws IOException {
        StringBuilder html = new StringBuilder("""
                <!doctype html><meta charset="utf-8"><title>JVM vs native</title>
                <style>body{font:14px -apple-system,sans-serif;margin:20px}table{border-collapse:collapse}
                td,th{border:1px solid #ccc;padding:3px 8px;text-align:left}.IDENTICAL{color:#1b7f3a}.NOISE{color:#8a6d00}.EXPECTED{color:#1565c0}.DIFFERENT,.SIZE,.ONLY_A,.ONLY_B{color:#c62828;font-weight:bold}
                .row{display:flex;gap:8px;margin:8px 0 24px}.row figure{margin:0;flex:1}.row img{width:100%;border:1px solid #ccc}
                figcaption{font-size:12px;color:#555}pre{background:#f6f6f6;padding:8px;white-space:pre-wrap}</style>
                """);
        html.append("<h1>").append(esc(verdict)).append("</h1>");
        html.append("<p>A: ").append(esc(a.toString())).append(" (").append(esc(String.valueOf(ra.get("runtime"))))
                .append(") &mdash; B: ").append(esc(b.toString())).append(" (").append(esc(String.valueOf(rb.get("runtime"))))
                .append(")</p><table><tr><th>image</th><th>status</th><th>differing pixels</th><th>max delta</th>")
                .append("<th>where</th></tr>");
        for (ImageResult r : images) {
            html.append("<tr><td><a href='#").append(esc(r.file)).append("'>").append(esc(r.file)).append("</a></td><td class='")
                    .append(r.status).append("'>").append(r.status).append("</td><td>")
                    .append(r.total == 0 ? "" : r.differing + String.format(" (%.3f%%)", 100.0 * r.differing / r.total))
                    .append("</td><td>").append(r.maxDelta).append("</td><td>").append(esc(r.where))
                    .append("</td></tr>");
        }
        html.append("</table>");
        if (!pageNotes.isEmpty()) {
            html.append("<h2>Checks and errors</h2><pre>");
            pageNotes.forEach((id, notes) -> {
                html.append(esc(id)).append('\n');
                notes.forEach(n -> html.append("    ").append(esc(n)).append('\n'));
            });
            html.append("</pre>");
        }
        html.append("<h2>Images</h2>");
        for (ImageResult r : images) {
            String file = r.file.contains(" (") ? r.file.substring(0, r.file.indexOf(" (")) : r.file;
            html.append("<h3 id='").append(esc(r.file)).append("'>").append(esc(r.file)).append(" <span class='")
                    .append(r.status).append("'>").append(r.status).append("</span></h3><div class='row'>");
            Path base = out.toAbsolutePath().normalize();
            figure(html, base.relativize(a.resolve(file).toAbsolutePath().normalize()), "A " + ra.get("runtime"));
            figure(html, base.relativize(b.resolve(file).toAbsolutePath().normalize()), "B " + rb.get("runtime"));
            if (r.diffFile != null) {
                figure(html, Path.of(r.diffFile), "differences (red)");
            }
            html.append("</div>");
        }
        Files.writeString(out.resolve("index.html"), html, StandardCharsets.UTF_8);
    }

    static void figure(StringBuilder html, Path src, String caption) {
        html.append("<figure><img loading='lazy' src='").append(esc(src.toString())).append("'><figcaption>")
                .append(esc(caption)).append("</figcaption></figure>");
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }

    /**
     * Minimal JSON parser for the reports written by the showcase.
     */
    static final class JsonParser {
        private final String s;
        private int i;

        JsonParser(String s) {
            this.s = s;
        }

        Object parse() {
            ws();
            char c = s.charAt(i);
            Object value;
            if (c == '{') {
                Map<String, Object> map = new LinkedHashMap<>();
                i++;
                ws();
                if (s.charAt(i) == '}') {
                    i++;
                    return map;
                }
                while (true) {
                    ws();
                    String key = (String) parse();
                    ws();
                    i++; // :
                    map.put(key, parse());
                    ws();
                    if (s.charAt(i++) == '}') {
                        return map;
                    }
                }
            } else if (c == '[') {
                List<Object> list = new ArrayList<>();
                i++;
                ws();
                if (s.charAt(i) == ']') {
                    i++;
                    return list;
                }
                while (true) {
                    list.add(parse());
                    ws();
                    if (s.charAt(i++) == ']') {
                        return list;
                    }
                }
            } else if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (s.charAt(i) != '"') {
                    char ch = s.charAt(i++);
                    if (ch == '\\') {
                        char e = s.charAt(i++);
                        switch (e) {
                            case 'n' -> sb.append('\n');
                            case 'r' -> sb.append('\r');
                            case 't' -> sb.append('\t');
                            case 'u' -> {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                            }
                            default -> sb.append(e);
                        }
                    } else {
                        sb.append(ch);
                    }
                }
                i++;
                value = sb.toString();
            } else if (s.startsWith("true", i)) {
                i += 4;
                value = true;
            } else if (s.startsWith("false", i)) {
                i += 5;
                value = false;
            } else if (s.startsWith("null", i)) {
                i += 4;
                value = null;
            } else {
                int start = i;
                while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                    i++;
                }
                value = Double.parseDouble(s.substring(start, i));
            }
            return value;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
