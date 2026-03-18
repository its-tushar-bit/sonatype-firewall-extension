/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomValidationException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;
import com.sonatype.insight.scan.file.ValidationException;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.Read;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import us.springett.parsers.cpe.util.Validate;

public final class SbomSpdxUtils
{
  private static final Logger log = LoggerFactory.getLogger(SbomSpdxUtils.class);

  private static final Gson gson = new GsonBuilder().create();

  public static final Pattern personSpdxPattern = Pattern.compile("Person: (.+)");

  public static final Pattern personEmailSpdxPattern =
      Pattern.compile("Person: .+ (\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]*)?\\))?");

  public static final Pattern organizationSpdxPattern =
      Pattern.compile("Organization: ([\\w\\u00C0-\\u024F\\-' ]+) ?(\\(([\\w@!.\\-#$%&'*+-/=?^`{|}~]+)?\\))?");

  public static final Pattern toolSpdxPattern =
      Pattern.compile("Tool: ([\\w'. ]+)-([\\w_\\-'. ]+)");

  private static final Pattern URL_REF_ID_PATTERN = Pattern.compile("([A-Za-z0-9]+(-[A-Za-z0-9]+)+(:[A-Za-z0-9]+)?)");

  private static final Set<String> SOURCE_NVD_DOMAINS = ImmutableSet.of("cve.mitre.org", "nvd.nist.gov", "cve.org");

  private static final String SOURCE_OSV_DOMAIN = "osv.dev";

  private static final String SOURCE_SONATYPE = "sonatype";

  private static final Pattern DEPRECATION_PATTERN =
      Pattern.compile(".*Relationship error: [^\\s]+ is deprecated\\..*");

  private static final String SWID_URI_PREFIX = "swid:";

