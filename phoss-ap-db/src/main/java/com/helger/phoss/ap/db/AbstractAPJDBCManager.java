package com.helger.phoss.ap.db;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.ap.api.datetime.IAPTimestampManager;

/**
 * Abstract base class for all AP JDBC manager
 *
 * @author Philip Helger
 */
public abstract class AbstractAPJDBCManager extends AbstractJDBCEnabledManager
{
  private final IAPTimestampManager m_aTimestampMgr;

  protected AbstractAPJDBCManager (@NonNull final IAPTimestampManager aTimestampMgr)
  {
    super (APDBExecutor.createNew ());
    m_aTimestampMgr = aTimestampMgr;
  }

  @NonNull
  @Nonempty
  protected final String createUniqueRowID ()
  {
    // Create a UUID v4
    return UUID.randomUUID ().toString ();
  }

  @NonNull
  protected final OffsetDateTime now ()
  {
    return m_aTimestampMgr.getCurrentDateTime ();
  }
}
