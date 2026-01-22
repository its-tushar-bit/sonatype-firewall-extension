/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class ManualPullRequestServiceTest
    extends AbstractServiceAuthzTest
{
  private static final String SUGGESTED_VERSION = "1.0.0";

  private static final String VALID_STAGE = SourceStageType.ID;

  private static final DependencyType VALID_DEPENDENCY_TYPE = DependencyType.DIRECT;

  public static final List<String> SUPPORTED_FORMATS = Arrays.asList("maven", "npm", "golang");

  public static final ComponentIdentifier SUPPORTED_FORMAT_MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

  @Inject
  private StageTypeService stageTypeService;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;
  
  @Mock
  private TenantUtil mockTenantUtil;

  @Inject
  private ManualPullRequestService manualPullRequestService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(ScmRepoVisibilityService.class).toInstance(mockScmRepoVisibilityService);
    binder.bind(TenantUtil.class).toInstance(mockTenantUtil);
  }

  @Before
  public void setup() {
    grantPermission(app.getId(), Permission.CREATE_PULL_REQUESTS);
    lenient().when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(any())).thenReturn(true);
    tempEntity.newSourceControl(getSourceControl());
  }

  @Test
  public void testIsManualPullRequestPossible_True_ValidSuggestedVersion() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange("1.0.0");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            app, remediationDto);

    assertThat(result).isEmpty();
  }

  @Test
  public void testIsManualPullRequestPossible_True_ValidVersionChanges() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();

    // testing that manual pull request is possible for all version change types
    for (ApiVersionChangeOptionType versionChangeType : ComponentRemediationService.PREFERABLE_TYPE_ORDER) {
      remediationDto.versionChanges = getVersionChanges(Map.of("1.0.0", versionChangeType));

      Optional<ManualPullRequestImpossibilityReason> result =
          manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
              VALID_DEPENDENCY_TYPE,
              app,
              remediationDto);

      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_NullRemediationDto() {
    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            app, null);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
  }

  @Test
  public void testIsManualPullRequestPossible__False_EmptySuggestedVersionAndVersionChanges() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            app,
            remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
  }

  @Test
  public void testIsManualPullRequestPossible_True_ValidStages() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange("1.0.0");

    Optional<ManualPullRequestImpossibilityReason> result;
    Collection<StageType> stageTypes = stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT);
    for (StageType stageType : stageTypes) {
      if (stageType instanceof DevelopStageType) {
        continue;
      }
      result =
          manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, stageType.getId(),
              VALID_DEPENDENCY_TYPE, app, remediationDto);

      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_InvalidStages() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange(SUGGESTED_VERSION);

    Optional<ManualPullRequestImpossibilityReason> result;
    Collection<StageType> invalidStageTypes = new ArrayList<>(StageTypes.getAll());
    invalidStageTypes.removeAll(stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT));
    invalidStageTypes.add(new DevelopStageType());
    for (StageType stageType : invalidStageTypes) {
      result =
          manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, stageType.getId(),
              VALID_DEPENDENCY_TYPE, app, remediationDto);

      assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.UNSUPPORTED_STAGE);
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_InvalidDependencyType() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange(SUGGESTED_VERSION);

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            DependencyType.TRANSITIVE,
            app, remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.UNSUPPORTED_DEPENDENCY_TYPE);

    result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            DependencyType.INNER_SOURCE,
            app, remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.UNSUPPORTED_DEPENDENCY_TYPE);
  }

  @Test
  public void testIsManualPullRequestPossible_True_FormatSupported() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange(SUGGESTED_VERSION);

    for (String format : SUPPORTED_FORMATS) {
      Map<String, String> coordinates = ComponentIdentifier.getAllRequiredCoordinateNames(format).stream()
          .collect(Collectors.toMap(name -> name, name -> name));
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(format, coordinates);
      Optional<ManualPullRequestImpossibilityReason> result =
          manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE, VALID_DEPENDENCY_TYPE,
              app, remediationDto);

      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_FormatNotSupported() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange(SUGGESTED_VERSION);

    Collection<String> notSupportedFormats = new ArrayList<>(ComponentIdentifier.getAllFormats());
    notSupportedFormats.removeAll(SUPPORTED_FORMATS);
    for (String format : notSupportedFormats) {
      Map<String, String> coordinates = ComponentIdentifier.getAllRequiredCoordinateNames(format).stream()
          .collect(Collectors.toMap(name -> name, name -> name));
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(format, coordinates);
      Optional<ManualPullRequestImpossibilityReason> result =
          manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE, VALID_DEPENDENCY_TYPE,
              app, remediationDto);

      assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.UNSUPPORTED_FORMAT);
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_RemediationBranchExist() {
    SourceControlEvent sourceControlEvent =
        new SourceControlEvent().forRemediationPullRequest()
            .setBranchName(app.getId().substring(0, 6) + "/g/a/e/c/v-to-1.0.0");
    sourceControlEvent.setApplicationId(app.getId());
    sourceControlEventDAO.insert(sourceControlEvent);
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange(SUGGESTED_VERSION);

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            app, remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.REMEDIATION_EVENT_EXISTS);
  }

  @Test
  public void testIsManualPullRequestPossible_False_ScmNotConfigured() {
    SourceControl sourceControl = getSourceControl();
    sourceControl.setToken("invalidToken");
    sourceControlDAO.updateWithoutValidation(sourceControl);

    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange("1.0.0");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            app, remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
  }

  @Test
  public void testIsManualPullRequestPossible_False_UnSupportedOwnerType() {
    Repository repository = tempEntity.newRepository("Repository 1");
    grantPermission(repository.getId(), Permission.CREATE_PULL_REQUESTS);
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    remediationDto.suggestedVersionChange = getSuggestedVersionChange("1.0.0");

    Optional<ManualPullRequestImpossibilityReason> result =
        manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
            VALID_DEPENDENCY_TYPE,
            repository,
            remediationDto);

    assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.UNSUPPORTED_OWNER_TYPE);
  }

  @Test
  public void testIsManualPullRequestPossible_False_SuggestedVersionIsCurrentVersion() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();

    for (ApiVersionChangeOptionType versionChangeType : ComponentRemediationService.PREFERABLE_TYPE_ORDER) {
      remediationDto.versionChanges = getVersionChanges(Map.of("v", versionChangeType));

      Optional<ManualPullRequestImpossibilityReason> result =
              manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
                      VALID_DEPENDENCY_TYPE,
                      app,
                      remediationDto);

      assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_MTIQ() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(true);

    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    for (ApiVersionChangeOptionType versionChangeType : ComponentRemediationService.PREFERABLE_TYPE_ORDER) {
      remediationDto.versionChanges = getVersionChanges(Map.of("v", versionChangeType));

      Optional<ManualPullRequestImpossibilityReason> result =
          manualPullRequestService.isManualPullRequestPossible(SUPPORTED_FORMAT_MAVEN_COORDINATES, VALID_STAGE,
              VALID_DEPENDENCY_TYPE,
              app,
              remediationDto);

      assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_MTIQ);
    }
  }

  @Test
  public void testIsManualPullRequestPossible_False_SuggestedVersionIsCurrentVersion_DifferentVersionStrings() {
    ApiComponentRemediationValueDTO remediationDto = new ApiComponentRemediationValueDTO();
    for (ApiVersionChangeOptionType versionChangeType : ComponentRemediationService.PREFERABLE_TYPE_ORDER) {
      remediationDto.versionChanges = getVersionChanges(Map.of("1.0.00", versionChangeType));

      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createMavenCoordinates("g", "a", "1.0.0", "c", "e");
      Optional<ManualPullRequestImpossibilityReason> result =
          manualPullRequestService.isManualPullRequestPossible(componentIdentifier, VALID_STAGE,
              VALID_DEPENDENCY_TYPE,
              app,
              remediationDto);

      assertThat(result).isPresent().contains(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }
  }

  private SourceControl getSourceControl() {
    SourceControl sourceControl = new SourceControl();
    sourceControl.setId("id");
    sourceControl.setOwnerId(app.getId());
    sourceControl.setRepositoryUrl("https://github.com/test/repo");
    sourceControl.setRepositorySshUrl("git@github.com:test/repo.git");
    sourceControl.setToken(passwordHandler.encryptPassword("testToken"));
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    sourceControl.setBaseBranch("main");
    sourceControl.setRemediationPullRequestsEnabled(true);
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setStatusChecksEnabled(true);
    sourceControl.setPullRequestCommentingEnabled(true);
    sourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControl.setSshEnabled(true);
    sourceControl.setSourceControlScanTarget("testScanTarget");
    return sourceControl;
  }

  private ApiSuggestedVersionChangeOptionDTO getSuggestedVersionChange(
      String version)
  {
    ApiComponentDTOV2 component = getComponent(version);
    ApiComponentChangeActionDTO componentChangeAction = new ApiComponentChangeActionDTO();
    componentChangeAction.setComponent(component);

    ApiSuggestedVersionChangeOptionDTO suggestedVersionChange = new ApiSuggestedVersionChangeOptionDTO();
    suggestedVersionChange.setData(componentChangeAction);

    return suggestedVersionChange;
  }

  private List<ApiVersionChangeOptionDTO> getVersionChanges(
      Map<String, ApiVersionChangeOptionType> versionMap)
  {
    List<ApiVersionChangeOptionDTO> versionChanges = new ArrayList<>();
    for (Map.Entry<String, ApiVersionChangeOptionType> entry : versionMap.entrySet()) {
      ApiComponentDTOV2 component = getComponent(entry.getKey());
      ApiComponentChangeActionDTO componentChangeAction = new ApiComponentChangeActionDTO();
      componentChangeAction.setComponent(component);

      ApiVersionChangeOptionDTO versionChange = new ApiVersionChangeOptionDTO();
      versionChange.setData(componentChangeAction);
      versionChange.setType(entry.getValue());

      versionChanges.add(versionChange);
    }

    return versionChanges;
  }

  private ApiComponentDTOV2 getComponent(String version) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();

    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", version, "c", "e"));
    return component;
  }
}
