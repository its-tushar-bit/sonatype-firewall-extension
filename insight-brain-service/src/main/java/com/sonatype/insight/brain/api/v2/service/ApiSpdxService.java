/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.SpdxPackage.SpdxPackageBuilder;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.ReferenceCategory;
import org.spdx.library.model.enumerations.RelationshipType;

@Named
@Singleton
public class ApiSpdxService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSpdxService.class);

  static final Set<String> SPDX_FORMATS = ImmutableSet.of("json", "xml");

  static final Set<String> SPDX_VERSIONS = ImmutableSet.of("2.3");

  static final String SPDX_REF_PREFIX = "SPDXRef-";

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ApplicationHelper applicationHelper;

  private final BaseUrl baseUrl;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final VersionService versionService;

  @Inject
  public ApiSpdxService(
      ApiReportDataServiceV2 apiReportDataServiceV2,
      ApplicationHelper applicationHelper,
      BaseUrl baseUrl,
      PolicyEvaluationDAO policyEvaluationDAO,
      VersionService versionService)
  {
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.applicationHelper = applicationHelper;
    this.baseUrl = baseUrl;
    this.policyEvaluationDAO = policyEvaluationDAO;
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
    if (!SystemConfigurationPropertyFeature.SPDX_EXPORT.isEnabled()) {
      throw new ConflictException("This API endpoint is currently disabled.");
    }

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);

    return getByScanId(application, scanId, validateFormat(format), generateCycloneDx,
        validateSpdxVersion(spdxVersion));
  }

  @Authorize(permission = Permission.READ)
  public Response getLatestForStage(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String stageId,
      String format,
      boolean generateCycloneDx,
      String spdxVersion)
  {
    if (!SystemConfigurationPropertyFeature.SPDX_EXPORT.isEnabled()) {
      throw new ConflictException("This API endpoint is currently disabled.");
    }

    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a policy evaluation for " + applicationId + " in stage " + stageId);
    }

    return getByScanId(application, evaluation.getScanId(), validateFormat(format), generateCycloneDx,
        validateSpdxVersion(spdxVersion));
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

      final SpdxDocument document = createDocument(spdxVersion, uri);
      final Map<String, SpdxPackage> purlElementMap = new HashMap<>();

      addPackages(data.components, document, purlElementMap);

      ApiDependencyTreeNodeDTO rootNodeDTO =
          apiReportDataServiceV2.getDependencyTreeNoAuth(application.getPublicId(), scanId);
      if (rootNodeDTO != null) {
        addRootPackage(rootNodeDTO, document, purlElementMap);
        addDependencyRelationships(rootNodeDTO, document, purlElementMap, true);
      }

      String appId = application.getId();
      String stageId = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId).getStageTypeId();
      String filename = String.format("%s-%s-%s.spdx", application.getPublicId(), stageId, scanId);

      return generateResponse(document, filename, format, generateCycloneDx);
    }
    catch (IOException | InvalidSPDXAnalysisException e) {
      throw new RuntimeException("An error occurred while generating the SPDX file", e);
    }
  }

  private void addRootPackage(
      final ApiDependencyTreeNodeDTO rootNodeDTO,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap)
      throws UnsupportedEncodingException, InvalidSPDXAnalysisException
  {
    String packageUrl = rootNodeDTO.getPackageUrl();
    if (packageUrl != null && !purlElementMap.containsKey(packageUrl)) {
      String version = getSpdxVersionFromPurl(packageUrl);
      addPackage(packageUrl, version, null, document, purlElementMap);
    }
  }

  private void addDependencyRelationships(
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

  private void addPackages(final List<ApiReportComponentDTOV2> reportComponents, final SpdxDocument document,
                           final Map<String, SpdxPackage> purlElementMap)
      throws InvalidSPDXAnalysisException, UnsupportedEncodingException
  {
    for (ApiReportComponentDTOV2 reportComponent : reportComponents) {
      if (!MatchState.UNKNOWN.getId().equals(reportComponent.matchState)) {
        addPackage(reportComponent, document, purlElementMap);
      }
    }
  }

  private void addPackage(
      final ApiReportComponentDTOV2 reportComponent,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap)
      throws InvalidSPDXAnalysisException, UnsupportedEncodingException
  {
    String packageUrl = getPackageUrl(reportComponent);
    if (packageUrl == null) {
      log.warn("Cannot determine the package URL for component: {}", reportComponent.displayName);
      return;
    }
    String version = reportComponent.componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    String sha256 = reportComponent.sha256;

    addPackage(packageUrl, version, sha256, document, purlElementMap);
  }

  private void addPackage(
      final String packageUrl,
      final String version,
      final String sha256,
      final SpdxDocument document,
      final Map<String, SpdxPackage> purlElementMap)
      throws InvalidSPDXAnalysisException, UnsupportedEncodingException
  {
    if (purlElementMap.containsKey(packageUrl)) {
      return; // avoids duplicates
    }

    ExternalRef purlRef = document.createExternalRef(ReferenceCategory.PACKAGE_MANAGER,
        new ReferenceType("purl"), packageUrl, null);

    SpdxPackageBuilder packageBuilder = document.createPackage(
            generateSpdxId(packageUrl),
            createSpdxNameFromPurl(packageUrl),
            // correct license data will be added with CLM-25906
            null, null, null)
        .setFilesAnalyzed(false)
        .addExternalRef(purlRef);

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

  private String generateSpdxId(final String packageUrl) throws UnsupportedEncodingException {
    String spdxId = URLDecoder.decode(packageUrl, StandardCharsets.UTF_8.name()).substring(4);
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
  private String createSpdxNameFromPurl(String purl) throws UnsupportedEncodingException {
    String name = URLDecoder.decode(purl, StandardCharsets.UTF_8.name());
    name = name.substring(purl.indexOf('/') + 1);
    int index = name.indexOf('@');
    if (index > -1) {
      name = name.substring(0, index);
    }
    return name.replace("/", ":");
  }

  private String getSpdxVersionFromPurl(String purl) {
    int index = purl.indexOf('@');
    if (index == -1) {
      return null;
    }
    String version = purl.substring(index + 1);
    index = version.indexOf('?');
    if (index >  -1) {
      version = version.substring(0, index);
    }
    return version;
  }

  private SpdxDocument createDocument(String spdxVersion, String uri) throws InvalidSPDXAnalysisException {
    DefaultModelStore.reset();
    SpdxDocument spdxDocument = new SpdxDocument(uri);
    spdxDocument.setSpecVersion("SPDX-" + spdxVersion);

    SpdxCreatorInformation creatorInfo = new SpdxCreatorInformation();
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .appendLiteral('T')
        .appendPattern("HH:mm:ss")
        .appendLiteral('Z');
    String date = LocalDateTime.now(ZoneOffset.UTC).format(builder.toFormatter());

    creatorInfo.setCreated(date);
    creatorInfo.getCreators().add("Tool: Sonatype IQ Server - " + versionService.getFullVersion());
    spdxDocument.setCreationInfo(creatorInfo);

    return spdxDocument;
  }

  private Response generateResponse(
      final SpdxDocument document,
      String filename,
      final String format,
      final boolean generateCycloneDx) throws IOException, InvalidSPDXAnalysisException
  {
    String content;
    MediaType type;
    if (generateCycloneDx) {
      type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }
    else {
      type = "json".equals(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
    }

    Format spdxFormat = "json".equals(format) ? Format.JSON_PRETTY : Format.XML;
    MultiFormatStore multiFormatStore = new MultiFormatStore(document.getModelStore(), spdxFormat, Verbose.STANDARD);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      multiFormatStore.serialize(document.getDocumentUri(), out);
      content = out.toString("UTF-8");
    }

    filename = filename + "." + type.getSubtype();
    return Response.ok(content, type)
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename)).build();
  }

  private String validateFormat(String format) {
    if (SPDX_FORMATS.contains(format)) {
      return format;
    }
    throw new BadRequestException("Invalid format: " + format + ". Supported formats: " + SPDX_FORMATS);
  }

  private String validateSpdxVersion(String spdxVersion) {
    if (SPDX_VERSIONS.contains(spdxVersion)) {
      return spdxVersion;
    }
    throw new BadRequestException(
        "Invalid SPDX version: " + spdxVersion + ". Supported SPDX versions: " + SPDX_VERSIONS);
  }
}
