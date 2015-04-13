organization := "edu.cmu.ece"

version := "0.0.alpha1"

scalaVersion := "2.11.6"

name := "Tichu Super Node"

libraryDependencies += "edu.cmu.ece" % "tichu-common_2.11" % "0.0.alpha1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4-SNAPSHOT"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"