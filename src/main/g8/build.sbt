import scala.sys.process._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import org.scalajs.sbtplugin._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._


lazy val buildSettings = Seq(
  organization := "me",
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

lazy val commonSettings = Seq(
  // version is applied to all sub-projects unless overridden
  version := "0.1.0",
  scalacOptions ++= commonScalacOptions,
  libraryDependencies ++= Seq(
    //"org.scalatest"          %%% "scalatest"    % "latest.release" % "test")
  )
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
//.settings(assemblyJarName := s"${(thisProject / name).value}-${(thisProject / version).value}-functionapp.jar")
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

//watchSources += baseDirectory.value / "src/main/js"
//watchSources += baseDirectory.value / "src/main/public"

val azureRG = settingKey[Option[String]]("azure resource group name")
azureRG := {
  sys.props.get("AZURE_RG")
    .orElse(sys.env.get("AZURE_RG"))
}

val azureFunctionappName = settingKey[Option[String]]("azure functionapp name")
azureFunctionappName := {
  sys.props.get("AZURE_FUNCTIONAPP_NAME")
    .orElse(sys.env.get("AZURE_FUNCTIONAPP_NAME"))
}

//
// Obtain env setting as a string: dev, prod, etc.
// @todo use enum or sealed trait
//
val buildEnv = settingKey[String]("Build environment.")
buildEnv := {
  sys.props.get("BULID_KIND")
    .orElse(sys.env.get("BUILD_KIND"))
    .getOrElse("dev")
}

onLoadMessage := {
  val defaultMessage = onLoadMessage.value
  s"""|${defaultMessage}
      |Running in build environment: ${buildEnv.value}""".stripMargin
}

lazy val dist = settingKey[File]("Target directory for assembling functions.")
dist := file("dist")

// root level remove dist directory
cleanFiles += dist.value

val zipName = settingKey[String]("Name of output zip deploy file.")
zipName := {
  //val format = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
  //val datepart = format.format(java.util.Calendar.getInstance().getTime())
  //could use s"""./${(root / name)value}-$datepart.zip"""
  sys.props.get("FUNCTIOINAPPS_ZIPNAME")
    .orElse(sys.env.get("FUNCTIONAPPS_ZIPNAME"))
  // the ./ is important due to bug in sbt.io.IO.zip
    .getOrElse(s"./${(root / name).value}.zip")
}

/**
 * Assume zip topdir is wwwroot, functions should be right below
 */
val createZip = taskKey[Unit]("Creating zip deploy distribution file.")
createZip := {
  import sbt.io._
  println("Building zip file.")
  val pairs = Path.allSubpaths(dist.value)
  IO.zip(pairs, file(zipName.value))
}

// copy root/src/main/resources to toplevel dist folder
val copyRoot = Def.task {
  println("Copying root level files from root project.") 
  import sbt.io._
    (root / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
      IO.copyDirectory(rdir, dist.value))
}

//
// p1: helloworldjvmfatjar
// Remember:
// helloworldjvmfatjar / package => produces .jar of your sources only
// helloworldjvmfatjar / assembly => produces fat jar via sbt-assembly plugin
lazy val helloworldJVMFatjarDist = taskKey[Unit]("Create dist JVM fatjar functtion.")
helloworldJVMFatjarDist := {
  println("Assembling helloworldjvmfatjar")
  // dtaskDyn of function = name of project
  val fname = (helloworldjvmfatjar / name).value // string
  val fdir = dist.value / fname // a File
                                    // copy unmanaged resources e.g. <project>/src/main/resources
    (helloworldjvmfatjar / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
      IO.copyDirectory(rdir, fdir))
  // run the assembly process (which in turn runs compiles)
    (assembly in helloworldjvmfatjar).value
  // copy executable artifacts: only 1 artifact since this is a fat jar
  val outputFile = (helloworldjvmfatjar / assembly / assemblyOutputPath).value
  val targetFile = fdir / (helloworldjvmfatjar / assembly / assemblyJarName).value
  if(outputFile.exists) IO.copyFile(outputFile, targetFile)
}

lazy val helloworldJVMFatjarFullDist = taskKey[Unit]("Create full dist for just the JVM fatjar function.")
helloworldJVMFatjarFullDist := {
  // order not guaranteed! but usually works...
  createDistDirectory.value
  copyRoot.value
  helloworldJVMFatjarDist.value
}

//
// helloworldjvm, copy individual jar files and dependencies into the
// functiondir. You could alter this to have all dependencies go to a common dir
// but space is cheap even in the cloud. The classpath is mostly broken in the
// current functionapps. For more than 1 jar, place jar into functionapp root
// directory, ensure your function.json indicates "../thejar.jar" and then place
// all other jars into a "root/lib" directory.
//
lazy val helloworldJVMDist = taskKey[Unit]("Create dist helloworldjvm")
helloworldJVMDist := {
  println("Assembling helloworldjvm")
  val fname = (helloworldjvm / name).value // string
  val fdir = dist.value / fname
  val libdir = dist.value / "lib"
  // copy unmanaged resources e.g. <project>/src/main/resources
    (helloworldjvm / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
      IO.copyDirectory(rdir, fdir))
  // this forces the package task to run...
  val p = (helloworldjvm / Compile / packageBin / packagedArtifact).value // (art, file)

  // copy jar created from project
  val outputFile = (helloworldjvm / Compile / packageBin / artifactPath).value
  //val targetFile =       fdir / p._2.name
  val targetFile =       dist.value / p._2.name
  if(outputFile.exists) IO.copyFile(outputFile, targetFile)
  // copy dependencies jars, often is a larger set of jars than you think :-)
  (helloworldjvm / Runtime / fullClasspath).value.files.filter(_.exists).filterNot(_.isDirectory).foreach{depfile =>
    IO.copyFile(depfile, libdir / depfile.name)
  }
}

lazy val helloworldJVMFullDist = taskKey[Unit]("Create full dist for just the JVM fatjar function.")
helloworldJVMFullDist := {
  // order not guaranteed! but usually works...
  createDistDirectory.value
  copyRoot.value
  helloworldJVMDist.value
}

// p3: helloworldjs, use webpack to bundle
// task that returns a task
//lazy val helloworldjsdist = taskKey[sbt.Def.Initialize[Task[Unit]]]("Create dist helloworldjs")
lazy val helloworldjsdist = taskKey[Unit]("Create dist helloworldjs")
helloworldjsdist := (Def.taskDyn {
  val s = streams.value
  s.log.info("Assembling helloworldjs")
  val fname = (helloworldjs / name).value // string
  val fdir = dist.value / fname
    (helloworldjs / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
      IO.copyDirectory(rdir, fdir))
  if(buildEnv.value.startsWith("prod")) Def.task[Unit] {
    (helloworldjs / Compile / fullOptJS).value
    "npm run functionapps -- --env.name=helloworldjs --env.BUILD_KIND=production" !
  }
  else Def.task[Unit] {
    (helloworldjs / Compile / fastOptJS).value
    "npm run functionapps -- --env.name=helloworldjs" !
  }
}).value

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

val createDistDirectory = Def.task {
  val s = streams.value
  s.log.info("Creating distribution directory.")
  // create target directory, if needed
  IO.createDirectory(dist.value)
}

addCommandAlias("buildAndUpload", ";createDist; createZip; upload")

addCommandAlias("buildAndUploadJS", ";createDistJS; createZip; upload")

addCommandAlias("buildAndUploadJVMFat", ";helloworldJVMFatjarFullDist; createZip; upload")

addCommandAlias("buildAndUploadJVM", ";helloworldJVMFullDist; createZip; upload")

addCommandAlias("watchJS", "~ buildAndUploadJS")

lazy val upload = taskKey[Unit]("Run the azure CLI to upload the dist. Requires env props/vars.")
upload := {
  val s = streams.value
  s.log.info("Uploading via azure.")
    (azureRG.value, azureFunctionappName.value) match {
    case (Some(r), Some(n)) =>
      s"az functionapp deployment source config-zip -g $r -n $n --src ${zipName.value}" !
    case _ => s.log.info("No env variables defined to run command.")
  }
}

lazy val restart = taskKey[Unit]("Restart the functiongrup via the CLI.")
restart := {
  val s = streams.value
  s.log.info("Restarting the functionapp")
    (azureRG.value, azureFunctionappName.value) match {
    case (Some(r), Some(n)) =>
      s"az functionapp restart $r -n $n" !
    case _ => s.log.info("No env variables defined to run command.")
  }
}
