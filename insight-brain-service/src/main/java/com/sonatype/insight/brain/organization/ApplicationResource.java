/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzErrorMsg;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(ApplicationResource.SERVICE_PATH)
public class ApplicationResource
    extends AbstractResourceWithIcon
{
  public static final String SERVICE_PATH = "rest/application";

  public static final String GET_APPLICATION_NAMES = "services/names";

  public static final String GET_APPLICATION_MANAGEMENT_SUMMARIES = "services/summary";

  public static final String GET_APPLICATION_MANAGEMENT_SUMMARY = GET_APPLICATION_MANAGEMENT_SUMMARIES + "/{applicationPublicId}";

  public static final String GET_SCAN_APPLICATION_MANAGEMENT_SUMMARY = GET_APPLICATION_MANAGEMENT_SUMMARIES
      + "/{applicationPublicId}/{scanId}";

  public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

  public static final String GET_APPLICATION_ICON_PATH = ICON_PATH + "/{applicationPublicId}";

  public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

  private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);

  private final ApplicationAdapter applicationAdapter;

  private final InsightWork work;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private ApplicationService applicationService;

  @Inject
  public ApplicationResource(final InsightWork work, final BaseUrl baseUrl,
      final SaasClient client, final PolicyEvaluationUtils policyEvaluationUtils,
      final ApplicationAdapter applicationAdapter, final ApplicationService applicationService)
  {
    super(client, baseUrl);
    this.work = work;
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.applicationAdapter = applicationAdapter;
    this.applicationService = applicationService;
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
    final List<ApplicationDTO> applications = applicationAdapter.convert(applicationService
        .getApplications());
    return applications;
  }

  /**
   * @since 1.4
   *
   * @Path changed in 1.6 from SERVICE_PATH to GET_APPLICATION_MANAGEMENT_SUMMARIES
   */
  @GET
  @Path(GET_APPLICATION_MANAGEMENT_SUMMARIES)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries() {
    final List<Application> applications = applicationService.getApplications();

    final List<ApplicationManagementSummaryDTO> applicationManagements = getApplicationManagementSummaries(
        applications);

    return applicationManagements;
  }

  @GET
  @Path(GET_APPLICATION_NAMES)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getApplicationNames() {
    return applicationService.getApplicationNames();
  }

  /**
   * @since 1.6
   */
  @GET
  @Path(GET_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationDTO getApplication(@PathParam("applicationPublicId") final String applicationPublicId)
  {
    Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
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
    final Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application);
  }

  /**
   * Get an ApplicatinoManagementSummary containing only the information for a specific scan.
   * 
   * @since 1.7
   */
  @GET
  @Path(GET_SCAN_APPLICATION_MANAGEMENT_SUMMARY)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationManagementSummaryDTO getApplicationManagementSummary(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @PathParam("scanId") final String scanId)
  {
    final Application application = applicationService.getApplicationByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application, scanId);
  }

  /**
   * @since 1.4
   */
  @Override
  @GET
  @Path(GENERATE_ICON_PATH)
  @Produces("image/png")
  public Response generateIcon(@PathParam("hashcode") final String hashcode, @Context final HttpServletRequest req)
      throws IOException
  {
    return super.generateIcon(hashcode, req);
  }

  @GET
  @Path(GET_APPLICATION_ICON_PATH)
  @Produces("image/png")
  public Response getIcon(@PathParam("applicationPublicId") final String applicationPublicId)
      throws IOException
  {
    String applicationId = null;
    Application application = applicationService.getApplicationByPublicId(applicationPublicId);
    if (application != null) {
      applicationId = application.getId();
    }
    return super.getIcon(applicationId, work.getApplicationIconDir());
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used for AJAX calls since
   * its return type is a JSON object.
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(ICON_PATH)
  @Authorize(permission = Permission.WRITE)
  public void setIcon(
      @FormDataParam("applicationId") @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource, @FormDataParam("robotHash") String robotHash,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException
  {
    super.setIcon(applicationId, work.getApplicationIconDir(), hasRobotSource, robotHash, uploadedInputStream,
        fileDetail);
  }

  /**
   * This is one of two service methods used for editing and adding icons. This method is used by angular ng-upload
   * and returns an empty string for success and the error message otherwise
   * 
   * @return String containing an error message, if any
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path(ICON_PATH_SYNC)
  @Authorize(permission = Permission.WRITE)
  @AuthzErrorMsg
  public String setIconSync(
      @FormDataParam("applicationId") @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource, @FormDataParam("robotHash") String robotHash,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail)
  {
    return super.setIconSync(applicationId, work.getApplicationIconDir(), hasRobotSource, robotHash,
        uploadedInputStream, fileDetail);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationDTO addApplication(Application application) {
    application = applicationService.addApplication(application);
    return applicationAdapter.convert(application);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationDTO updateApplication(Application application) {
    application = applicationService.updateApplication(application);
    return applicationAdapter.convert(application);
  }

  @DELETE
  @Path(GET_APPLICATION_PATH)
  public void deleteApplication(
      @PathParam("applicationPublicId") final String applicationPublicId)
      throws IOException
  {
    applicationService.deleteApplicationByPublicId(applicationPublicId);
  }

  private List<ApplicationManagementSummaryDTO> getApplicationManagementSummaries(final List<Application> applications)
  {

    // Create the summary DTOs from the applications
    final List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    // Now evaluate the policy for each
    for (ApplicationManagementSummaryDTO applicationManagement : applicationManagementSummaryDTOs) {
      loadPolicyEvaluations(applicationManagement);
    }

    return applicationManagementSummaryDTOs;
  }

  private ApplicationManagementSummaryDTO getApplicationManagementSummary(final Application application) {
    final ApplicationManagementSummaryDTO applicationManagement = applicationAdapter
        .createApplicationManagementSummary(application);
    loadPolicyEvaluations(applicationManagement);

    return applicationManagement;
  }

  private ApplicationManagementSummaryDTO getApplicationManagementSummary(final Application application, String scanId)
  {
    final String applicationPublicId = application.getPublicId();
    final String applicationId = application.getId();
    log.debug("Found application with public id {}", applicationPublicId);

    PolicyEvaluation evaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(applicationId, scanId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate requested scan");
    }
    ApplicationManagementSummaryDTO summary = applicationAdapter
        .createApplicationManagementSummary(application);
    summary.setPolicyEvaluations(Collections.singletonMap(evaluation.getStageTypeId(), evaluation));
    return summary;
  }

  private void loadPolicyEvaluations(final ApplicationManagementSummaryDTO applicationManagement) {

    final String applicationPublicId = applicationManagement.getPublicId();
    log.debug("Found application with public id {}", applicationPublicId);

    File[] scans = work.getScanDir(applicationManagement.getId()).listFiles();
    applicationManagement.setScansCount(scans != null ? scans.length : 0);

    final List<PolicyEvaluation> policyEvaluationList = getMostRecentPolicyEvaluations(applicationManagement.getId());
    Map<String, PolicyEvaluation> policyEvaluations = new HashMap<String, PolicyEvaluation>();
    Map<String, PolicyEvaluationResult> policyEvaluationResults = new HashMap<String, PolicyEvaluationResult>();
    for (PolicyEvaluation policyEvaluation : policyEvaluationList) {
      policyEvaluations.put(policyEvaluation.getStageTypeId(), policyEvaluation);

      List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(policyEvaluation);
      final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
      policyEvaluationResult.setAlerts(alerts);
      policyEvaluationUtils.calculateCounters(policyEvaluationResult);

      // Alerts are not needed by the Application Management UI and greatly bloat the JSON response
      policyEvaluationResult.setAlerts(null);

      policyEvaluationResults.put(policyEvaluation.getStageTypeId(), policyEvaluationResult);
    }

    applicationManagement.setPolicyEvaluations(policyEvaluations);
    applicationManagement.setPolicyEvaluationsResults(policyEvaluationResults);
  }

  private List<PolicyEvaluation> getMostRecentPolicyEvaluations(final String appId) {
    final List<PolicyEvaluation> policyEvaluations = new ArrayList<PolicyEvaluation>();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (StageType stageType : StageTypes.getAll()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastByApplicationIdAndStageId(appId, stageType.getId());
      if (eval != null) {
        policyEvaluations.add(eval);
      }
    }
    return policyEvaluations;
  }

  @Override
  protected String getDefaultIconFilename() {
    return "defaulticon_application.png";
  }
}
