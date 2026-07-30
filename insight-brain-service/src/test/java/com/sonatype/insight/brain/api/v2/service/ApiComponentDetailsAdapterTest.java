/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList.ApiRepositoryComponentEvaluationResult;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import jakarta.inject.Inject;
import org.junit.Test;

public class ApiComponentDetailsAdapterTest
    extends AbstractComponentTest
{
  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private ApiComponentDetailsAdapter adapter;

  @Test
  public void convertToDTO_multipleComponents_returnsOrderedResultsWithMixedState() {
    Repository repository = tempEntity.newRepository();

    ProxyRepositoryComponent componentA =
        tempEntity.newRepositoryComponent(repository.getId(), "/a", null, null);
    ProxyRepositoryPolicyViolation violationA =
        tempEntity.newRepositoryPolicyViolation(componentA, 7, false, "Policy A", null);

    Date quarantineTimeB = new Date();
    ProxyRepositoryComponent componentB =
        tempEntity.newRepositoryComponent(repository.getId(), "/b", quarantineTimeB, null);

    ProxyRepositoryComponent componentC =
        tempEntity.newRepositoryComponent(repository.getId(), "/c", null, null);

    ApiRepositoryComponentEvaluationRequestList requestList =
        new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(componentA.getPathname(), componentA.getHash()));
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(componentB.getPathname(), componentB.getHash()));
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(componentC.getPathname(), componentC.getHash()));

    RepositoryComponentEvaluationDataList evalDataList = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData rcedA = new RepositoryComponentEvaluationData();
    rcedA.requestIndex = 0;
    rcedA.quarantine = false;
    rcedA.catalogDate = new Date();
    RepositoryComponentEvaluationData rcedB = new RepositoryComponentEvaluationData();
    rcedB.requestIndex = 1;
    rcedB.quarantine = true;
    rcedB.catalogDate = new Date();
    RepositoryComponentEvaluationData rcedC = new RepositoryComponentEvaluationData();
    rcedC.requestIndex = 2;
    rcedC.quarantine = false;
    rcedC.catalogDate = new Date();
    evalDataList.componentEvalResults.add(rcedA);
    evalDataList.componentEvalResults.add(rcedB);
    evalDataList.componentEvalResults.add(rcedC);

    RepositoryManager repositoryManager =
        repositoryManagerDAO.getById(repository.getRepositoryManagerId());

    ApiRepositoryComponentEvaluationResultList result =
        adapter.convertToDTO(repositoryManager, repository, requestList, evalDataList);

    assertThat(result.results).hasSize(3);

    ApiRepositoryComponentEvaluationResult resultA = result.results.get(0);
    assertThat(resultA.component).isEqualTo(requestList.components.get(0));
    assertThat(resultA.quarantined).isFalse();
    assertThat(resultA.quarantineDate).isNull();
    assertThat(resultA.policyViolations).hasSize(1);
    assertThat(resultA.policyViolations.get(0).policyId).isEqualTo(violationA.getPolicyId());
    assertThat(resultA.policyViolations.get(0).constraintViolations).isNotEmpty();

    ApiRepositoryComponentEvaluationResult resultB = result.results.get(1);
    assertThat(resultB.component).isEqualTo(requestList.components.get(1));
    assertThat(resultB.quarantined).isTrue();
    assertThat(resultB.quarantineDate).isEqualTo(quarantineTimeB);
    assertThat(resultB.policyViolations).isEmpty();

    ApiRepositoryComponentEvaluationResult resultC = result.results.get(2);
    assertThat(resultC.component).isEqualTo(requestList.components.get(2));
    assertThat(resultC.quarantined).isFalse();
    assertThat(resultC.quarantineDate).isNull();
    assertThat(resultC.policyViolations).isEmpty();
  }

  @Test
  public void convertToDTO_nullPathnameEntry_returnsEmptyResultRow() {
    Repository repository = tempEntity.newRepository();

    ProxyRepositoryComponent componentX =
        tempEntity.newRepositoryComponent(repository.getId(), "/x", null, null);
    ProxyRepositoryComponent componentZ =
        tempEntity.newRepositoryComponent(repository.getId(), "/z", null, null);

    ApiRepositoryComponentEvaluationRequestList requestList =
        new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(componentX.getPathname(), componentX.getHash()));
    ApiRepositoryComponentEvaluationRequest nullPathnameRequest =
        new ApiRepositoryComponentEvaluationRequest();
    nullPathnameRequest.pathname = null;
    nullPathnameRequest.packageUrl = null;
    nullPathnameRequest.hash = "dummy-hash";
    requestList.components.add(nullPathnameRequest);
    requestList.components.add(
        new ApiRepositoryComponentEvaluationRequest(componentZ.getPathname(), componentZ.getHash()));

    RepositoryComponentEvaluationDataList evalDataList = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData rcedX = new RepositoryComponentEvaluationData();
    rcedX.requestIndex = 0;
    rcedX.quarantine = false;
    rcedX.catalogDate = new Date();
    RepositoryComponentEvaluationData rcedNull = new RepositoryComponentEvaluationData();
    rcedNull.requestIndex = 1;
    rcedNull.quarantine = false;
    rcedNull.catalogDate = new Date();
    RepositoryComponentEvaluationData rcedZ = new RepositoryComponentEvaluationData();
    rcedZ.requestIndex = 2;
    rcedZ.quarantine = false;
    rcedZ.catalogDate = new Date();
    evalDataList.componentEvalResults.add(rcedX);
    evalDataList.componentEvalResults.add(rcedNull);
    evalDataList.componentEvalResults.add(rcedZ);

    RepositoryManager repositoryManager =
        repositoryManagerDAO.getById(repository.getRepositoryManagerId());

    ApiRepositoryComponentEvaluationResultList result =
        adapter.convertToDTO(repositoryManager, repository, requestList, evalDataList);

    assertThat(result.results).hasSize(3);

    ApiRepositoryComponentEvaluationResult resultX = result.results.get(0);
    assertThat(resultX.component).isEqualTo(requestList.components.get(0));
    assertThat(resultX.quarantineDate).isNull();
    assertThat(resultX.policyViolations).isEmpty();

    ApiRepositoryComponentEvaluationResult resultNull = result.results.get(1);
    assertThat(resultNull.component).isEqualTo(requestList.components.get(1));
    assertThat(resultNull.quarantineDate).isNull();
    assertThat(resultNull.policyViolations).isEmpty();

    ApiRepositoryComponentEvaluationResult resultZ = result.results.get(2);
    assertThat(resultZ.component).isEqualTo(requestList.components.get(2));
    assertThat(resultZ.quarantineDate).isNull();
    assertThat(resultZ.policyViolations).isEmpty();
  }
}
