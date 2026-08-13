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

package com.github.sideeffffect.sbtlogger

import sbt._
import sbt.testing.{NestedTestSelector, OptionalThrowable, Status, TestSelector}

import java.io.{PrintWriter, StringWriter}

/** Reports test events as GitHub Actions workflow commands.
  *
  * @param ap
  *   the appender that renders the commands.
  * @param groupSuites
  *   whether to wrap each test suite in its own collapsible log group. This must be off when test suites run in
  *   parallel: GitHub Actions groups are a single global, non-nestable log stream, so concurrent suites would otherwise
  *   interleave into one mislabelled group. When off, suites are not grouped and only failures are reported (as
  *   annotations, which are immune to interleaving).
  */
class GHAReportListener(ap: LogAppender, groupSuites: Boolean) extends TestReportListener {

  val appender: LogAppender = ap

  def startGroup(name: String): Unit =
    if (groupSuites) appender.testSuiteStart(name, flowId)

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent): Unit = {
    event.detail.foreach(logSingleTest)
  }

  def formattedException(t: OptionalThrowable): String = {
    if (t.isDefined) {
      val w = new StringWriter
      val p = new PrintWriter(w)
      t.get.printStackTrace(p)
      w.toString
    } else ""
  }

  // GitHub Actions has no equivalent of TeamCity's parallel message "flows", so this value is
  // ignored by the appender; the thread name is a convenient, non-deprecated per-thread identifier
  // (`Thread.getId` is deprecated for removal since Java 19).
  def flowId: String = Thread.currentThread().getName

  protected def logSingleTest(event: sbt.testing.Event): Unit = {
    val fqn = event.fullyQualifiedName
    val status = event.status.toString
    val duration = event.duration
    val throwable = event.throwable

    val testName = event.selector match {
      case s: TestSelector =>
        if (fqn == s.testName()) fqn
        else fqn + "." + s.testName

      case ns: NestedTestSelector =>
        val prefix =
          if (fqn == ns.testName()) ""
          else fqn + "."
        prefix + ns.suiteId + "." + ns.testName

      case _ => fqn
    }

    appender.testStart(s"$testName", flowId)

    event.status match {
      case Status.Success                => // nothing extra to report
      case Status.Error | Status.Failure =>
        appender.testFailed(testName, formattedException(throwable), flowId)
      case Status.Skipped | Status.Ignored | Status.Pending =>
        appender.testSkipped(testName, flowId)
      case Status.Canceled =>
        appender.testCancelled(testName, flowId)
    }

    appender.testFinished(s"$testName", status, duration, flowId)
  }

  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable): Unit = {
    // The suite-level failure is always reported as an annotation; the group is only closed if we
    // opened one in the first place.
    appender.testSuiteFailed(name, t, flowId)
    if (groupSuites) appender.testSuiteFinished(name, flowId)
  }

  /** called if test completed */
  def endGroup(name: String, result: TestResult): Unit =
    if (groupSuites) appender.testSuiteFinished(name, flowId)

}
