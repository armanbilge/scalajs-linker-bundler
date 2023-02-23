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
import com.google.javascript.jscomp.DependencyOptions
import com.google.javascript.jscomp.JSChunk
import com.google.javascript.jscomp.ModuleIdentifier
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.jscomp.deps.ModuleLoader.ResolutionMode
import org.scalajs.linker.MemOutputDirectory
import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.OutputDirectory
import org.scalajs.linker.interface.Report
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.interface.unstable.OutputDirectoryImpl
import org.scalajs.linker.standard.LinkerBackend
import org.scalajs.linker.standard.ModuleSet
import org.scalajs.linker.standard.StandardLinkerBackend
import org.scalajs.logging.Logger

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.Arrays
import java.util.Collections
import java.util.function.BiPredicate
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class BundlingLinkerBackend(
    linkerConfig: StandardConfig,
    nodeModules: Option[Path]
) extends LinkerBackend {

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

      val nodeModulesChunk = nodeModules.filter(Files.exists(_)).map { nodeModulesPath =>
        val ch = new JSChunk("node_modules")

        val matcher: BiPredicate[Path, BasicFileAttributes] = { (path, attr) =>
          def checkExts(fn: String) =
            List(".js", ".cjs", ".mjs").exists(fn.endsWith(_))

          attr.isRegularFile() &&
          (path.endsWith("package.json") || checkExts(path.getFileName.toString.toLowerCase))
        }

        Files.find(nodeModulesPath, Int.MaxValue, matcher).forEach { path =>
          ch.add(
            SourceFile
              .builder()
              .withContent(new FileInputStream(path.toFile))
              .withPath(s"node_modules/${nodeModulesPath.relativize(path).toString}")
              .build()
          )
        }

        ch
      }

      def getOrCreateChunk(moduleID: ModuleSet.ModuleID): JSChunk = {
        chunks.getOrElseUpdate(
          moduleID, {

            val module = modules(moduleID)
            val ch = new JSChunk(moduleID.id)

            nodeModulesChunk.foreach(ch.addDependency(_))

            module.internalDependencies.foreach { mid =>
              ch.addDependency(getOrCreateChunk(mid))
            }

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

      val entrypoints =
        memOutput.fileNames().filterNot(_.endsWith(".map")).map(ModuleIdentifier.forFile(_))

      val compiler = new Compiler

      val compilerOptions = new CompilerOptions
      compilerOptions.setModuleResolutionMode(ResolutionMode.NODE)
      compilerOptions.setDependencyOptions(
        DependencyOptions.pruneForEntryPoints(Arrays.asList(entrypoints.toArray: _*))
      )
      compilerOptions.setProcessCommonJSModules(true)
      linkerConfig.moduleKind match {
        case ModuleKind.NoModule =>
          compilerOptions.setChunkOutputType(CompilerOptions.ChunkOutputType.GLOBAL_NAMESPACE)
        case _ => // otherwise force ES modules
          compilerOptions.setChunkOutputType(CompilerOptions.ChunkOutputType.ES_MODULES)
      }

      compilerOptions.setLanguage(linkerConfig.esFeatures.esVersion match {
        case ESVersion.ES2015 => CompilerOptions.LanguageMode.ECMASCRIPT_2015
      })

      compiler.compileModules(
        Collections.emptyList[SourceFile],
        Arrays.asList((nodeModulesChunk.toList ::: chunks.values.toList): _*),
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
