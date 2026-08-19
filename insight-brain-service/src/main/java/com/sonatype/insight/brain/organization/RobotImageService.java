/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.InputStream;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Select an image by mapping an input string to an image based on internal algorithm.
 */
@Named
@Singleton
public class RobotImageService
{
  private static final Logger log = LoggerFactory.getLogger(RobotImageService.class);

  public static final int IMAGE_NUMBER_MAX = 100;

  public byte[] getImage(String hashcode) {
    int imageNumber = Math.abs(hashcode.hashCode()) % IMAGE_NUMBER_MAX + 1;
    log.debug("Loading image at next index {}", imageNumber);
    String resourceFileName = "robot_" + imageNumber + ".png";
    final String imageFile = "/com/sonatype/insight/brain/robot/images/" + resourceFileName;
    try (InputStream stream = getClass().getResourceAsStream(imageFile)) {
      return IOUtils.toByteArray(stream);
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to process resource " + resourceFileName, e);
    }
  }
}
