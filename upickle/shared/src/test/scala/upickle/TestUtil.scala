package upickle
import utest._
import acyclic.file
/**
* Created by haoyi on 4/22/14.
*/
object TestUtil extends TestUtil[upickle.default.type](upickle.default)
object LegacyTestUtil extends TestUtil[upickle.legacy.type](upickle.legacy)
class TestUtil[Api <: upickle.Api](api: Api){
  import api._
  def rw[T: Reader: Writer](t: T, s: String*) = {
    rwk[T, T](t, s:_*)(x => x)
  }
  def rwk[T: Reader: Writer, V](t: T, sIn: String*)(normalize: T => V) = {
    val writtenT = write(t)

    val strings = sIn.map(_.trim)

//    if (strings.length > 0) {
//      val inputs = strings.map(api.read)
//      val output = api.read(writtenT)
//      assert(inputs.contains(output))
//    }
    for (s <- strings) {
      val readS = read[T](s)
      val normalizedReadS = normalize(readS)
      val normalizedT = normalize(t)
      assert(normalizedReadS == normalizedT)
    }

    assert(normalize(read[T](writtenT)) == normalize(t))
  }
}