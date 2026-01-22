/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyViolationAdapterTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Test
  public void testConvert() {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    repositoryPolicyViolation.setWaiveTime(DateUtils.addDays(repositoryPolicyViolation.getOpenTime(), 1));
    repositoryPolicyViolationDAO.update(repositoryPolicyViolation);

    var result = ApiPolicyViolationAdapter.convert(repositoryPolicyViolation);

    assertThat(result).isNotNull();
    assertThat(result.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(result.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(result.threatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(result.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(result.openTime).isNotNull().isEqualTo(repositoryPolicyViolation.getOpenTime());
    assertThat(result.waiveTime).isNotNull().isEqualTo(repositoryPolicyViolation.getWaiveTime());
    assertThat(result.constraintViolations).usingRecursiveComparison()
        .isEqualTo(PolicyViolationAdapter.convert(repositoryPolicyViolation));
  }
}
