val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.23")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "0.4.0")
