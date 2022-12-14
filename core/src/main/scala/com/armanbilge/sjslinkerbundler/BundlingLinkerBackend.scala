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
import org.scalajs.linker.interface.unstable.OutputDirectoryImpl
import org.scalajs.linker.standard.LinkerBackend
import org.scalajs.linker.standard.ModuleSet
import org.scalajs.linker.standard.StandardLinkerBackend
import org.scalajs.logging.Logger

import java.nio.ByteBuffer
import java.util.Arrays
import java.util.Collections
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class BundlingLinkerBackend(
    linkerConfig: StandardConfig
) extends LinkerBackend {

  private[this] val compilerOptions = new CompilerOptions

  compilerOptions.setChunkOutputType(CompilerOptions.ChunkOutputType.ES_MODULES)

  private[this] val standard = StandardLinkerBackend(linkerConfig)

  val coreSpec = standard.coreSpec

  val symbolRequirements = standard.symbolRequirements

  def injectedIRFiles = standard.injectedIRFiles

  def emit(moduleSet: ModuleSet, output: OutputDirectory, logger: Logger)(
      implicit ec: ExecutionContext
  ): Future[Report] = {
    val memOutput = MemOutputDirectory()
    standard.emit(moduleSet, memOutput, logger).flatMap { report =>
      val modules = moduleSet.modules.map(m => m.id -> m).toMap
      val chunks = mutable.Map[ModuleSet.ModuleID, JSChunk]()

      def getOrCreateChunk(moduleID: ModuleSet.ModuleID): JSChunk = {
        chunks.getOrElseUpdate(
          moduleID, {

            val module = modules(moduleID)
            val ch = new JSChunk(moduleID.id)

            module.internalDependencies.foreach { mid =>
              ch.addDependency(getOrCreateChunk(mid))
            }

            // TODO
            // module.externalDependencies

            memOutput
              .fileNames()
              .filter(_.startsWith(moduleID.id))
              .flatMap(fn => memOutput.content(fn).map(fn -> _))
              .foreach {
                case (fn, content) =>
                  val fixedContent =
                    new String(content).replace("\\uff3f": CharSequence, "__": CharSequence)

                  ch.add(
                    SourceFile.builder().withPath(fn).withContent(fixedContent).build()
                  )
              }

            ch
          }
        )
      }

      moduleSet.modules.foreach(module => getOrCreateChunk(module.id))

      val compiler = new Compiler
      compiler.compileModules(
        Collections.emptyList[SourceFile],
        Arrays.asList(chunks.values.toSeq: _*),
        compilerOptions
      )

      val outputImpl = OutputDirectoryImpl.fromOutputDirectory(output)
      val src = compiler.toSource()
      // val srcMap = compiler.getSourceMap()
      val publicModule = report.publicModules.head

      val result = outputImpl
        .writeFull(publicModule.jsFileName, ByteBuffer.wrap(src.getBytes()))
        .map(_ => report)

      result
    }
  }

}
