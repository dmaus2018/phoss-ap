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
package com.helger.phoss.ap.basic.mgr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.wrapper.Wrapper;
import com.helger.phoss.ap.api.mgr.IDocumentPayloadManager;

/**
 * Abstract contract test suite for {@link IDocumentPayloadManager} implementations. Each behavioral
 * expectation is written once here and verified identically against every backend (filesystem, S3,
 * etc.).
 * <p>
 * Subclasses provide the concrete implementation via {@link #createManager()} and a base directory
 * via {@link #getBaseDir()}.
 *
 * @author Philip Helger
 */
public abstract class AbstractDocumentPayloadManagerContractTest
{
  protected static final OffsetDateTime REFERENCE_DT = OffsetDateTime.of (2026, 3, 18, 14, 30, 0, 0, ZoneOffset.UTC);

  private IDocumentPayloadManager m_aManager;

  /**
   * @return A freshly initialized {@link IDocumentPayloadManager} for this test. Called before each
   *         test method.
   */
  protected abstract IDocumentPayloadManager createManager ();

  /**
   * @return The base directory (or S3 key prefix) to use in store operations.
   */
  protected abstract String getBaseDir ();

  /**
   * Optional cleanup hook called after each test.
   */
  protected void cleanup ()
  {
    // Override if needed
  }

  @Before
  public void setUp ()
  {
    m_aManager = createManager ();
    assertNotNull ("createManager() must not return null", m_aManager);
  }

  @After
  public void tearDown ()
  {
    cleanup ();
  }

  // --- storeDocument / readDocument round-trip ---

  @Test
  public void testStoreAndReadRoundTrip ()
  {
    final byte [] aPayload = "Hello, Peppol!".getBytes (StandardCharsets.UTF_8);
    final String sPath = m_aManager.storeDocument (getBaseDir (), REFERENCE_DT, "test-doc.xml", aPayload);
    assertNotNull (sPath);
    assertFalse (sPath.isEmpty ());

    final byte [] aRead = m_aManager.readDocument (sPath);
    assertArrayEquals (aPayload, aRead);
  }

  @Test
  public void testStoreEmptyDocument ()
  {
    final byte [] aPayload = {};
    final String sPath = m_aManager.storeDocument (getBaseDir (), REFERENCE_DT, "empty.xml", aPayload);
    assertNotNull (sPath);

    final byte [] aRead = m_aManager.readDocument (sPath);
    assertEquals (0, aRead.length);
  }

  @Test
  public void testStoreLargeDocument ()
  {
    // 1 MB payload
    final byte [] aPayload = new byte [1024 * 1024];
    for (int i = 0; i < aPayload.length; i++)
      aPayload[i] = (byte) (i % 256);

    final String sPath = m_aManager.storeDocument (getBaseDir (), REFERENCE_DT, "large.bin", aPayload);
    final byte [] aRead = m_aManager.readDocument (sPath);
    assertArrayEquals (aPayload, aRead);
  }

  // --- openDocumentStreamForWrite / openDocumentStreamForRead ---

  @Test
  public void testStreamWriteAndStreamRead ()
  {
    final byte [] aPayload = "Stream round-trip".getBytes (StandardCharsets.UTF_8);
    final Wrapper <String> aPathHolder = Wrapper.empty ();

    try (final OutputStream aOS = m_aManager.openDocumentStreamForWrite (getBaseDir (),
                                                                         REFERENCE_DT,
                                                                         "stream-doc",
                                                                         ".xml",
                                                                         aPathHolder::set))
    {
      aOS.write (aPayload);
    }
    catch (final Exception ex)
    {
      throw new AssertionError ("Failed to write via stream", ex);
    }

    assertTrue ("Path consumer must have been called", aPathHolder.isSet ());
    final String sPath = aPathHolder.get ();

    try (final InputStream aIS = m_aManager.openDocumentStreamForRead (sPath))
    {
      final byte [] aRead = aIS.readAllBytes ();
      assertArrayEquals (aPayload, aRead);
    }
    catch (final Exception ex)
    {
      throw new AssertionError ("Failed to read via stream", ex);
    }
  }

