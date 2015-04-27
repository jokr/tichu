package tichu.model

import scala.collection.mutable

class Token extends Serializable {
  var stack = mutable.Stack[Combination]()
}
