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

import scala.collection.mutable.ListBuffer

class GHALogAppenderSuite extends munit.FunSuite {

  private def withAppender(body: (GHALogAppender, ListBuffer[String]) => Unit): List[String] = {
    val emitted = ListBuffer.empty[String]
    body(new GHALogAppender(emitted += _), emitted)
    emitted.toList
  }

  test("a plain compilation is one balanced group") {
    val out = withAppender { (a, _) =>
      a.compilationBlockStart("")
      a.compilationBlockEnd("")
    }
    assertEquals(out, List("::group::Compile", "::endgroup::"))
  }

  test("nested compilation blocks collapse into the outermost group") {
    // `Test / compile` runs `Compile / compile` as a dependency, so the hooks nest.
    val out = withAppender { (a, _) =>
      a.compilationTestBlockStart("")
      a.compilationBlockStart("")
      a.compilationBlockEnd("")
      a.compilationTestBlockEnd("")
    }
    assertEquals(out, List("::group::Compile (test)", "::endgroup::"))
  }

  test("overlapping (parallel) suite groups still collapse into a single balanced group") {
    val out = withAppender { (a, _) =>
      a.testSuiteStart("A", "")
      a.testSuiteStart("B", "")
      a.testSuiteFinished("A", "")
      a.testSuiteFinished("B", "")
    }
    assertEquals(out, List("::group::Test: A", "::endgroup::"))
  }

  test("an unbalanced close never emits a stray endgroup") {
    val out = withAppender { (a, _) =>
      a.compilationBlockEnd("")
    }
    assertEquals(out, Nil)
  }

  test("a test failure is an error annotation with an escaped title") {
    val out = withAppender { (a, _) =>
      a.testSuiteStart("MySuite", "")
      a.testFailed("MySuite.this fails", "boom", "")
      a.testSuiteFinished("MySuite", "")
    }
    assertEquals(
      out,
      List(
        "::group::Test: MySuite",
        "::error title=Test failed%3A MySuite.this fails::boom",
        "::endgroup::",
      ),
    )
  }

  test("individual test start/finish and passing tests emit nothing") {
    val out = withAppender { (a, _) =>
      a.testStart("t", "")
      a.testFinished("t", "Success", 1L, "")
    }
    assertEquals(out, Nil)
  }
}
