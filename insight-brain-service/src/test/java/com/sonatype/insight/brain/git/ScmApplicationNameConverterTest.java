/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.junit.Test;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmApplicationNameConverterTest
    extends AbstractComponentTest
{
  @Inject
  ScmApplicationNameConverter scmApplicationNameConverter;

  @Test
  public void testBuildPublicId_concatenatesNames() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);

    assertThat(actualId).isEqualTo("bar__foo");
  }

  @Test
  public void testBuildName_concatenatesNames() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualName = scmApplicationNameConverter.buildName(repo);

    assertThat(actualName).isEqualTo("Bar - Foo");
  }

  @Test
  public void testBuildPublicId_allowedSpecialCharacters() {
    SCMRepository repo = buildRepo("foo-common_bar.foo", "bar-common_foo.bar");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);

    assertThat(actualId).isEqualTo("bar-common_foo.bar__foo-common_bar.foo");
  }

  @Test
  public void testBuildName_allowedSpecialCharacters() {
    SCMRepository repo = buildRepo("foo-common_bar.foo", "bar-common_foo.bar");
    String actualName = scmApplicationNameConverter.buildName(repo);

    assertThat(actualName).isEqualTo("Bar Common Foo.bar - Foo Common Bar.foo");
    NameHelper.validate(actualName);
  }

  @Test
  public void testBuildPublicId_stripsInvalidCharacters() {
    SCMRepository repo = buildRepo("2a-c∫6d 2=*", "1ab∫cd 2-_*");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);
    assertThat(actualId).isEqualTo("1abcd2-___2a-c6d2");
  }

  @Test
  public void testBuildName_stripsInvalidCharacters() {
    SCMRepository repo = buildRepo("2a-c∫6d 2=*", "1ab∫cd 2-_*");
    String actualName = scmApplicationNameConverter.buildName(repo);
    assertThat(actualName).isEqualTo("1abcd 2 - 2a C6d 2");
    NameHelper.validate(actualName);
  }

  @Test
  public void testBuildPublicIdWithPostfix_appendsPostfix() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualId = scmApplicationNameConverter.buildPublicIdWithPostfix(repo, 3);
    assertThat(actualId).isEqualTo("bar__foo_3");
  }

  @Test
  public void testBuildNameWithPostfix_appendsPostfix() {
    SCMRepository repo = buildRepo("foo", "bar");
    String actualName = scmApplicationNameConverter.buildNameWithPostfix(repo, 3);
    assertThat(actualName).isEqualTo("Bar - Foo - 3");
  }

  @Test
  public void testBuildPublicIdWithPostfix_stripsInvalidCharacters() {
    SCMRepository repo = buildRepo("foo", "1ab∫cd 2-_*");
    String actualId = scmApplicationNameConverter.buildPublicIdWithPostfix(repo, 3);
    assertThat(actualId).isEqualTo("1abcd2-___foo_3");
  }

  @Test
  public void testBuildNameWithPostfix_stripsInvalidCharacters() {
    SCMRepository repo = buildRepo("foo", "1ab∫cd 2-_*");
    String actualName = scmApplicationNameConverter.buildNameWithPostfix(repo, 3);
    assertThat(actualName).isEqualTo("1abcd 2 - Foo - 3");
    NameHelper.validate(actualName);
  }

  @Test
  public void testBuildPublicId_RetainsAccents() {
    SCMRepository repo = buildRepo("cömpütër", "Präzisionsmeßgerät");
    String actualId = scmApplicationNameConverter.buildPublicId(repo);
    assertThat(actualId).isEqualTo("Präzisionsmeßgerät__cömpütër");
  }

  @Test
  public void testBuildName_RetainsAccents() {
    SCMRepository repo = buildRepo("cömpütër", "Präzisionsmeßgerät");
    String actualName = scmApplicationNameConverter.buildName(repo);
    assertThat(actualName).isEqualTo("Präzisionsmeßgerät - Cömpütër");
    NameHelper.validate(actualName);
  }

  private SCMRepository buildRepo(final String namespace, final String project) {
    return new SCMRepository(BITBUCKET, "http://example.com", null, false, namespace, project, "description");
  }
}
