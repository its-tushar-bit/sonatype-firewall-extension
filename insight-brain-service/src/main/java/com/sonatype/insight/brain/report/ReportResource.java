/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ReportResource.RESOURCE_PATH)
@Named
@Timed
public class ReportResource
{
  public static final String RESOURCE_PATH = "rest/report/{applicationPublicId}/{scanId}";

  public static final String BROWSE_PATH = "browseReport";

  public static final String PRINT_PATH = "printReport";

  public static final String DOWNLOAD_BUNDLE_PATH = "downloadBundle";

  public static final String PREPARE_PATH = "prepareReport";

  public static final String METADATA_PATH = "metadata";

  private static final Logger log = LoggerFactory.getLogger(ReportResource.class);

  private static final Set<Character> INVALID_FILESYSTEM_CHARACTERS;

  private static final long YEAR = (long) 365 * 24 * 60 * 60 * 1000;

  private final InsightWork work;

  private final BaseUrl baseUrl;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private ApplicationAdapter applicationAdapter;

  private final ReportService reportService;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final ApiReportDataServiceV2 reportDataService;

  private final ReleaseGraphService releaseGraphService;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final VersionService versionService;

  static {
    Set<Character> invalid = new HashSet<>(
        Arrays.asList(new Character[] { '*', '\\', '/', '?', ':', '|', '"', '<', '>' }));

    for (char c = 0; c <= 31; c++) {
      invalid.add(c);
    }
    INVALID_FILESYSTEM_CHARACTERS = Collections.unmodifiableSet(invalid);
  }

  @Inject
  public ReportResource(final ReportService reportService,
                        final ScanPolicyEvaluator scanPolicyEvaluator,
                        InsightWork work,
                        BaseUrl baseUrl,
                        ApplicationAdapter applicationAdapter,
                        ApiReportDataServiceV2 reportDataService,
                        ReleaseGraphService releaseGraphService,
                        ComponentDetailsLoader componentDetailsLoader,
                        VersionService versionService)
  {
    this.reportService = reportService;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.work = work;
    this.baseUrl = baseUrl;
    this.applicationAdapter = applicationAdapter;
    this.reportDataService = reportDataService;
    this.releaseGraphService = releaseGraphService;
    this.componentDetailsLoader = componentDetailsLoader;
    this.versionService = versionService;
  }

  /**
   * @deprecated Support legacy CI instances (pre 2.11) and Nexus CLM plugins that persisted a report link obtained from
   *             CLM 1.6-
   */
  @Deprecated
  @GET
  @Path("embedReport/{path:.*}")
  @Authorize(permission = Permission.READ, anonymousAllowed = true)
  public Response embedReport(
      @PathParam("applicationPublicId") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String appPublicId,
      @PathParam("scanId") final String scanId,
      @PathParam("path") final String path)
  {
    if ("index.html".equals(path) || path.isEmpty()) {
      UriBuilder uriBuilder = baseUrl.redirect();
      uriBuilder.path(UserInterfaceLinksResource.RESOURCE_PATH + "/" + UserInterfaceLinksResource.REPORT_PATH);

      StringBuilder sb = new StringBuilder();
      sb.append("<html>");
      sb.append("<body style='font: 12px Verdana, Helvetica;margin-top:50px;'>");
      sb.append("<h1>This report has moved</h1>");
      sb.append("<p>Your Nexus IQ Server was updated, causing the report formerly at this location to be moved ");
      sb.append("<a target='_blank' href='" + uriBuilder.build(appPublicId, scanId) + "'>here</a></p>");
      sb.append("</body>");
      sb.append("</html>");

      final ResponseBuilder response = Response.ok(sb.toString());
      response.type("text/html;charset=UTF-8");
      response.expires(new Date(0));
      return response.build();
    }
    throw new NotFoundException("Reports have been moved.  Clear cache and reload.");
  }

