/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
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
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(ApplicationResource.SERVICE_PATH)
public class ApplicationResource
    extends AbstractResourceWithIcon
{
  public static final String SERVICE_PATH = "rest/application";

  public static final String GET_APPLICATION_NAMES = "services/names";

  public static final String GET_APPLICATION_PATH = "{applicationPublicId}";

  public static final String GET_APPLICATION_ICON_PATH = ICON_PATH + "/{applicationPublicId}";

  public static final String VALIDATE_PATH = "validate/{applicationPublicId}";

  private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);

  private static final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final InsightWork work;

  private final CLMLicenseManager licenseManager;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  @Inject
  public ApplicationResource(final InsightWork work, final BaseUrl baseUrl, final CLMLicenseManager licenseManager,
      final SaasClient client, final PolicyEvaluationUtils policyEvaluationUtils)
  {
    this.work = work;
    this.licenseManager = licenseManager;
    this.policyEvaluationUtils = policyEvaluationUtils;
  }

  @GET
  @Path(VALIDATE_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public String validateApplicationPublicId(@PathParam("applicationPublicId") final String applicationPublicId) {
    return validateApplicationPublicIdInternal(applicationPublicId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationManagementSummary> getApplications() throws IOException {
    final List<ApplicationManagementSummary> applicationManagements = new ArrayList<ApplicationManagementSummary>();
    final List<Application> applications = applicationDAO.getAll();
    for (Application application : applications) {
      applicationManagements.add(getApplicationManagementSummary(application));
    }

    return applicationManagements;
  }

  @GET
  @Path(GET_APPLICATION_NAMES)
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getApplicationNames() {
    final List<Application> applications = applicationDAO.getAll();
    Map<String, String> applicationPublicIDNamePairs = new LinkedHashMap<String, String>();

    for (Application application : applications) {
      log.debug("Found application with public id {}", application.getPublicId());
      applicationPublicIDNamePairs.put(application.getPublicId(), application.getName());
    }

    return applicationPublicIDNamePairs;
  }

  @GET
  @Path(GET_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationManagementSummary getApplication(@PathParam("applicationPublicId") final String applicationPublicId)
      throws IOException
  {
    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    return getApplicationManagementSummary(application);
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
  public Response getIcon(@PathParam("applicationPublicId") final String applicationPublicId) throws IOException {
    String applicationId = null;
    Application application = applicationDAO.getByPublicId(applicationPublicId);
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
  public void setIcon(@FormDataParam("applicationId") String applicationId,
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
  public String setIconSync(@FormDataParam("applicationId") String applicationId,
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
  public ApplicationManagementSummary addApplication(Application application) throws IOException {
    int appLimit = licenseManager.getApplicationCountLimit();

    if (applicationDAO.getAll().size() >= appLimit) {
      throw new PaymentRequiredException("You have exceeded the licensed limit of " + appLimit + " applications.");
    }

    if (application.getOrganizationId() == null) {
      throw new InvalidApplicationException("Applications must have a parent organization.");
    }

    applicationDAO.insert(application);

    return getApplicationManagementSummary(application);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationManagementSummary updateApplication(Application application) throws IOException {
    if (application.getOrganizationId() == null) {
      throw new InvalidApplicationException("Applications must have a parent organization.");
    }

    List<Lock> readLocks = new ArrayList<Lock>();
    try {
      Application existingApp = applicationDAO.getByIdNotNull(application.getId());
      if (existingApp.getOrganizationId() == null) {
        PolicyDAO policyDAO = new PolicyDAO(work.getWorkDir());
        policyDAO.validateNamesWithinHierarchy(application.getOrganizationId(), application.getId(), readLocks);
      }

      applicationDAO.update(application);
    }
    finally {
      PolicyDAO.unlock(readLocks);
    }

    return getApplicationManagementSummary(application);
  }

  @DELETE
  @Path(GET_APPLICATION_PATH)
  public void deleteApplication(@PathParam("applicationPublicId") final String applicationPublicId) throws IOException {
    EntityManager em = applicationDAO.createEntityManager();
    try {
      deleteApplication(em, applicationPublicId);
    }
    finally {
      AbstractDAO.close(em);
    }
  }

  public void deleteApplication(final EntityManager em, final String applicationPublicId) throws IOException {
    Application application = applicationDAO.getByPublicIdNotNull(em, applicationPublicId);

    PolicyDAO policyDAO = new PolicyDAO(work.getWorkDir());
    policyDAO.deleteByOwnerId(application.getId()); // as of 1.6, not stored in database

    FileUtils.deleteDirectory(work.getScanDir(application.getId()));
    FileUtils.deleteDirectory(work.getAuditDir(application.getId()));
    FileUtils.deleteDirectory(work.getReportDir(application.getId()));

    // delete application last, this way the operation can be retried later if anything goes wrong
    applicationDAO.deleteWithIcon(em, application, work.getApplicationIconDir());
  }

  private ApplicationManagementSummary getApplicationManagementSummary(final Application application)
      throws IOException
  {
    final String applicationPublicId = application.getPublicId();
    final String applicationId = application.getId();
    log.debug("Found application with public id {}", applicationPublicId);

    final ApplicationManagementSummary applicationManagement = ApplicationManagementSummary
        .fromApplication(application);
    File[] scans = work.getScanDir(applicationManagement.getId()).listFiles();
    applicationManagement.setScansCount(scans != null ? scans.length : 0);

    final List<PolicyEvaluation> policyEvaluationList = work.getMostRecentPolicyEvaluations(application.getId());
    Map<String, PolicyEvaluation> policyEvaluations = new HashMap<String, PolicyEvaluation>();
    Map<String, PolicyEvaluationResult> policyEvaluationResults = new HashMap<String, PolicyEvaluationResult>();
    for (PolicyEvaluation policyEvaluation : policyEvaluationList) {
      final Stage stage = policyEvaluation.getStage();
      policyEvaluations.put(stage.getStageTypeId(), policyEvaluation);

      List<PolicyAlert> alerts = policyEvaluationUtils.findPolicyAlerts(applicationPublicId, applicationId,
          policyEvaluation.getScanId());
      final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
      policyEvaluationResult.setAlerts(alerts);
      policyEvaluationUtils.calculateCounters(policyEvaluationResult);

      // Alerts are not needed by the Application Management UI and greatly bloat the JSON response
      policyEvaluationResult.setAlerts(null);

      policyEvaluationResults.put(stage.getStageTypeId(), policyEvaluationResult);
    }

    applicationManagement.setPolicyEvaluations(policyEvaluations);
    applicationManagement.setPolicyEvaluationsResults(policyEvaluationResults);

    return applicationManagement;
  }

  public static String validateApplicationPublicIdInternal(String applicationPublicId) {
    if (applicationDAO.getByPublicId(applicationPublicId) == null) {
      return "Invalid application id " + applicationPublicId;
    }

    log.debug("Found application with public id {}", applicationPublicId);
    return "OK";
  }

  @Override
  protected String getDefaultIconFilename() {
    return "defaulticon_application.png";
  }
}
