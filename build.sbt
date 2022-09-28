ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / githubWorkflowBuildSbtStepPreamble := Seq()

val scala2_12 = "2.12.16"
val scala2_13 = "2.13.8"

lazy val root = project.in(file(".")).aggregate(core2_12, core2_13, sbtPlugin)

lazy val core = projectMatrix
  .in(file("core"))
  .settings(
    name := "scalajs-linker-bundler",
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-linker" % scalaJSVersion
    )
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = Seq(scala2_12, scala2_13))

lazy val core2_12 = core.jvm(scala2_12)
lazy val core2_13 = core.jvm(scala2_13)

lazy val sbtPlugin = project
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    name := "sbt-scalajs-linker-bundler",
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    buildInfoPackage := "com.armanbilge.sbt.sjslinkerbundler"
  )
