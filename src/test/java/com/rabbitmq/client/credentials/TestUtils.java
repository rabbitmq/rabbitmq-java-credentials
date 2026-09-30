// Copyright (c) 2024-2025 Broadcom. All Rights Reserved.
// The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.
package com.rabbitmq.client.credentials;

import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.function.Supplier;

public final class TestUtils {

  private TestUtils() {}

  public static Duration waitAtMost(
      Duration timeout,
      Duration waitTime,
      CallableBooleanSupplier condition,
      Supplier<String> message)
      throws Exception {
    if (condition.getAsBoolean()) {
      return Duration.ZERO;
    }
    Duration waitedTime = Duration.ZERO;
    Exception exception = null;
    while (waitedTime.compareTo(timeout) <= 0) {
      Thread.sleep(waitTime.toMillis());
      waitedTime = waitedTime.plus(waitTime);
      try {
        if (condition.getAsBoolean()) {
          return waitedTime;
        }
        exception = null;
      } catch (Exception e) {
        exception = e;
      }
    }
    String msg;
    if (message == null) {
      msg = "Waited " + timeout.getSeconds() + " second(s), condition never got true";
    } else {
      msg = "Waited " + timeout.getSeconds() + " second(s), " + message.get();
    }
    if (exception == null) {
      fail(msg);
    } else {
      fail(msg, exception);
    }
    return waitedTime;
  }

  public static Duration waitAtMost(
      Duration timeout, Duration waitTime, CallableBooleanSupplier condition) throws Exception {
    return waitAtMost(timeout, waitTime, condition, null);
  }

  public static <A, B> Pair<A, B> pair(A v1, B v2) {
    return new Pair<>(v1, v2);
  }

  public interface CallableBooleanSupplier {
    boolean getAsBoolean() throws Exception;
  }

  public static class Pair<A, B> {

    private final A v1;
    private final B v2;

    private Pair(A v1, B v2) {
      this.v1 = v1;
      this.v2 = v2;
    }

    public A v1() {
      return this.v1;
    }

    public B v2() {
      return this.v2;
    }
  }
}
