import scala.sys.process._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import org.scalajs.sbtplugin._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import functionapps.AzureFunctionappPlugin.autoImport._

lazy val buildSettings = Seq(
  organization := "$org$",
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  scalaVersion := "2.12.6",
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

lazy val scalajsSettings = Seq(
  scalacOptions ++=
    (if (scalaJSVersion.startsWith("0.6"))
      Seq("-P:scalajs:sjsDefinedByDefault")
    else Nil),
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },  
)

lazy val testLibs = Def.setting(Seq(
    "org.scalatest"          %%% "scalatest"    % "latest.release" % "test"
))

lazy val commonSettings = Seq(
  // version is applied to all sub-projects unless overridden
  version := "0.1.0",
  scalacOptions ++= commonScalacOptions,
  libraryDependencies ++= testLibs.value,
)

lazy val libsettings = buildSettings ++ commonSettings

lazy val root = project.in(file("."))
  .settings(name := "azure-functionapps")
  .settings(libsettings)
  .aggregate(helloworldjvm, helloworldjvmfatjar, helloworldjs, commonJS, commonJVM)
  .enablePlugins(functionapps.AzureFunctionappPlugin)

val circeVersion = "0.9.3" // json processing
lazy val circe = Def.setting(Seq(
  "io.circe" %%% "circe-core",
  "io.circe" %%% "circe-generic",
  "io.circe" %%% "circe-parser",
  "io.circe" %%% "circe-optics",
).map(_ % circeVersion))

lazy val commonlibs = Seq(libraryDependencies ++= circe.value)

/**
 * Common means both scala-js specific, scala-jvm specific or shared code that
 * is shared across the jvm or js platform or across functions.
 */
lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(commonlibs)
  .jsSettings(scalajsSettings)

lazy val commonJS = common.js
lazy val commonJVM = common.jvm

lazy val helloworldjvm = project
  .settings(libsettings)
  .settings(commonlibs)
  .settings(libraryDependencies ++= Seq(
    "com.microsoft.azure" % "azure-functions-java-core" % "1.0.0-beta-3",
    "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
  ))
  .dependsOn(commonJVM)

lazy val helloworldjvmfatjar = project
// if you rename the fatjar here, createDist should still be Ok
//.settings(assemblyJarName := s"\${(thisProject / name).value}-\${(thisProject / version).value}-functionapp.jar")
  .settings(libsettings)
  .settings(commonlibs)
  .settings(libraryDependencies ++= Seq(
    ("com.microsoft.azure" % "azure-functions-java-core" % "1.0.0-beta-3")
  ))
  .dependsOn(commonJVM)

lazy val helloworldjs = project
  .settings(libsettings)
  .settings(scalajsSettings)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(commonJS)
  .settings(commonlibs)
  .settings(libraryDependencies ++=
    Seq("io.scalajs"             %%% "nodejs"      % "0.4.2",
  ))

/**
 * Assume zip topdir is wwwroot, functions should be right below
 */


val copyRoot = Def.task {
  println("Copying root level files from root project.") 
  import sbt.io._
    (root / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
      IO.copyDirectory(rdir, dist.value))
}

lazy val helloworldJVMFatjarDist = taskKey[Unit]("Create dist JVM fatjar functtion.")
helloworldJVMFatjarDist := {
  jvmFatJarProject(helloworldjvmfatjar).value
}

lazy val helloworldJVMFatjarFullDist = taskKey[Unit]("Create full dist for just the JVM fatjar function.")
helloworldJVMFatjarFullDist := {
  // order not guaranteed! but usually works...
  createDistDirectory.value
  copyRoot.value
  helloworldJVMFatjarDist.value
}

lazy val helloworldJVMDist = taskKey[Unit]("Create dist helloworldjvm")
helloworldJVMDist := {
  jvmProjectDist(helloworldjvm).value
}

lazy val helloworldJVMFullDist = taskKey[Unit]("Create full dist for just the JVM fatjar function.")
helloworldJVMFullDist := {
  // order not guaranteed! but usually works...
  createDistDirectory.value
  copyRoot.value
  helloworldJVMDist.value
}

lazy val helloworldjsdist = taskKey[Unit]("Create dist helloworldjst")
helloworldjsdist := {
  jsProject(helloworldjs).value
}

lazy val createDistJS = taskKey[Unit]("Create distribution for just the js function.")
createDistJS := {
  // order not guaranteed! but usually works...
  createDistDirectory.value
  copyRoot.value
  helloworldjsdist.value
}

/**
 * Given the diferent source/output layouts you may have, we use a task defined
 * directly in the build file to assemble the distribution tree. We need some
 * processing for each project that contributes a function to the
 * functionapp. The root project is a source of some resources related to the
 * overall functionapp.
 */
lazy val createDist = taskKey[Unit]("Create distribution tree.")
createDist := {
  val s = streams.value
  s.log.info("Assembling functionapps distribution directory.")
  createDistDirectory.value
  copyRoot.value
  helloworldJVMFatjarDist.value
  helloworldJVMDist.value
  helloworldjsdist.value
}

addCommandAlias("buildAndUpload", ";createDist; createZip; upload")

addCommandAlias("buildAndUploadJS", ";createDistJS; createZip; upload")

addCommandAlias("buildAndUploadJVMFat", ";helloworldJVMFatjarFullDist; createZip; upload")

addCommandAlias("buildAndUploadJVM", ";helloworldJVMFullDist; createZip; upload")

addCommandAlias("watchJS", "~ buildAndUploadJS")
