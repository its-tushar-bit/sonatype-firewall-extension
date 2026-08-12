/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.client.utils.HttpClientUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class LuceneIndexSearchingTest
    extends AbstractIndexSearchingTest
{
  @Before
  @Override
  public void before() {
    InsightProxy testProxy = mock(InsightProxy.class);
    doAnswer(invocation -> {
      HttpClientUtils.Configuration config = invocation.getArgument(0);
      config.setServerUrl(hdsMockServer.getHttpUrl());
      config.setUserAgent("LuceneIndexSearchingTest");
      return config;
    }).when(testProxy).contextualize(any(HttpClientUtils.Configuration.class));
    applyBeanFieldOverride(HdsClient.class, "proxy", testProxy);

    ShutdownHandler mockShutdownHandler =
        (ShutdownHandler) ReflectionTestUtils.getField(this, "mockShutdownHandler");
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");

    SecurityManager securityManager = (SecurityManager) ReflectionTestUtils.getField(this, "securityManager");
    SecurityUtils.setSecurityManager(securityManager);

    UserPrincipal userPrincipal = (UserPrincipal) subject.getPrincipal();
    Role role = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), userPrincipal.getUsername());
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  private void resetTenantExecutor(Object bean, String fieldName) {
    @SuppressWarnings("unchecked")
    TenantReference<ExecutorService> executors =
        (TenantReference<ExecutorService>) ReflectionTestUtils.getField(bean, fieldName);
    if (executors == null) {
      return;
    }
    ExecutorService oldExecutor = executors.remove();
    if (oldExecutor != null) {
      oldExecutor.shutdownNow();
    }
  }

  @Override
  @Test
  public void testBoosting() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    lookup(IndexService.class).createSearchIndex();

    SearchService searchService = lookup(SearchService.class);
    List<String> applicationIds = searchByApplicationId(searchService,
        FieldIdentifier.APPLICATION_ID + ":" + app1.getId() + "^2 OR " + FieldIdentifier.APPLICATION_ID + ":"
            + app2.getId());
    assertThat(applicationIds).containsExactly(app1.getId(), app2.getId());

    applicationIds = searchByApplicationId(searchService,
        FieldIdentifier.APPLICATION_ID + ":" + app1.getId() + " OR " + FieldIdentifier.APPLICATION_ID + ":"
            + app2.getId() + "^2");
    assertThat(applicationIds).containsExactly(app2.getId(), app1.getId());
  }

  @Override
  @Test
  public void testSearchByFieldsFromDifferentDocumentTypes() throws Exception {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    var tag = tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID);
    lookup(IndexService.class).createSearchIndex();

    SearchService searchService = lookup(SearchService.class);
    SearchResultDTO searchResultDTO = searchService.searchIndex(
        FieldIdentifier.POLICY_ID + ":" + policy.getId() + " OR " + FieldIdentifier.APPLICATION_CATEGORY_ID + ":"
            + tag.getId(),
        Integer.MAX_VALUE,
        1,
        true,
        null,
        null);

    List<String> itemTypes = searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.itemType)
        .collect(Collectors.toList());
    assertThat(itemTypes).containsExactlyInAnyOrder("POLICY", "APPLICATION_CATEGORY");
  }

  private List<String> searchByApplicationId(final SearchService searchService, final String query) {
    SearchResultDTO searchResultDTO = searchService.searchIndex(query, Integer.MAX_VALUE, 1, true, null, null);
    return searchResultDTO.groupingByDTOS.stream()
        .flatMap(groupingByDTO -> groupingByDTO.searchResultItemDTOS.stream())
        .map(searchResultItemDTO -> searchResultItemDTO.applicationId)
        .collect(Collectors.toList());
  }
}
