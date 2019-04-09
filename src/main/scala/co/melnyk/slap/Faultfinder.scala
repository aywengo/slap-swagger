package co.melnyk.slap

import cats.data.EitherT
import cats.implicits._

import com.softwaremill.sttp._
import com.typesafe.scalalogging._
import gnieh.diffson._
import gnieh.diffson.circe._
import io.circe._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scala.util.control.NonFatal

object Faultfinder extends LazyLogging {

  implicit val lcs: Patience[Json] = new Patience[Json]
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  val tmpDir: String = "tmp/"
  val tmpFileName: String = tmpDir + "last.json"

  def go(url: String)(implicit ec: ExecutionContext): Future[Unit] = {
    (for {
      actual <- fetchFile(uri"$url")
      stored <- readTheLastSaved(actual)
      diff <- compare(actual, stored)
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
              case NonFatal(e) =>
                Left(s"Comparision error: $e")
            },
            diff =>
              if (diff.ops.isEmpty) Right("No changes so far.")
              else {
                // save the raw diff
                IO.Helper
                  .saveFile(tmpDir + IO.Helper.timestamp + ".json", diff.toString)
                  .fold(logger.error(_), _ => Right(()))
                // save the snapshot
                IO.Helper
                  .saveFile(tmpFileName, actual)
                  .fold(logger.error(_), _ => Right(()))
                Left(prettyReport(diff.ops))
            }
          )
      )
    )

  private val prettyReport: List[Operation] => String =
    _.groupBy(_.path)
      .mapValues(_.map {
        case Add(path, value) =>
          s"*ADDED* ${pretty(path)}:\n$value"
        case Remove(path, old) =>
          s"*REMOVED* ~${pretty(path)}~"
        case Replace(path, value, old) =>
          s"*CHANGED* ${pretty(path)}:\n$value"
        case Move(from, path) =>
          s"*MOVED* ~${pretty(from)}~ to ${pretty(path)}"
        case Copy(from, path) =>
          s"*COPIED* ${pretty(from)} to ${pretty(path)}"
        case other: Operation =>
          other.toString
      }.mkString("\n"))
      // .sorted
      .values
      .mkString("\n")

  private val pretty: JsonPointer => String =
    p =>
      Pointer
        .unapplySeq(p.path)
        .map(_.last.fold(_.toString, _.toString))
        .getOrElse("...")
}
