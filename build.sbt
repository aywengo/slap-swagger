import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "co.melnyk"
ThisBuild / organizationName := "melnyk.co"

lazy val root = (project in file("."))
  .settings(
    name := "slap swagger",
    libraryDependencies ++= akka ++
      circe ++
      difson ++
      Seq(
        scalaTest % Test,
        typesafeconfig,
        sftp
    )
  )

enablePlugins(JavaServerAppPackaging)

mainClass in Compile := Some("co.melnyk.slap.SlApp")