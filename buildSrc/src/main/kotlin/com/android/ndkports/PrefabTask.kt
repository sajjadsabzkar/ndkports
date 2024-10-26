package com.android.ndkports

import com.google.prefab.api.Android
import com.google.prefab.api.BuildSystemInterface
import com.google.prefab.api.Package
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import java.util.zip.ZipFile

abstract class PrefabTask : DefaultTask() {
    @InputFiles
    lateinit var aars: FileCollection  // Keep as FileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    val generatedDirectory: Provider<Directory>
        get() = outputDirectory.dir("generated")

    @get:Optional
    @get:Input
    abstract val generator: Property<Class<out BuildSystemInterface>>

    @get:InputDirectory
    abstract val ndkPath: DirectoryProperty

    @get:Input
    abstract val minSdkVersion: Property<Int>

    private val ndk: Ndk
        get() = Ndk(ndkPath.asFile.get())

    @TaskAction
    fun run() {
        if (!generator.isPresent) {
            generatedDirectory.get().asFile.mkdirs()
            return
        }

        val outDir = outputDirectory.get().asFile
        val packages = mutableListOf<Package>()

        for (aar in aars) {
            logger.lifecycle("Processing AAR: ${aar.absolutePath}")
            val packagePath = outDir.resolve(aar.nameWithoutExtension)

            if (packagePath.exists()) {
                packagePath.deleteRecursively()
            }
            packagePath.mkdirs()

            extract(aar, packagePath)

            val prefabPath = packagePath.resolve("prefab")
            if (!prefabPath.exists()) {
                throw RuntimeException("Prefab directory not found in AAR at ${prefabPath.absolutePath}")
            }

            packages.add(Package(prefabPath.toPath()))
        }

        generateSysroot(packages, minSdkVersion.get(), ndk.version.major)
    }

    private fun extract(aar: File, extractDir: File) {
        logger.lifecycle("Extracting AAR to: ${extractDir.absolutePath}")

        ZipFile(aar).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val outFile = extractDir.resolve(entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile.mkdirs()
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        // Verify the AndroidManifest.xml exists
        val manifestFile = extractDir.resolve("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            throw RuntimeException("AndroidManifest.xml not found in AAR at ${manifestFile.absolutePath}")
        }
    }

    private fun generateSysroot(
        packages: List<Package>,
        osVersion: Int,
        ndkVersion: Int
    ) {
        val generatorType = generator.get()
        val constructor = generatorType.getConstructor(File::class.java, List::class.java)
        val buildSystemIntegration = constructor.newInstance(
            generatedDirectory.get().asFile,
            packages
        )

        buildSystemIntegration.generate(Android.Abi.values().map { abi ->
            Android(abi, osVersion, Android.Stl.CxxShared, ndkVersion)
        })
    }
}