  private SbomSpdxUtils() {
    // no-op
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

  public static void validateDocument(
      SbomFormat format,
      SpdxDocument spdxDocument) throws UnsupportedSbomException, SbomValidationException
  {
    if (format != null) {
      try {
        String specVersion = spdxDocument.getSpecVersion();
        if (StringUtils.isBlank(specVersion)) {
          throw new UnsupportedSbomException("SPDX version is not specified.");
        }
        if (!ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.containsKey(specVersion)) {
          throw new UnsupportedSbomException("SPDX " + specVersion.replace("SPDX-", "") + " version is not supported.");
        }
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new UnsupportedSbomException("SPDX version is not specified.");
      }
      validateSpdx(spdxDocument);
    }
    else {
      throw new UnsupportedSbomException("Missing SPDX encoding type.");
    }
  }

  private static void validateSpdx(SpdxDocument spdxDocument) throws SbomValidationException {
    List<String> verificationErrors = spdxDocument.verify()
        .stream()
        .filter(s -> !DEPRECATION_PATTERN.matcher(s).matches())
        .toList();

    if (!verificationErrors.isEmpty()) {
      SbomValidationException sbomValidationException = new SbomValidationException("The spdx document is not valid.");
      // the "Relationship error: " prefix is added sometimes multiple times and doesn't bring any value in itself
      verificationErrors.forEach(ve -> sbomValidationException.addSuppressed(
          new ValidationException(null, null, null, ve.replace("Relationship error: ", ""), null)));
      throw sbomValidationException;
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

  public static SpdxPackage getRootPackage(SpdxDocument document) throws InvalidSPDXAnalysisException {
    if (document != null) {
      Collection<SpdxElement> describes = document.getDocumentDescribes();
      if (!describes.isEmpty()) {
        Optional<SpdxPackage> documentDescribesPackage = describes.stream()
            .filter(Objects::nonNull)
            .filter(spdxElement -> spdxElement instanceof SpdxPackage)
            .map(spdxElement -> (SpdxPackage) spdxElement)
            .findFirst();
        if (documentDescribesPackage.isPresent()) {
          return documentDescribesPackage.get();
        }
      }
      List<SpdxPackage> spdxPackages = getAllPackages(document);
      if (spdxPackages.size() == 1) {
        return spdxPackages.get(0);
      }
    }
    return null;
  }

  public static List<SpdxPackage> getAllPackages(SpdxDocument document) throws InvalidSPDXAnalysisException {
    if (document != null) {
      return Read.getAllPackages(document.getModelStore(), document.getDocumentUri()).collect(Collectors.toList());
    }
    return null;
  }

  public static SpdxPackage getPackageById(
      SpdxDocument document,
      String packageId) throws InvalidSPDXAnalysisException
  {
    if (document != null) {
      return Read.getAllPackages(document.getModelStore(), document.getDocumentUri())
          .filter(it -> it.getId()
              .equals(packageId))
          .findAny()
          .orElse(null);
    }
    return null;
  }

  public static String getPurl(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    // try resolve from external refs first
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.PACKAGE_MANAGER &&
          externalRef.getReferenceType().getIndividualURI().endsWith("/purl"))
      {
        if (StringUtils.isNotBlank(externalRef.getReferenceLocator())) {
          return externalRef.getReferenceLocator();
        }
      }
    }
    // fallback to cpe
    String cpe = getCpe(spdxPackage);
    PackageUrlIdentifier packageUrlIdentifier = SbomCommonUtils.getPackageUrlIdentifierFromCpe(cpe);
    if (packageUrlIdentifier != null) {
      return packageUrlIdentifier.getPackageUrl();
    }
    return null;
  }

  public static String getCpe(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        if (referenceType.endsWith("cpe23Type") || referenceType.endsWith("cpe22Type") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                externalRef.getReferenceLocator().startsWith("cpe")))
        {
          if (Validate.cpe(externalRef.getReferenceLocator()).isValid()) {
            return externalRef.getReferenceLocator();
          }
          else {
            log.debug("Invalid CPE: {}", externalRef.getReferenceLocator());
          }
        }
      }
    }
    return null;
  }

  public static Map<String, ExternalRef> getVulnerabilitiesForPackage(
      final SpdxPackage pkg) throws InvalidSPDXAnalysisException
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

  public static List<ExternalRef> getAllVulnerabilities(SpdxDocument document) throws InvalidSPDXAnalysisException {
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

  public static Pair<String, String> getRefIdAndSourceForVulnerability(
      final ExternalRef externalRef) throws InvalidSPDXAnalysisException
  {
    String link = externalRef.getReferenceLocator();
    if (StringUtils.isBlank(link)) {
      return null;
    }
    return getRefIdAndSourceForVulnerability(link);
  }

  public static String getRefIdForVulnerability(final ExternalRef externalRef) throws InvalidSPDXAnalysisException {
    Pair<String, String> refIdAndSource = getRefIdAndSourceForVulnerability(externalRef);
    return refIdAndSource != null ? refIdAndSource.getKey() : null;
  }

  @VisibleForTesting
  static Pair<String, String> getRefIdAndSourceForVulnerability(final String link) {
    if (!isValidURL(link)) {
      return null;
    }

    String copyOfLink = link;
    // Remove any trailing /
    if (link.endsWith("/")) {
      copyOfLink = copyOfLink.substring(0, link.lastIndexOf("/"));
    }
    // Now work with the last part of the url, after the last /,? or =
    int questionMarkIdx = copyOfLink.lastIndexOf("?");
    int equalsIdx = copyOfLink.lastIndexOf("=");
    int backSlashIdx = copyOfLink.lastIndexOf("/");
    int hashIdx = copyOfLink.lastIndexOf("#");
    Integer maxLastSpecialCharIdx =
        Stream.of(questionMarkIdx, equalsIdx, backSlashIdx, hashIdx).max(Integer::compareTo).orElse(null);
    copyOfLink = copyOfLink.substring(maxLastSpecialCharIdx + 1);

    Matcher matcher = URL_REF_ID_PATTERN.matcher(copyOfLink);
    if (matcher.find()) {
      String refId = matcher.group(1);
      if (StringUtils.isNotBlank(refId)) {
        return Pair.of(refId, getSourceForUrl(link, refId));
      }
    }
    return null;
  }

  private static String getSourceForUrl(final String url, final String refId) {
    if (SOURCE_NVD_DOMAINS.stream().anyMatch(domain -> StringUtils.containsIgnoreCase(url, domain))) {
      return "NVD";
    }
    if (StringUtils.containsIgnoreCase(url, SOURCE_OSV_DOMAIN)) {
      return "OSV";
    }
    if (StringUtils.containsIgnoreCase(refId, SOURCE_SONATYPE)) {
      return "SONATYPE";
    }
    return "OTHER";
  }

  private static boolean isValidURL(String url) {
    try {
      new URL(url).toURI();
      return true;
    }
    catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
  }

  public static SbomCreationDetails getSbomCreationDetails(SpdxDocument document) throws InvalidSPDXAnalysisException {
    if (document != null) {
      SbomCreationDetails sbomCreationDetails = new SbomCreationDetails();
      sbomCreationDetails.created = document.getCreationInfo() != null ? document.getCreationInfo().getCreated() : null;
      SpdxPackage spdxPackage = getRootPackage(document);

      if (spdxPackage != null) {
        sbomCreationDetails.type = spdxPackage.getPrimaryPurpose().map(Enum::name).orElse(null);
      }

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

  public static Optional<Swid> getSwid(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        String referenceType = externalRef.getReferenceType().getIndividualURI();
        String referenceLocator = externalRef.getReferenceLocator();
        if (referenceType.endsWith("swid") ||
            (referenceType.equals(ReferenceType.MISSING_REFERENCE_TYPE_URI) &&
                referenceLocator.startsWith(SWID_URI_PREFIX)))
        {
          Swid swid = new Swid();
          String tagId = referenceLocator.startsWith(SWID_URI_PREFIX)
              ? referenceLocator.substring(
                  SWID_URI_PREFIX.length())
              : referenceLocator;
          swid.setTagId(tagId);
          return Optional.of(swid);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<String> getChecksum(
      final SpdxPackage spdxPackage,
      ChecksumAlgorithm algorithm) throws InvalidSPDXAnalysisException
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

  public static boolean looksLikeSpdxDocument(String spdxDocument) {
    if (StringUtils.isEmpty(spdxDocument)) {
      return false;
    }
    return spdxDocument.contains("SPDXRef-DOCUMENT") &&
        (spdxDocument.contains("<SPDXID>") || spdxDocument.contains("\"SPDXID\""));
  }

  public static String getSpdxCpeVersion(String cpe) {
    return cpe.startsWith("cpe:2.3") ? "cpe23Type" : "cpe22Type";
  }
}
