/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testsupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 replacement for the JUnit 4 {@code org.junit.rules.TemporaryFolder} rule, exposing the same
 * {@code getRoot()} / {@code newFile(...)} / {@code newFolder(...)} / {@code create()} API so migrated tests keep
 * their call sites unchanged. Register it as an instance field with {@code @RegisterExtension} to mirror the rule's
 * per-test lifecycle (a fresh temp directory before each test, recursively deleted afterwards).
 */
public class TempFolder
    implements BeforeEachCallback, AfterEachCallback
{
  private File root;

  @Override
  public void beforeEach(final ExtensionContext context) throws IOException {
    create();
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    delete();
  }

  /**
   * Creates the temporary folder. Called automatically before each test; also exposed for tests that created the
   * instance manually and drive it themselves.
   */
  public void create() throws IOException {
    root = Files.createTempDirectory("junit").toFile();
  }

  public File getRoot() {
    if (root == null) {
      throw new IllegalStateException(
          "the temporary folder has not yet been created (use @RegisterExtension or call create())");
    }
    return root;
  }

  public File newFile() throws IOException {
    return File.createTempFile("junit", null, getRoot());
  }

  public File newFile(final String fileName) throws IOException {
    File file = new File(getRoot(), fileName);
    if (!file.createNewFile()) {
      throw new IOException("a file with the name '" + fileName + "' already exists in the test folder");
    }
    return file;
  }

  public File newFolder() throws IOException {
    return Files.createTempDirectory(getRoot().toPath(), "junit").toFile();
  }

  public File newFolder(final String... folderNames) throws IOException {
    File file = getRoot();
    for (String folderName : folderNames) {
      file = new File(file, folderName);
      // Match the JUnit 4 TemporaryFolder contract (and newFile above): fail if the folder could not be created,
      // including when it already exists.
      if (!file.mkdir()) {
        throw new IOException("a folder with the name '" + folderName + "' already exists");
      }
    }
    return file;
  }

  public void delete() {
    if (root == null || !root.exists()) {
      return;
    }
    try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
      paths.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
    }
    catch (IOException e) {
      // best-effort cleanup, mirroring the JUnit 4 rule which ignores deletion failures
    }
    finally {
      root = null;
    }
  }
}
