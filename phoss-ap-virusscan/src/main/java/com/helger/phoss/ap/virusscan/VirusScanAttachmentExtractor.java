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
package com.helger.phoss.ap.virusscan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;

/**
 * Utility to extract embedded binary attachments from XML documents using StAX streaming and Base64
 * decoding.
 *
 * @author Philip Helger
 */
@Immutable
public final class VirusScanAttachmentExtractor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (VirusScanAttachmentExtractor.class);

  @Immutable
  public static final class AttachmentScanResult
  {
    private final int m_nAttachmentCount;
    private final IcapScanResult m_aScanResult;

    public AttachmentScanResult (final int nAttachmentCount, @NonNull final IcapScanResult aScanResult)
    {
      m_nAttachmentCount = nAttachmentCount;
      m_aScanResult = ValueEnforcer.notNull (aScanResult, "ScanResult");
    }

    public int getAttachmentCount ()
    {
      return m_nAttachmentCount;
    }

    public boolean hasAttachments ()
    {
      return m_nAttachmentCount > 0;
    }

    @NonNull
    public IcapScanResult getScanResult ()
    {
      return m_aScanResult;
    }
  }

  private VirusScanAttachmentExtractor ()
  {}

  private static boolean _isAttachmentElement (@NonNull final String sLocalName)
  {
    return "EmbeddedDocumentBinaryObject".equalsIgnoreCase (sLocalName) ||
           "AttachmentBinaryObject".equalsIgnoreCase (sLocalName) ||
           "BinaryContent".equalsIgnoreCase (sLocalName);
  }

  /**
   * Scan embedded binary attachments inside an XML input stream.
   *
   * @param aXmlIS
   *        The XML input stream. May not be <code>null</code>.
   * @param aScanner
   *        Function that accepts a decoded raw binary attachment input stream and returns an
   *        {@link IcapScanResult}. May not be <code>null</code>.
   * @return The {@link AttachmentScanResult} containing count and scan result. Never <code>null</code>.
   */
  @NonNull
  public static AttachmentScanResult scanEmbeddedAttachments (@NonNull final InputStream aXmlIS,
                                                              @NonNull final Function <InputStream, IcapScanResult> aScanner)
  {
    ValueEnforcer.notNull (aXmlIS, "XmlIS");
    ValueEnforcer.notNull (aScanner, "Scanner");

    int nAttachmentCount = 0;
    try
    {
      final XMLInputFactory aFactory = XMLInputFactory.newInstance ();
      aFactory.setProperty (XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
      aFactory.setProperty (XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

      final XMLStreamReader aReader = aFactory.createXMLStreamReader (aXmlIS);
      try
      {
        while (aReader.hasNext ())
        {
          final int nEvent = aReader.next ();
          if (nEvent == XMLStreamConstants.START_ELEMENT)
          {
            final String sLocalName = aReader.getLocalName ();
            if (_isAttachmentElement (sLocalName))
            {
              nAttachmentCount++;
              final String sBase64Content = aReader.getElementText ();
              if (sBase64Content != null && !sBase64Content.isBlank ())
              {
                final byte [] aBase64Bytes = sBase64Content.trim ().getBytes (StandardCharsets.UTF_8);
                try (final InputStream aBase64IS = new ByteArrayInputStream (aBase64Bytes);
                     final InputStream aDecodedBinaryIS = Base64.getDecoder ().wrap (aBase64IS))
                {
                  final IcapScanResult aResult = aScanner.apply (aDecodedBinaryIS);
                  if (!aResult.isPassed ())
                  {
                    LOGGER.warn ("Embedded attachment #" + nAttachmentCount + " failed scan: " + aResult);
                    return new AttachmentScanResult (nAttachmentCount, aResult);
                  }
                }
              }
            }
          }
        }
      }
      finally
      {
        aReader.close ();
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error extracting embedded attachments for virus scan: " + ex.getMessage (), ex);
    }

    return new AttachmentScanResult (nAttachmentCount, IcapScanResult.passed ());
  }
}
