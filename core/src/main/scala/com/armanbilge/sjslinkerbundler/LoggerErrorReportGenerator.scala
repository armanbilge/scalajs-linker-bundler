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

/*
 * Scala.js (https://www.scala-js.org/)
 *
 * Copyright EPFL.
 *
 * Licensed under Apache License 2.0
 * (https://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package com.armanbilge.sjslinkerbundler

import com.google.javascript.jscomp._

import org.scalajs.logging._

private final class LoggerErrorReportGenerator(logger: Logger)
    extends SortingErrorManager.ErrorReportGenerator {

  def generateReport(manager: SortingErrorManager): Unit = {
    /* We should use `manager.getSortedDiagnostics()` rather than using
     * separately getWarnings() and getErrors(), but it is package-private.
     */
    manager.getWarnings().forEach { warning => logger.warn(warning.toString()) }
    manager.getErrors().forEach { error => logger.error(error.toString()) }

    val errorCount = manager.getErrorCount
    val warningCount = manager.getWarningCount
    val msg = s"Closure: $errorCount error(s), $warningCount warning(s)"

    if (errorCount > 0)
      logger.error(msg)
    else if (warningCount > 0)
      logger.warn(msg)
    else
      logger.info(msg)
  }
}
