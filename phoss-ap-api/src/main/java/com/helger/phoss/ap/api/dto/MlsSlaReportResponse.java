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
package com.helger.phoss.ap.api.dto;

import java.util.List;

/**
 * JSON response DTO representing an MLS SLA compliance report with individual measurement entries
 * and aggregated statistics. Usable both for server-side serialization and client-side
 * deserialization.
 * <p>
 * Note: The server-side {@code fromDomain(MlsSlaReport)} factory method remains in the webapp
 * module because it depends on {@code phoss-ap-db} types.
 *
 * @author Philip Helger
 */
public class MlsSlaReportResponse
{
  private int totalCount;
  private int withinSlaCount;
  private double compliancePercent;
  private double targetPercent;
  private long thresholdSeconds;
  private boolean meetingSla;
  private List <MlsSlaEntryResponse> entries;

  /**
   * Default constructor for JSON deserialization.
   */
  public MlsSlaReportResponse ()
  {}

  /** @return the total number of measured transactions */
  public int getTotalCount ()
  {
    return totalCount;
  }

  /**
   * @param n
   *        The total count to set.
   */
  public void setTotalCount (final int n)
  {
    totalCount = n;
  }

  /** @return the number of transactions within the SLA threshold */
  public int getWithinSlaCount ()
  {
    return withinSlaCount;
  }

  /**
   * @param n
   *        The within-SLA count to set.
   */
  public void setWithinSlaCount (final int n)
  {
    withinSlaCount = n;
  }

  /** @return the SLA compliance percentage */
  public double getCompliancePercent ()
  {
    return compliancePercent;
  }

  /**
   * @param d
   *        The compliance percentage to set.
   */
  public void setCompliancePercent (final double d)
  {
    compliancePercent = d;
  }

  /** @return the target SLA percentage */
  public double getTargetPercent ()
  {
    return targetPercent;
  }

  /**
   * @param d
   *        The target percentage to set.
   */
  public void setTargetPercent (final double d)
  {
    targetPercent = d;
  }

  /** @return the SLA threshold in seconds */
  public long getThresholdSeconds ()
  {
    return thresholdSeconds;
  }

  /**
   * @param n
   *        The threshold in seconds to set.
   */
  public void setThresholdSeconds (final long n)
  {
    thresholdSeconds = n;
  }

  /** @return <code>true</code> if the SLA target is being met */
  public boolean isMeetingSla ()
  {
    return meetingSla;
  }

  /**
   * @param b
   *        <code>true</code> if meeting the SLA target.
   */
  public void setMeetingSla (final boolean b)
  {
    meetingSla = b;
  }

  /** @return the list of individual SLA measurement entries */
  public List <MlsSlaEntryResponse> getEntries ()
  {
    return entries;
  }

  /**
   * @param aEntries
   *        The SLA measurement entries to set.
   */
  public void setEntries (final List <MlsSlaEntryResponse> aEntries)
  {
    entries = aEntries;
  }
}
