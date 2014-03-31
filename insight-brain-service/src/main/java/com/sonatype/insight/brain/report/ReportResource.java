/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphService;
import com.sonatype.insight.brain.saas.ComponentDetailsLoader;
import com.sonatype.insight.brain.security.AuditUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.MediaTypeUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.cache.CacheBuilder;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ReportResource.SERVICE_PATH)
@Named
public class ReportResource
{
  public static final String SERVICE_PATH = "rest/report/{applicationPublicId}/{scanId}";

  public static final String PRINT_PATH = "printReport";

  public static final String DOWNLOAD_BUNDLE_PATH = "downloadBundle";

  private static final Logger log = LoggerFactory.getLogger(ReportResource.class);

  private static final ConcurrentMap<String, Lock> LOCK_TABLE = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  private static final long YEAR = (long) 365 * 24 * 60 * 60 * 1000;

  static final ConcurrentMap<String, Integer> MODIFICATION_COUNTS = CacheBuilder.newBuilder().maximumSize(8192)
      .<String, Integer> build().asMap();

  private final InsightWork work;

  private final BaseUrl baseUrl;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private ApplicationAdapter applicationAdapter;

  private final ReportDownloader reportDownloader;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final ReportDataService reportDataService;

  private final ReleaseGraphService releaseGraphService;

  private final ComponentDetailsLoader componentDetailsLoader;

  @Inject
  public ReportResource(final ReportDownloader reportDownloader, final PolicyEvaluationUtils policyEvaluationUtils,
      InsightWork work, BaseUrl baseUrl, ApplicationAdapter applicationAdapter, ReportDataService reportDataService,
      ReleaseGraphService releaseGraphService, ComponentDetailsLoader componentDetailsLoader)
  {
    this.reportDownloader = reportDownloader;
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.work = work;
    this.baseUrl = baseUrl;
    this.applicationAdapter = applicationAdapter;
    this.reportDataService = reportDataService;
    this.releaseGraphService = releaseGraphService;
    this.componentDetailsLoader = componentDetailsLoader;
  }

