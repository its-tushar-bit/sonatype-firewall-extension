/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

public class Zipper
{
  public static void zip(File sourceDir, File destZipFile) throws IOException {
    URI zipUri = URI.create("jar:" + destZipFile.toURI());

    Map<String, String> env = Collections.singletonMap("create", "true");
    try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipUri, env)) {
      addToZip(sourceDir.toPath(), zipFileSystem.getPath("/"));
    }
  }

  private static void addToZip(Path sourceDir, Path parentPathInZip) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourceDir)) {
      for (Path path : dirStream) {
        String filename = path.getFileName().toString();
        Path pathInZip = parentPathInZip.resolve(filename);
        Files.copy(path, pathInZip, StandardCopyOption.REPLACE_EXISTING);
        if (Files.isDirectory(path)) {
          addToZip(path, pathInZip);
        }
      }
    }
  }
}
