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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.Telemetry;

/**
 * Stream-based client for performing ICAP REQMOD (RFC 3507) virus scanning against ICAP servers
 * such as ClamAV c-icap, Kaspersky, Symantec, or Sophos.
 *
 * @author Philip Helger
 */
@Immutable
public final class IcapScanClient
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IcapScanClient.class);
  private static final int CHUNK_SIZE = 8192;
  private static final String CRLF = "\r\n";

  private final String m_sHost;
  private final int m_nPort;
  private final String m_sService;
  private final Duration m_aTimeout;

  /**
   * Constructor.
   *
   * @param sHost
   *        ICAP server hostname. May not be <code>null</code> nor empty.
   * @param nPort
   *        ICAP server port.
   * @param sService
   *        ICAP service name (e.g. "avscan"). May not be <code>null</code> nor empty.
   * @param aTimeout
   *        Connection and socket read timeout. May not be <code>null</code>.
   */
  public IcapScanClient (@NonNull final String sHost,
                         final int nPort,
                         @NonNull final String sService,
                         @NonNull final Duration aTimeout)
  {
    m_sHost = ValueEnforcer.notEmpty (sHost, "Host");
    m_nPort = ValueEnforcer.isGE0 (nPort, "Port");
    m_sService = ValueEnforcer.notEmpty (sService, "Service");
    m_aTimeout = ValueEnforcer.notNull (aTimeout, "Timeout");
  }

  /**
   * Perform an ICAP REQMOD scan on the provided input stream.
   *
   * @param aPayloadIS
   *        The input stream to scan. May not be <code>null</code>.
   * @return An {@link IcapScanResult} representing the outcome. Never <code>null</code>.
   */
  @NonNull
  public IcapScanResult scan (@NonNull final InputStream aPayloadIS)
  {
    ValueEnforcer.notNull (aPayloadIS, "PayloadIS");

    try (final ITelemetrySpan aSpan = Telemetry.startSpan ("phoss.ap.virusscan.icap",
                                                           ETelemetrySpanKind.CLIENT)
                                               .setAttribute ("icap.host", m_sHost)
                                               .setAttribute ("icap.port", m_nPort)
                                               .setAttribute ("icap.service", m_sService))
    {
      final IcapScanResult aResult = _doScan (aPayloadIS);
      if (aResult != null)
      {
        aSpan.setAttribute ("icap.result.category", aResult.getCategory ().getID ());
        if (aResult.isRejection ())
          aSpan.setAttribute ("icap.threat_name", aResult.getThreatName ());
        if (aResult.isServiceUnavailable ())
          aSpan.setAttribute ("icap.error", aResult.getErrorMessage ());
      }
      return aResult;
    }
  }

  @NonNull
  private IcapScanResult _doScan (@NonNull final InputStream aPayloadIS)
  {
    final int nTimeoutMs = (int) Math.min (m_aTimeout.toMillis (), Integer.MAX_VALUE);

    try (final Socket aSocket = new Socket ())
    {
      aSocket.connect (new InetSocketAddress (m_sHost, m_nPort), nTimeoutMs);
      aSocket.setSoTimeout (nTimeoutMs);

      final OutputStream aOS = aSocket.getOutputStream ();
      final InputStream aIS = aSocket.getInputStream ();

      // RFC 3507 REQMOD: encapsulate a minimal HTTP request header + the payload as req-body.
      // Note: res-hdr is RESPMOD only and must NOT appear in a REQMOD request.
      final String sDummyHttpRequest = "POST /scan HTTP/1.1" + CRLF +
                                       "Host: " + m_sHost + CRLF +
                                       "User-Agent: phoss-ap-virusscan" + CRLF +
                                       CRLF;

      final byte [] aDummyReqBytes = sDummyHttpRequest.getBytes (StandardCharsets.UTF_8);

      final int nReqHdrOffset = 0;
      final int nReqBodyOffset = aDummyReqBytes.length;

      final StringBuilder aIcapHdr = new StringBuilder ();
      aIcapHdr.append ("REQMOD icap://").append (m_sHost).append (":").append (m_nPort).append ("/").append (m_sService).append (" ICAP/1.0").append (CRLF);
      aIcapHdr.append ("Host: ").append (m_sHost).append (":").append (m_nPort).append (CRLF);
      aIcapHdr.append ("User-Agent: phoss-ap-virusscan/0.11.1").append (CRLF);
      aIcapHdr.append ("Allow: 204").append (CRLF);
      aIcapHdr.append ("Encapsulated: req-hdr=").append (nReqHdrOffset)
              .append (", req-body=").append (nReqBodyOffset).append (CRLF);
      aIcapHdr.append (CRLF);

      aOS.write (aIcapHdr.toString ().getBytes (StandardCharsets.UTF_8));
      aOS.write (aDummyReqBytes);

      // Stream payload in HTTP chunked encoding
      final byte [] aBuffer = new byte [CHUNK_SIZE];
      int nRead;
      while ((nRead = aPayloadIS.read (aBuffer)) != -1)
      {
        if (nRead > 0)
        {
          final String sChunkHdr = Integer.toHexString (nRead) + CRLF;
          aOS.write (sChunkHdr.getBytes (StandardCharsets.UTF_8));
          aOS.write (aBuffer, 0, nRead);
          aOS.write (CRLF.getBytes (StandardCharsets.UTF_8));
        }
      }
      // End chunk
      aOS.write (("0" + CRLF + CRLF).getBytes (StandardCharsets.UTF_8));
      aOS.flush ();

      // Read ICAP response headers
      final String sResponseHeaders = _readHeaders (aIS);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("ICAP response headers:\n" + sResponseHeaders);

      if (sResponseHeaders.startsWith ("ICAP/1.0 204") || sResponseHeaders.startsWith ("ICAP/1.1 204"))
        return IcapScanResult.passed ();

      final String sThreatName = _extractThreatName (sResponseHeaders);
      if (StringHelper.hasText (sThreatName))
        return IcapScanResult.rejection (sThreatName);

      if (sResponseHeaders.startsWith ("ICAP/1.0 200") || sResponseHeaders.startsWith ("ICAP/1.1 200"))
      {
        // Check encapsulated body headers for threat indication
        final String sEncapsulatedBody = _readHeaders (aIS);
        final String sBodyThreat = _extractThreatName (sEncapsulatedBody);
        if (StringHelper.hasText (sBodyThreat))
          return IcapScanResult.rejection (sBodyThreat);

        return IcapScanResult.rejection ("Threat detected by ICAP scanner");
      }

      return IcapScanResult.serviceUnavailable ("Unexpected ICAP response: " + _getFirstLine (sResponseHeaders));
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("ICAP scanning failed for " + m_sHost + ":" + m_nPort + "/" + m_sService + ": " + ex.getMessage ());
      return IcapScanResult.serviceUnavailable (ex.getClass ().getSimpleName () + ": " + ex.getMessage ());
    }
  }

  @NonNull
  private static String _readHeaders (@NonNull final InputStream aIS) throws Exception
  {
    final StringBuilder aSb = new StringBuilder ();
    int nPrevious = -1;
    int nCurr;
    while ((nCurr = aIS.read ()) != -1)
    {
      aSb.append ((char) nCurr);
      if (nPrevious == '\r' && nCurr == '\n')
      {
        final int nLen = aSb.length ();
        if (nLen >= 4 && aSb.substring (nLen - 4).equals ("\r\n\r\n"))
          break;
      }
      nPrevious = nCurr;
    }
    return aSb.toString ();
  }

  @Nullable
  private static String _extractThreatName (@NonNull final String sHeaders)
  {
    for (final String sLine : sHeaders.split ("\r?\n"))
    {
      final String sLower = sLine.toLowerCase ();
      if (sLower.startsWith ("x-infection-found:") ||
          sLower.startsWith ("x-virus-id:") ||
          sLower.startsWith ("x-violated-policy:") ||
          sLower.startsWith ("x-threat-name:"))
      {
        final int nColon = sLine.indexOf (':');
        if (nColon != -1)
        {
          String sValue = sLine.substring (nColon + 1).trim ();
          final int nThreatIndex = sValue.toLowerCase ().indexOf ("threat=");
          if (nThreatIndex != -1)
          {
            sValue = sValue.substring (nThreatIndex + 7);
            final int nSemi = sValue.indexOf (';');
            if (nSemi != -1)
              sValue = sValue.substring (0, nSemi);
          }
          return sValue.trim ();
        }
      }
    }
    return null;
  }

  @NonNull
  private static String _getFirstLine (@NonNull final String sText)
  {
    final int nIdx = sText.indexOf ("\r\n");
    return nIdx != -1 ? sText.substring (0, nIdx) : sText;
  }
}
