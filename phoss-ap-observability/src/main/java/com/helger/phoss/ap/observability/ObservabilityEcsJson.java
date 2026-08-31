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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;

/**
 * Helper class to construct single-line JSON records adhering strictly to the Elastic Common
 * Schema (ECS 8.x).
 *
 * @author Philip Helger
 */
@Immutable
public final class ObservabilityEcsJson
{
  public static final String ECS_VERSION = "8.11.0";
  public static final String SERVICE_NAME = "phoss-ap";
  public static final String EVENT_DATASET = "phoss.ap.events";

  private ObservabilityEcsJson ()
  {}

  /**
   * Fluent builder for single-line ECS JSON events.
   */
  public static final class Builder
  {
    private final String m_sLogLevel;
    private final String m_sAction;
    private final String m_sOutcome;
    private Long m_aDurationNanos;
    private final IJsonObject m_aPeppol = new JsonObject ();
    private String m_sErrorMessage;
    private String m_sErrorType;
    private String m_sStackTrace;

    public Builder (@NonNull final String sLogLevel,
                    @NonNull final String sAction,
                    @NonNull final String sOutcome)
    {
      m_sLogLevel = sLogLevel;
      m_sAction = sAction;
      m_sOutcome = sOutcome;
    }

    @NonNull
    public Builder duration (@Nullable final Duration aDuration)
    {
      if (aDuration != null)
        m_aDurationNanos = Long.valueOf (aDuration.toNanos ());
      return this;
    }

    @NonNull
    public Builder peppol (@NonNull final String sKey, @Nullable final String sValue)
    {
      if (sValue != null)
        m_aPeppol.add (sKey, sValue);
      return this;
    }

    @NonNull
    public Builder peppol (@NonNull final String sKey, final boolean bValue)
    {
      m_aPeppol.add (sKey, bValue);
      return this;
    }

    @NonNull
    public Builder peppol (@NonNull final String sKey, final int nValue)
    {
      m_aPeppol.add (sKey, nValue);
      return this;
    }

    @NonNull
    public Builder error (@Nullable final String sMessage)
    {
      m_sErrorMessage = sMessage;
      return this;
    }

    @NonNull
    public Builder exception (@Nullable final Exception aException)
    {
      if (aException != null)
      {
        m_sErrorType = aException.getClass ().getName ();
        if (m_sErrorMessage == null)
          m_sErrorMessage = aException.getMessage ();

        final StringWriter aSW = new StringWriter ();
        aException.printStackTrace (new PrintWriter (aSW));
        m_sStackTrace = aSW.toString ();
      }
      return this;
    }

    @NonNull
    public String buildJson ()
    {
      final IJsonObject aRoot = new JsonObject ();
      aRoot.add ("@timestamp", DateTimeFormatter.ISO_INSTANT.format (Instant.now ()));
      aRoot.add ("ecs.version", ECS_VERSION);
      aRoot.add ("log.level", m_sLogLevel);
      aRoot.add ("service.name", SERVICE_NAME);

      final IJsonObject aEvent = new JsonObject ();
      aEvent.add ("dataset", EVENT_DATASET);
      aEvent.add ("action", m_sAction);
      aEvent.add ("outcome", m_sOutcome);
      if (m_aDurationNanos != null)
        aEvent.add ("duration", m_aDurationNanos.longValue ());
      aRoot.add ("event", aEvent);

      if (m_sErrorMessage != null || m_sStackTrace != null || m_sErrorType != null)
      {
        final IJsonObject aError = new JsonObject ();
        if (m_sErrorMessage != null)
          aError.add ("message", m_sErrorMessage);
        if (m_sErrorType != null)
          aError.add ("type", m_sErrorType);
        if (m_sStackTrace != null)
          aError.add ("stack_trace", m_sStackTrace);
        aRoot.add ("error", aError);
      }

      if (m_aPeppol.isNotEmpty ())
        aRoot.add ("peppol", m_aPeppol);

      return aRoot.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS);
    }
  }

  @NonNull
  public static Builder createInfo (@NonNull final String sAction)
  {
    return new Builder ("INFO", sAction, "success");
  }

  @NonNull
  public static Builder createWarn (@NonNull final String sAction)
  {
    return new Builder ("WARN", sAction, "failure");
  }

  @NonNull
  public static Builder createError (@NonNull final String sAction)
  {
    return new Builder ("ERROR", sAction, "failure");
  }
}
