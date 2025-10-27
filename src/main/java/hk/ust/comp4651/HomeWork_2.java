package hk.ust.comp4651;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * You only need to complete TWO TODO methods:
 *   1) delEmptyFilesRecursive
 *   2) delBySuffixRecursive
 * Do NOT modify other parts.
 */
public class HomeWork_2 {

    // Safety guard: only allow deletions inside this sandbox prefix.
    private static String sandboxPrefix(String user) {
        return "/user/" + user + "/hw2/";
    }

    /** Returns a FileSystem instance. Do NOT modify. */
    private static FileSystem fs() throws IOException {
        return HDFSUtils.getFileSystem();
    }

    /** Recursively counts number of files and total bytes under root. Do NOT modify. */
    private static long[] countFilesAndBytes(FileSystem fs, Path root) throws IOException {
        long files = 0, bytes = 0;
        if (!fs.exists(root)) return new long[]{0, 0};
        for (FileStatus st : fs.listStatus(root)) {
            if (st.isDirectory()) {
                long[] sub = countFilesAndBytes(fs, st.getPath());
                files += sub[0];
                bytes += sub[1];
            } else {
                files += 1;
                bytes += st.getLen();
            }
        }
        return new long[]{files, bytes};
    }

    /**
     * Generates random test files directly in HDFS (including empty files and
     * files with different suffixes). Do NOT modify.
     */
    public static void generateTestFilesHDFS(Path baseDir, int nTxt, int nLog, int nTmp, int nEmpty) throws IOException {
        FileSystem fs = fs();
        fs.mkdirs(baseDir);

        Random rnd = new Random(42);

        // Create several non-empty .txt/.log/.tmp files
        for (int i = 0; i < nTxt; i++) writeRandomFile(fs, new Path(baseDir, "t_" + i + ".txt"), 1024 + rnd.nextInt(4096));
        for (int i = 0; i < nLog; i++) writeRandomFile(fs, new Path(baseDir, "l_" + i + ".log"),  512 + rnd.nextInt(2048));
        for (int i = 0; i < nTmp; i++) writeRandomFile(fs, new Path(baseDir, "x_" + i + ".tmp"),  128 + rnd.nextInt(1024));

        // Create several empty files
        for (int i = 0; i < nEmpty; i++) {
            Path p = new Path(baseDir, "empty_" + i);
            if (fs.exists(p)) fs.delete(p, false);
            fs.create(p, true).close();
        }
    }

    /** Writes a single random-content file of size 'sizeBytes' in HDFS. Do NOT modify. */
    private static void writeRandomFile(FileSystem fs, Path p, int sizeBytes) throws IOException {
        if (fs.exists(p)) fs.delete(p, false);
        try (FSDataOutputStream out = fs.create(p, true)) {
            byte[] buf = new byte[4096];
            Random r = new Random(p.toString().hashCode());
            int written = 0;
            while (written < sizeBytes) {
                int n = Math.min(buf.length, sizeBytes - written);
                r.nextBytes(buf);
                out.write(buf, 0, n);
                written += n;
            }
        }
    }

    /**
     * TODO: Recursively delete ALL empty files (length == 0) under 'root'.
     * Requirements:
     *  - Only operate under /user/<you>/hw1/; otherwise throw IOException.
     *  - Traverse directories; delete files with st.getLen() == 0.
     *  - Must be robust if 'root' is missing or directories are empty (no crash).
     *  - Idempotent: re-running does nothing harmful.
     */
    public static void delEmptyFilesRecursive(Path root) throws IOException {
    
    }

    /**
     * TODO: Recursively delete ALL files whose names end with 'suffix' under 'root'.
     * Requirements:
     *  - Only operate under /user/<you>/hw1/; otherwise throw IOException.
     *  - Traverse directories; delete files with path.getName().endsWith(suffix).
     *  - Exact suffix match, case-sensitive. Example: ".tmp" matches "a.tmp" but NOT "a.tmp.bak".
     *  - Idempotent and robust.
     */
    public static void delBySuffixRecursive(Path root, String suffix) throws IOException {

    }

    /**
     * Main flow (Do NOT modify):
     * 1) Recreate sandbox.
     * 2) Generate test data in HDFS (txt/log/tmp/empty).
     * 3) Print BEFORE summary; invoke your two deletion methods; print AFTER summary.
     */
    public static void main(String[] args) throws Exception {
        FileSystem fs = fs();
        String user = System.getProperty("user.name");
        Path base = new Path(sandboxPrefix(user));

        // Repeatable runs: clean then create
        if (fs.exists(base)) fs.delete(base, true);
        fs.mkdirs(base);

        // 1) Generate data
        generateTestFilesHDFS(base, /*txt*/5, /*log*/3, /*tmp*/4, /*empty*/2);

        // 2) BEFORE summary
        long[] before = countFilesAndBytes(fs, base);
        System.out.printf("BEFORE files=%d bytes=%d%n", before[0], before[1]);

        // 3) Student implementations
        delEmptyFilesRecursive(base);
        delBySuffixRecursive(base, ".tmp");

        // 4) AFTER summary
        long[] after = countFilesAndBytes(fs, base);
        System.out.printf("AFTER  files=%d bytes=%d%n", after[0], after[1]);
    }
}