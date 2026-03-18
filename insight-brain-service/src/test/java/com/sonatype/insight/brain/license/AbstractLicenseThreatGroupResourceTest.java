/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.LicenseThreatGroupsByOwner;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractLicenseThreatGroupResourceTest
    extends AbstractResourceTest
{
  protected OrganizationDAO orgDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Before
  public void setUp() {
    orgDAO = lookup(OrganizationDAO.class);
    licenseThreatGroupDAO = lookup(LicenseThreatGroupDAO.class);
  }

  protected HttpRequest restRequest(String ownerId) {
    return restRequest().path(LicenseThreatGroupResource.RESOURCE_PATH).parameter(getOwnerType(), ownerId);
  }

  protected void testDelete_OwnerIdMismatch(Owner owner1, Owner owner2) throws Exception {
    LicenseThreatGroup group = tempEntity.newLicenseThreatGroup(owner1.getId());

    HttpResponse response = restRequest(owner2.getPublicId()).path(group.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a license threat group with ID " + group.getId() + " for "
        + getOwnerType() + " ID " + owner2.getPublicId());

    // Verify that the group was not deleted
    assertThat(licenseThreatGroupDAO.getById(group.getId())).isNotNull();
  }

  protected void testDelete_InUseByPolicy(Owner owner) throws Exception {
    testDelete_InUseByPolicy(owner.getPublicId(), owner.getId(), owner.getId(), null);
  }

  protected void testDelete_InUseByPolicy(
      String ownerPublicId,
      String ownerId,
      String policyOwnerId,
      String policyLocation) throws Exception
  {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(ownerId);

    Policy policy = new Policy(null, "policyName");
    policy.setOwnerId(policyOwnerId);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is", ltg.getId()));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    HttpResponse response = restRequest(ownerPublicId).path(ltg.getId()).delete();
    assertResponseStatus(400, response);

    String error =
        "Cannot delete the license threat group because it is used in a condition for the 'policyName' policy";
    if (null != policyLocation) {
      error = error + " " + policyLocation;
    }

    assertThat(response.getBodyText()).isEqualTo(error);
    assertThat(licenseThreatGroupDAO.getById(ltg.getId())).isNotNull();
  }

  protected void assertLicenseThreatGroup(String ownerId, String name, int threatLevel, LicenseThreatGroup actual) {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getName()).isEqualTo(name);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
  }

  protected ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(String ownerId) throws Exception {
    HttpResponse response = restRequest(ownerId).path("applicable").get();
    assertResponseStatus(200, response);
    return response.getBody(ApplicableLicenseThreatGroups.class);
  }

  protected void assertLicenseThreatGroupsByOwner(
      String ownerId,
      String ownerName,
      OwnerType ownerType,
      int licenseThreatGroupCount,
      LicenseThreatGroupsByOwner actual)
  {
    assertThat(actual.ownerId).isEqualTo(ownerId);
    assertThat(actual.ownerName).isEqualTo(ownerName);
    assertThat(actual.ownerType).isEqualTo(ownerType);
    assertThat(actual.licenseThreatGroups).hasSize(licenseThreatGroupCount)
        .allSatisfy(ltgwl -> assertThat(ltgwl.licenses).isNotNull());
  }

  protected abstract String getOwnerType();
}
