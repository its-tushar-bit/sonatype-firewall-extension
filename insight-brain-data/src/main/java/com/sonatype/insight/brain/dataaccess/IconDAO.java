/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import com.sonatype.insight.brain.utils.IdValidationUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.io.FileUtils;

public class IconDAO
{
  private static final String ICON_FILE_NAME = "icon420px.png";

  public byte[] getIcon(String ownerId, File iconDirectory) throws IOException {
    // Validate the ownerId to prevent traversal attacks on file create
    IdValidationUtils.validate(ownerId);

    File applicationIconDirectory = new File(iconDirectory, ownerId);
    if (!applicationIconDirectory.exists()) {
      return null;
    }
    File iconFile = new File(applicationIconDirectory, ICON_FILE_NAME);
    if (!iconFile.exists()) {
      return null;
    }

    BufferedImage image = ImageIO.read(iconFile);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", byteArrayOutputStream);
    return byteArrayOutputStream.toByteArray();
  }

  public void setIcon(String ownerId, File iconDirectory, InputStream imageStream) throws IOException {
    final int dimension = 420;
    Image image = ImageIO.read(imageStream);

    // Invalid image types do not throw exception on ImageIO.read but instead returns null. Throw exception when
    // null is returned
    if (image == null) {
      throw new BadRequestException("Invalid image file.");
    }

    // Validate the ownerId to prevent traversal attacks on file create
    IdValidationUtils.validate(ownerId);

    BufferedImage resizedImage = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = resizedImage.createGraphics();
    g.drawImage(image, 0, 0, dimension, dimension, null);
    g.dispose();

    File applicationIconDirectory = new File(iconDirectory, ownerId);
    Files.createDirectories(applicationIconDirectory.toPath());

    File iconFile = new File(applicationIconDirectory, ICON_FILE_NAME);
    if (!iconFile.exists()) {
      iconFile.createNewFile();
    }

    ImageIO.write(resizedImage, "png", iconFile);
  }

  public void deleteIcon(String ownerId, File iconDirectory) throws IOException {
    // Validate the ownerId to prevent traversal attacks on file create
    IdValidationUtils.validate(ownerId);

    File applicationIconDirectory = new File(iconDirectory, ownerId);
    if (applicationIconDirectory.exists()) {
      FileUtils.deleteDirectory(applicationIconDirectory);
    }
  }
}
