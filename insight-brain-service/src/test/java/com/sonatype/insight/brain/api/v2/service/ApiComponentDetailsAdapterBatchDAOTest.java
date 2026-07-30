/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

// Companion to ApiComponentDetailsAdapterTest: locks in the batch-DAO contract that the real-DAO
// integration tests cannot express. A regression to per-component singular DAO reads (N+1) would
// still return correct rows from the DB, so only these verify(...) assertions catch it.
@RunWith(MockitoJUnitRunner.class)
public class ApiComponentDetailsAdapterBatchDAOTest
{
  @Mock
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Mock
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @InjectMocks
  private ApiComponentDetailsAdapter adapter;

  @Test
  public void convertToDTO_multipleComponents_usesBatchDAOMethods() {
    when(repositoryManager.getId()).thenReturn("rm-1");
    when(repository.getId()).thenReturn("repo-1");
    when(repository.getPublicId()).thenReturn("repo-public-1");
    when(repository.getRepositoryType()).thenReturn(RepositoryType.proxy);

    ApiRepositoryComponentEvaluationRequestList requestList =
        new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/a", "hash-a"));
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/b", "hash-b"));
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/c", "hash-c"));

    RepositoryComponentEvaluationDataList evalDataList = new RepositoryComponentEvaluationDataList();
    for (int i = 0; i < 3; i++) {
      RepositoryComponentEvaluationData d = new RepositoryComponentEvaluationData();
      d.requestIndex = i;
      d.quarantine = false;
      evalDataList.componentEvalResults.add(d);
    }

    when(proxyRepositoryComponentDAO.getByRepositoryIdAndPathnames(eq("repo-1"), any()))
        .thenReturn(Collections.<ProxyRepositoryComponent>emptyList());
    when(proxyRepositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnames(eq("repo-1"), any()))
        .thenReturn(Collections.<ProxyRepositoryPolicyViolation>emptyList());

    adapter.convertToDTO(repositoryManager, repository, requestList, evalDataList);

    verify(proxyRepositoryComponentDAO, times(1)).getByRepositoryIdAndPathnames(eq("repo-1"), any());
    verify(proxyRepositoryComponentDAO, never()).getByRepositoryIdAndPathname(anyString(), anyString());

    verify(proxyRepositoryPolicyViolationDAO, times(1))
        .getActiveByRepositoryIdAndPathnames(eq("repo-1"), any());
    verify(proxyRepositoryPolicyViolationDAO, never())
        .getActiveByRepositoryIdAndPathname(anyString(), anyString());

    // With no violations, the adapter must skip loadConstraintFacts entirely.
    verify(proxyRepositoryPolicyViolationDAO, never()).loadConstraintFacts(any());
  }

  @Test
  public void convertToDTO_multipleComponentsWithViolations_loadsConstraintFactsOnce() {
    when(repositoryManager.getId()).thenReturn("rm-1");
    when(repository.getId()).thenReturn("repo-1");
    when(repository.getPublicId()).thenReturn("repo-public-1");
    when(repository.getRepositoryType()).thenReturn(RepositoryType.proxy);

    ApiRepositoryComponentEvaluationRequestList requestList =
        new ApiRepositoryComponentEvaluationRequestList();
    requestList.format = ComponentIdentifier.FORMAT_MAVEN;
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/a", "hash-a"));
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/b", "hash-b"));
    requestList.components.add(new ApiRepositoryComponentEvaluationRequest("/c", "hash-c"));

    RepositoryComponentEvaluationDataList evalDataList = new RepositoryComponentEvaluationDataList();
    for (int i = 0; i < 3; i++) {
      RepositoryComponentEvaluationData d = new RepositoryComponentEvaluationData();
      d.requestIndex = i;
      d.quarantine = false;
      evalDataList.componentEvalResults.add(d);
    }

    List<ProxyRepositoryPolicyViolation> violations = Arrays.asList(
        newViolation("/a"),
        newViolation("/b"),
        newViolation("/c"));

    when(proxyRepositoryComponentDAO.getByRepositoryIdAndPathnames(eq("repo-1"), any()))
        .thenReturn(Collections.<ProxyRepositoryComponent>emptyList());
    when(proxyRepositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnames(eq("repo-1"), any()))
        .thenReturn(violations);

    adapter.convertToDTO(repositoryManager, repository, requestList, evalDataList);

    // Locks in the "called exactly once regardless of component count" contract: a regression
    // that moved loadConstraintFacts back into the per-component loop would fail here.
    verify(proxyRepositoryPolicyViolationDAO, times(1)).loadConstraintFacts(any());
  }

  private static ProxyRepositoryPolicyViolation newViolation(String pathname) {
    // Mock rather than instantiate: ProxyRepositoryPolicyViolation.getConstraintFacts() throws unless
    // loadConstraintFacts() actually populates them, which the mocked DAO does not do here.
    ProxyRepositoryPolicyViolation violation = mock(ProxyRepositoryPolicyViolation.class);
    when(violation.getPathname()).thenReturn(pathname);
    when(violation.getConstraintFacts()).thenReturn(Collections.emptyList());
    return violation;
  }
}
