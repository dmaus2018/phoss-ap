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

/**
 * Special configuration for AP JDBC stuff
 *
 * @author Philip Helger
 */
@Immutable
public final class APJDBCConfiguration
{
  private APJDBCConfiguration ()
  {}

  @NonNull
  private static IConfigWithFallback _getConfig ()
  {
    return APConfiguration.getConfig ();
  }

  @Nullable
  public static String getJdbcDriver ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_DRIVER);
  }

  @Nullable
  public static String getJdbcUrl ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_URL);
  }

  @Nullable
  public static String getJdbcUser ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_USER);
  }

  @Nullable
  public static String getJdbcPassword ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_PASSWORD);
  }

  @Nullable
  public static String getJdbcSchema ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_SCHEMA);
  }

  @Nullable
  public static String getTargetDatabaseType ()
  {
    return _getConfig ().getAsString (APConfigurationProperties.JDBC_DATABASE_TYPE);
  }

  public static boolean isJdbcExecutionTimeWarningEnabled ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_ENABLED,
                                       APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_ENABLED_DEFAULT);
  }

  public static long getJdbcExecutionTimeWarningMilliseconds ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_MS,
                                    APConfigurationProperties.JDBC_EXECUTION_TIME_WARNING_MS_DEFAULT);
  }

  public static boolean isJdbcDebugConnections ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.JDBC_DEBUG_CONNECTIONS,
                                       APConfigurationProperties.JDBC_DEBUG_CONNECTIONS_DEFAULT);
  }

  public static boolean isJdbcDebugTransactions ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.JDBC_DEBUG_TRANSACTIONS,
                                       APConfigurationProperties.JDBC_DEBUG_TRANSACTIONS_DEFAULT);
  }

  public static boolean isJdbcDebugSQL ()
  {
    return _getConfig ().getAsBoolean (APConfigurationProperties.JDBC_DEBUG_SQL,
                                       APConfigurationProperties.JDBC_DEBUG_SQL_DEFAULT);
  }

  public static int getJdbcPoolingMaxConnections ()
  {
    return _getConfig ().getAsInt (APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS,
                                   APConfigurationProperties.JDBC_POOLING_MAX_CONNECTIONS_DEFAULT);
  }

  public static long getJdbcPoolingMaxWaitMillis ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS,
                                    APConfigurationProperties.JDBC_POOLING_MAX_WAIT_MILLIS_DEFAULT);
  }

  public static long getJdbcPoolingBetweenEvictionRunsMillis ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS,
                                    APConfigurationProperties.JDBC_POOLING_BETWEEN_EVICTIONS_RUNS_MILLIS_DEFAULT);
  }

  public static long getJdbcPoolingMinEvictableIdleMillis ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS,
                                    APConfigurationProperties.JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS_DEFAULT);
  }

  public static long getJdbcPoolingRemoveAbandonedTimeoutMillis ()
  {
    return _getConfig ().getAsLong (APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS,
                                    APConfigurationProperties.JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS_DEFAULT);
  }
}
