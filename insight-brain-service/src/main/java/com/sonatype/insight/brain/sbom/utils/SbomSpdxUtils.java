/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.Read;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ReferenceCategory;

public final class SbomSpdxUtils
{
  private SbomSpdxUtils() {
    //no-op
  }

  public static SpdxPackage getRootPackage(SpdxDocument document)
      throws InvalidSPDXAnalysisException
  {
    if (document != null) {
      Collection<SpdxElement> describes = document.getDocumentDescribes();
      if (!describes.isEmpty()) {
        final SpdxElement rootElement = describes.iterator().next();
        if (rootElement instanceof SpdxPackage) {
          return (SpdxPackage) rootElement;
        }
      }
    }
    return null;
  }

  public static List<SpdxPackage> getAllPackages(SpdxDocument document)
      throws InvalidSPDXAnalysisException
  {
    if (document != null) {
      return Read.getAllItems(document.getModelStore(), document.getDocumentUri(), SpdxConstants.CLASS_SPDX_PACKAGE)
          .map(modelObject -> (SpdxPackage) modelObject).collect(Collectors.toList());
    }
    return null;
  }

  public static List<ExternalRef> getAllVulnerabilities(SpdxDocument document)
      throws InvalidSPDXAnalysisException
  {
    if (document != null) {
      List<ExternalRef> vulnerabilities = new ArrayList<>();
      for (SpdxPackage spdxPackage : getAllPackages(document)) {
        for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
          if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
            vulnerabilities.add(externalRef);
          }
        }
      }
      return vulnerabilities;
    }
    return null;
  }

  public static String getOrGenerateSpdxSerialNumber(SpdxDocument spdxDocument) {
    return spdxDocument.getDocumentUri() == null || spdxDocument.getDocumentUri().trim().isEmpty()
        ? String.format("sonatype/spdxdocs/uuid/%s", UUID.randomUUID())
        : spdxDocument.getDocumentUri();
  }
}
