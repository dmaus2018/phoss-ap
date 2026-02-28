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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.phoss.ap.api.IArchivalManager;
import com.helger.phoss.ap.api.IInboundForwardingAttemptManager;
import com.helger.phoss.ap.api.IInboundTransactionManager;
import com.helger.phoss.ap.api.IOutboundSendingAttemptManager;
import com.helger.phoss.ap.api.IOutboundTransactionManager;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.phoss.ap.db.flyway.APFlywayMigrator;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Central class to access all JDBC managers
 *
 * @author Philip Helger
 */
public final class APJDBCMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APJDBCMetaManager.class);

  private APDataSourceProvider m_aDSP;
  private OutboundTransactionManagerJDBC m_aOutboundTxMgr;
  private OutboundSendingAttemptManagerJDBC m_aOutboundAttemptMgr;
  private InboundTransactionManagerJDBC m_aInboundTxMgr;
  private IInboundForwardingAttemptManager m_aInboundAttemptMgr;
  private IArchivalManager m_aArchivalMgr;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public APJDBCMetaManager ()
  {}

  @NonNull
  public static APJDBCMetaManager getInstance ()
  {
    return getGlobalSingleton (APJDBCMetaManager.class);
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    LOGGER.info ("Initializing " + ClassHelper.getClassLocalName (this));
    try
    {
      final var aTimestampMgr = APBasicMetaManager.getTimestampMgr ();

      // Run Flyway
      APFlywayMigrator.runFlyway ();

      // Create DataSource and DBExecutor
      m_aDSP = new APDataSourceProvider ();
      APDBExecutor.setDataSourceProvider (m_aDSP);

      // Create managers
      m_aOutboundTxMgr = new OutboundTransactionManagerJDBC (aTimestampMgr);
      m_aOutboundAttemptMgr = new OutboundSendingAttemptManagerJDBC (aTimestampMgr);
      m_aInboundTxMgr = new InboundTransactionManagerJDBC (aTimestampMgr);
      m_aInboundAttemptMgr = new InboundForwardingAttemptManagerJDBC (aTimestampMgr);
      m_aArchivalMgr = new ArchivalManagerJDBC (aTimestampMgr);

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @Override
  protected void onBeforeDestroy (@NonNull final IScope aScopeToBeDestroyed) throws Exception
  {
    LOGGER.info ("Shutting down " + ClassHelper.getClassLocalName (this));
    if (m_aDSP != null)
    {
      try
      {
        m_aDSP.close ();
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error closing DataSource", ex);
      }
    }
  }

  @NonNull
  public static IOutboundTransactionManager getOutboundTransactionMgr ()
  {
    return getInstance ().m_aOutboundTxMgr;
  }

  @NonNull
  public static IOutboundSendingAttemptManager getOutboundSendingAttemptMgr ()
  {
    return getInstance ().m_aOutboundAttemptMgr;
  }

  @NonNull
  public static IInboundTransactionManager getInboundTransactionMgr ()
  {
    return getInstance ().m_aInboundTxMgr;
  }

  @NonNull
  public static IInboundForwardingAttemptManager getInboundForwardingAttemptMgr ()
  {
    return getInstance ().m_aInboundAttemptMgr;
  }

  @NonNull
  public static IArchivalManager getArchivalMgr ()
  {
    return getInstance ().m_aArchivalMgr;
  }
}