  // --- openTemporaryDocumentStreamForWrite ---

  @Test
  public void testTemporaryStreamWrite ()
  {
    final byte [] aPayload = "Temporary content".getBytes (StandardCharsets.UTF_8);
    final Wrapper <String> aPathHolder = Wrapper.empty ();

    try (final OutputStream aOS = m_aManager.openTemporaryDocumentStreamForWrite (getBaseDir (),
                                                                                  REFERENCE_DT,
                                                                                  aPathHolder::set))
    {
      aOS.write (aPayload);
    }
    catch (final Exception ex)
    {
      throw new AssertionError ("Failed to write temporary stream", ex);
    }

    assertTrue ("Path consumer must have been called", aPathHolder.isSet ());
    final byte [] aRead = m_aManager.readDocument (aPathHolder.get ());
    assertArrayEquals (aPayload, aRead);
  }

  // --- existsDocument ---

  @Test
  public void testExistsAfterStore ()
  {
    final String sPath = m_aManager.storeDocument (getBaseDir (),
                                                   REFERENCE_DT,
                                                   "exists-test.xml",
                                                   "data".getBytes (StandardCharsets.UTF_8));
    assertTrue (m_aManager.existsDocument (sPath));
  }

  @Test
  public void testExistsReturnsFalseForNonExistent ()
  {
    assertFalse (m_aManager.existsDocument (getNonExistentPath ()));
  }

  // --- deleteDocument ---

  @Test
  public void testDeleteExistingDocument ()
  {
    final String sPath = m_aManager.storeDocument (getBaseDir (),
                                                   REFERENCE_DT,
                                                   "delete-test.xml",
                                                   "data".getBytes (StandardCharsets.UTF_8));
    assertTrue (m_aManager.existsDocument (sPath));

    final boolean bDeleted = m_aManager.deleteDocument (sPath);
    assertTrue ("deleteDocument should return true for existing document", bDeleted);
    assertFalse ("Document should no longer exist after deletion", m_aManager.existsDocument (sPath));
  }

  @Test
  public void testDeleteNonExistentDocument ()
  {
    final boolean bDeleted = m_aManager.deleteDocument (getNonExistentPath ());
    assertFalse ("deleteDocument should return false for non-existent document", bDeleted);
  }

  // --- renameFile ---

  @Test
  public void testRenameFile ()
  {
    final byte [] aPayload = "Rename me".getBytes (StandardCharsets.UTF_8);
    final Wrapper <String> aPathHolder = Wrapper.empty ();

    try (final OutputStream aOS = m_aManager.openTemporaryDocumentStreamForWrite (getBaseDir (),
                                                                                  REFERENCE_DT,
                                                                                  aPathHolder::set))
    {
      aOS.write (aPayload);
    }
    catch (final Exception ex)
    {
      throw new AssertionError ("Failed to write temporary stream", ex);
    }

    final String sTempPath = aPathHolder.get ();
    assertTrue (m_aManager.existsDocument (sTempPath));

    // Extract directory from the temp path for use as target dir
    final String sTargetDir = extractDirectory (sTempPath);

    final String sNewPath = m_aManager.renameFile (sTempPath, sTargetDir, "final-name", ".sbd");
    assertNotNull (sNewPath);
    assertFalse (sNewPath.isEmpty ());

    // Old path should be gone, new path should have the content
    assertFalse ("Original file should no longer exist", m_aManager.existsDocument (sTempPath));
    assertTrue ("Renamed file should exist", m_aManager.existsDocument (sNewPath));
    assertArrayEquals (aPayload, m_aManager.readDocument (sNewPath));
  }

  // --- Helpers for subclasses ---

  /**
   * @return A path that is guaranteed to not exist in the current backend. Subclasses override for
   *         backend-specific non-existent paths.
   */
  protected abstract String getNonExistentPath ();

  /**
   * Extract the directory portion from a document path. For filesystem paths this is the parent
   * directory; for S3 URIs this is the key prefix up to the last slash.
   *
   * @param sPath
   *        The full document path.
   * @return The directory portion.
   */
  protected abstract String extractDirectory (String sPath);
}
