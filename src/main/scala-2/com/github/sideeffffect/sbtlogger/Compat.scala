/*
 * Copyright 2013-2021 JetBrains s.r.o.
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

/** sbt 1.x flavour of the task settings that differ between sbt versions.
  *
  * On sbt 1.x tasks are not result-cached, so the side-effecting logger tasks and the redefinitions of `compile` /
  * `compilerReporter` can be assigned directly. sbt 1.x also still exposes the log4j-based `extraLoggers` extension
  * point (see `extraLoggerSettings`), which sbt 2.x removed.
  */
object Compat {

  import SbtGitHubActionsLogger._

  // GitHub Actions has no concept of TeamCity's parallel message "flows", so the flowId that the
  // appender API still carries is always empty here.
  private val noFlowId: String = ""

  def loggerTaskSettings: Seq[Def.Setting[_]] =
    extraLoggerSettings ++ Seq(
      startCompilationLogger := ghaLogAppender.compilationBlockStart(noFlowId),
      startTestCompilationLogger := ghaLogAppender.compilationTestBlockStart(noFlowId),
      endCompilationLogger := ghaLogAppender.compilationBlockEnd(noFlowId),
      endTestCompilationLogger := ghaLogAppender.compilationTestBlockEnd(noFlowId),
      Compile / compile := (Compile / compile).dependsOn(startCompilationLogger).value,
      Test / compile := (Test / compile).dependsOn(startTestCompilationLogger).value,
      ghaEndCompilation := endCompilationLogger.triggeredBy(Compile / compile).value,
      ghaEndTestCompilation := endTestCompilationLogger.triggeredBy(Test / compile).value,
    ) ++
      inConfig(Compile)(Seq(reporterSetting)) ++
      inConfig(Test)(Seq(reporterSetting))

  private def reporterSetting: Def.Setting[_] =
    compile / Unhide.compilerReporter := {
      new GHACompilerReporter((compile / Unhide.compilerReporter).value)
    }

  private def extraLoggerSettings: Seq[Def.Setting[_]] = Seq(
    extraLoggers := {
      val currentFunction: Def.ScopedKey[_] => Seq[org.apache.logging.log4j.core.Appender] = extraLoggers.value
      // The log4j appender name must be unique per scoped key, so it is derived from the key's scope.
      (key: Def.ScopedKey[_]) => {
        val scope = "" + key.scope.project.hashCode()
        val appender = new GHALoggerAppender(ghaLogAppender, scope)
        appender.start()
        appender +: currentFunction(key)
      }
    },
  )
}
