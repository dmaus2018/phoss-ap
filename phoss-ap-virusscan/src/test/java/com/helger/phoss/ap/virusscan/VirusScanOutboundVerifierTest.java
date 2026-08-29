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

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.ap.api.model.VerificationOutcome;
import com.helger.phoss.ap.basic.APBasicMetaManager;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for {@link VirusScanOutboundVerifier}.
 *
 * @author Philip Helger
 */
public final class VirusScanOutboundVerifierTest
{
  @Rule
  public final ScopeTestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testDisabledByDefault ()
  {
    final IIdentifierFactory aIF = APBasicMetaManager.getIdentifierFactory ();
    final IDocumentTypeIdentifier aDocTypeID = aIF.createDocumentTypeIdentifierWithDefaultScheme ("dummy-doctype");
    final IProcessIdentifier aProcessID = aIF.createProcessIdentifierWithDefaultScheme ("dummy-process");

    final VirusScanOutboundVerifier aVerifier = new VirusScanOutboundVerifier ();
    final VerificationOutcome aOutcome = aVerifier.verifyOutboundDocument ("dummy-path", aDocTypeID, aProcessID);
    assertTrue (aOutcome.isPassed ());
  }
}
