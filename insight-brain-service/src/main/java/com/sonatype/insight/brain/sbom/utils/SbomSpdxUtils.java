/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
  private static final Gson gson =  new GsonBuilder().create();

  public static final Pattern personSpdxPattern = Pattern.compile("Person: (.+)");

  public static final Pattern personEmailSpdxPattern =
      Pattern.compile("Person: .+ (\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]*)?\\))?");

  public static final Pattern organizationSpdxPattern =
      Pattern.compile("Organization: ([\\w\\u00C0-\\u024F\\-' ]+) ?(\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]+)?\\))?");

  public static final Pattern toolSpdxPattern =
      Pattern.compile("Tool: ([\\w'. ]+)-([\\w_\\-'. ]+)");

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

  public static String getSbomCreationDetailsJson(SpdxDocument document) throws InvalidSPDXAnalysisException {
    if (document != null) {
      SbomCreationDetails sbomCreationDetails = new SbomCreationDetails();
      sbomCreationDetails.created = document.getCreationInfo() != null ? document.getCreationInfo().getCreated() : null;
      sbomCreationDetails.type = getRootPackage(document).getPrimaryPurpose().map(Enum::name).orElse(null);
      if (document.getCreationInfo() != null) {
        document.getCreationInfo().getCreators().forEach(it -> classifyAndExtractMetadata(sbomCreationDetails, it));
      }
      return gson.toJson(sbomCreationDetails);
    }
    return null;
  }

  private static void classifyAndExtractMetadata(SbomCreationDetails sbomCreationDetails, String unfilteredData) {
    Matcher personMatcher = personSpdxPattern.matcher(unfilteredData);
    Matcher organizationMatcher = organizationSpdxPattern.matcher(unfilteredData);
    Matcher toolMatcher = toolSpdxPattern.matcher(unfilteredData);
    if (personMatcher.matches()) {
      Matcher personEmailMatcher = personEmailSpdxPattern.matcher(unfilteredData);
      SbomCreationDetails.Creator creator = new SbomCreationDetails.Creator();
      creator.type = SbomCreationDetails.CreatorType.Person.name();
      if (personEmailMatcher.matches()) {
        if (personEmailMatcher.start(2) > 0 && !personEmailMatcher.group(2).isEmpty()) {
          creator.email = personEmailMatcher.group(2);
        }
        int nameStartIndex = personMatcher.start(1);
        int nameEndIndex = personEmailMatcher.start(1);
        creator.name = unfilteredData.substring(nameStartIndex, nameEndIndex).trim();
      }
      else {
        creator.name = personMatcher.group(1).trim();
      }
      if (sbomCreationDetails.creators == null) {
        sbomCreationDetails.creators = new ArrayList<>();
      }
      sbomCreationDetails.creators.add(creator);
    }
    else if (organizationMatcher.matches()) {
      SbomCreationDetails.Creator creator = new SbomCreationDetails.Creator();
      creator.type = SbomCreationDetails.CreatorType.Organization.name();
      creator.name = organizationMatcher.group(1).trim();
      if (emailAvailable(organizationMatcher, unfilteredData.length())) {
        creator.email = organizationMatcher.group(3).trim();
      }
      if (sbomCreationDetails.creators == null) {
        sbomCreationDetails.creators = new ArrayList<>();
      }
      sbomCreationDetails.creators.add(creator);
    }
    else if (toolMatcher.matches()) {
      SbomCreationDetails.Tool tool = new SbomCreationDetails.Tool();
      tool.name = toolMatcher.group(1).trim();
      tool.version = toolMatcher.group(2).trim();
      if (sbomCreationDetails.tools == null) {
        sbomCreationDetails.tools = new ArrayList<>();
      }
      sbomCreationDetails.tools.add(tool);
    }
  }

  private static boolean emailAvailable(Matcher matcher, int length) {
    return matcher.end(1) != length && matcher.end(1) != length - 2;
  }

  public static String getOrGenerateSpdxSerialNumber(SpdxDocument spdxDocument) {
    return spdxDocument.getDocumentUri() == null || spdxDocument.getDocumentUri().trim().isEmpty()
        ? String.format("sonatype/spdxdocs/uuid/%s", UUID.randomUUID())
        : spdxDocument.getDocumentUri();
  }
}
