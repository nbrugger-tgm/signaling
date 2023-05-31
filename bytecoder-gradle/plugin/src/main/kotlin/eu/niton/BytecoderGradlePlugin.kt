package eu.niton

import de.mirkosertic.bytecoder.api.Logger
import de.mirkosertic.bytecoder.cli.BytecoderCLI
import de.mirkosertic.bytecoder.core.Slf4JLogger
import de.mirkosertic.bytecoder.core.backend.CompileOptions
import de.mirkosertic.bytecoder.core.backend.js.JSBackend
import de.mirkosertic.bytecoder.core.backend.js.JSIntrinsics
import de.mirkosertic.bytecoder.core.ir.AnalysisException
import de.mirkosertic.bytecoder.core.ir.AnalysisStack
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader
import de.mirkosertic.bytecoder.core.optimizer.Optimizations
import de.mirkosertic.bytecoder.core.parser.CompileUnit
import de.mirkosertic.bytecoder.core.parser.Loader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.Type
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.nio.file.Files


/**
 * A simple 'hello world' plugin.
 */
class BytecoderGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("bytecoder", BytecoderExtension::class.java)
        extension.outputDir.convention(project.layout.buildDirectory.dir("generated/bytecoder"));
        extension.language.convention("js")
        val transpile = project.tasks.register("transpile") { task ->
            task.inputs.property("out", extension.outputDir.get())
            task.inputs.property("main", extension.mainClass.get())
            task.inputs.property("lang", extension.language.get())
            task.dependsOn(project.tasks.withType(Jar::class.java));
            val classpath = getClasspath(project)
            task.inputs.files(classpath)
            val unifiedClasspath = project.layout.buildDirectory.dir("bytecoder");

            task.doFirst {
                println(classpath?.joinToString(";"))
                project.mkdir(unifiedClasspath)
                runTranspileCommand(extension.mainClass.get(), extension.outputDir.get().toString(), classpath?.joinToString (";" ))
            };
        }
        project.tasks.named("build") {
            it.dependsOn(transpile)
        }
    }

    fun getClasspath(project: Project): FileCollection? {
        val sourceSet =
            project.extensions.findByType(JavaPluginExtension::class.java)?.sourceSets?.named("main")?.get();
        return sourceSet?.runtimeClasspath//?.plus(sourceSet.compileClasspath)
    }

}

fun flatMapClasspath(item: File): Sequence<File> {
    return if (item.isFile) arrayOf(item).asSequence() else item.listFiles()?.asSequence()
        ?: listOf<File>().asSequence();
}

fun runTranspileCommand(mainClass: String, buildDirectory: String, classpath: String?) {
    try {
        val rootClassLoader = BytecoderCLI::class.java.classLoader
        val classLoader = URLClassLoader(classpath?.split(";")?.map { classpathItem ->
            File(classpathItem).toURI().toURL()
        }?.toTypedArray(), rootClassLoader)
        val loader: Loader = BytecoderLoader(classLoader)
        val logger: Logger = Slf4JLogger()
        logger.info("Compiling main class {} to directory {}", mainClass, buildDirectory)
        val compileUnit = CompileUnit(loader, logger, JSIntrinsics())
        val invokedType = Type.getObjectType(mainClass.replace('.', '/'))
        compileUnit.resolveMainMethod(
            invokedType,
            "main",
            Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;"))
        )
        compileUnit.finalizeLinkingHierarchy()
        compileUnit.logStatistics()
        val compileOptions =
            CompileOptions(logger, Optimizations.DEFAULT, arrayOf(), "bytecoder", false)
        val backend = JSBackend()
        val result = backend.generateCodeFor(compileUnit, compileOptions)
        for (content in result.content) {
            val outputFile: File = File(buildDirectory, content.fileName)
            FileOutputStream(outputFile).use { theFos -> content.writeTo(theFos) }
        }
    } catch (e: AnalysisException) {
        e.analysisStack.dumpAnalysisStack(System.out)
        throw e
    }
}

interface BytecoderExtension {
    val outputDir: DirectoryProperty
    val mainClass: Property<String>
    val language: Property<String>
}