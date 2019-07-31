/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.stream.Collectors;

import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.70
 */
@Named
public class ApiPolicyWaiverService
{
  public void addPolicyWaiver(final String policyViolationId,
                              final OwnerType ownerType,
                              final String comment)
  {
    PolicyViolation policyViolation = new PolicyViolationDAO().getById(policyViolationId);

    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + policyViolationId + ".");
    }

    final String ownerId;
    switch (ownerType) {
      case APPLICATION:
        ownerId = policyViolation.getApplicationId();
        AuditData.get().setData("applicationId", ownerId).setApplication(new ApplicationDAO().getById(ownerId));
        break;
      case ORGANIZATION:
        ownerId = new ApplicationDAO().getByIdNotNull(policyViolation.getApplicationId()).getOrganizationId();
        AuditData.get().setData("organizationId", ownerId).setOrganization(new OrganizationDAO().getById(ownerId));
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }

    addPolicyWaiver(ownerType, ownerId, policyViolation, comment);
  }

  @Authorize(permission = Permission.WRITE)
  void addPolicyWaiver(
      /* used to perform authz check even though owner type is unused */
      @AuthzContext(Key.TYPE) @SuppressWarnings("unused") final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final PolicyViolation policyViolation,
      final String comment)
  {
    PolicyWaiver policyWaiver =
        new PolicyWaiver(policyViolation.getHash(), policyViolation.getPolicyId(), ownerId, comment);
    policyWaiver.setConstraintFactsJson(policyViolation.getConstraintFactsJson());

    new PolicyWaiverDAO().insert(policyWaiver);
    auditPolicyWaiver(policyWaiver);
  }

  private void auditPolicyWaiver(PolicyWaiver policyWaiver) {
    AuditData.get().setData("policyWaiverId", policyWaiver.getId())
        .setPolicy(new PolicyDAO().getByIdNotNull(policyWaiver.getPolicyId()))
        .setComment(policyWaiver.getComment())
        .setComponentHash(policyWaiver.getHash());
    AuditData.get().setData("policyConstraints",
        policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
  }
}
