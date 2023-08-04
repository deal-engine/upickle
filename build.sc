// plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $ivy.`com.github.lolgab::mill-mima::0.0.23`

// imports
import mill._
import mill.define.Target
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import mill.scalanativelib._
import mill.scalalib.api.ZincWorkerUtil.isScala3
import mill.scalanativelib.api.{LTO, ReleaseMode}
import de.tobiasroeser.mill.vcs.version.VcsVersion
import com.github.lolgab.mill.mima._

val scala213  = "2.13.10"

val scala3   = "3.2.2"
val scalaNative = "0.4.14"
val acyclic = "0.3.8"

val sourcecode = "0.3.0"

val dottyCustomVersion = Option(sys.props("dottyVersion"))

val scala2JVMVersions = Seq(scala213)
val scalaVersions = scala2JVMVersions ++ Seq(scala3) ++ dottyCustomVersion

trait CommonPlatformModule extends ScalaModule with PlatformScalaModule{

  def sources = T.sources {
    super.sources() ++
    Seq(PathRef(millSourcePath / "src-2.13+")) ++
    (platformScalaSuffix match {
      case "jvm" => Seq(PathRef(millSourcePath / "src-jvm-native"))
      case "native" => Seq(PathRef(millSourcePath / "src-js-native"), PathRef(millSourcePath / "src-jvm-native"))
      case "js" => Seq(PathRef(millSourcePath / "src-js-native"))
    })
  }
}

trait CommonPublishModule
  extends ScalaModule with PublishModule with Mima { outer =>

  def publishVersion = VcsVersion.vcsState().format()
  def mimaPreviousVersions = Seq("3.0.0")
  def isDotty = T { scalaVersion().startsWith("0") || scalaVersion().startsWith("3") }
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.github.deal-engine.upickle",
    url = "https://github.com/lihaoyi/upickle",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github(owner = "com-lihaoyi", repo = "upickle"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )

  def publishProperties: Target[Map[String, String]] = super.publishProperties() ++ Map(
    "info.releaseNotesURL" -> "https://com-lihaoyi.github.io/upickle/#VersionHistory"
  )

  def versionScheme: T[Option[VersionScheme]] = T(Some(VersionScheme.SemVerSpec))

  def templates = T.sources {
    for(src <- sources()) yield{
      val s"src$rest" = src.path.last
      PathRef(src.path / os.up / s"templates$rest")
    }
  }
  def generatedSources = T{
    for{
      pathRef <- templates()
      p <- if (os.exists(pathRef.path)) os.list(pathRef.path) else Nil
      rename <- Seq("Char", "Byte")
    }{
      def replace(s: String) = s.replace("Elem", rename).replace("elem", rename.toLowerCase)
      os.write(T.dest / replace(p.last), replace(os.read(p)))
    }

    Seq(PathRef(T.dest))
  }

  def scalacOptions = T {
    Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-Xfatal-warnings") ++
    Agg.when(!isScala3(scalaVersion()))("-opt:l:method").toSeq ++
    (if (isDotty()) Nil else Seq("-Ytasty-reader"))
  }

  trait CommonTestModule0 extends ScalaModule with TestModule.Utest {
    def ivyDeps = {
      Agg(ivy"com.lihaoyi::utest::0.8.1") ++
      Option.when(!isScala3(scalaVersion()))(ivy"com.lihaoyi:::acyclic:$acyclic")
    }

    def scalacOptions = super.scalacOptions() ++
      Agg.when(isScala3(scalaVersion())) (
        "-Ximplicit-search-limit",
        "200000"
      )
  }
}

trait CommonJvmModule extends CommonPublishModule with CommonPlatformModule{
  trait CommonTestModule extends ScalaTests with CommonTestModule0
}

trait CommonJsModule extends CommonPublishModule with ScalaJSModule with CommonPlatformModule {
  def scalaJSVersion = "1.13.0"

  private def sourceMapOptions = T.task {
    val vcsState = VcsVersion.vcsState()
    vcsState.lastTag.collect {
      case tag if vcsState.commitsSinceLastTag == 0 =>
        val baseUrl = pomSettings().url.replace("github.com", "raw.githubusercontent.com")
        val sourcesOptionName = if(isScala3(scalaVersion())) "-scalajs-mapSourceURI" else "-P:scalajs:mapSourceURI"
        s"$sourcesOptionName:${T.workspace.toIO.toURI}->$baseUrl/$tag/"
    }
  }

  def scalacOptions = super.scalacOptions() ++ sourceMapOptions()

  trait CommonTestModule extends ScalaJSTests with CommonTestModule0
}

