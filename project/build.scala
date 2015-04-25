import sbt.Keys._
import sbt._

object Build extends Build {
  lazy val commonSettings = Seq(
    organization := "edu.cmu.ece",
    version := "0.0.alpha1",
    scalaVersion := "2.11.6"
  )

  val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
    "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT"
  )

  lazy val messages = (project in file("messages")).settings(commonSettings: _*).settings(
    name := "Tichu Messages",
    libraryDependencies ++= akkaDependencies,
    resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
  )

  lazy val client = (project in file("client")).settings(commonSettings: _*).settings(
    name := "Tichu Client",
    libraryDependencies ++= akkaDependencies,
    libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.0-R4",
    libraryDependencies += "org.controlsfx" % "controlsfx" % "8.20.8",
    resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
  ).dependsOn(ordinarynode)

  lazy val supernode = (project in file("supernode")).settings(commonSettings: _*).settings(
    name := "Tichu Super Node",
    libraryDependencies ++= akkaDependencies,
    resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
  ).dependsOn(messages)

  lazy val ordinarynode = (project in file("ordinarynode")).settings(commonSettings: _*).settings(
    name := "Tichu Ordinary Node",
    libraryDependencies ++= akkaDependencies,
    resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
  ).dependsOn(messages)
}