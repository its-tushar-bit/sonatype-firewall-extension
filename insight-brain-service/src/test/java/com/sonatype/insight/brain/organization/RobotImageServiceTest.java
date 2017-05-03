/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class RobotImageServiceTest
{

  @Test
  public void testGetImage() throws IOException {
    RobotImageService robotImageService = new RobotImageService();
    for (int i = 0; i <= RobotImageService.IMAGE_NUMBER_MAX; i++) {
      String hash = new String(new char[]{(char) i});
      assertThat(hash.hashCode(), is(i));
      int expectedFileIndex = (i == 0 || i == RobotImageService.IMAGE_NUMBER_MAX) ? 1 : i + 1;
      byte[] expectedFileBytes = IOUtils.toByteArray(getClass()
          .getResourceAsStream("/com/sonatype/insight/brain/robot/images/robot_" + expectedFileIndex + ".png"));
      byte[] actualFileBytes = robotImageService.getImage(hash);
      assertArrayEquals(expectedFileBytes, actualFileBytes);
    }
  }
}
