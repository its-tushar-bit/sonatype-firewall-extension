/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoriesListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.tag.TagService;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@ComponentH2Test
public class ApiApplicationServiceTest
    extends AbstractComponentH2Test
{
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private ApiApplicationService applicationService;

  @Inject
  private MembershipMappingDAO membershipMappingDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = Organization.ROOT_ORGANIZATION_ID;
    assertThatExceptionOfType(InvalidApplicationException.class)
        .isThrownBy(() -> applicationService.addApplication(app))
        .withMessage("Applications cannot have the root organization as parent.");
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = org.getId();
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = membershipMappingDAO
        .getByContextIdAndRoleId(app.id, Role.OWNER_ROLE_ID);
    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getMemberName()).isEqualTo(USERNAME);
    assertThat(mappings.get(0).getMemberType()).isEqualTo(MemberType.USER);
  }

  @Test
  public void testDeleteApplication_PolicyViolationLogger_LogsClearEvent() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    Date before = new Date();
    applicationService.deleteApplication(app.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertApplicationPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, org, app,
            before, after);
  }

  @Test
  public void testGetApplicationsWithReadPermission() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    Application app4 = tempEntity.newApplicationWithParent();
    Application app5 = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    List<Application> applications = applicationService.getApplicationsWithReadPermission(Sets.newHashSet(
        StringUtils.swapCase(app1.getPublicId()),
        app2.getPublicId(),
        StringUtils.swapCase(app4.getPublicId()),
        app5.getPublicId()));

    assertThat(applications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(app1, app2, app4, app5);
  }

  @Test
  public void testGetApplicationsWithReadPermission_ExcludeRepositoryManagerAndRelatedRepository() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();
    Organization orgExclude = tempEntity.newOrganizationWithRepositoryManager("org-exclude");
    tempEntity.newApplication(orgExclude.getId());
    tempEntity.newApplication(orgExclude.getId());
    List<Application> applications = applicationService.getApplicationsWithReadPermission(Collections.emptySet());

    assertThat(applications)
        .noneMatch(app -> orgExclude.getId().equals(app.getOrganizationId()));
  }

  @Test
  public void testGetApplicationsByOrganizationId() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    tempEntity.newApplicationWithParent();

    ApiApplicationListDTO apiApplicationListDTO =
        applicationService.getApplicationsByOrganizationId(app1.getOrganizationId());

    assertThat(apiApplicationListDTO).isNotNull();
    assertThat(apiApplicationListDTO.applications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(ApiApplicationAdapter.convertToDTO(app1, Collections.emptyList()),
            ApiApplicationAdapter.convertToDTO(app2, Collections.emptyList()));
  }

  @Test
  public void testGetApplicationsByOrganizationId_NoApplications() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplicationWithParent();

    ApiApplicationListDTO apiApplicationListDTO =
        applicationService.getApplicationsByOrganizationId(org.getId());

    assertThat(apiApplicationListDTO).isNotNull();
    assertThat(apiApplicationListDTO.applications).isEmpty();
  }

  @Test
  public void testGetApplicationsWithAppliedCategories() {
    Tag rootOrgTag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Tag otherRootOrgTag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    Organization org = tempEntity.newOrganization();
    Tag orgTag = tempEntity.newTag(org.getId());
    Tag otherOrgTag = tempEntity.newTag(org.getId());
    Application appWithNoTags = tempEntity.newApplication(org.getId());
    Application appWithRootOrgTag = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(appWithRootOrgTag.getId(), rootOrgTag.getId());
    Application appWithOrgTag = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(appWithOrgTag.getId(), orgTag.getId());
    Application appWithRootAndOrgTag = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(appWithRootAndOrgTag.getId(), rootOrgTag.getId());
    tempEntity.newApplicationTag(appWithRootAndOrgTag.getId(), orgTag.getId());
    Application appWithMultipleRootAndOrgTags = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(appWithMultipleRootAndOrgTags.getId(), rootOrgTag.getId());
    tempEntity.newApplicationTag(appWithMultipleRootAndOrgTags.getId(), otherRootOrgTag.getId());
    tempEntity.newApplicationTag(appWithMultipleRootAndOrgTags.getId(), orgTag.getId());
    tempEntity.newApplicationTag(appWithMultipleRootAndOrgTags.getId(), otherOrgTag.getId());

    ApiApplicationCategoriesListDTO applicationsCategories =
        applicationService.getApplicationsWithAppliedCategories(Collections.emptySet());

    Map<Application, List<Tag>> expected = new HashMap<>();
    expected.put(appWithNoTags, Collections.emptyList());
    expected.put(appWithRootOrgTag, Collections.singletonList(rootOrgTag));
    expected.put(appWithOrgTag, Collections.singletonList(orgTag));
    expected.put(appWithRootAndOrgTag, Arrays.asList(rootOrgTag, orgTag));
    expected.put(appWithMultipleRootAndOrgTags, Arrays.asList(rootOrgTag, otherRootOrgTag, orgTag, otherOrgTag));
    assertApiApplicationCategoriesListDTO(applicationsCategories, expected);
  }

  @Test
  public void testGetApplicationsWithAppliedCategories_Filtered() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    ApiApplicationCategoriesListDTO applicationsCategories =
        applicationService.getApplicationsWithAppliedCategories(Collections.singleton(app.getPublicId()));

    Map<Application, List<Tag>> expected = new HashMap<>();
    expected.put(app, Collections.emptyList());
    assertApiApplicationCategoriesListDTO(applicationsCategories, expected);
  }

  @Test
  public void testGetApplicationsWithAppliedCategories_NoApplications() {
    ApiApplicationCategoriesListDTO applicationsCategories =
        applicationService.getApplicationsWithAppliedCategories(Collections.emptySet());

    assertThat(applicationsCategories).isNotNull();
    assertThat(applicationsCategories.applications).isEmpty();
  }

  @Test
  public void testUpdateApplication() {
    // Given
    Application app = tempEntity.newApplicationWithParent();
    app.setName("New Name");

    // When
    final ApiApplicationDTO updatedApp =
        applicationService.updateApplication(ApiApplicationAdapter.convertToDTO(app, Collections.emptyList()));

    // Then
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REAL_OWNER_IDS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());

    final Map<String, Object> attributesMap = telemetryData.getAttributes();
    assertThat(attributesMap).containsKey(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);

    final OwnerMaintenanceTelemetry actualAttributes =
        (OwnerMaintenanceTelemetry) attributesMap.get(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY);
    assertThat(actualAttributes.getOwnerId()).isEqualTo(app.getId());
    assertThat(actualAttributes.getOwnerName()).isEqualTo(updatedApp.name);
    assertThat(actualAttributes.getOwnerMaintenanceType()).isEqualTo(OwnerMaintenanceTelemetry.TYPE_UPDATE);
  }

  @Test
  public void testAddApplication_LicenseLimit() {
    testProductLicense.setMaxApplications(0);

    Organization org = tempEntity.newOrganization();

    ApiApplicationDTO app1 = new ApiApplicationDTO();
    app1.publicId = "app1";
    app1.name = "app1";
    app1.organizationId = org.getId();
    assertThatExceptionOfType(PaymentRequiredException.class)
        .isThrownBy(() -> applicationService.addApplication(app1));

    testProductLicense.setMaxApplications(1);
    assertThatCode(() -> applicationService.addApplication(app1)).doesNotThrowAnyException();

    ApiApplicationDTO app2 = new ApiApplicationDTO();
    app2.publicId = "app2";
    app2.name = "app2";
    app2.organizationId = org.getId();
    assertThatExceptionOfType(PaymentRequiredException.class)
        .isThrownBy(() -> applicationService.addApplication(app2));

    testProductLicense.setMaxApplications(2);
    assertThatCode(() -> applicationService.addApplication(app2)).doesNotThrowAnyException();
  }

  private void assertApiApplicationCategoriesListDTO(
      ApiApplicationCategoriesListDTO actual,
      Map<Application, List<Tag>> tagsByApplication)
  {
    assertThat(actual.applications).hasSize(tagsByApplication.size());
    for (Entry<Application, List<Tag>> entry : tagsByApplication.entrySet()) {
      assertApiApplicationCategoriesDTO(
          actual.applications.stream()
              .filter(dto -> dto.id.equals(entry.getKey().getId()))
              .findFirst()
              .orElseThrow(() -> new RuntimeException("Not found")),
          entry.getKey(), entry.getValue());
    }
  }

  private void assertApiApplicationCategoriesDTO(ApiApplicationCategoriesDTO actual, Application app, List<Tag> tags) {
    assertThat(actual.id).isEqualTo(app.getId());
    assertThat(actual.publicId).isEqualTo(app.getPublicId());
    assertThat(actual.organizationId).isEqualTo(app.getOrganizationId());
    assertThat(actual.name).isEqualTo(app.getName());
    assertThat(actual.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(actual.categories).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            tags.stream().map(TagService::toDTO).toArray(ApiApplicationCategoryDTO[]::new));
  }
}
