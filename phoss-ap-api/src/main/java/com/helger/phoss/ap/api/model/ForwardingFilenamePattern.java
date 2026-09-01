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
package com.helger.phoss.ap.api.model;

import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.config.IConfig;
import com.helger.io.file.FilenameHelper;

/**
 * The configurable name under which a document forwarder stores a forwarded document - the base
 * name of a file or the object key of a blob store, so without the ".xml" of the document and
 * without the ".json" of an optional metadata sidecar.
 * <p>
 * A downstream routing service that picks the documents up from a shared directory usually routes
 * them by the receiver participant ID. A fixed layout forces it to open every payload or to read
 * the metadata sidecar for that, which is why the layout is configurable per forwarder since
 * v0.12.0. The supported placeholders are usable in the <code>{name}</code> as well as in the
 * <code>${name}</code> syntax.
 * </p>
 * <p>
 * Every placeholder value comes from the received document and is therefore sanitized: everything
 * that is not an ASCII letter, an ASCII digit, '.', '-' or '_' is replaced with '_'. That is
 * deliberately stricter than {@link FilenameHelper#getAsSecureValidASCIIFilename(String)}, because
 * the path separators must not survive - the resolved name is appended to an upload directory, so a
 * sender provided value like the SBDH Instance Identifier must never be able to point outside of it
 * - and because ':' is illegal on Windows only, which would name the very same document differently
 * depending on the operating system the AP runs on.
 * </p>
 *
 * @author Philip Helger
 * @since 0.12.0
 */
public class ForwardingFilenamePattern
{
  /** Reception timestamp, formatted according to {@link #DATETIME_PATTERN} */
  public static final String PLACEHOLDER_DATETIME = "datetime";
  /** The phase4 Incoming ID; the transaction ID for a self-generated document */
  public static final String PLACEHOLDER_INCOMING_ID = "incoming-id";
  /** The SBDH Instance Identifier */
  public static final String PLACEHOLDER_SBDH_INSTANCE_ID = "sbdh-instance-id";
  /** The receiver participant ID including the identifier scheme */
  public static final String PLACEHOLDER_RECEIVER_ID = "receiver-id";
  /** The receiver participant ID without the identifier scheme */
  public static final String PLACEHOLDER_RECEIVER_VALUE = "receiver-value";
  /** The sender participant ID including the identifier scheme */
  public static final String PLACEHOLDER_SENDER_ID = "sender-id";
  /** The sender participant ID without the identifier scheme */
  public static final String PLACEHOLDER_SENDER_VALUE = "sender-value";
  /** The Document Type ID */
  public static final String PLACEHOLDER_DOCTYPE_ID = "doctype-id";
  /** The Process ID */
  public static final String PLACEHOLDER_PROCESS_ID = "process-id";

  /** The format of the {@link #PLACEHOLDER_DATETIME} value */
  public static final String DATETIME_PATTERN = "yyyyMMddHHmmss";
  /**
   * Maximum length of a resolved filename, leaving room for the extension and for the ".tmp" of an
   * atomic write within the usual limit of 255 bytes per filename
   */
  public static final int MAX_LENGTH_FILENAME = 200;
  /** Maximum length of a resolved object key, leaving room for the key prefix and the extension */
  public static final int MAX_LENGTH_OBJECT_KEY = 900;

  private static final Logger LOGGER = LoggerFactory.getLogger (ForwardingFilenamePattern.class);
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern (DATETIME_PATTERN);
  private static final ICommonsOrderedSet <String> ALL_PLACEHOLDERS = new CommonsLinkedHashSet <> (PLACEHOLDER_DATETIME,
                                                                                                   PLACEHOLDER_INCOMING_ID,
                                                                                                   PLACEHOLDER_SBDH_INSTANCE_ID,
                                                                                                   PLACEHOLDER_RECEIVER_ID,
                                                                                                   PLACEHOLDER_RECEIVER_VALUE,
                                                                                                   PLACEHOLDER_SENDER_ID,
                                                                                                   PLACEHOLDER_SENDER_VALUE,
                                                                                                   PLACEHOLDER_DOCTYPE_ID,
                                                                                                   PLACEHOLDER_PROCESS_ID);

