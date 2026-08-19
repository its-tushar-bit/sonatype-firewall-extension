/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.indexquery.IndexQueryResponse.IndexQueryFacetBucket;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Real read-gate coverage for the {@code organizations} facet of {@code POST /rest/search/index-query}:
 * real Lucene index, real H2 DB and real Shiro authorization (default-deny, one explicitly granted
 * READ, and the live {@code @AuthzFilter} aspect on
 * {@code OrganizationSummaryService#getOrganizationsForRead}).
 * <p>
 * The facet aggregates {@code parentOrganizationId}, which every document carries as its FULL ancestor
 * closure (self..root). That is deliberate — it is what makes an ancestor bucket count its whole subtree
 * — but it means a caller who can read only a leaf organization's documents still produces buckets for
 * every organization ABOVE its read scope. The read gate in {@code IndexQueryService} is the only thing
 * that keeps those out of the response, so it is asserted here against real authorization rather than a
 * stubbed readable-set.
 * <p>
 * This is a separate class from {@link IndexQueryServiceFacetsRealIndexTest} because the read gate is
 * only observable under default-deny: that class inherits the harness default of granting the test user
 * every permission globally, which makes every ancestor readable and the gate a no-op. Default-deny is a
 * class-level property of the {@code AbstractComponentH2AuthzTest} base ({@code enforceSecurityAspects}
 * plus the declined {@code grantDefaultTestUserAllPermissions}), so it cannot be turned on for a single
 * test method in the other class.
 */
@ComponentH2Test
public class IndexQueryServiceOrganizationFacetAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private IndexQueryService indexQueryService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Organization orgGrandparent;

  private Organization orgParent;

  private Organization orgChild;

  private Application appChild;

  private User globalReader;

  @BeforeEach
  public void setUpIndex() {
    // IqLocalSearchService.search rejects every call unless the Global Search preview flag is on (the
    // same flag IndexQueryResource gates the HTTP endpoint behind); calling the service directly
    // bypasses the resource's gate check, so it must be set here instead.
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);

    // Mirror the other real-index tests in this module: swap in a mocked shutdown handler and reset the
    // lazily-created indexing executors so repeated full re-indexes within this reused-context module
    // behave deterministically across test classes.
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");
  }

  @AfterEach
  public void clearPreviewFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  private static void resetTenantExecutor(final Object bean, final String fieldName) {
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

  /**
   * Real estate: grandparent org -> parent org -> child org, with the only application (and its two
   * waivers) at the bottom, under the child org. Every waiver document therefore carries all three
   * organizations plus ROOT in its {@code parentOrganizationId} closure, which is what gives the read
   * gate something to hide.
   * <p>
   * {@code globalReader} is the control subject: it reads the identical documents through the identical
   * aggregation, so the difference between the two facets is attributable to the read gate alone.
   */
  @Override
  protected void setUpSecurity() {
    super.setUpSecurity();

    orgGrandparent = tempEntity.newOrganization();
    orgParent = tempEntity.newOrganization(orgGrandparent);
    orgChild = tempEntity.newOrganization(orgParent);
    appChild = tempEntity.newApplication(orgChild.getId());

    Policy policyHigh = tempEntity.newPolicy(orgGrandparent.getId(), "Policy High", 9);
    Policy policyLow = tempEntity.newPolicy(orgGrandparent.getId(), "Policy Low", 3);
    tempEntity.newWaiver(policyHigh.getId(), appChild.getId());
    tempEntity.newWaiver(policyLow.getId(), appChild.getId());

    globalReader = tempEntity.newUser();
  }

  @Test
  public void testQueryWaivers_OrganizationsFacet_OffersTheReadableOrgAndHidesItsUnreadableAncestors() {
    luceneSearchIndexClient.populateIndex();

    // A caller whose only grant is READ on the leaf organization.
    grantReadPermission(orgChild.getId());

    IndexQueryResponse scopedResponse = queryWaivers();

    // The caller genuinely sees the documents, so the aggregation genuinely ran over an ancestor closure
    // that contains the unreadable orgs -- without this the facet assertion below would pass vacuously
    // on an empty result.
    assertThat(scopedResponse.totalEstimate()).isEqualTo(2);

    // Its own organization is offered, with the resolved real-cased name and its subtree count...
    assertThat(scopedResponse.facets().get("organizations"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::displayName, IndexQueryFacetBucket::count)
        .containsExactly(tuple(orgChild.getId(), orgChild.getName(), 2L));

    // ...and nothing above it. The parent and grandparent have a bucket in the aggregation (see the
    // control below) but the caller may not read them, so they must not reach the response -- neither
    // their ids nor, through the facet's display names, their names. ROOT is never a bucket for anyone.
    assertThat(scopedResponse.facets().get("organizations")).extracting(IndexQueryFacetBucket::value)
        .doesNotContain(orgParent.getId(), orgGrandparent.getId(), Organization.ROOT_ORGANIZATION_ID);
    assertThat(scopedResponse.facets().get("organizations")).extracting(IndexQueryFacetBucket::displayName)
        .doesNotContain(orgParent.getName(), orgGrandparent.getName());

    // Control: the same documents, the same aggregation, a caller who may read every organization. The
    // ancestors show up here, which is what proves the assertions above are the read gate at work and
    // not simply an aggregation that never produced ancestor buckets.
    Role globalReadRole = tempEntity.newRole(true /* global */, Permission.READ);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, globalReadRole.getId(), globalReader.getUsername());
    loginAs(globalReader);

    IndexQueryResponse globalResponse = queryWaivers();

    assertThat(globalResponse.totalEstimate()).isEqualTo(2);
    assertThat(globalResponse.facets().get("organizations"))
        .extracting(IndexQueryFacetBucket::value, IndexQueryFacetBucket::count)
        .containsExactlyInAnyOrder(
            tuple(orgGrandparent.getId(), 2L),
            tuple(orgParent.getId(), 2L),
            tuple(orgChild.getId(), 2L));
  }

  private IndexQueryResponse queryWaivers() {
    IndexQueryRequest request = new IndexQueryRequest("WAIVER", Map.of(), 1, 100, null, null, true);
    return indexQueryService.query(IndexQueryType.WAIVER, request);
  }

  private void loginAs(final User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    subject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
  }
}
