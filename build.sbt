ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / crossScalaVersions := Seq("2.12.16", "2.13.8")

lazy val root = tlCrossRootProject.aggregate(core, example)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scalajs-linker-bundler",
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-linker" % scalaJSVersion
    )
  )

import com.armanbilge.sjslinkerbundler.BundlingLinkerImpl
import com.google.javascript.jscomp.CompilerOptions
import org.scalajs.sbtplugin.LinkerImpl
import org.scalajs.linker.interface._

lazy val example = project
  .in(file("example"))
  .settings(
    Compile / fastLinkJS / scalaJSLinker := {
      val config = (Compile / fastLinkJS / scalaJSLinkerConfig).value
      val box = (Compile / fastLinkJS / scalaJSLinkerBox).value
      val linkerImpl = (Compile / fastLinkJS / scalaJSLinkerImpl).value

      box.ensure {
        BundlingLinkerImpl.clearableLinker(config, new CompilerOptions)
      }
    },
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerImpl := {
      val cp = (scalaJSLinkerImpl / fullClasspath).value
      scalaJSLinkerImplBox.value.ensure {
        new LinkerImpl.Forwarding(LinkerImpl.reflect(Attributed.data(cp))) {
          override def clearableLinker(cfg: StandardConfig): ClearableLinker =
            BundlingLinkerImpl.clearableLinker(cfg, new CompilerOptions)
        }
      }
    }
  )
  .enablePlugins(ScalaJSPlugin, NoPublishPlugin)
