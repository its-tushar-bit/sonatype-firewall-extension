/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.SpdxPackage.SpdxPackageBuilder;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.ListedLicenses;
import org.spdx.library.model.license.SpdxListedLicense;
import org.spdx.library.model.license.SpdxNoAssertionLicense;

import static com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2.buildFakeParentPackageUrl;

@Named
@Singleton
public class ApiSpdxService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSpdxService.class);

  static final Set<String> SPDX_FORMATS = ImmutableSet.of("json", "xml");

  static final String SPDX_REF_PREFIX = "SPDXRef-";

  static final String LICENSE_REF_PREFIX = "LicenseRef-";

  static final String NVD = "NVD";

  static final String CVE = "cve";

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ApplicationHelper applicationHelper;

  private final BaseUrl baseUrl;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final VersionService versionService;

  private final ApiCycloneDxServiceV2 apiCycloneDxService;

  @Inject
  public ApiSpdxService(
      ApiReportDataServiceV2 apiReportDataServiceV2,
      ApiCycloneDxServiceV2 apiCycloneDxService,
      ApplicationHelper applicationHelper,
      BaseUrl baseUrl,
      PolicyEvaluationDAO policyEvaluationDAO,
      MultiLicenseDAO multiLicenseDAO,
      VersionService versionService)
  {
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.apiCycloneDxService = apiCycloneDxService;
    this.applicationHelper = applicationHelper;
    this.baseUrl = baseUrl;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.versionService = versionService;
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String scanId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    ThirdPartyUtils.validateSpdxVersion(spdxVersion);

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return getByScanId(application, scanId, validateFormat(format), generateCycloneDx, spdxVersion);
  }

  @Authorize(permission = Permission.READ)
  public Response getLatestForStage(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String stageId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    ThirdPartyUtils.validateSpdxVersion(spdxVersion);

    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a policy evaluation for " + applicationId + " in stage " + stageId);
    }

    return getByScanId(application, evaluation.getScanId(), validateFormat(format), generateCycloneDx, spdxVersion);
  }

  private Response getByScanId(
      Application application,
      String scanId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    AuditData.get().setScanId(scanId);

    try {
      ApiReportRawDataDTOV2 data = apiReportDataServiceV2.getDataNoAuth(application.getPublicId(), scanId);

      String uri = getReportUrl(application.getPublicId(), scanId);

      final SpdxDocument document = createDocument(spdxVersion, uri, data);
      final Map<String, SpdxPackage> purlElementMap = new HashMap<>();

      addPackages(data.components, document, purlElementMap, spdxVersion);

      ApiDependencyTreeNodeDTO rootNodeDTO =
          apiReportDataServiceV2.getDependencyTreeNoAuth(application.getPublicId(), scanId);
      if (rootNodeDTO != null) {
        addRootPackage(rootNodeDTO, document, purlElementMap, application.getName(), scanId, spdxVersion);
        addDependencyRelationships(rootNodeDTO, document, purlElementMap, true);
      }

      return generateResponse(document, application, scanId, format, generateCycloneDx);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException("An error occurred while generating the SPDX file", e);
    }
  }

  private void addRootPackage(
      final ApiDependencyTreeNodeDTO rootNodeDTO,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap,
      final String applicationName,
      final String scanId,
      final String spdxVersion) throws InvalidSPDXAnalysisException
  {
    String packageUrl = rootNodeDTO.getPackageUrl();
    if (StringUtils.isBlank(packageUrl)) {
      packageUrl = buildFakeParentPackageUrl(rootNodeDTO, applicationName, scanId);
    }
    if (packageUrl != null) {
      if (!purlElementMap.containsKey(packageUrl)) {
        String version = getSpdxVersionFromPurl(packageUrl);
        SpdxNoAssertionLicense noAssertionLicense = new SpdxNoAssertionLicense();
        addPackage(packageUrl, version, null, noAssertionLicense, noAssertionLicense, Collections.emptyList(),
            document, purlElementMap, spdxVersion);
      }
      document.setName(packageUrl);
    }
  }

  // visible for testing
  void addDependencyRelationships(
      final ApiDependencyTreeNodeDTO nodeDTO,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap,
      final boolean isRootNode) throws InvalidSPDXAnalysisException
  {
    SpdxPackage spdxPackage = getSpdxPackageForNode(nodeDTO, purlElementMap);
    if (spdxPackage == null) {
      return;
    }

    if (isRootNode) {
      document.getDocumentDescribes().add(spdxPackage);
    }

    if (nodeDTO.getChildren() == null) {
      return;
    }

    for (ApiDependencyTreeNodeDTO childNode : nodeDTO.getChildren()) {
      SpdxPackage childSpdxPackage = getSpdxPackageForNode(childNode, purlElementMap);
      if (childSpdxPackage != null) {
        Relationship relationship =
            document.createRelationship(childSpdxPackage, RelationshipType.DEPENDS_ON, null);
        spdxPackage.addRelationship(relationship);
      }
      addDependencyRelationships(childNode, document, purlElementMap, false);
    }
  }

  private SpdxPackage getSpdxPackageForNode(ApiDependencyTreeNodeDTO nodeDTO, Map<String, SpdxPackage> purlElementMap) {
    String packageUrl = nodeDTO.getPackageUrl();
    return packageUrl == null ? null : purlElementMap.get(packageUrl);
  }

  private String getReportUrl(String applicationPublicId, String scanId) {
    String iqBaseUrl = "";
    try {
      iqBaseUrl = baseUrl.get();
    }
    catch (IllegalStateException e) {
      log.warn("IQ Server base URL is not configured", e);
    }
    return iqBaseUrl + UserInterfaceLinksHelper.getReportUrl(applicationPublicId, scanId);
  }

  private void addPackages(
      final List<ApiReportComponentDTOV2> reportComponents,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap,
      final String spdxVersion) throws InvalidSPDXAnalysisException
  {
    Map<String, ExtractedLicenseInfo> extractedLicenseInfoMap = new HashMap<>();
    for (ApiReportComponentDTOV2 reportComponent : reportComponents) {
      if (!MatchState.UNKNOWN.getId().equals(reportComponent.matchState)) {
        addPackage(reportComponent, document, purlElementMap, extractedLicenseInfoMap, spdxVersion);
      }
    }
    if (!extractedLicenseInfoMap.isEmpty()) {
      document.setExtractedLicenseInfos(new ArrayList<>(extractedLicenseInfoMap.values()));
    }
  }

  private void addPackage(
      final ApiReportComponentDTOV2 reportComponent,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap,
      final Map<String, ExtractedLicenseInfo> extractedLicenseInfoMap,
      final String spdxVersion) throws InvalidSPDXAnalysisException
  {
    String packageUrl = getPackageUrl(reportComponent);
    if (packageUrl == null) {
      log.warn("Cannot determine the package URL for component: {}", reportComponent.displayName);
      return;
    }
    String version = reportComponent.componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    String sha256 = reportComponent.sha256;

    // SPDX declared-license-field combines Sonatype's declared and observed license sets
    Set<AnyLicenseInfo> licenses = new LinkedHashSet<>();
    for (ApiLicenseDTO licenseDTO : reportComponent.licenseData.declaredLicenses) {
      AnyLicenseInfo licenseInfo = createLicenseInfo(licenseDTO, document, extractedLicenseInfoMap);
      licenses.add(licenseInfo);
    }
    for (ApiLicenseDTO licenseDTO : reportComponent.licenseData.observedLicenses) {
      AnyLicenseInfo licenseInfo = createLicenseInfo(licenseDTO, document, extractedLicenseInfoMap);
      licenses.add(licenseInfo);
    }
    AnyLicenseInfo declaredLicenseInfo = createLicenseInfo(licenses, document);

    // SPDX concluded-license-field is the same as Sonatype's effective license set
    licenses = new LinkedHashSet<>();
    for (ApiLicenseDTO licenseDTO : reportComponent.licenseData.effectiveLicenses) {
      AnyLicenseInfo licenseInfo = createLicenseInfo(licenseDTO, document, extractedLicenseInfoMap);
      licenses.add(licenseInfo);
    }
    AnyLicenseInfo concludedLicenseInfo = createLicenseInfo(licenses, document);

    List<ExternalRef> additionalExternalRefs = addVulnerabilities(reportComponent, document);

    if (reportComponent.swid != null) {
      additionalExternalRefs.add(document.createExternalRef(ReferenceCategory.SECURITY,
          new ReferenceType(SpdxConstants.SPDX_LISTED_REFERENCE_TYPES_PREFIX + "swid"),
          "swid:" + reportComponent.swid.getTagId(),
          null));
    }

    if (reportComponent.cpe != null) {
      String cpeVersion = SbomSpdxUtils.getSpdxCpeVersion(reportComponent.cpe);
      additionalExternalRefs.add(document.createExternalRef(ReferenceCategory.SECURITY,
          new ReferenceType(SpdxConstants.SPDX_LISTED_REFERENCE_TYPES_PREFIX + cpeVersion), reportComponent.cpe,
          null));
    }

    addPackage(packageUrl, version, sha256, declaredLicenseInfo, concludedLicenseInfo, additionalExternalRefs,
        document, purlElementMap, spdxVersion);
  }

  private List<ExternalRef> addVulnerabilities(
      ApiReportComponentDTOV2 component,
      SpdxDocument document) throws InvalidSPDXAnalysisException
  {
    if (component.securityData == null || CollectionUtils.isEmpty(component.securityData.securityIssues)) {
      return new ArrayList<>();
    }
    List<ExternalRef> externalRefs = new ArrayList<>();
    for (ApiSecurityIssueDTO securityIssue : component.securityData.securityIssues) {
      String securityUrl = securityIssue.url;
      if (StringUtils.isNotBlank(securityUrl)) {
        String comment = null;
        if (StringUtils.isNotBlank(securityIssue.source)) {
          comment = CVE.equals(securityIssue.source)
              ? "source: " + NVD
              : "source: " + securityIssue.source.toUpperCase(Locale.ROOT);
        }
        ExternalRef externalRef = document.createExternalRef(ReferenceCategory.SECURITY,
            new ReferenceType("advisory"), securityUrl, comment);
        externalRefs.add(externalRef);
      }
    }
    return externalRefs;
  }

  private AnyLicenseInfo createLicenseInfo(
      Set<AnyLicenseInfo> licenses,
      SpdxDocument document) throws InvalidSPDXAnalysisException
  {
    if (licenses.isEmpty()) {
      return new SpdxNoAssertionLicense();
    }
    if (licenses.size() == 1) {
      return licenses.iterator().next();
    }
    return document.createConjunctiveLicenseSet(licenses);
  }

  private AnyLicenseInfo createLicenseInfo(
      ApiLicenseDTO apiLicense,
      SpdxDocument document,
      Map<String, ExtractedLicenseInfo> extractedLicenseInfoMap) throws InvalidSPDXAnalysisException
  {
    final Set<License> licenseSet = multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(apiLicense.licenseId);
    if (licenseSet.isEmpty()) {
      return new SpdxNoAssertionLicense();
    }
    if (licenseSet.size() == 1) {
      String licenseId = licenseSet.iterator().next().getId();
      return createLicenseObject(licenseId, extractedLicenseInfoMap);
    }
    List<AnyLicenseInfo> members = new ArrayList<>();
    for (License license : licenseSet) {
      members.add(createLicenseObject(license.getId(), extractedLicenseInfoMap));
    }
    return document.createDisjunctiveLicenseSet(members);
  }

  private AnyLicenseInfo createLicenseObject(
      String licenseId,
      Map<String, ExtractedLicenseInfo> extractedLicenseInfoMap) throws InvalidSPDXAnalysisException
  {
    if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
      // Recover valid SPDX license ID here respecting the case instead of trusting original value which might not
      // be an exact match. As a fallback we use the original value
      Optional<String> foundSpdxLicenseIdCaseSensitiveOptional = ListedLicenses.getListedLicenses()
          .listedLicenseIdCaseSensitive(licenseId);
      return ListedLicenses.getListedLicenses()
          .getListedLicenseById(foundSpdxLicenseIdCaseSensitiveOptional
              .orElse(licenseId));
    }
    if (extractedLicenseInfoMap.containsKey(licenseId)) {
      return extractedLicenseInfoMap.get(licenseId);
    }
    ExtractedLicenseInfo licenseInfo = new ExtractedLicenseInfo(LICENSE_REF_PREFIX + licenseId, licenseId);
    extractedLicenseInfoMap.put(licenseId, licenseInfo);
    return licenseInfo;
  }

  private void addPackage(
      final String packageUrl,
      final String version,
      final String sha256,
      final AnyLicenseInfo declaredLicenseInfo,
      final AnyLicenseInfo concludedLicenseInfo,
      final List<ExternalRef> additionalExternalRefs,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap,
      String spdxVersion) throws InvalidSPDXAnalysisException
  {
    if (purlElementMap.containsKey(packageUrl)) {
      return; // avoids duplicates
    }

    ExternalRef purlRef = document.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
        new ReferenceType("purl"), packageUrl, null);

    String copyrightText = null;

    if (org.spdx.library.Version.TWO_POINT_TWO_VERSION.endsWith(spdxVersion)) {
      copyrightText = SpdxConstants.NOASSERTION_VALUE;
    }

    SpdxPackageBuilder packageBuilder =
        document.createPackage(generateSpdxId(packageUrl),
            createSpdxNameFromPurl(packageUrl),
            concludedLicenseInfo,
            copyrightText,
            declaredLicenseInfo)
            .setFilesAnalyzed(false)
            .setDownloadLocation(SpdxConstants.NOASSERTION_VALUE)
            .addExternalRef(purlRef);

    for (ExternalRef externalRef : additionalExternalRefs) {
      packageBuilder.addExternalRef(externalRef);
    }

    if (StringUtils.isNotBlank(version)) {
      packageBuilder.setVersionInfo(version);
    }

    if (StringUtils.isNotBlank(sha256)) {
      final Checksum checksum = document.createChecksum(ChecksumAlgorithm.SHA256, sha256);
      packageBuilder.setChecksums(ImmutableList.of(checksum));
    }
    SpdxPackage spdxPackage = packageBuilder.build();
    purlElementMap.put(packageUrl, spdxPackage);
  }

  private String getPackageUrl(final ApiReportComponentDTOV2 reportComponent) {
    if (StringUtils.isNotBlank(reportComponent.packageUrl)) {
      return reportComponent.packageUrl;
    }
    PackageUrlIdentifier purl =
        PackageUrlIdentifier.fromComponentIdentifier(reportComponent.componentIdentifier.toComponentIdentifier());
    return purl == null ? null : purl.getPackageUrl();
  }

  private String generateSpdxId(final String packageUrl) {
    String spdxId = URLDecoder.decode(packageUrl, StandardCharsets.UTF_8).substring(4);
    int index = spdxId.indexOf('?');
    if (index > -1) {
      spdxId = spdxId.substring(0, index);
    }
    spdxId = spdxId.replaceAll("[^a-zA-Z0-9.]+", "-");
    return SPDX_REF_PREFIX + spdxId;
  }

  /**
   * Given a PURL like "{@code scheme:type/namespace/name@version?qualifiers#subpath}" it creates an SPDX name as
   * "{@code namespace:name}", if the namespace element exists; otherwise it's the same as "{@code name}"
   */
  private String createSpdxNameFromPurl(String purl) {
    PackageUrlIdentifier purlIdentifier = new PackageUrlIdentifier(purl);

    if (purlIdentifier.getPackageUrl() != null) {
      String name = purlIdentifier.getName();
      if (StringUtils.isNotBlank(purlIdentifier.getNamespace())) {
        return purlIdentifier.getNamespace() + ":" + name;
      }
      return name;
    }
    return null;
  }

  private String getSpdxVersionFromPurl(String purl) {
    int index = purl.indexOf('@');
    if (index == -1) {
      return null;
    }
    String version = purl.substring(index + 1);
    index = version.indexOf('?');
    if (index > -1) {
      version = version.substring(0, index);
    }
    return version;
  }

  private SpdxDocument createDocument(
      String spdxVersion,
      String uri,
      ApiReportRawDataDTOV2 data) throws InvalidSPDXAnalysisException
  {
    DefaultModelStore.reset();
    SpdxDocument spdxDocument = new SpdxDocument(uri);
    spdxDocument.setSpecVersion("SPDX-" + spdxVersion);
    spdxDocument.setName(spdxDocument.getId());
    spdxDocument.setDataLicense(new SpdxListedLicense(SpdxConstants.SPDX_DATA_LICENSE_ID));

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendLiteral('T')
        .appendPattern("HH:mm:ss")
        .appendLiteral('Z');
    String date = LocalDateTime.now(ZoneOffset.UTC).format(builder.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype IQ Server - " + versionService.getFullVersion());
    if (StringUtils.isNotBlank(data.globalInformation.dataVersionDate)) {
      creatorInfo.setComment("Data Date: " + data.globalInformation.dataVersionDate);
    }

    spdxDocument.setCreationInfo(creatorInfo);

    return spdxDocument;
  }

  private Response generateResponse(
      final SpdxDocument document,
      final Application application,
      final String scanId,
      final String format,
      final boolean generateCycloneDx) throws Exception
  {
    MediaType type = "json".equals(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
    String spdxFilename = createFileName(application, scanId, ".spdx") + "." + type.getSubtype();
    String cdxFilename = createFileName(application, scanId, ".bom") + "." + type.getSubtype();

    if (generateCycloneDx) {
      addCycloneDxExternalRef(document, cdxFilename);
    }

    String spdxContent;
    Format spdxFormat = "json".equals(format) ? Format.JSON_PRETTY : Format.XML;
    try (MultiFormatStore multiFormatStore =
        new MultiFormatStore(document.getModelStore(), spdxFormat, Verbose.STANDARD);
        ByteArrayOutputStream out = new ByteArrayOutputStream())
    {
      multiFormatStore.serialize(document.getDocumentUri(), out);
      spdxContent = out.toString(StandardCharsets.UTF_8);
    }

    if (generateCycloneDx) {
      Response response = apiCycloneDxService.getByScanId(
          application, scanId, "application/" + format, Version.VERSION_16, "file://" + spdxFilename);
      String cdxContent = response.getEntity().toString();

      String filename = createFileName(application, scanId, "") + ".tar.gz";
      File outputFile = createTarGzFromContent(spdxContent, spdxFilename, cdxContent, cdxFilename);
      return Response.ok(outputFile, MediaType.APPLICATION_OCTET_STREAM)
          .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename))
          .build();
    }
    else {
      return Response.ok(spdxContent, type)
          .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(spdxFilename))
          .build();
    }
  }

  private File createTarGzFromContent(
      String spdxContent,
      String spdxFilename,
      String cdxContent,
      String cdxFilename) throws IOException
  {
    File outputFile = Files.createTempFile("spdx-", ".tar.gz").toFile();
    outputFile.deleteOnExit();
    try (OutputStream outputStream = Files.newOutputStream(outputFile.toPath());
        GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(outputStream);
        TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut))
    {
      // SPDX entry
      TarArchiveEntry spdxEntry = new TarArchiveEntry(spdxFilename);
      spdxEntry.setSize(spdxContent.length());
      tarOut.putArchiveEntry(spdxEntry);
      tarOut.write(spdxContent.getBytes(StandardCharsets.UTF_8));
      tarOut.closeArchiveEntry();
      // CycloneDX entry
      TarArchiveEntry cdxEntry = new TarArchiveEntry(cdxFilename);
      cdxEntry.setSize(cdxContent.length());
      tarOut.putArchiveEntry(cdxEntry);
      tarOut.write(cdxContent.getBytes(StandardCharsets.UTF_8));
      tarOut.closeArchiveEntry();

      tarOut.finish();
    }
    return outputFile;
  }

  private void addCycloneDxExternalRef(
      final SpdxDocument document,
      final String cdxFilename) throws InvalidSPDXAnalysisException
  {
    Collection<SpdxElement> documentDescribes = document.getDocumentDescribes();
    if (!documentDescribes.isEmpty()) {
      SpdxElement spdxElement = documentDescribes.iterator().next();
      if (spdxElement instanceof SpdxPackage) {
        ExternalRef externalRef = document.createExternalRef(ReferenceCategory.SECURITY,
            new ReferenceType("advisory"), "file://" + cdxFilename, "type: CycloneDX");
        ((SpdxPackage) spdxElement).addExternalRef(externalRef);
      }
    }
  }

  private String createFileName(Application application, String scanId, String suffix) {
    String appId = application.getId();
    String stageId = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId).getStageTypeId();
    return String.format("%s-%s-%s%s", application.getPublicId(), stageId, scanId, suffix);
  }

  private String validateFormat(String format) {
    if (SPDX_FORMATS.contains(format)) {
      return format;
    }
    throw new BadRequestException("Invalid format: " + format + ". Supported formats: " + SPDX_FORMATS);
  }
}
