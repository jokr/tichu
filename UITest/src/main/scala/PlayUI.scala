/*
 * Copyright 2013 ScalaFX Project
 * All right reserved.
 */
 
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, Button,TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{AnchorPane, BorderPane, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
 
/** An example of  a BorderPane layout, with placement of children in the top,
  * left, center, right, and bottom positions.
  *
  * @see scalafx.scene.layout.BorderPane
  * @resource /scalafx/ensemble/images/icon-48x48.png
  */
object PlayUI extends JFXApp {
 
  stage = new JFXApp.PrimaryStage {
    title = "Tichu Game"
    width = 1200
    height = 750
    scene = new Scene {
      root = {
        // Top content using a rectangle
        val topRectangle = new Rectangle() {
          width = 900
          height = 100
          fill = Color.White
          stroke = Color.Black
        }

        val bottomRectangle = new Rectangle() {
          width = 900
          height = 100
          fill = Color.White
          stroke = Color.Black
        }

        val leftRectangle = new Rectangle() {
          width = 100
          height = 500
          fill = Color.White
          stroke = Color.Black
        }

        val rightRectangle = new Rectangle() {
          width = 100
          height = 500
          fill = Color.White
          stroke = Color.Black
        }

        val horizontalCard = new Rectangle() {
          width = 50
          height = 25
          fill = Color.Red
          stroke = Color.Black
        }

     
        // Center content using Anchor Pane
        val centerLabel = Label("Welcome to Tichu Game")
        //val imageButton = new ImageView {
        //  image = new Image(this.getClass.getResourceAsStream("/images/logo.jpg"))
        //}
        var startButton = new Button() {
            text = "Start"
            maxWidth = Double.MaxValue
        }        
        AnchorPane.setTopAnchor(startButton, 400.0)
        AnchorPane.setLeftAnchor(startButton, 350.0)

        AnchorPane.setTopAnchor(centerLabel, 200.0)
        AnchorPane.setLeftAnchor(centerLabel, 350.0)
        AnchorPane.setTopAnchor(rightRectangle, 0.0)
        AnchorPane.setLeftAnchor(rightRectangle, 700.0)  

        //AnchorPane.setLeftAnchor(imageButton, 80.0)
        val centerAnchorPane = new AnchorPane {
          children = List(centerLabel,rightRectangle,startButton)
        }

        val sysInfo = new Label(){
          text = "Sys Info : "
          id = "label1"
          minWidth = 200
          minHeight = 20
          maxWidth = 200
          maxHeight = 20
        }

        val message = new Label(){
          text = "Message: "
          id = "label2"
          maxWidth = 200
          maxHeight = 500
        }
        val text = new TextField() {
            promptText = "You can talk public here !"
            maxWidth = 200
        }
        // Right content using VBox
        val rightVBox = new VBox {
          spacing = 10
          children = List(Label("Control Panel"), sysInfo, message, text)
        }
     
        // Right content
        val bottomLabel = Label("I am a status message. I am at the bottom")
     
        new BorderPane {
          maxWidth = 400
          maxHeight = 300
          padding = Insets(20)
          top = topRectangle
          left = leftRectangle
          center = centerAnchorPane
          right = rightVBox
          bottom = bottomRectangle
        }
      }
    }
  }
}