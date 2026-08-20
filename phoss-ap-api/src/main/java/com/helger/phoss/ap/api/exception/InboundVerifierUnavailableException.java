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
package com.helger.phoss.ap.api.exception;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;

/**
 * Checked exception to be thrown by an
 * {@link com.helger.phoss.ap.api.spi.IInboundDocumentVerifierSPI}, if the document could not be
 * verified, because the verifier backend service was unavailable. It is the exception based
 * alternative to returning an {@link com.helger.phoss.ap.api.model.MlsOutcome} of category
 * {@link com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory#SERVICE_UNAVAILABLE} and is
 * handled identically.
 * <p>
 * Throwing this exception is never an implicit rejection of the document. How it is handled,
 * depends on the configured {@link com.helger.phoss.ap.api.codelist.EVerificationFailMode}.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public class InboundVerifierUnavailableException extends Exception
{
  private final String m_sVerifierName;

  /**
   * Constructor.
   *
   * @param sVerifierName
   *        The name of the verifier that was unavailable. Usually the class name. May neither be
   *        <code>null</code> nor empty.
   * @param sErrorMessage
   *        The error message detailing the service unavailability. May neither be <code>null</code>
   *        nor empty.
   */
  public InboundVerifierUnavailableException (@NonNull @Nonempty final String sVerifierName,
                                              @NonNull @Nonempty final String sErrorMessage)
  {
    super (ValueEnforcer.notEmpty (sErrorMessage, "ErrorMessage"));
    m_sVerifierName = ValueEnforcer.notEmpty (sVerifierName, "VerifierName");
  }

  /**
   * Constructor.
   *
   * @param sVerifierName
   *        The name of the verifier that was unavailable. Usually the class name. May neither be
   *        <code>null</code> nor empty.
   * @param sErrorMessage
   *        The error message detailing the service unavailability. May neither be <code>null</code>
   *        nor empty.
   * @param aCause
   *        The causing exception. May be <code>null</code>.
   */
  public InboundVerifierUnavailableException (@NonNull @Nonempty final String sVerifierName,
                                              @NonNull @Nonempty final String sErrorMessage,
                                              @NonNull final Throwable aCause)
  {
    super (ValueEnforcer.notEmpty (sErrorMessage, "ErrorMessage"), aCause);
    m_sVerifierName = ValueEnforcer.notEmpty (sVerifierName, "VerifierName");
  }

  /**
   * @return The name of the verifier that was unavailable. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getVerifierName ()
  {
    return m_sVerifierName;
  }
}
