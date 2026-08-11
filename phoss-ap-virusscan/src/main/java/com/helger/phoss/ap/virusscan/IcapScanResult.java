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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phoss.ap.api.codelist.EVerificationOutcomeCategory;

/**
 * Immutable DTO representing the outcome of an ICAP scan operation.
 *
 * @author Philip Helger
 */
@Immutable
public final class IcapScanResult
{
  private final EVerificationOutcomeCategory m_eCategory;
  private final String m_sThreatName;
  private final String m_sErrorMessage;

  private IcapScanResult (@NonNull final EVerificationOutcomeCategory eCategory,
                          @Nullable final String sThreatName,
                          @Nullable final String sErrorMessage)
  {
    ValueEnforcer.notNull (eCategory, "Category");
    m_eCategory = eCategory;
    m_sThreatName = sThreatName;
    m_sErrorMessage = sErrorMessage;
  }

  /**
   * @return The outcome category. Never <code>null</code>.
   */
  @NonNull
  public EVerificationOutcomeCategory getCategory ()
  {
    return m_eCategory;
  }

  /**
   * @return <code>true</code> if the document passed virus scanning (no threats found).
   */
  public boolean isPassed ()
  {
    return m_eCategory == EVerificationOutcomeCategory.PASSED;
  }

  /**
   * @return <code>true</code> if a virus or malware threat was detected.
   */
  public boolean isRejection ()
  {
    return m_eCategory == EVerificationOutcomeCategory.REJECTION;
  }

  /**
   * @return <code>true</code> if the ICAP server was unavailable or failed.
   */
  public boolean isServiceUnavailable ()
  {
    return m_eCategory == EVerificationOutcomeCategory.SERVICE_UNAVAILABLE;
  }

  /**
   * @return The threat name if infected, or <code>null</code>.
   */
  @Nullable
  public String getThreatName ()
  {
    return m_sThreatName;
  }

  /**
   * @return The error message if service unavailable, or <code>null</code>.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("category", m_eCategory)
                                       .appendIfNotNull ("threatName", m_sThreatName)
                                       .appendIfNotNull ("errorMessage", m_sErrorMessage)
                                       .getToString ();
  }

  /**
   * Create a clean/passed scan result.
   *
   * @return Clean scan result.
   */
  @NonNull
  public static IcapScanResult passed ()
  {
    return new IcapScanResult (EVerificationOutcomeCategory.PASSED, null, null);
  }

  /**
   * Create an infected/rejection scan result.
   *
   * @param sThreatName
   *        The threat name detected. May be <code>null</code>.
   * @return Rejection scan result.
   */
  @NonNull
  public static IcapScanResult rejection (@Nullable final String sThreatName)
  {
    final String sEffectiveThreat = StringHelper.hasText (sThreatName) ? sThreatName : "Virus detected";
    return new IcapScanResult (EVerificationOutcomeCategory.REJECTION, sEffectiveThreat, null);
  }

  /**
   * Create a service unavailable scan result.
   *
   * @param sErrorMessage
   *        The connection or scanner error message. May be <code>null</code>.
   * @return Service unavailable scan result.
   */
  @NonNull
  public static IcapScanResult serviceUnavailable (@Nullable final String sErrorMessage)
  {
    final String sEffectiveError = StringHelper.hasText (sErrorMessage) ? sErrorMessage
                                                                        : "ICAP service unavailable";
    return new IcapScanResult (EVerificationOutcomeCategory.SERVICE_UNAVAILABLE, null, sEffectiveError);
  }
}
