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

import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.peppol.mls.EPeppolMLSResponseCode;
import com.helger.phoss.ap.api.model.VerifierResult;
import com.helger.phoss.ap.api.otel.CPhossAPOtel;
import com.helger.phoss.ap.api.spi.IAPNotificationHandlerSPI;
import com.helger.phoss.ap.observability.ObservabilityEcsJson.Builder;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Implementation of {@link IAPNotificationHandlerSPI} for unified enterprise observability:
 * <ul>
 * <li>Attaches error status and span events to the active OpenTelemetry span (Dynatrace)</li>
 * <li>Logs structured, single-line ECS JSON events via SLF4J (Elasticsearch)</li>
 * </ul>
 * Registered via {@code META-INF/services} and discovered automatically at startup.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class APNotificationHandlerObservability implements IAPNotificationHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger ("com.helger.phoss.ap.observability.events");

  private static void _recordSpanError (@NonNull final String sErrorMsg, @Nullable final Attributes aAttrs)
  {
    final Span aSpan = Span.current ();
    if (aSpan != null && aSpan.isRecording ())
    {
      aSpan.setStatus (StatusCode.ERROR, sErrorMsg);
      if (aAttrs != null)
        aSpan.addEvent ("error", aAttrs);
    }
  }

  private static void _logSafe (@NonNull final Builder aBuilder)
  {
    try
    {
      LOGGER.error (aBuilder.buildJson ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Internal error emitting observability log", ex);
    }
  }

  private static void _logWarnSafe (@NonNull final Builder aBuilder)
  {
    try
    {
      LOGGER.warn (aBuilder.buildJson ());
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("Internal error emitting observability log", ex);
    }
  }

  public void onInboundReceiverNotServiced (@NonNull final String sSenderID,
                                            @NonNull final String sReceiverID,
                                            @NonNull final String sDocTypeID,
                                            @NonNull final String sProcessID,
                                            @NonNull final String sSbdhInstanceID)
  {
    final String sMsg = "Inbound receiver not serviced: " + sReceiverID;
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_SENDER_ID), sSenderID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_RECEIVER_ID), sReceiverID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_DOCTYPE_ID), sDocTypeID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_PROCESS_ID), sProcessID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onInboundReceiverNotServiced")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("sender_id", sSenderID)
                                 .peppol ("receiver_id", sReceiverID)
                                 .peppol ("doc_type_id", sDocTypeID)
                                 .peppol ("process_id", sProcessID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .error (sMsg));
  }

  public void onInboundVerificationRejection (@NonNull final String sTransactionID,
                                              @NonNull final String sSbdhInstanceID,
                                              @Nullable final String sErrorDetails)
  {
    final String sMsg = "Inbound verification rejected" + (sErrorDetails != null ? ": " + sErrorDetails : "");
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onInboundVerificationRejection")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .error (sErrorDetails != null ? sErrorDetails : "Inbound verification rejected"));
  }

  public void onInboundVerificationDeferred (@NonNull final String sTransactionID,
                                             @NonNull final String sSbdhInstanceID,
                                             @NonNull final String sVerifierName,
                                             @NonNull final OffsetDateTime aNextRetryDT,
                                             @Nullable final String sErrorDetails)
  {
    final Span aSpan = Span.current ();
    if (aSpan != null && aSpan.isRecording ())
    {
      aSpan.addEvent ("inbound.verification_deferred",
                      Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                     AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID));
    }

    _logWarnSafe (ObservabilityEcsJson.createWarn ("onInboundVerificationDeferred")
                                      .peppol ("direction", "INBOUND")
                                      .peppol ("transaction_id", sTransactionID)
                                      .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                      .peppol ("verifier_name", sVerifierName)
                                      .peppol ("next_retry_dt", aNextRetryDT.toString ())
                                      .error (sErrorDetails != null ? sErrorDetails : "Inbound verification deferred"));
  }

  public void onOutboundVerificationRejection (@NonNull final String sSbdhInstanceID,
                                               @NonNull final VerifierResult aVerifierResult)
  {
    final String sReason = aVerifierResult.outcome ().getMessage ();
    final String sMsg = "Outbound verification rejected: " + (sReason != null ? sReason : aVerifierResult.verifierName ());
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onOutboundVerificationRejection")
                                 .peppol ("direction", "OUTBOUND")
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("verifier_name", aVerifierResult.verifierName ())
                                 .error (sReason != null ? sReason : "Outbound verification rejected"));
  }

  public void onInboundDuplicateRejected (@NonNull final String sSenderID,
                                          @NonNull final String sReceiverID,
                                          @NonNull final String sDocTypeID,
                                          @NonNull final String sProcessID,
                                          @Nullable final String sSenderProviderID,
                                          @Nullable final String sAS4MessageID,
                                          @NonNull final String sSbdhInstanceID,
                                          final boolean bIsDuplicateAS4,
                                          final boolean bIsDuplicateSBDH,
                                          @NonNull final String sErrorDetails)
  {
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_SENDER_ID), sSenderID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_RECEIVER_ID), sReceiverID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sErrorDetails, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onInboundDuplicateRejected")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("sender_id", sSenderID)
                                 .peppol ("receiver_id", sReceiverID)
                                 .peppol ("doc_type_id", sDocTypeID)
                                 .peppol ("process_id", sProcessID)
                                 .peppol ("sender_provider_id", sSenderProviderID)
                                 .peppol ("as4_message_id", sAS4MessageID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .peppol ("is_duplicate_as4", bIsDuplicateAS4)
                                 .peppol ("is_duplicate_sbdh", bIsDuplicateSBDH)
                                 .error (sErrorDetails));
  }

  public void onInboundMLSCorrelationError (@NonNull final String sTransactionID,
                                            @NonNull final String sReferencedSbdhInstanceID,
                                            @NonNull final EPeppolMLSResponseCode eMlsResponseCode)
  {
    final String sMsg = "Inbound MLS correlation error for " + sReferencedSbdhInstanceID;
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sReferencedSbdhInstanceID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_MLS_RESPONSE_CODE), eMlsResponseCode.getID ());
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onInboundMLSCorrelationError")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("referenced_sbdh_instance_id", sReferencedSbdhInstanceID)
                                 .peppol ("mls_response_code", eMlsResponseCode.getID ())
                                 .error (sMsg));
  }

  public void onSpecialMlsToNotReachable (@NonNull final String sOutboundTransactionID,
                                          @NonNull final String sReferencedSbdhInstanceID,
                                          @NonNull final String sAttemptedMlsToParticipantID,
                                          @NonNull final String sFallbackDefaultSpidParticipantID)
  {
    final String sMsg = "MLS receiver '" + sAttemptedMlsToParticipantID + "' unreachable, falling back to '" + sFallbackDefaultSpidParticipantID + "'";
    _logWarnSafe (ObservabilityEcsJson.createWarn ("onSpecialMlsToNotReachable")
                                      .peppol ("direction", "OUTBOUND")
                                      .peppol ("transaction_id", sOutboundTransactionID)
                                      .peppol ("referenced_sbdh_instance_id", sReferencedSbdhInstanceID)
                                      .peppol ("attempted_mls_to", sAttemptedMlsToParticipantID)
                                      .peppol ("fallback_spid", sFallbackDefaultSpidParticipantID)
                                      .error (sMsg));
  }

  public void onInboundForwardingError (@NonNull final String sTransactionID, final boolean bIsRetry)
  {
    final String sMsg = "Inbound forwarding error (retry=" + bIsRetry + ")";
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.booleanKey (CPhossAPOtel.ATTR_IS_RETRY), Boolean.valueOf (bIsRetry));
    _recordSpanError (sMsg, aAttrs);

    _logWarnSafe (ObservabilityEcsJson.createWarn ("onInboundForwardingError")
                                      .peppol ("direction", "INBOUND")
                                      .peppol ("transaction_id", sTransactionID)
                                      .peppol ("is_retry", bIsRetry)
                                      .error (sMsg));
  }

  public void onInboundPermanentForwardingFailure (@NonNull final String sTransactionID,
                                                   @NonNull final String sSbdhInstanceID,
                                                   @Nullable final String sErrorDetails)
  {
    final String sMsg = "Inbound permanent forwarding failure" + (sErrorDetails != null ? ": " + sErrorDetails : "");
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onInboundPermanentForwardingFailure")
                                 .peppol ("direction", "INBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .error (sErrorDetails != null ? sErrorDetails : "Inbound permanent forwarding failure"));
  }

  public void onOutboundPermanentSendingFailure (@NonNull final String sTransactionID,
                                                 @NonNull final String sSbdhInstanceID,
                                                 @Nullable final String sErrorDetails)
  {
    final String sMsg = "Outbound permanent sending failure" + (sErrorDetails != null ? ": " + sErrorDetails : "");
    final Attributes aAttrs = Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_TRANSACTION_ID), sTransactionID,
                                             AttributeKey.stringKey (CPhossAPOtel.ATTR_SBDH_INSTANCE_ID), sSbdhInstanceID);
    _recordSpanError (sMsg, aAttrs);

    _logSafe (ObservabilityEcsJson.createError ("onOutboundPermanentSendingFailure")
                                 .peppol ("direction", "OUTBOUND")
                                 .peppol ("transaction_id", sTransactionID)
                                 .peppol ("sbdh_instance_id", sSbdhInstanceID)
                                 .error (sErrorDetails != null ? sErrorDetails : "Outbound permanent sending failure"));
  }

  public void onPeppolReportingTSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final String sPeriod = aYearMonth.toString ();
    final String sMsg = "Peppol TSR reporting failed for " + sPeriod;
    _recordSpanError (sMsg, Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH), sPeriod));

    _logSafe (ObservabilityEcsJson.createError ("onPeppolReportingTSRFailure")
                                 .peppol ("reporting_period", sPeriod)
                                 .error (sMsg));
  }

  public void onPeppolReportingEUSRFailure (@NonNull final YearMonth aYearMonth)
  {
    final String sPeriod = aYearMonth.toString ();
    final String sMsg = "Peppol EUSR reporting failed for " + sPeriod;
    _recordSpanError (sMsg, Attributes.of (AttributeKey.stringKey (CPhossAPOtel.ATTR_REPORT_YEAR_MONTH), sPeriod));

    _logSafe (ObservabilityEcsJson.createError ("onPeppolReportingEUSRFailure")
                                 .peppol ("reporting_period", sPeriod)
                                 .error (sMsg));
  }

  public void onUnexpectedException (@NonNull final String sContext,
                                     @NonNull final String sMessage,
                                     @NonNull final Exception aException)
  {
    final Span aSpan = Span.current ();
    if (aSpan != null && aSpan.isRecording ())
    {
      aSpan.setStatus (StatusCode.ERROR, sContext + ": " + sMessage);
      aSpan.recordException (aException);
    }

    _logSafe (ObservabilityEcsJson.createError ("onUnexpectedException")
                                 .peppol ("exception_context", sContext)
                                 .error (sContext + ": " + sMessage)
                                 .exception (aException));
  }
}
