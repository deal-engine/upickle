package upickle.json

trait CustomVisitor[-T, +V] extends upickle.json.Visitor[T, V] {
  def expectedMsg: String
  def visitNull(index: Int): V = null.asInstanceOf[V]
  def visitTrue(index: Int): V =  throw new AbortJsonProcessingException(expectedMsg + " got boolean")
  def visitFalse(index: Int): V = throw new AbortJsonProcessingException(expectedMsg + " got boolean")

  def visitString(s: CharSequence, index: Int): V = {
    throw new AbortJsonProcessingException(expectedMsg + " got string")
  }
  def visitNum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): V = {
    throw new AbortJsonProcessingException(expectedMsg + " got number")
  }

  def visitObject(index: Int): upickle.json.ObjVisitor[T, V] = {
    throw new AbortJsonProcessingException(expectedMsg + " got dictionary")
  }
  def visitArray(index: Int): upickle.json.ArrVisitor[T, V] = {
    throw new AbortJsonProcessingException(expectedMsg + " got sequence")
  }

}
