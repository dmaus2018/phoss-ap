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
package com.helger.phoss.ap.observability;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.Test;

import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.codelist.EOutboundStatus;
import com.helger.phoss.ap.api.codelist.EReportingStatus;
import com.helger.phoss.ap.api.codelist.ESourceType;
import com.helger.phoss.ap.api.codelist.ETransactionType;
import com.helger.phoss.ap.api.codelist.EVerificationResult;
import com.helger.phoss.ap.api.model.IOutboundTransaction;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.model.VerificationIssue;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.api.model.VerifierResult;

/**
 * Unit tests for {@link APNotificationHandlerObservability} and
 * {@link APLifecycleHandlerObservability}.
 *
 * @author Philip Helger
 */
public final class ObservabilityHandlersTest
{
  private static IOutboundTransaction _createOutboundTx ()
  {
    final OffsetDateTime aNow = OffsetDateTime.now ();
    return new IOutboundTransaction ()
    {
      public String getID () { return "tx-orig-1"; }
      public ETransactionType getTransactionType () { return ETransactionType.BUSINESS_DOCUMENT; }
      public String getSenderID () { return "iso6523-actorid-upis::0088:sender"; }
      public String getReceiverID () { return "iso6523-actorid-upis::0088:receiver"; }
      public String getDocTypeID () { return "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice"; }
      public String getProcessID () { return "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"; }
      public String getSbdhInstanceID () { return "sbdh-ref-1"; }
      public ESourceType getSourceType () { return ESourceType.PAYLOAD_ONLY; }
      public String getDocumentPath () { return "/tmp/doc.xml"; }
      public long getDocumentSize () { return 1024L; }
      public String getDocumentHash () { return "sha256-hash"; }
      public String getC1CountryCode () { return "AU"; }
      public EOutboundStatus getStatus () { return EOutboundStatus.SENDING; }
      public int getAttemptCount () { return 1; }
      public OffsetDateTime getCreatedDT () { return aNow; }
      public OffsetDateTime getCompletedDT () { return null; }
      public EReportingStatus getReportingStatus () { return EReportingStatus.PENDING; }
      public OffsetDateTime getNextRetryDT () { return null; }
      public String getErrorDetails () { return null; }
      public String getMlsTo () { return null; }
      public EMlsReceptionStatus getMlsStatus () { return EMlsReceptionStatus.PENDING; }
      public OffsetDateTime getMlsReceivedDT () { return null; }
      public String getMlsID () { return null; }
      public String getMlsInboundTransactionID () { return null; }
      public String getSbdhStandard () { return null; }
      public String getSbdhTypeVersion () { return null; }
      public String getSbdhType () { return null; }
      public String getPayloadMimeType () { return null; }
      public String getCustom1 () { return "cust-1"; }
      public String getCustom2 () { return "cust-2"; }
      public String getCustom3 () { return null; }
    };
  }

  @Test
  public void testEcsJsonBuilderSingleLine ()
  {
    final String sJson = ObservabilityEcsJson.createError ("onUnexpectedException")
                                            .peppol ("transaction_id", "tx-123")
                                            .peppol ("is_retry", true)
                                            .outboundTransaction (_createOutboundTx ())
                                            .error ("Something failed\nwith a newline")
                                            .exception (new IllegalArgumentException ("Bad arg\nmultiline error"))
                                            .buildJson ();

    assertNotNull (sJson);
    assertFalse ("JSON must be single-line (no raw newlines)", sJson.contains ("\n") || sJson.contains ("\r"));
    assertTrue (sJson.contains ("\"ecs.version\":\"8.11.0\""));
    assertTrue (sJson.contains ("\"service.name\":\"phoss-ap\""));
    assertTrue (sJson.contains ("\"action\":\"onUnexpectedException\""));
    assertTrue (sJson.contains ("\"outcome\":\"failure\""));
    assertTrue (sJson.contains ("\"custom1\":\"cust-1\""));
  }

