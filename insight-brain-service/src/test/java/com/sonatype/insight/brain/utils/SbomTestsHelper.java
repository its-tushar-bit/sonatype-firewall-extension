/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SbomTestsHelper
{
  public static Path createTestFileForSbomMetadata(File sbomDir, URL fileURL) throws IOException {
    Files.createDirectories(sbomDir.toPath().normalize());
    final Path tempFilePath =
        Files.createTempFile(sbomDir.toPath().normalize(), "", ".xml");

    Files.copy(new File(fileURL.getPath()).toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

    return tempFilePath;
  }
}
