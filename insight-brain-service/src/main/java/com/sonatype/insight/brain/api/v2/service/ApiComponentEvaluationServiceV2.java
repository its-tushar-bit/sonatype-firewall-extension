/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList.ComponentEvaluationDataRequest;
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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.saas.ComponentDetailsLoader;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13.0
 */
@Named
@Singleton
public class ApiComponentEvaluationServiceV2
{
  public static final String HDS_EVALUATION_COMPONENTS_PATH = "rest/evaluation/components";

  private int chunkSize = 100;

  private static final Logger log = LoggerFactory.getLogger(ApiComponentEvaluationServiceV2.class);

  private final ExecutorService executor = Executors.newFixedThreadPool(4, createThreadFactory());

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluator policyEvaluator;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final ApiComponentDetailsAdapter componentDetailsAdapter;

  private final SaasClient client;

  private final InsightWork work;

  private final ErrorResponseGenerator errorResponseGenerator;


  @Inject
  public ApiComponentEvaluationServiceV2(final ApplicationDAO applicationDAO, final PolicyEvaluator policyEvaluator,
      final ComponentDetailsLoader componentDetailsLoader, final ApiComponentDetailsAdapter componentDetailsAdapter,
      final SaasClient client, final InsightWork work, final ErrorResponseGenerator errorResponseGenerator)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluator = policyEvaluator;
    this.componentDetailsLoader = componentDetailsLoader;
    this.componentDetailsAdapter = componentDetailsAdapter;
    this.client = client;
    this.work = work;
    this.errorResponseGenerator = errorResponseGenerator;
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    validateRequest(evaluationRequest);

    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = createEvaluationTicket(applicationId);
    executor.submit(new SystemRunnable(new ComponentEvaluationTask(evaluationTicketDTO, evaluationRequest)));

    return evaluationTicketDTO;
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentEvaluationResultDTOV2 getComponentEvaluation(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId, final String resultId)
      throws IOException
  {
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
    this.chunkSize = chunkSize;
  }

  private void validateRequest(final ApiComponentEvaluationRequestDTOV2 evaluationRequest) {
    if (evaluationRequest.components == null || evaluationRequest.components.isEmpty()) {
      throw new BadRequestException("No components provided for evaluation");
    }
    for (ApiComponentDTOV2 componentDTO : evaluationRequest.components) {
      if (componentDTO.componentIdentifier != null) {
        validateComponentIdentifier(componentDTO);
      }
      else if (componentDTO.hash == null) {
        throw new BadRequestException("One of either componentIdentifier or hash must be supplied.");
      }
    }
  }

  private void validateComponentIdentifier(final ApiComponentDTOV2 componentDTO) {
    try {
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
          componentDTO.componentIdentifier.getCoordinates());
      componentIdentifier.ensureComplete();
    } catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private ApiComponentEvaluationTicketDTOV2 createEvaluationTicket(final String applicationId) {
    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = new ApiComponentEvaluationTicketDTOV2();
    evaluationTicketDTO.resultId = UUID.randomUUID().toString().replace("-", "");
    evaluationTicketDTO.submittedDate = new Date();
    evaluationTicketDTO.applicationId = applicationId;
    evaluationTicketDTO.resultsUrl = PublicApiPaths.APPLICATION_EVALUATION_PATH_V2
        + "/" + applicationId + "/results/" + evaluationTicketDTO.resultId;

    return evaluationTicketDTO;
  }

