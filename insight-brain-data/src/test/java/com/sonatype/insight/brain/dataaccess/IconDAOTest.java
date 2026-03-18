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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IconDAOTest
{
  private final IconDAO iconDAO = new IconDAO();

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
    assertThat(iconBytes).isNotEmpty();
  }

  @Test
  public void testGetIcon_InvalidOwnerId() throws Exception {
    File iconDir = tmpDir.newFolder();
    assertThatThrownBy(() -> iconDAO.getIcon(BAD_OWNER_ID, iconDir)).isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid value: " + BAD_OWNER_ID);
  }

  @Test
  public void testSetIcon_InvalidOwnerId() throws Exception {
    File iconDir = tmpDir.newFolder();
    ByteArrayInputStream byteArrayInputStream = getIconImageStream();
    assertThatThrownBy(() -> iconDAO.setIcon(BAD_OWNER_ID, iconDir, byteArrayInputStream))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid value: " + BAD_OWNER_ID);
  }

  @Test
  public void testDeleteIcon() throws Exception {
    String ownerId = "testId";
    File iconDir = tmpDir.newFolder();
    ByteArrayInputStream byteArrayInputStream = getIconImageStream();
    iconDAO.setIcon(ownerId, iconDir, byteArrayInputStream);

    byte[] iconBytes = iconDAO.getIcon(ownerId, iconDir);
    assertThat(iconBytes).isNotEmpty();

    iconDAO.deleteIcon(ownerId, iconDir);
    assertThat(new File(iconDir, ownerId)).doesNotExist();
  }

  private ByteArrayInputStream getIconImageStream() throws IOException {
    BufferedImage image = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteArrayOutputStream);
    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
  }
}
