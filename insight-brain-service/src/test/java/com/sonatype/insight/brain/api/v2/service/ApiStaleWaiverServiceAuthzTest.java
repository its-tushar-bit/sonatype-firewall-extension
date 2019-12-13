/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiverDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiStaleWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiStaleWaiverService apiStaleWaiverService;

  @Test
  public void testGetStaleRepositoryWaivers_Unauthenticated() {
    assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetStaleRepositoryWaivers_UnauthorizedButAuthenticated() {
    login();
    assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated();
  }

  @Test
  public void testGetStaleRepositoryWaivers_StaleWaiversInReadableAndUnreadableRepos() {
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");

    // only has access to repo1
    grantPermission(repo1.getId(), Permission.READ);

    // create waivers, but they are not applied
    PolicyWaiver repo1PolicyWaiver = tempEntity.newWaiver("hash1", policy.getId(), repo1.getId(),
        null, "Some comments here");
    tempEntity.newWaiver("hash2", policy.getId(), repo2.getId(), null, "Some comments here2");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleRepositoryWaivers();

    assertThat(staleRepositoryWaivers).hasSize(1);
    ApiStaleWaiverDTO policyWaiver = staleRepositoryWaivers.get(0);
    assertThat(policyWaiver).isNotNull();
    assertThat(policyWaiver.waiverId).isEqualTo(repo1PolicyWaiver.getId());
    assertThat(policyWaiver.comment).isEqualTo(repo1PolicyWaiver.getComment());
    assertThat(policyWaiver.createTime).isEqualTo(repo1PolicyWaiver.getCreateTime());
    assertThat(policyWaiver.isObsolete).isEqualTo(true);
  }

  @Test
  public void testGetStaleRepositoryWaivers_WaiversUsedInUnReadableRepos() {
    Date date = new Date();
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo1 = tempEntity.newRepository("repo1");
    Repository repo2 = tempEntity.newRepository("repo2");
    ConstraintFact constraintFact = new ConstraintFact("constraintFact1", "aa c", "OR");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // only has access to repo1
    grantPermission(repo1.getId(), Permission.READ);

    PolicyWaiver repo2PolicyWaiver = tempEntity.newWaiver("hash1", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        null, "Some comments here");
    tempEntity.newRepositoryPolicyViolation(
        repo2.getId(), 6, "pathName1", "hash1", Arrays.asList(constraintFact), true, true,
        "actionId1", policy.getId(), policy.getName(), componentIdentifier, date,
        repo2PolicyWaiver.getId(), repo2PolicyWaiver.getComment(), date);

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleRepositoryWaivers();
    assertThat(staleRepositoryWaivers).isEmpty();
  }

  private void assertEmptyStaleRepositoryWaiversWhenUnauthorizedOrAuthenticated() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId());
    Repository repo = tempEntity.newRepository("repo");

    // create waiver, but it is not applied
    tempEntity.newWaiver("hash", policy.getId(), repo.getId(), null, "Some comments here");

    List<ApiStaleWaiverDTO> staleRepositoryWaivers = apiStaleWaiverService.getStaleRepositoryWaivers();
    assertThat(staleRepositoryWaivers).isEmpty();
  }
}
