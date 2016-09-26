import org.gradle.api.Plugin;
import org.gradle.api.Project;
import java.nio.file.Files
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class CopyTest implements Plugin<Project> {
    private static final String LOG_TAG = "CopyTest: ";
    private static final String EXPLODED_AAR_PATH = File.separator + "build" + File.separator + "intermediates" + File.separator + "exploded-aar"

    void apply(Project project) {
        final def log = project.logger
        def variants = project.android.applicationVariants

        variants.all { variant ->
            log.info(LOG_TAG + "variant -----> " + variant + " " + variant.buildType.name)

            javaCompile.doFirst {
                log.info(LOG_TAG + "doFirst")

                javaCompile.classpath.asPath.tokenize(File.pathSeparator).each { fileName ->
                    if (fileName.contains(EXPLODED_AAR_PATH) && fileName.endsWith(File.separator + "classes.jar")) {
                        weavefile(log, fileName);
                    }
                }
            }
        }
    }

    private static void weavefile(log, fileName)
    {
        String absolutePathStr = fileName;
        String absolutePathWeavedStr = absolutePathStr.replace(".jar", "_weaved.jar");
        try {
            Path absolutePath = Paths.get(absolutePathStr);
            Path absolutePathWeaved = Paths.get(absolutePathWeavedStr);

            log.info(LOG_TAG + "Copying " + absolutePathStr + " onto " + absolutePathWeaved);
            Files.copy(absolutePath, absolutePathWeaved, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

            log.info(LOG_TAG + "Copying " + absolutePathWeavedStr + " back onto " + absolutePathStr);
            Files.copy(absolutePathWeaved, absolutePath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (FileSystemException e) {
            log.warn(LOG_TAG + "Exception in weavefile " + absolutePathStr, e);
        }
    }
}