  private final String m_sPattern;
  private final boolean m_bAllowPathSeparator;
  private final int m_nMaxLength;

  /**
   * Constructor. Use {@link #checkPattern(String, String, boolean)} to verify the pattern before,
   * or create the instance from the configuration with
   * {@link #createFilenameFromConfig(IConfig, String, String)} or with
   * {@link #createObjectKeyFromConfig(IConfig, String, String)}.
   *
   * @param sPattern
   *        The pattern to be resolved. May neither be <code>null</code> nor empty.
   * @param bAllowPathSeparator
   *        <code>true</code> if the literal part of the pattern may contain path separators, so
   *        that it can span "directories" - suitable for an object key but not for a filename. The
   *        placeholder values never contain a path separator, no matter what is passed here.
   * @param nMaxLength
   *        The maximum length of the resolved name. Must be &gt; 0.
   */
  public ForwardingFilenamePattern (@NonNull @Nonempty final String sPattern,
                                    final boolean bAllowPathSeparator,
                                    @Nonnegative final int nMaxLength)
  {
    ValueEnforcer.notEmpty (sPattern, "Pattern");
    ValueEnforcer.isGT0 (nMaxLength, "MaxLength");
    m_sPattern = sPattern;
    m_bAllowPathSeparator = bAllowPathSeparator;
    m_nMaxLength = nMaxLength;
  }

