package functionapps

import scala.scalajs.js
import js.|
import js.annotation._

trait Request[T <: js.Object] extends js.Object {
  val method: String
  val url: String
  val originalUrl: String
  val headers: js.Dictionary[String]
  val query: js.Object
  val params: js.Object
  def body: T
  val rawBody: String
}

/** Required values are vals. */
trait Response[T <: js.Object] extends js.Object {
  val status: Int
  val body: T
  var headers: js.UndefOr[js.Dictionary[String]] = js.undefined
}

/**
 * If your bindings follow the functionapp tuturial, your HTTP input will be
 * called req and the response resp.
 */
trait HttpArgs[I <: js.Object, O <: js.Object] extends js.Object {
  val req: Request[I]
  var res: Response[O]
}

trait FunctionAppLogging extends js.Object {
  def error(msg: String): Unit
  def warn(msg: String): Unit
  def info(msg: String): Unit
  def verbose(msg: String): Unit
}

/**
 * @see https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-node
 */
trait FunctionAppContext extends FunctionAppLogging {
  /** 
   * Define a bindings object structure to match your bindings declaration,
   * including output bindings.
   */
  def bindings[T <: js.Object]: T
  def done(code: js.UndefOr[Int | Null], propertyBag: js.UndefOr[js.Object|js.Dynamic]): Unit
  def done(): Unit
  def log(msg: String): Unit
  def log: FunctionAppLogging
}
