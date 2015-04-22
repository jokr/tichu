import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
//import scalafx.scene.paint.Color
//import scalafx.scene.shape.Rectangle
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.scene.control.{Label,Button}

object TichuLogin extends JFXApp {
  
  stage = new JFXApp.PrimaryStage {
    title.value = "Tichu Online Card Game"
    width = 900
    height = 650
    scene = new Scene {
       root = {
          var url = this.getClass.getResource("/images/logo.jpg").toExternalForm
          
          val tichulogo = new ImageView(new Image(url)) {
              // Image can be resized to preferred width  
              fitWidth = 500
              preserveRatio = true
          }

          val button = new Button {
              text = "Play Tichu"
              defaultButton = true
          }

          val label = new Label {
            text = "Copyright@2015 :CMU-18842 TEAM18"
          }
          new VBox {
            alignment = Pos.TOP_CENTER
            spacing = 20
            padding = Insets(20)
            children = List(
               tichulogo,button,label
            )
          }
        }
    }

  }
}