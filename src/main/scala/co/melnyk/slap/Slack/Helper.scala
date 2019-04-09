package co.melnyk.slap.Slack

import com.softwaremill.sttp._
import com.typesafe.scalalogging._

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import co.melnyk.slap.Config
import co.melnyk.slap.Slack.models._

import scala.collection.immutable

object Helper extends LazyLogging {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  def pushToSlack(msg: String): Unit = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)

    val message = printer
      .pretty(
        Payload(
          attachments = Some(
            immutable.Seq(
              Attachment(
                text = msg,
                color = Some("warning"),
                pretext = Some(Config.url),
                title = Some(extractNameFromUrl(Config.url))
              )
            )
          )
        ).asJson
      )

    logger.warn(s"Slack => $message")

    sttp
      .header("Content-type", "application/json")
      .body(message)
      .post(uri"${Config.slackUrl}")
      .send()
  }

  private def extractNameFromUrl(url: String) = {
    val pattern = """^(?:https?:\/\/)?(?:[^@\/\n]+@)?(?:www\.)?([^:\/?\n]+)""".r

    pattern
      .findAllIn(url)
      .matchData
      .map(_.group(1))
      .toSeq
      .headOption
      .map(found => s"API status on $found :")
      .getOrElse("Tracked API status:")
  }
}
