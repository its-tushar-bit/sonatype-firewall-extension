/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader.HostedDataServicesSource;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphService;
import com.sonatype.insight.brain.report.ReportBundleUpdater.FilenameMapping;
import com.sonatype.insight.brain.report.pdf.PdfGeneratorService;
import com.sonatype.insight.brain.sbom.policy.SbomPolicyService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.utils.JsonFileStore;
import com.sonatype.insight.brain.utils.JsonStore;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.COMPONENTS_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.INDEX_HTML;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_ALERTS;

@Path(ReportResource.RESOURCE_PATH)
@Named
@Timed
public class ReportResource
{
  public static final String RESOURCE_PATH = "rest/report/{applicationPublicId}";

  public static final String BROWSE_PATH = "{scanId}/browseReport";

  public static final String SBOM_POLICY_VIOLATION_REPORT = "sbom/{sbomVersion}/sbomPolicyViolationReport";

  public static final String PRINT_PATH = "{scanId}/printReport";

  public static final String SBOM_PRINT_PATH = "sbom/{sbomVersion}/printReport";

  public static final String DOWNLOAD_BUNDLE_PATH = "{scanId}/downloadBundle";

  public static final String PREPARE_PATH = "{scanId}/prepareReport";

  public static final String METADATA_PATH = "{scanId}/metadata";

  private static final Set<Character> INVALID_FILESYSTEM_CHARACTERS;

  public static Long FILE_SIZE_THRESHOLD = 200_000_000L; // 200MB

  private static final long YEAR = (long) 365 * 24 * 60 * 60 * 1000;

  private final InsightWork work;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ReportService reportService;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final ApiReportDataServiceV2 reportDataService;

  private final SbomPolicyService sbomPolicyService;

  private final ReleaseGraphService releaseGraphService;

  private final VersionService versionService;

  private final PdfGeneratorService pdfGeneratorService;

  private final ClusterLockManager clusterLockManager;

  static {
    Set<Character> invalid = new HashSet<>(
        Arrays.asList('*', '\\', '/', '?', ':', '|', '"', '<', '>'));

    for (char c = 0; c <= 31; c++) {
      invalid.add(c);
    }
    INVALID_FILESYSTEM_CHARACTERS = Collections.unmodifiableSet(invalid);
  }

