package tichu.gui

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, Priority}

class GameScreen {
  lazy val screen = new Scene {
    root = new BorderPane {

      vgrow = Priority.ALWAYS
      hgrow = Priority.ALWAYS
    }
  }
}
