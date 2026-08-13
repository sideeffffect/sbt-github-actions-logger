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
import java.util.concurrent.atomic.AtomicInteger

/** Turns sbt compilation and test events into GitHub Actions workflow commands.
  *
  * Compilation and test suites are wrapped in collapsible [[GHACommands.startGroup log groups]] and failures are
  * surfaced as [[GHACommands.annotation annotations]]. GitHub Actions log groups cannot be nested, so individual tests
  * do not open their own group; they only contribute output (an annotation on failure) inside the enclosing test-suite
  * group.
  *
  * GitHub Actions groups still cannot be nested even at the block level, but sbt readily produces nested blocks: `Test
  * / compile` runs `Compile / compile` as a dependency, so both fire their start hooks before either end hook, and test
  * suites may run in parallel. To stay valid the appender is depth-aware — it only emits the outermost `::group::` /
  * `::endgroup::` pair and swallows the inner ones, so the output is always a flat, balanced sequence of groups.
  *
  * The `flowId` parameters are a leftover of the TeamCity model (which supported parallel message flows); GitHub
  * Actions has no equivalent, so they are ignored.
  *
  * @param sink
  *   where rendered commands are written; defaults to stdout, overridable in tests.
  */
class GHALogAppender(sink: String => Unit) extends LogAppender {

  def this() = this(line => println(line))

  private def emit(command: String): Unit = sink(command)

  // Number of currently open (logical) groups. Only transitions to/from zero produce output.
  private val groupDepth = new AtomicInteger(0)

  private def openGroup(title: String): Unit =
    if (groupDepth.getAndIncrement() == 0) emit(GHACommands.startGroup(title))

  private def closeGroup(): Unit =
    // getAndUpdate never lets the counter go negative, so an unbalanced close (e.g. a failed compile
    // whose `triggeredBy` end hook did not run) is simply ignored rather than emitting a stray
    // `::endgroup::`.
    if (groupDepth.getAndUpdate(depth => if (depth > 0) depth - 1 else 0) == 1) emit(GHACommands.endGroup)

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

  def compilationBlockStart(flowId: String): Unit = openGroup("Compile")
  def compilationBlockEnd(flowId: String): Unit = closeGroup()
  def compilationTestBlockStart(flowId: String): Unit = openGroup("Compile (test)")
  def compilationTestBlockEnd(flowId: String): Unit = closeGroup()

  // --- test suites -----------------------------------------------------------

  def testSuiteStart(name: String, flowId: String): Unit = openGroup(s"Test: $name")

  def testSuiteFinished(name: String, flowId: String): Unit = closeGroup()

  def testSuiteFailed(name: String, t: Throwable, flowId: String): Unit =
    emit(
      GHACommands.annotation(
        GHACommands.Severity.Error,
        message = stackTraceOf(t),
        title = Some(s"Test suite failed: $name"),
      ),
    )

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
