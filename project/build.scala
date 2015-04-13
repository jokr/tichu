import sbt._
import Keys._

object Tichu extends Build {
    lazy val root = Project(id = "tichu",
                            base = file(".")) aggregate(supernode, ordinarynode)

    lazy val supernode = Project(id = "tichu-supernode",
                           base = file("supernode"))

    lazy val ordinarynode = Project(id = "tichu-ordinary-node",
                       base = file("ordinarynode"))
}