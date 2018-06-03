import scala.sys.process._

lazy val buildSettings = Seq(
  organization := "$org$",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.4",
)

val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
  "-Xfuture",
  // important to always have
    "-Ypartial-unification",
)

lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions ++
        (if (scalaJSVersion.startsWith("0.6."))
      Seq("-P:scalajs:sjsDefinedByDefault")
        else Nil),
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  libraryDependencies ++= Seq(
    "org.scalatest"          %%% "scalatest"    % "latest.release" % "test")
)

lazy val libsettings = buildSettings ++ commonSettings

lazy val root = project.in(file("."))
  .settings(libsettings)
  // this name must be coordinated with scala.webpack.config.js
  .settings(name := "app")
  .enablePlugins(ScalaJSPlugin)
  .aggregate(helloworldjvm, helloworldjs)

val circeVersion = "0.9.3" // json processing

lazy val helloworldjvm = project
  .settings(baseCommonSettings)
  .settings(
    .settings(libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-optics",      
  ).map(_ % circeVersion))
  .settings(libraryDependencies ++= Seq(
    ("com.microsoft.azure" % "azure-functions-java-core" % "1.0.0-beta-2")
      //.exclude("commons-logging","commons-logging-api"),
  ))
  .settings(
    // assemblyMergeStrategy in assembly := {
    //   case "META-INF/blueprint.handlers" => MergeStrategy.first
    //   case "META-INF/cxf/bus-extensions.txt" => MergeStrategy.first
    //   case "mozilla/public-suffix-list.txt" => MergeStrategy.first
    //   case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
    //   case x =>
    //     val oldStrategy = (assemblyMergeStrategy in assembly).value
    //     oldStrategy(x)
    // }
  )

    lazy val helloworldjs = project
      .settings(baseCommonSettings)



// Watch non-scala assets, when they change trigger sbt
// if you are using ~npmBuildFast, you get a rebuild
// when non-scala assets change.
watchSources += baseDirectory.value / "src/main/js"
watchSources += baseDirectory.value / "src/main/public"

val npmBuild = taskKey[Unit]("fullOptJS then webpack")
npmBuild := {
  (fullOptJS in Compile).value
  "npm run app" !
}

val npmBuildFast = taskKey[Unit]("fastOptJS then webpack")
npmBuildFast := {
  (fastOptJS in Compile).value
  "npm run app:dev" !
}

val npmRunDemo = taskKey[Unit]("fastOptJS then run webpack server")
npmRunDemo := {
  (fastOptJS in Compile).value
  "npm run app:dev-start" !
}
