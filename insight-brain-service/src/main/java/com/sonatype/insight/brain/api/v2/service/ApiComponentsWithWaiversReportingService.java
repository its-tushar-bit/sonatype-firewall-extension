/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationStageDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.telemetry.ReportsTelemetry;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.utils.ExecutorThreadPools.getThreadPool;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.75
 */
public class ApiComponentsWithWaiversReportingService
{
  private static final String APPLICATION_COMPONENTS_AUDIT_KEY = "numberOfApplicationComponents";

  private static final String REPOSITORY_COMPONENTS_AUDIT_KEY = "numberOfRepositoryComponents";

  private static final String POLICY_WAIVER_NOT_FOUND_MSG = "Related policy waiver not found. Please re-evaluate.";

  private final PolicyViolationLoader policyViolationLoader;

  private final RepositoryService repositoryService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDao;

  private final PolicyWaiverDAO policyWaiverDao;

  private static final Logger log = LoggerFactory.getLogger(ApiComponentsWithWaiversReportingService.class);

  private final ApplicationService applicationService;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final ReportsTelemetry reportsTelemetry;

  @Inject
  public ApiComponentsWithWaiversReportingService(
      ApplicationService applicationService,
      PolicyViolationAdapter policyViolationAdapter,
      PolicyViolationLoader policyViolationLoader,
      ReportsTelemetry reportsTelemetry,
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDao,
      PolicyWaiverDAO policyWaiverDao)
  {
    this.applicationService = applicationService;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyViolationLoader = policyViolationLoader;
    this.reportsTelemetry = reportsTelemetry;
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDao = repositoryPolicyViolationDao;
    this.policyWaiverDao = policyWaiverDao;
  }

  public ApiComponentWaiversDTO getComponentsWithWaivers() {
    final AtomicInteger componentsWithWaiversCount = new AtomicInteger(0);

    reportsTelemetry.sendComponentWithWaiversTelemetry();

    List<Application> applications = applicationService.getApplications();
    Collection<ApplicationView> appViews =
        policyViolationLoader.getViolations(applications, null, false,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED).asPolicyViolationPredicate());

    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    ApiComponentWaiversDTO componentWaiversDTO = new ApiComponentWaiversDTO();
    componentWaiversDTO.applicationWaivers = buildApplicationWaiverDTOs(appViews, componentsWithWaiversCount);
    componentWaiversDTO.repositoryWaivers = buildRepositoryWaiverDTOs(repositoryDTOs, componentsWithWaiversCount);

    log.debug("getComponentsWithWaivers: Processed {} components with waived policy violations.",
        componentsWithWaiversCount);

