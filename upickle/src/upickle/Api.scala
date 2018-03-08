package upickle


import jawn.Facade

import language.experimental.macros
import scala.reflect.ClassTag
import language.higherKinds
/**
 * An instance of the upickle API. There's a default instance at
 * `upickle.default`, but you can also implement it yourself to customize
 * its behavior. Override the `annotate` methods to control how a sealed
 * trait instance is tagged during reading and writing.
 */
trait Api extends Types with Implicits with LowPriX{
  def read[T: Reader](s: String) = {
    jawn.Parser.parseUnsafe(s)(implicitly[Reader[T]])
  }
  def write[T: Writer](t: T) = {
    val out = new java.io.StringWriter()
    implicitly[Writer[T]].write(new Renderer(out), t)
    out.toString
  }
//  def annotate[V: ClassTag](rw: Reader[V], n: String): Reader[V]
//  def annotate[V: ClassTag](rw: Writer[V], n: String): Writer[V]
}

/**
 * The default way of accessing upickle
 */
object default extends AttributeTagged{

}
/**
 * An instance of the upickle API that follows the old serialization for
 * tagged instances of sealed traits.
 */
object legacy extends Api{
//  def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
//    case Js.Arr(Js.Str(`n`), x) => rw.read(x)
//  }
//
//  def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
//    case x: V => Js.Arr(Js.Str(n), rw.write(x))
//  }
}

/**
 * A `upickle.Api` that follows the default sealed-trait-instance-tagging
 * behavior of using an attribute, but allow you to control what the name
 * of the attribute is.
 */
trait AttributeTagged extends Api{
  def tagName = "$type"
//  def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
//    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n))) =>
//    rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
//
//  }
//
//  def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
//    Js.Obj((tagName, Js.Str(n)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
//  }
}

/**
 * Stupid hacks to work around scalac not forwarding macro type params properly
 */
object Forwarder{
  def dieIfNothing[T: c.WeakTypeTag]
                  (c: scala.reflect.macros.blackbox.Context)
                  (name: String) = {
    if (c.weakTypeOf[T] =:= c.weakTypeOf[Nothing]) {
      c.abort(
        c.enclosingPosition,
        s"uPickle is trying to infer a $name[Nothing]. That probably means you messed up"
      )
    }
  }
  def applyR[T](c: scala.reflect.macros.blackbox.Context)
              (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Reader")
    c.Expr[T](q"${c.prefix}.macroR0[$e, ${c.prefix}.Reader]")
  }
  def applyW[T](c: scala.reflect.macros.blackbox.Context)
              (implicit e: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._
    dieIfNothing[T](c)("Writer")
    c.Expr[T](q"${c.prefix}.macroW0[$e, ${c.prefix}.Writer]")
  }

}
trait LowPriX extends LowPriY {this: Api =>
}
trait LowPriY{ this: Api =>
//  implicit def macroSingletonR[T <: Singleton]: Reader[T] = macro Forwarder.applyR[T]
//  implicit def macroSingletonW[T <: Singleton]: Writer[T] = macro Forwarder.applyW[T]
//  implicit def macroSingletonRW[T <: Singleton]: ReadWriter[T] = macro Forwarder.applyRW[T]
  def macroR[T]: Reader[T] = macro Forwarder.applyR[T]
  def macroW[T]: Writer[T] = macro Forwarder.applyW[T]
  def macroRW[T: Reader: Writer]: ReadWriter[T] = new Reader[T] with Writer[T] {
    override def jnull(index: Int) = implicitly[Reader[T]].jnull(index)
    override def jtrue(index: Int) = implicitly[Reader[T]].jtrue(index)
    override def jfalse(index: Int) = implicitly[Reader[T]].jfalse(index)

    override def jstring(s: CharSequence, index: Int) = implicitly[Reader[T]].jstring(s, index)
    override def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) = {
      implicitly[Reader[T]].jnum(s, decIndex, expIndex, index)
    }

    override def objectContext(index: Int) = implicitly[Reader[T]].objectContext(index)
    override def arrayContext(index: Int) = implicitly[Reader[T]].arrayContext(index)
    override def singleContext(index: Int) = implicitly[Reader[T]].singleContext(index)

    def write(out: Facade[Unit], v: T): Unit = {
      implicitly[Writer[T]].write(out, v)
    }
  }
  def macroR0[T, M[_]]: Reader[T] = macro Macros.macroRImpl[T, M]
  def macroW0[T, M[_]]: Writer[T] = macro Macros.macroWImpl[T, M]
}