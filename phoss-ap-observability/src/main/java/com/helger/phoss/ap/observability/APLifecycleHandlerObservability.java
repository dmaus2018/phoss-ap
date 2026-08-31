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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.codelist.EMlsReceptionStatus;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.spi.IAPLifecycleEventSPI;
import com.helger.phoss.ap.observability.ObservabilityEcsJson.Builder;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;

/**
 * Implementation of {@link IAPLifecycleEventSPI} for positive milestone observability:
 * <ul>
 * <li>Attaches span events to the active OpenTelemetry span (Dynatrace)</li>
 * <li>Logs structured, single-line ECS JSON events via SLF4J (Elasticsearch)</li>
 * </ul>
 * Registered via {@code META-INF/services} and discovered automatically at startup.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class APLifecycleHandlerObservability implements IAPLifecycleEventSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger ("com.helger.phoss.ap.observability.events");

  private static void _addSpanEvent (@NonNull final String sEventName, @Nullable final Attributes aAttrs)
  {
    final Span aSpan = Span.current ();
    if (aSpan != null && aSpan.isRecording ())
    {
      if (aAttrs != null)
        aSpan.addEvent (sEventName, aAttrs);
      else
        aSpan.addEvent (sEventName);
    }
  }

  private static void _logSafe (@NonNull final Builder aBuilder)
  {
    try
    {
      LOGGER.info (aBuilder.buildJson ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error emitting observability log", ex);
    }
  }

  public void onInboundDocumentReceived (@NonNull final String sTransactionID,
                                         @NonNull final String sSenderID,
                                         @NonNull final String sReceiverID,
                                         @NonNull final String sDocTypeID,
                                         @NonNull final String sProcessID,
                                         @NonNull final String sSbdhInstanceID,
                                         final boolean bIsDuplicateAS4,
                                         final boolean bIsDuplicateSBDH)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SENDER_ID), sSenderID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_RECEIVER_ID), sReceiverID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_DOCTYPE_ID), sDocTypeID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_PROCESS_ID), sProcessID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _addSpanEvent ("inbound.document_received", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onInboundDocumentReceived")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("sender_id", sSenderID)
                                 .peppol ("receiver_id", sReceiverID)
                                 .peppol ("doc_type_id", sDocTypeID)
                                 .peppol ("process_id", sProcessID)
                                 .peppol ("is_duplicate_as4", bIsDuplicateAS4)
                                 .peppol ("is_duplicate_sbdh", bIsDuplicateSBDH));
  }

  public void onInboundVerificationAccepted (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _addSpanEvent ("inbound.verification_accepted", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onInboundVerificationAccepted")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID));
  }

  public void onOutboundVerificationAccepted (@NonNull final String sSbdhInstanceID)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _addSpanEvent ("outbound.verification_accepted", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onOutboundVerificationAccepted")
                                 .peppol ("direction", "OUTBOUND")
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID));
  }

  public void onInboundMLSCorrelated (@NonNull final String sMlsTransactionID,
                                      @NonNull final String sReferencedSbdhInstanceID,
                                      @NonNull final String sCorrelatedOutboundTransactionID,
                                      @NonNull final EPeppolMLSResponseCode eMlsResponseCode,
                                      @NonNull final EMlsReceptionStatus eMlsReceptionStatus,
                                      @Nullable final String sMlsDocumentID,
                                      @NonNull final OffsetDateTime aMlsReceivedDT,
                                      @Nullable final Duration aRoundTrip)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sMlsTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sReferencedSbdhInstanceID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE), eMlsResponseCode.getID (),
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_MLS_RECEPTION_STATUS), eMlsReceptionStatus.getID ());
    _addSpanEvent ("inbound.mls_correlated", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onInboundMLSCorrelated")
                                 .duration (aRoundTrip)
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("mls_transaction_id", sMlsTransactionID)
                                 .peppol ("referenced_sbdh_instance_id", sReferencedSbdhInstanceID)
                                 .peppol ("correlated_outbound_transaction_id", sCorrelatedOutboundTransactionID)
                                 .peppol ("mls_response_code", eMlsResponseCode.getID ())
                                 .peppol ("mls_reception_status", eMlsReceptionStatus.getID ())
                                 .peppol ("mls_document_id", sMlsDocumentID));
  }

  public void onInboundDocumentForwarded (@NonNull final String sTransactionID,
                                          @NonNull final String sSbdhInstanceID,
                                          @Nullable final Duration aForwardingDuration,
                                          final boolean bIsRetry)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID,
                                             AttributeKey.booleanKey (CPhossAPOtel.ATTR_IS_RETRY), Boolean.valueOf (bIsRetry));
    _addSpanEvent ("inbound.document_forwarded", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onInboundDocumentForwarded")
                                 .duration (aForwardingDuration)
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("is_retry", bIsRetry));
  }

  public void onOutboundDocumentAccepted (@NonNull final String sTransactionID,
                                          @NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @NonNull final String sSbdhInstanceID)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SENDER_ID), sSenderID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_RECEIVER_ID), sReceiverID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_DOCTYPE_ID), sDocTypeID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_PROCESS_ID), sProcessID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _addSpanEvent ("outbound.document_accepted", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onOutboundDocumentAccepted")
                                 .peppol ("direction", "OUTBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("sender_id", sSenderID)
                                 .peppol ("receiver_id", sReceiverID)
                                 .peppol ("doc_type_id", sDocTypeID)
                                 .peppol ("process_id", sProcessID));
  }

  public void onOutboundDocumentSent (@NonNull final String sTransactionID,
                                      @NonNull final String sSbdhInstanceID,
                                      @Nullable final Duration aSendingDuration,
                                      @Nonnegative final int nAttempts)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _addSpanEvent ("outbound.document_sent", aAttrs);

    _logSafe (ObservabilityEcsJson.createInfo ("onOutboundDocumentSent")
                                 .duration (aSendingDuration)
                                 .peppol ("direction", "OUTBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("attempts", nAttempts));
  }

  public void onPeppolReportingTSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    final String sPeriod = aYearMonth.toString ();
    _addSpanEvent ("reporting.tsr_success", Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH), sPeriod));

    _logSafe (ObservabilityEcsJson.createInfo ("onPeppolReportingTSRSuccess")
                                 .peppol ("reporting_period", sPeriod));
  }

  public void onPeppolReportingEUSRSuccess (@NonNull final YearMonth aYearMonth)
  {
    final String sPeriod = aYearMonth.toString ();
    _addSpanEvent ("reporting.eusr_success", Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH), sPeriod));

    _logSafe (ObservabilityEcsJson.createInfo ("onPeppolReportingEUSRSuccess")
                                 .peppol ("reporting_period", sPeriod));
  }

  public void onRetrySchedulerCycle (final boolean bIsOutbound,
                                     @Nonnegative final int nProcessed,
                                     @NonNull final Duration aCycleDuration)
  {
    _logSafe (ObservabilityEcsJson.createInfo ("onRetrySchedulerCycle")
                                 .duration (aCycleDuration)
                                 .peppol ("is_outbound", bIsOutbound)
                                 .peppol ("items_processed", nProcessed));
  }

  public void onArchivalSchedulerCycle (final boolean bIsOutbound,
                                        @Nonnegative final int nArchived,
                                        @NonNull final Duration aCycleDuration)
  {
    _logSafe (ObservabilityEcsJson.createInfo ("onArchivalSchedulerCycle")
                                 .duration (aCycleDuration)
                                 .peppol ("is_outbound", bIsOutbound)
                                 .peppol ("items_archived", nArchived));
  }

  public void onCleanupSchedulerCycle (final boolean bIsOutbound,
                                       @Nonnegative final int nDeleted,
                                       @NonNull final Duration aCycleDuration)
  {
    _logSafe (ObservabilityEcsJson.createInfo ("onCleanupSchedulerCycle")
                                 .duration (aCycleDuration)
                                 .peppol ("is_outbound", bIsOutbound)
                                 .peppol ("items_deleted", nDeleted));
  }
}
