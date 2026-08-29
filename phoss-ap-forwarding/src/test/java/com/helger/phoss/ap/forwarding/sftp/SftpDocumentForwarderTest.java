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
package com.helger.phoss.ap.forwarding.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.config.ConfigFactory;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.peppol.sbdh.EPeppolMLSType;
import com.helger.phoss.ap.api.codelist.EInboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.config.APConfigurationProperties;
import com.helger.phoss.ap.api.model.IInboundTransaction;

/**
 * Test class for {@link SftpDocumentForwarder}.
 *
 * @author Philip Helger
 */
public final class SftpDocumentForwarderTest
{
  private static final class MockInboundTransaction implements IInboundTransaction
  {
    private final OffsetDateTime m_aReceivedDT;
    private final String m_sIncomingID;
    private final String m_sSbdhInstanceID;
    private final String m_sReceiverID;
    private final String m_sSenderID;
    private final String m_sDocTypeID;
    private final String m_sProcessID;

    public MockInboundTransaction (final OffsetDateTime aReceivedDT,
                                   final String sIncomingID,
                                   final String sSbdhInstanceID,
                                   final String sReceiverID,
                                   final String sSenderID,
                                   final String sDocTypeID,
                                   final String sProcessID)
    {
      m_aReceivedDT = aReceivedDT;
      m_sIncomingID = sIncomingID;
      m_sSbdhInstanceID = sSbdhInstanceID;
      m_sReceiverID = sReceiverID;
      m_sSenderID = sSenderID;
      m_sDocTypeID = sDocTypeID;
      m_sProcessID = sProcessID;
    }

    @NonNull public String getID () { return "tx-123"; }
    @NonNull public String getIncomingID () { return m_sIncomingID; }
    @NonNull public String getC2SeatID () { return "POP000001"; }
    @NonNull public String getC3SeatID () { return "PAU000345"; }
    @NonNull public String getSigningCertCN () { return "Test"; }
    @NonNull public String getSenderID () { return m_sSenderID; }
    @NonNull public String getReceiverID () { return m_sReceiverID; }
    @NonNull public String getDocTypeID () { return m_sDocTypeID; }
    @NonNull public String getProcessID () { return m_sProcessID; }
    @NonNull public String getDocumentPath () { return "/tmp/doc.xml"; }
    public long getDocumentSize () { return 1024; }
    @NonNull public String getDocumentHash () { return "hash"; }
    @NonNull public String getAS4MessageID () { return "as4-1"; }
    @NonNull public OffsetDateTime getAS4Timestamp () { return m_aReceivedDT; }
    @NonNull public String getSbdhInstanceID () { return m_sSbdhInstanceID; }
    @Nullable public String getC1CountryCode () { return "AU"; }
    @Nullable public String getC4CountryCode () { return "AU"; }
    public boolean isDuplicateAS4 () { return false; }
    public boolean isDuplicateSBDH () { return false; }
    @NonNull public EInboundStatus getStatus () { return EInboundStatus.RECEIVED; }
    public int getAttemptCount () { return 0; }
    @NonNull public OffsetDateTime getReceivedDT () { return m_aReceivedDT; }
    @Nullable public OffsetDateTime getCompletedDT () { return null; }
    @NonNull public EReportingStatus getReportingStatus () { return EReportingStatus.NOT_REPORTED; }
    @Nullable public OffsetDateTime getNextRetryDT () { return null; }
    @Nullable public String getErrorDetails () { return null; }
    @Nullable public String getMlsTo () { return null; }
    @NonNull public EPeppolMLSType getMlsType () { return EPeppolMLSType.NONE; }
    @Nullable public EPeppolMLSResponseCode getMlsResponseCode () { return null; }
    @Nullable public String getMlsOutboundTransactionID () { return null; }
    @Nullable public EVerificationResult getVerificationResult () { return null; }
    @Nullable public String getVerificationDetails () { return null; }
  }

