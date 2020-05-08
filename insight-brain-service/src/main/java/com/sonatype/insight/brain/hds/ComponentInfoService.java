/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.LicenseUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.IdentificationSource.isThirdPartyIdentificationSource;

@Named
public class ComponentInfoService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentInfoService.class);

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  private final HdsClient hdsClient;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ComponentRemediationService componentRemediationService;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private String toolName;

  @Inject
  public ComponentInfoService(
      HdsClient hdsClient,
      ComponentDetailsLoader componentDetailsLoader,
      ComponentPolicyEvaluator componentPolicyEvaluator,
      ComponentRemediationService componentRemediationService,
      ThirdPartyComponentDAO thirdPartyComponentDAO)
  {
    this.hdsClient = hdsClient;
    this.componentDetailsLoader = componentDetailsLoader;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.componentRemediationService = componentRemediationService;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public NamedComponentDetails getComponentDetails_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier identifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest) throws IOException
  {
    auditComponentAccess(identifier, hash);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    NamedComponentDetails details = getComponentDetails(app, identifier, matchState, hash, proprietary, httpRequest);

    return details;
  }

  @Authorize(permission = Permission.READ)
  public NamedComponentDetails getComponentDetails_ReadPermission(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      String matchState,
      String hash,
      boolean proprietary,
      HttpServletRequest httpRequest,
      String identificationSource,
      String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, hash);
    final Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    return getComponentDetails(owner, componentIdentifier, matchState, hash, proprietary, httpRequest,
        identificationSource, scanId);
  }

  NamedComponentDetails getComponentDetails(Owner owner,
                                            final ComponentIdentifier identifier,
                                            String matchState,
                                            String hash,
                                            boolean proprietary,
                                            HttpServletRequest httpRequest) throws IOException
  {
    return getComponentDetails(owner, identifier, matchState, hash, proprietary, httpRequest, null, null);
  }

  NamedComponentDetails getComponentDetails(Owner owner,
                                            final ComponentIdentifier identifier,
                                            String matchState,
                                            String hash,
                                            boolean proprietary,
                                            HttpServletRequest httpRequest,
                                            String identificationSource,
                                            String scanId) throws IOException
  {
    long start = System.currentTimeMillis();

    // clients like Nexus provide full SHA1 values
    hash = HashHelper.truncateHash(hash);

    NamedComponentDetails componentDetails;

    if (identifier != null) {
      if (isThirdPartyIdentificationSource(identificationSource)) {
        componentDetails = thirdPartyComponentDAO.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);
      }
      else {
        componentDetails = getComponentDetailsFromHDS(matchState, hash, identifier, httpRequest);
      }
    }
    else {
      // See CLM-4195
      componentDetails = createEmptyComponentDetails(hash, identifier);
    }

    Component component = componentDetailsLoader.augmentComponentDetails(owner, componentDetails);
    component.setProprietary(proprietary);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(owner.getId(), new Stage(DevelopStageType.ID),
        Collections.singletonList(component));
    componentDetails.setPolicyAlerts(policyAlerts);

    log.debug("Loaded component details for {}, hash {}, in {} ms.", identifier, hash, System.currentTimeMillis()
        - start);
    return componentDetails;
  }

  private NamedComponentDetails getComponentDetailsFromHDS(String matchState,
                                                           final String hash,
                                                           final ComponentIdentifier identifier,
                                                           final HttpServletRequest httpRequest) throws IOException
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
              componentDetails = hdsClient.relay(httpRequest, NamedComponentDetails.class, "rest/" + toolName
                  + "/componentDetails", queryParams);
              componentDetails.setMatchState(MatchState.EXACT.getId());
            }
            catch (NotFoundException e) {
              // Identifier is unknown to HDS, still want to provide minimal data for details view
              componentDetails = createEmptyComponentDetails(hash, identifier);
            }
            return componentDetails;
          }
        });
  }

  // Intended for unknown cases
  private NamedComponentDetails createEmptyComponentDetails(String hash, ComponentIdentifier identifier) {
    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(identifier);
    details.setHash(hash);
    details.setMatchState(MatchState.UNKNOWN.getId());
    return details;
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   *
   * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentDetailsList getComponentDetailsList_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier identifier,
      String matchState)
  {
    auditComponentAccess(identifier, null);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    ComponentDetailsList componentDetailsList = getComponentDetailsList(identifier, null, null, null);
    componentDetailsLoader.augmentComponentDetails(app, componentDetailsList.getList(), matchState);
    return componentDetailsList;
  }

  /**
   * Returns a list of component details for the given application and component identifier.
   * It also evaluates policies and returns max threat levels per category, as well as count of violated policies.
   *
   * This method is called by the IDE and RM plugins, so it needs to check the EVALUATE_COMPONENT permission.
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentVersionInfoDTO getComponentVersionInfo_EvaluateComponentPermission(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier componentIdentifier)
  {
    auditComponentAccess(componentIdentifier, null);
    return getComponentVersionInfoNoAuth(OwnerType.APPLICATION, applicationPublicId, componentIdentifier,
        null, null, null);
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   * 
    * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @Authorize(permission = Permission.READ)
  public ComponentDetailsList getComponentDetailsList_ReadPermission(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      String matchState)
  {
    auditComponentAccess(componentIdentifier, null);
    final Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    ComponentDetailsList componentDetailsList = getComponentDetailsList(componentIdentifier, owner, null, null);
    componentDetailsLoader.augmentComponentDetails(owner, componentDetailsList.getList(), matchState);
    return componentDetailsList;
  }

  /**
   * Returns a list of component details for the given application and component identifier.
   * It also evaluates policies and returns max threat levels per category, as well as count of violated policies.
   *
   * This method is called by the CIP, so it needs to check the READ permission.
   */
  @Authorize(permission = Permission.READ)
  public ComponentVersionInfoDTO getComponentVersionInfo_ReadPermission(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId)
  {
    auditComponentAccess(componentIdentifier, null);
    return getComponentVersionInfoNoAuth(ownerType, ownerId, componentIdentifier, stageId, identificationSource,
        scanId);
  }

  public ComponentVersionInfoDTO getComponentVersionInfoNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String identificationSource,
      String scanId)
  {
    return getComponentVersionInfoNoAuth(ownerType, ownerId, componentIdentifier, null, identificationSource,
        scanId);
  }

  public ComponentVersionInfoDTO getComponentVersionInfoNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId)
  {
    List<ComponentDetailsDTO> componentDetailsDTOs = getComponentDetailsForAllVersionsNoAuth(ownerType, ownerId,
        componentIdentifier, stageId, identificationSource, scanId);

    ApiComponentRemediationValueDTO remediationDto;
    if (IdentificationSource.isThirdPartyIdentificationSource(identificationSource)) {
      Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
      remediationDto = thirdPartyComponentDAO.getSuggestedRemmediation(owner.getId(), componentIdentifier, scanId);
    }
    else {
      remediationDto = componentRemediationService.getSuggestedRemediation(componentIdentifier, componentDetailsDTOs,
          ownerType, ownerId, stageId);
    }
    return new ComponentVersionInfoDTO(componentDetailsDTOs, remediationDto);
  }

  public List<ComponentDetailsDTO> getComponentDetailsForAllVersionsNoAuth(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String stageId,
      String identificationSource,
      String scanId)
  {
    final Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);
    List<ComponentDetails> componentDetailsList =
        getComponentDetailsList(componentIdentifier, owner, identificationSource, scanId).getList();
    // Fix match state to exact as there's no point propagating it to other versions.
    List<Component> components = componentDetailsLoader.augmentComponentDetails(owner, componentDetailsList,
        MatchState.EXACT.getId());

    // Evaluate the policies and get the PolicyAlerts
    List<PolicyAlert> allPolicyAlerts = componentPolicyEvaluator
        .evaluate(owner.getId(), new Stage(stageId != null ? stageId : BuildStageType.ID), components);

    Map<ComponentIdentifier, List<PolicyAlert>> policyAlertsByComponent = new HashMap<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      for (ComponentFact componentFact : policyAlert.getTrigger().getComponentFacts()) {
        policyAlertsByComponent.computeIfAbsent(componentFact.getComponentIdentifier(), key -> new ArrayList<>())
            .add(policyAlert);
      }
    }

    // All policies that were part of this evaluation, indexed by id
    Map<String, Policy> policiesById = new PolicyDAO().getApplicableByOwnerId(owner.getId()).stream()
        .collect(Collectors.toMap(Policy::getId, Function.identity()));

    List<ComponentDetailsDTO> componentDetailsDTOs = new ArrayList<>(componentDetailsList.size());
    for (ComponentDetails componentDetails : componentDetailsList) {
      ComponentDetailsDTO dto = new ComponentDetailsDTO();
      dto.matchState = componentDetails.getMatchState();
      dto.declaredLicenses = componentDetails.getDeclaredLicenses();
      dto.observedLicenses = componentDetails.getObservedLicenses();
      dto.overriddenLicenses = componentDetails.getOverriddenLicenses();
      dto.effectiveLicenses = componentDetails.getEffectiveLicenses();
      dto.effectiveLicenseStatus = componentDetails.getEffectiveLicenseStatus();
      dto.catalogDate = componentDetails.getCatalogDate();
      dto.relativePopularity = componentDetails.getRelativePopularity();
      dto.website = componentDetails.getWebsite();
      dto.majorRevisionStep = componentDetails.isMajorRevisionStep();
      dto.identificationSource = componentDetails.getIdentificationSource();
      dto.identificationSourceComment = componentDetails.getIdentificationSourceComment();

      List<PolicyAlert> policyAlerts = policyAlertsByComponent
          .getOrDefault(componentDetails.getComponentIdentifier(), Collections.emptyList());

      dto.policyAlerts = policyAlerts;
      dto.policyMaxThreatLevelsByCategory = new HashMap<>();
      for (PolicyAlert policyAlert : policyAlerts) {
        PolicyFact policyFact = policyAlert.getTrigger();
        PolicyThreatCategory threatCategory = policiesById.get(policyFact.getPolicyId()).getThreatCategory();
        dto.policyMaxThreatLevelsByCategory.merge(threatCategory, policyFact.getThreatLevel(), Math::max);
      }

      dto.violatedPolicyCount = policyAlerts.stream().map(PolicyAlert::getTrigger).map(PolicyFact::getPolicyId)
          .collect(Collectors.toSet()).size();

      OptionalDouble highestSecurityVulnerabilitySeverity = componentDetails.getSecurityVulnerabilities().stream()
          .mapToDouble(SecurityVulnerability::getSeverity).max();

      dto.securityVulnerabilityCount = componentDetails.getSecurityVulnerabilities().size();
      dto.highestSecurityVulnerabilitySeverity = (float) highestSecurityVulnerabilitySeverity.orElse(0);

      dto.displayName = ComponentDisplayNameUtil.fromIdentifier(componentDetails.getComponentIdentifier());
      dto.componentIdentifier = componentDetails.getComponentIdentifier();
      componentDetailsDTOs.add(dto);
    }

    return componentDetailsDTOs;
  }

  private NamedComponentDetails getComponentDetails(
      String matchState,
      final String hash,
      final ComponentIdentifier identifier,
      final HttpServletRequest httpRequest,
      final Owner owner,
      final String identificationSource,
      final String scanId) throws IOException
  {
    if (IdentificationSource.isThirdPartyIdentificationSource(identificationSource)) {
      return thirdPartyComponentDAO.getComponentDetailsByIdentifier(identifier, owner.getId(), scanId);
    }
    return getComponentDetailsFromHDS(matchState, hash, identifier, httpRequest);
  }

  ComponentDetailsList getComponentDetailsList(
      ComponentIdentifier identifier,
      Owner owner,
      String identificationSource,
      String scanId)
  {
    long start = System.currentTimeMillis();

    if (identifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    ComponentDetailsList componentDetailsList;

    if (isThirdPartyIdentificationSource(identificationSource)) {
      componentDetailsList = thirdPartyComponentDAO.getAllVersions(owner.getId(), identifier, scanId);
    }
    else {
      String url = "rest/" + toolName + "/componentDetails/list";
      componentDetailsList = hdsClient.get(ComponentDetailsList.class, url,
          Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier)));
    }

    log.debug("Loaded component details list for {} versions of component identifier {} in {} ms.",
        componentDetailsList.getList().size(), identifier, System.currentTimeMillis() - start);

    return componentDetailsList;
  }

  /**
   *
   * @since 1.76
   */
  @Authorize(permission = Permission.READ)
  public ComponentLicenses getLicenses(@AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
                                       @AuthzContext(AuthzContext.Key.ID) final String ownerId,
                                       ComponentIdentifier componentIdentifier,
                                       HttpServletRequest httpRequest,
                                       String identificationSource,
                                       String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, null);
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    final Owner owner = IdUtils.getOwnerNotNull(ownerType, ownerId);

    ComponentLicenses result = new ComponentLicenses();

    ComponentDetails componentDetails;
    if (IdentificationSource.isThirdPartyIdentificationSource(identificationSource)) {
      componentDetails =
          thirdPartyComponentDAO.getComponentDetailsByIdentifier(componentIdentifier, owner.getId(), scanId);
    }
    else {
      componentDetails = getComponentDetailsFromHDS(null, null, componentIdentifier, httpRequest);
    }

    componentDetailsLoader.augmentComponentDetails(owner, componentDetails);
    result.declaredlicenses = getLicensesWithThreatLevels(owner, componentDetails.getDeclaredLicenses());
    result.observedlicenses = getLicensesWithThreatLevels(owner, componentDetails.getObservedLicenses());
    result.effectiveLicenses = getLicensesWithThreatLevels(owner, componentDetails.getEffectiveLicenses());
    result.selectableLicenses = new ArrayList<>(getSelectableLicenses(componentDetails.getDeclaredLicenses(),
        componentDetails.getObservedLicenses()));

    return result;
  }

  /**
   * @since 1.18.0
   */
  @Authorize(permission = Permission.READ)
  public ComponentSecurityVulnerabilities getSecurityVulnerabilities(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) final String ownerId,
      final String hash,
      final ComponentIdentifier componentIdentifier,
      final HttpServletRequest httpRequest,
      final String identificationSource,
      final String scanId) throws IOException
  {
    auditComponentAccess(componentIdentifier, hash);
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    String internalId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    Owner owner = new OwnerDAO().getById(internalId);

    ComponentDetails componentDetails =
        getComponentDetails(null, hash, componentIdentifier, httpRequest, owner, identificationSource, scanId);
    componentDetailsLoader.augmentComponentDetails(owner, componentDetails);
    return new ComponentSecurityVulnerabilities(componentDetails.getSecurityVulnerabilities());
  }

  private void auditComponentAccess(final ComponentIdentifier identifier, final String hash) {
    AuditData.get().setComponentIdentifier(identifier).setComponentHash(hash);
  }

  private Set<License> getSelectableLicenses(Collection<License> declared, Collection<License> observed) {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    Set<License> result = new LinkedHashSet<>();
    Set<License> licenses = new LinkedHashSet<>();
    licenses.addAll(declared);
    licenses.addAll(observed);
    for (final License license : licenses) {
      if (com.sonatype.insight.brain.model.license.License.isEffectivelyUnspecified(license.getLicenseId())) {
        continue;
      }

      MultiLicense multiLicense = multiLicenseDAO.getById(license.getLicenseId());
      Set<com.sonatype.insight.brain.model.license.License> dbLicenses = multiLicenseDAO
          .getLicensesByMultiLicenseIdNotNull(multiLicense.getId());
      for (com.sonatype.insight.brain.model.license.License dbLicense : dbLicenses) {
        if (dbLicense.getId().endsWith("-UNSPECIFIED")) {
          String licenseIdPrefix = dbLicense.getId().substring(0, dbLicense.getId().length() - "UNSPECIFIED".length());
          for (com.sonatype.insight.brain.model.license.License otherLicense : licenseDAO.getAll()) {
            if (otherLicense.getId().startsWith(licenseIdPrefix) && !dbLicense.getId().equals(otherLicense.getId())) {
              result.add(new License(otherLicense.getId(), otherLicense.getShortDisplayName()));
            }
          }
        }
        result.add(new License(dbLicense.getId(), dbLicense.getShortDisplayName()));
      }
    }
    return result;
  }

  /**
   * @since 1.6
   */
  private List<LicenseWithThreatLevel> getLicensesWithThreatLevels(Owner owner, Set<License> multiLicenses) {
    List<LicenseWithThreatLevel> result = new ArrayList<>();

    if (multiLicenses != null) {
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      for (License multiLicense : multiLicenses) {
        Set<com.sonatype.insight.brain.model.license.License> licenses = multiLicenseDAO
            .getLicensesByMultiLicenseIdNotNull(multiLicense.getLicenseId());
        for (com.sonatype.insight.brain.model.license.License license : licenses) {
          LicenseWithThreatLevel licenseWithThreatLevel = LicenseUtils.getLicenseWithThreatLevel(owner, license);
          result.add(licenseWithThreatLevel);
        }
      }
    }

    return result;
  }

  /**
   * @since 1.18.0
   */
  public static class ComponentSecurityVulnerabilities
  {
    public List<SecurityVulnerability> securityVulnerabilities;

    public ComponentSecurityVulnerabilities() {
    }

    public ComponentSecurityVulnerabilities(final List<SecurityVulnerability> securityVulnerabilities) {
      this.securityVulnerabilities = securityVulnerabilities;
    }
  }

  /**
   * @since 1.6
   */
  public static class ComponentLicenses
  {
    public List<LicenseWithThreatLevel> declaredlicenses;

    public List<LicenseWithThreatLevel> observedlicenses;

    /**
     * @since 1.12
     */
    public List<LicenseWithThreatLevel> effectiveLicenses;

    /**
     * @since 1.13
     */
    public List<License> selectableLicenses;
  }

  /**
   * @since 1.6
   */
  public static class LicenseWithThreatLevel
  {
    public License license;

    public Integer threatLevel;
  }

  public void setToolName(String toolName) {
    this.toolName = toolName;
  }
}