trait CommonNativeModule extends CommonPublishModule with ScalaNativeModule with CommonPlatformModule {
  def scalaNativeVersion = scalaNative

  trait CommonTestModule extends ScalaNativeTests with CommonTestModule0
}

object upack extends Module {
  object js extends JsModule
  trait JsModule extends CommonJsModule {
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.js)

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(ujson.js.test, upickle.core.js.test)
    }
  }

  object jvm extends JvmModule
  trait JvmModule extends CommonJvmModule {
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.jvm)
    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm.test, upickle.core.jvm.test)
    }
  }

  object native extends NativeModule
  trait NativeModule extends CommonNativeModule {
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.native)

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(ujson.native.test, upickle.core.native.test)
    }
  }
}

object ujson extends Module{
  object js extends JsModule
  trait JsModule extends CommonJsModule {
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.js)

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(upickle.core.js.test)
    }
  }

  object jvm extends JvmModule
  trait JvmModule extends CommonJvmModule{
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.jvm)

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(upickle.core.jvm.test)
    }
  }

  object native extends NativeModule
  trait NativeModule extends CommonNativeModule {
    def scalaVersion = "3.2.2"
    def moduleDeps = Seq(upickle.core.native)

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(upickle.core.native.test)
    }
  }
}

object upickle extends Module{  
  object core extends Module {
    trait CommonCoreModule extends CommonPublishModule {
      def ivyDeps = Agg(ivy"com.lihaoyi::geny::1.0.0")
    }

    object js extends CoreJsModule
    trait CoreJsModule extends CommonJsModule with CommonCoreModule {
      def scalaVersion = "3.2.2"
      object test extends CommonTestModule
    }

    object jvm extends CoreJvmModule
    trait CoreJvmModule extends CommonJvmModule with CommonCoreModule{
      def scalaVersion = "3.2.2"
      object test extends CommonTestModule
    }

    object native extends CoreNativeModule
    trait CoreNativeModule extends CommonNativeModule with CommonCoreModule {
      def scalaVersion = "3.2.2"
      object test extends CommonTestModule
    }
  }

  object `implicits-compat` extends Module {
    trait ImplicitsModule extends CommonPublishModule{
      def compileIvyDeps = T{
        Agg.when(!isDotty())(
          ivy"com.lihaoyi:::acyclic:$acyclic",
          ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
        )
      }
    }

