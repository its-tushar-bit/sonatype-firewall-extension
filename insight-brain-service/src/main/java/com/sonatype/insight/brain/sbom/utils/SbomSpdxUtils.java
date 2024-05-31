/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.thirdparty.SbomIdentityUtils;
import com.sonatype.insight.scan.file.InvalidSbomException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Swid;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.Read;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

public final class SbomSpdxUtils
{
  private static final Gson gson = new GsonBuilder().create();

  public static final Pattern personSpdxPattern = Pattern.compile("Person: (.+)");

  public static final Pattern personEmailSpdxPattern =
      Pattern.compile("Person: .+ (\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]*)?\\))?");

  public static final Pattern organizationSpdxPattern =
      Pattern.compile("Organization: ([\\w\\u00C0-\\u024F\\-' ]+) ?(\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]+)?\\))?");

  public static final Pattern toolSpdxPattern =
      Pattern.compile("Tool: ([\\w'. ]+)-([\\w_\\-'. ]+)");

  private static final Pattern CVE_LINK_PATTERN =
      Pattern.compile("https?://cve.mitre.org/cgi-bin/cvename.cgi\\?name=([^=]+)");

  public static final Pattern NVD_LINK_PATTERN = Pattern.compile("https?://nvd.nist.gov/vuln/detail/([^/]+)");

  public static final Pattern OSV_LINK_PATTERN = Pattern.compile("https?://osv.dev/vulnerability/([^/]+)");

  public static final Pattern SONATYPE_LINK_PATTERN = Pattern.compile("https?://.+/vln/(sonatype-[0-9-]+)");

  private static final Pattern DEPRECATION_PATTERN =
      Pattern.compile(".*Relationship error: [^\\s]+ is deprecated\\..*");

  private static final String SWID_URI_PREFIX = "swid:";

  private SbomSpdxUtils() {
    //no-op
  }

  public static SpdxDocument parseContentNoValidation(String content, SbomFormat sbomFormat) {
    return parseContentStreamNoValidation(IOUtils.toInputStream(content, StandardCharsets.UTF_8), sbomFormat);
  }

