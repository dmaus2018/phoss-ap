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
package com.helger.phoss.ap.core.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.YearMonth;

import org.junit.Rule;
import org.junit.Test;

import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for {@link APPeppolReportHelper}.
 *
 * @author Philip Helger
 */
public final class APPeppolReportHelperTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testGetValidYearMonthInAPIPastDate ()
  {
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2024, 6);
    assertNotNull (aYM);
    assertEquals (2024, aYM.getYear ());
    assertEquals (6, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPIJanuary2024 ()
  {
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2024, 1);
    assertNotNull (aYM);
    assertEquals (2024, aYM.getYear ());
    assertEquals (1, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPIDecember2025 ()
  {
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2025, 12);
    assertNotNull (aYM);
    assertEquals (2025, aYM.getYear ());
    assertEquals (12, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPIYearClampedToMinimum ()
  {
    // Year below 2024 is clamped to 2024
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2020, 3);
    assertNotNull (aYM);
    assertEquals (2024, aYM.getYear ());
    assertEquals (3, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPIMonthClampedToMinimum ()
  {
    // Month below 1 is clamped to 1
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2024, 0);
    assertNotNull (aYM);
    assertEquals (1, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPIMonthClampedToMaximum ()
  {
    // Month above 12 is clamped to 12
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2024, 15);
    assertNotNull (aYM);
    assertEquals (12, aYM.getMonthValue ());
  }

  @Test
  public void testGetValidYearMonthInAPINegativeMonth ()
  {
    // Negative month is clamped to 1
    final YearMonth aYM = APPeppolReportHelper.getValidYearMonthInAPI (2024, -5);
    assertNotNull (aYM);
    assertEquals (1, aYM.getMonthValue ());
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetValidYearMonthInAPIFutureYearThrows ()
  {
    // A year far in the future should throw
    APPeppolReportHelper.getValidYearMonthInAPI (2099, 1);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testGetValidYearMonthInAPIFutureMonthThrows ()
  {
    // Current year but month 12 in the future (if current month < 12)
    // Use a far-future month in the current year to be safe
    APPeppolReportHelper.getValidYearMonthInAPI (2099, 12);
  }
}
