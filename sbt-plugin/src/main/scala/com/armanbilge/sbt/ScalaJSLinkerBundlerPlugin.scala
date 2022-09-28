/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.armanbilge.sbt

import com.armanbilge.sbt.sjslinkerbundler.BuildInfo
import org.scalajs.linker.interface.ClearableLinker
import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.sbtplugin.LinkerImpl
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.Stage
import sbt._

import Keys._
import ScalaJSPlugin.autoImport._

object ScalaJSLinkerBundlerPlugin extends AutoPlugin {

  private class BundlingLinkerImpl(base: LinkerImpl.Reflect)
      extends LinkerImpl.Forwarding(base) {

    private val loader = base.loader

    private val clearableLinkerMethod = {
      Class
        .forName("com.armanbilge.sjslinkerbundler.BundlingLinkerImpl", true, loader)
        .getMethod("clearableLinker", classOf[StandardConfig])
    }

    def bundlingLinker(config: StandardConfig): ClearableLinker = {
      clearableLinkerMethod.invoke(null, config).asInstanceOf[ClearableLinker]
    }
  }

  override def requires: Plugins = ScalaJSPlugin

  override def globalSettings: Seq[Setting[_]] = Seq(
    scalaJSLinkerImpl / fullClasspath := {
      val s = streams.value
      val log = s.log
      val retrieveDir = s.cacheDirectory / "scalajs-linker-bundler"
      val lm = (scalaJSLinkerImpl / dependencyResolution).value
      val dummyModuleID =
        BuildInfo.organization %% "scalajs-linker-bundler-and-scalajs-linker" % s"${BuildInfo.version}/$scalaJSVersion"
      val dependencies = Vector(
        // Load our linker back-end
        BuildInfo.organization %% BuildInfo.coreModule % BuildInfo.version,
        // And force-bump the dependency on scalajs-linker to match the version of sbt-scalajs
        "org.scala-js" %% "scalajs-linker" % scalaJSVersion
      )
      val moduleDescriptor =
        lm.moduleDescriptor(dummyModuleID, dependencies, scalaModuleInfo = None)
      lm.retrieve(moduleDescriptor, retrieveDir, log)
        .fold(w => throw w.resolveException, Attributed.blankSeq(_))
    },
    scalaJSLinkerImpl := {
      val cp = (scalaJSLinkerImpl / fullClasspath).value
      scalaJSLinkerImplBox.value.ensure {
        new BundlingLinkerImpl(LinkerImpl.reflect(Attributed.data(cp)))
      }
    }
  )

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(configSettings) ++ inConfig(Test)(configSettings)

  private val configSettings: Seq[Setting[_]] = Def.settings(
    scalaJSStageSettings(FastOptStage, fastLinkJS, fastOptJS),
    scalaJSStageSettings(FullOptStage, fullLinkJS, fullOptJS)
  )

  private def scalaJSStageSettings(
      stage: Stage,
      key: TaskKey[Attributed[Report]],
      legacyKey: TaskKey[Attributed[File]]
  ): Seq[Setting[_]] = Seq(
    legacyKey / scalaJSLinker := {
      val config = (key / scalaJSLinkerConfig).value
      val box = (key / scalaJSLinkerBox).value
      val linkerImpl = (key / scalaJSLinkerImpl).value

      box.ensure {
        linkerImpl.asInstanceOf[BundlingLinkerImpl].bundlingLinker(config)
      }
    }
  )

}
