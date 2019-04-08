package co.melnyk.slap

import com.softwaremill.sttp._
import com.typesafe.scalalogging._
import scala.concurrent.{Future, ExecutionContext}
import scala.collection.immutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.Try

import java.util.Calendar
import java.nio.file.{Files, Paths}

import gnieh.diffson._
import gnieh.diffson.circe._

import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

import cats.data.EitherT
import cats.implicits._

object Faultfinder extends LazyLogging {

  implicit val lcs = new Patience[Json]
  implicit val backend = HttpURLConnectionBackend()

  val tmpFileName = "tmp/last.json"

  def go(url: String)(implicit ec: ExecutionContext): Future[Unit] = {
    (for {
      actual <- fetchFile(uri"$url")
      stored <- readTheLastSaved(actual)
      diff <- compare(actual, stored)
      _ = saveFile(actual)
    } yield diff)
      .fold(
        pushToSlack,
        logger.debug(_)
      )
  }

  private def fetchFile(url: Uri)(implicit ec: ExecutionContext): EitherT[Future, String, String] = EitherT(Future(
    Try {
      val response = sttp.get(url).send()

      response.code match {
        case 200 =>
          Right(response.unsafeBody)
        case e =>
          Left(e)
      }
    }.fold(
      e => Left(s"IO error during fetching from the URL: $e"),
      _.fold(c => Left(s"Expected 200 but responded with $c code."), Right(_))
    )
  ))

  private def saveFile(content: String)(implicit ec: ExecutionContext): Either[String, Unit] =
    Try(reflect.io.File(tmpFileName).writeAll(content))
      .fold(e => Left(s"IO error during saving the file: $e"), _ => Right(()))

  private def readTheLastSaved(content: String)
    (implicit ec: ExecutionContext): EitherT[Future, String, String] = EitherT(Future {
    if (Files.exists(Paths.get(tmpFileName))) {
      readFromFile(tmpFileName)
    } else {
      // the first comparison
      saveFile(content) match {
        case Right(_) => Left(s"New api-doc has been saved! Let's wait for changes.")
        case Left(e) => Left(e)
      }
    }
  }
  )

  private def compare(actual: String, before: String)
    (implicit ec: ExecutionContext): EitherT[Future, String, String] = EitherT(Future(
    Try(JsonMergeDiff.diff(before, actual))
      .fold({
        case parsingError: io.circe.ParsingFailure => Left(s"Could not parse due to an error $parsingError:\n$actual")
        case NonFatal(e) => Left(s"Comparision error: $e")
      },
        diff => if (diff == JsonMergePatch.Object(Map())) Right("No changes so far.") else Left(diff.toString)
      )
  ))

  private def readFromFile(fileName: String) = {
    val bufferedSource = Source.fromFile(fileName)
    try {
        Right(bufferedSource.getLines.mkString)
    }
    catch {
      case e: Exception => Left(s"IO error during reading old doc file: $e")
     }
    finally {
       bufferedSource.close
    }
  }

  private def pushToSlack(msg: String): Unit = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)

    val message = printer
      .pretty(
        Payload(text = "Tracked API status:", 
          attachments = Some(immutable.Seq(
            Attachment(text = msg)))).asJson)

    logger.warn(s"Slack => ${message}")

    sttp
    .header("Content-type", "application/json")
    .body(message)
    .post(uri"${Config.slackUrl}")
    .send()
  }
}

case class Payload(
  text: String,
  channel: Option[String] = None,
  username: Option[String] = None,
  icon_url: Option[String] = None,
  icon_emoji: Option[String] = None,
  attachments: Option[Seq[Attachment]] = None
)

case class Attachment(
  title: Option[String] = None,
  text: String,
  fallback: Option[String] = None,
  image_url: Option[String] = None,
  thumb_url: Option[String] = None,
  title_link: Option[String] = None,
  color: Option[String] = None,
  pretext: Option[String] = None,
  author_name: Option[String] = None,
  author_link: Option[String] = None,
  author_icon: Option[String] = None
)
