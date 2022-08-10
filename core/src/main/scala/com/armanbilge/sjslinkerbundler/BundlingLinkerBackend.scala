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

import org.scalajs.linker.interface.OutputDirectory
import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.standard.LinkerBackend
import org.scalajs.linker.standard.ModuleSet
import org.scalajs.linker.standard.StandardLinkerBackend
import org.scalajs.logging.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class BundlingLinkerBackend(linkerConfig: StandardConfig) extends LinkerBackend {
  private[this] val standard = StandardLinkerBackend(linkerConfig)

  override val coreSpec = standard.coreSpec

  override val symbolRequirements = standard.symbolRequirements

  override def injectedIRFiles = standard.injectedIRFiles

  override def emit(moduleSet: ModuleSet, output: OutputDirectory, logger: Logger)(
      implicit ec: ExecutionContext
  ): Future[Report] = {
    standard.emit(moduleSet, output, logger)
  }

}
