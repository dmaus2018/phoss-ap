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
package com.helger.phoss.ap.storage.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.scope.mock.ScopeTestRule;

/**
 * Unit test for {@link DocumentPayloadManagerDb}
 *
 * @author Philip Helger
 */
public final class DocumentPayloadManagerDbTest
{
  @ClassRule
  public static final ScopeTestRule RULE = new ScopeTestRule ();

  @Test
  public void testBasicOperations () throws Exception
  {
    final DocumentPayloadManagerDb aMgr = new DocumentPayloadManagerDb ();
    aMgr.verifyConfiguration ();

    final OffsetDateTime aReferenceDT = OffsetDateTime.of (2026, 3, 18, 14, 30, 0, 0, ZoneOffset.UTC);
    final String sPath = "test-path/doc.xml";
    final byte [] aBytes = "hello database payload!".getBytes (StandardCharsets.UTF_8);

    assertFalse (aMgr.existsDocument (sPath));

    // Store
    aMgr.storeDocument ("test-path", aReferenceDT, "doc.xml", aBytes);
    assertTrue (aMgr.existsDocument (sPath));
    assertArrayEquals (aBytes, aMgr.readDocument (sPath));

    // Open read stream
    try (final InputStream aIS = aMgr.openDocumentStreamForRead (sPath))
    {
      final byte [] aReadBytes = com.helger.base.io.stream.StreamHelper.getAllBytes (aIS);
      assertArrayEquals (aBytes, aReadBytes);
    }

    // Rename
    final String sRenamedPath = aMgr.renameFile (sPath, "test-path", "doc-renamed", ".xml");
    assertEquals ("test-path/doc-renamed.xml", sRenamedPath);
    assertFalse (aMgr.existsDocument (sPath));
    assertTrue (aMgr.existsDocument (sRenamedPath));
    assertArrayEquals (aBytes, aMgr.readDocument (sRenamedPath));

    // Delete
    assertTrue (aMgr.deleteDocument (sRenamedPath));
    assertFalse (aMgr.existsDocument (sRenamedPath));
    assertFalse (aMgr.deleteDocument (sRenamedPath));
  }
}
