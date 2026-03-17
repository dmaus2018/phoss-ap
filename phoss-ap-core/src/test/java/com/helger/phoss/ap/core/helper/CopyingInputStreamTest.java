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
package com.helger.phoss.ap.core.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Test class for {@link CopyingInputStream}.
 *
 * @author Philip Helger
 */
public final class CopyingInputStreamTest
{
  @Test
  public void testSingleByteReadCopies () throws IOException
  {
    final byte [] aInput = { 1, 2, 3, 4, 5 };
    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      for (int i = 0; i < aInput.length; i++)
        assertEquals (aInput[i], aCIS.read ());
      // EOF
      assertEquals (-1, aCIS.read ());
    }
    assertArrayEquals (aInput, aOS.toByteArray ());
  }

  @Test
  public void testBulkReadCopies () throws IOException
  {
    final byte [] aInput = "Hello Peppol World".getBytes (StandardCharsets.UTF_8);
    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      final byte [] aBuf = new byte [1024];
      final int nRead = aCIS.read (aBuf, 0, aBuf.length);
      assertEquals (aInput.length, nRead);
    }
    assertArrayEquals (aInput, aOS.toByteArray ());
  }

  @Test
  public void testEmptyInput () throws IOException
  {
    final byte [] aInput = new byte [0];
    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      assertEquals (-1, aCIS.read ());
    }
    assertEquals (0, aOS.size ());
  }

  @Test
  public void testPartialBulkReads () throws IOException
  {
    final byte [] aInput = "ABCDEFGHIJ".getBytes (StandardCharsets.UTF_8);
    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      // Read in small chunks
      final byte [] aBuf = new byte [3];
      int nTotal = 0;
      int nRead;
      while ((nRead = aCIS.read (aBuf, 0, aBuf.length)) != -1)
        nTotal += nRead;
      assertEquals (aInput.length, nTotal);
    }
    assertArrayEquals (aInput, aOS.toByteArray ());
  }

  @Test
  public void testMixedSingleAndBulkReads () throws IOException
  {
    final byte [] aInput = { 10, 20, 30, 40, 50, 60 };
    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      // Single byte
      assertEquals (10, aCIS.read ());
      // Bulk read rest
      final byte [] aBuf = new byte [10];
      final int nRead = aCIS.read (aBuf, 0, aBuf.length);
      assertEquals (5, nRead);
    }
    assertArrayEquals (aInput, aOS.toByteArray ());
  }

  @Test
  public void testLargePayload () throws IOException
  {
    final byte [] aInput = new byte [100_000];
    for (int i = 0; i < aInput.length; i++)
      aInput[i] = (byte) (i & 0xFF);

    final ByteArrayOutputStream aOS = new ByteArrayOutputStream ();

    try (final CopyingInputStream aCIS = new CopyingInputStream (new ByteArrayInputStream (aInput), aOS))
    {
      final byte [] aBuf = new byte [8192];
      while (aCIS.read (aBuf, 0, aBuf.length) != -1)
      {
        // just drain
      }
    }
    assertArrayEquals (aInput, aOS.toByteArray ());
  }
}
