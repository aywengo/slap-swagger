import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.1"
ThisBuild / organization     := "co.melnyk"
ThisBuild / organizationName := "melnyk.co"

lazy val root = (project in file("."))
  .settings(
    name := "slap swagger",
    libraryDependencies ++= akka ++
      circe ++
      difson ++
      logger ++
      Seq(
        scalaTest % Test,
        typesafeconfig,
        sftp
    )
  )

enablePlugins(JavaServerAppPackaging)

scalafmtOnCompile := true

mainClass in Compile := Some("co.melnyk.slap.SlApp")

dockerBaseImage       := "openjdk:jre"

dockerExposedVolumes += "tmp"