  @Test
  public void testNotificationHandlerAllMethods ()
  {
    final APNotificationHandlerObservability aHandler = new APNotificationHandlerObservability ();
    final IOutboundTransaction aOutTx = _createOutboundTx ();

    aHandler.onInboundReceiverNotServiced ("sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1");
    final MlsOutcome aOutcome = MlsOutcome.rejection ("Schematron rule failed",
                                                     MlsOutcomeIssue.businessRuleViolation ("XPath", "Schematron rule failed"));
    aHandler.onInboundVerificationRejection ("tx-1", "sbdh-1", "Schematron rule failed", aOutcome);
    aHandler.onInboundVerificationRejection ("tx-1", "sbdh-1", null, aOutcome);

    aHandler.onInboundVerificationDeferred ("tx-1", "sbdh-1", "phorm", OffsetDateTime.now (), "timeout");
    aHandler.onInboundVerificationDeferred ("tx-1", "sbdh-1", "phorm", OffsetDateTime.now (), null);

    final VerificationIssue aIssue = VerificationIssue.businessRuleViolation ("ERR-01", "/Invoice/cbc:ID", "Rule failed");
    final VerifierResult aVR = new VerifierResult ("phorm", "phorm", VerificationOutcome.rejected ("Failed rule", List.of (aIssue)));
    aHandler.onOutboundVerificationRejection ("sbdh-1", aVR);

    aHandler.onInboundDuplicateRejected ("sender-1", "receiver-1", "doc-1", "proc-1", "POP000306", "as4-1", "sbdh-1", true, false, "Duplicate AS4");
    aHandler.onInboundDuplicateRejected ("sender-1", "receiver-1", "doc-1", "proc-1", null, null, "sbdh-1", false, true, "Duplicate SBDH");

    aHandler.onInboundMLSCorrelationError ("tx-1", "sbdh-ref-1", EPeppolMLSResponseCode.REJECTION);
    aHandler.onSpecialMlsToNotReachable ("tx-out-1", "sbdh-ref-1", "target-spid", "fallback-spid");

    aHandler.onInboundForwardingError ("tx-1", false);
    aHandler.onInboundForwardingError ("tx-1", true);

    aHandler.onInboundPermanentForwardingFailure ("tx-1", "sbdh-1", "Connection refused");
    aHandler.onInboundPermanentForwardingFailure ("tx-1", "sbdh-1", null);

    aHandler.onOutboundPermanentSendingFailure (aOutTx, "HTTP 503");
    aHandler.onOutboundPermanentSendingFailure (aOutTx, null);

    aHandler.onPeppolReportingTSRFailure (YearMonth.now ());
    aHandler.onPeppolReportingEUSRFailure (YearMonth.now ());

    aHandler.onUnexpectedException ("Context", "Message", new RuntimeException ("Test ex"));
  }

  @Test
  public void testLifecycleHandlerAllMethods ()
  {
    final APLifecycleHandlerObservability aHandler = new APLifecycleHandlerObservability ();
    final IOutboundTransaction aOutTx = _createOutboundTx ();

    aHandler.onInboundDocumentReceived ("tx-1", "sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1", false, false);
    aHandler.onInboundVerificationAccepted ("tx-1", "sbdh-1");
    aHandler.onOutboundVerificationAccepted ("sbdh-1");

    aHandler.onInboundMLSCorrelated ("tx-mls-1", aOutTx, EPeppolMLSResponseCode.ACCEPTANCE, EMlsReceptionStatus.RECEIVED_AP, "doc-id-1", OffsetDateTime.now (), Duration.ofSeconds (5));
    aHandler.onInboundMLSCorrelated ("tx-mls-1", aOutTx, EPeppolMLSResponseCode.ACCEPTANCE, EMlsReceptionStatus.RECEIVED_AP, null, OffsetDateTime.now (), null);

    aHandler.onInboundDocumentForwarded ("tx-1", "sbdh-1", Duration.ofMillis (1200), false, EVerificationResult.PASSED);
    aHandler.onInboundDocumentForwarded ("tx-1", "sbdh-1", null, true, null);

    aHandler.onOutboundDocumentAccepted ("tx-1", "sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1");

    aHandler.onOutboundDocumentSent (aOutTx, Duration.ofMillis (850), 1);
    aHandler.onOutboundDocumentSent (aOutTx, null, 3);

    aHandler.onPeppolReportingTSRSuccess (YearMonth.now ());
    aHandler.onPeppolReportingEUSRSuccess (YearMonth.now ());

    aHandler.onRetrySchedulerCycle (true, 5, Duration.ofSeconds (2));
    aHandler.onArchivalSchedulerCycle (false, 10, Duration.ofSeconds (3));
    aHandler.onCleanupSchedulerCycle (true, 0, Duration.ofMillis (500));
  }
}
