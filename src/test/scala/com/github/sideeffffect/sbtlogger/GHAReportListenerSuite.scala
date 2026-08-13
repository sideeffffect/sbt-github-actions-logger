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

class GHAReportListenerSuite extends munit.FunSuite {

  private def run(groupSuites: Boolean)(body: GHAReportListener => Unit): List[String] = {
    val emitted = ListBuffer.empty[String]
    body(new GHAReportListener(new GHALogAppender(emitted += _), groupSuites))
    emitted.toList
  }

  test("groupSuites = true wraps the suite in a group and still annotates a suite failure") {
    val out = run(groupSuites = true) { listener =>
      listener.startGroup("MySuite")
      listener.endGroup("MySuite", new RuntimeException("kaboom"))
    }
    assertEquals(out.head, "::group::Test: MySuite")
    assertEquals(out.last, "::endgroup::")
    assert(out.exists(_.startsWith("::error title=Test suite failed%3A MySuite::")))
  }

  test("groupSuites = false (parallel) emits no group, only the failure annotation") {
    val out = run(groupSuites = false) { listener =>
      listener.startGroup("MySuite")
      listener.endGroup("MySuite", new RuntimeException("kaboom"))
    }
    assert(!out.exists(_.startsWith("::group::")), s"unexpected group in $out")
    assert(!out.contains("::endgroup::"), s"unexpected endgroup in $out")
    assertEquals(out.count(_.startsWith("::error title=Test suite failed%3A MySuite::")), 1)
  }
}
