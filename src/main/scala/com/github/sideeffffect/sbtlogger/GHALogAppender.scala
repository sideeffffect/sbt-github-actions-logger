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

import java.io.{PrintWriter, StringWriter}

/** Turns sbt compilation and test events into GitHub Actions workflow commands.
  *
  * Compilation and test suites are wrapped in collapsible [[GHACommands.startGroup log groups]] and failures are
  * surfaced as [[GHACommands.annotation annotations]]. GitHub Actions log groups cannot be nested, so individual tests
  * do not open their own group; they only contribute output (an annotation on failure) inside the enclosing test-suite
  * group.
  *
  * The `flowId` parameters are a leftover of the TeamCity model (which supported parallel message flows); GitHub
  * Actions has no equivalent, so they are ignored.
  */
class GHALogAppender extends LogAppender {

  private def emit(command: String): Unit = println(command)

  // --- generic sbt log messages ---------------------------------------------

  // sbt already prints [warn]/[error] lines to the console, and compiler problems are turned into
  // file annotations by GHACompilerReporter, so ordinary log messages are not duplicated as extra
  // annotations here. The one exception is a test that could not even be started (see below).
  def log(level: sbt.Level.Value, message: => String, flowId: String): Unit =
    if (level == sbt.Level.Error) processSpecialErrorsMessage(message, flowId)

  def log(level: String, message: => String, flowId: String): Unit =
    if (level == "ERROR") processSpecialErrorsMessage(message, flowId)

  private def processSpecialErrorsMessage(message: String, flowId: String): Unit = {
    val suffix = "java.lang.ExceptionInInitializerError"
    val prefix = "Could not run test"
    if (message.contains(suffix) && message.contains(prefix)) {
      val testName = message.substring(message.indexOf(prefix) + prefix.length, message.indexOf(suffix)).trim()
      testFailed(testName, message, flowId)
    }
  }

  // --- compilation -----------------------------------------------------------

  def compilationBlockStart(flowId: String): Unit = emit(GHACommands.startGroup("Compile"))
  def compilationBlockEnd(flowId: String): Unit = emit(GHACommands.endGroup)
  def compilationTestBlockStart(flowId: String): Unit = emit(GHACommands.startGroup("Compile (test)"))
  def compilationTestBlockEnd(flowId: String): Unit = emit(GHACommands.endGroup)

  // --- test suites -----------------------------------------------------------

  def testSuiteStart(name: String, flowId: String): Unit = emit(GHACommands.startGroup(s"Test: $name"))

  def testSuiteSuccessfulResult(name: String, flowId: String): Unit = emit(GHACommands.endGroup)

  def testSuiteFailResult(name: String, t: Throwable, flowId: String): Unit = {
    emit(
      GHACommands.annotation(
        GHACommands.Severity.Error,
        message = stackTraceOf(t),
        title = Some(s"Test suite failed: $name"),
      ),
    )
    emit(GHACommands.endGroup)
  }

  // --- individual tests ------------------------------------------------------

  // No group is opened per test (groups cannot be nested inside the suite group). Passing and
  // finishing tests therefore produce no output; only failures and skips do.
  def testStart(name: String, flowId: String): Unit = ()

  def testFinished(name: String, status: String, duration: Long, flowId: String): Unit = ()

  def testFailed(name: String, details: String, flowId: String): Unit =
    emit(
      GHACommands.annotation(
        GHACommands.Severity.Error,
        message = details,
        title = Some(s"Test failed: $name"),
      ),
    )

  def testSkipped(name: String, flowId: String): Unit =
    emit(GHACommands.debug(s"Test skipped: $name"))

  def testCancelled(name: String, flowId: String): Unit =
    emit(GHACommands.debug(s"Test cancelled: $name"))

  private def stackTraceOf(t: Throwable): String = {
    val writer = new StringWriter
    t.printStackTrace(new PrintWriter(writer))
    writer.toString
  }
}
