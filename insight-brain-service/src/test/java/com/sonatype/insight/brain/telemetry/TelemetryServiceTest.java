/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class TelemetryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private TelemetryService telemetryService;

  private TelemetrySender mockTelemetrySender;

  @Test
  public void testForwardFrontendTelemetryToHds() {
    String sessionId = "foo-session";
    String hashedSessionId = "d8fa4b2326f092919dc983bd895cabe6c10144f8359ec9d2787113908fcd0c7e";
    String groupName = "group";

    // Add the user to a group
    ((UserPrincipal) subject.getPrincipal()).getMembership().add(groupName);

    Application application1 = tempEntity.newApplicationWithParent();
    String applicationId1 = application1.getId();
    Application application2 = tempEntity.newApplicationWithParent();
    String applicationId2 = application2.getId();

    Role customRole = tempEntity.newRole("Custom Role", "Custom Role", false);
    String customRoleId = customRole.getId();

    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID, USERNAME);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID, USERNAME);
    tempEntity.newMembershipMapping(applicationId1, Role.DEVELOPER_ROLE_ID, groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(applicationId1, Role.APPLICATION_EVALUATOR_ROLE_ID, USERNAME);
    tempEntity.newMembershipMapping(applicationId1, Role.COMPONENT_EVALUATOR_ROLE_ID, USERNAME);
    tempEntity.newMembershipMapping(applicationId1, customRoleId, groupName, MemberType.GROUP);

    tempEntity.newMembershipMapping(applicationId2, Role.OWNER_ROLE_ID, USERNAME);

    TelemetryData input = new TelemetryData(TelemetryPurpose.GETTING_STARTED_USAGE, 1L);
    Map<String, Object> inputAttrs = input.getAttributes();
    inputAttrs.put("attr1", "value1");
    inputAttrs.put("attr2", Arrays.asList("value2", "value3", 4));

    TelemetryData expectedByTelemetrySender = new TelemetryData(TelemetryPurpose.GETTING_STARTED_USAGE, 1L);
    Map<String, Object> expectedAttrs = expectedByTelemetrySender.getAttributes();
    Set<String> expectedRoles = new HashSet<>(Arrays.asList("Policy Administrator", "System Administrator", "Owner",
        "Developer", "Application Evaluator", "Component Evaluator", "CUSTOM"));

    expectedAttrs.putAll(inputAttrs);
    expectedAttrs.put(TelemetryService.SESSION_ID_ATTR, hashedSessionId);
    expectedAttrs.put(TelemetryService.USER_ROLES_ATTR, expectedRoles);

    telemetryService.forwardFrontendTelemetryToHds(input, sessionId);

    verify(mockTelemetrySender).send(expectedByTelemetrySender);
  }
}
