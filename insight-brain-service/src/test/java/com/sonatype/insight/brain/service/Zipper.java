/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

  public static void zipFilesInDirectory(File sourceDir, File zipTarget) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(zipTarget); ZipOutputStream zos = new ZipOutputStream(fos)) {
      File[] children = sourceDir.listFiles();
      assert children != null;
      for (File childFile : children) {
        zipFile(childFile, childFile.getName(), zos);
      }
    }
  }

  public static void zipDirectory(File sourceDir, File zipTarget) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(zipTarget); ZipOutputStream zos = new ZipOutputStream(fos)) {
      zipFile(sourceDir, sourceDir.getName(), zos);
    }
  }

  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    if (fileToZip.isDirectory()) {
      if (fileName.endsWith("/")) {
        zos.putNextEntry(new ZipEntry(fileName + "/"));
      }
      else {
        zos.putNextEntry(new ZipEntry(fileName + "/"));
      }
      zos.closeEntry();
      File[] children = fileToZip.listFiles();
      assert children != null;
      for (File childFile : children) {
        zipFile(childFile, fileName + "/" + childFile.getName(), zos);
      }
      return;
    }
    try (FileInputStream fis = new FileInputStream(fileToZip)) {
      ZipEntry zipEntry = new ZipEntry(fileName);
      zos.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zos.write(bytes, 0, length);
      }
      zos.closeEntry();
    }
  }
}
