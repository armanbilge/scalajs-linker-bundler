ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / githubWorkflowBuildSbtStepPreamble := Seq()

ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v3"),
    name = Some("Setup Node.js"),
    params = Map("node-version" -> "16", "cache" -> "npm")
  ),
  WorkflowStep.Run(
    List("npm install")
  )
)

val scala2_12 = "2.12.17"
val scala2_13 = "2.13.10"

ThisBuild / tlMimaPreviousVersions := Set()
ThisBuild / tlCiMimaBinaryIssueCheck := false

lazy val root =
  project.in(file(".")).aggregate(core2_12, core2_13, sbtPlugin).enablePlugins(NoPublishPlugin)

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
    buildInfoPackage := "com.armanbilge.sbt.sjslinkerbundler",
    buildInfoKeys ++= Seq(organization, scalaBinaryVersion),
    buildInfoKeys += "coreModule" -> (core2_12 / moduleName).value,
    Test / test := {
      scripted.toTask("").value
    },
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= {
      Seq(
        "-Xmx1024M",
        s"-Dplugin.version=${version.value}",
        s"-Dnode.modules=${((LocalRootProject / baseDirectory).value / "node_modules").getAbsolutePath}"
      )
    },
    scripted := scripted.dependsOn(core2_12 / publishLocal).evaluated
  )
