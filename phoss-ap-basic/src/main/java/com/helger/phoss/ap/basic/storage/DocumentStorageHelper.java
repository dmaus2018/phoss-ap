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
package com.helger.phoss.ap.basic.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;

/**
 * Utility class for reading and writing document files on disk. Documents are
 * stored as flat files rather than as BYTEA columns in the database.
 *
 * @author Philip Helger
 */
@Immutable
public final class DocumentStorageHelper
{
  private DocumentStorageHelper ()
  {}

  /**
   * Write bytes to a file under the given base directory, using the provided
   * filename. Creates the directory if needed.
   *
   * @param aBaseDir
   *        The base directory to store the file in. May not be
   *        <code>null</code>.
   * @param sFilename
   *        The filename to use. May not be <code>null</code>.
   * @param aBytes
   *        The bytes to write. May not be <code>null</code>.
   * @return The absolute path of the stored file.
   */
  @NonNull
  public static String storeDocument (@NonNull final File aBaseDir,
                                      @NonNull final String sFilename,
                                      final byte @NonNull [] aBytes)
  {
    ValueEnforcer.notNull (aBaseDir, "BaseDir");
    ValueEnforcer.notNull (sFilename, "Filename");
    ValueEnforcer.notNull (aBytes, "Bytes");

    try
    {
      Files.createDirectories (aBaseDir.toPath ());
      final Path aFilePath = aBaseDir.toPath ().resolve (sFilename);
      Files.write (aFilePath, aBytes);
      return aFilePath.toAbsolutePath ().toString ();
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException ("Failed to store document '" + sFilename + "' in " + aBaseDir.getAbsolutePath (),
                                      ex);
    }
  }

  /**
   * Read all bytes from the file at the given absolute path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return The file contents as a byte array.
   */
  public static byte @NonNull [] readDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.readAllBytes (Path.of (sAbsolutePath));
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException ("Failed to read document from " + sAbsolutePath, ex);
    }
  }

  /**
   * Open an {@link InputStream} for the file at the given absolute path. The
   * caller is responsible for closing the stream.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return An open input stream.
   */
  @NonNull
  public static InputStream openDocumentStream (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.newInputStream (Path.of (sAbsolutePath));
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException ("Failed to open document stream for " + sAbsolutePath, ex);
    }
  }

  /**
   * Delete the document file at the given path.
   *
   * @param sAbsolutePath
   *        The absolute path of the file. May not be <code>null</code>.
   * @return <code>true</code> if the file was deleted, <code>false</code> if it
   *         did not exist.
   */
  public static boolean deleteDocument (@NonNull final String sAbsolutePath)
  {
    ValueEnforcer.notNull (sAbsolutePath, "AbsolutePath");

    try
    {
      return Files.deleteIfExists (Path.of (sAbsolutePath));
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException ("Failed to delete document at " + sAbsolutePath, ex);
    }
  }
}
