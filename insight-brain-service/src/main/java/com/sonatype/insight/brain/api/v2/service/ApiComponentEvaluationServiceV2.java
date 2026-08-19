/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

/**
 * @since 1.13.0
 */
@Named
@Singleton
public class ApiComponentEvaluationServiceV2
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentEvaluationServiceV2.class);

  private final ExecutorService executor = Executors.newFixedThreadPool(4, createThreadFactory());

  private final ApplicationDAO applicationDAO;

  private final ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ApiComponentDetailsAdapter componentDetailsAdapter;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final InsightWork work;

  private final ErrorResponseGenerator errorResponseGenerator;

  @Inject
  public ApiComponentEvaluationServiceV2(
      final ApplicationDAO applicationDAO,
      final ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2,
      final ComponentPolicyEvaluator componentPolicyEvaluator,
      final ApiComponentDetailsAdapter componentDetailsAdapter,
      final ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      final InsightWork work,
      final ErrorResponseGenerator errorResponseGenerator,
      final ShutdownHandler shutdownHandler)
  {
    this.applicationDAO = applicationDAO;
    this.apiComponentDetailsServiceV2 = apiComponentDetailsServiceV2;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentDetailsAdapter = componentDetailsAdapter;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.work = work;
    this.errorResponseGenerator = errorResponseGenerator;
    shutdownHandler.add(executor);
  }

  // Visible for testing
  ExecutorService getExecutor() {
    return executor;
  }

  @Override
  public void stop() throws Exception {
    executor.shutdown();
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    validateRequest(evaluationRequest);

    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = createEvaluationTicket(applicationId);
    AuditData.get() //
        .setData("componentCount", evaluationRequest.components.size()) //
        .setData("resultId", evaluationTicketDTO.resultId) //
        .continueAsync(executor,
            new OneTimeSystemRunnable(new ComponentEvaluationTask(evaluationTicketDTO, evaluationRequest)));

    return evaluationTicketDTO;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentEvaluationResultDTOV2 getComponentEvaluation(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String resultId) throws IOException
  {
    AuditData.get().setData("resultId", resultId);
    File componentDetailsFile = work.getComponentDetailsFile(applicationId, resultId);
    try {
      return JsonUtils.read(componentDetailsFile, ApiComponentEvaluationResultDTOV2.class);
    }
    catch (FileNotFoundException ignore) {
      throw new NotFoundException("Evaluation result not found for " + resultId);
    }
  }

  // For testing
  void setChunkSize(final int chunkSize) {
    apiComponentDetailsServiceV2.setChunkSize(chunkSize);
  }

  private void validateRequest(final ApiComponentEvaluationRequestDTOV2 evaluationRequest) {
    if (evaluationRequest.components == null || evaluationRequest.components.isEmpty()) {
      throw new BadRequestException("No components provided for evaluation");
    }
    for (ApiComponentDTOV2 componentDTO : evaluationRequest.components) {
      if (componentDTO.packageUrl != null) {
        validatePackageUrl(componentDTO);
      }
      else if (componentDTO.componentIdentifier != null) {
        validateComponentIdentifier(componentDTO);
      }
      else if (componentDTO.hash == null) {
        throw new BadRequestException("One of either componentIdentifier, packageUrl, or hash must be supplied.");
      }
    }
  }

  private void validatePackageUrl(final ApiComponentDTOV2 componentDTO) {
    new PackageUrlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
  }

  private void validateComponentIdentifier(final ApiComponentDTOV2 componentDTO) {
    try {
      ComponentIdentifier componentIdentifier = componentDTO.componentIdentifier.toComponentIdentifier();
      componentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private ApiComponentEvaluationTicketDTOV2 createEvaluationTicket(final String applicationId) {
    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = new ApiComponentEvaluationTicketDTOV2();
    evaluationTicketDTO.resultId = UUID.randomUUID().toString().replace("-", "");
    evaluationTicketDTO.submittedDate = new Date();
    evaluationTicketDTO.applicationId = applicationId;
    evaluationTicketDTO.resultsUrl = PublicApiPaths.APPLICATION_EVALUATION_PATH_V2 + "/" + applicationId + "/results/"
        + evaluationTicketDTO.resultId;

    return evaluationTicketDTO;
  }

  class ComponentEvaluationTask
      implements Runnable
  {
    private final ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO;

    private final ApiComponentEvaluationRequestDTOV2 evaluationRequestDTO;

    public ComponentEvaluationTask(
        final ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO,
        final ApiComponentEvaluationRequestDTOV2 evaluationRequestDTO)
    {
      this.evaluationTicketDTO = evaluationTicketDTO;
      this.evaluationRequestDTO = evaluationRequestDTO;
    }

    @Override
    public void run() {
      Application application = applicationDAO.getById(evaluationTicketDTO.applicationId);
      ApiComponentEvaluationResultDTOV2 evaluationResultDTO = new ApiComponentEvaluationResultDTOV2();
      evaluationResultDTO.submittedDate = evaluationTicketDTO.submittedDate;
      evaluationResultDTO.evaluationDate = new Date();
      evaluationResultDTO.applicationId = application.getId();

      try {
        ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(application);
        List<ComponentEvaluationData> componentEvaluationDataList = apiComponentDetailsServiceV2
            .getComponentDetailsListFromHds(evaluationRequestDTO,
                ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
        for (ComponentEvaluationData componentEvaluationData : componentEvaluationDataList) {
          NamedComponentDetails componentDetails = ComponentDetailsAdapter.convert(componentEvaluationData);
          // use the claimed component data if found
          NamedComponentDetails localComponentDetails = ComponentDetailsLoader
              .getComponentDetailsLocally(componentDetails.getComponentIdentifier(), componentDetails.getHash());
          if (localComponentDetails != null) {
            componentDetails = localComponentDetails;
          }
          Component component = componentDetailsLoader.augmentComponentDetails(componentDetails);
          augmentSecurityVulnerabilities(component, componentEvaluationData.securityVulnerabilities);
          ApiComponentDTOV2 componentDTO = evaluationRequestDTO.components.get(componentEvaluationData.requestIndex);
          component.setProprietary(componentDTO.proprietary);
          // Evaluate the policies
          List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(application.getId(), new Stage(
              DevelopStageType.ID), Collections.singletonList(component));
          componentDetails.setPolicyAlerts(policyAlerts);

          ApiComponentDetailsDTOV2 componentDetailsDTO = componentDetailsAdapter.convertToDTO(component, policyAlerts);
          evaluationResultDTO.results.add(componentDetailsDTO);
        }
      }
      catch (Exception e) {
        evaluationResultDTO.isError = true;
        evaluationResultDTO.errorMessage = errorResponseGenerator.mapExceptionAndLog(e).getMessageBody();
        AuditData.get().setException(e);
      }

      try {
        File componentDetailsFile = work.getComponentDetailsFile(application.getId(), evaluationTicketDTO.resultId);
        log.debug("Writing component evaluation results for application id {} and result id {} to file {}",
            application.getId(), evaluationTicketDTO.resultId, componentDetailsFile.getAbsolutePath());

        // Write first to a temp file and rename it in the end to avoid the read of an incomplete results' file.
        File tmpComponentDetailsFile = work.getComponentDetailsFile(application.getId(), evaluationTicketDTO.resultId
            + "-temp");
        JsonUtils.write(tmpComponentDetailsFile, evaluationResultDTO);
        if (!tmpComponentDetailsFile.renameTo(componentDetailsFile)) {
          try {
            FileUtils.copyFile(tmpComponentDetailsFile, componentDetailsFile);
          }
          catch (IOException e) {
            componentDetailsFile.delete();
            throw e;
          }
          finally {
            tmpComponentDetailsFile.delete();
          }
        }
      }
      catch (IOException e) {
        log.error(e.getMessage(), e);
        AuditData.get().setException(e);
      }
    }

    private void augmentSecurityVulnerabilities(Component component, List<SecurityVulnerability> vulnerabilities) {
      if (component.getSecurityVulnerabilities() != null) {
        for (com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component
            .getSecurityVulnerabilities())
        {
          if (sv.getUrl() == null && sv.getRefId() != null) {
            for (SecurityVulnerability vulnerability : vulnerabilities) {
              if (sv.getRefId().equals(vulnerability.getRefId())) {
                sv.setUrl(vulnerability.getUrl());
                break;
              }
            }
          }
        }
      }
    }
  }

  private ThreadFactory createThreadFactory() {
    return new ThreadFactoryBuilder().setNameFormat("ApiComponentEvaluationServiceV2-%d").build();
  }
}
