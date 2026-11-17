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
package com.helger.phoss.ap.api;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.base.state.ESuccess;

/**
 * Base interface for archiving transactions.
 *
 * @author Philip Helger
 */
public interface IArchivalManager
{
  /**
   * Archive the provided outbound transaction. This includes the main transaction as well as all
   * attempts. The transaction is only archived, if it still fulfils the criteria of
   * {@link IOutboundTransactionManager#getAllForArchival(int)} - it may have changed since it was
   * selected.
   *
   * @param sID
   *        The outbound transaction to archive. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if this call archived the transaction,
   *         {@link ESuccess#FAILURE} if it is already archived, no longer eligible for archival or
   *         locked by a concurrent transaction.
   */
  @NonNull
  ESuccess archiveOutboundTransaction (@NonNull String sID);

  /**
   * Archive the provided inbound transaction. This includes the main transaction as well as all
   * attempts. The transaction is only archived, if it still fulfils the criteria of
   * {@link IInboundTransactionManager#getAllForArchival(int, boolean)} - it may have changed since
   * it was selected.
   *
   * @param sID
   *        The inbound transaction to archive. May not be <code>null</code>.
   * @param bExcludeMlsPending
   *        <code>true</code> to not archive a transaction that still waits for its MLS. Must be the
   *        same value that was used for {@link IInboundTransactionManager#getAllForArchival(int, boolean)}.
   * @return {@link ESuccess#SUCCESS} if this call archived the transaction,
   *         {@link ESuccess#FAILURE} if it is already archived, no longer eligible for archival or
   *         locked by a concurrent transaction.
   * @since 0.14.0
   */
  @NonNull
  ESuccess archiveInboundTransaction (@NonNull String sID, boolean bExcludeMlsPending);

  /**
   * Run a single cleanup pass on archived outbound transactions whose {@code completed_dt} is older
   * than the cutoff. For each candidate, the {@code aDocumentDeleter} predicate is invoked with the
   * absolute document path; if it returns <code>true</code>, the archive row and its associated
   * sending-attempt rows are deleted in a single transaction. If it returns <code>false</code>, the
   * row is left for the next cycle (so a transient storage error does not produce orphan files).
   *
   * @param aCutoff
   *        Cutoff timestamp; rows older than this are eligible for cleanup. May not be
   *        <code>null</code>.
   * @param nBatchSize
   *        Maximum number of rows to consider in this pass. Must be &gt;= 1.
   * @param aDocumentDeleter
   *        Predicate invoked with the absolute document path; returns <code>true</code> when row
   *        deletion should proceed. May not be <code>null</code>.
   * @return The number of archive rows successfully deleted.
   * @since 0.9.0
   */
  @Nonnegative
  int cleanupOutbound (@NonNull OffsetDateTime aCutoff,
                       @Nonnegative int nBatchSize,
                       @NonNull Predicate <String> aDocumentDeleter);

  /**
   * Run a single cleanup pass on archived inbound transactions. Symmetric to
   * {@link #cleanupOutbound(OffsetDateTime, int, Predicate)}.
   *
   * @param aCutoff
   *        Cutoff timestamp; rows older than this are eligible for cleanup. May not be
   *        <code>null</code>.
   * @param nBatchSize
   *        Maximum number of rows to consider in this pass. Must be &gt;= 1.
   * @param aDocumentDeleter
   *        Predicate invoked with the absolute document path; returns <code>true</code> when row
   *        deletion should proceed. May not be <code>null</code>.
   * @return The number of archive rows successfully deleted.
   * @since 0.9.0
   */
  @Nonnegative
  int cleanupInbound (@NonNull OffsetDateTime aCutoff,
                      @Nonnegative int nBatchSize,
                      @NonNull Predicate <String> aDocumentDeleter);
}
