package helloworldjs

import scala.scalajs.js
import js.Dynamic.{literal => jsobj}
import js.|
import js.annotation._
import functionapps._
import io.scalajs.nodejs.process
import io.scalajs.nodejs.util._

trait Input extends js.Object {
  val lastname: js.UndefOr[String] = js.undefined
  val firstname: js.UndefOr[String] = js.undefined
  // coulud make this a header instead
  val logContext: js.UndefOr[Boolean] = js.undefined
}

trait Output extends js.Object {
  val fullname: String
}

trait Error extends js.Object {
  val error: String
}

/** To get around psuedo-union types, by chaining 2 implicits together. */
sealed trait OutputOrError extends js.Object
object OutputOrError {
  implicit def fromOutputUnionError[A](x: A)(implicit ev: A => Output | Error): OutputOrError =
    ev(x).asInstanceOf[OutputOrError]
}

object functions {

  import OutputOrError._

  def pprint(o: js.Object,
    opts: InspectOptions = new InspectOptions(depth = null, maxArrayLength = 10)) =
    Util.inspect(o, opts)

  /**
   * You can name the function anything, but the export of "main" matches the
   * convention used in webpack bundling. Function arguments are `context,
   * ...bindings in order` but you can also access the bindings via the context
   * object using a `with YourJSTrait` approach since they area also directly
   * attached to the context object by name.
   */
  @JSExportTopLevel("main")
  def helloworldjs(context: FunctionAppContext with HttpArgs[Input, OutputOrError]): Unit = {
    val rbody = context.req.body    
    context.log("main: scala-js HTTP trigger function processed a request.")
    if(rbody.logContext.getOrElse(false)) {
      context.log(s"context: ${pprint(context)}")
      context.log(s"environment: ${pprint(process.env.asInstanceOf[js.Object])}")
    }
      (rbody.lastname.toOption, rbody.firstname.toOption) match {
      case (Some(l), Some(f)) =>
        context.res = new Response[OutputOrError] {
          val status =  200
          val body = new Output {
            val fullname = rbody.lastname.getOrElse("<last name>") + ", " +
            rbody.firstname.getOrElse("<first name>")
          }}
      case _ =>
        context.res = new Response[OutputOrError] {
          val status = 422
          val body = new Error {
            val error = "Lastname and firstname must be provided."
          }
        }
    }
    // could alse return a response through context.done(null, jsobjectresponse)
    context.done()
  }
}
