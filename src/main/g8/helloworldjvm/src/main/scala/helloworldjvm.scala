package helloworldjvm

import java.util._
import java.io._
import com.microsoft.azure.serverless.functions.annotation._
import com.microsoft.azure.serverless.functions._

import scala.util.control._
import cats._, cats.data._, cats.implicits._

import org.apache.tika, tika._, tika.metadata._
import _root_.io.circe._, _root_.io.circe.parser.{parse => parsejson}
import _root_.io.circe.optics.JsonPath._
import _root_.io.circe.generic.auto._, _root_.io.circe.syntax._

case class ParseSuccess(
  /** Text with escaped whitespace characters. */
  response: String
)

class Function {

  def processPayload(firstname: String, lastname: String) = {
    s"$firstname $lastname".toRight[String]
  }

  @FunctionName("helloworldjvm")
  def helloworldjvm(request: HttpRequestMessage[Optional[String]],
    context: ExecutionContext): HttpResponseMessage[String] = {

    context.getLogger().info("HTTP trigger: helloworldjvm")
    val args: ValidatedNel[String, (String, String)] =
      parsejson(request.getBody().orElse("{}")) match {
        case Left(pfailure) => Validated.invalidNel(s"Malformed body: ${pfailure.show}")
        case Right(json) =>
          (
            root.firstname.string.getOption(json).toValidNel[String]("No firstname field provided."),
            root.lastname.string.getOption(json).toValidNel[String]("No lastname field provided.")
          ).mapN{ (f,c) =>  (f,c) }
      }
    context.getLogger().info(s"helloworldjvm args: $args")
    args match {
      case Validated.Valid((f, c)) =>
        processPayload(firstname, lastname) match {
          case Right(extracted) =>
            request.createResponse(200, Json.obj("response" -> Json.fromString(extracted)).noSpaces)
          case Left(t) =>
            request.createResponse(422, Json.obj("errors" -> Json.arr(Json.fromString(t.getMessage()))).noSpaces)
        }
      case Validated.Invalid(msgs) =>
        request.createResponse(400, Json.obj("errors" -> Json.fromValues(msgs.toList.map(Json.fromString))).noSpaces)
    }
  }
}
