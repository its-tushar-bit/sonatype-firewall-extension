/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.model.tag.Tag;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Additional assertion methods specific to our entities.
 */
public class Assert
{
  public static void assertTag(Tag expected, Tag actual) {
    assertThat(actual.getOrganizationId(), is(expected.getOrganizationId()));
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getNameLowercaseNoWhitespace(), is(expected.getNameLowercaseNoWhitespace()));
    assertThat(actual.getDescription(), is(expected.getDescription()));
    assertThat(actual.getColor(), is(expected.getColor()));
  }
}
