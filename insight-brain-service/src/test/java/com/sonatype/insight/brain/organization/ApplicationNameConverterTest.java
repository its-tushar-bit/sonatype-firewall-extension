/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationNameConverterTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationNameConverter applicationNameConverter;

  @Test
  public void testConvertName_removesNonWordCharacters() {
    String actual = applicationNameConverter.toName("n*a%m@e!");
    assertThat(actual).isEqualTo("name");
  }

  @Test
  public void testConvertName_removesWhitespace_keepsSingleSpace() {
    String actual = applicationNameConverter.toName(" l1 2  3   t\tnl\n ");
    assertThat(actual).isEqualTo("l1 2 3 tnl");
  }

  @Test
  public void testConvertPublicId_removesWhitespace() {
    String actual = applicationNameConverter.toPublicId(" l1 2  3   t\tnl\n ");
    assertThat(actual).isEqualTo("l123tnl");
  }

  @Test
  public void testConvertName_stripAccents() {
    String actualName = applicationNameConverter.toName("hélló cömpütër");
    assertThat(actualName).isEqualTo("hello coempueter");
  }

  @Test
  public void testConvertPublicId_stripAccents() {
    String actualId = applicationNameConverter.toPublicId("hélló cömpütër");
    assertThat(actualId).isEqualTo("hellocoempueter");
  }
}
