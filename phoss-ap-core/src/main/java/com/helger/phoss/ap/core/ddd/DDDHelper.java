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
package com.helger.phoss.ap.core.ddd;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.ddd.DDDVersion;
import com.helger.ddd.DocumentDetails;
import com.helger.ddd.DocumentDetailsDeterminator;
import com.helger.ddd.model.DDDSyntaxList;
import com.helger.ddd.model.DDDValueProviderList;
import com.helger.phoss.ap.basic.APBasicMetaManager;

/**
 * Thin wrapper around the DDD (Document Details Determinator) library. Provides a static method to
 * determine Peppol identifiers (document type, process, sender, receiver) from a raw XML document.
 *
 * @author Philip Helger
 * @since v0.1.4
 */
@Immutable
public final class DDDHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (DDDHelper.class);
  private static final DocumentDetailsDeterminator s_aDDD;

  static
  {
    s_aDDD = new DocumentDetailsDeterminator (DDDSyntaxList.getDefaultSyntaxList (),
                                              DDDValueProviderList.getDefaultValueProviderList ());
    s_aDDD.setIdentifierFactory (APBasicMetaManager.getIdentifierFactory ());
    LOGGER.info ("DDD (Document Details Determinator) " + DDDVersion.getVersionNumber () + "initialized");
  }

  private DDDHelper ()
  {}

  /**
   * Determine document details from a raw XML element. This performs syntax detection and value
   * extraction using the DDD library.
   *
   * @param aRootElement
   *        The root element of the XML document. May not be <code>null</code>.
   * @return The determined {@link DocumentDetails}, or <code>null</code> if the document type could
   *         not be determined.
   */
  @Nullable
  public static DocumentDetails findDocumentDetails (@NonNull final Element aRootElement)
  {
    return s_aDDD.findDocumentDetails (aRootElement);
  }
}
