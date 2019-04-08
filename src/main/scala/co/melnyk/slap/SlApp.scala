package co.melnyk.slap

import akka.actor.ActorSystem
import com.typesafe.scalalogging._
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

object SlApp extends App with LazyLogging {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  logger.info(
    s"Starting to keep track on api-doc at ${Config.url} every ${Config.interval} min.\n" +
    "Press [Ctrl+C] to stop and exit."
  )

  val system = ActorSystem("SlapSystem")
  system.scheduler
    .schedule(initialDelay = 0 seconds, interval = Config.interval minutes)(
      Await.ready(Faultfinder.go(Config.url), 5 minutes)
    )

  Await.result(system.whenTerminated, Duration.Inf)
}
