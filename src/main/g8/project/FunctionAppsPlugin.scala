package functionapps

import scala.sys.process._
import sbt._
import sbt.io.IO
import sbt.Keys._
import complete.DefaultParsers._

object AzureFunctionappPlugin extends AutoPlugin {
  //override def requires: Plugins = plugins.JvmPlugin && org.scalajs.sbtplugin.ScalaJSPlugin

  object autoImport {
    /** Test input key. */
    val hello = inputKey[Unit]("Says hello!")
    val azureRG = settingKey[Option[String]]("azure resource group name")
    val azureFunctionappName = settingKey[String]("azure functionapp name. Defaults to azure-functionapp")
    val buildEnv = settingKey[String]("Build environment.")
    val dist = settingKey[File]("Target directory for assembling functions.")
    val zipName = settingKey[String]("Name of output zip deploy file. Defaults to azureFunctionappName.")

    val restart = taskKey[Unit]("Restart the functiongrup via the CLI.")
    val createDistDirectory = taskKey[Unit]("Create zip deploy distribution directory.")
    val createZip = taskKey[Unit]("Creating zip deploy distribution file.")
    val upload = taskKey[Unit]("Run the azure CLI to upload the dist. Requires env props/vars.")
  }

  import autoImport._

  override def globalSettings: Seq[Setting[_]] = {
    Seq(
      dist := file("dist"),
      //cleanFiles += dist.value,
      // clean in (This, Zero, This) := {
      //   // run standard clean
      //   val _ = (clean in (This, Zero, This)).value
      //   // clean dist
      //   IO.delete(dist.value)
      //   ()
      // },
      azureRG := {
        sys.props.get("AZURE_RG")
          .orElse(sys.env.get("AZURE_RG"))
      },
      azureFunctionappName := {
        sys.props.get("AZURE_FUNCTIONAPP_NAME")
          .orElse(sys.env.get("AZURE_FUNCTIONAPP_NAME"))
          .getOrElse("azure-functionapp")
      },
      buildEnv := {
        sys.props.get("BULID_KIND")
          .orElse(sys.env.get("BUILD_KIND"))
          .getOrElse("dev")
      },
      zipName := {
        //val format = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
        //val datepart = format.format(java.util.Calendar.getInstance().getTime())
        //you could use s"""./${(azureFunctionappName / name)value}-$datepart.zip"""
        sys.props.get("FUNCTIOINAPPS_ZIPNAME")
          .orElse(sys.env.get("FUNCTIONAPPS_ZIPNAME"))
        // the ./ is important due to bug in sbt.io.IO.zip
          .getOrElse(s"./${azureFunctionappName.value}.zip")
      },
      restart := {
        val s = streams.value
        s.log.info("Restarting functionapp")
          (azureRG.value, azureFunctionappName.value) match {
          case (Some(r), n) =>
            s"az functionapp restart $r -n $n" !
          case _ => s.log.info("Both azure resource group and appfunction name must be defined to restart.")
        }
      },
      createDistDirectory := {
        val s = streams.value
        s.log.info("Creating distribution directory ${dist.value}")
        IO.createDirectory(dist.value)
      },
      createZip := {
        val s = streams.value
        s.log.info("Building zip file ${zipName.value}")
        val pairs = Path.allSubpaths(dist.value)
        IO.zip(pairs, file(zipName.value))
      },
      upload := {
        val s = streams.value
        s.log.info("Uploading via azure CLI.")
          (azureRG.value, azureFunctionappName.value) match {
          case (Some(r), n) =>
            s"az functionapp deployment source config-zip -g $r -n $n --src ${zipName.value}" !
          case _ => s.log.info("No env variables defined to run command.")
        }
      }
    )}

  onLoadMessage := {
    val defaultMessage = onLoadMessage.value
    s"""|\${defaultMessage}
        |Running in build environment: ${buildEnv.value}""".stripMargin
  }

  override lazy val projectSettings = Seq(
    autoImport.hello := {
      val s = streams.value
      val args = spaceDelimited("your name").parsed
      s.log.info(s"Hello, ${args(0)}")
    }
  )
}
