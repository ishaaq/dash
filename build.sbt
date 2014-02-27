import sbt.Package.ManifestAttributes
import AssemblyKeys._

name := "dash"

version := "0.4.3"

organization := "biz.chandy"

crossScalaVersions := Seq("2.9.2", "2.10.3")

libraryDependencies ++= Seq(
    "jline" % "jline" % "1.0" intransitive(),
    "args4j" % "args4j" % "2.0.16" intransitive(),
    "org.apache.mina" % "mina-core" % "2.0.4" intransitive(),
    "org.slf4j" % "slf4j-api" % "1.7.5" intransitive(),
    "org.slf4j" % "slf4j-jdk14" % "1.7.5" intransitive()
)

unmanagedJars in Compile  += file(sys.props("java.home") + "/../lib/tools.jar")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

packageOptions := Seq(ManifestAttributes(
                    "Agent-Class" -> "dash.Agent",
                    "Main-Class" -> "dash.Cli"))

assemblySettings

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {_.data.getName == "tools.jar"}
}

jarName in assembly := "dash_uber_" + CrossVersion.binaryScalaVersion(scalaVersion.value) +  "_" + version.value + ".jar"
