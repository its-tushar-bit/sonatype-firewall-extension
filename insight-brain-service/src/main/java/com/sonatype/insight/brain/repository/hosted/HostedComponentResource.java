/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import org.apache.commons.lang3.StringUtils;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for accepting scan.xml.gz uploads from NXRM hosted repositories.
 * <p>
 * Queues the scan for background processing and returns 202 Accepted.
 */
@Named
@Timed
@Path(HostedComponentResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class HostedComponentResource
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentResource.class);

  public static final String RESOURCE_PATH = "api/v2/repositories";

  static final String UPLOAD_PATH = "{repositoryManagerId}/{repositoryId}/components";

  private final HostedComponentEvaluationService hostedComponentEvaluationService;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public HostedComponentResource(
      final HostedComponentEvaluationService hostedComponentEvaluationService,
      final RepositoryDAO repositoryDAO)
  {
    this.hostedComponentEvaluationService = hostedComponentEvaluationService;
    this.repositoryDAO = repositoryDAO;
  }

  /**
   * Accepts a scan.xml.gz upload from NXRM and queues it for background processing.
   *
   * @param repositoryManagerInstanceId the NXRM repository manager instance ID
   * @param repositoryPublicId the public name of the hosted repository (e.g. "maven-releases")
   * @param componentId the unique component identifier within the repository
   * @param policyEvaluationStage the stage at which to evaluate
   * @param scanFile the pre-formed scan.xml.gz file received from NXRM
   */
  @POST
  @Path(UPLOAD_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public Response uploadScan(
      @PathParam("repositoryManagerId") final String repositoryManagerInstanceId,
      @PathParam("repositoryId") final String repositoryPublicId,
      @FormDataParam("componentId") final String componentId,
      @FormDataParam("policyEvaluationStage") final String policyEvaluationStage,
      @FormDataParam("scanFile") final File scanFile) throws Exception
  {
    if (!SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.getId() + " feature is disabled");
    }
    if (StringUtils.isBlank(repositoryPublicId)) {
      throw new BadRequestException("Missing required path parameter: repositoryId");
    }
    if (StringUtils.isBlank(componentId)) {
      throw new BadRequestException("Missing required parameter: componentId");
    }
    if (scanFile == null) {
      throw new BadRequestException("Missing required parameter: scanFile");
    }

    log.debug("Received scan upload for repositoryManagerInstanceId={}, repositoryPublicId={}, componentId={}",
        repositoryManagerInstanceId, repositoryPublicId, componentId);

    Repository repository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Repository not found: repositoryManagerInstanceId=" + repositoryManagerInstanceId
          + ", repositoryPublicId=" + repositoryPublicId);
    }

    return handleAsynchronous(repository, componentId, policyEvaluationStage, scanFile);
  }

  private Response handleAsynchronous(
      final Repository repository,
      final String componentId,
      final String policyEvaluationStage,
      final File scanFile) throws Exception
  {
    String jobId;
    try {
      jobId = hostedComponentEvaluationService.queueScan(
          repository.getId(), componentId, policyEvaluationStage, scanFile);
    }
    catch (ScanFileTooLargeException e) {
      throw new WebApplicationException(
          Response.status(Status.REQUEST_ENTITY_TOO_LARGE).entity(e.getMessage()).build());
    }

    return Response.status(Status.ACCEPTED).entity(new HostedComponentScanResponse(componentId, jobId)).build();
  }
}