  class ComponentEvaluationTask
      implements Runnable
  {
    private final ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO;

    private final ApiComponentEvaluationRequestDTOV2 evaluationRequestDTO;

    public ComponentEvaluationTask(final ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO,
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
        List<ComponentEvaluationData> componentEvaluationDataList = getComponentDetailsList(evaluationRequestDTO);
        for (ComponentEvaluationData componentEvaluationData : componentEvaluationDataList) {
          NamedComponentDetails componentDetails = convert(componentEvaluationData);
          // use the claimed component data if found
          NamedComponentDetails localComponentDetails = componentDetailsLoader
              .getComponentDetailsLocally(componentDetails.getComponentIdentifier(), componentDetails.getHash());
          if (localComponentDetails != null) {
            componentDetails = localComponentDetails;
          }
          Component component = componentDetailsLoader.augmentComponentDetails(application, componentDetails);
          augmentSecurityVulnerabilities(component, componentEvaluationData.securityVulnerabilities);
          ApiComponentDTOV2 componentDTO = evaluationRequestDTO.components.get(componentEvaluationData.requestIndex);
          component.setProprietary(componentDTO.proprietary);
          // Evaluate the policies
          List<PolicyAlert> policyAlerts = policyEvaluator.evaluate(application.getId(), new Stage(DevelopStageType.ID),
              new PolicyDAO(), Collections.singletonList(component));
          componentDetails.setPolicyAlerts(policyAlerts);

          ApiComponentDetailsDTOV2 componentDetailsDTO = componentDetailsAdapter.convertToDTO(component, policyAlerts);
          evaluationResultDTO.results.add(componentDetailsDTO);
        }
      }
      catch (Exception e) {
        evaluationResultDTO.isError = true;
        evaluationResultDTO.errorMessage = errorResponseGenerator.mapException(e).getMessageBody();
      }

      try {
        File componentDetailsFile = work.getComponentDetailsFile(application.getId(), evaluationTicketDTO.resultId);
        log.debug("Writing component evaluation results for appliction id {} and result id {} to file {}",
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
      }
    }

    private void augmentSecurityVulnerabilities(Component component,
        List<SecurityVulnerability> vulnerabilities)
    {
      if (component.getSecurityVulnerabilities() != null) {
        for (com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component
            .getSecurityVulnerabilities()) {
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

    private NamedComponentDetails convert(final ComponentEvaluationData componentEvaluationData) {
      NamedComponentDetails componentDetails = new NamedComponentDetails();
      componentDetails.setCatalogDate(componentEvaluationData.catalogDate);
      componentDetails.setHash(componentEvaluationData.hash);
      componentDetails.setComponentIdentifier(componentEvaluationData.componentIdentifier);
      componentDetails.setMatchState(componentEvaluationData.matchState);
      componentDetails.setDeclaredLicenses(componentEvaluationData.declaredLicenses);
      componentDetails.setObservedLicenses(componentEvaluationData.observedLicenses);
      componentDetails
          .setSecurityVulnerabilities(convertToSecurityVulnerability(componentEvaluationData.securityVulnerabilities));
      componentDetails.setMatchState(componentEvaluationData.matchState);
      return componentDetails;
    }

    private List<SecurityVulnerability> convertToSecurityVulnerability(
        List<SecurityVulnerability> vulnerabilities)
    {
      if (vulnerabilities == null) {
        return null;
      }

      return new ArrayList<>(vulnerabilities);
    }

    private List<ComponentEvaluationData> getComponentDetailsList(
        final ApiComponentEvaluationRequestDTOV2 evaluationRequestDTO)
        throws IOException
    {
      ComponentEvaluationDataList returnList = new ComponentEvaluationDataList();
      returnList.components = new ArrayList<>();

      int indexAdjust = 0;
      List<List<ApiComponentDTOV2>> componentChunks = createChunks(evaluationRequestDTO.components, chunkSize);
      for (List<ApiComponentDTOV2> componentChunk : componentChunks) {
        ComponentEvaluationDataRequestList componentEvaluationDataRequestList = convert(componentChunk);
        ComponentEvaluationDataList componentEvaluationDataList = client.post(ComponentEvaluationDataList.class,
            HDS_EVALUATION_COMPONENTS_PATH, componentEvaluationDataRequestList);

        for (ComponentEvaluationData componentEvaluationData : componentEvaluationDataList.components) {
          componentEvaluationData.requestIndex += indexAdjust * chunkSize;
          returnList.components.add(componentEvaluationData);
        }
        indexAdjust++;
      }

      return returnList.components;
    }

    private ComponentEvaluationDataRequestList convert(final List<ApiComponentDTOV2> components) {
      ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();
      componentEvaluationDataRequestList.components = new ArrayList<>();
      for (ApiComponentDTOV2 componentDTO : components) {
        ComponentEvaluationDataRequest componentEvaluationDataRequest = convert(componentDTO);
        componentEvaluationDataRequestList.components.add(componentEvaluationDataRequest);
      }
      return componentEvaluationDataRequestList;
    }

    private ComponentEvaluationDataRequest convert(final ApiComponentDTOV2 componentDTO) {
      ComponentEvaluationDataRequest componentEvaluationDataRequest = new ComponentEvaluationDataRequest();
      componentEvaluationDataRequest.hash = componentDTO.hash;
      if (componentDTO.componentIdentifier != null) {
        componentEvaluationDataRequest.componentIdentifier = new ComponentIdentifier(
            componentDTO.componentIdentifier.getFormat(), componentDTO.componentIdentifier.getCoordinates());
        componentEvaluationDataRequest.componentIdentifier.ensureComplete();
      }
      return componentEvaluationDataRequest;
    }

    private <T> List<List<T>> createChunks(List<T> bigList, int n) {
      List<List<T>> chunks = new ArrayList<>();

      for (int i = 0; i < bigList.size(); i += n) {
        List<T> chunk = bigList.subList(i, Math.min(bigList.size(), i + n));
        chunks.add(chunk);
      }

      return chunks;
    }
  }

  private ThreadFactory createThreadFactory() {
    return new ThreadFactoryBuilder().setNameFormat("ApiComponentEvaluationServiceV2-%d").build();
  }
}