  @Test
  public void testInitFromConfigDefault () throws Exception
  {
    System.setProperty ("forwarding.mode", "sftp");
    System.setProperty ("forwarding.sftp.host", "localhost");
    System.setProperty ("forwarding.sftp.port", "2222");
    System.setProperty ("forwarding.sftp.user", "dm");
    System.setProperty ("forwarding.sftp.password", "m@nage");
    System.setProperty ("forwarding.sftp.uploaddir", "/inbound/");

    try
    {
      final var aConfigWithFallback = new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
      final SftpDocumentForwarder aForwarder = new SftpDocumentForwarder ();
      final var eSuccess = aForwarder.initFromConfiguration (aConfigWithFallback, "forwarding.");
      assertTrue (eSuccess.isSuccess ());
      assertEquals (APConfigurationProperties.FORWARDING_SFTP_FILENAME_PATTERN_DEFAULT, aForwarder.getFilenamePattern ());
    }
    finally
    {
      System.clearProperty ("forwarding.mode");
      System.clearProperty ("forwarding.sftp.host");
      System.clearProperty ("forwarding.sftp.port");
      System.clearProperty ("forwarding.sftp.user");
      System.clearProperty ("forwarding.sftp.password");
      System.clearProperty ("forwarding.sftp.uploaddir");
    }
  }

  @Test
  public void testInitFromConfigCustomPattern () throws Exception
  {
    System.setProperty ("forwarding.mode", "sftp");
    System.setProperty ("forwarding.sftp.host", "localhost");
    System.setProperty ("forwarding.sftp.port", "2222");
    System.setProperty ("forwarding.sftp.user", "dm");
    System.setProperty ("forwarding.sftp.password", "m@nage");
    System.setProperty ("forwarding.sftp.uploaddir", "/inbound/");
    System.setProperty ("forwarding.sftp.filename-pattern", "{datetime}_{receiver-value}_{incoming-id}");

    try
    {
      final var aConfigWithFallback = new ConfigWithFallback (ConfigFactory.createDefaultValueProvider ());
      final SftpDocumentForwarder aForwarder = new SftpDocumentForwarder ();
      final var eSuccess = aForwarder.initFromConfiguration (aConfigWithFallback, "forwarding.");
      assertTrue (eSuccess.isSuccess ());
      assertEquals ("{datetime}_{receiver-value}_{incoming-id}", aForwarder.getFilenamePattern ());
    }
    finally
    {
      System.clearProperty ("forwarding.mode");
      System.clearProperty ("forwarding.sftp.host");
      System.clearProperty ("forwarding.sftp.port");
      System.clearProperty ("forwarding.sftp.user");
      System.clearProperty ("forwarding.sftp.password");
      System.clearProperty ("forwarding.sftp.uploaddir");
      System.clearProperty ("forwarding.sftp.filename-pattern");
    }
  }

  @Test
  public void testGetResolvedBaseNameDefault ()
  {
    final IInboundTransaction aTx = new MockInboundTransaction (
        OffsetDateTime.of (2026, 8, 29, 19, 10, 34, 0, ZoneOffset.UTC),
        "d6c8ec5f-0908-4f6e-9bb5-f889ce628e97",
        "a7f61007-b08b-49b5-b361-690df20c0c16",
        "0151:35747532810",
        "0151:90794605008",
        "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
        "urn:peppol:bis:billing");

    final String sResult = SftpDocumentForwarder.getResolvedBaseName (APConfigurationProperties.FORWARDING_SFTP_FILENAME_PATTERN_DEFAULT, aTx);
    assertEquals ("20260829191034_d6c8ec5f-0908-4f6e-9bb5-f889ce628e97", sResult);
  }

  @Test
  public void testGetResolvedBaseNameCustomTokens ()
  {
    final IInboundTransaction aTx = new MockInboundTransaction (
        OffsetDateTime.of (2026, 8, 29, 19, 10, 34, 0, ZoneOffset.UTC),
        "d6c8ec5f-0908-4f6e-9bb5-f889ce628e97",
        "a7f61007-b08b-49b5-b361-690df20c0c16",
        "0151:35747532810",
        "0151:90794605008",
        "Invoice-2",
        "billing");

    final String sPattern = "{datetime}_{receiver-value}_{incoming-id}";
    final String sResult = SftpDocumentForwarder.getResolvedBaseName (sPattern, aTx);
    assertEquals ("20260829191034_35747532810_d6c8ec5f-0908-4f6e-9bb5-f889ce628e97", sResult);

    final String sPattern2 = "{receiver-id}_{sbdh-instance-id}";
    final String sResult2 = SftpDocumentForwarder.getResolvedBaseName (sPattern2, aTx);
    assertEquals ("0151_35747532810_a7f61007-b08b-49b5-b361-690df20c0c16", sResult2);

    final String sPattern3 = "${datetime}_${receiver-value}_${incoming-id}";
    final String sResult3 = SftpDocumentForwarder.getResolvedBaseName (sPattern3, aTx);
    assertEquals ("20260829191034_35747532810_d6c8ec5f-0908-4f6e-9bb5-f889ce628e97", sResult3);
  }
}