  @Inject
  public ReportResource(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final ReportService reportService,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      final InsightWork work,
      final BaseUrl baseUrl,
      final ApiReportDataServiceV2 reportDataService,
      final SbomPolicyService sbomPolicyService,
      final ReleaseGraphService releaseGraphService,
      final VersionService versionService,
      final PdfGeneratorService pdfGeneratorService,
      final ClusterLockManager clusterLockManager)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.reportService = reportService;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.work = work;
    this.baseUrl = baseUrl;
    this.reportDataService = reportDataService;
    this.sbomPolicyService = sbomPolicyService;
    this.releaseGraphService = releaseGraphService;
    this.versionService = versionService;
    this.pdfGeneratorService = pdfGeneratorService;
    this.clusterLockManager = clusterLockManager;
  }

  /**
   * Get resources for a scan report
   *
   * @since 1.7
   */
  @GET
  @Path(BROWSE_PATH)
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public Response browseReportRoot(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId,
      @Context final HttpServletRequest httpRequest)
  {
    return browseReport(appPublicId, scanId, INDEX_HTML.getName(), httpRequest);
  }

  @GET
  @Path(BROWSE_PATH + "/{path:.*}")
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public Response browseReport(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId,
      @PathParam("path") final String path,
      @Context final HttpServletRequest httpRequest)
  {
    ReportEntry reportEntry = reportService.processBrowseReport(applicationDAO.getByPublicId(appPublicId).getId(),
        scanId, normalizeBrowsePath(path));
    if (reportEntry == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return downloadReportEntry(reportEntry, httpRequest);
  }

  @GET
  @Path(SBOM_POLICY_VIOLATION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  public Response getSbomPolicyViolationReport(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("sbomVersion") final String sbomVersion,
      @QueryParam("componentRef") final String componentRef,
      @QueryParam("fileCoordinateId") final String fileCoordinateId,
      @QueryParam("hash") final String hash,
      @Context final HttpServletRequest httpRequest) throws IOException
  {
    String applicationInternalId = applicationDAO.getByPublicIdNotNull(applicationPublicId).getId();
    ReportEntry policyThreatsReportEntry =
        sbomPolicyService.getPolicyViolationsReportEntry(applicationInternalId, sbomVersion);

    if (policyThreatsReportEntry == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    if (!StringUtils.isAllBlank(fileCoordinateId, componentRef, hash)) {
      JsonNode jsonNode = sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(applicationInternalId,
          sbomVersion, componentRef, fileCoordinateId, hash, policyThreatsReportEntry, null);

      if (jsonNode != null) {
        ResponseBuilder response = Response.ok(jsonNode);
        response.lastModified(new Date(policyThreatsReportEntry.time));
        response.type(httpRequest.getServletContext().getMimeType(policyThreatsReportEntry.name));
        return response.build();
      }
      else {
        // There are no policy violations for the given component
        return Response.ok(JsonNodeFactory.instance.objectNode()).type(MediaType.APPLICATION_JSON_TYPE).build();
      }
    }
    else {
      return downloadReportEntry(policyThreatsReportEntry, httpRequest);
    }
  }

  private Response downloadReportEntry(ReportEntry reportEntry, HttpServletRequest httpRequest) {
    // we don't want to deal with any kind file timestamp stuff with index.html, since we are modifying
    // the contents loaded from the file before serving up to the browser, the timestamp on the file won't
    // change, even when brain versions do, so index.html is always sent in response
    if (reportEntry.name.equals(INDEX_HTML.getName())) {
      reportEntry = appendCacheBustingParams(reportEntry, versionService.getVersion());
    }
    else {
      final long ifModifiedSince = httpRequest.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
      if (ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000) {
        return Response.status(304).build();
      }
    }
    if (BOM_JSON.getName().equals(reportEntry.name) && (reportEntry.buf.length > FILE_SIZE_THRESHOLD)) {
      reportEntry = removeBomPathnames(reportEntry);
    }
    String mimeType = httpRequest.getServletContext().getMimeType(reportEntry.name);
    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }
    else if (mimeType.startsWith("text")) {
      mimeType += ";charset=UTF-8";
    }
    final ResponseBuilder response = Response.ok(reportEntry.buf);
    response.lastModified(new Date(reportEntry.time));
    response.type(mimeType);
    if (!reportEntry.name.endsWith(".json") && !reportEntry.name.equals(INDEX_HTML.getName())) {
      response.expires(new Date(System.currentTimeMillis() + YEAR));
    }
    else {
      // JSON files and the index.html should always check with the server to ensure they are updated.
      // A 304 will be returned for JSON files if they don't need updating, index.html will ALWAYS be
      // returned with a 200 status
      response.expires(new Date());
    }
    return response.build();
  }

  @VisibleForTesting
  public static ReportEntry appendCacheBustingParams(ReportEntry reportEntry, String clmVersion) {
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent = originalIndexHtmlContent.replace("/brain.client.js",
        "/brain.client.js?" + clmVersion).replace("/cip-loader.js", "/cip-loader.js?" + clmVersion);
    return new ReportEntry(reportEntry.name, reportEntry.time,
        augmentedIndexHtmlContent.getBytes(StandardCharsets.UTF_8));
  }

  private ReportEntry removeBomPathnames(ReportEntry reportEntry) {
    byte[] jsonWithoutFieldBuf = JsonUtils.setFieldToEmptyArray(reportEntry.buf, "pathnames");
    return new ReportEntry(reportEntry.name, reportEntry.time, jsonWithoutFieldBuf);
  }

  /**
   * @since 1.35
   */
  @GET
  @Path(METADATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public ReportMetadataDTO getReportMetadata(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    return reportService.getReportMetadata(applicationPublicId, scanId);
  }

  /**
   * Re-evaluates the policy for a scan. The policy must have been evaluated for the given scan at least once. This
   * method should not send policy evaluation notifications.
   */
  @POST
  @Path("{scanId}/reevaluatePolicy")
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
  public Response reevaluatePolicy(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId,
      @DefaultValue("false") @QueryParam("skipAutoWaivers") final Boolean skipAutoWaivers,
      @Context HttpServletRequest request) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    Organization organization = organizationDAO.getById(application.getOrganizationId());

    if (organization.getRelatedRepositoryId() == null) {
      checkEvaluateApplicationPermission(application);
    }
    else {
      checkEvaluateComponentPermission(application);
    }

    String clientUserAgent = HdsClient.getClientUserAgent(request);
    PolicyEvaluation policyEvaluation = reportService.reUploadScanToHds(application.getId(), scanId, clientUserAgent);
    Stage stage = new Stage(policyEvaluation.getStageTypeId());
    scanPolicyEvaluator.evaluate(application, scanId, stage, policyEvaluation.getScanTriggerType(),
        policyEvaluation.getClientScanType(), skipAutoWaivers);
    return Response.ok().build();
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  void checkEvaluateApplicationPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.APPLICATION) Application application)
  {
    // actual work done by AOP interceptor
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.APPLICATION) Application application)
  {
    // actual work done by AOP interceptor
  }

  @GET
  @Path(PRINT_PATH)
  @Produces("application/pdf")
  @Audited(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public Response printReport(
      @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    return pdfGeneratorService.printReport(appPublicId, scanId);
  }

  @GET
  @Path(SBOM_PRINT_PATH)
  @Produces("application/pdf")
  @Audited(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  public Response printSbomReport(
      @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("sbomVersion") final String sbomVersion) throws IOException
  {
    return pdfGeneratorService.printSbomReport(appPublicId, sbomVersion);
  }

  /**
   * Retrieves a self-contained ZIP bundle of the specified report for use by 3rd-party integrators like HP Fortify.
   * Obviously, the employed report template also needs to support self-containment.
   *
   * @since 1.10
   */
  @GET
  @Path(DOWNLOAD_BUNDLE_PATH)
  @Produces("application/zip")
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public Response downloadBundle(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    // TODO: Matt Johnson - this needs extracting into reportUtils
    AuditData.get().setReportId(scanId);
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    ApplicationReport applicationReport = reportService.getReport(app, scanId);
    String filename = "report-" + scanId + ".zip";

    Properties templateProps = applicationReport.getTemplateProperties();
    String cipDetailsPath = templateProps.getProperty("cip.details.path", "");
    String cipListPath = templateProps.getProperty("cip.list.path", "");
    int dataVersion = Integer.parseInt(templateProps.getProperty("data.version", "0"));
    String dataPath = "data/";

    ReportPdfEntity reportPdf = pdfGeneratorService.generateReport(app, scanId);

    ApiReportRawDataDTOV2 reportData = reportDataService.getDataNoAuth(appPublicId, scanId);
    List<PolicyAlert> alerts = Arrays.asList(
        JsonUtils.parse(applicationReport.getEntry(POLICY_ALERTS.getName()).buf,
            PolicyAlert[].class));

    File updatedFile = Files.createTempFile("report", ".zip").toFile();
    try (Stream<ReportEntity> reportEntities = applicationReport.getAllEntities();
        ReportBundleUpdater updater = new ReportBundleUpdater(
            reportEntities,
            updatedFile,
            new FilenameMapping("^.*\\.json$", dataPath + "$0")))
    {

      addLegacyReportArtifacts(updater);

      updater.remove("detail.rptdesign");
      try (InputStream inputStream = reportPdf.getInputStream()) {
        updater.add(dataPath + "report.pdf", inputStream);
      }
      updater.add(dataPath + COMPONENTS_JSON.getName(), reportData);

      addUniqueComponentsToUpdater(appPublicId, scanId, dataPath, dataVersion, reportData.components, updater);

      try (Stream<ReportEntity> reportEntities2 = applicationReport.getAllEntities()) {
        Map<String, ReportEntity> entitiesByName =
            reportEntities2.collect(Collectors.toMap(ReportEntity::getName, Function.identity()));

        ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(app);

        for (ReportEntity entity : entitiesByName.values()) {
          String name = entity.getName();
          if (!cipDetailsPath.isEmpty() && name.startsWith(cipDetailsPath)) {

            final NamedComponentDetails hdsDetails;
            try (var entityStream = entity.getInputStream()) {
              hdsDetails = JsonUtils.parse(entityStream, NamedComponentDetails.class);
            }

            NamedComponentDetails clmDetails =
                ComponentDetailsLoader.getComponentDetails(hdsDetails.getComponentIdentifier(), hdsDetails.getHash(),
                    hdsDetails.getMatchState(), new HostedDataServicesSource()
                    {
                      @Override
                      public NamedComponentDetails getDetails() throws IOException {
                        return hdsDetails;
                      }
                    });
            componentDetailsLoader.augmentComponentDetails(clmDetails);
            clmDetails.setPolicyAlerts(getAlertsForComponent(clmDetails.getHash(), alerts));
            updater.add(dataPath + name, clmDetails);

            if (!cipListPath.isEmpty()
                && IdentificationSource.MANUAL.getId().equals(clmDetails.getIdentificationSource()))
            {
              String listPath = cipListPath;
              if (dataVersion >= 3) {
                listPath += toDataPathV3(clmDetails.getComponentIdentifier()) + "/list.json";
              }
              else if (dataVersion >= 1) {
                listPath += toDataPath(clmDetails.getComponentIdentifier()) + "/list.json";
              }
              else {
                listPath += toLegacyDataPath(clmDetails) + ".json";
              }
              if (!entitiesByName.containsKey(listPath) && !updater.contains(dataPath + listPath)) {
                // CIP expects this to be an empty (!) array for every GAV but the HDS doesn't know about claimed
                // components
                ComponentDetailsList list = new ComponentDetailsList();
                list.setList(Collections.emptyList());
                updater.add(dataPath + listPath, list);
              }
            }
          }
          if (!cipListPath.isEmpty() && name.startsWith(cipListPath)) {
            final ComponentDetailsList list;
            try (var entityStream = entity.getInputStream()) {
              list = JsonUtils.parse(entityStream, ComponentDetailsList.class);
            }

            for (ComponentDetails details : list.getList()) {
              componentDetailsLoader.augmentComponentDetails(details);
            }
            updater.add(dataPath + name, list);
          }
        }
      }
    }
    catch (IOException | RuntimeException e) {
      updatedFile.delete();
      throw e;
    }

    final ResponseBuilder response = Response.ok();
    response.entity(new FileInputStream(updatedFile)
    {
      @Override
      public void close() throws IOException {
        try {
          super.close();
        }
        finally {
          updatedFile.delete();
        }
      }
    });
    response.header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename));
    return response.build();
  }

  private static final List<String> legacyReportArtifacts = ImmutableList.of(
      "appcheck.js",
      "flag_white.png",
      "header-columns-bg.gif",
      "lib.min.css",
      "popularity.png",
      "protovis-tipsy.js",
      "red_arrow.png",
      "report.css",
      "sort-asc.gif",
      "yellow_arrow.png",
      "artifactInfoIcon.png",
      "dirty.gif",
      "glyphicons-halflings.png",
      "history.png",
      "lib.min.js",
      "protovis.min.js",
      "protovis-xpan.js",
      "release-header-bg.png",
      "report.js",
      "sort-desc.gif",
      "collapse.gif",
      "expand.gif",
      "glyphicons-halflings-white.png",
      "insight-slick-grid.merged.min.js",
      "orange_arrow.png",
      "protovis-msie.min.js",
      "release-tooltip.png",
      "slick.grid-2.0.merged.min.js",
      "ui-icons_888888_256x240.png",
      "public/bg-score-critical.png",
      "public/bg-score-moderate.png",
      "public/bg-score-severe.png",
      "public/blue.png",
      "public/coord-unknown.png",
      "public/grey.png",
      "public/orange.png",
      "public/security-icon_16x16.png",
      "public/yellow.png",
      "public/bg-score-ignore.png",
      "public/bg-score-none.png",
      "public/bg-score-unspecified.png",
      "public/coord-component.png",
      "public/glypyicons-halfligns-icon-info-sign.png",
      "public/license-icon_16x16.png",
      "public/red.png",
      "public/sonatype.png");

  private void addLegacyReportArtifacts(final ReportBundleUpdater updater) throws IOException {
    for (String zipEntry : legacyReportArtifacts) {
      String resource = "/com/sonatype/insight/brain/legacy.report/" + zipEntry;
      try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
        updater.add(zipEntry, inputStream);
      }
    }
  }

  static String toDataPathV3(ComponentIdentifier componentIdentifier) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(componentIdentifier.getFormat());
    for (Entry<String, String> entry : componentIdentifier.getCoordinates().entrySet()) {
      buffer.append('/').append(entry.getKey());
      buffer.append('=').append(encode(entry.getValue()));
    }
    return buffer.toString();
  }

  private static String encode(String piece) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < piece.length(); i++) {
      Character c = piece.charAt(i);
      if (INVALID_FILESYSTEM_CHARACTERS.contains(c)) {
        String hexString = Integer.toHexString(c);
        sb.append("%");
        if (hexString.length() == 1) {
          sb.append('0');
        }
        sb.append(Integer.toHexString(c));
      }
      else {
        sb.append(piece.charAt(i));
      }
    }

    return sb.toString();
  }

  private String toDataPath(ComponentIdentifier componentIdentifier) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(componentIdentifier.getFormat());
    for (Entry<String, String> entry : componentIdentifier.getCoordinates().entrySet()) {
      buffer.append('/').append(entry.getKey());
      buffer.append('=').append(entry.getValue());
    }
    return buffer.toString();
  }

  private String toLegacyDataPath(String groupId, String artifactId, String version) {
    return groupId + '/' + artifactId + '/' + version;
  }

  @SuppressWarnings("deprecation")
  private String toLegacyDataPath(NamedComponentDetails namedComponentDetails) {
    return toLegacyDataPath(namedComponentDetails.getGroupId(), namedComponentDetails.getArtifactId(),
        namedComponentDetails.getVersion());
  }

  private void addUniqueComponentsToUpdater(
      final String applicationPublicId,
      final String scanId,
      final String dataPath,
      final int dataVersion,
      final List<ApiReportComponentDTOV2> components,
      final ReportBundleUpdater updater) throws IOException
  {
    for (ApiReportComponentDTOV2 component : components) {
      ComponentIdentifier componentIdentifier =
          ApiComponentIdentifierDTOV2.toComponentIdentifier(component.componentIdentifier);
      if (componentIdentifier != null) {
        String imagePath;
        if (dataVersion >= 3) {
          imagePath = toDataPathV3(componentIdentifier) + "/releases.png";
        }
        else if (dataVersion >= 1) {
          imagePath = toDataPath(componentIdentifier) + "/releases.png";
        }
        else if (!componentIdentifier.isMaven()) {
          continue;
        }
        else {
          imagePath = toLegacyDataPath(componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
              componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID),
              componentIdentifier.get(ComponentIdentifier.VERSION))
              + ".png";
        }
        imagePath = dataPath + "release-graph/" + imagePath;
        if (!updater.contains(imagePath)) {
          byte[] imageData = releaseGraphService.getImage(applicationPublicId, scanId, componentIdentifier);
          updater.add(imagePath, imageData);
        }
      }
    }
  }

  private List<PolicyAlert> getAlertsForComponent(String hash, List<PolicyAlert> appAlerts) {
    List<PolicyAlert> componentAlerts = new ArrayList<>();
    for (PolicyAlert appAlert : appAlerts) {
      PolicyAlert componentAlert = null;
      for (ComponentFact fact : appAlert.getTrigger().getComponentFacts()) {
        if (hash.equals(fact.getHash())) {
          if (componentAlert == null) {
            PolicyFact appAlertTrigger = appAlert.getTrigger();
            componentAlert =
                new PolicyAlert(new PolicyFact(appAlertTrigger.getPolicyId(), appAlertTrigger.getPolicyName(),
                    appAlertTrigger.getThreatLevel(), appAlertTrigger.getPolicyViolationId()), null);
            componentAlerts.add(componentAlert);
          }
          componentAlert.getTrigger().addComponentFact(fact);
        }
      }
    }
    return componentAlerts;
  }

  @GET
  @Path("{scanId}/auditLog/{path}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public Response auditLog(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("path") final String path,
      @QueryParam("key") final String encodedKey) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(appPublicId);
    String appId = application.getId();

    final JsonStore store = new JsonFileStore(work.getAuditDir(appId), appId, clusterLockManager);
    final ContainerNode<?> key = decodeKey(encodedKey);
    final ContainerNode<?> feed = store.history(key, path.split("[+]+"));
    if (feed != null) {
      return Response.ok(JsonUtils.generate(feed)).build();
    }

    return Response.ok().build();
  }

  @GET
  @Path("{scanId}/brain/policy-assets/js/{path:.*}")
  public Response brainGetJs(final @PathParam("path") String path) {
    return redirectToBrainJs(baseUrl, path);
  }

  @GET
  @Path("{scanId}/brain/{path:.*}")
  public Response brainGet(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @POST
  @Path("{scanId}/brain/{path:.*}")
  public Response brainPost(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @PUT
  @Path("{scanId}/brain/{path:.*}")
  public Response brainPut(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @DELETE
  @Path("{scanId}/brain/{path:.*}")
  public Response brainDelete(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  private static Response redirectToBrainJs(final BaseUrl baseUrl, final String path) {
    UriBuilder uriBuilder = baseUrl.redirect().path("assets").path(path);

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  private static Response redirectToBrain(final BaseUrl baseUrl, final String path) {
    String normalizedPath = normalizeRedirectPath(path);
    java.net.URI baseUri = baseUrl.redirect().build();
    if (normalizedPath.isEmpty()) {
      return Response.temporaryRedirect(baseUri).build();
    }

    String baseLocation = baseUrl.get();
    if (!baseLocation.endsWith("/")) {
      baseLocation += "/";
    }
    UriBuilder uriBuilder = UriBuilder.fromUri(baseLocation + normalizedPath);
    if (baseUri.getRawQuery() != null) {
      uriBuilder.replaceQuery(baseUri.getRawQuery());
    }

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  private static String normalizeBrowsePath(final String path) {
    if (StringUtils.isBlank(path) || "/".equals(path)) {
      return INDEX_HTML.getName();
    }
    return normalizeRedirectPath(path);
  }

  private static String normalizeRedirectPath(final String path) {
    return path == null ? "" : path.replaceFirst("^/+", "");
  }

  /**
   * @since 1.13.0
   *        Given an encodedKey for a given Component, check for legacy GAV format and replace with ComponentIdentifier
   *        if
   *        it is missing.
   *        Necessary to maintain backwards compatibility for older reports which encode this key as a JSON Object in
   *        this
   *        format: {"hash":"hashValue","groupId":"g","artifactId":"a","version":"v"}
   */
  private ContainerNode<?> decodeKey(final String encodedKey) throws IOException {
    if (encodedKey == null) {
      return null;
    }
    ContainerNode<?> decodedKey = JsonUtils.parse(encodedKey.getBytes(StandardCharsets.UTF_8));
    ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) decodedKey);
    return decodedKey;
  }
}
