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
package com.helger.phoss.ap.testbackend.model;

import java.time.OffsetDateTime;

public class ReceivedDocument
{
  private final String m_sID;
  private final String m_sChannel;
  private final String m_sFilename;
  private final long m_nSizeBytes;
  private final OffsetDateTime m_aReceivedDT;
  private final String m_sStoragePath;
  private boolean m_bCallbackSent;

  public ReceivedDocument (final String sID,
                           final String sChannel,
                           final String sFilename,
                           final long nSizeBytes,
                           final OffsetDateTime aReceivedDT,
                           final String sStoragePath)
  {
    m_sID = sID;
    m_sChannel = sChannel;
    m_sFilename = sFilename;
    m_nSizeBytes = nSizeBytes;
    m_aReceivedDT = aReceivedDT;
    m_sStoragePath = sStoragePath;
  }

  public String getID ()
  {
    return m_sID;
  }

  public String getChannel ()
  {
    return m_sChannel;
  }

  public String getFilename ()
  {
    return m_sFilename;
  }

  public long getSizeBytes ()
  {
    return m_nSizeBytes;
  }

  public OffsetDateTime getReceivedDT ()
  {
    return m_aReceivedDT;
  }

  public String getStoragePath ()
  {
    return m_sStoragePath;
  }

  public boolean isCallbackSent ()
  {
    return m_bCallbackSent;
  }

  public void setCallbackSent (final boolean bCallbackSent)
  {
    m_bCallbackSent = bCallbackSent;
  }
}
