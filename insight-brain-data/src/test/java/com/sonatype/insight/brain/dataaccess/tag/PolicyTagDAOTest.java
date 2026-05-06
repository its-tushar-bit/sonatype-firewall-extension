/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.9
 */
public class PolicyTagDAOTest
    extends AbstractDbDAOTest
{
  private PolicyTagDAO dao;

  private Tag tag;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyTagDAO();
    tag = tempEntity.newTag(organization.getId());
  }

  @Test
  public void testCRUD() {
    Policy policy = tempEntity.newPolicy(organization);
    String policyId = policy.getId();

    // Create
    PolicyTag policyTag = new PolicyTag(policyId, tag.getId());
    dao.insert(policyTag);
    assertThat(policyTag.getId()).isNotNull();

    // Get
    policyTag = dao.getById(policyTag.getId());
    assertThat(policyTag).isNotNull();
    assertPolicyTag(policyId, tag.getId(), policyTag);

    // Delete
    dao.delete(policyTag);

    // Get
    policyTag = dao.getById(policyTag.getId());
    assertThat(policyTag).isNull();
  }

  @Test
  public void testUpdateNotSupported() {
    Policy policy = tempEntity.newPolicy(organization);
    PolicyTag policyTag = new PolicyTag(policy.getId(), tag.getId());
    dao.insert(policyTag);

    PolicyTag updatedPolicyTag = new PolicyTag("updated_policy_id", tag.getId());
    updatedPolicyTag.setId(policyTag.getId());

    assertThatThrownBy(() -> dao.update(updatedPolicyTag)).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The PolicyTag table does not support update operations");
  }

  @Test
  public void testGetByPolicyId() {
    Policy policy1 = tempEntity.newPolicy(organization);
    String policy1Id = policy1.getId();
    Policy policy2 = tempEntity.newPolicy(organization);
    String policy2Id = policy2.getId();
    List<Tag> policy1Tags = new ArrayList<>();
    List<Tag> policy2Tags = new ArrayList<>();

    policy1Tags.add(tempEntity.newTag(organization.getId()));
    policy1Tags.add(tempEntity.newTag(organization.getId()));
    policy2Tags.add(tempEntity.newTag(organization.getId()));
    policy2Tags.add(tempEntity.newTag(organization.getId()));

    for (Tag tag : policy1Tags) {
      tempEntity.newPolicyTag(policy1Id, tag.getId());
    }

    for (Tag tag : policy2Tags) {
      tempEntity.newPolicyTag(policy2Id, tag.getId());
    }

    assertPolicyTags(policy1Id, policy1Tags, dao.getByPolicyId(policy1Id));
    assertPolicyTags(policy2Id, policy2Tags, dao.getByPolicyId(policy2Id));
  }

  @Test
  public void testGetByOrgId() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");

    List<PolicyTag> org1PolicyTags = new ArrayList<>();
    List<PolicyTag> org2PolicyTags = new ArrayList<>();

    Policy policy1 = tempEntity.newPolicy(org1);
    Policy policy2 = tempEntity.newPolicy(org1);
    Policy policy3 = tempEntity.newPolicy(org2);
    Policy policy4 = tempEntity.newPolicy(org2);

    // Create tags and apply to policies
    org1PolicyTags.add(tempEntity.newPolicyTag(policy1.getId(), tempEntity.newTag(org1.getId()).getId()));
    org1PolicyTags.add(tempEntity.newPolicyTag(policy2.getId(), tempEntity.newTag(org1.getId()).getId()));
    org1PolicyTags.add(tempEntity.newPolicyTag(policy1.getId(), tempEntity.newTag(org1.getId()).getId()));
    org1PolicyTags.add(tempEntity.newPolicyTag(policy2.getId(), org1PolicyTags.get(2).getTagId()));

    org2PolicyTags.add(tempEntity.newPolicyTag(policy3.getId(), tempEntity.newTag(org2.getId()).getId()));
    org2PolicyTags.add(tempEntity.newPolicyTag(policy4.getId(), tempEntity.newTag(org2.getId()).getId()));
    org2PolicyTags.add(tempEntity.newPolicyTag(policy3.getId(), tempEntity.newTag(org2.getId()).getId()));
    org2PolicyTags.add(tempEntity.newPolicyTag(policy4.getId(), org2PolicyTags.get(2).getTagId()));

    // Create tags but do not apply to policies
    tempEntity.newTag(org1.getId());
    tempEntity.newTag(org2.getId());

    assertPolicyTags(org1PolicyTags, dao.getByOrganizationId(org1.getId()));
    assertPolicyTags(org2PolicyTags, dao.getByOrganizationId(org2.getId()));
  }

  @Test
  public void testGetByOrganizationIds() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3"); // No policy tags for this org

    List<PolicyTag> org1PolicyTags = new ArrayList<>();
    List<PolicyTag> org2PolicyTags = new ArrayList<>();

    Policy policy1 = tempEntity.newPolicy(org1);
    Policy policy2 = tempEntity.newPolicy(org1);
    Policy policy3 = tempEntity.newPolicy(org2);
    Policy policy4 = tempEntity.newPolicy(org2);

    // Create tags and apply to policies for org1
    org1PolicyTags.add(tempEntity.newPolicyTag(policy1.getId(), tempEntity.newTag(org1.getId()).getId()));
    org1PolicyTags.add(tempEntity.newPolicyTag(policy2.getId(), tempEntity.newTag(org1.getId()).getId()));
    org1PolicyTags.add(tempEntity.newPolicyTag(policy1.getId(), tempEntity.newTag(org1.getId()).getId()));

    // Create tags and apply to policies for org2
    org2PolicyTags.add(tempEntity.newPolicyTag(policy3.getId(), tempEntity.newTag(org2.getId()).getId()));
    org2PolicyTags.add(tempEntity.newPolicyTag(policy4.getId(), tempEntity.newTag(org2.getId()).getId()));

    // Batch fetch for org1 and org2
    List<PolicyTag> batchResults = dao.getByOrganizationIds(Arrays.asList(org1.getId(), org2.getId()));
    assertThat(batchResults).hasSize(org1PolicyTags.size() + org2PolicyTags.size());

    // Verify all expected policy tags are in the batch results
    Set<String> expectedIds = new HashSet<>();
    for (PolicyTag pt : org1PolicyTags) {
      expectedIds.add(pt.getId());
    }
    for (PolicyTag pt : org2PolicyTags) {
      expectedIds.add(pt.getId());
    }

    Set<String> actualIds = new HashSet<>();
    for (PolicyTag pt : batchResults) {
      actualIds.add(pt.getId());
    }
    assertThat(actualIds).isEqualTo(expectedIds);
  }

  @Test
  public void testGetByOrganizationIds_EmptyCollection() {
    assertThat(dao.getByOrganizationIds(Collections.emptyList())).isEmpty();
    assertThat(dao.getByOrganizationIds(null)).isEmpty();
  }

  @Test
  public void testGetByOrganizationIdsWithNonMatchingIds() {
    Policy policy = tempEntity.newPolicy(organization);
    tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(organization.getId()).getId());

    // Query with non-existent organization IDs
    List<PolicyTag> results = dao.getByOrganizationIds(Arrays.asList("non-existent-org-1", "non-existent-org-2"));
    assertThat(results).isEmpty();
  }

  private void assertPolicyTag(String policyId, String tagId, PolicyTag actual) {
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getTagId()).isEqualTo(tagId);
  }

  private void assertPolicyTags(String policyId, List<Tag> expected, List<PolicyTag> actual) {
    assertThat(actual).hasSameSizeAs(expected);

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (PolicyTag policyTag : actual) {
      assertThat(policyTag.getPolicyId()).isEqualTo(policyId);
      assertThat(policyTag.getTagId()).isIn(tagIds);
    }
  }

  private void assertPolicyTags(List<PolicyTag> expected, List<PolicyTag> actual) {
    assertThat(actual).hasSameSizeAs(expected);

    Map<String, PolicyTag> expectedPolicyTags = new HashMap<>();
    for (PolicyTag policyTag : expected) {
      expectedPolicyTags.put(policyTag.getId(), policyTag);
    }

    for (PolicyTag policyTag : actual) {
      PolicyTag expectedPolicyTag = expectedPolicyTags.get(policyTag.getId());
      assertThat(expectedPolicyTag).isNotNull();
      assertThat(policyTag.getTagId()).isEqualTo(expectedPolicyTag.getTagId());
      assertThat(policyTag.getPolicyId()).isEqualTo(expectedPolicyTag.getPolicyId());
    }
  }

  @Test
  public void testIsPolicyApplicable() {
    Policy policy = tempEntity.newPolicy(organization);

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThat(dao.isPolicyApplicable(tx, policy.getId(), Collections.singleton(tag.getId()))).isTrue();

      tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(organization.getId()).getId());
      assertThat(dao.isPolicyApplicable(tx, policy.getId(), Collections.singleton(tag.getId()))).isFalse();

      tempEntity.newPolicyTag(policy.getId(), tag.getId());
      assertThat(dao.isPolicyApplicable(tx, policy.getId(), Collections.singleton(tag.getId()))).isTrue();
    }
  }
}
