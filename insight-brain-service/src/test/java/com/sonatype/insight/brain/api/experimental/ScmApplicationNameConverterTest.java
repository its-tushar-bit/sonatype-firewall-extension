/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import com.google.inject.Inject;
import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmApplicationNameConverterTest
    extends AbstractComponentTest
{
  @Inject
  ScmApplicationNameConverter scmApplicationNameConverter;

  @Test
  public void testConcatenatesNames() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);
    String actualName = scmApplicationNameConverter.buildName(repo);

    assertThat(actualId).isEqualTo("bar__foo");
    assertThat(actualName).isEqualTo("Bar - Foo");
  }

  @Test
  public void testStripsInvalidCharacters() {
    SCMRepository repo = buildRepo("2a-c∫6d 2=*", "1ab∫cd 2-_*");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);
    String actualName = scmApplicationNameConverter.buildName(repo);
    assertThat(actualId).isEqualTo("1abcd2-___2a-c6d2");
    assertThat(actualName).isEqualTo("1abcd 2 _ - 2a C6d 2");
  }

  @Test
  public void testAppendsPostfix() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualId = scmApplicationNameConverter.buildPublicIdWithPostfix(repo, 3);
    String actualName = scmApplicationNameConverter.buildNameWithPostfix(repo, 3);
    assertThat(actualId).isEqualTo("bar__foo_3");
    assertThat(actualName).isEqualTo("Bar - Foo - 3");
  }

  @Test
  public void testAppendsPostfix_stripsInvalidCharacters() {
    SCMRepository repo = buildRepo("foo", "1ab∫cd 2-_*");
    String actualId = scmApplicationNameConverter.buildPublicIdWithPostfix(repo, 3);
    String actualName = scmApplicationNameConverter.buildNameWithPostfix(repo, 3);
    assertThat(actualId).isEqualTo("1abcd2-___foo_3");
    assertThat(actualName).isEqualTo("1abcd 2 _ - Foo - 3");
  }

  @Test
  public void testBuildPublicIdWithPostfix_stripAccents() {
    SCMRepository repo = buildRepo("cömpütër", "Präzisionsmeßgerät");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);
    String actualName = scmApplicationNameConverter.buildName(repo);
    assertThat(actualId).isEqualTo("Praezisionsmessgeraet__coempueter");
    assertThat(actualName).isEqualTo("Praezisionsmessgeraet - Coempueter");
  }

  private SCMRepository buildRepo(final String namespace, final String project) {
    return new SCMRepository(BITBUCKET, "http://example.com", false, namespace, project, "description");
  }
}
