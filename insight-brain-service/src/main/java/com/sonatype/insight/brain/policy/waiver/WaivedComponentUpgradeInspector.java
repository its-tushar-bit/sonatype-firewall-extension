/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaivedComponentUpgradeInspector
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeInspector.class);

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final RepositoryDAO repositoryDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyDAO policyDAO;

  private final ApiComponentRemediationService apiComponentRemediationService;

  private Map<String, Owner> ownersById;

  private String configuredStage;

  @Inject
  public WaivedComponentUpgradeInspector(
      OrganizationDAO organizationDAO,
      ApplicationDAO applicationDAO,
      RepositoryDAO repositoryDAO,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ApiComponentRemediationService apiComponentRemediationService)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryDAO = repositoryDAO;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.apiComponentRemediationService = apiComponentRemediationService;
  }

  @Override
  public void run() {
    configuredStage = getConfiguredStage();
    if (StringUtils.isEmpty(configuredStage)) {
      log.info("Could not run WaivedComponentUpgradeInspector as stage is not configured");
      return;
    }

    ownersById = getAllOwnersById();

    // Query by policy to reduce memory load while doing less database hits than querying by owners
    policyDAO.getAll().forEach(this::inspectWaiversForPolicy);
  }

  private String getConfiguredStage() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    return rootOrganization != null ? rootOrganization.getWaivedComponentUpgradeStageTypeId() : null;
  }

  private Map<String, Owner> getAllOwnersById() {
    Collector<Owner, ?, Map<String, Owner>> ownerCollector =
        Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);
    Map<String, Owner> owners = new HashMap<>();

    owners.putAll(applicationDAO.getAll().stream().collect(ownerCollector));
    owners.putAll(organizationDAO.getAll().stream().collect(ownerCollector));
    owners.putAll(repositoryDAO.getAll().stream().collect(ownerCollector));
    owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
    return owners;
  }

  private void inspectWaiversForPolicy(final Policy policy) {
    List<PolicyWaiver> waiversForRemediationInspection = getWaiversForRemediationInspection(policy);
    for (PolicyWaiver waiver : waiversForRemediationInspection) {
      try {
        if (waiverContainsUpgradeableComponent(waiver)) {
          waiver.setComponentUpgradeAvailable(true);
          policyWaiverDAO.update(waiver);
        }
      }
      catch (RuntimeException e) {
        log.error("Error when marking waiver as having component upgrade available. Waiver id: " + waiver.getId(), e);
      }
    }
  }

  private List<PolicyWaiver> getWaiversForRemediationInspection(final Policy policy) {
    Predicate<PolicyWaiver> componentUpgradeAvailableNotSet =
        policyWaiver -> policyWaiver.isComponentUpgradeAvailable() == null ||
            !policyWaiver.isComponentUpgradeAvailable();

    return policyWaiverDAO.getActiveByPolicyId(policy.getId()).stream()
        .filter(componentUpgradeAvailableNotSet)
        .filter(policyWaiver -> ComponentMatcherStrategyForWaiver.EXACT_COMPONENT.equals(
            policyWaiver.getComponentMatchStrategy()))
        .filter(policyWaiver -> policyWaiver.getAssociatedPackageUrl() != null)
        .collect(Collectors.toList());
  }

  private boolean waiverContainsUpgradeableComponent(final PolicyWaiver waiver) {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.packageUrl = waiver.getAssociatedPackageUrl();
    OwnerType waiverOwnerType = ownersById.get(waiver.getOwnerId()).getType();
    String stageId = getProxyStageIdForRepositoryOrDefault(waiverOwnerType, configuredStage);

    ApiComponentRemediationDTO suggestedRemediationForComponent =
        apiComponentRemediationService.getSuggestedRemediationForComponent(componentDTOV2, waiverOwnerType,
            waiver.getOwnerId(), stageId, null, null);

    return isRemediationAvailable(suggestedRemediationForComponent, waiver);
  }

  private String getProxyStageIdForRepositoryOrDefault(final OwnerType ownerType, final String defaultStageType) {
    return OwnerType.REPOSITORY.equals(ownerType) ||
        OwnerType.REPOSITORY_CONTAINER.equals(ownerType) ? ProxyStageType.ID : defaultStageType;
  }

  /*
   * TODO due to https://issues.sonatype.org/browse/CLM-22331 we need to check that the remediation does
   * not suggest the same version that is already related to the waiver itself.
   * This should be a simple check of existence of values in the versionChanges list once that ticket is fixed
   */
  private static boolean isRemediationAvailable(
      final ApiComponentRemediationDTO suggestedRemediationForComponent,
      final PolicyWaiver waiver)
  {
    if (suggestedRemediationForComponent == null) {
      return false;
    }
    for (ApiVersionChangeOptionDTO versionChange : suggestedRemediationForComponent.remediation.versionChanges) {
      if (!versionChange.getData().getComponent().packageUrl.equals(waiver.getAssociatedPackageUrl())) {
        return true;
      }
    }

    return false;
  }
}
