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

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.JSChunk
import com.google.javascript.jscomp.SourceFile
import org.scalajs.linker.MemOutputDirectory
import org.scalajs.linker.interface.OutputDirectory
import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.standard.LinkerBackend
import org.scalajs.linker.standard.ModuleSet
import org.scalajs.linker.standard.StandardLinkerBackend
import org.scalajs.logging.Logger

import java.io.ByteArrayInputStream
import java.util.Arrays
import java.util.Collections
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class BundlingLinkerBackend(
    linkerConfig: StandardConfig,
    compilerOptions: CompilerOptions
) extends LinkerBackend {

  compilerOptions.setChunkOutputType(CompilerOptions.ChunkOutputType.ES_MODULES)

  private[this] val standard = StandardLinkerBackend(linkerConfig)

  val coreSpec = standard.coreSpec

  val symbolRequirements = standard.symbolRequirements

  def injectedIRFiles = standard.injectedIRFiles

  def emit(moduleSet: ModuleSet, output: OutputDirectory, logger: Logger)(
      implicit ec: ExecutionContext
  ): Future[Report] = {
    val memOutput = MemOutputDirectory()
    standard.emit(moduleSet, memOutput, logger).map { report =>
      val publicModules =
        report.publicModules.toList.flatMap(m => memOutput.content(m.jsFileName).map(m -> _))

      val publicModuleNames = report.publicModules.map(_.jsFileName).toSet
      val internalFiles = memOutput
        .fileNames()
        .filterNot(publicModuleNames.contains(_))
        .flatMap(fn => memOutput.content(fn).map(fn -> _))

      val internalChunk = new JSChunk("internal")
      internalFiles.foreach {
        case (fn, content) =>
          internalChunk.add(
            SourceFile
              .builder()
              .withPath(fn)
              .withContent(new ByteArrayInputStream(content))
              .build()
          )
      }

      val publicChunks = publicModules.map {
        case (mod, content) =>
          val ch = new JSChunk(mod.moduleID)
          ch.addDependency(internalChunk)
          ch.add(
            SourceFile
              .builder()
              .withPath(mod.jsFileName)
              .withContent(new ByteArrayInputStream(content))
              .build()
          )
          ch
      }

      val compiler = new Compiler
      compiler.compileModules(
        Collections.emptyList,
        Arrays.asList((publicChunks :+ internalChunk): _*),
        compilerOptions
      )

      report
    }
  }

}
