package co.melnyk.slap

import akka.actor.ActorSystem
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object SlApp extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  println(s"Starting to keep track of api-doc on URL ${Config.url} every ${Config.interval} min.\n" +
    "Press [Ctrl+C] to stop and exit.")

  val system = ActorSystem("SlapSystem")
  system.scheduler
    .schedule(initialDelay = 0 seconds,
      interval = Config.interval minutes
    )(Await.ready(Faultfinder.go(Config.url), 5 minutes))

  StdIn.readLine()
  system.terminate()
}
