package github.plugin.genetest.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "help")
public class HelpMojo extends AbstractMojo {

    @Override
    public void execute() {
        String help
            = "This plugin has 2 goals:\n\n"
            + "Command description:\n\n"
            + "  [genetest:help]\n"
            + "     Display help information on genetest-maven-plugin.\n\n"
            + "  [genetest:gene]\n"
            + "     Generate test class by given parameters.\n"
            + "     Parameter description:\n\n"
            + "       [mock]      Create test file use mock util,\n"
            + "                   required false, support value: [mockito], default value: [null].\n"
            + "                   use: mvn genetest:gene -Dmock=mockito\n"
            + "       [mode]      If test file exists, append or overwrite file by given mode,\n"
            + "                   required false, support value: [append|overwrite], default value: [append].\n"
            + "                   use: mvn genetest:gene -Dmode=overwrite\n"
            + "       [suffix]    Create test file name suffix,\n"
            + "                   required false, support value: [anyString], default value: [Test].\n"
            + "                   use: mvn genetest:gene -suffix=Test\n"
            + "       [includes]  Create a test file by specifying the included package name or class full name, \n"
            + "                   required false, support value: [<package_name>|<class_full_name>], default value: [null].\n"
            + "                   use: mvn genetest:gene -Dincludes=<package_name>|<class_full_name>\n"
            + "       [excludes]  Create a test file by specifying the excluded package name or class full name, \n"
            + "                   required false, support value: [<package_name>|<class_full_name>], default value: [null].\n"
            + "                   use: mvn genetest:gene -Dexcludes=<package_name>|<class_full_name>\n\n"
            + "       All the above parameters can be freely combined.\n"
            + "                    use: mvn genetest:gene -Dmock=mockito -Dcover=true -Dincludes=<package_name>|<class_full_name> -Dexcludes=<package_name>|<class_full_name>\n";

        getLog().info(help);
    }

}
