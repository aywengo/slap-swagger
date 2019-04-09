package co.melnyk.slap

import com.softwaremill.sttp._
import com.typesafe.scalalogging._
import scala.concurrent.{ Future, ExecutionContext }
import scala.collection.immutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.Try

import java.util.Calendar
import java.nio.file.{ Files, Paths }

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
      _ = IO.Helper.saveFile(tmpFileName, actual)
    } yield diff)
      .fold(
        Slack.Helper.pushToSlack,
        logger.debug(_)
      )
  }

  private def fetchFile(url: Uri)(implicit ec: ExecutionContext): EitherT[Future, String, String] =
    EitherT(
      Future(
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
      )
    )

  private def readTheLastSaved(
      content: String
  )(implicit ec: ExecutionContext): EitherT[Future, String, String] =
    EitherT(IO.Helper.readAndOverride(tmpFileName, content))

  private def compare(actual: String, before: String)(
      implicit ec: ExecutionContext
  ): EitherT[Future, String, String] =
    EitherT(
      Future(
        Try(JsonDiff.diff(before, actual, remember = true))
          .fold(
            {
              case parsingError: io.circe.ParsingFailure =>
                Left(s"Could not parse due to an error $parsingError:\n$actual")
              case NonFatal(e) => Left(s"Comparision error: $e")
            },
            diff =>
              if (diff.ops.isEmpty) Right("No changes so far.")
              else Left(diff.toString)
          )
      )
    )
}