  /**
   * @deprecated Support legacy CI instances (pre 2.11) and Nexus CLM plugins that persisted a report link obtained from
   *             CLM 1.6-
   */
  @Deprecated
  @GET
  @Path("embedReport/{path:.*}")
  public Response embedReport(@PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId, @PathParam("path") final String path,
      @Context final HttpServletRequest httpRequest)
  {
    if ("index.html".equals(path) || path.isEmpty()) {
      UriBuilder uriBuilder = baseUrl.redirect();
      uriBuilder.path(UserInterfaceLinksResource.SERVICE_PATH + "/" + UserInterfaceLinksResource.REPORT_PATH);
      
      StringBuilder sb = new StringBuilder();
      sb.append("<html>");
      sb.append("<body style='font: 12px Verdana, Helvetica;margin-top:50px;'>");
      sb.append("<h1>This report has moved</h1>");
      sb.append("<p>Your Sonatype CLM Server was updated, causing the report formerly at this location to be moved ");
      sb.append("<a target='_blank' href='" + uriBuilder.build(applicationPublicId, scanId) + "'>here</a></p>");
      sb.append("</body>");
      sb.append("</html>");
      
      final ResponseBuilder response = Response.ok(sb.toString());
      response.type(MediaTypeUtils.byName("index.html"));
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
  @Path("browseReport/{path:.*}")
  @Authorize(permission = Permission.READ)
  public Response browseReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId, @PathParam("path") final String path,
      @Context final HttpServletRequest httpRequest) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final String name = Report.toEntryName(path);
    final File reportFile = fetchReport(reportDownloader, work, appId, scanId, false);
    ReportEntry reportEntry = null;
    try {
      reportEntry = Report.getEntry(reportFile, name);
    }
    catch (final Exception e) {
      log.warn("Problem embedding report: " + e.getMessage(), e);
    }
    if (reportEntry != null) {
      final long ifModifiedSince = httpRequest.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
      if (ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000) {
        return Response.status(304).build();
      }
      final ResponseBuilder response = Response.ok(reportEntry.buf);
      response.lastModified(new Date(reportEntry.time));
      response.type(MediaTypeUtils.byName(name));
      if (!name.endsWith(".json")) {
        response.expires(new Date(System.currentTimeMillis() + YEAR));
      }
      else {
        // JSON files should always check with the server to ensure they are updated. A 304 will be returned if
        // they don't need updating
        response.expires(new Date());
      }
      return response.build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  /**
   * Re-evaluates the policy for a scan. The policy must have been evaluated for the given scan at least once. This
   * method should not send policy evaluation notifications.
   */
  @GET
  @Path("reevaluatePolicy")
  @Authorize(permission = Permission.READ)
  public Response reevaluatePolicy(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(appId, scanId);

    if (policyEvaluation == null) {
      throw new BadRequestException("Policy evaluation for scan " + scanId + " does not exist on the server");
    }

    policyEvaluationUtils.evaluate(applicationPublicId, scanId, new Stage(policyEvaluation.getStageTypeId()));

    return Response.ok().build();
  }

  @GET
  @Path(PRINT_PATH)
  @Produces("application/pdf")
  @Authorize(permission = Permission.READ)
  public Response printReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId, @QueryParam("projectName") final String projectName,
      @QueryParam("buildNumber") final int buildNumber) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();
    ContactDTO contact = applicationAdapter.getContact(application.getContactInternalName());

    final File reportFile = fetchReport(reportDownloader, work, appId, scanId, true);

    final ResponseBuilder response = Response.ok();

    Report.printPdf(reportFile, StringUtils.defaultString(projectName, "clm"), buildNumber, contact, response);

    return response.build();
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
  @Authorize(permission = Permission.READ)
  public Response downloadBundle(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = fetchReport(reportDownloader, work, app.getId(), scanId, true);
    String filename = "report-" + scanId + ".zip";

    Properties templateProps = Report.getTemplateProperties(reportFile);
    String cipDetailsPath = templateProps.getProperty("cip.details.path", "");
    String cipListPath = templateProps.getProperty("cip.list.path", "");

    ContactDTO contact = applicationAdapter.getContact(app.getContactInternalName());
    File pdfFile = Report.printPdf(reportFile, "", 0, contact);

    ReportData reportData = reportDataService.getData(applicationPublicId, scanId);
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(app.getId(), scanId);
    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(policyEvaluation);

    File updatedFile = File.createTempFile("report", "zip");
    try (ReportBundleUpdater updater = new ReportBundleUpdater(reportFile, updatedFile)) {
      updater.remove("detail.rptdesign");
      updater.add("report.pdf", pdfFile);
      updater.add("components.json", reportData);

      for (ReportData.Component component : reportData.components) {
        ReportData.Coordinates gav = component.mavenCoordinates;
        if (gav != null) {
          String imagePath = "release-graph/" + gav.groupId + "/" + gav.artifactId + "/" + gav.version + ".png";
          byte[] imageData = releaseGraphService.getImage(applicationPublicId, scanId, gav.groupId, gav.artifactId,
              gav.version);
          updater.add(imagePath, imageData);
        }
      }

      File[] cachedFiles = Report.getCacheDir(reportFile).listFiles();
      if (cachedFiles != null) {
        for (File cachedFile : cachedFiles) {
          updater.add(cachedFile.getName(), cachedFile);
        }
      }

      try (ZipFile reportZip = new ZipFile(reportFile)) {
        for (Enumeration<? extends ZipEntry> en = reportZip.entries(); en.hasMoreElements();) {
          ZipEntry entry = en.nextElement();
          if (entry.isDirectory()) {
            continue;
          }
          if (!cipDetailsPath.isEmpty() && entry.getName().startsWith(cipDetailsPath)) {
            final ComponentDetails hdsDetails = JsonUtils
                .parse(reportZip.getInputStream(entry), ComponentDetails.class);
            ComponentDetails clmDetails = componentDetailsLoader.getComponentDetails(hdsDetails.getGroupId(),
                hdsDetails.getArtifactId(), hdsDetails.getVersion(), hdsDetails.getHash(), hdsDetails.getMatchState(),
                new ComponentDetailsLoader.HostedDataServicesSource()
                {
                  @Override
                  public ComponentDetails getDetails() throws IOException {
                    return hdsDetails;
                  }
                });
            componentDetailsLoader.augmentComponentDetails(app, clmDetails);
            clmDetails.setPolicyAlerts(getAlertsForComponent(clmDetails.getHash(), alerts));
            updater.add(entry.getName(), clmDetails);

            if (!cipListPath.isEmpty()
                && IdentificationSource.MANUAL.getId().equals(clmDetails.getIdentificationSource())) {
              String listPath = cipListPath + clmDetails.getGroupId() + "/" + clmDetails.getArtifactId() + "/"
                  + clmDetails.getVersion() + ".json";
              if (reportZip.getEntry(listPath) == null && !updater.contains(listPath)) {
                // CIP expects this to be an empty (!) array for every GAV but the HDS doesn't know about claimed components
                ComponentDetailsList list = new ComponentDetailsList();
                list.setList(Collections.<ComponentDetails> emptyList());
                updater.add(listPath, list);
              }
            }
          }
          if (!cipListPath.isEmpty() && entry.getName().startsWith(cipListPath)) {
            final ComponentDetailsList list = JsonUtils.parse(reportZip.getInputStream(entry),
                ComponentDetailsList.class);
            for (ComponentDetails details : list.getList()) {
              componentDetailsLoader.augmentComponentDetails(app, details);
            }
            updater.add(entry.getName(), list);
          }
        }
      }
    }

    final ResponseBuilder response = Response.ok();
    response.entity(updatedFile);
    response.header("Content-Disposition", "attachment; filename=" + UrlUtils.encodeUrlComponent(filename));
    return response.build();
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

  @POST
  @Path("augmentData/{path}")
  @Authorize(permission = Permission.WRITE)
  public Response augmentData(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("path") final String path, @QueryParam("where") final String where,
      @Context final HttpServletRequest request, final InputStream stream)
      throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    if (path.endsWith(".json") && request.getContentLength() > 0) {
      final ContainerNode<?> data;
      try {
        data = JsonUtils.parse(IOUtil.toByteArray(stream));
      }
      finally {
        IOUtil.close(stream);
      }

      // Save the data in the audit log
      final JsonStore store = JsonUtils.fileStore(work.getAuditDir(appId));
      store.commit(path, JsonUtils.stamp(AuditUtils.findUser(), AuditUtils.findIP(request), where, data));

      if ("licenses.json".equals(path)) {
        // Save the data as license overrides
        for (int i = 0; i < data.size(); i++) {
          saveLicenseOverride(appId, data.get(i));
        }
      }

      return Response.ok().build();
    }
    return Response.status(Status.BAD_REQUEST).build();
  }

  /**
   * Supports the bulk license editor which does not use LicenseOverrideResource.
   * 
   * @since 1.6
   */
  private void saveLicenseOverride(String appId, JsonNode licenseData) {
    String groupId = licenseData.get("groupId").asText();
    String artifactId = licenseData.get("artifactId").asText();
    String version = licenseData.get("version").asText();
    String statusName = licenseData.get("status").asText();

    String licenseOverrideId = null;
    LicenseOverrideStatus status = LicenseOverrideStatus.getByName(statusName);
    JsonNode licenseOverrideJsonNode = licenseData.get("overriddenLicenses");
    if (licenseOverrideJsonNode != null) {
      licenseOverrideJsonNode = licenseOverrideJsonNode.get(0);
      if (licenseOverrideJsonNode != null) {
        String licenseOverrideName = licenseOverrideJsonNode.asText();
        licenseOverrideId = new LicenseDAO().getByNameNotNull(licenseOverrideName).getId();
      }
    }
    String comment = JsonUtils.getNullableString(licenseData.get("comment"));

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride licenseOverride = licenseOverrideDAO.getByOwnerIdAndGAV(appId, groupId, artifactId, version);
    if (licenseOverride == null) {
      licenseOverride = new LicenseOverride(appId, groupId, artifactId, version, status, licenseOverrideId, comment);
      licenseOverrideDAO.insert(licenseOverride);
    }
    else {
      licenseOverride.setStatus(status);
      licenseOverride.setLicenseId(licenseOverrideId);
      licenseOverride.setComment(comment);
      licenseOverrideDAO.update(licenseOverride);
    }
  }

  @GET
  @Path("auditLog/{path}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public Response auditLog(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("path") final String path, @QueryParam("key") final String encodedKey) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final JsonStore store = JsonUtils.fileStore(work.getAuditDir(appId));
    final ContainerNode<?> key = encodedKey != null ? JsonUtils.parse(encodedKey.getBytes("UTF-8")) : null;
    final ContainerNode<?> feed = store.history(key, path.split("[+]+"));
    if (feed != null) {
      return Response.ok(JsonUtils.generate(feed)).build();
    }

    return Response.ok().build();
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

  private static Response redirectToBrain(final BaseUrl baseUrl, final String path) {
    UriBuilder uriBuilder = baseUrl.redirect().path(path);

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  public static File fetchReport(final ReportDownloader reportDownloader, final InsightWork work, final String appId,
      final String scanId, final boolean waitForReport) throws IOException
  {
    final File reportFile = work.getReportFile(appId, scanId);
    if (reportDownloader == null && !reportFile.exists()) {
      return null;
    }
    final Lock lock = lockFor(appId, scanId);
    if (waitForReport || reportFile.exists()) {
      lock.lock(); // protect against concurrent download as well as concurrent editing of the report
    }
    else if (!lock.tryLock()) {
      throw new NotFoundException("The report for scan id " + scanId + " is still being downloaded");
    }
    try {
      if (!reportFile.exists()) {
        int attempts = 0;
        int interval = 0;

        if (waitForReport) {
          attempts = 30;
          interval = 30;
        }
        final File tempFile = FileUtils.createTempFile("temp-", ".zip", reportFile.getParentFile());
        if (!reportDownloader.downloadReport(scanId, tempFile, attempts, interval)) {
          throw new NotFoundException("Could not download the report for scan id " + scanId);
        }
        FileUtils.rename(tempFile, reportFile);
      }

      final File appAuditDir = work.getAuditDir(appId);
      int newCount = JsonUtils.fileStore(appAuditDir).modificationCount();
      Application application = new ApplicationDAO().getByIdNotNull(appId);
      File orgAuditDir = work.getAuditDir(application.getOrganizationId());
      newCount += JsonUtils.fileStore(orgAuditDir).modificationCount();
      final Integer oldCount = MODIFICATION_COUNTS.get(appId + '-' + scanId);

      if (oldCount == null || oldCount < newCount) {
        Report.deletePdf(reportFile);

        Report.applyChanges(application, reportFile, appAuditDir);

        MODIFICATION_COUNTS.put(appId + '-' + scanId, newCount);
      }

      return reportFile;
    }
    finally {
      lock.unlock();
    }
  }

  public static File getReport(final InsightWork work, final String appId, final String scanId) throws IOException {
    return fetchReport(null, work, appId, scanId, false);
  }

  public static void flushReportChanges(final String appId, final String scanId) {
    MODIFICATION_COUNTS.remove(appId + '-' + scanId);
  }

  public static void flushReportChanges() {
    MODIFICATION_COUNTS.clear();
  }

  private static Lock lockFor(final String appId, final String scanId) {
    Lock lock = LOCK_TABLE.get(appId + '-' + scanId);
    if (lock == null) {
      final Lock newLock = new ReentrantLock();
      lock = LOCK_TABLE.putIfAbsent(appId + '-' + scanId, newLock);
      if (lock == null) {
        lock = newLock;
      }
    }
    return lock;
  }

  public static String getReportPath(final String appPublicId, final String scanId) {
    String url = ReportResource.SERVICE_PATH + "/browseReport/";
    url = url.replace("{applicationPublicId}", UrlUtils.encodeUrlComponent(appPublicId));
    url = url.replace("{scanId}", UrlUtils.encodeUrlComponent(scanId));
    return url;
  }
}
