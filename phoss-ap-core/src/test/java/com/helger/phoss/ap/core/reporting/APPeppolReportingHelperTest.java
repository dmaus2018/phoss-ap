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

import org.junit.Test;

import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for {@link APPeppolReportingHelper}.
 *
 * @author Philip Helger
 */
public final class APPeppolReportingHelperTest
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;

  @Test
  public void testGetEffectiveEndUserIDUnified ()
  {
    // No mapping, but the value is lower cased
    assertEquals ("iso6523-actorid-upis::0088:abc12345",
                  APPeppolReportingHelper.getEffectiveEndUserID (IF.createParticipantIdentifierWithDefaultScheme ("0088:ABC12345")));
  }

  @Test
  public void testGetEffectiveEndUserIDMapped ()
  {
    // Belgian VAT number is mapped to the Belgian enterprise number
    assertEquals ("iso6523-actorid-upis::0208:0123456789",
                  APPeppolReportingHelper.getEffectiveEndUserID (IF.createParticipantIdentifierWithDefaultScheme ("9925:BE0123456789")));
    // Same End User as above
    assertEquals ("iso6523-actorid-upis::0208:0123456789",
                  APPeppolReportingHelper.getEffectiveEndUserID (IF.createParticipantIdentifierWithDefaultScheme ("0208:0123456789")));

    // German VAT number is mapped to the German Electronic Business Address number
    assertEquals ("iso6523-actorid-upis::0246:de123456789",
                  APPeppolReportingHelper.getEffectiveEndUserID (IF.createParticipantIdentifierWithDefaultScheme ("9930:DE123456789")));

    // Finnish OVT identifier is mapped to the Finnish OVT code
    assertEquals ("iso6523-actorid-upis::0216:00371234567800001",
                  APPeppolReportingHelper.getEffectiveEndUserID (IF.createParticipantIdentifierWithDefaultScheme ("0037:1234567800001")));
  }
}
