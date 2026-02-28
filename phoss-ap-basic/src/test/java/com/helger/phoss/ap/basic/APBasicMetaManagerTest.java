package com.helger.phoss.ap.basic;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;

import org.junit.Rule;
import org.junit.Test;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for class {@link APBasicMetaManager}.
 *
 * @author Philip Helger
 */
public final class APBasicMetaManagerTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testBasic ()
  {
    final var aTimestampMgr = APBasicMetaManager.getTimestampMgr ();
    final OffsetDateTime aDT1 = aTimestampMgr.getCurrentDateTime ();
    assertNotNull (aDT1);
    ThreadHelper.sleep (10);
    final OffsetDateTime aDT2 = aTimestampMgr.getCurrentDateTime ();
    assertNotNull (aDT2);
    assertTrue (aDT2.isAfter (aDT1));
  }
}
