/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

public final class TemplateHelper
{
  private TemplateHelper() {

  }

  public static String readResource(Class<?> testClazz, String resourceName) throws Exception {
    return readResource(testClazz.getSimpleName(), resourceName);
  }

  public static String readResource(String dirName, String resourceName) throws IOException, URISyntaxException {
    final String file = format("/%s/", dirName) + resourceName;
    final URL dirUrl = TemplateHelper.class.getResource(file);
    if (dirUrl == null) {
      throw new IOException(format("The resource %s does not exist", file));
    }
    final Path path = Paths.get(dirUrl.toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  public static String removeDateFromOutput(final String value) {
    return value.trim().replaceAll("as of _.*", "");
  }

  public static void assertRenderedOutput(
      final Optional<String> actualOutput,
      final Class<?> clazz,
      final String expectedOutputFile) throws Exception
  {
    final String expectedOutput;
    expectedOutput = readResource(clazz, expectedOutputFile);
    assertThat(actualOutput).isNotEmpty();
    assertThat(removeDateFromOutput(actualOutput.get())).isEqualTo(removeDateFromOutput(expectedOutput));
  }

  public static void write(String content, String outputPath) throws IOException {
    Files.write(Paths.get(outputPath), content.getBytes(), CREATE, WRITE, TRUNCATE_EXISTING);
  }
}
