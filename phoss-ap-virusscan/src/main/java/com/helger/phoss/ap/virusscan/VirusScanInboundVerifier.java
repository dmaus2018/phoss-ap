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

import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSStatusReasonCode;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.ap.api.exception.InboundVerifierDeferredException;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Inbound document verifier implementation performing ICAP (RFC 3507) virus scanning on received
 * document payloads.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class VirusScanInboundVerifier implements IInboundDocumentVerifierSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (VirusScanInboundVerifier.class);

  @Nullable
  public MlsOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                            @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                            @NonNull final IProcessIdentifier aProcessID)
  {
    if (!VirusScanConfig.isInboundEnabled ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("ICAP virus scanning is disabled for inbound documents");
      return null;
    }

    final IcapScanClient aClient = new IcapScanClient (VirusScanConfig.getHost (),
                                                      VirusScanConfig.getPort (),
                                                      VirusScanConfig.getService (),
                                                      VirusScanConfig.getTimeoutDuration ());

    VirusScanAttachmentExtractor.AttachmentScanResult aAttResult = null;

    // First scan embedded binary attachments
    try (final InputStream aIS = APBasicMetaManager.getDocPayloadMgr ().openDocumentStreamForRead (sDocumentPath))
    {
      if (aIS != null)
        aAttResult = VirusScanAttachmentExtractor.scanEmbeddedAttachments (aIS, aClient::scan);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to open payload stream for attachment virus scan on '" + sDocumentPath + "': " + ex.getMessage (), ex);
    }

    if (aAttResult != null && !aAttResult.getScanResult ().isPassed ())
    {
      return _handleResult (sDocumentPath, aAttResult.getScanResult ());
    }

    if (VirusScanConfig.isAttachmentsOnly ())
    {
      if (aAttResult != null && !aAttResult.hasAttachments ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Document '" + sDocumentPath + "' is pure XML with 0 attachments; bypassing ICAP scan (attachments-only mode)");
        return null;
      }
    }

    // Also scan full document stream directly
    IcapScanResult aResult = null;
    try (final InputStream aIS = APBasicMetaManager.getDocPayloadMgr ().openDocumentStreamForRead (sDocumentPath))
    {
      if (aIS != null)
        aResult = aClient.scan (aIS);
      else
        aResult = IcapScanResult.serviceUnavailable ("Failed to open document payload stream for '" + sDocumentPath + "'");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to open document payload stream for ICAP virus scan on '" + sDocumentPath + "': " + ex.getMessage (), ex);
      aResult = IcapScanResult.serviceUnavailable ("Failed to open payload stream: " + ex.getMessage ());
    }

    return _handleResult (sDocumentPath, aResult);
  }

  @Nullable
  private static MlsOutcome _handleResult (@NonNull final String sDocumentPath,
                                           @NonNull final IcapScanResult aResult)
  {
    if (aResult.isPassed ())
      return null;

    if (aResult.isRejection ())
    {
      final String sThreat = aResult.getThreatName ();
      LOGGER.warn ("Virus scan REJECTED document '" + sDocumentPath + "': Threat=" + sThreat);
      return MlsOutcome.rejection ("Virus detected in document payload: " + sThreat,
                                   MlsOutcomeIssue.ofNA (EPeppolMLSStatusReasonCode.BUSINESS_RULE_VIOLATION_FATAL,
                                                         "Virus detected: " + sThreat));
    }

    final String sFailMode = VirusScanConfig.getFailMode ();
    final String sErr = aResult.getErrorMessage ();

    if ("open".equalsIgnoreCase (sFailMode))
    {
      LOGGER.error ("ICAP virus scanner unavailable for '" + sDocumentPath + "' (fail-open mode), bypassing check: " + sErr);
      return null;
    }

    if ("deferred".equalsIgnoreCase (sFailMode))
    {
      LOGGER.warn ("ICAP virus scanner unavailable for '" + sDocumentPath + "' (deferred mode), throwing deferred exception: " + sErr);
      throw new InboundVerifierDeferredException ("VirusScanInboundVerifier", "ICAP virus scanner unavailable: " + sErr);
    }

    // Default: closed
    LOGGER.warn ("ICAP virus scanner unavailable for '" + sDocumentPath + "' (fail-closed mode), rejecting document: " + sErr);
    return MlsOutcome.serviceUnavailable ("Virus scan service unavailable: " + sErr,
                                         MlsOutcomeIssue.ofNA (EPeppolMLSStatusReasonCode.FAILURE_OF_DELIVERY,
                                                               "Virus scan service unavailable: " + sErr));
  }
}
