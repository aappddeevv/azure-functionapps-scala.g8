package functionapps

import sbt.Keys._
import sbt._
import complete.DefaultParsers._

object AzureFunctionappPlugin extends AutoPlugin {

  object autoImport {
    val hello = inputKey[Unit]("Says hello!")
  }

  override lazy val projectSettings = Seq(
    autoImport.hello := {
      val s = streams.value
      val args = spaceDelimited("your name").parsed
      s.log.info(s"Hello, ${args(0)}")
    }
  )
}
