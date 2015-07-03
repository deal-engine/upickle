package example

import upickle.TestUtil
import utest._

object Simple {
  case class Thing(a: Int, b: String)
  case class Big(i: Int, b: Boolean, str: String, c: Char, t: Thing)
}
object Sealed{
  sealed trait IntOrTuple
  case class IntThing(i: Int) extends IntOrTuple
  case class TupleThing(name: String, t: (Int, Int)) extends IntOrTuple
}
object Recursive{
  case class Foo(i: Int)
  case class Bar(name: String, foos: Seq[Foo])
}
object Defaults{
  case class FooDefault(i: Int = 10, s: String = "lol")
}
object Keyed{
  import upickle.key
  case class KeyBar(@key("hehehe") kekeke: Int)
}
object KeyedTag{
  import upickle.key
  sealed trait A
  @key("Bee") case class B(i: Int) extends A
  case object C extends A
}
object Custom{
  class CustomThing(val i: Int, val s: String)
  object CustomThing{
    def apply(i: Int) = new CustomThing(i + 10, "s" * (i + 10))
    def unapply(t: CustomThing) = Some(t.i - 10)
  }
}
object Custom2{
  import upickle.Js
  class CustomThing2(val i: Int, val s: String)
  object CustomThing2{
    implicit val thing2Writer = upickle.Writer[CustomThing2]{
      case t => Js.Str(t.i + " " + t.s)
    }
    implicit val thing2Reader = upickle.Reader[CustomThing2]{
      case Js.Str(str) =>
        val Array(i, s) = str.split(" ")
        new CustomThing2(i.toInt, s)
    }
  }
}

case class A1 (x: A2 , y: A2 )
case class A2 (x: A3 , y: A3 )
case class A3 (x: A4 , y: A4 )
case class A4 (x: A5 , y: A5 )
case class A5 (x: A6 , y: A6 )
case class A6 (x: A7 , y: A7 )
case class A7 (x: A8 , y: A8 )
case class A8 (x: A9 , y: A9 )
case class A9 (x: A10, y: A10)
case class A10(x: A11, y: A11)
case class A11(x: A12, y: A12)
case class A12(x: A13, y: A13)
case class A13(x: A14, y: A14)
case class A14(x: A15, y: A15)
case class A15(x: A16, y: A16)
case class A16(x: A17, y: A17)
case class A17(x: A18, y: A18)
case class A18()

import KeyedTag._
import Keyed._
import Sealed._
import Simple._
import Recursive._
import Defaults._

object ExampleTests extends TestSuite{
  import TestUtil._
  val tests = TestSuite{
//    'simple{
//      import upickle._
//
//      write(1)                          --> "1"
//
//      write(Seq(1, 2, 3))               --> "[1,2,3]"
//
//      read[Seq[Int]]("[1, 2, 3]")       --> List(1, 2, 3)
//
//      write((1, "omg", true))           --> """[1,"omg",true]"""
//
//      type Tup = (Int, String, Boolean)
//
//      read[Tup]("""[1, "omg", true]""") --> (1, "omg", true)
//    }
//    'more{
//      import upickle._
//      'booleans{
//        write(true: Boolean)              --> "true"
//        write(false: Boolean)             --> "false"
//      }
//      'numbers{
//        write(12: Int)                    --> "12"
//        write(12: Short)                  --> "12"
//        write(12: Byte)                   --> "12"
//        write(12.5f: Float)               --> "12.5"
//        write(12.5: Double)               --> "12.5"
//      }
//      'longs{
//        write(12: Long)                   --> "\"12\""
//        write(4000000000000L: Long)       --> "\"4000000000000\""
//      }
//      'specialNumbers{
//        write(1.0/0: Double)              --> "\"Infinity\""
//        write(Float.PositiveInfinity)     --> "\"Infinity\""
//        write(Float.NegativeInfinity)     --> "\"-Infinity\""
//      }
//      'charStrings{
//        write('o')                        --> "\"o\""
//        write("omg")                      --> "\"omg\""
//      }
//      'seqs{
//        write(Array(1, 2, 3))             --> "[1,2,3]"
//        write(Seq(1, 2, 3))               --> "[1,2,3]"
//        write(Vector(1, 2, 3))            --> "[1,2,3]"
//        write(List(1, 2, 3))              --> "[1,2,3]"
//        import collection.immutable.SortedSet
//        write(SortedSet(1, 2, 3))         --> "[1,2,3]"
//      }
//      'options{
//        write(Some(1))                    --> "[1]"
//        write(None)                       --> "[]"
//      }
//      'tuples{
//        write((1, "omg"))                 --> """[1,"omg"]"""
//        write((1, "omg", true))           --> """[1,"omg",true]"""
//      }

      'caseClass{
        import upickle._
        write(Thing(1, "gg"))                     --> """{"a":1,"b":"gg"}"""
        write(Big(1, true, "lol", 'Z', Thing(7, "")))(Writer.macroW)    -->
          """{"i":1,"b":true,"str":"lol","c":"Z","t":{"a":7,"b":""}}"""

        val ww1 = implicitly[upickle.Writer[A1]]


        // kill compiler
      }


//      'sealed{
//        write(IntThing(1)) --> """["example.Sealed.IntThing",{"i":1}]"""
//
//        write(TupleThing("naeem", (1, 2))) -->
//          """["example.Sealed.TupleThing",{"name":"naeem","t":[1,2]}]"""
//
//        // You can read tagged value without knowing its
//        // type in advance, just use type of the sealed trait
//        read[IntOrTuple]("""["example.Sealed.IntThing", {"i": 1}]""") --> IntThing(1)
//
//      }
//      'recursive{
//        write((((1, 2), (3, 4)), ((5, 6), (7, 8)))) -->
//          """[[[1,2],[3,4]],[[5,6],[7,8]]]"""
//
//        write(Seq(Thing(1, "g"), Thing(2, "k"))) -->
//          """[{"a":1,"b":"g"},{"a":2,"b":"k"}]"""
//
//        write(Bar("bearrr", Seq(Foo(1), Foo(2), Foo(3)))) -->
//          """{"name":"bearrr","foos":[{"i":1},{"i":2},{"i":3}]}"""
//      }
//      'null{
//        write(Bar(null, Seq(Foo(1), null, Foo(3)))) -->
//          """{"name":null,"foos":[{"i":1},null,{"i":3}]}"""
//      }
//    }
//    'defaults{
//      import upickle._
//      'reading{
//
//        read[FooDefault]("{}")                --> FooDefault(10, "lol")
//        read[FooDefault]("""{"i": 123}""")    --> FooDefault(123,"lol")
//
//      }
//      'writing{
//        write(FooDefault(i = 11, s = "lol"))  --> """{"i":11}"""
//        write(FooDefault(i = 10, s = "lol"))  --> """{}"""
//        write(FooDefault())                   --> """{}"""
//      }
//    }
//    'keyed{
//      import upickle._
//      'attrs{
//        write(KeyBar(10))                     --> """{"hehehe":10}"""
//        read[KeyBar]("""{"hehehe": 10}""")    --> KeyBar(10)
//      }
//      'tag{
//        write(B(10))                          --> """["Bee",{"i":10}]"""
//        read[B]("""["Bee", {"i": 10}]""")     --> B(10)
//      }
//    }
  }
}


