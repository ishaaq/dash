import sbt._
import java.io.File

class GripeProject(info: ProjectInfo) extends DefaultProject(info) {
    override def mainClass = Some("com.fastsearch.gripe.GripeCLI")
    override def manifestClassPath = Some("scala-library.jar")

    override def packageOptions =  ManifestAttributes(("Agent-Class", "com.fastsearch.gripe.GripeAgent")) :: super.packageOptions.toList

    val mavenLocal = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
    val nexusReleases = "Melbourne R&D Repository" at "http://mel1u114:8081/nexus/content/groups/central"
    val nexusSnapshots = "Melbourne R&D Snapshots Repository" at "http://mel1u114:8081/nexus/content/groups/snapshots"

    // java.home may point to the jre home, we want the jvm home to get to tools.jar
    // to be safe we resolve both locations...
    val javaHomeStr = System.getProperty("java.home");
    val javaHome = Path.fromFile(javaHomeStr)
    val jvmHome = Path.fromFile(javaHomeStr + "/..")
    def toolsJar = javaHome / "lib" / "tools.jar" +++ jvmHome / "lib" / "tools.jar"

    override def compileClasspath = super.compileClasspath +++ toolsJar
}
