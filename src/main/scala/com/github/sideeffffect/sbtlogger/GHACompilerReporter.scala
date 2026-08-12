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

import com.github.sideeffffect.sbtlogger.GHACompilerReporter.annotationFor
import sbt.sbtloggerhack.apiAdapter.{ReporterAdapter, toFilePosition}
import xsbti.{Position, Problem}

class GHACompilerReporter(delegate: xsbti.Reporter) extends ReporterAdapter(delegate) {

  override def reset(): Unit = delegate.reset()
  override def hasErrors: Boolean = delegate.hasErrors
  override def hasWarnings: Boolean = delegate.hasWarnings
  override def printSummary(): Unit = delegate.printSummary()
  override def problems(): Array[Problem] = delegate.problems()
  override def comment(pos: Position, msg: String): Unit = delegate.comment(pos, msg)

  override def log(problem: Problem): Unit = {
    annotationFor(problem).foreach(println)
    delegateLog(problem)
  }
}

object GHACompilerReporter {

  /** Position of a compiler [[xsbti.Problem]] within a source file, with 1-based line/column numbers. */
  final case class FilePosition(
      sourcePath: String,
      startLine: Option[Int],
      endLine: Option[Int],
      startColumn: Option[Int],
      endColumn: Option[Int],
  )

  /** Renders a compiler problem as a GitHub Actions annotation, if it carries a source position. */
  def annotationFor(problem: Problem): Option[String] =
    toFilePosition(problem.position()).map { position =>
      GHACommands.annotation(
        severity = severityOf(problem.severity()),
        message = problem.message(),
        title = Option(problem.category()).filter(_.nonEmpty),
        file = Some(position.sourcePath),
        line = position.startLine,
        endLine = position.endLine,
        col = position.startColumn,
        endColumn = position.endColumn,
      )
    }

  private def severityOf(severity: xsbti.Severity): GHACommands.Severity =
    severity match {
      case xsbti.Severity.Info  => GHACommands.Severity.Notice
      case xsbti.Severity.Warn  => GHACommands.Severity.Warning
      case xsbti.Severity.Error => GHACommands.Severity.Error
    }
}
