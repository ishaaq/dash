import sbt._
import java.io.File

class Project(info: ProjectInfo) extends DefaultProject(info) {
    override def mainClass = Some("dash.Cli")

    override def packageOptions =  ManifestAttributes(("Agent-Class", "dash.Agent")) :: super.packageOptions.toList

    val mavenLocal = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
    val nexusReleases = "Melbourne R&D Repository" at "http://mel1u114:8081/nexus/content/groups/central"
    val nexusSnapshots = "Melbourne R&D Snapshots Repository" at "http://mel1u114:8081/nexus/content/groups/snapshots"

    def toolsJar = {
      // java.home may point to the jre home, we want the jvm home to get to tools.jar
      // to be safe we resolve both locations...
      val javaHomeStr = System.getProperty("java.home");
      val javaHome = Path.fromFile(javaHomeStr)
      val jvmHome = Path.fromFile(javaHomeStr + "/..")
      javaHome / "lib" / "tools.jar" +++ jvmHome / "lib" / "tools.jar"
    }

    override def compileClasspath = super.compileClasspath +++ toolsJar

    val jline = "jline" % "jline" % "0.9.93" intransitive
    val args4j = "args4j" % "args4j" % "2.0.16" intransitive
    val mina = "org.apache.mina" % "mina-core" % "2.0.0-RC1" intransitive
    val slf4j = "org.slf4j" % "slf4j-api" % "1.5.8" intransitive
    val slf4j_jdk14 = "org.slf4j" % "slf4j-jdk14" % "1.5.8" intransitive

    def assemblyExclude(base: PathFinder) = base / "META-INF" ** "*"

    def assemblyOutputPath = outputPath / assemblyJarName
    def assemblyJarName = artifactID + ".jar"
    def assemblyTemporaryPath = outputPath / "assembly-libs"
    def assemblyClasspath = super.compileClasspath +++ mainResources
    def assemblyExtraJars = mainDependencies.scalaLibrary
    def assemblyPaths(tempDir: Path, classpath: PathFinder, extraJars: PathFinder, exclude: PathFinder => PathFinder) = {
        val (libs, directories) = classpath.get.toList.partition(ClasspathUtilities.isArchive)
        for(jar <- extraJars.get ++ libs) FileUtilities.unzip(jar, tempDir, log).left.foreach(error)
        val base = (Path.lazyPathFinder(tempDir :: directories) ##)
        (descendents(base, "*") --- exclude(base)).get
    }

    lazy val assembly = assemblyTask(assemblyTemporaryPath, assemblyClasspath, assemblyExtraJars, assemblyExclude) dependsOn(compile)
    def assemblyTask(tempDir: Path, classpath: PathFinder, extraJars: PathFinder, exclude: PathFinder => PathFinder) =
    packageTask(Path.lazyPathFinder(assemblyPaths(tempDir, classpath, extraJars, exclude)), assemblyOutputPath, packageOptions)
}
