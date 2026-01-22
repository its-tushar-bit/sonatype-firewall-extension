/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApplicationNameConverterTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationNameConverter applicationNameConverter;

  @Test
  public void testToName_removesNonWordCharacters() {
    String actual = applicationNameConverter.toName("n*a%m@e!.fo*o-ba@r_baz#");
    assertThat(actual).isEqualTo("name.foo-bar_baz");
  }

  @Test
  public void testToName_removesWhitespace_keepsSingleSpace() {
    String actual = applicationNameConverter.toName(" l1 2  3   t\tnl\n ");
    assertThat(actual).isEqualTo("l1 2 3 tnl");
  }

  @Test
  public void testToPublicId_removesWhitespace() {
    String actual = applicationNameConverter.toPublicId(" l1 2  3   t\tnl\n ");
    assertThat(actual).isEqualTo("l123tnl");
  }

  @Test
  public void testToName_retainsAccents() {
    String actualName = applicationNameConverter.toName("hélló cömpütër");
    assertThat(actualName).isEqualTo("hélló cömpütër");
    NameHelper.validate(actualName);
  }

  @Test
  public void testToPublicId_retainsAccents() {
    String actualId = applicationNameConverter.toPublicId("hélló cömpütër");
    assertThat(actualId).isEqualTo("héllócömpütër");
    NameHelper.validate(actualId);
  }
}