    return componentWaiversDTO;
  }

  private List<ApiApplicationWaiverDTO> buildApplicationWaiverDTOs(
      Collection<ApplicationView> appViews,
      final AtomicInteger componentsWithWaiversCount)
  {
    List<ApiApplicationWaiverDTO> applicationWaiverDTOs = new ArrayList<>();
    final AtomicInteger applicationComponentsWithWaiversCount = new AtomicInteger(0);

    List<CompletableFuture<List<ApiApplicationWaiverDTO>>> dtoFutures = appViews.stream()
        .map(appView -> {
          return CompletableFuture.supplyAsync(() -> {
            Application app = appView.getApplication();

            List<PolicyViolation> allAppPolicyViolations = new ArrayList<>();
            List<ApiApplicationWaiverDTO> localDTOs = new ArrayList<>();

            ApiApplicationWaiverDTO applicationWaiverDTO = new ApiApplicationWaiverDTO();
            applicationWaiverDTO.application = buildApplicationBaseDTO(app);

            // We need to report on the latest evaluation for EACH stage.
            for (ApplicationStageView appStageView : appView.getStageViews()) {
              Collection<PolicyViolation> policyViolations = appStageView.getFilteredViolations();
              if (policyViolations.isEmpty()) {
                continue;
              }
              allAppPolicyViolations.addAll(policyViolations);

              ApiPolicyViolationStageDTO policyViolationStageDTO =
                  buildPolicyViolationStageDTO(applicationWaiverDTO, appStageView.getStageType().getId());

              List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();
              policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

              /*
               * Since our dto model consists of list of waived policy violations associated with a single
               * component identifier, we group the waived policy violations by component identifier and process
               * accordingly.
               */
              policyViolations.stream().collect(Collectors.groupingBy(PolicyViolation::getComponentIdentifier))
                  .forEach((componentIdentifier, policyViolationsByComponent) -> {
                    applicationComponentsWithWaiversCount.incrementAndGet();

                    ApiComponentPolicyViolationDTO componentPolicyViolationDTO = new ApiComponentPolicyViolationDTO();

                    // Grab the first hash it should be the same for all violations
                    String hash = policyViolationsByComponent.get(0).getHash();
                    componentPolicyViolationDTO.component = buildComponentDTO(componentIdentifier, hash);

                    // for this component identifier create a dto list of all the waived policy violations
                    componentPolicyViolationDTO.waivedPolicyViolations =
                        policyViolationsByComponent.stream()
                            .map(policyViolation -> buildWaivedPolicyViolationDTO(policyViolation))
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

    AuditData.get().setData(APPLICATION_COMPONENTS_AUDIT_KEY, applicationComponentsWithWaiversCount);
    componentsWithWaiversCount.addAndGet(applicationComponentsWithWaiversCount.get());

    return applicationWaiverDTOs;
  }

  private List<ApiRepositoryWaiverDTO> buildRepositoryWaiverDTOs(
      List<RepositoryDTO> repositoryDTOs,
      final AtomicInteger componentsWithWaiversCount)
  {
    List<ApiRepositoryWaiverDTO> repositoryWaiverDTOs = new ArrayList<>();
    MutableInt repositoryComponentsWithWaiversCount = new MutableInt(0);

    if (repositoryDTOs != null && !repositoryDTOs.isEmpty()) {
      Map<String, Repository> idToRepositoryMap = repositoryDTOs.stream()
          .collect(Collectors.toMap(repositoryDTO -> repositoryDTO.repository.getId(),
              repositoryDTO -> repositoryDTO.repository));

      List<RepositoryPolicyViolation> repositoryPolicyViolations =
          repositoryPolicyViolationDao.getActiveWaivedRepositoryPolicyViolations(idToRepositoryMap.keySet());
      repositoryPolicyViolations.sort(PolicyViolationComparator.COMPARATOR);

      getGroupedRepositoryPolicyViolations(repositoryPolicyViolations)
          .forEach((repositoryId, groupedPolicyViolationsByComponent) -> {
            ApiRepositoryDTO repositoryDTO =
                buildRepositoryDTO(idToRepositoryMap.get(repositoryId));
            List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs = new ArrayList<>();

            groupedPolicyViolationsByComponent
                .forEach((componentIdentifier, repositoryViolations) -> {
                  repositoryComponentsWithWaiversCount.increment();
                  List<ApiWaivedPolicyViolationDTO> waivedPolicyViolationDTOs = new ArrayList<>();
                  for (RepositoryPolicyViolation violation : repositoryViolations) {
                    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO =
                        buildWaivedPolicyViolationDTO(violation);
                    waivedPolicyViolationDTOs.add(waivedPolicyViolationDTO);
                  }

                  ApiComponentPolicyViolationDTO componentPolicyViolationDTO =
                      buildComponentPolicyViolationDTO(buildComponentDTO(componentIdentifier,
                          repositoryViolations.get(0).getHash()),
                          waivedPolicyViolationDTOs);
                  componentPolicyViolationDTOs.add(componentPolicyViolationDTO);
                });

            // Sort components by packageUrl
            componentPolicyViolationDTOs.sort(Comparator.comparing(c -> c.component.packageUrl));

            ApiPolicyViolationStageDTO policyViolationStageDTO =
                buildPolicyViolationStageDTO(componentPolicyViolationDTOs);

            ApiRepositoryWaiverDTO repositoryWaiverDTO = new ApiRepositoryWaiverDTO();
            repositoryWaiverDTO.repository = repositoryDTO;
            repositoryWaiverDTO.stages = Arrays.asList(policyViolationStageDTO);
            repositoryWaiverDTOs.add(repositoryWaiverDTO);
          });

      // Sort repos by publicId
      repositoryWaiverDTOs.sort(Comparator.comparing(r -> r.repository.publicId));
    }

    AuditData.get().setData(REPOSITORY_COMPONENTS_AUDIT_KEY, repositoryComponentsWithWaiversCount.intValue());
    componentsWithWaiversCount.addAndGet(repositoryComponentsWithWaiversCount.intValue());

    return repositoryWaiverDTOs;
  }

  private ApiApplicationBaseDTO buildApplicationBaseDTO(Application app) {
    ApiApplicationBaseDTO application = new ApiApplicationBaseDTO();
    application.id = app.getId();
    application.publicId = app.getPublicId();
    application.name = app.getName();
    application.organizationId = app.getOrganizationId();
    application.contactUserName = app.getContactInternalName();

    return application;
  }

  private ApiPolicyViolationStageDTO buildPolicyViolationStageDTO(
      ApiApplicationWaiverDTO applicationWaiverDTO,
      String stageId)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = stageId;
    applicationWaiverDTO.stages.add(policyViolationStageDTO);

    return policyViolationStageDTO;
  }

  private ApiComponentDTOV2 buildComponentDTO(ComponentIdentifier componentIdentifier, String hash) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    componentDTOV2.hash = hash;
    componentDTOV2.proprietary = null;
    componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);

    return componentDTOV2;
  }

  private ApiRepositoryDTO buildRepositoryDTO(Repository repository) {
    ApiRepositoryDTO repositoryDTO = new ApiRepositoryDTO();
    repositoryDTO.repositoryId = repository.getId();
    repositoryDTO.publicId = repository.getPublicId();
    repositoryDTO.format = repository.getFormat();

    return repositoryDTO;
  }

  private ApiWaivedPolicyViolationDTO buildWaivedPolicyViolationDTO(AbstractPolicyViolation policyViolation) {
    ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO = new ApiWaivedPolicyViolationDTO();
    waivedPolicyViolationDTO.policyId = policyViolation.getPolicyId();
    waivedPolicyViolationDTO.policyName = policyViolation.getPolicyName();
    waivedPolicyViolationDTO.policyViolationId = policyViolation.getId();
    waivedPolicyViolationDTO.threatLevel = policyViolation.getThreatLevel();
    waivedPolicyViolationDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);

    ApiPolicyWaiverDTO policyWaiverDTO = new ApiPolicyWaiverDTO();
    String policyWaiverId = policyViolation.getPolicyWaiverId();
    PolicyWaiver policyWaiver = null;

    if (policyWaiverId != null) {
      policyWaiver = policyWaiverDao.getById(policyWaiverId);
    }

    if (policyWaiver == null) {
      policyWaiverDTO.isObsolete = true;
      policyWaiverDTO.comment = POLICY_WAIVER_NOT_FOUND_MSG;
    }
    else {
      policyWaiverDTO.isObsolete = false;
      policyWaiverDTO.policyWaiverId = policyWaiver.getId();
      policyWaiverDTO.comment = policyWaiver.getComment();
      policyWaiverDTO.createTime = policyWaiver.getCreateTime();
    }

    waivedPolicyViolationDTO.policyWaiver = policyWaiverDTO;

    return waivedPolicyViolationDTO;
  }

  private ApiComponentPolicyViolationDTO buildComponentPolicyViolationDTO(ApiComponentDTOV2 componentDTOV2,
      List<ApiWaivedPolicyViolationDTO> waivedPolicyViolationDTOs)
  {
    ApiComponentPolicyViolationDTO componentPolicyViolationDTO = new ApiComponentPolicyViolationDTO();
    componentPolicyViolationDTO.component = componentDTOV2;
    componentPolicyViolationDTO.waivedPolicyViolations = waivedPolicyViolationDTOs;

    return componentPolicyViolationDTO;
  }

  private ApiPolicyViolationStageDTO buildPolicyViolationStageDTO(
      List<ApiComponentPolicyViolationDTO> componentPolicyViolationDTOs)
  {
    ApiPolicyViolationStageDTO policyViolationStageDTO = new ApiPolicyViolationStageDTO();
    policyViolationStageDTO.stageId = Stage.ID_PROXY;
    policyViolationStageDTO.componentPolicyViolations = componentPolicyViolationDTOs;

    return policyViolationStageDTO;
  }

  private Map<String, Map<ComponentIdentifier, List<RepositoryPolicyViolation>>>
      getGroupedRepositoryPolicyViolations(List<RepositoryPolicyViolation> repositoryPolicyViolations)
  {
    return repositoryPolicyViolations.stream()
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getRepositoryId,
                Collectors.groupingBy(RepositoryPolicyViolation::getComponentIdentifier, toList())));
  }
}
