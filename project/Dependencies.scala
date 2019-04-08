import sbt._

object Dependencies {

  val akkaVersion = "2.5.5"
  val circeVersion = "0.11.1"
  val diffsonVersion = "3.1.1"
  val sftpVersion = "1.5.11"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor",
    "com.typesafe.akka" %% "akka-testkit"
  ).map(_ % akkaVersion)
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val sftp = "com.softwaremill.sttp" %% "core" % sftpVersion
  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)
  lazy val difson = Seq(
    "org.gnieh" %% "diffson-circe",
    "org.gnieh" %% "diffson-core"
  ).map(_ % diffsonVersion)
  lazy val typesafeconfig = "com.typesafe" % "config" % "1.3.2"
  lazy val logger = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3")
}
