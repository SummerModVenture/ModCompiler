
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.time.Instant

// this will have to do until i figure out the github api
val mods = listOf(
        Mod("SpicyCore", "https://github.com/SummerModVenture/SpicyCore.git"),
        Mod("SpicyPineapple", "https://github.com/SummerModVenture/SpicyPineapple.git"),
        Mod("SpicyWailsMod", "https://github.com/SummerModVenture/SpicyWailsMod.git"),
        Mod("SpicyTech", "https://github.com/SummerModVenture/SpicyTech.git"),
        Mod("OuterSpice", "https://github.com/SummerModVenture/OuterSpice.git"),
        Mod("SpicyPlasma", "https://github.com/SummerModVenture/SpicyPlasma.git"),
        Mod("SpicyHero", "https://github.com/SummerModVenture/SpicyHero.git"),
        Mod("SpicyFarm", "https://github.com/SummerModVenture/SpicyFarm.git"),
        Mod("Spicy_Runes", "https://github.com/SummerModVenture/Spicy_Runes.git")
)

fun main(args: Array<String>) {
    val compiledModsFolder = File("mods")
    if (compiledModsFolder.exists()) {
        compiledModsFolder.deleteRecursively()
        compiledModsFolder.mkdir()
    } else {
        compiledModsFolder.mkdir()
    }

    val initialTime = Instant.now().epochSecond

    for (i in mods.indices) {
        val time1 = Instant.now().epochSecond

        val mod = mods[i]
        val folder = File(mod.name)
        val isNewMod = !folder.exists()

        println("Starting for mod ${i+1}/${mods.size}: ${mod.name}")
        val gitProcess = ProcessBuilder().inheritIO()
        if (!isNewMod) {
            println("Found git repository for ${mod.name}, pulling latest version.")
            gitProcess.directory(folder).command("git", "pull").start().waitFor()
        } else {
            println("New mod, cloning into ${folder.absolutePath}.")
            gitProcess.command("git", "clone", mod.gitUrl, "--recursive").start().waitFor()
        }

        val buildFolder = File("${mod.name}/build/libs/")
        if (buildFolder.exists())
            buildFolder.deleteRecursively()

        println("Compiling ${mod.name}")
        try {
            val gradleWrapper = File("${mod.name}/gradlew.bat")
            val gradleProcess = ProcessBuilder().inheritIO()
            val tasks = mutableListOf<String>(gradleWrapper.absolutePath)
            if (isNewMod)
                tasks.add("setupDecompWorkspace")
            tasks.add("build")
            gradleProcess.directory(folder).command(tasks).start().waitFor()

            val jarFile = File("${mod.name}/build/libs/")
                    .walkTopDown()
                    .filter { it.isFile && !it.name.contains("source") }
                    .first()
            jarFile.copyTo(File("mods/${jarFile.name}"))

            println("Generating ${jarFile.name} as zip file.")
            val zipFile = ZipFile(File("mods/${jarFile.name.substring(0 until jarFile.name.length - 3)}zip"))
            val params = ZipParameters()
            params.rootFolderInZip = "mods/"
            zipFile.addFile(jarFile, params)

            println("Finished compiling mod ${i+1}/${mods.size}: ${mod.name}")
        } catch (t: Throwable) {
            println("Could not generate mod file: $t")
            println("Skipping: ${mod.name}")
        }
        println("Time elapsed: ${Instant.now().epochSecond - time1} seconds.")
        println()
    }
    println("Total time elapsed: ${Instant.now().epochSecond - initialTime} seconds.")
}

data class Mod(val name: String, val gitUrl: String)