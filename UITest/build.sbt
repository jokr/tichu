name := "scalafx-ensemble"

version := "1.0-SNAPSHOT"

organization := "org.scalafx"

scalaVersion := "2.11.6"



libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.40-R8",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
)
unmanagedJars in Compile += Attributed.blank(file(scala.util.Properties.javaHome) / "/lib/jfxrt.jar")

resolvers += Opts.resolver.sonatypeSnapshots

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")


// Fork a new JVM for 'run' and 'test:run'
fork := true

//fork in console := true