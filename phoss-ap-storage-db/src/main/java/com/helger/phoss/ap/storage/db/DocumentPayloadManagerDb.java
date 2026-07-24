/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phoss.ap.storage.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;

import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;
import com.helger.phoss.ap.api.spi.IDocumentPayloadManagerProviderSPI;
import com.helger.phoss.ap.db.APJdbcMetaManager;
import com.helger.phoss.ap.db.config.APJdbcConfiguration;
import com.helger.phoss.ap.db.AbstractAPJdbcManager;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;

/**
 * Implementation of {@link IDocumentPayloadManager} and
 * {@link IDocumentPayloadManagerProviderSPI} that stores payloads in the database.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class DocumentPayloadManagerDb extends AbstractAPJdbcManager implements
                                       IDocumentPayloadManager,
                                       IDocumentPayloadManagerProviderSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DocumentPayloadManagerDb.class);
  private String m_sTableName;

  public DocumentPayloadManagerDb ()
  {
    super (APBasicMetaManager.getTimestampMgr ());
  }

  @NonNull
  private String _getTableName ()
  {
    String sTableName = m_sTableName;
    if (sTableName == null)
    {
      final APJdbcConfiguration aJdbcConfig = APJdbcMetaManager.getJdbcConfig ();
      sTableName = com.helger.db.api.helper.DBSystemHelper.getTableNamePrefix (aJdbcConfig.getJdbcDatabaseSystemType (),
                                                                                 aJdbcConfig.getJdbcSchema ()) +
                   "ap_payload";
      m_sTableName = sTableName;
    }
    return sTableName;
  }

  @Override
  @NonNull
  @Nonempty
  public String getID ()
  {
    return "db";
  }

  @Override
  @NonNull
  public IDocumentPayloadManager createDocumentPayloadManager ()
  {
    return this;
  }

  private void _initFlyway ()
  {
    final APJdbcConfiguration aJdbcConfig = APJdbcMetaManager.getJdbcConfig ();
    final com.helger.config.IConfig aConfig = com.helger.phoss.ap.api.config.APConfigProvider.getConfig ();
    final com.helger.db.flyway.FlywayConfiguration aFlywayCfg = new com.helger.phoss.ap.db.config.APFlywayConfigurationBuilder (aConfig,
                                                                                                                                   aJdbcConfig).build ();
    if (aFlywayCfg.isFlywayEnabled ())
    {
      final String sPayloadLocation = "db/phoss-ap-flyway-payload-" +
                                      aJdbcConfig.getJdbcDatabaseSystemType ().getID ();
      LOGGER.info ("Running payload Flyway migrations from " + sPayloadLocation);
      com.helger.db.flyway.FlywayMigrationRunner.runFlyway (aJdbcConfig,
                                                            aFlywayCfg,
                                                            sPayloadLocation,
                                                            (org.flywaydb.core.api.migration.JavaMigration []) null,
                                                            (org.flywaydb.core.api.callback.Callback []) null);
    }
  }

  @Override
  public void verifyConfiguration ()
  {
    _initFlyway ();
    try
    {
      final boolean bExists = newExecutor ().queryCount ("SELECT COUNT(*) FROM " + _getTableName ()) >= 0;
      if (!bExists)
        throw new IllegalStateException ("Table " + _getTableName () + " is missing!");
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to verify database configuration for payload storage", ex);
    }
  }

  @NonNull
  private String _createPath (@NonNull final String sBaseDir, @NonNull final String sFilename)
  {
    return sBaseDir + (sBaseDir.endsWith ("/") ? "" : "/") + sFilename;
  }

  @Override
  @NonNull
  public String storeDocument (@NonNull final String sBaseDir,
                               @NonNull final OffsetDateTime aReferenceDT,
                               @NonNull final String sFilename,
                               byte @NonNull [] aBytes)
  {
    final String sPath = _createPath (sBaseDir, sFilename);
    final DBExecutor aExecutor = newExecutor ();
    
    // Check if it exists
    if (existsDocument (sPath))
    {
      final int nRows = (int) aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                                _getTableName () +
                                                                " SET reference_dt=?, content=? WHERE file_path=?",
                                                                new ConstantPreparedStatementDataProvider (toTS (aReferenceDT),
                                                                                                           aBytes,
                                                                                                           sPath));
      if (nRows == 1)
        LOGGER.info ("Updated existing database payload for " + sPath);
      else
        LOGGER.warn ("Failed to update existing database payload for " + sPath);
    }
    else
    {
      final int nRows = (int) aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                _getTableName () +
                                                                " (file_path, reference_dt, content) VALUES (?, ?, ?)",
                                                                new ConstantPreparedStatementDataProvider (sPath,
                                                                                                           toTS (aReferenceDT),
                                                                                                           aBytes));
      if (nRows == 1)
        LOGGER.info ("Stored database payload for " + sPath);
      else
        LOGGER.warn ("Failed to store database payload for " + sPath);
    }
    
    return sPath;
  }

  @Override
  @NonNull
  public OutputStream openDocumentStreamForWrite (@NonNull final String sBaseDir,
                                                  @NonNull final OffsetDateTime aReferenceDT,
                                                  @NonNull final String sFilename,
                                                  @NonNull final String sFileExt,
                                                  @NonNull final Consumer <String> aPathConsumer)
  {
    final String sFinalFilename = sFilename + sFileExt;
    final String sPath = _createPath (sBaseDir, sFinalFilename);
    aPathConsumer.accept (sPath);

    return new ByteArrayOutputStream ()
    {
      @Override
      public void close () throws IOException
      {
        super.close ();
        storeDocument (sBaseDir, aReferenceDT, sFinalFilename, toByteArray ());
      }
    };
  }

  @Override
  @NonNull
  public OutputStream openTemporaryDocumentStreamForWrite (@NonNull final String sBaseDir,
                                                           @NonNull final OffsetDateTime aReferenceDT,
                                                           @NonNull final Consumer <String> aPathConsumer)
  {
    // Generate a temporary filename
    final String sFilename = "temp_" + System.nanoTime () + ".tmp";
    return openDocumentStreamForWrite (sBaseDir, aReferenceDT, sFilename, "", aPathConsumer);
  }

  @Override
  @NonNull
  public String renameFile (@NonNull final String sSrcFile,
                            @NonNull final String sTargetDir,
                            @NonNull @Nonempty final String sBaseName,
                            @NonNull @Nonempty final String sFileExt)
  {
    final String sTargetPath = _createPath (sTargetDir, sBaseName + sFileExt);
    final DBExecutor aExecutor = newExecutor ();
    final int nRows = (int) aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                              _getTableName () +
                                                              " SET file_path=? WHERE file_path=?",
                                                              new ConstantPreparedStatementDataProvider (sTargetPath,
                                                                                                         sSrcFile));
    if (nRows == 1)
      LOGGER.info ("Renamed payload from " + sSrcFile + " to " + sTargetPath);
    else
      LOGGER.warn ("Failed to rename payload from " + sSrcFile + " to " + sTargetPath + " (row count " + nRows + ")");
    return sTargetPath;
  }

  @Override
  public byte @NonNull [] readDocument (@NonNull final String sAbsolutePath)
  {
    final var aJdbcConfig = APJdbcMetaManager.getJdbcConfig ();
    final var aDSP = new com.helger.db.jdbc.DataSourceProviderFromJdbcConfiguration (aJdbcConfig);
    final javax.sql.DataSource aDS = aDSP.getDataSource ();

    try (final java.sql.Connection aConn = aDS.getConnection ();
         final java.sql.PreparedStatement aPS = aConn.prepareStatement ("SELECT content FROM " + _getTableName () + " WHERE file_path=?"))
    {
      aPS.setString (1, sAbsolutePath);
      try (final java.sql.ResultSet aRS = aPS.executeQuery ())
      {
        if (aRS.next ())
        {
          final java.sql.Blob aBlob = aRS.getBlob (1);
          if (aBlob != null)
          {
            return aBlob.getBytes (1, (int) aBlob.length ());
          }
          final byte [] aBytes = aRS.getBytes (1);
          if (aBytes != null)
          {
            return aBytes;
          }
          throw new IllegalStateException ("Payload content was null for path: " + sAbsolutePath);
        }
      }
    }
    catch (final java.sql.SQLException ex)
    {
      throw new IllegalStateException ("Failed to read payload from DB for path: " + sAbsolutePath, ex);
    }

    // Fallback to filesystem
    final java.io.File aFile = new java.io.File (sAbsolutePath);
    if (aFile.exists ())
    {
      LOGGER.info ("Payload not found in DB for " + sAbsolutePath + ", falling back to filesystem");
      try
      {
        return java.nio.file.Files.readAllBytes (aFile.toPath ());
      }
      catch (final java.io.IOException ex)
      {
        throw new IllegalStateException ("Failed to read fallback file: " + sAbsolutePath, ex);
      }
    }
    throw new IllegalArgumentException ("No payload found in DB or filesystem for path: " + sAbsolutePath);
  }

  @Override
  @NonNull
  public InputStream openDocumentStreamForRead (@NonNull final String sAbsolutePath)
  {
    return new ByteArrayInputStream (readDocument (sAbsolutePath));
  }

  @Override
  public boolean deleteDocument (@NonNull final String sAbsolutePath)
  {
    final DBExecutor aExecutor = newExecutor ();
    final int nRows = (int) aExecutor.insertOrUpdateOrDelete ("DELETE FROM " + _getTableName () + " WHERE file_path=?",
                                                              new ConstantPreparedStatementDataProvider (sAbsolutePath));
    return nRows > 0;
  }

  @Override
  public boolean existsDocument (@NonNull final String sAbsolutePath)
  {
    final DBExecutor aExecutor = newExecutor ();
    final long nCount = aExecutor.queryCount ("SELECT COUNT(*) FROM " + _getTableName () + " WHERE file_path=?",
                                              new ConstantPreparedStatementDataProvider (sAbsolutePath));
    return nCount > 0;
  }
}
