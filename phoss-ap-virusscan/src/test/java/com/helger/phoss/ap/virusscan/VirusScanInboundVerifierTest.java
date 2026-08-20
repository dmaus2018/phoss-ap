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
package com.helger.phoss.ap.virusscan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.ap.api.codelist.EVerificationFailMode;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for {@link VirusScanInboundVerifier} and {@link VirusScanConfig}.
 *
 * @author Philip Helger
 */
public final class VirusScanInboundVerifierTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testDisabledByDefault () throws Exception
  {
    assertFalse (VirusScanConfig.isEnabled ());
    assertEquals ("localhost", VirusScanConfig.getHost ());
    assertEquals (1344, VirusScanConfig.getPort ());
    assertEquals ("avscan", VirusScanConfig.getService ());
    assertEquals (EVerificationFailMode.DEFAULT, VirusScanConfig.getFailMode ());

    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IDocumentTypeIdentifier aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme ("dummy-doctype");
    final IProcessIdentifier aProcessID = aIF.createProcessIdentifierWithDefaultScheme ("dummy-process");

    final VirusScanInboundVerifier aVerifier = new VirusScanInboundVerifier ();
    assertNull (aVerifier.verifyInboundDocument ("dummy-path", aDocTypeID, aProcessID));
  }

  @Test
  public void testResultDTO ()
  {
    final IcapScanResult aPassed = IcapScanResult.passed ();
    assertTrue (aPassed.isPassed ());
    assertFalse (aPassed.isRejection ());
    assertFalse (aPassed.isServiceUnavailable ());
    assertEquals (EVerificationOutcomeCategory.PASSED, aPassed.getCategory ());

    final IcapScanResult aRejection = IcapScanResult.rejection ("Eicar-Test-Signature");
    assertFalse (aRejection.isPassed ());
    assertTrue (aRejection.isRejection ());
    assertEquals ("Eicar-Test-Signature", aRejection.getThreatName ());
    assertEquals (EVerificationOutcomeCategory.REJECTION, aRejection.getCategory ());

    final IcapScanResult aUnavailable = IcapScanResult.serviceUnavailable ("Connection refused");
    assertFalse (aUnavailable.isPassed ());
    assertTrue (aUnavailable.isServiceUnavailable ());
    assertEquals ("Connection refused", aUnavailable.getErrorMessage ());
    assertEquals (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, aUnavailable.getCategory ());
  }

  @Test
  public void testAttachmentExtractor ()
  {
    final String sRawData = "Hello Peppol ICAP Attachment!";
    final String sBase64 = Base64.getEncoder ().encodeToString (sRawData.getBytes (StandardCharsets.UTF_8));
    final String sXml = "<Invoice xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n" +
                        "  <cbc:EmbeddedDocumentBinaryObject encodingCode=\"Base64\">" + sBase64 + "</cbc:EmbeddedDocumentBinaryObject>\n" +
                        "</Invoice>";

    final ByteArrayInputStream aXmlIS = new ByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8));
    final VirusScanAttachmentExtractor.AttachmentScanResult aAttResult = VirusScanAttachmentExtractor.scanEmbeddedAttachments (aXmlIS, aStream -> {
      try
      {
        final String sDecoded = new String (aStream.readAllBytes (), StandardCharsets.UTF_8);
        assertEquals (sRawData, sDecoded);
        return IcapScanResult.passed ();
      }
      catch (final Exception ex)
      {
        return IcapScanResult.serviceUnavailable (ex.getMessage ());
      }
    });

    assertEquals (1, aAttResult.getAttachmentCount ());
    assertTrue (aAttResult.getScanResult ().isPassed ());
  }
}
