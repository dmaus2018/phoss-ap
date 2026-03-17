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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.Test;

import com.helger.security.messagedigest.EMessageDigestAlgorithm;

/**
 * Test class for {@link HashHelper}.
 *
 * @author Philip Helger
 */
public final class HashHelperTest
{
  @Test
  public void testMDAlgorithmIsSHA256 ()
  {
    assertEquals (EMessageDigestAlgorithm.SHA_256, HashHelper.MD_ALGO);
  }

  @Test
  public void testCreateMessageDigest ()
  {
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    assertNotNull (aMD);
    assertEquals ("SHA-256", aMD.getAlgorithm ());
  }

  @Test
  public void testSha256HexEmptyArray ()
  {
    // SHA-256 of empty input is well-known
    final String sHash = HashHelper.sha256Hex (new byte [0]);
    assertEquals ("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sHash);
  }

  @Test
  public void testSha256HexKnownInput ()
  {
    // SHA-256("hello") is well-known
    final String sHash = HashHelper.sha256Hex ("hello".getBytes (StandardCharsets.UTF_8));
    assertEquals ("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sHash);
  }

  @Test
  public void testSha256HexConsistency ()
  {
    final byte [] aInput = "Peppol Access Point".getBytes (StandardCharsets.UTF_8);
    final String sHash1 = HashHelper.sha256Hex (aInput);
    final String sHash2 = HashHelper.sha256Hex (aInput);
    assertEquals (sHash1, sHash2);
  }

  @Test
  public void testGetDigestHexWithMessageDigest ()
  {
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    aMD.update ("hello".getBytes (StandardCharsets.UTF_8));
    final String sHash = HashHelper.getDigestHex (aMD);
    assertEquals ("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sHash);
  }

  @Test
  public void testGetDigestHexFromBytes ()
  {
    // Manually compute a digest and pass the raw bytes
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    final byte [] aDigest = aMD.digest ("hello".getBytes (StandardCharsets.UTF_8));
    final String sHash = HashHelper.getDigestHex (aDigest);
    assertEquals ("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sHash);
  }

  @Test
  public void testSha256HexProducesLowercaseHex ()
  {
    final String sHash = HashHelper.sha256Hex (new byte [] { 0x00 });
    assertNotNull (sHash);
    // SHA-256 produces 32 bytes = 64 hex chars
    assertEquals (64, sHash.length ());
    // Verify all lowercase hex
    assertEquals (sHash, sHash.toLowerCase (java.util.Locale.US));
  }

  @Test
  public void testIncrementalDigestMatchesSingleShot ()
  {
    final byte [] aInput = "incremental test data".getBytes (StandardCharsets.UTF_8);

    // Single-shot
    final String sSingleShot = HashHelper.sha256Hex (aInput);

    // Incremental via MessageDigest
    final MessageDigest aMD = HashHelper.createMessageDigest ();
    aMD.update (aInput, 0, 5);
    aMD.update (aInput, 5, aInput.length - 5);
    final String sIncremental = HashHelper.getDigestHex (aMD);

    assertEquals (sSingleShot, sIncremental);
  }
}
