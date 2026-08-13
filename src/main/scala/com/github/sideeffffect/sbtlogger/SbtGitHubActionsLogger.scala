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
import sbt.plugins.JvmPlugin

object SbtGitHubActionsLogger extends AutoPlugin {

  override def requires: Plugins = JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  lazy val ghaLogAppender = new GHALogAppender()
  lazy val startCompilationLogger: TaskKey[Unit] = TaskKey[Unit]("start-compilation-logger", "runs before compile")
  lazy val startTestCompilationLogger: TaskKey[Unit] =
    TaskKey[Unit]("start-test-compilation-logger", "runs before compile in test")
  lazy val endCompilationLogger: TaskKey[Unit] = TaskKey[Unit]("end-compilation-logger", "runs after compile")
  lazy val endTestCompilationLogger: TaskKey[Unit] =
    TaskKey[Unit]("end-test-compilation-logger", "runs after compile in test")
  lazy val ghaEndCompilation: TaskKey[Unit] = TaskKey[Unit]("gha-end-compilation", "")
  lazy val ghaEndTestCompilation: TaskKey[Unit] = TaskKey[Unit]("gha-end-test-compilation", "")

  // `GITHUB_ACTIONS` is set to "true" whenever a workflow runs; it is the canonical way to detect
  // that we are running inside GitHub Actions. `GITHUB_ACTION` (singular) is only the id of the
  // currently running action step, so it is used purely for the informational status message.
  val ghaFound: Boolean = sys.env.get("GITHUB_ACTIONS").contains("true")
  val ghaAction: Option[String] = sys.env.get("GITHUB_ACTION")

  // The plugin only adds GitHub Actions output; it must not otherwise change the build's behaviour.
  // In particular it must NOT touch `testResultLogger`: a failing test suite has to fail the build
  // (non-zero exit) exactly as it would without this plugin. Test failures are still surfaced to
  // GitHub Actions as `::error` annotations via the test listener in `loggerOnSettings`.
  override lazy val projectSettings: Seq[Def.Setting[_]] =
    if (ghaFound) loggerOnSettings
    else loggerOffSettings

  // The bulk of the settings redefine sbt tasks (`compile`, `compilerReporter`) or rely on the
  // log4j-based `extraLoggers`; both of these differ between sbt 1.x and sbt 2.x, so they live in
  // the version-specific `Compat` object.
  lazy val loggerOnSettings: Seq[Def.Setting[_]] = Seq(
    commands += ghaLoggerStatusCommand,
    // Per-suite log groups only when tests run sequentially; with parallel execution (the sbt
    // default) suites would interleave into one group, so we group nothing and rely on annotations.
    Test / testListeners += new GHAReportListener(ghaLogAppender, groupSuites = !(Test / parallelExecution).value),
  ) ++ Compat.loggerTaskSettings

  lazy val loggerOffSettings: Seq[Def.Setting[_]] = Seq(
    commands += ghaLoggerStatusCommand,
  )

  def ghaLoggerStatusCommand: Command = Command.command("sbt-github-actions-logger") { state =>
    doCommand(state)
  }

  private def doCommand(state: State): State = {
    println("Plugin sbt-github-actions-logger was loaded.")
    if (ghaFound) {
      val action = ghaAction.getOrElse("undefined")
      println(s"GitHub Actions was discovered. Logger is switched on. Current action: '$action'.")
    } else {
      println("GitHub Actions was not discovered. Logger is switched off.")
    }
    state
  }

}
