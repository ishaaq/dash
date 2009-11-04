import sbt._
import java.io.File

class Project(info: ProjectInfo) extends DefaultProject(info) {
    override def mainClass = Some("dash.Cli")

    override def packageOptions =  ManifestAttributes(("Agent-Class", "dash.Agent")) :: super.packageOptions.toList

    val mavenLocal = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"

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
    val rhino = "rhino" % "js" % "1.7R2" intransitive
}
