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
 * Two modes are supported on the same endpoint, distinguished by the {@code evaluationMode}
 * form field:
 * <ul>
 * <li>{@code ASYNCHRONOUS} (default; field absent or any value other than SYNCHRONOUS) —
 * existing behaviour: queue the scan for background processing and return 202 Accepted.</li>
 * <li>{@code SYNCHRONOUS} (CLM-39870) — evaluate the scan inline on the servlet thread and
 * return 200 OK with a {@link HostedEvaluationResult} body. NXRM uses this to decide
 * whether to commit the artifact to the repository or return HTTP 403 to the developer.</li>
 * </ul>
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

  static final String EVALUATION_MODE_SYNCHRONOUS = "SYNCHRONOUS";

  // HTTP 422 Unprocessable Entity. JAX-RS Status enum lacks this constant in our Jakarta
  // baseline; named here to keep the magic number out of the response builder.
  private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

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
   * Accepts a scan.xml.gz upload from NXRM. Behaviour depends on {@code evaluationMode}:
   * <ul>
   * <li>{@code SYNCHRONOUS}: evaluate inline, return 200 OK with {@link HostedEvaluationResult}.</li>
   * <li>otherwise: queue for async processing, return 202 Accepted with {@link HostedComponentScanResponse}.</li>
   * </ul>
   *
   * @param repositoryManagerInstanceId the NXRM repository manager instance ID
   * @param repositoryPublicId the public name of the hosted repository (e.g. "maven-releases")
   * @param componentId the unique component identifier within the repository
   * @param purl the package URL of the component
   * @param policyEvaluationStage the stage at which to evaluate (async only; sync always uses hosted)
   * @param evaluationMode optional; {@code "SYNCHRONOUS"} switches to sync enforcement
   * @param correlationId optional per-deploy UUID supplied by NXRM; echoed back in the response
   * @param requestedBy optional NXRM-authenticated principal; audited
   * @param client optional client tool identifier (e.g. "maven", "npm")
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
      @FormDataParam("purl") final String purl,
      @FormDataParam("policyEvaluationStage") final String policyEvaluationStage,
      @FormDataParam("evaluationMode") final String evaluationMode,
      @FormDataParam("correlationId") final String correlationId,
      @FormDataParam("requestedBy") final String requestedBy,
      @FormDataParam("client") final String client,
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

    log.debug("Received scan upload for repositoryManagerInstanceId={}, repositoryPublicId={}, componentId={}, "
        + "purl={}, evaluationMode={}, correlationId={}, client={}",
        repositoryManagerInstanceId, repositoryPublicId, componentId, purl, evaluationMode, correlationId, client);

    Repository repository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      throw new NotFoundException("Repository not found: repositoryManagerInstanceId=" + repositoryManagerInstanceId
          + ", repositoryPublicId=" + repositoryPublicId);
    }

    if (EVALUATION_MODE_SYNCHRONOUS.equalsIgnoreCase(evaluationMode)) {
      return handleSynchronous(repository, componentId, purl, policyEvaluationStage, scanFile,
          correlationId, requestedBy, client);
    }
    return handleAsynchronous(repository, componentId, purl, policyEvaluationStage, scanFile);
  }

  private Response handleAsynchronous(
      final Repository repository,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final File scanFile) throws Exception
  {
    String jobId;
    try {
      jobId = hostedComponentEvaluationService.queueScan(
          repository.getId(), componentId, purl, policyEvaluationStage, scanFile);
    }
    catch (ScanFileTooLargeException e) {
      throw new WebApplicationException(
          Response.status(Status.REQUEST_ENTITY_TOO_LARGE).entity(e.getMessage()).build());
    }

    return Response.status(Status.ACCEPTED).entity(new HostedComponentScanResponse(componentId, jobId)).build();
  }

  private Response handleSynchronous(
      final Repository repository,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final File scanFile,
      final String correlationId,
      final String requestedBy,
      final String client) throws Exception
  {
    try {
      HostedEvaluationResult result = hostedComponentEvaluationService.evaluateSynchronously(
          repository, componentId, purl, policyEvaluationStage, scanFile,
          correlationId, requestedBy, client);
      return Response.status(Status.OK).entity(result).build();
    }
    catch (ScanFileTooLargeException e) {
      throw new WebApplicationException(
          Response.status(Status.REQUEST_ENTITY_TOO_LARGE).entity(e.getMessage()).build());
    }
    catch (UnscannableArtifactException e) {
      // 422 Unprocessable Entity — well-formed scan, but no fingerprint could be extracted
      // (sources jars, javadoc, signature files, etc.). Distinguishes from a true 500 and
      // lets NXRM treat it as "skip enforcement, allow upload" instead of "IQ unavailable".
      // The @HttpStatusCode(422) on the exception class drives the same outcome via the
      // global ErrorResponseGenerator; this explicit catch shapes the response body so
      // NXRM's IQEvaluationResponse parser can consume a structured error envelope.
      log.info("Sync enforcement: unscannable artifact componentId={} correlationId={}: {}",
          componentId, correlationId, e.getMessage());
      return Response.status(HTTP_UNPROCESSABLE_ENTITY)
          .entity(new UnscannableArtifactResponse(
              "UNSCANNABLE_ARTIFACT", e.getMessage(), correlationId))
          .build();
    }
  }

  /**
   * Structured error body for HTTP 422 responses on {@link UnscannableArtifactException}.
   * Caller (NXRM) inspects {@code errorCode} to distinguish from generic 4xx/5xx errors
   * and decide whether to fail the upload or pass it through.
   */
  static record UnscannableArtifactResponse(String errorCode, String message, String correlationId)
  {
  }
}
