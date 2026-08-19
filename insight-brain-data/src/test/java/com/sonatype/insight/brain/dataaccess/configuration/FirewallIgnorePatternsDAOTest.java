/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class FirewallIgnorePatternsDAOTest
    extends AbstractDbDAOTest
{
  private FirewallIgnorePatternsDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createFirewallIgnorePatternsDAO();
  }

  @Test
  public void testReadUpdate() {
    assertFirewallIgnorePatterns(dao.get(), new FirewallIgnorePatterns());

    FirewallIgnorePatterns expectedFirewallIgnorePatterns = createFirewallIgnorePatterns();

    dao.update(expectedFirewallIgnorePatterns);

    assertFirewallIgnorePatterns(dao.get(), expectedFirewallIgnorePatterns);
  }

  @Test
  public void testInsert_Unsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> dao.insert(createFirewallIgnorePatterns()));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    FirewallIgnorePatterns expectedFirewallIgnorePatterns = dao.get();
    assertFirewallIgnorePatterns(expectedFirewallIgnorePatterns, new FirewallIgnorePatterns());

    String notSingletonEntityId = "not-" + FirewallIgnorePatternsDAO.SINGLETON_ENTITY_ID;
    expectedFirewallIgnorePatterns
        .setFirewallIgnorePatterns(createFirewallIgnorePatterns().getFirewallIgnorePatterns());

    dao.update(expectedFirewallIgnorePatterns);

    assertThat(dao.getById(notSingletonEntityId)).isNull();
    assertFirewallIgnorePatterns(dao.get(), expectedFirewallIgnorePatterns);
  }

  @Test
  public void testDelete_Unsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> dao.delete(dao.get()));
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
