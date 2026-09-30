// Copyright (c) 2026 Broadcom. All Rights Reserved.
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
package com.rabbitmq.client.credentials.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

public class TlsUtilsTest {

  @Test
  @EnabledForJreRange(max = JRE.JAVA_19)
  void setNamedGroupsShouldBeUnsupportedBeforeJava20() {
    assertThatThrownBy(() -> TlsUtils.setNamedGroups(new SSLParameters(), new String[] {"x25519"}))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Java 20");
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_20)
  void setNamedGroupsShouldSetGroupsFromJava20() throws Exception {
    SSLParameters parameters = new SSLParameters();
    String[] namedGroups = new String[] {"x25519", "secp256r1"};
    TlsUtils.setNamedGroups(parameters, namedGroups);
    // compiled for Java 8, so SSLParameters#getNamedGroups() is not visible at compile time
    Object result = SSLParameters.class.getMethod("getNamedGroups").invoke(parameters);
    assertThat((String[]) result).containsExactly(namedGroups);
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_20)
  void setNamedGroupsShouldWrapErrorsInOAuth2Exception() {
    assertThatThrownBy(
            () -> TlsUtils.setNamedGroups(new SSLParameters(), new String[] {"x25519", "x25519"}))
        .isInstanceOf(OAuth2Exception.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }
}
