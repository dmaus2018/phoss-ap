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

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.phoss.ap.api.codelist.EVerificationFailMode;
import com.helger.phoss.ap.api.config.APConfigProvider;
import com.helger.phoss.ap.api.config.APConfigurationProperties;

/**
 * Accessor for ICAP virus scanning configuration properties.
 *
 * @author Philip Helger
 */
@Immutable
public final class VirusScanConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (VirusScanConfig.class);

  public static final String VIRUSSCAN_ICAP_ENABLED = "virusscan.icap.enabled";
  public static final boolean VIRUSSCAN_ICAP_ENABLED_DEFAULT = false;

  public static final String VIRUSSCAN_ICAP_INBOUND_ENABLED = "virusscan.icap.inbound.enabled";
  public static final String VIRUSSCAN_ICAP_OUTBOUND_ENABLED = "virusscan.icap.outbound.enabled";

  public static final String VIRUSSCAN_ICAP_HOST = "virusscan.icap.host";
  public static final String VIRUSSCAN_ICAP_HOST_DEFAULT = "localhost";

  public static final String VIRUSSCAN_ICAP_PORT = "virusscan.icap.port";
  public static final int VIRUSSCAN_ICAP_PORT_DEFAULT = 1344;

  public static final String VIRUSSCAN_ICAP_SERVICE = "virusscan.icap.service";
  public static final String VIRUSSCAN_ICAP_SERVICE_DEFAULT = "avscan";

  public static final String VIRUSSCAN_ICAP_TIMEOUT = "virusscan.icap.timeout";
  public static final Duration VIRUSSCAN_ICAP_TIMEOUT_DEFAULT = Duration.ofSeconds (5);

  public static final String VIRUSSCAN_ICAP_FAIL_MODE = "virusscan.icap.fail-mode";

  public static final String VIRUSSCAN_ICAP_ATTACHMENTS_ONLY = "virusscan.icap.attachments-only";
  public static final boolean VIRUSSCAN_ICAP_ATTACHMENTS_ONLY_DEFAULT = false;

  private VirusScanConfig ()
  {}

  @NonNull
  private static IConfig _getConfig ()
  {
    return APConfigProvider.getConfig ();
  }

  /**
   * @return <code>true</code> if ICAP virus scanning is enabled globally via
   *         <code>virusscan.icap.enabled</code>.
   */
  public static boolean isEnabled ()
  {
    return _getConfig ().getAsBoolean (VIRUSSCAN_ICAP_ENABLED, VIRUSSCAN_ICAP_ENABLED_DEFAULT);
  }

  /**
   * @return <code>true</code> if ICAP virus scanning is enabled for inbound documents. Checks
   *         <code>virusscan.icap.inbound.enabled</code> if set, otherwise falls back to
   *         {@link #isEnabled()}.
   */
  public static boolean isInboundEnabled ()
  {
    final String sVal = _getConfig ().getAsString (VIRUSSCAN_ICAP_INBOUND_ENABLED);
    if (StringHelper.hasText (sVal))
      return _getConfig ().getAsBoolean (VIRUSSCAN_ICAP_INBOUND_ENABLED, isEnabled ());
    return isEnabled ();
  }

  /**
   * @return <code>true</code> if ICAP virus scanning is enabled for outbound documents. Checks
   *         <code>virusscan.icap.outbound.enabled</code> if set, otherwise falls back to
   *         {@link #isEnabled()}.
   */
  public static boolean isOutboundEnabled ()
  {
    final String sVal = _getConfig ().getAsString (VIRUSSCAN_ICAP_OUTBOUND_ENABLED);
    if (StringHelper.hasText (sVal))
      return _getConfig ().getAsBoolean (VIRUSSCAN_ICAP_OUTBOUND_ENABLED, isEnabled ());
    return isEnabled ();
  }

  /**
   * @return The ICAP server hostname. Never <code>null</code>.
   */
  @NonNull
  public static String getHost ()
  {
    return _getConfig ().getAsString (VIRUSSCAN_ICAP_HOST, VIRUSSCAN_ICAP_HOST_DEFAULT);
  }

  /**
   * @return The ICAP server port.
   */
  public static int getPort ()
  {
    return _getConfig ().getAsInt (VIRUSSCAN_ICAP_PORT, VIRUSSCAN_ICAP_PORT_DEFAULT);
  }

  /**
   * @return The ICAP service/path name. Never <code>null</code>.
   */
  @NonNull
  public static String getService ()
  {
    return _getConfig ().getAsString (VIRUSSCAN_ICAP_SERVICE, VIRUSSCAN_ICAP_SERVICE_DEFAULT);
  }

  /**
   * @return The ICAP timeout duration. Never <code>null</code>.
   */
  @NonNull
  public static Duration getTimeoutDuration ()
  {
    final Duration aDuration = _getConfig ().getAsConfigDuration (VIRUSSCAN_ICAP_TIMEOUT,
                                                                  sErr -> LOGGER.warn ("Failed to parse configuration key '" +
                                                                                       VIRUSSCAN_ICAP_TIMEOUT +
                                                                                       "' as duration: " +
                                                                                       sErr));
    return aDuration != null ? aDuration : VIRUSSCAN_ICAP_TIMEOUT_DEFAULT;
  }

  /**
   * @return The effective fail-mode for ICAP virus scanning ({@link EVerificationFailMode#CLOSED},
   *         {@link EVerificationFailMode#OPEN}, or {@link EVerificationFailMode#DEFERRED}). Checks
   *         <code>virusscan.icap.fail-mode</code> first, falling back to global
   *         <code>verification.verifier-fail-mode</code>.
   */
  @NonNull
  public static EVerificationFailMode getFailMode ()
  {
    final String sSpecific = _getConfig ().getAsString (VIRUSSCAN_ICAP_FAIL_MODE);
    if (StringHelper.hasText (sSpecific))
    {
      final EVerificationFailMode eMode = EVerificationFailMode.getFromIDOrNull (sSpecific);
      if (eMode != null)
        return eMode;
    }

    final String sGlobal = _getConfig ().getAsString (APConfigurationProperties.VERIFICATION_FAIL_MODE);
    if (StringHelper.hasText (sGlobal))
    {
      final EVerificationFailMode eMode = EVerificationFailMode.getFromIDOrNull (sGlobal);
      if (eMode != null)
        return eMode;
    }

    return EVerificationFailMode.DEFAULT;
  }

  /**
   * @return <code>true</code> if only embedded binary attachments should be scanned, bypassing ICAP
   *         scanning for pure XML messages without attachments.
   */
  public static boolean isAttachmentsOnly ()
  {
    return _getConfig ().getAsBoolean (VIRUSSCAN_ICAP_ATTACHMENTS_ONLY,
                                       VIRUSSCAN_ICAP_ATTACHMENTS_ONLY_DEFAULT);
  }
}
