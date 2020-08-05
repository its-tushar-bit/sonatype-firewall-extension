/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Arrays;
import java.util.Collections;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class FirewallIgnorePatternsDAOTest
    extends AbstractDbDAOTest
{
  private final FirewallIgnorePatternsDAO dao = new FirewallIgnorePatternsDAO();

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    FirewallIgnorePatterns expectedFirewallIgnorePatterns = createFirewallIgnorePatterns();

    dao.insert(expectedFirewallIgnorePatterns);

    assertFirewallIgnorePatterns(dao.get(), expectedFirewallIgnorePatterns);

    com.sonatype.clm.dto.model.component.FirewallIgnorePatterns firewallIgnorePatterns =
        expectedFirewallIgnorePatterns.getFirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("conda", Collections.singletonList(".*\\.json"));
    expectedFirewallIgnorePatterns.setFirewallIgnorePatterns(firewallIgnorePatterns);

    dao.update(expectedFirewallIgnorePatterns);

    assertFirewallIgnorePatterns(dao.get(), expectedFirewallIgnorePatterns);

    dao.delete();

    assertThat(dao.get()).isNull();
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(createFirewallIgnorePatterns());

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
      dao.insert(createFirewallIgnorePatterns());
    }).withCauseInstanceOf(EntityExistsException.class);

    FirewallIgnorePatterns firewallIgnorePatterns = createFirewallIgnorePatterns();
    firewallIgnorePatterns.setId("not-" + FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID);

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
      dao.insert(firewallIgnorePatterns);
    }).withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    FirewallIgnorePatterns expectedFirewallIgnorePatterns = createFirewallIgnorePatterns();
    dao.insert(expectedFirewallIgnorePatterns);

    String notSingletonEntityId = "not-" + FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID;
    expectedFirewallIgnorePatterns.setId(notSingletonEntityId);
    com.sonatype.clm.dto.model.component.FirewallIgnorePatterns firewallIgnorePatterns =
        expectedFirewallIgnorePatterns.getFirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("conda", Collections.singletonList(".*\\.json"));
    expectedFirewallIgnorePatterns.setFirewallIgnorePatterns(firewallIgnorePatterns);

    dao.update(expectedFirewallIgnorePatterns);

    assertThat(dao.getById(notSingletonEntityId)).isNull();
    FirewallIgnorePatterns actual = dao.get();
    assertFirewallIgnorePatterns(actual, expectedFirewallIgnorePatterns);
  }

  private FirewallIgnorePatterns createFirewallIgnorePatterns() {
    com.sonatype.clm.dto.model.component.FirewallIgnorePatterns ignorePatterns =
        new com.sonatype.clm.dto.model.component.FirewallIgnorePatterns();
    ignorePatterns.regexpsByRepositoryFormat.put("format1", Arrays.asList("a", "b"));
    ignorePatterns.regexpsByRepositoryFormat.put("format2", Collections.singletonList("c"));
    return new FirewallIgnorePatterns(ignorePatterns);
  }

  private void assertFirewallIgnorePatterns(FirewallIgnorePatterns actual, FirewallIgnorePatterns expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.getFirewallIgnorePatterns()).usingRecursiveComparison()
        .isEqualTo(expected.getFirewallIgnorePatterns());
  }
}
