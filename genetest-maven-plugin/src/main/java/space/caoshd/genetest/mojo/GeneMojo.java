package space.caoshd.genetest.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import space.caoshd.genetest.tool.GeneTool;
import space.caoshd.genetest.util.FileUtils;
import space.caoshd.genetest.util.StringUtils;

import java.io.File;
import java.util.List;

@Mojo(name = "gene")
public class GeneMojo extends AbstractMojo {

    public static final String TEST_FILENAME_SUFFIX = "Test";

    private Log log;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "mock")
    private String mock;
    @Parameter(property = "cover", defaultValue = "false")
    private boolean cover;

    @Parameter(property = "includes")
    private String includes;

    @Parameter(property = "excludes")
    private String excludes;

    @Override
    public void execute() {
        log = getLog();
        log.info("mvn param cover: " + cover);
        log.info("mvn param includes: " + includes);
        log.info("mvn param excludes: " + excludes);

        String srcRootPath = project.getCompileSourceRoots().get(0).toString();
        String testRootPath = project.getTestCompileSourceRoots().get(0).toString();
        FileUtils.walkFile(new File(srcRootPath), "java", (srcFile) -> {
            generateTestFile(srcFile, srcRootPath, testRootPath);
        });
    }

    private void generateTestFile(File srcFile, String srcRootPath, String testRootPath) {
        String srcClassFullName = calcClassFullName(srcFile, srcRootPath);
        if (checkGenerate(srcClassFullName)) {
            // generate test file
            File testFile = calcTestFile(srcFile, srcRootPath, testRootPath);
            generate(srcFile, testFile);
        }
    }

    private boolean checkGenerate(String srcClassFullName) {
        if (StringUtils.isBlank(includes) && StringUtils.isBlank(excludes)) {
            // when includes and excludes param all no set
            return true;
        }

        if (!StringUtils.isBlank(includes)) {
            // when includes param set
            if (StringUtils.includeStartsWith(includes, srcClassFullName)) {
                if (!StringUtils.isBlank(excludes)) {
                    // when excludes param set
                    return !StringUtils.includeStartsWith(excludes, srcClassFullName);
                }
                return true;
            } else {
                return false;
            }
        }

        if (!StringUtils.isBlank(excludes)) {
            // when excludes param set
            return !StringUtils.includeStartsWith(excludes, srcClassFullName);
        }

        return false;
    }

    private void generate(File srcFile, File testFile) {
        if (testFile.exists()) {
            if (cover) {
                log.info("recreate file: " + testFile);
                new GeneTool(mock, srcFile, testFile, log).generate();
            } else {
                log.warn("file already exist: " + testFile);
            }
        } else {
            FileUtils.makeParentDir(testFile);
            log.info("create file: " + testFile);
            new GeneTool(mock, srcFile, testFile, log).generate();
        }
    }


    private static String calcClassFullName(File srcFile, String srcRootPath) {
        String srcRootTemp = srcRootPath.replaceAll("\\\\", ".") + ".";
        String srcFilePath = srcFile.getAbsolutePath();
        String srcPackageTemp = srcFilePath.replaceAll("\\\\", ".");
        return srcPackageTemp.replaceAll(srcRootTemp, "");
    }

    private static File calcTestFile(File srcFile, String srcRootPath, String testRootPath) {
        String srcFileName = srcFile.getName();
        List<String> srcFileNameSplit = StringUtils.split(srcFileName, "\\.");
        String testFileName = srcFileNameSplit.get(0) + TEST_FILENAME_SUFFIX + "." + srcFileNameSplit.get(1);
        String srcFilePath = srcFile.getAbsolutePath();
        String testFilePath = srcFilePath.replace(srcRootPath, testRootPath).replace(srcFileName, testFileName);
        return new File(testFilePath);
    }

}