    object js extends JsModule
    trait JsModule extends ImplicitsModule with CommonJsModule {
      def scalaVersion = "2.13.10"
      def compileModuleDeps = Seq(core.js)

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.js.test, core.js.test)
      }
    }

    object jvm extends JvmModule
    trait JvmModule extends ImplicitsModule with CommonJvmModule {
      def scalaVersion = "2.13.10"
      def compileModuleDeps = Seq(core.jvm)

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm.test, core.jvm.test)
      }
    }

    object native extends NativeModule
    trait NativeModule extends ImplicitsModule with CommonNativeModule {
      def scalaVersion = "2.13.10"
      def compileModuleDeps = Seq(core.native)

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.native.test, core.native.test)
      }
    }
  }

  object implicits extends Module {
    trait ImplicitsModule extends CommonPublishModule{
      def compileIvyDeps = T{
        Agg.when(!isDotty())(
          ivy"com.lihaoyi:::acyclic:$acyclic",
          ivy"org.scala-lang:scala-reflect:${scalaVersion()}"
        )
      }

      def generatedSources = T{
        val dir = T.ctx().dest
        val file = dir / "upickle" / "Generated.scala"
        os.makeDir(dir / "upickle")
        val tuples = (1 to 22).map{ i =>
          def commaSeparated(s: Int => String) = (1 to i).map(s).mkString(", ")
          val writerTypes = commaSeparated(j => s"T$j: Writer")
          val readerTypes = commaSeparated(j => s"T$j: Reader")
          val typeTuple = commaSeparated(j => s"T$j")
          val implicitWriterTuple = commaSeparated(j => s"implicitly[Writer[T$j]]")
          val implicitReaderTuple = commaSeparated(j => s"implicitly[Reader[T$j]]")
          val lookupTuple = commaSeparated(j => s"x(${j-1})")
          val fieldTuple = commaSeparated(j => s"x._$j")
          s"""
          implicit def Tuple${i}Writer[$writerTypes]: TupleNWriter[Tuple$i[$typeTuple]] =
            new TupleNWriter[Tuple$i[$typeTuple]](Array($implicitWriterTuple), x => if (x == null) null else Array($fieldTuple))
          implicit def Tuple${i}Reader[$readerTypes]: TupleNReader[Tuple$i[$typeTuple]] =
            new TupleNReader(Array($implicitReaderTuple), x => Tuple$i($lookupTuple).asInstanceOf[Tuple$i[$typeTuple]])
          """
        }

        os.write(file, s"""
        package upickle.implicits
        /**
         * Auto-generated picklers and unpicklers, used for creating the 22
         * versions of tuple-picklers and case-class picklers
         */
        trait Generated extends TupleReadWriters{
          ${tuples.mkString("\n")}
        }
      """)
        Seq(PathRef(dir))
      }
    }

    object js extends JsModule
    trait JsModule extends ImplicitsModule with CommonJsModule {
      def scalaVersion = "3.2.2"
      def compileModuleDeps = Seq(core.js, `implicits-compat`.js)

      // Fix me D:
      override def docJar: T[PathRef] = core.jvm.docJar
      override def scalacOptions = super.scalacOptions() ++ Seq("-language:experimental.macros", "-explain")

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.js.test, core.js.test)
      }
    }

    object jvm extends JvmModule
    trait JvmModule extends ImplicitsModule with CommonJvmModule {
      def scalaVersion = "3.2.2"
      def compileModuleDeps = Seq(core.jvm, `implicits-compat`.jvm)


      // Fix me D:
      override def docJar: T[PathRef] = core.jvm.docJar
      override def scalacOptions = super.scalacOptions() ++ Seq("-language:experimental.macros", "-explain")

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.jvm.test, core.jvm.test)
      }
    }
    

    object native extends NativeModule
    trait NativeModule extends ImplicitsModule with CommonNativeModule {
      def scalaVersion = "3.2.2"
      def compileModuleDeps = Seq(core.native, `implicits-compat`.native)

      // Fix me D:
      override def docJar: T[PathRef] = core.jvm.docJar
      override def scalacOptions = super.scalacOptions() ++ Seq("-language:experimental.macros", "-explain")

      object test extends CommonTestModule{
        def moduleDeps = super.moduleDeps ++ Seq(ujson.native.test, core.native.test)
      }
    }
  }

  trait UpickleModule extends CommonPublishModule {
    def compileIvyDeps =
      Agg.when(!isDotty())(
        ivy"com.lihaoyi:::acyclic:$acyclic",
        ivy"org.scala-lang:scala-reflect:${scalaVersion()}",
        ivy"org.scala-lang:scala-compiler:${scalaVersion()}"
      )
  }

  object jvm extends JvmModule
  trait JvmModule extends UpickleModule with CommonJvmModule{
    def scalaVersion = "3.2.2"
    def compileModuleDeps = Seq(implicits.jvm)
    def moduleDeps = Seq(ujson.jvm, upack.jvm)
    // Fix me D:
    override def docJar: T[PathRef] = core.jvm.docJar

    object test extends CommonTestModule{
      def moduleDeps =
        super.moduleDeps ++
        Seq(core.jvm.test)
    }

    object testNonUtf8 extends CommonTestModule {
      def forkArgs = Seq("-Dfile.encoding=US-ASCII")
    }

    object testSlow extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(JvmModule.this.test)
    }
  }

  object js extends JsModule
  trait JsModule extends UpickleModule with CommonJsModule {
    def scalaVersion = "3.2.2"
    def compileModuleDeps = Seq(implicits.js)
    def moduleDeps = Seq(ujson.js, upack.js)
    // Fix me D:
    override def docJar: T[PathRef] = core.jvm.docJar

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(core.js.test)
    }
  }

  object native extends NativeModule
  trait NativeModule extends UpickleModule with CommonNativeModule {
    def scalaVersion = "3.2.2"
    def compileModuleDeps = Seq(implicits.native)
    def moduleDeps = Seq(ujson.native, upack.native)
    // Fix me D:
    override def docJar: T[PathRef] = core.jvm.docJar

    object test extends CommonTestModule{
      def moduleDeps = super.moduleDeps ++ Seq(core.native.test)
      def allSourceFiles = T(super.allSourceFiles().filter(_.path.last != "DurationsTests.scala"))
    }
  }
}

def exampleJson = T.source(millSourcePath / "exampleJson")

trait BenchModule extends CommonPlatformModule{
  def scalaVersion = scala213
  def ivyDeps = Agg(
    ivy"io.circe::circe-core::0.14.5",
    ivy"io.circe::circe-generic::0.14.5",
    ivy"io.circe::circe-parser::0.14.5",
    ivy"com.typesafe.play::play-json::2.9.4",
    ivy"io.argonaut::argonaut:6.2.6",
    ivy"org.json4s::json4s-ast:3.6.12",
    ivy"com.lihaoyi::sourcecode::$sourcecode",
  )
}
