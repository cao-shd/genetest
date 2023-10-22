package github.plugin.genetest.util;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class FileUtils {

    public static void makeParentDir(File file) {
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
    }

    public static void walkFile(File rootFile, String fileExt, Consumer<File> consumer) {
        Arrays.stream(Objects.requireNonNull(rootFile.listFiles())).filter(file -> file.getName().endsWith("." + fileExt) || file.isDirectory()).forEach(file -> {
            if (file.isDirectory()) {
                walkFile(file, fileExt, consumer);
            } else {
                consumer.accept(file);
            }
        });
    }
}
