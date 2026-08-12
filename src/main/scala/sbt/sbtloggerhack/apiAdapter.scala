/*
 * Copyright 2013-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package sbt.sbtloggerhack

import com.github.sideeffffect.sbtlogger.GHACompilerReporter.FilePosition
import xsbti.Problem

/** Small shim isolating the parts of the sbt API that this plugin depends on.
  *
  * The pieces that differ between sbt 1.x and sbt 2.x live in the version-specific `Compat` objects (`src/main/scala-2`
  * and `src/main/scala-3`); everything here compiles unchanged on both.
  */
object apiAdapter {

  def toFilePosition(position: xsbti.Position): Option[FilePosition] = {
    val path = position.sourcePath()
    if (!path.isPresent) None
    else {
      def opt(value: java.util.Optional[Integer]): Option[Int] =
        if (value.isPresent) Some(value.get().intValue()) else None
      // sbt sometimes reports paths as "${BASE}/src/...."; GitHub annotations need a path relative
      // to the repository root, so strip that prefix.
      val sourcePath = path.get().replaceFirst("""\$\{BASE\}/""", "")
      // xsbti reports 0-based columns; GitHub Actions annotations use 1-based columns.
      Some(
        FilePosition(
          sourcePath = sourcePath,
          startLine = opt(position.startLine()),
          endLine = opt(position.endLine()),
          startColumn = opt(position.startColumn()).map(_ + 1),
          endColumn = opt(position.endColumn()).map(_ + 1),
        ),
      )
    }
  }

  abstract class ReporterAdapter(delegate: xsbti.Reporter) extends xsbti.Reporter {
    def delegateLog(problem: Problem): Unit = {
      delegate.log(problem)
    }
  }
}
