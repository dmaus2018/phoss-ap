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
  @Test
  public void testEcsJsonBuilderSingleLine ()
  {
    final String sJson = ObservabilityEcsJson.createError ("onUnexpectedException")
                                            .peppol ("transaction_id", "tx-123")
                                            .peppol ("is_retry", true)
                                            .error ("Something failed\nwith a newline")
                                            .exception (new IllegalArgumentException ("Bad arg\nmultiline error"))
                                            .buildJson ();

    assertNotNull (sJson);
    assertFalse ("JSON must be single-line (no raw newlines)", sJson.contains ("\n") || sJson.contains ("\r"));
    assertTrue (sJson.contains ("\"ecs.version\":\"8.11.0\""));
    assertTrue (sJson.contains ("\"service.name\":\"phoss-ap\""));
    assertTrue (sJson.contains ("\"action\":\"onUnexpectedException\""));
    assertTrue (sJson.contains ("\"outcome\":\"failure\""));
  }

  @Test
  public void testNotificationHandlerAllMethods ()
  {
    final APNotificationHandlerObservability aHandler = new APNotificationHandlerObservability ();

    aHandler.onInboundReceiverNotServiced ("sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1");
    aHandler.onInboundVerificationRejection ("tx-1", "sbdh-1", "Schematron rule failed");
    aHandler.onInboundVerificationRejection ("tx-1", "sbdh-1", null);

    aHandler.onInboundVerificationDeferred ("tx-1", "sbdh-1", "phorm", OffsetDateTime.now (), "timeout");
    aHandler.onInboundVerificationDeferred ("tx-1", "sbdh-1", "phorm", OffsetDateTime.now (), null);

    final VerificationIssue aIssue = VerificationIssue.businessRuleViolation ("ERR-01", "/Invoice/cbc:ID", "Rule failed");
    final VerifierResult aVR = new VerifierResult (VerificationOutcome.rejected ("Failed rule", List.of (aIssue)), "phorm");
    aHandler.onOutboundVerificationRejection ("sbdh-1", aVR);

    aHandler.onInboundDuplicateRejected ("sender-1", "receiver-1", "doc-1", "proc-1", "POP000306", "as4-1", "sbdh-1", true, false, "Duplicate AS4");
    aHandler.onInboundDuplicateRejected ("sender-1", "receiver-1", "doc-1", "proc-1", null, null, "sbdh-1", false, true, "Duplicate SBDH");

    aHandler.onInboundMLSCorrelationError ("tx-1", "sbdh-ref-1", EPeppolMLSResponseCode.REJECTION);
    aHandler.onSpecialMlsToNotReachable ("tx-out-1", "sbdh-ref-1", "target-spid", "fallback-spid");

    aHandler.onInboundForwardingError ("tx-1", false);
    aHandler.onInboundForwardingError ("tx-1", true);

    aHandler.onInboundPermanentForwardingFailure ("tx-1", "sbdh-1", "Connection refused");
    aHandler.onInboundPermanentForwardingFailure ("tx-1", "sbdh-1", null);

    aHandler.onOutboundPermanentSendingFailure ("tx-1", "sbdh-1", "HTTP 503");
    aHandler.onOutboundPermanentSendingFailure ("tx-1", "sbdh-1", null);

    aHandler.onPeppolReportingTSRFailure (YearMonth.now ());
    aHandler.onPeppolReportingEUSRFailure (YearMonth.now ());

    aHandler.onUnexpectedException ("Context", "Message", new RuntimeException ("Test ex"));
  }

  @Test
  public void testLifecycleHandlerAllMethods ()
  {
    final APLifecycleHandlerObservability aHandler = new APLifecycleHandlerObservability ();

    aHandler.onInboundDocumentReceived ("tx-1", "sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1", false, false);
    aHandler.onInboundVerificationAccepted ("tx-1", "sbdh-1");
    aHandler.onOutboundVerificationAccepted ("sbdh-1");

    aHandler.onInboundMLSCorrelated ("tx-mls-1", "sbdh-ref-1", "tx-orig-1", EPeppolMLSResponseCode.ACCEPTANCE, EMlsReceptionStatus.RECEIVED_AP, "doc-id-1", OffsetDateTime.now (), Duration.ofSeconds (5));
    aHandler.onInboundMLSCorrelated ("tx-mls-1", "sbdh-ref-1", "tx-orig-1", EPeppolMLSResponseCode.ACCEPTANCE, EMlsReceptionStatus.RECEIVED_AP, null, OffsetDateTime.now (), null);

    aHandler.onInboundDocumentForwarded ("tx-1", "sbdh-1", Duration.ofMillis (1200), false);
    aHandler.onInboundDocumentForwarded ("tx-1", "sbdh-1", null, true);

    aHandler.onOutboundDocumentAccepted ("tx-1", "sender-1", "receiver-1", "doc-1", "proc-1", "sbdh-1");

    aHandler.onOutboundDocumentSent ("tx-1", "sbdh-1", Duration.ofMillis (850), 1);
    aHandler.onOutboundDocumentSent ("tx-1", "sbdh-1", null, 3);

    aHandler.onPeppolReportingTSRSuccess (YearMonth.now ());
    aHandler.onPeppolReportingEUSRSuccess (YearMonth.now ());

    aHandler.onRetrySchedulerCycle (true, 5, Duration.ofSeconds (2));
    aHandler.onArchivalSchedulerCycle (false, 10, Duration.ofSeconds (3));
    aHandler.onCleanupSchedulerCycle (true, 0, Duration.ofMillis (500));
  }
}
