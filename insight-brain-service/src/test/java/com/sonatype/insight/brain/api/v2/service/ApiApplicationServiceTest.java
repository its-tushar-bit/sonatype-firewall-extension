/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiApplicationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private ApiApplicationService applicationService;

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = Organization.ROOT_ORGANIZATION_ID;
    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.addApplication(app);
    }).withMessage("Applications cannot have the root organization as parent.");
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = org.getId();
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = new MembershipMappingDAO()
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
  public void testGetApplications() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    Application app4 = tempEntity.newApplicationWithParent();
    Application app5 = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    List<Application> applications = applicationService.getApplications(Sets.newHashSet(
        StringUtils.swapCase(app1.getPublicId()),
        app2.getPublicId(),
        StringUtils.swapCase(app4.getPublicId()),
        app5.getPublicId()));

    assertThat(applications).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(app1, app2, app4, app5);
  }
}