  public static SpdxDocument parseContentStreamNoValidation(InputStream is, SbomFormat sbomFormat) {
    Format format = sbomFormat == SbomFormat.JSON ? Format.JSON : Format.XML;
    DefaultModelStore.reset();
    IModelStore baseStore = new InMemSpdxStore();

    try (MultiFormatStore multiFormatStore = new MultiFormatStore(baseStore, format, Verbose.COMPACT)) {
      return new SpdxDocument(multiFormatStore, multiFormatStore.deSerialize(is, true),
          DefaultModelStore.getDefaultCopyManager(), true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void validateDocument(SbomFormat format, SpdxDocument spdxDocument)
      throws UnsupportedSbomException
  {
    if (format != null) {
      try {
        String specVersion = spdxDocument.getSpecVersion();
        if (StringUtils.isBlank(specVersion)) {
          throw new UnsupportedSbomException("SPDX version is not specified");
        }
        if (!ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.containsKey(specVersion)) {
          throw new UnsupportedSbomException("SPDX " + specVersion.replace("SPDX-", "") + " version is not supported");
        }
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new UnsupportedSbomException("SPDX version is not specified");
      }
      validateSpdx(spdxDocument);
    }
    else {
      throw new UnsupportedSbomException("Missing SPDX encoding type");
    }
  }

  private static void validateSpdx(SpdxDocument spdxDocument) {
    List<String> verificationErrors = spdxDocument.verify().stream()
        .filter(s -> !DEPRECATION_PATTERN.matcher(s).matches())
        .collect(Collectors.toList());

    if (!verificationErrors.isEmpty()) {
      InvalidSbomException invalidSbomException = new InvalidSbomException("The spdx document is not valid.");
      // the "Relationship error: " prefix is added sometimes multiple times and doesn't bring any value in itself
      verificationErrors.forEach(ve -> invalidSbomException.addSuppressed(
          new InvalidSPDXAnalysisException(ve.replace("Relationship error: ", ""))));
      throw invalidSbomException;
    }
  }

  public static SbomFormat determineSbomFormat(SpdxDocument document) {
    Format format = ((MultiFormatStore) document.getModelStore()).getFormat();
    if (Format.JSON.equals(format) || Format.JSON_PRETTY.equals(format)) {
      return SbomFormat.JSON;
    }
    else if (Format.XML.equals(format)) {
      return SbomFormat.XML;
    }
    return null;
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

  public static String getPurl(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    //try resolve from external refs first
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.PACKAGE_MANAGER &&
          externalRef.getReferenceType().getIndividualURI().endsWith("/purl")) {
        if (StringUtils.isNotBlank(externalRef.getReferenceLocator())) {
          return externalRef.getReferenceLocator();
        }
      }
    }
    //fallback to cpe
    String cpe = getCpe(spdxPackage);
    if (cpe != null) {
      return SbomIdentityUtils.buildPackageUrlFromCpe(cpe).getPackageUrl();
    }

    return null;
  }

  public static String getCpe(final SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        if (referenceType.endsWith("cpe23Type") || referenceType.endsWith("cpe22Type") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                externalRef.getReferenceLocator().startsWith("cpe"))) {
          return externalRef.getReferenceLocator();
        }
      }
    }
    return null;
  }

  public static Map<String, ExternalRef> getVulnerabilitiesForPackage(final SpdxPackage pkg)
      throws InvalidSPDXAnalysisException
  {
    Map<String, ExternalRef> vulnerabilityMap = new HashMap<>();
    for (ExternalRef externalRef : pkg.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String refId = getRefIdForVulnerability(externalRef);
        if (refId != null) {
          vulnerabilityMap.put(refId, externalRef);
        }
      }
    }
    return vulnerabilityMap;
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

  public static String getRefIdForVulnerability(final ExternalRef externalRef) throws InvalidSPDXAnalysisException {
    String link = externalRef.getReferenceLocator();
    if (StringUtils.isEmpty(link) && StringUtils.isBlank(link)) {
      return null;
    }
    Matcher matcher = CVE_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    matcher = NVD_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    matcher = OSV_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    matcher = SONATYPE_LINK_PATTERN.matcher(link);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return link;
  }

  public static SbomCreationDetails getSbomCreationDetails(SpdxDocument document) throws InvalidSPDXAnalysisException {
    if (document != null) {
      SbomCreationDetails sbomCreationDetails = new SbomCreationDetails();
      sbomCreationDetails.created = document.getCreationInfo() != null ? document.getCreationInfo().getCreated() : null;
      sbomCreationDetails.type = getRootPackage(document).getPrimaryPurpose().map(Enum::name).orElse(null);
      if (document.getCreationInfo() != null) {
        document.getCreationInfo().getCreators().forEach(it -> classifyAndExtractMetadata(sbomCreationDetails, it));
      }
      return sbomCreationDetails;
    }
    return null;
  }

  public static String getSbomCreationDetailsJson(SpdxDocument document) throws InvalidSPDXAnalysisException {
    SbomCreationDetails sbomCreationDetails = getSbomCreationDetails(document);
    return sbomCreationDetails != null ? gson.toJson(sbomCreationDetails) : null;
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

  public static Optional<Swid> getSwid(final SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        String referenceLocator = externalRef.getReferenceLocator();
        if (referenceType.endsWith("swid") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                referenceLocator.startsWith(SWID_URI_PREFIX))) {
          Swid swid = new Swid();
          String tagId = referenceLocator.startsWith(SWID_URI_PREFIX) ? referenceLocator.substring(
              SWID_URI_PREFIX.length()) : referenceLocator;
          swid.setTagId(tagId);
          return Optional.of(swid);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<String> getChecksum(final SpdxPackage spdxPackage, ChecksumAlgorithm algorithm)
      throws InvalidSPDXAnalysisException
  {
    final Collection<Checksum> checksums = spdxPackage.getChecksums();
    for (Checksum checksum : checksums) {
      if (checksum.getAlgorithm() == algorithm) {
        return Optional.of(checksum.getValue());
      }
    }
    return Optional.empty();
  }

  public static String getPackageUrlFromCoordinates(String name, String version) {
    String group = null;
    if (name.contains(":")) {
      final String[] parts = name.split(":");
      if (parts.length == 2) {
        group = parts[0];
        name = parts[1];
      }
    }
    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(PackageURL.StandardTypes.GENERIC)
        .withName(name)
        .withVersion(version);
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    try {
      return packageURLBuilder.build().toString();
    }
    catch (MalformedPackageURLException e) {
      return null;
    }
  }

  public static List<Relationship> getDependenciesBySpdxPackage(SpdxPackage spdxPackage)
      throws InvalidSPDXAnalysisException
  {
    return spdxPackage.getRelationships().stream().filter(it -> {
      try {
        return it.getRelationshipType().equals(RelationshipType.DEPENDS_ON);
      }
      catch (InvalidSPDXAnalysisException e) {
        return false;
      }
    }).collect(Collectors.toList());
  }
}
