/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.List;
import java.util.concurrent.Callable;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MultiTenantIndexSearchingTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  protected Subject mockSubject;

  @Mock
  private SecurityManager mockSecurityManager;

  @Before
  public void before() {
    lenient().when(mockSubject.getPrincipal()).thenReturn(new UserPrincipal("testuser", "Test User", InternalRealm.ID));
    // Support for TenantAwareOneTimeRunnable which uses subject.associateWith() to propagate security context
    // to worker threads (required since Shiro 2.0.4+ removed InheritableThreadLocal from ThreadContext)
    lenient().when(mockSubject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(mockSubject.associateWith(any(Callable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(mockSecurityManager.createSubject(any(SubjectContext.class))).thenReturn(mockSubject);
    ThreadContext.bind(mockSecurityManager);
    ThreadContext.bind(mockSubject);
  }

  @After
  public void after() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  private void setupTestUser() {
    tenantTemporaryEntity.newUser("testuser");
    tenantTemporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.SYSTEM_ADMIN_ROLE_ID,
        "testuser");
    tenantTemporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID,
        "testuser");
  }

  @Test
  public void testIndex_TenantAware() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      provisionTenant(tenant.tenantSlug);
      setupTestUser();
      tenantTemporaryEntity.newOrganization("orgA");
      getCLMServer().getInstance(IndexService.class).createSearchIndex();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      provisionTenant(tenant.tenantSlug);
      setupTestUser();
      tenantTemporaryEntity.newOrganization("orgB");
      getCLMServer().getInstance(IndexService.class).createSearchIndex();
    });

    testAsTenant(tenant1, tenant -> {
      List<SearchResultItemDTO> results = search(FieldIdentifier.ORGANIZATION_ID, "*");
      assertThat(results).extracting(org -> org.organizationName)
          .containsExactlyInAnyOrder("Root Organization", "orgA");
    });

    testAsTenant(tenant2, tenant -> {
      List<SearchResultItemDTO> results = search(FieldIdentifier.ORGANIZATION_ID, "*");
      assertThat(results).extracting(org -> org.organizationName)
          .containsExactlyInAnyOrder("Root Organization", "orgB");
    });
  }

  private List<SearchResultItemDTO> search(FieldIdentifier fieldIdentifier, String fieldValue) {
    return search(fieldIdentifier + ":" + fieldValue, false);
  }

  private List<SearchResultItemDTO> search(String query, boolean allComponents) {
    return getCLMServer().getInstance(SearchService.class)
        .searchIndex(query, Integer.MAX_VALUE, 1, allComponents, null, null).groupingByDTOS
        .stream()
        .map(groupDTO -> groupDTO.searchResultItemDTOS).flatMap(List::stream)
        .toList();
  }
}
