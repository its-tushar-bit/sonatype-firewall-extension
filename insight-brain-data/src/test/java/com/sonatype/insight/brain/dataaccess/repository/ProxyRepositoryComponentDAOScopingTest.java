/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ProxyRepositoryComponentDAOScopingTest
    extends AbstractDbDAOTest
{
  private ProxyRepositoryComponentDAO dao;

  private Repository repoA;

  private Repository repoB;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryComponentDAO();
    RepositoryManager managerA = tempEntity.newRepositoryManager();
    repoA = tempEntity.newRepository(managerA);
    RepositoryManager managerB = tempEntity.newRepositoryManager();
    repoB = tempEntity.newRepository(managerB);
  }

  @Test
  public void getFirewallRepositoryComponents_scopedToPermittedRepos() {
    Date quarantineTime = new Date();
    tempEntity.newRepositoryComponent(repoA.getId(), "/compA", quarantineTime, null);
    tempEntity.newRepositoryComponent(repoB.getId(), "/compB", quarantineTime, null);

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(
        1, 100, FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());
    filter.permittedRepositoryIds = Set.of(repoA.getId());

    List<ProxyRepositoryComponent> result = dao.getFirewallRepositoryComponents(filter);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRepositoryId()).isEqualTo(repoA.getId());
  }

  @Test
  public void getTotalFirewallRepositoryComponents_scopedToPermittedRepos() {
    Date quarantineTime = new Date();
    tempEntity.newRepositoryComponent(repoA.getId(), "/compA", quarantineTime, null);
    tempEntity.newRepositoryComponent(repoB.getId(), "/compB", quarantineTime, null);

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(
        1, 100, FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());
    filter.permittedRepositoryIds = Set.of(repoA.getId());

    long total = dao.getTotalFirewallRepositoryComponents(filter);

    assertThat(total).isEqualTo(1);
  }

  @Test
  public void getQuarantinedComponentsDetails_scopedToPermittedRepos() {
    Date quarantineTime = new Date();
    tempEntity.newRepositoryComponent(repoA.getId(), "/compA", quarantineTime, null);
    tempEntity.newRepositoryComponent(repoB.getId(), "/compB", quarantineTime, null);
    // action_type_id must be 'fail' and active=true, waived=false for getQuarantinedComponentsDetails
    tempEntity.newRepositoryPolicyViolation(repoA.getId(), 5, "/compA", false, "fail", "policyA", "Policy A", null);
    tempEntity.newRepositoryPolicyViolation(repoB.getId(), 5, "/compB", false, "fail", "policyB", "Policy B", null);

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(
        1, 100, FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());
    filter.permittedRepositoryIds = Set.of(repoA.getId());

    List<FirewallQuarantinedComponentDetails> results = dao.getQuarantinedComponentsDetails(filter);

    assertThat(results).isNotEmpty();
    assertThat(results).allSatisfy(d -> assertThat(d.repositoryId).isEqualTo(repoA.getId()));
  }

  @Test
  public void nullPermittedRepositoryIds_returnsAllRepos() {
    Date quarantineTime = new Date();
    tempEntity.newRepositoryComponent(repoA.getId(), "/compA", quarantineTime, null);
    tempEntity.newRepositoryComponent(repoB.getId(), "/compB", quarantineTime, null);

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(
        1, 100, FirewallRepositoryComponentFilter.FirewallComponentFilterState.QUARANTINE,
        FirewallSortableField.QUARANTINE_TIME, false, Collections.emptyList());
    // permittedRepositoryIds left null = full access

    long total = dao.getTotalFirewallRepositoryComponents(filter);

    assertThat(total).isGreaterThanOrEqualTo(2);
  }
}
