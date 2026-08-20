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
package com.helger.phoss.ap.core.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;
import com.helger.phoss.ap.api.exception.InboundVerifierUnavailableException;
import com.helger.phoss.ap.api.model.MlsOutcome;
import com.helger.phoss.ap.api.model.MlsOutcomeIssue;
import com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI;
import com.helger.phoss.ap.core.inbound.InboundOrchestrator.VerifierResult;

/**
 * Test class for the inbound document verifier evaluation of {@link InboundOrchestrator}.
 *
 * @author Philip Helger
 */
public final class InboundOrchestratorVerifierTest
{
  private static final String LOG_PREFIX = "[Test] ";
  private static final String DOC_PATH = "/tmp/whatever.sbd";
  private static final IDocumentTypeIdentifier DOCTYPE_ID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
  private static final IProcessIdentifier PROCESS_ID = PeppolIdentifierFactory.INSTANCE.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");

  /**
   * A verifier that always returns the provided outcome and remembers if it was called.
   */
  private static final class MockVerifier implements IInboundDocumentVerifierSPI
  {
    private final MlsOutcome m_aOutcome;
    private final InboundVerifierUnavailableException m_aEx;
    private boolean m_bCalled = false;

    MockVerifier (@Nullable final MlsOutcome aOutcome, @Nullable final InboundVerifierUnavailableException aEx)
    {
      m_aOutcome = aOutcome;
      m_aEx = aEx;
    }

    @Nullable
    public MlsOutcome verifyInboundDocument (@NonNull @Nonempty final String sDocumentPath,
                                             @NonNull final IDocumentTypeIdentifier aDocTypeID,
                                             @NonNull final IProcessIdentifier aProcessID) throws InboundVerifierUnavailableException
    {
      m_bCalled = true;
      if (m_aEx != null)
        throw m_aEx;
      return m_aOutcome;
    }
  }

  @NonNull
  private static VerifierResult _run (@NonNull final List <MockVerifier> aVerifiers)
  {
    return InboundOrchestrator.runInboundVerifiers (LOG_PREFIX, aVerifiers, DOC_PATH, DOCTYPE_ID, PROCESS_ID);
  }

  @Test
  public void testAllAccepting ()
  {
    final VerifierResult aVR = _run (new CommonsArrayList <> (new MockVerifier (null, null),
                                                              new MockVerifier (MlsOutcome.acceptance (), null)));
    assertSame (EVerificationOutcomeCategory.PASSED, aVR.category ());
    assertNull (aVR.outcome ());
    assertNull (aVR.verifierName ());
  }

  @Test
  public void testRejectionWins ()
  {
    // The rejection of the second verifier must win over the unavailability of the first one
    final MlsOutcome aRejection = MlsOutcome.rejection ("Malware found",
                                                        MlsOutcomeIssue.businessRuleViolation ("NA", "Virus found"));
    final MockVerifier aUnavailable = new MockVerifier (MlsOutcome.serviceUnavailable ("Scanner down",
                                                                                        MlsOutcomeIssue.failureOfDelivery ("Connection refused")),
                                                         null);
    final MockVerifier aRejecting = new MockVerifier (aRejection, null);
    final VerifierResult aVR = _run (new CommonsArrayList <> (aUnavailable, aRejecting));
    assertSame (EVerificationOutcomeCategory.REJECTION, aVR.category ());
    assertSame (aRejection, aVR.outcome ());
    assertEquals ("InboundOrchestratorVerifierTest$MockVerifier", aVR.verifierName ());
  }

  @Test
  public void testRejectionStopsEvaluation ()
  {
    final MockVerifier aRejecting = new MockVerifier (MlsOutcome.rejection ("Invalid",
                                                                            MlsOutcomeIssue.businessRuleViolation ("NA",
                                                                                                                   "Invalid document")),
                                                       null);
    final MockVerifier aNeverCalled = new MockVerifier (null, null);
    final VerifierResult aVR = _run (new CommonsArrayList <> (aRejecting, aNeverCalled));
    assertSame (EVerificationOutcomeCategory.REJECTION, aVR.category ());
    assertFalse (aNeverCalled.m_bCalled);
  }

  @Test
  public void testUnavailableViaOutcome ()
  {
    final MlsOutcome aUnavailable = MlsOutcome.serviceUnavailable ("Scanner down",
                                                                   MlsOutcomeIssue.failureOfDelivery ("Connection refused"));
    final MockVerifier aVerifier1 = new MockVerifier (aUnavailable, null);
    // The remaining verifiers must still be evaluated
    final MockVerifier aVerifier2 = new MockVerifier (null, null);
    final VerifierResult aVR = _run (new CommonsArrayList <> (aVerifier1, aVerifier2));
    assertSame (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, aVR.category ());
    assertSame (aUnavailable, aVR.outcome ());
    assertEquals ("InboundOrchestratorVerifierTest$MockVerifier", aVR.verifierName ());
    assertTrue (aVerifier2.m_bCalled);
  }

  @Test
  public void testUnavailableViaException ()
  {
    final MockVerifier aVerifier = new MockVerifier (null,
                                                     new InboundVerifierUnavailableException ("VirusScanVerifier",
                                                                                              "ICAP connection refused"));
    final VerifierResult aVR = _run (new CommonsArrayList <> (aVerifier));
    assertSame (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, aVR.category ());
    assertEquals ("VirusScanVerifier", aVR.verifierName ());
    // A synthetic outcome must be present, so that the fail modes "closed" and "deferred" have
    // something to send as MLS
    assertNotNull (aVR.outcome ());
    assertSame (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, aVR.outcome ().getCategory ());
    assertEquals (1, aVR.outcome ().getIssues ().size ());
  }

  @Test
  public void testFirstUnavailableWins ()
  {
    final MockVerifier aVerifier1 = new MockVerifier (null,
                                                      new InboundVerifierUnavailableException ("First", "down"));
    final MockVerifier aVerifier2 = new MockVerifier (null,
                                                      new InboundVerifierUnavailableException ("Second", "down"));
    final VerifierResult aVR = _run (new CommonsArrayList <> (aVerifier1, aVerifier2));
    assertSame (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, aVR.category ());
    assertEquals ("First", aVR.verifierName ());
  }
}
