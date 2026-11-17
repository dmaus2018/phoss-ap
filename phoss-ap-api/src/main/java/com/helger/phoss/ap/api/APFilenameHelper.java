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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FilenameHelper;

/**
 * Helper to derive file names from values that are not under the control of the AP, like the SBDH
 * Instance Identifier.
 * <p>
 * Everything that is not an ASCII letter, an ASCII digit, '.', '-' or '_' is replaced with '_'. That
 * is deliberately stricter than {@link FilenameHelper#getAsSecureValidASCIIFilename(String)},
 * because the path separators must not survive - a sender provided value must never be able to
 * point outside of the directory it is stored in, or to create nested directories - and because
 * ':' is illegal on Windows only, which would name the very same document differently depending on
 * the operating system the AP runs on.
 * </p>
 *
 * @author Philip Helger
 * @since 0.13.3
 */
@Immutable
public final class APFilenameHelper
{
  /** The character that replaces every character that is not safe within a filename */
  public static final char REPLACEMENT_CHAR = '_';

  private APFilenameHelper ()
  {}

  /**
   * Check if the passed character may be used unchanged within a filename.
   *
   * @param c
   *        The character to check.
   * @return <code>true</code> if it is an ASCII letter, an ASCII digit, '.', '-' or '_'.
   */
  public static boolean isSafeFilenameChar (final char c)
  {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_';
  }

  /**
   * Replace every character that is not safe within a filename with {@link #REPLACEMENT_CHAR}.
   * Nothing else is changed, so the result may still be empty, start with a dot or be a Windows
   * reserved name. Use {@link #getAsSafeFilename(String)} to get a usable filename.
   *
   * @param sValue
   *        The value to be sanitised. May be <code>null</code>.
   * @param bAllowPathSeparator
   *        <code>true</code> to keep the Unix path separator '/'. The Windows path separator is
   *        always replaced.
   * @return The sanitised value. Never <code>null</code> but maybe empty.
   * @see #isSafeFilenameChar(char)
   */
  @NonNull
  public static String getSanitized (@Nullable final String sValue, final boolean bAllowPathSeparator)
  {
    if (StringHelper.isEmpty (sValue))
      return "";

    final StringBuilder ret = new StringBuilder (sValue.length ());
    for (final char c : sValue.toCharArray ())
      if (isSafeFilenameChar (c) || (bAllowPathSeparator && c == FilenameHelper.UNIX_SEPARATOR))
        ret.append (c);
      else
        ret.append (REPLACEMENT_CHAR);
    return ret.toString ();
  }

  /**
   * Convert the passed value into a filename that can be used as a single path segment on every
   * operating system:
   * <ol>
   * <li>Replace all unsafe characters including the path separators (see
   * {@link #getSanitized(String, boolean)})</li>
   * <li>Remove all leading dots, so that the result is no hidden file that a downstream poller never
   * picks up</li>
   * <li>Apply {@link FilenameHelper#getAsSecureValidASCIIFilename(String)} for the illegal suffixes
   * and the Windows reserved names</li>
   * </ol>
   *
   * @param sValue
   *        The value to be converted. May be <code>null</code>.
   * @return <code>null</code> if nothing usable remains, e.g. for a value consisting of dots only.
   *         Otherwise the safe filename, which is never empty.
   */
  @Nullable
  @Nonempty
  public static String getAsSafeFilename (@Nullable final String sValue)
  {
    final String sSanitized = StringHelper.trimStartRepeatedly (getSanitized (sValue, false), '.');
    return FilenameHelper.getAsSecureValidASCIIFilename (sSanitized);
  }
}
