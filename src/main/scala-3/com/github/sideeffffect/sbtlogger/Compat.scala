/*
 * Copyright 2026 Ondra Pelech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.github.sideeffffect.sbtlogger

import sbt._
import sbt.Keys._
import sbt.sbtloggerhack.Unhide

/** sbt 2.x flavour of the task settings that differ between sbt versions.
  *
  * sbt 2.x caches task results, and refuses to (re)define a task whose result type has no `JsonFormat` (such as
  * `compile`'s `CompileAnalysis` or `compilerReporter`'s `xsbti.Reporter`) unless the assignment opts out with
  * `Def.uncached`. The side-effecting logger tasks are also wrapped in `Def.uncached` so that they run on every
  * invocation instead of being served from the cache. sbt 2.x removed the log4j-based `extraLoggers` extension point
  * that the sbt 1.x flavour uses, so nothing is contributed for it here.
  */
object Compat {

  import SbtGitHubActionsLogger._

  // GitHub Actions has no concept of TeamCity's parallel message "flows", so the flowId that the
  // appender API still carries is always empty here.
  private val noFlowId: String = ""

  def loggerTaskSettings: Seq[Def.Setting[_]] =
    Seq(
      startCompilationLogger := Def.uncached(ghaLogAppender.compilationBlockStart(noFlowId)),
      startTestCompilationLogger := Def.uncached(ghaLogAppender.compilationTestBlockStart(noFlowId)),
      endCompilationLogger := Def.uncached(ghaLogAppender.compilationBlockEnd(noFlowId)),
      endTestCompilationLogger := Def.uncached(ghaLogAppender.compilationTestBlockEnd(noFlowId)),
      Compile / compile := Def.uncached((Compile / compile).dependsOn(startCompilationLogger).value),
      Test / compile := Def.uncached((Test / compile).dependsOn(startTestCompilationLogger).value),
      ghaEndCompilation := Def.uncached(endCompilationLogger.triggeredBy(Compile / compile).value),
      ghaEndTestCompilation := Def.uncached(endTestCompilationLogger.triggeredBy(Test / compile).value),
    ) ++
      inConfig(Compile)(Seq(reporterSetting)) ++
      inConfig(Test)(Seq(reporterSetting))

  private def reporterSetting: Def.Setting[_] =
    compile / Unhide.compilerReporter := Def.uncached {
      new GHACompilerReporter((compile / Unhide.compilerReporter).value)
    }
}
