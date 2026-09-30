// Copyright (c) 2024 Broadcom. All Rights Reserved.
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.credentials.Token;
import com.rabbitmq.client.credentials.TokenParser;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Token parser for <a href="https://www.rfc-editor.org/rfc/rfc6749#section-5.1">JSON OAuth 2 Access
 * tokens</a>.
 *
 * <p>Uses <a href="https://github.com/google/gson">GSON</a> for the JSON parsing.
 */
public final class GsonTokenParser implements TokenParser {

  static final String GSON_CLASS_NAME = "com.google.gson.Gson";

  /**
   * Create a parser.
   *
   * @throws IllegalStateException if GSON is not on the classpath
   */
  public GsonTokenParser() {
    try {
      Class.forName(GSON_CLASS_NAME, false, GsonTokenParser.class.getClassLoader());
    } catch (ClassNotFoundException | LinkageError e) {
      throw new IllegalStateException(
          "GSON is required to parse OAuth 2 token responses, "
              + "add com.google.code.gson:gson to the classpath "
              + "or use another token parser implementation",
          e);
    }
  }

  @Override
  public Token parse(String tokenAsString) {
    Map<String, Object> tokenAsMap;
    try {
      tokenAsMap = Json.parse(tokenAsString);
    } catch (Exception e) {
      throw new OAuth2Exception("Error while parsing token response as JSON", e);
    }
    if (tokenAsMap == null) {
      throw new OAuth2Exception("Token response is not a JSON object: " + tokenAsString);
    }
    Object accessTokenValue = tokenAsMap.get("access_token");
    if (!(accessTokenValue instanceof String)) {
      throw new OAuth2Exception(
          "Token response has no 'access_token' string field: " + tokenAsString);
    }
    String accessToken = (String) accessTokenValue;
    Object expiresInValue = tokenAsMap.get("expires_in");
    if (!(expiresInValue instanceof Number)) {
      throw new OAuth2Exception(
          "Token response has no 'expires_in' number field: " + tokenAsString);
    }
    // in seconds, see https://www.rfc-editor.org/rfc/rfc6749#section-5.1
    Duration expiresIn = Duration.ofSeconds(((Number) expiresInValue).longValue());
    Instant expirationTime =
        Instant.ofEpochMilli(System.currentTimeMillis() + expiresIn.toMillis());
    return new DefaultTokenInfo(accessToken, expirationTime);
  }

  // isolates the GSON references, so that the parser class can load without GSON
  private static final class Json {

    private static final Gson GSON = new Gson();
    // fromJson(String, Type) rather than fromJson(String, TypeToken), which requires GSON 2.10+
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static Map<String, Object> parse(String json) {
      return GSON.fromJson(json, MAP_TYPE);
    }
  }

  private static final class DefaultTokenInfo implements Token {

    private final String value;
    private final Instant expirationTime;

    private DefaultTokenInfo(String value, Instant expirationTime) {
      this.value = value;
      this.expirationTime = expirationTime;
    }

    @Override
    public String value() {
      return this.value;
    }

    @Override
    public Instant expirationTime() {
      return this.expirationTime;
    }
  }
}
