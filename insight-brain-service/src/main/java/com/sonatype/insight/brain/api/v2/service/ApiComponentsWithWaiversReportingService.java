/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.telemetry.ReportsTelemetry;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.75
 */
public class ApiComponentsWithWaiversReportingService
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentsWithWaiversReportingService.class);

  private final ApplicationService applicationService;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final PolicyViolationLoader policyViolationLoader;

  private final ReportsTelemetry reportsTelemetry;

  @Inject
  public ApiComponentsWithWaiversReportingService(
      ApplicationService applicationService,
      PolicyViolationAdapter policyViolationAdapter,
      PolicyViolationLoader policyViolationLoader,
      ReportsTelemetry reportsTelemetry)
  {
    this.applicationService = applicationService;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyViolationLoader = policyViolationLoader;
    this.reportsTelemetry = reportsTelemetry;
  }

  public ApiComponentWaiversDTO getComponentsWithWaivers() {
    reportsTelemetry.sendComponentWithWaiversTelemetry();

    List<Application> applications = applicationService.getApplications();

    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(applications, null, false,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED).asPolicyViolationPredicate());

    ApiComponentWaiversDTO componentWaiversDTO = new ApiComponentWaiversDTO();
    componentWaiversDTO.applicationWaivers = buildApplicationWaiverDTOs(appViews);

    //move this next line to whatever method gets added that has the data
    AuditData.get().setData("numberOfRepositoryComponents", 0);

    return componentWaiversDTO;
  }

  private List<ApiApplicationWaiverDTO> buildApplicationWaiverDTOs(Collection<ApplicationView> appViews) {
    List<ApiApplicationWaiverDTO> applicationWaiverDTOs = new ArrayList<>();

    final AtomicInteger componentsWithWaiversCount = new AtomicInteger(0);

    List<CompletableFuture<List<ApiApplicationWaiverDTO>>> dtoFutures = appViews.stream()
        .map(appView -> {
          return CompletableFuture.supplyAsync(() -> {
            Application app = appView.getApplication();

            List<PolicyViolation> allAppPolicyViolations = new ArrayList<>();
            List<ApiApplicationWaiverDTO> localDTOs = new ArrayList<>();

            ApiApplicationWaiverDTO applicationWaiverDTO = new ApiApplicationWaiverDTO();
            applicationWaiverDTO.application = createApplicationBaseDTO(app);

            // We need to report on the latest evaluation for EACH stage.
            for (ApplicationStageView appStageView : appView.getStageViews()) {
              Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
              if (policyViolations.isEmpty()) {
                continue;
              }
              allAppPolicyViolations.addAll(policyViolations);

              ApiPolicyViolationStageDTO policyViolationStageDTO =
                  createPolicyViolationStageDTO(applicationWaiverDTO, appStageView.getStageType().getId());

              List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();
              policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

              /*
               * Since our dto model consists of list of waived policy violations associated with a single
               * component identifier, we group the waived policy violations by component identifier and process
               * accordingly.
               */
              policyViolations.stream().collect(Collectors.groupingBy(PolicyViolation::getComponentIdentifier))
                  .forEach((componentIdentifier, policyViolationsByComponent) -> {
                    componentsWithWaiversCount.incrementAndGet();

                    ApiComponentPolicyViolationDTO componentPolicyViolationDTO = new ApiComponentPolicyViolationDTO();

                    // Grab the first hash it should be the same for all violations
                    String hash = policyViolationsByComponent.get(0).getHash();
                    componentPolicyViolationDTO.component = createComponentDTO(componentIdentifier, hash);

                    // for this component identifier create a dto list of all the waived policy violations
                    componentPolicyViolationDTO.waivedPolicyViolations =
                        policyViolationsByComponent.stream().map(this::createWaivedPolicyViolation)
                            .collect(Collectors.toList());
                    componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                  });
            }

            if (!allAppPolicyViolations.isEmpty()) {
              localDTOs.add(applicationWaiverDTO);
            }
            return localDTOs;
          }, getThreadPool(ThreadPools.GENERAL));
        }).collect(toList());

    dtoFutures.stream().map(CompletableFuture::join).forEach(applicationWaiverDTOs::addAll);

    log.debug("getComponentsWithWaivers: Processed {} components with waived policy violations.",
        componentsWithWaiversCount);
    AuditData.get().setData("numberOfApplicationComponents", componentsWithWaiversCount);

    return applicationWaiverDTOs;
  }

  private ApiApplicationBaseDTO createApplicationBaseDTO(Application app) {
    ApiApplicationBaseDTO application = new ApiApplicationBaseDTO();
    application.id = app.getId();
    application.publicId = app.getPublicId();
    application.name = app.getName();
    application.organizationId = app.getOrganizationId();
    application.contactUserName = app.getContactInternalName();

    return application;
  }

  private ApiPolicyViolationStageDTO createPolicyViolationStageDTO(
      ApiApplicationWaiverDTO applicationWaiverDTO,
      String stageId)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = stageId;
    applicationWaiverDTO.stages.add(policyViolationStageDTO);

    return policyViolationStageDTO;
  }

  private ApiComponentDTOV2 createComponentDTO(ComponentIdentifier componentIdentifier, String hash) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTOV2.hash = hash;
    componentDTOV2.proprietary = null;
    componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);

    return componentDTOV2;
  }

  private ApiWaivedPolicyViolationDTO createWaivedPolicyViolation(PolicyViolation policyViolation) {
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = new ApiWaivedPolicyViolationDTO();
    waivedPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    waivedPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    waivedPolicyViolationDTO.policyViolationId = policyViolation.getId();
    waivedPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    waivedPolicyViolationDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);

    ApiPolicyWaiverDTO policyWaiverDTO = new ApiPolicyWaiverDTO();
    policyWaiverDTO.createTime = policyViolation.getWaiveTime();
    policyWaiverDTO.policyWaiverId = policyViolation.getPolicyWaiverId();
    policyWaiverDTO.comment = policyViolation.getPolicyWaiverComment();
    waivedPolicyViolationDTO.policyWaiver = policyWaiverDTO;

    return waivedPolicyViolationDTO;
  }
}
