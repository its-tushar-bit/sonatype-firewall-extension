/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
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

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Path(ApplicationResource.RESOURCE_PATH)
public class ApplicationResource
    extends AbstractResourceWithIcon
{
  public static final String RESOURCE_PATH = "rest/application";

  public static final String GET_APPLICATION_NAMES = "services/names";

  public static final String GET_APPLICATION_MANAGEMENT_SUMMARIES = "services/summary";

  public static final String GET_APPLICATION_MANAGEMENT_SUMMARY = GET_APPLICATION_MANAGEMENT_SUMMARIES
      + "/{applicationPublicId}";

  public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

  static final String GET_APPLICATION_LEGAL_REVIEWER_PATH = "legalReviewer/{applicationPublicId}";

  public static final String GET_APPLICATION_ICON_PATH = ICON_PATH + "/{applicationPublicId}";

  public static final String SET_APPLICATION_ICON_PATH = ICON_PATH + "/{applicationId}";

  public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

  public static final String GET_LATEST_REPORT_INFO_PATH =
      "{applicationPublicId}/{stageTypeId}/latestReportInformation";

  private final InsightWork work;

  private ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationManagementService applicationManagementService;

  private final ApplicationAdapter applicationAdapter;

  @Inject
  public ApplicationResource(
      final InsightWork work,
      final BaseUrl baseUrl,
      final RobotImageService robotImageService,
      final ApplicationService applicationService,
      final NgUploadResponseGenerator ngUploadResponseGenerator,
      final OrganizationDAO organizationDAO,
      final ApplicationManagementService applicationManagementService,
      final ApplicationAdapter applicationAdapter)
  {
    super(baseUrl, ngUploadResponseGenerator, robotImageService);
    this.work = work;
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.applicationManagementService = applicationManagementService;
    this.applicationAdapter = applicationAdapter;
  }

  @GET
  @Path(VALIDATE_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public String validateApplicationPublicId(@PathParam("applicationPublicId") final String applicationPublicId) {
    return applicationService.validateApplicationPublicId(applicationPublicId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationDTO> getApplications() {
    List<Application> apps = applicationService.getAllWithoutRelatedRepositories();
    final List<ApplicationDTO> applications = applicationAdapter.convert(apps);
    return applications;
  }

  /**
   * @since 1.4
   *
   * @Path changed in 1.6 from RESOURCE_PATH to GET_APPLICATION_MANAGEMENT_SUMMARIES
   */
  @GET
  @Path(GET_APPLICATION_MANAGEMENT_SUMMARIES)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries(
      @QueryParam("nameFilter") String nameFilter,
      @QueryParam("order") @DefaultValue("APP_NAME_ASC") ApplicationManagementSummaryOrder order,
      @QueryParam("page") Integer page,
      @QueryParam("pageSize") Integer pageSize)
  {
    return applicationManagementService.getApplicationManagementSummaries(nameFilter, order, page, pageSize);
  }

  /**
   * @since 1.179.0
   */
  @GET
  @Path(GET_APPLICATION_NAMES)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getApplicationNamesForEvaluateComponent() {
    return applicationService.getApplicationNamesForEvaluateComponent();
  }

  @GET
  @Path(GET_LATEST_REPORT_INFO_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public LatestReportInformation getLatestReportIdentifier(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("stageTypeId") final String stageTypeId)
  {
    return applicationService.getLatestReportInformation(applicationPublicId, stageTypeId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path(GET_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationDTO getApplication(@PathParam("applicationPublicId") final String applicationPublicId) {
    Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
    return applicationAdapter.convert(application);
  }

  /**
   * @since 1.167
   */
  @GET
  @Path(GET_APPLICATION_LEGAL_REVIEWER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationDTO getApplicationByPublicIdForLegalReviewer(
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    Application application = applicationService.getApplicationByPublicIdForLegalReviewer(applicationPublicId);
    return applicationAdapter.convert(application);
  }

  /**
   * @since 1.4
   *
   * @Path changed in 1.6 from GET_APPLICATION_PATH to GET_APPLICATION_MANAGEMENT_SUMMARY
   */
  @GET
  @Path(GET_APPLICATION_MANAGEMENT_SUMMARY)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationManagementSummaryDTO getApplicationManagementSummary(
      @PathParam("applicationPublicId") final String applicationPublicId)
  {
    return applicationManagementService.getApplicationManagementSummary(applicationPublicId);
  }

  /**
   * @since 1.4
   */
  @Override
  @GET
  @Path(GENERATE_ICON_PATH)
  @Produces("image/png")
  public Response generateIcon(@PathParam("hashcode") final String hashcode) {
    return super.generateIcon(hashcode);
  }

  @GET
  @Path(GET_APPLICATION_ICON_PATH)
  @Produces("image/png")
  public Response getIcon(@PathParam("applicationPublicId") final String applicationPublicId) throws IOException {
    Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
    return super.getIcon(application.getId(), work.getApplicationIconDir());
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
   * its return type is a JSON object.
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Path(SET_APPLICATION_ICON_PATH)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_APPLICATION_ICON)
  public Response setIcon(
      @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
      @Context HttpHeaders headers,
      @AuthzContext(Key.APPLICATION_ID) @PathParam("applicationId") String applicationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource,
      @FormDataParam("hashcode") String hashcode,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @QueryParam("noFormData") boolean noFormData) throws Exception
  {
    return super.setIcon(applicationId, work.getApplicationIconDir(), hasRobotSource, hashcode, uploadedInputStream,
        fileDetail, csrfToken, headers, noFormData);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION)
  public ApplicationDTO addApplication(Application application) {
    AuditData.get().setParentOrganization(organizationDAO.getById(application.getParentOwnerId()));
    application = applicationService.addApplication(application);
    return applicationAdapter.convert(application);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_APPLICATION)
  public ApplicationDTO updateApplication(Application application) {
    AuditData.get().setApplicationWithDetails(application);
    application = applicationService.updateApplication(application);
    return applicationAdapter.convert(application);
  }

  @DELETE
  @Path(GET_APPLICATION_PATH)
  @Audited(AuditEvent.DELETE_APPLICATION)
  public void deleteApplication(@PathParam("applicationPublicId") final String applicationPublicId) throws IOException {
    applicationService.deleteApplicationByPublicId(applicationPublicId);
  }

  @Override
  protected String getDefaultIconFilename(String ownerId) {
    return "defaulticon_application.png";
  }
}
