/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDefinitionDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.saas.CIComponentInfoResource;
import com.sonatype.insight.brain.saas.ComponentDetailsLoader;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13.0
 */
@Named
@Singleton
public class ApiComponentEvaluationServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentEvaluationServiceV2.class);

  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluator policyEvaluator;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final ApiComponentDetailsAdapter componentDetailsAdapter;

  private final SaasClient client;

  private final InsightWork work;

  private final ApiComponentIdentifierValidator componentIdentifierValidator;

  private final ErrorResponseGenerator errorResponseGenerator;


  @Inject
  public ApiComponentEvaluationServiceV2(final ApplicationDAO applicationDAO, final PolicyEvaluator policyEvaluator,
      final ComponentDetailsLoader componentDetailsLoader, final ApiComponentDetailsAdapter componentDetailsAdapter,
      final SaasClient client, final InsightWork work,
      final ApiComponentIdentifierValidator componentIdentifierValidator,
      final ErrorResponseGenerator errorResponseGenerator)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluator = policyEvaluator;
    this.componentDetailsLoader = componentDetailsLoader;
    this.componentDetailsAdapter = componentDetailsAdapter;
    this.client = client;
    this.work = work;
    this.componentIdentifierValidator = componentIdentifierValidator;
    this.errorResponseGenerator = errorResponseGenerator;
  }

  @Authorize(permission = Permission.READ)
  public ApiComponentEvaluationTicketDTOV2 evaluateComponents(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final ApiComponentEvaluationRequestDTOV2 evaluationRequest)
  {
    validateComponentIdentifiers(evaluationRequest);

    ApiComponentEvaluationTicketDTOV2 evaluationTicketDTO = createEvaluationTicket(applicationId);
    executor.submit(new ComponentEvaluationTask(evaluationTicketDTO, evaluationRequest));

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

  /**
   * Validate the component identifier of each component and
   * throws BadRequestException on first invalid component identifier
   */
  private void validateComponentIdentifiers(final ApiComponentEvaluationRequestDTOV2 evaluationRequest) {
    if (evaluationRequest.components == null || evaluationRequest.components.isEmpty()) {
      throw new BadRequestException("No components provided for evaluation");
    }
    for (ApiComponentDefinitionDTOV2 componentDTO : evaluationRequest.components) {
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
          componentDTO.componentIdentifier.getCoordinates());
      componentIdentifierValidator.validate(componentIdentifier);
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
        Map<ApiComponentDefinitionDTOV2, NamedComponentDetails> componentDetailsMap =
            getComponentDetails(evaluationRequestDTO);
        for (Entry<ApiComponentDefinitionDTOV2, NamedComponentDetails> componentDetailsEntry :
            componentDetailsMap.entrySet()) {
          ApiComponentDefinitionDTOV2 componentDefinitionDTO = componentDetailsEntry.getKey();
          NamedComponentDetails componentDetails = componentDetailsEntry.getValue();
          Component component = componentDetailsLoader.augmentComponentDetails(application, componentDetails);
          component.setProprietary(componentDefinitionDTO.proprietary);
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
        JsonUtils.write(componentDetailsFile, evaluationResultDTO);
      }
      catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    }

    private Map<ApiComponentDefinitionDTOV2, NamedComponentDetails> getComponentDetails(
        final ApiComponentEvaluationRequestDTOV2 evaluationRequestDTO)
        throws IOException
    {
      Map<ApiComponentDefinitionDTOV2, NamedComponentDetails> componentMap = new LinkedHashMap<>();
      for (ApiComponentDefinitionDTOV2 componentDTO : evaluationRequestDTO.components) {
        ComponentIdentifier componentIdentifier = new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
            componentDTO.componentIdentifier.getCoordinates());
        NamedComponentDetails componentDetails = getComponentDetails("", componentDTO.hash, componentIdentifier);
        componentMap.put(componentDTO, componentDetails);
      }
      return componentMap;
    }

    private NamedComponentDetails getComponentDetails(final String matchState, final String hash,
        final ComponentIdentifier identifier)
        throws IOException
    {
      return componentDetailsLoader.getComponentDetails(identifier, hash, matchState,
          new ComponentDetailsLoader.HostedDataServicesSource()
          {
            @Override
            public NamedComponentDetails getDetails() throws IOException {
              NamedComponentDetails componentDetails;

              Map<String, String> queryParams = new HashMap<>();
              queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier));
              if (hash != null) {
                queryParams.put("hash", hash);
              }

              try {
                componentDetails = client.get(NamedComponentDetails.class, CIComponentInfoResource.SERVICE_PATH,
                    queryParams);
                componentDetails.setMatchState(MatchState.EXACT.getId());
              }
              catch (NotFoundException e) {
                // Identifier is unknown to HDS, still want to provide minimal data
                componentDetails = new NamedComponentDetails();
                componentDetails.setComponentIdentifier(identifier);
                componentDetails.setMatchState(MatchState.UNKNOWN.getId());
              }
              return componentDetails;
            }
          });
    }
  }
}
