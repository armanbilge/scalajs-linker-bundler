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

package com.armanbilge.sjslinkerbundler

import org.scalajs.linker.ClearableLinker
import org.scalajs.linker.interface
import org.scalajs.linker.interface.Linker
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.standard.StandardLinkerFrontend
import org.scalajs.linker.standard.StandardLinkerImpl

import java.nio.file.Path

object BundlingLinkerImpl {

  def linker(config: StandardConfig, nodeModules: Option[Path]): Linker = {
    val frontend = StandardLinkerFrontend(config)
    val backend = new BundlingLinkerBackend(config, nodeModules)
    StandardLinkerImpl(frontend, backend)
  }

  def clearableLinker(
      config: StandardConfig,
      nodeModules: Option[Path]
  ): interface.ClearableLinker =
    ClearableLinker(() => linker(config, nodeModules), config.batchMode)

}
