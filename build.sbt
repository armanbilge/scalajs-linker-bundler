ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / crossScalaVersions := Seq("2.12.16", "2.13.8")

val sjsVersion = "1.10.1"

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scalajs-linker-bundler",
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-linker" % sjsVersion
    )
  )
