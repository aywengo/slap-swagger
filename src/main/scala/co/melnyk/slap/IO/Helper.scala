package co.melnyk.slap.IO

import java.nio.file.{ Files, Paths }
import java.text.SimpleDateFormat

import scala.concurrent.{ Future, ExecutionContext }
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.Try

object Helper {

  def readFromFile(fileName: String): Either[String, String] = {
    val bufferedSource = Source.fromFile(fileName)
    try {
      Right(bufferedSource.getLines.mkString)
    } catch {
      case NonFatal(e) => Left(s"IO error during reading old doc file: $e")
    } finally {
      bufferedSource.close
    }
  }

  def saveFile(tmpFileName: String, content: String): Either[String, Unit] =
    Try(reflect.io.File(tmpFileName).writeAll(content))
      .fold(e => Left(s"IO error during saving the file: $e"), _ => Right(()))

  def readAndOverride(tmpFileName: String, content: String)(
      implicit ec: ExecutionContext
  ): Future[Either[String, String]] =
    Future {
      if (Files.exists(Paths.get(tmpFileName))) {
        readFromFile(tmpFileName)
      } else {
        // the first comparison
        saveFile(tmpFileName, content) match {
          case Right(_) => Left(s"New api-doc has been saved! Let's wait for changes.")
          case Left(e)  => Left(e)
        }
      }
    }

  def timestamp: String = {
    val ts = System.currentTimeMillis()
    val df: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    df.format(ts)
  }
}
