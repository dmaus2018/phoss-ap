/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.db.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phoss.ap.api.config.APConfiguration;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

@Immutable
public final class APFlywayConfiguration
{
  private APFlywayConfiguration ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfiguration.getConfig ();
  }

  public static boolean isFlywayEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.FLYWAY_ENABLED, APConfigurationProperties.FLYWAY_ENABLED_DEFAULT);
  }

  @Nullable
  public static String getFlywayJdbcUrl ()
  {
    final String sRet = _getConfig ().getAsString (APConfigurationProperties.FLYWAY_JDBC_URL);
    return sRet != null ? sRet : APJDBCConfiguration.getJdbcUrl ();
  }

  @Nullable
  public static String getFlywayJdbcUser ()
  {
    final String sRet = _getConfig ().getAsString (APConfigurationProperties.FLYWAY_JDBC_USER);
    return sRet != null ? sRet : APJDBCConfiguration.getJdbcUser ();
  }

  @Nullable
  public static String getFlywayJdbcPassword ()
  {
    final String sRet = _getConfig ().getAsString (APConfigurationProperties.FLYWAY_JDBC_PASSWORD);
    return sRet != null ? sRet : APJDBCConfiguration.getJdbcPassword ();
  }

  public static boolean isFlywaySchemaCreate ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.FLYWAY_JDBC_SCHEMA_CREATE, APConfigurationProperties.FLYWAY_JDBC_SCHEMA_CREATE_DEFAULT);
  }

  public static int getFlywayBaselineVersion ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.FLYWAY_BASELINE_VERSION, APConfigurationProperties.FLYWAY_BASELINE_VERSION_DEFAULT);
  }
}
