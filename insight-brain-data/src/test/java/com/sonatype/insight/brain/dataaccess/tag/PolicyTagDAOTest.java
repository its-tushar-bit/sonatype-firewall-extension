/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class PolicyTagDAOTest
    extends AbstractDbDAOTest
{
  private PolicyTagDAO dao = new PolicyTagDAO();

  private Tag tag;

  private String policyId = "PolicyTagDAOTest_PolicyId";

  @Before
  public void before() {
    createDefaultApplication();
    tag = createTag("TestTag name", "TestTag description", organization.getId());
  }

  @Test
  public void testCRUD() throws Exception {
    policyId = "PolicyTagDAOTest_PolicyId";

    // Create
    PolicyTag policyTag = new PolicyTag(policyId, tag.getId());
    dao.insert(policyTag);
    assertThat(policyTag.getId(), notNullValue());

    // Get
    policyTag = dao.getById(policyTag.getId());
    assertThat(policyTag, notNullValue());
    assertPolicyTag(policyId, tag.getId(), policyTag);

    // Delete
    dao.delete(policyTag);

    // Get
    policyTag = dao.getById(policyTag.getId());
    assertThat(policyTag, nullValue());
  }

  @Test
  public void testUpdateNotSupported() throws Exception {
    PolicyTag policyTag = new PolicyTag(policyId, tag.getId());
    dao.insert(policyTag);

    PolicyTag updatedPolicyTag = new PolicyTag("updated_policy_id", tag.getId());
    updatedPolicyTag.setId(policyTag.getId());

    try {
      dao.update(updatedPolicyTag);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The PolicyTag table does not support update operations"));
    }
  }

  @Test
  public void testGetByPolicyId() throws Exception {
    String policy1Id = policyId + "1";
    String policy2Id = policyId + "2";
    List<Tag> policy1Tags = new ArrayList<>();
    List<Tag> policy2Tags = new ArrayList<>();

    policy1Tags.add(createTag("tag1", "tag1", organization.getId()));
    policy1Tags.add(createTag("tag2", "tag2", organization.getId()));
    policy2Tags.add(createTag("tag3", "tag3", organization.getId()));
    policy2Tags.add(createTag("tag4", "tag4", organization.getId()));

    for (Tag tag : policy1Tags) {
      createPolicyTag(policy1Id, tag.getId());
    }

    for (Tag tag : policy2Tags) {
      createPolicyTag(policy2Id, tag.getId());
    }

    assertPolicyTags(policy1Id, policy1Tags, dao.getByPolicyId(policy1Id));
    assertPolicyTags(policy2Id, policy2Tags, dao.getByPolicyId(policy2Id));
  }

  @Test
  public void testGetByOrgId() throws Exception {
    Organization org1 = createOrganization("org1");
    Organization org2 = createOrganization("org2");

    List<PolicyTag> org1PolicyTags = new ArrayList<>();
    List<PolicyTag> org2PolicyTags = new ArrayList<>();

    //Create tags and apply to policies
    org1PolicyTags.add(createPolicyTag("policyId1", createTag(org1.getId()).getId()));
    org1PolicyTags.add(createPolicyTag("policyId2", createTag(org1.getId()).getId()));
    org1PolicyTags.add(createPolicyTag("policyId1", createTag(org1.getId()).getId()));
    org1PolicyTags.add(createPolicyTag("policyId2", org1PolicyTags.get(2).getTagId()));

    org2PolicyTags.add(createPolicyTag("policyId3", createTag(org2.getId()).getId()));
    org2PolicyTags.add(createPolicyTag("policyId4", createTag(org2.getId()).getId()));
    org2PolicyTags.add(createPolicyTag("policyId3", createTag(org2.getId()).getId()));
    org2PolicyTags.add(createPolicyTag("policyId4", org2PolicyTags.get(2).getTagId()));

    //Create tags but do not apply to policies
    createTag(org1.getId());
    createTag(org2.getId());

    assertPolicyTags(org1PolicyTags, dao.getByOrganizationId(org1.getId()));
    assertPolicyTags(org2PolicyTags, dao.getByOrganizationId(org2.getId()));
  }

  private void assertPolicyTag(String policyId, String tagId, PolicyTag actual) {
    assertThat(actual.getPolicyId(), is(policyId));
    assertThat(actual.getTagId(), is(tagId));
  }

  private void assertPolicyTags(String policyId, List<Tag> expected, List<PolicyTag> actual) {
    assertThat(actual, hasSize(expected.size()));

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (PolicyTag policyTag : actual) {
      assertThat(policyTag.getPolicyId(), equalTo(policyId));
      assertThat(tagIds.contains(policyTag.getTagId()), is(true));
    }
  }

  private void assertPolicyTags(List<PolicyTag> expected, List<PolicyTag> actual) {
    assertThat(actual, hasSize(expected.size()));

    Map<String, PolicyTag> expectedPolicyTags = new HashMap<>();
    for (PolicyTag policyTag : expected) {
      expectedPolicyTags.put(policyTag.getId(), policyTag);
    }

    for (PolicyTag policyTag : actual) {
      PolicyTag expectedPolicyTag = expectedPolicyTags.get(policyTag.getId());
      assertThat(expectedPolicyTag, notNullValue());
      assertThat(policyTag.getTagId(), is(expectedPolicyTag.getTagId()));
      assertThat(policyTag.getPolicyId(), is(expectedPolicyTag.getPolicyId()));
    }
  }
}
