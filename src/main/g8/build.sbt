import scala.sys.process._


// import sbtcrossproject.CrossPlugin.autoImport.crossProject
// import sbtcrossproject.CrossType

// lazy val bar = crossProject(JSPlatform, JVMPlatform)
//   .crossType(CrossType.Pure)
//   .settings(scalaVersion := "2.11.12")

// lazy val barJS = bar.js
// lazy val barJVM = bar.jvm

// lazy val client = (project in file("js"))
//   .enablePlugins(ScalaJSPlugin)
//   .dependsOn(barJS)


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
    (if (scalaJSVersion.startsWith("0.6."))
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
  .settings(libsettings)
  // this name must be coordinated with scala.webpack.config.js
  .settings(name := "app")
  .aggregate(helloworldjvm, helloworldjvmfatjar, helloworldjs)

val circeVersion = "0.9.3" // json processing
lazy val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-optics",
).map(_ % circeVersion)

lazy val helloworldjvm = project
  .settings(libsettings)
  .settings(libraryDependencies ++= circe)
  .settings(libraryDependencies ++= Seq(
    ("com.microsoft.azure" % "azure-functions-java-core" % "1.0.0-beta-3")
  ))

lazy val helloworldjvmfatjar = project
  // if you rename the fatjar here, createDist should still be Ok
  //.settings(assemblyJarName := s"${(thisProject / name).value}-${(thisProject / version).value}-functionapp.jar")
  .settings(libsettings)
  .settings(libraryDependencies ++= circe)
  .settings(libraryDependencies ++= Seq(
    ("com.microsoft.azure" % "azure-functions-java-core" % "1.0.0-beta-3")
  ))

lazy val helloworldjs = project
  .settings(libsettings)
  .settings(scalajsSettings)
  .enablePlugins(ScalaJSPlugin)

/**
 * Watch non-scala assets, when they change trigger sbt if you are using
 * ~npmBuildFast, you get a rebuild when non-scala assets change.
 */
//watchSources += baseDirectory.value / "src/main/js"
//watchSources += baseDirectory.value / "src/main/public"

//
// obtain env setting
//
val buildEnv = settingKey[String]("Build environment.")
buildEnv := {
  sys.props.get("env")
    .orElse(sys.env.get("BUILD_ENV"))
    .getOrElse("dev")
}

onLoadMessage := {
  val defaultMessage = onLoadMessage.value
  s"""|${defaultMessage}
      |Running in build environment: ${buildEnv.value}""".stripMargin
}

/**
 * Given the diferent source/output layouts you may have, we use a task defined
 * directly in the bulid file to assemble the distribution tree. We need some
 * processing for each project that contributes a function to the
 * functionapp. The root project is a source of some resources related to the
 * overall functionapp.
 */
lazy val createDist = taskKey[Unit]("Create distribution tree.")
//createDist := Def.taskDyn {
createDist := (Def.taskDyn {
  import sbt.io._
  println("Assembling dist.")
  val targetdir = file("dist")

  // create target directory, if needed
  IO.createDirectory(targetdir)

  //
  // root project
  //
  // copy unmanged resources to toplevel dist
  (root / Compile / unmanagedResourceDirectories).value.foreach(rdir =>
    IO.copyDirectory(rdir, targetdir))

  //
  // p1: helloworldjvmfatjar
  // Remember:
  // helloworldjvmfatjar / package => produces .jar of your sources only
  // helloworldjvmfatjar / assembly => produces fat jar via sbt-assembly plugin
  //
  // run the assembly process (which in turn runs compiles)
  (assembly in helloworldjvmfatjar).value
  // name of function = name of project
  val p1fname = (helloworldjvmfatjar / name).value // string
  val p1fdir = targetdir / p1fname // a File
  // copy unmanaged resources e.g. <project>/src/main/resources
  (helloworldjvmfatjar / Compile / unmanagedResourceDirectories).value.foreach(rdir => 
    IO.copyDirectory(rdir, p1fdir))
  // copy executable artifacts: only 1 artifact since this is a fat jar
  IO.copyFile(
    (helloworldjvmfatjar / assembly / assemblyOutputPath).value,
    p1fdir / (helloworldjvmfatjar / assembly / assemblyJarName).value
  )

  //
  // p2: helloworldjs, copy individual jar files and dependencies into
  // the functiondir. You could alter this to have all dependencies
  // go to a common dir but space is cheap even in the cloud.
  //
  (helloworldjvm / Compile / packageBin).value
  val p2fname = (helloworldjvm / name).value // string
  val p2fdir = targetdir / p2fname
  // this forces the package task to run...
  val p = (helloworldjvm / Compile / packageBin / packagedArtifact).value // (art, file)
  // copy unmanaged resources e.g. <project>/src/main/resources
  (helloworldjvm / Compile / unmanagedResourceDirectories).value.foreach(rdir => 
    IO.copyDirectory(rdir, p2fdir))
  // copy jar created from project
  IO.copyFile(
    (helloworldjvm / Compile / packageBin / artifactPath).value,
    p2fdir / p._2.name
  )
  // copy dependencies jars, often is a large set of jars than you think :-)
  (helloworldjvm / Runtime / fullClasspath).value.files.filterNot(_.isDirectory).foreach{depfile =>
      println(s"$depfile");
      IO.copyFile(depfile, p2fdir / depfile.name)
  }

  //
  // p3: helloworldjs, use webpack to bundle
  //
  val p3fname = (helloworldjs / name).value // string
  val p3fdir = targetdir / p3fname
  (helloworldjs / Compile / unmanagedResourceDirectories).value.foreach(rdir => 
    IO.copyDirectory(rdir, p3fdir))
  if(buildEnv.value == "dev") Def.task {
    (helloworldjs / Compile / fastOptJS).value
    "npm run helloworldjs:dev" !
  }
  else Def.task {
    (helloworldjs / Compile / fullOptJS).value
    "npm run helloworldjs" !
  }
}).value

// assume zip topdir is wwwroot, functions should be right below
val createZip = taskKey[Unit]("Create final zip file.")
createZip := {
  import sbt.io._  
  println("Building zip file.")
  IO.zip(Seq((file("dist/host.json"), "host.json")), file("function.zip"))
}