  /**
   * Replace every character that is not safe within a name with an underscore.
   *
   * @param sValue
   *        The value to be sanitized. May be <code>null</code>.
   * @param bAllowPathSeparator
   *        <code>true</code> to keep the path separators.
   * @return The sanitized value. Never <code>null</code> but maybe empty.
   */
  @NonNull
  private static String _getSanitized (@Nullable final String sValue, final boolean bAllowPathSeparator)
  {
    if (StringHelper.isEmpty (sValue))
      return "";

    final StringBuilder ret = new StringBuilder (sValue.length ());
    for (final char c : sValue.toCharArray ())
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          c == '.' ||
          c == '-' ||
          c == '_' ||
          (bAllowPathSeparator && c == '/'))
        ret.append (c);
      else
        ret.append ('_');
    return ret.toString ();
  }

  /**
   * Get the value part of a URI encoded Peppol identifier, meaning everything after the "::" that
   * separates the identifier scheme from the identifier value. So
   * "iso6523-actorid-upis::0088:123456" becomes "0088:123456" - the ICD stays part of the value,
   * because the same number in a different ICD is a different participant.
   *
   * @param sURIEncodedID
   *        The URI encoded identifier. May not be <code>null</code>.
   * @return The identifier value, or the unchanged input if it contains no scheme separator. Never
   *         <code>null</code>.
   */
  @NonNull
  private static String _getValueWithoutScheme (@NonNull final String sURIEncodedID)
  {
    final int nIndex = sURIEncodedID.indexOf ("::");
    return nIndex < 0 ? sURIEncodedID : sURIEncodedID.substring (nIndex + 2);
  }

  /**
   * Walk the pattern and replace every placeholder with the value the resolver returns for it. Both
   * the "{name}" and the "${name}" syntax are supported. An opening brace that has no matching
   * closing one - or that contains another opening brace - is taken literally.
   *
   * @param sPattern
   *        The pattern to be resolved. May not be <code>null</code>.
   * @param aPlaceholderResolver
   *        The resolver invoked with the name of each placeholder found. May not be
   *        <code>null</code> and may not return <code>null</code>.
   * @param aLiteralResolver
   *        The resolver invoked with each literal part between the placeholders. May not be
   *        <code>null</code> and may not return <code>null</code>.
   * @return The resolved pattern. Never <code>null</code>.
   */
  @NonNull
  private static String _resolve (@NonNull final String sPattern,
                                  @NonNull final UnaryOperator <String> aPlaceholderResolver,
                                  @NonNull final UnaryOperator <String> aLiteralResolver)
  {
    final int nMax = sPattern.length ();
    final StringBuilder aResolved = new StringBuilder (nMax);
    final StringBuilder aLiteral = new StringBuilder ();
    int nIndex = 0;
    while (nIndex < nMax)
    {
      final char c = sPattern.charAt (nIndex);
      final boolean bDollar = c == '$' && nIndex + 1 < nMax && sPattern.charAt (nIndex + 1) == '{';
      if (bDollar || c == '{')
      {
        final int nStart = bDollar ? nIndex + 1 : nIndex;
        final int nEnd = sPattern.indexOf ('}', nStart + 1);
        // A nested opening brace means that this one is not a placeholder start
        final int nNested = sPattern.indexOf ('{', nStart + 1);
        if (nEnd >= 0 && (nNested < 0 || nNested > nEnd))
        {
          aResolved.append (aLiteralResolver.apply (aLiteral.toString ()));
          aLiteral.setLength (0);
          aResolved.append (aPlaceholderResolver.apply (sPattern.substring (nStart + 1, nEnd)));
          nIndex = nEnd + 1;
          continue;
        }
      }
      aLiteral.append (c);
      nIndex++;
    }
    return aResolved.append (aLiteralResolver.apply (aLiteral.toString ())).toString ();
  }

  /**
   * Collect the unsanitized values of all supported placeholders of a single document.
   *
   * @param aDocument
   *        The document to take the values from. May not be <code>null</code>.
   * @return The map from placeholder name to value. Never <code>null</code>.
   */
  @NonNull
  private static ICommonsMap <String, String> _getPlaceholderValues (@NonNull final IForwardableDocument aDocument)
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    ret.put (PLACEHOLDER_DATETIME, DATETIME_FORMATTER.format (aDocument.timestamp ()));
    ret.put (PLACEHOLDER_INCOMING_ID, aDocument.localID ());
    ret.put (PLACEHOLDER_SBDH_INSTANCE_ID, aDocument.sbdhInstanceID ());
    ret.put (PLACEHOLDER_RECEIVER_ID, aDocument.receiverID ());
    ret.put (PLACEHOLDER_RECEIVER_VALUE, _getValueWithoutScheme (aDocument.receiverID ()));
    ret.put (PLACEHOLDER_SENDER_ID, aDocument.senderID ());
    ret.put (PLACEHOLDER_SENDER_VALUE, _getValueWithoutScheme (aDocument.senderID ()));
    ret.put (PLACEHOLDER_DOCTYPE_ID, aDocument.docTypeID ());
    ret.put (PLACEHOLDER_PROCESS_ID, aDocument.processID ());
    return ret;
  }

  /**
   * @return The names of all supported placeholders, without the surrounding braces. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsOrderedSet <String> getAllPlaceholders ()
  {
    return ALL_PLACEHOLDERS.getClone ();
  }

  /**
   * Check a pattern before it is used the first time, so that a typo shows up as a configuration
   * error at startup and not as a directory full of files called "{reciever-id}.xml". All problems
   * are logged as errors, except the one that cannot guarantee unique names - that one is a
   * warning, because it may well be intended.
   *
   * @param sConfigKey
   *        The configuration key the pattern was read from, for logging only. May not be
   *        <code>null</code>.
   * @param sPattern
   *        The pattern to be checked. May be <code>null</code>.
   * @param bAllowPathSeparator
   *        <code>true</code> if the literal part of the pattern may contain path separators.
   * @return {@link ESuccess#SUCCESS} if the pattern is usable. Never <code>null</code>.
   */
  @NonNull
  public static ESuccess checkPattern (@NonNull final String sConfigKey,
                                       @Nullable final String sPattern,
                                       final boolean bAllowPathSeparator)
  {
    ValueEnforcer.notNull (sConfigKey, "ConfigKey");

    if (StringHelper.isEmpty (sPattern))
    {
      LOGGER.error ("The forwarding filename pattern in '" + sConfigKey + "' may not be empty");
      return ESuccess.FAILURE;
    }

    final ICommonsOrderedSet <String> aUsed = new CommonsLinkedHashSet <> ();
    final ICommonsOrderedSet <String> aUnknown = new CommonsLinkedHashSet <> ();
    // The result is the literal part of the pattern only - all placeholders resolve to nothing
    final String sLiteralPart = _resolve (sPattern, sName -> {
      aUsed.add (sName);
      if (!ALL_PLACEHOLDERS.contains (sName))
        aUnknown.add (sName);
      return "";
    }, UnaryOperator.identity ());

    if (aUnknown.isNotEmpty ())
    {
      LOGGER.error ("The forwarding filename pattern in '" +
                    sConfigKey +
                    "' uses the unknown placeholder(s) " +
                    StringImplode.getImploded (", ", aUnknown) +
                    ". Supported are only " +
                    StringImplode.getImploded (", ", ALL_PLACEHOLDERS));
      return ESuccess.FAILURE;
    }

    if (sLiteralPart.indexOf ('{') >= 0 || sLiteralPart.indexOf ('}') >= 0)
    {
      LOGGER.error ("The forwarding filename pattern in '" + sConfigKey + "' contains unbalanced placeholder braces");
      return ESuccess.FAILURE;
    }

    if (!bAllowPathSeparator && StringHelper.containsAny (sLiteralPart, FilenameHelper::isPathSeparatorChar))
    {
      LOGGER.error ("The forwarding filename pattern in '" +
                    sConfigKey +
                    "' may not contain a path separator - every document is stored in the configured target directory");
      return ESuccess.FAILURE;
    }

    if (!aUsed.contains (PLACEHOLDER_INCOMING_ID) && !aUsed.contains (PLACEHOLDER_SBDH_INSTANCE_ID))
      LOGGER.warn ("The forwarding filename pattern in '" +
                   sConfigKey +
                   "' contains neither the '{" +
                   PLACEHOLDER_INCOMING_ID +
                   "}' nor the '{" +
                   PLACEHOLDER_SBDH_INSTANCE_ID +
                   "}' placeholder, so two documents may resolve to the same name and overwrite each other");

    return ESuccess.SUCCESS;
  }

  /**
   * Read a pattern that results in a filename from the configuration. Path separators are not
   * allowed and the resolved name is limited to {@link #MAX_LENGTH_FILENAME} characters.
   *
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @param sConfigKey
   *        The configuration key to read. May neither be <code>null</code> nor empty.
   * @param sDefaultPattern
   *        The pattern to be used if the configuration key is not set. May neither be
   *        <code>null</code> nor empty.
   * @return <code>null</code> if the configured pattern is unusable - in that case the reason was
   *         logged as an error.
   */
  @Nullable
  public static ForwardingFilenamePattern createFilenameFromConfig (@NonNull final IConfig aConfig,
                                                                    @NonNull @Nonempty final String sConfigKey,
                                                                    @NonNull @Nonempty final String sDefaultPattern)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigKey, "ConfigKey");
    ValueEnforcer.notEmpty (sDefaultPattern, "DefaultPattern");

    final String sPattern = aConfig.getAsString (sConfigKey, sDefaultPattern);
    if (checkPattern (sConfigKey, sPattern, false).isFailure ())
      return null;
    return new ForwardingFilenamePattern (sPattern, false, MAX_LENGTH_FILENAME);
  }

  /**
   * Read a pattern that results in the object key of a blob store from the configuration. Contrary
   * to a filename, the literal part may contain path separators - so that the key can span
   * "directories" - and the resolved key is limited to {@link #MAX_LENGTH_OBJECT_KEY} characters.
   *
   * @param aConfig
   *        The configuration to read from. May not be <code>null</code>.
   * @param sConfigKey
   *        The configuration key to read. May neither be <code>null</code> nor empty.
   * @param sDefaultPattern
   *        The pattern to be used if the configuration key is not set. May neither be
   *        <code>null</code> nor empty.
   * @return <code>null</code> if the configured pattern is unusable - in that case the reason was
   *         logged as an error.
   */
  @Nullable
  public static ForwardingFilenamePattern createObjectKeyFromConfig (@NonNull final IConfig aConfig,
                                                                     @NonNull @Nonempty final String sConfigKey,
                                                                     @NonNull @Nonempty final String sDefaultPattern)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notEmpty (sConfigKey, "ConfigKey");
    ValueEnforcer.notEmpty (sDefaultPattern, "DefaultPattern");

    final String sPattern = aConfig.getAsString (sConfigKey, sDefaultPattern);
    if (checkPattern (sConfigKey, sPattern, true).isFailure ())
      return null;
    return new ForwardingFilenamePattern (sPattern, true, MAX_LENGTH_OBJECT_KEY);
  }

  /**
   * @return The pattern as it was configured. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getPattern ()
  {
    return m_sPattern;
  }

  /**
   * @return <code>true</code> if the literal part of the pattern may contain path separators.
   */
  public final boolean isAllowPathSeparator ()
  {
    return m_bAllowPathSeparator;
  }

  /**
   * @return The maximum length of a resolved name. Always &gt; 0.
   */
  @Nonnegative
  public final int getMaxLength ()
  {
    return m_nMaxLength;
  }

  /**
   * Resolve the pattern for a single document, resulting in the base name of the stored document -
   * so without the ".xml" of the document and without the ".json" of an optional metadata sidecar.
   *
   * @param aDocument
   *        The document to be forwarded. May not be <code>null</code>.
   * @return The resolved base name. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getResolvedBaseName (@NonNull final IForwardableDocument aDocument)
  {
    ValueEnforcer.notNull (aDocument, "Document");

    // An unknown placeholder is rejected by checkPattern already
    final ICommonsMap <String, String> aValues = _getPlaceholderValues (aDocument);
    final String sResolved = _resolve (m_sPattern,
                                       x -> _getSanitized (aValues.get (x), false),
                                       x -> _getSanitized (x, m_bAllowPathSeparator));
    // The leading dots are removed, so that no value can turn the result into a hidden file that a
    // downstream poller never picks up
    String sBaseName = StringHelper.trimStartRepeatedly (sResolved, '.');

    if (sBaseName.length () > m_nMaxLength)
    {
      LOGGER.warn ("The resolved forwarding filename of transaction '" +
                   aDocument.id () +
                   "' is " +
                   sBaseName.length () +
                   " characters long and is therefore truncated to " +
                   m_nMaxLength +
                   " characters");
      sBaseName = sBaseName.substring (0, m_nMaxLength);
    }

    // Takes care of the Windows reserved filenames and of the illegal suffixes
    final String sSecureBaseName = FilenameHelper.getAsSecureValidASCIIFilename (sBaseName);
    if (StringHelper.isEmpty (sSecureBaseName))
    {
      // E.g. a pattern of "{sbdh-instance-id}" and an Instance Identifier consisting of dots only
      LOGGER.warn ("The forwarding filename pattern resolved to an unusable name for transaction '" +
                   aDocument.id () +
                   "' - using the default layout instead");
      return DATETIME_FORMATTER.format (aDocument.timestamp ()) + "_" + _getSanitized (aDocument.localID (), false);
    }

    return sSecureBaseName;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Pattern", m_sPattern)
                                       .append ("AllowPathSeparator", m_bAllowPathSeparator)
                                       .append ("MaxLength", m_nMaxLength)
                                       .getToString ();
  }
}
