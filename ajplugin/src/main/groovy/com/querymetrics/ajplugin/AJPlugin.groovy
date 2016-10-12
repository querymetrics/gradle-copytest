package com.querymetrics.ajplugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import java.nio.file.Files
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.slf4j.Logger

class AJPlugin implements Plugin<Project> {
    private static final String LOG_TAG = "AJPlugin: ";
    private static final String EXPLODED_AAR_PATH = File.separator + "build" + File.separator + "intermediates" + File.separator + "exploded-aar"

    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        if (!hasApp && !hasLib) {
            throw new IllegalStateException(LOG_TAG + "'android' or 'android-library' plugin required.")
        }

        final def log = project.logger
        final def variants
        if (hasApp) {
            variants = project.android.applicationVariants
        } else {
            variants = project.android.libraryVariants
        }

        project.dependencies {
            compile 'org.aspectj:aspectjrt:1.8.9'
        }

        variants.all { variant ->
            log.info(LOG_TAG + "variant -----> " + variant + " " + variant.buildType.name)

            JavaCompile javaCompile = variant.javaCompile

            // Create an array of classpaths
            def classPaths = javaCompile.classpath.asPath
            classPaths = classPaths.tokenize(File.pathSeparator)

            // Find ajexample library that contains the aspectj classes
            def aspectPath = classPaths.find { fileName ->
                if (fileName.contains(EXPLODED_AAR_PATH) &&
                        fileName.contains("ajaspects") &&
                        fileName.endsWith(File.separator + 'classes.jar')) {
                    return true
                }
                return false
            }

            javaCompile.doFirst {
                if (!aspectPath)
                {
                    throw new IllegalStateException(LOG_TAG + "ajaspects library required.")
                }
                log.info(LOG_TAG + "doFirst, aspectPath:" + aspectPath)

                classPaths.each { fileName ->
                    if (fileName.contains(EXPLODED_AAR_PATH) && fileName.endsWith(File.separator + "classes.jar")) {
                        weavefile(log, fileName, aspectPath, javaCompile.classpath.asPath, project.android.bootClasspath.join(File.pathSeparator));
                    }
                }
            }
        }
    }

    private static boolean weave(String[] args, Logger log) {
        MessageHandler handler = new MessageHandler(true);
        log.debug(LOG_TAG + "ajc args: " + Arrays.toString(args))
        new Main().run(args, handler);
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    return false;
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break;
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break;
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break;
            }
        }

        return true;
    }

    private static boolean weave(Logger log, String inPath, String aspectPath, String classPath, String bootClassPath, String outJar)
    {
        String[] args = [
                "-showWeaveInfo",
                "-Xlint:adviceDidNotMatch=ignore,cantFindType=ignore",
                "-1.7",
                "-inpath", inPath,
                "-aspectpath", aspectPath,
                "-outjar", outJar,
                "-classpath", classPath,
                "-bootclasspath", bootClassPath
        ]

        return weave(args, log)
    }

    private static void weavefile(log, fileName, aspectPath, classPath, bootClassPath)
    {
        String absolutePathStr = fileName;
        String absolutePathOriginalStr = absolutePathStr.replace(".jar", "_orig.jar");
        String absolutePathWeavedStr = absolutePathStr.replace(".jar", "_weaved.jar");
        try {
            Path absolutePath = Paths.get(absolutePathStr);
            Path absolutePathOriginal = Paths.get(absolutePathOriginalStr);

            log.info(LOG_TAG + "Copying " + absolutePathStr + " onto " + absolutePathOriginalStr);
            Files.copy(absolutePath, absolutePathOriginal, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

            log.info(LOG_TAG + "Weaving " + absolutePathStr + " onto " + absolutePathWeavedStr);
            boolean success = weave(log, absolutePathStr, aspectPath, classPath, bootClassPath, absolutePathWeavedStr);
            if (!success) {
                throw new Exception(LOG_TAG + "Unable to instrument Jar file: " + absolutePathStr, null);
            }

            Path absolutePathWeaved = Paths.get(absolutePathWeavedStr);
            log.info(LOG_TAG + "Copying " + absolutePathWeavedStr + " back onto " + absolutePathStr);
            Files.copy(absolutePathWeaved, absolutePath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (FileSystemException e) {
            log.warn(LOG_TAG + "Exception in weavefile " + absolutePathStr, e);
        }
    }
}
