package github.plugin.genetest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class FileUtils {

    public static void createParentDir(File file) {
        File parentFile = file.getParentFile();
        if (parentFile.mkdirs()) {
            System.out.println("create directory: " + parentFile);
        }
    }

    public static void walk(File rootFile, String fileExt, Consumer<File> consumer) {
        Arrays.stream(Objects.requireNonNull(rootFile.listFiles()))
            .filter(file -> file.getName().endsWith("." + fileExt) || file.isDirectory())
            .forEach(file -> {
                if (file.isDirectory()) {
                    walk(file, fileExt, consumer);
                } else {
                    consumer.accept(file);
                }
            });
    }

    public static void output(File file, String content) {
        // create parent dir
        createParentDir(file);

        // create output file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
