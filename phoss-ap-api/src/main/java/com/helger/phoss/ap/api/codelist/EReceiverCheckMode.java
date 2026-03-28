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
package com.helger.phoss.ap.api.codelist;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Receiver check mode for incoming Peppol AS4 messages. Determines how the AP validates that it is
 * the intended recipient for a message by cross-checking against SMP registrations.
 *
 * @author Philip Helger
 * @since v0.1.3
 */
public enum EReceiverCheckMode implements IHasID <String>
{
  /** Receiver checks are disabled. */
  NONE ("none"),
  /** Receiver check uses a fixed SMP URL (requires {@code peppol.smp.url}). */
  SMP ("smp"),
  /** Receiver check uses dynamic per-participant SMP resolution via SML. */
  SML ("sml");

  public static final EReceiverCheckMode DEFAULT = NONE;

  private final String m_sID;

  EReceiverCheckMode (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  /** {@inheritDoc} */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Find the enum constant matching the given ID.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or <code>null</code> if not found.
   */
  @Nullable
  public static EReceiverCheckMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EReceiverCheckMode.class, sID);
  }

  /**
   * Find the enum constant matching the given ID or use the default.
   *
   * @param sID
   *        The ID to look up. May be <code>null</code>.
   * @return The matching enum constant, or {@link #DEFAULT} if not found.
   */
  @NonNull
  public static EReceiverCheckMode getFromIDOrDefault (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrDefault (EReceiverCheckMode.class, sID, DEFAULT);
  }
}
