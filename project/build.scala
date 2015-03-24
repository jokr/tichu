import sbt._
import Keys._

object Tichu extends Build {
    lazy val root = Project(id = "tichu",
                            base = file(".")) aggregate(node)

    lazy val node = Project(id = "tichu-node",
                           base = file("node"))
}