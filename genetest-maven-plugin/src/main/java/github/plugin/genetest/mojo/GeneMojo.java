package github.plugin.genetest.mojo;

import github.plugin.genetest.tool.GeneTool;
import github.plugin.genetest.util.FileUtils;
import github.plugin.genetest.util.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

@Mojo(name = "gene")
public class GeneMojo extends AbstractMojo {

    public static final String DEFAULT_TEST_FILENAME_SUFFIX = "Test";

    public static final String MODE_APPEND = "append";

    public static final String MODE_OVERWRITE = "overwrite";

    private Log log;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "mock")
    private String mock;

    @Parameter(property = "mode", defaultValue = MODE_APPEND)
    private String mode;

    @Parameter(property = "suffix", defaultValue = DEFAULT_TEST_FILENAME_SUFFIX)
    private String suffix;

    @Parameter(property = "includes")
    private String includes;

    @Parameter(property = "excludes")
    private String excludes;

    @Override
    public void execute() {
        log = getLog();
        infoParams();
        generate();
    }

    private void generate() {
        String srcRootPath = project.getCompileSourceRoots().get(0).toString();
        String testRootPath = project.getTestCompileSourceRoots().get(0).toString();
        FileUtils.walk(
            new File(srcRootPath),
            "java",
            srcFile -> generateTestFile(srcFile, srcRootPath, testRootPath)
        );
    }

    private void infoParams() {
        log.info("mvn param mock: " + mock);
        log.info("mvn param mode: " + mode);
        log.info("mvn param suffix: " + suffix);
        log.info("mvn param includes: " + includes);
        log.info("mvn param excludes: " + excludes);
    }

    private void generateTestFile(File srcFile, String srcRootPath, String testRootPath) {
        String srcClassFullName = calcClassFullName(srcFile, srcRootPath);
        if (checkGenerate(srcClassFullName)) {
            File testFile = calcTestFile(srcFile, srcRootPath, testRootPath);
            generate(srcFile, testFile);
        }
    }

    private boolean checkGenerate(String srcClassFullName) {
        if (StringUtils.isBlank(includes) && StringUtils.isBlank(excludes)) {
            return true;
        }

        if (!StringUtils.isBlank(includes)) {
            if (StringUtils.includeStartsWith(includes, srcClassFullName)) {
                if (!StringUtils.isBlank(excludes)) {
                    return !StringUtils.includeStartsWith(excludes, srcClassFullName);
                }
                return true;
            } else {
                return false;
            }
        }

        if (!StringUtils.isBlank(excludes)) {
            return !StringUtils.includeStartsWith(excludes, srcClassFullName);
        }

        return false;
    }

    private void generate(File srcFile, File testFile) {
        if (testFile.exists()) {
            if (MODE_OVERWRITE.equals(mode)) {
                log.info("overwrite test file: " + testFile);
                new GeneTool(mock, srcFile, testFile, false, log).generate();
            } else {
                log.info("append test file: " + testFile);
                new GeneTool(mock, srcFile, testFile, true, log).generate();
            }
        } else {
            FileUtils.createParentDir(testFile);
            log.info("create test file: " + testFile);
            new GeneTool(mock, srcFile, testFile, false, log).generate();
        }
    }

    private String calcClassFullName(File srcFile, String srcRootPath) {
        String srcRootTemp = srcRootPath.replaceAll("\\\\", ".") + ".";
        String srcFilePath = srcFile.getAbsolutePath();
        String srcPackageTemp = srcFilePath.replaceAll("\\\\", ".");
        return srcPackageTemp.replaceAll(srcRootTemp, "");
    }

    private File calcTestFile(File srcFile, String srcRootPath, String testRootPath) {
        String srcFileName = srcFile.getName();
        List<String> srcFileNameSplit = StringUtils.split(srcFileName, "\\.");
        String testFileName = srcFileNameSplit.get(0) + suffix + "." + srcFileNameSplit.get(1);
        String srcFilePath = srcFile.getAbsolutePath();
        String testFilePath = srcFilePath.replace(srcRootPath, testRootPath).replace(srcFileName, testFileName);
        return new File(testFilePath);
    }

}
