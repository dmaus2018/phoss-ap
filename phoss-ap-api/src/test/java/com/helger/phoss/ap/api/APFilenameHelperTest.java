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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link APFilenameHelper}.
 *
 * @author Philip Helger
 */
public final class APFilenameHelperTest
{
  @Test
  public void testIsSafeFilenameChar ()
  {
    for (final char c : "azAZ09.-_".toCharArray ())
      assertTrue (APFilenameHelper.isSafeFilenameChar (c));
    for (final char c : "/\\: \t\0*?<>|\"%äé".toCharArray ())
      assertFalse (APFilenameHelper.isSafeFilenameChar (c));
  }

  @Test
  public void testGetSanitized ()
  {
    assertEquals ("", APFilenameHelper.getSanitized (null, false));
    assertEquals ("", APFilenameHelper.getSanitized ("", true));
    assertEquals ("IM_2026_10_4807678", APFilenameHelper.getSanitized ("IM 2026/10/4807678", false));
    assertEquals ("IM_2026/10/4807678", APFilenameHelper.getSanitized ("IM 2026/10/4807678", true));
    // The Windows separator is never kept
    assertEquals ("a_b", APFilenameHelper.getSanitized ("a\\b", true));
    // Leading dots are kept
    assertEquals ("../x", APFilenameHelper.getSanitized ("../x", true));
    assertEquals ("a_b_c", APFilenameHelper.getSanitized ("a:bäc", false));
  }

  @Test
  public void testGetAsSafeFilename ()
  {
    assertEquals ("plain.sbd", APFilenameHelper.getAsSafeFilename ("plain.sbd"));
    assertEquals ("IM_2026_10_4807678.sbd", APFilenameHelper.getAsSafeFilename ("IM 2026/10/4807678.sbd"));

    // Path traversal
    assertEquals ("_.._etc_passwd", APFilenameHelper.getAsSafeFilename ("../../etc/passwd"));
    assertEquals ("a_.._.._x.sbd", APFilenameHelper.getAsSafeFilename ("a/../../x.sbd"));

    // Hidden files
    assertEquals ("hidden", APFilenameHelper.getAsSafeFilename (".hidden"));
    assertEquals ("sbd", APFilenameHelper.getAsSafeFilename ("....sbd"));

    // Illegal suffixes
    assertEquals ("abc", APFilenameHelper.getAsSafeFilename ("abc..."));

    // Windows reserved names
    assertEquals ("_CON", APFilenameHelper.getAsSafeFilename ("CON"));
    assertEquals ("_nul.sbd", APFilenameHelper.getAsSafeFilename ("nul.sbd"));

    // Nothing usable remains
    assertNull (APFilenameHelper.getAsSafeFilename (null));
    assertNull (APFilenameHelper.getAsSafeFilename (""));
    assertNull (APFilenameHelper.getAsSafeFilename ("."));
    assertNull (APFilenameHelper.getAsSafeFilename ("..."));
  }
}