  /**
   * Get resources for a scan report
   *
   * @since 1.7
   */
  @GET
  @Path(BROWSE_PATH + "/{path:.*}")
  @Authorize(permission = Permission.READ)
  @Audited(AuditEvent.VIEW_APPLICATION_COMPOSITION_REPORT)
  public Response browseReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId,
      @PathParam("path") final String path,
      @Context final HttpServletRequest httpRequest) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(appPublicId);
    String appId = application.getId();

    final String name = Report.toEntryName(path);
    auditBrowseReport(scanId, name);
    final File reportFile = reportService.fetchReport(work, appId, scanId, false);
    ReportEntry reportEntry = null;
    try {
      reportEntry = Report.getEntry(reportFile, name);
    }
    catch (final Exception e) {
      log.warn("Problem embedding report: " + e.getMessage(), e);
    }
    if (reportEntry != null) {
      // we don't want to deal with any kind file timestamp stuff with index.html, since we are modifying
      // the contents loaded from the file before serving up to the browser, the timestamp on the file won't
      // change, even when brain versions do, so index.html is always sent in response
      if (reportEntry.name.equals("index.html")) {
        reportEntry = Report.appendCacheBustingParams(reportEntry, versionService.getVersion());
      }
      else {
        final long ifModifiedSince = httpRequest.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000) {
          return Response.status(304).build();
        }
      }
      String mimeType = httpRequest.getServletContext().getMimeType(name);
      if (mimeType == null) {
        mimeType = "application/octet-stream";
      }
      else if (mimeType.startsWith("text")) {
        mimeType += ";charset=UTF-8";
      }
      final ResponseBuilder response = Response.ok(reportEntry.buf);
      response.lastModified(new Date(reportEntry.time));
      response.type(mimeType);
      if (!name.endsWith(".json") && !name.equals("index.html")) {
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
    return Response.status(Status.NOT_FOUND).build();
  }

  private void auditBrowseReport(final String scanId, final String name) {
    if (name.endsWith(".json")) {
      AuditData.get().setReportId(scanId);
    }
    else {
      AuditData.get().setEvent(null);
    }
  }

  /**
   * @since 1.35
   */
  @GET
  @Path(METADATA_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ReportMetadataDTO getReportMetadata(@PathParam("applicationPublicId") final String applicationPublicId,
                                             @PathParam("scanId") final String scanId) throws IOException
  {
    return reportService.getReportMetadata(applicationPublicId, scanId);
  }

  /**
   * Re-evaluates the policy for a scan. The policy must have been evaluated for the given scan at least once. This
   * method should not send policy evaluation notifications.
   */
  @POST
  @Path("reevaluatePolicy")
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public Response reevaluatePolicy(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId")
      final String applicationPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    AuditData.get().setScanId(scanId);

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId);

    if (policyEvaluation == null) {
      throw new BadRequestException("Policy evaluation for scan " + scanId + " does not exist on the server.");
    }

    scanPolicyEvaluator.evaluate(application, scanId, new Stage(policyEvaluation.getStageTypeId()));

    return Response.ok().build();
  }

  @GET
  @Path(PRINT_PATH)
  @Produces("application/pdf")
  @Audited(AuditEvent.PRINT_APPLICATION_COMPOSITION_REPORT)
  public Response printReport(
      @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    return reportService.printReport(appPublicId, scanId);
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
  public Response downloadBundle(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    AuditData.get().setReportId(scanId);
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    File reportFile = reportService.fetchReport(work, app.getId(), scanId, true);
    String filename = "report-" + scanId + ".zip";

    Properties templateProps = Report.getTemplateProperties(reportFile);
    String cipDetailsPath = templateProps.getProperty("cip.details.path", "");
    String cipListPath = templateProps.getProperty("cip.list.path", "");
    int dataVersion = Integer.parseInt(templateProps.getProperty("data.version", "0"));
    String dataPath = "data/";

    ContactDTO contact = applicationAdapter.getContact(app.getContactInternalName());
    File pdfFile = Report.printPdf(reportFile, "", "", contact);

    ApiReportDataDTOV2 reportData = reportDataService.getDataNoAuth(appPublicId, scanId);
    List<PolicyAlert> alerts = Arrays.asList(JsonUtils
        .parse(Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME).buf, PolicyAlert[].class));

    File updatedFile = File.createTempFile("report", ".zip");
    try (ReportBundleUpdater updater = new ReportBundleUpdater(reportFile, updatedFile,
        new ReportBundleUpdater.FilenameMapping("^.*\\.json$", dataPath + "$0"))) {
      updater.remove("detail.rptdesign");
      updater.add(dataPath + "report.pdf", pdfFile);
      updater.add(dataPath + "components.json", reportData);

      addUniqueComponentsToUpdater(appPublicId, scanId, dataPath, dataVersion, reportData.components, updater);

      File[] cachedFiles = Report.getCacheDir(reportFile).listFiles();
      if (cachedFiles != null) {
        for (File cachedFile : cachedFiles) {
          updater.add(dataPath + cachedFile.getName(), cachedFile);
        }
      }

      try (ZipFile reportZip = new ZipFile(reportFile)) {
        for (Enumeration<? extends ZipEntry> en = reportZip.entries(); en.hasMoreElements();) {
          ZipEntry entry = en.nextElement();
          if (entry.isDirectory()) {
            continue;
          }
          if (!cipDetailsPath.isEmpty() && entry.getName().startsWith(cipDetailsPath)) {
            final NamedComponentDetails hdsDetails = JsonUtils.parse(reportZip.getInputStream(entry),
                NamedComponentDetails.class);
            NamedComponentDetails clmDetails = componentDetailsLoader.getComponentDetails(
                hdsDetails.getComponentIdentifier(), hdsDetails.getHash(), hdsDetails.getMatchState(),
                new ComponentDetailsLoader.HostedDataServicesSource()
                {
                  @Override
                  public NamedComponentDetails getDetails() throws IOException {
                    return hdsDetails;
                  }
                });
            componentDetailsLoader.augmentComponentDetails(app, clmDetails);
            clmDetails.setPolicyAlerts(getAlertsForComponent(clmDetails.getHash(), alerts));
            updater.add(dataPath + entry.getName(), clmDetails);

            if (!cipListPath.isEmpty()
                && IdentificationSource.MANUAL.getId().equals(clmDetails.getIdentificationSource())) {
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
              if (reportZip.getEntry(listPath) == null && !updater.contains(dataPath + listPath)) {
                // CIP expects this to be an empty (!) array for every GAV but the HDS doesn't know about claimed
                // components
                ComponentDetailsList list = new ComponentDetailsList();
                list.setList(Collections.emptyList());
                updater.add(dataPath + listPath, list);
              }
            }
          }
          if (!cipListPath.isEmpty() && entry.getName().startsWith(cipListPath)) {
            final ComponentDetailsList list = JsonUtils.parse(reportZip.getInputStream(entry),
                ComponentDetailsList.class);
            for (ComponentDetails details : list.getList()) {
              componentDetailsLoader.augmentComponentDetails(app, details);
            }
            updater.add(dataPath + entry.getName(), list);
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
    response.header("Content-Disposition", "attachment; filename=" + UrlUtils.encodeUrlComponent(filename));
    return response.build();
  }

  static String toDataPathV3(ComponentIdentifier componentIdentifier) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(componentIdentifier.getFormat());
    for (Map.Entry<String, String> entry : componentIdentifier.getCoordinates().entrySet()) {
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
    for (Map.Entry<String, String> entry : componentIdentifier.getCoordinates().entrySet()) {
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

  private void addUniqueComponentsToUpdater(final String applicationPublicId,
                                            final String scanId,
                                            final String dataPath,
                                            final int dataVersion,
                                            final List<ApiReportComponentDTOV2> components,
                                            final ReportBundleUpdater updater) throws IOException
  {
    for (ApiReportComponentDTOV2 component : components) {
      ComponentIdentifier componentIdentifier = convertFromApi(component.componentIdentifier);
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

  private ComponentIdentifier convertFromApi(ApiComponentIdentifierDTOV2 apiComponentIdentifier) {
    if (apiComponentIdentifier == null) {
      return null;
    }

    return new ComponentIdentifier(apiComponentIdentifier.getFormat(), apiComponentIdentifier.getCoordinates());
  }

  private List<PolicyAlert> getAlertsForComponent(String hash, List<PolicyAlert> appAlerts) {
    List<PolicyAlert> componentAlerts = new ArrayList<>();
    for (PolicyAlert appAlert : appAlerts) {
      PolicyAlert componentAlert = null;
      for (ComponentFact fact : appAlert.getTrigger().getComponentFacts()) {
        if (hash.equals(fact.getHash())) {
          if (componentAlert == null) {
            componentAlert = appAlert.with(appAlert.getTrigger().with(new ArrayList<ComponentFact>()));
            componentAlerts.add(componentAlert);
          }
          componentAlert.getTrigger().addComponentFact(fact);
        }
      }
    }
    return componentAlerts;
  }

  @GET
  @Path("auditLog/{path}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public Response auditLog(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String appPublicId,
      @PathParam("path") final String path,
      @QueryParam("key") final String encodedKey) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(appPublicId);
    String appId = application.getId();

    final JsonStore store = JsonUtils.fileStore(work.getAuditDir(appId));
    final ContainerNode<?> key = decodeKey(encodedKey);
    final ContainerNode<?> feed = store.history(key, path.split("[+]+"));
    if (feed != null) {
      return Response.ok(JsonUtils.generate(feed)).build();
    }

    return Response.ok().build();
  }

  @GET
  @Path("brain/policy-assets/js/{path:.*}")
  public Response brainGetJs(final @PathParam("path") String path) {
    return redirectToBrainJs(baseUrl, path);
  }

  @GET
  @Path("brain/{path:.*}")
  public Response brainGet(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @POST
  @Path("brain/{path:.*}")
  public Response brainPost(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @PUT
  @Path("brain/{path:.*}")
  public Response brainPut(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  @DELETE
  @Path("brain/{path:.*}")
  public Response brainDelete(final @PathParam("path") String path) {
    return redirectToBrain(baseUrl, path);
  }

  private static Response redirectToBrainJs(final BaseUrl baseUrl, final String path) {
    UriBuilder uriBuilder = baseUrl.redirect().path("assets").path(path);

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  private static Response redirectToBrain(final BaseUrl baseUrl, final String path) {
    UriBuilder uriBuilder = baseUrl.redirect().path(path);

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  public static String getReportPath(final String appPublicId, final String scanId) {
    String url = ReportResource.RESOURCE_PATH + "/browseReport/";
    url = url.replace("{applicationPublicId}", UrlUtils.encodeUrlComponent(appPublicId));
    url = url.replace("{scanId}", UrlUtils.encodeUrlComponent(scanId));
    return url;
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
    ContainerNode<?> decodedKey = JsonUtils.parse(encodedKey.getBytes("UTF-8"));
    ComponentIdentifierAdapter.replaceGavWithComponentIdentifier((ObjectNode) decodedKey);
    return decodedKey;
  }

  /**
   * Prepares the report for an expanded coverage scan to be available when the customer loads it in a browser.
   * It waits for the report to become available on the HDS.
   * 
   * @since 1.37
   */
  @POST
  @Path(PREPARE_PATH)
  public void prepareExpandedCoverageReport(@PathParam("applicationPublicId") String applicationPublicId,
                                            @PathParam("scanId") String scanId)
      throws IOException
  {
    reportService.prepareExpandedCoverageReport(applicationPublicId, scanId);
  }
}
