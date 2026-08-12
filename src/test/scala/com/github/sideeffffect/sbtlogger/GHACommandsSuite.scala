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

import com.github.sideeffffect.sbtlogger.GHACommands.Severity

class GHACommandsSuite extends munit.FunSuite {

  test("startGroup renders a group command with an escaped title") {
    assertEquals(GHACommands.startGroup("Compile (test)"), "::group::Compile (test)")
    assertEquals(GHACommands.startGroup("line1\nline2"), "::group::line1%0Aline2")
  }

  test("endGroup takes no arguments") {
    assertEquals(GHACommands.endGroup, "::endgroup::")
  }

  test("debug renders a debug command") {
    assertEquals(GHACommands.debug("hello"), "::debug::hello")
  }

  test("annotation without a location is a bare error command") {
    assertEquals(
      GHACommands.annotation(Severity.Error, "boom"),
      "::error::boom",
    )
  }

  test("annotation renders severity, properties and message") {
    val rendered = GHACommands.annotation(
      severity = Severity.Warning,
      message = "unused import",
      title = Some("lint"),
      file = Some("src/main/scala/Foo.scala"),
      line = Some(10),
      endLine = Some(10),
      col = Some(3),
      endColumn = Some(9),
    )
    assertEquals(
      rendered,
      "::warning title=lint,file=src/main/scala/Foo.scala,line=10,endLine=10,col=3,endColumn=9::unused import",
    )
  }

  test("message escaping covers %, CR and LF") {
    assertEquals(GHACommands.escapeData("100% done\r\nnext"), "100%25 done%0D%0Anext")
  }

  test("property escaping additionally covers ':' and ','") {
    assertEquals(GHACommands.escapeProperty("a:b,c"), "a%3Ab%2Cc")
  }

  test("newlines in an annotation message are escaped, keeping the command on one line") {
    val rendered = GHACommands.annotation(Severity.Error, "first line\nsecond line")
    assertEquals(rendered, "::error::first line%0Asecond line")
    assert(!rendered.contains("\n"))
  }

  test("severity maps to the matching workflow command name") {
    assertEquals(GHACommands.annotation(Severity.Notice, "x"), "::notice::x")
    assertEquals(GHACommands.annotation(Severity.Warning, "x"), "::warning::x")
    assertEquals(GHACommands.annotation(Severity.Error, "x"), "::error::x")
  }
}
