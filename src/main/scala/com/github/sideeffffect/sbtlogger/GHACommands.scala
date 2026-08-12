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

/** Rendering of
  * [[https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands GitHub Actions workflow commands]].
  *
  * A workflow command is a specially formatted line printed to stdout:
  * {{{::name parameter1=data1,parameter2=data2::message}}} The GitHub Actions runner parses these lines and turns them
  * into log groups, annotations, etc.
  *
  * The escaping rules mirror the ones used by the official `@actions/core` toolkit.
  */
object GHACommands {

  /** Severity of an annotation, mapped to the corresponding workflow command. */
  sealed abstract class Severity(val command: String)
  object Severity {
    case object Notice extends Severity("notice")
    case object Warning extends Severity("warning")
    case object Error extends Severity("error")
  }

  // NB: this is *not* URL/percent-encoding. GitHub Actions only escapes this exact, tiny set of
  // characters (matching the official `@actions/core` toolkit); everything else, including spaces
  // and parentheses, must be passed through verbatim. `java.net.URLEncoder` would over-escape (it
  // turns spaces into `+` and encodes `(`, `)`, `/`, `=`, ... ), producing literal `%xx` noise in
  // the log, so the escaping is spelled out by hand here.

  /** Escaping for the message part of a command (the text after `::`). */
  def escapeData(value: String): String =
    value
      .replace("%", "%25")
      .replace("\r", "%0D")
      .replace("\n", "%0A")

  /** Escaping for a property value (before the `::`); on top of [[escapeData]] it also escapes the `:` and `,`
    * separators.
    */
  def escapeProperty(value: String): String =
    escapeData(value)
      .replace(":", "%3A")
      .replace(",", "%2C")

  /** Start of a collapsible log group. Groups cannot be nested. */
  def startGroup(title: String): String =
    s"::group::${escapeData(title)}"

  /** End of the currently open log group. */
  val endGroup: String =
    "::endgroup::"

  /** A debug message. Only shown when step debug logging is enabled (`ACTIONS_STEP_DEBUG`). */
  def debug(message: String): String =
    s"::debug::${escapeData(message)}"

  /** An annotation (notice / warning / error). When `file` is given the annotation is attached to the source location;
    * `line`/`col` are 1-based and omitted when absent.
    */
  def annotation(
      severity: Severity,
      message: String,
      title: Option[String] = None,
      file: Option[String] = None,
      line: Option[Int] = None,
      endLine: Option[Int] = None,
      col: Option[Int] = None,
      endColumn: Option[Int] = None,
  ): String = {
    val properties = List(
      "title" -> title.map(escapeProperty),
      "file" -> file.map(escapeProperty),
      "line" -> line.map(_.toString),
      "endLine" -> endLine.map(_.toString),
      "col" -> col.map(_.toString),
      "endColumn" -> endColumn.map(_.toString),
    ).collect { case (key, Some(value)) => s"$key=$value" }
    val renderedProperties = if (properties.isEmpty) "" else properties.mkString(" ", ",", "")
    s"::${severity.command}$renderedProperties::${escapeData(message)}"
  }
}
