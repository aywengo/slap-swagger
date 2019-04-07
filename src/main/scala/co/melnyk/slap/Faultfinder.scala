package co.melnyk.slap

import com.softwaremill.sttp._

import scala.concurrent.{Future, ExecutionContext}
import scala.io.Source
import scala.util.Try

import java.util.Calendar
import java.nio.file.{Files, Paths}

import gnieh.diffson._
import gnieh.diffson.circe._

import io.circe._
import io.circe.parser._

import cats.data.EitherT
import cats.implicits._

object Faultfinder {

  implicit val lcs = new Patience[Json]
  implicit val backend = HttpURLConnectionBackend()

  val tmpFileName = "tmp/last.json"

  def go(url: String)(implicit ec: ExecutionContext): Future[Unit] = {
    (for {
      actual <- fetchFile(uri"$url")
      stored <- syncAndSaveTheDoc(actual)
      _ = saveFile(actual)
      diff <- compare(actual, stored)
    } yield diff)
      .fold(
        pushToSlack,
        pr => println(s"${Calendar.getInstance().getTime} - $pr")
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

  private def syncAndSaveTheDoc(content: String)
    (implicit ec: ExecutionContext): EitherT[Future, String, String] = EitherT(Future {
    if (Files.exists(Paths.get(tmpFileName))) {
      val bufferedSource = Source.fromFile(tmpFileName)
      try {
        Right(bufferedSource.getLines.mkString)
      }
      catch {
        case e: Exception => Left(s"IO error during reading old doc file: $e")
      }
      finally {
        bufferedSource.close
      }
    } else {
      // the first comparison
      saveFile(content) match {
        case Right(_) => Left(s"New api-doc has been saved!")
        case Left(e) => Left(e)
      }
    }
  }
  )

  private def compare(actual: String, before: String)
    (implicit ec: ExecutionContext): EitherT[Future, String, String] = EitherT(Future(
    Try(JsonMergeDiff.diff(before, actual))
      .fold(e => Left(s"Comparision error: $e"),
        diff => if (!diff.toJson.isArray) Right("No changes so far.") else Left(diff.toString)
      )
  )
  )

  private def pushToSlack(msg: String): Unit = {
    println(s"Slack => $msg")

    // TODO: https://api.slack.com/incoming-webhooks#posting_with_webhooks
    // sttp
    // .body(msg)
    // .post(uri"${Config.slackUrl}")
    // .send()
  }
}
