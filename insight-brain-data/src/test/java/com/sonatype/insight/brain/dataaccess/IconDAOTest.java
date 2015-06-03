/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class IconDAOTest
{
  private IconDAO iconDAO = new IconDAO();

  private static final String BAD_OWNER_ID = "/../bad";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void testGetIcon() throws Exception {

    String ownerId = "testId";
    File iconDir = tmpDir.newFolder();
    ByteArrayInputStream byteArrayInputStream = getIconImageStream();
    iconDAO.setIcon(ownerId, iconDir, byteArrayInputStream);

    byte[] iconBytes = iconDAO.getIcon(ownerId, iconDir);
    assertThat(iconBytes, notNullValue());
    assertThat(iconBytes.length, greaterThan(0));
  }

  @Test
  public void testGetIcon_InvalidOwnerId() throws Exception {
    File iconDir = tmpDir.newFolder();
    try {
      iconDAO.getIcon(BAD_OWNER_ID, iconDir);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid value: " + BAD_OWNER_ID));
    }
  }

  @Test
  public void testSetIcon_InvalidOwnerId() throws Exception {
    File iconDir = tmpDir.newFolder();
    try {
      ByteArrayInputStream byteArrayInputStream = getIconImageStream();
      iconDAO.setIcon(BAD_OWNER_ID, iconDir, byteArrayInputStream);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid value: " + BAD_OWNER_ID));
    }
  }

  private ByteArrayInputStream getIconImageStream() throws IOException {
    BufferedImage image = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteArrayOutputStream);
    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
  }
}
