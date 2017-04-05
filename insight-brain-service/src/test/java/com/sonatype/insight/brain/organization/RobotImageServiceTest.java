/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class RobotImageServiceTest
{
  @Test
  public void testDoGetImage() {
    RobotImageService robotImageService = new RobotImageService();
    for (int i = 1; i <= RobotImageService.IMAGE_NUMBER_MAX; i++) {
      byte[] imageBytes = robotImageService.doGetImage(i);
      assertThat(imageBytes, notNullValue());
      assertThat(imageBytes.length, greaterThan(0));
    }
  }
}
