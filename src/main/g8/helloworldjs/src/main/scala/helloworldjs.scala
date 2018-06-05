package helloworldjs

import scala.scalajs.js
import js.|
import js.annotation._

/** The default javascript function from azure...
module.exports = function (context, req) {
    context.log('JavaScript HTTP trigger function processed a request.');

    if (req.query.name || (req.body && req.body.name)) {
        context.res = {
            // status: 200, /* Defaults to 200 */
            body: "Hello " + (req.query.name || req.body.name)
        };
    }
    else {
        context.res = {
            status: 400,
            body: "Please pass a name on the query string or in the request body"
        };
    }
    context.done();
};
*/

@js.native
trait FunctionAppLogging extends js.Object {
  def error(msg: String): Unit = js.native
  def warn(msg: String): Unit = js.native
  def info(msg: String): Unit = js.native
  def verbose(msg: String): Unit = js.native
}

/**
 * @see https://docs.microsoft.com/en-us/azure/azure-functions/functions-reference-node
 */
@js.native
trait FunctionAppContext extends FunctionAppLogging {
  /** Define a bindings object structure to match your function.json bindings. */
  def bindings[T <: js.Object]: T = js.native 
  def done(code: js.UndefOr[Int | Null] = js.undefined,
    propertyBag: js.UndefOr[js.Object|js.Dynamic] = js.undefined): Unit = js.native
  def log(msg: String): Unit = js.native
  def log: FunctionAppLogging = js.native
}

object functions {
  /**
   * You can name the function anything, but the export of "main"
   * matches the convention used in webpack bundling.
   */
  @JSExportTopLevel("main")
  def helloworldjs(context: FunctionAppContext): Unit = {
    context.log("main: scala-js HTTP trigger function processed a request.")
    context.done()
  }
}
