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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.state.ESuccess;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.ap.api.codelist.EVerificationFailMode;
import com.helger.phoss.ap.api.spi.IOutboundDocumentVerifierSPI;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Outbound document verifier implementation performing ICAP (RFC 3507) virus scanning on outbound
 * documents submitted for AS4 sending.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class VirusScanOutboundVerifier implements IOutboundDocumentVerifierSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (VirusScanOutboundVerifier.class);

  @NonNull
  public ESuccess verifyOutboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                          @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                          @NonNull final IProcessIdentifier aProcessID)
  {
    if (!VirusScanConfig.isOutboundEnabled ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("ICAP virus scanning is disabled for outbound documents");
      return ESuccess.SUCCESS;
    }

    final IcapScanClient aClient = new IcapScanClient (VirusScanConfig.getHost (),
                                                      VirusScanConfig.getPort (),
                                                      VirusScanConfig.getService (),
                                                      VirusScanConfig.getTimeoutDuration ());

    VirusScanAttachmentExtractor.AttachmentScanResult aAttResult = null;

    // Scan embedded binary attachments
    try (final InputStream aIS = APBasicMetaManager.getDocPayloadMgr ().openDocumentStreamForRead (sDocumentPath))
    {
      if (aIS != null)
        aAttResult = VirusScanAttachmentExtractor.scanEmbeddedAttachments (aIS, aClient::scan);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to open payload stream for outbound attachment virus scan on '" + sDocumentPath + "': " + ex.getMessage (), ex);
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
          LOGGER.debug ("Outbound document '" + sDocumentPath + "' is pure XML with 0 attachments; bypassing ICAP scan (attachments-only mode)");
        return ESuccess.SUCCESS;
      }
    }

    // Scan full document payload
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
      LOGGER.error ("Failed to open document payload stream for outbound ICAP virus scan on '" + sDocumentPath + "': " + ex.getMessage (), ex);
      aResult = IcapScanResult.serviceUnavailable ("Failed to open payload stream: " + ex.getMessage ());
    }

    return _handleResult (sDocumentPath, aResult);
  }

  @NonNull
  private static ESuccess _handleResult (@NonNull final String sDocumentPath,
                                         @NonNull final IcapScanResult aResult)
  {
    if (aResult.isPassed ())
      return ESuccess.SUCCESS;

    if (aResult.isRejection ())
    {
      LOGGER.warn ("Outbound document '" + sDocumentPath + "' REJECTED by virus scan: Threat=" + aResult.getThreatName ());
      return ESuccess.FAILURE;
    }

    final EVerificationFailMode eFailMode = VirusScanConfig.getFailMode ();
    final String sErr = aResult.getErrorMessage ();

    if (eFailMode == EVerificationFailMode.OPEN)
    {
      LOGGER.error ("Outbound ICAP virus scanner unavailable for '" + sDocumentPath + "' (fail-open mode), bypassing check: " + sErr);
      return ESuccess.SUCCESS;
    }

    LOGGER.warn ("Outbound ICAP virus scanner unavailable for '" + sDocumentPath + "' (fail-closed mode), rejecting outbound submission: " + sErr);
    return ESuccess.FAILURE;
  }
}
