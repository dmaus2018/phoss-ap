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
package com.helger.phoss.ap.db;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.helper.DBSystemHelper;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.ap.db.config.APJDBCConfiguration;

/**
 * Special {@link DBExecutor} for the AP
 *
 * @author Philip Helger
 */
public final class APDBExecutor extends DBExecutor
{
  public static final EDatabaseSystemType DB_SYSTEM_TYPE = EDatabaseSystemType.POSTGRESQL;
  public static final String TABLE_NAME_PREFIX;
  static
  {
    TABLE_NAME_PREFIX = DBSystemHelper.getTableNamePrefix (DB_SYSTEM_TYPE, APJDBCConfiguration.getJdbcSchema ());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (APDBExecutor.class);

  private static APDataSourceProvider s_aDSP;

  @NonNull
  private static APDataSourceProvider _getDSPNotNull ()
  {
    final APDataSourceProvider ret = s_aDSP;
    if (ret == null)
      throw new IllegalStateException ("The DataSourceProvider was never initialized");
    return ret;
  }

  public static void setDataSourceProvider (@NonNull final APDataSourceProvider aDSP)
  {
    ValueEnforcer.notNull (aDSP, "DataSourceProvider");
    if (s_aDSP != null)
      throw new IllegalStateException ("Another DataSourceProvider was already initialized");
    s_aDSP = aDSP;
  }

  private APDBExecutor ()
  {
    super (_getDSPNotNull ());

    setDebugConnections (APJDBCConfiguration.isJdbcDebugConnections ());
    setDebugTransactions (APJDBCConfiguration.isJdbcDebugTransactions ());
    setDebugSQLStatements (APJDBCConfiguration.isJdbcDebugSQL ());

    if (APJDBCConfiguration.isJdbcExecutionTimeWarningEnabled ())
    {
      final long nMillis = APJDBCConfiguration.getJdbcExecutionTimeWarningMilliseconds ();
      if (nMillis > 0)
        setExecutionDurationWarnMS (nMillis);
      else
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Ignoring execution time warning setting because it is invalid.");
    }
    else
    {
      setExecutionDurationWarnMS (0);
    }
  }

  @NonNull
  public static Supplier <APDBExecutor> createNew ()
  {
    return APDBExecutor::new;
  }
}
