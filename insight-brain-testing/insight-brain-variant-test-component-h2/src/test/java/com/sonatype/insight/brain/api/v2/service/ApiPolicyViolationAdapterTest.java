/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApiPolicyViolationAdapterTest
    extends AbstractComponentH2Test
{
  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Test
  public void testConvert() {
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId());
    proxyRepositoryPolicyViolation.setWaiveTime(DateUtils.addDays(proxyRepositoryPolicyViolation.getOpenTime(), 1));
    proxyRepositoryPolicyViolationDAO.update(proxyRepositoryPolicyViolation);

    var result = ApiPolicyViolationAdapter.convert(proxyRepositoryPolicyViolation);

    assertThat(result).isNotNull();
    assertThat(result.policyId).isEqualTo(proxyRepositoryPolicyViolation.getPolicyId());
    assertThat(result.policyName).isEqualTo(proxyRepositoryPolicyViolation.getPolicyName());
    assertThat(result.threatLevel).isEqualTo(proxyRepositoryPolicyViolation.getThreatLevel());
    assertThat(result.policyViolationId).isEqualTo(proxyRepositoryPolicyViolation.getId());
    assertThat(result.openTime).isNotNull().isEqualTo(proxyRepositoryPolicyViolation.getOpenTime());
    assertThat(result.waiveTime).isNotNull().isEqualTo(proxyRepositoryPolicyViolation.getWaiveTime());
    assertThat(result.constraintViolations).usingRecursiveComparison()
        .isEqualTo(PolicyViolationAdapter.convert(proxyRepositoryPolicyViolation));
  }
}
