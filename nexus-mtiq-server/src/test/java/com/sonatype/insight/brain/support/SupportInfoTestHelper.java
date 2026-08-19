/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;

public class SupportInfoTestHelper
{
  public static final String WORK_DIR = "support";

  public static File writeFile(String workDir, String fileContent, String fileName) throws IOException {
    File outputFile = new File(workDir, fileName);
    FileUtils.write(outputFile, fileContent, StandardCharsets.UTF_8);

    return outputFile;
  }

  public static void cleanWorkDir(String workDir) throws IOException {
    Files.walkFileTree(Paths.get(workDir),
        new SimpleFileVisitor<Path>()
        {
          @Override
          public FileVisitResult postVisitDirectory(
              Path dir,
              IOException exc) throws IOException
        {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(
              Path file,
              BasicFileAttributes attrs) throws IOException
        {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
