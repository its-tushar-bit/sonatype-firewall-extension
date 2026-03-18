/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class FIPSKeystorePasswordGeneratorTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    insertBouncyCastleFipsProvider();
  }

  @After
  public void tearDown() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testGenerateDeterministicPassword_returnsValidBase64() throws Exception {
    File workDir = temporaryFolder.newFolder("sonatype-work");

    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);

    // Base64 encoded 256-bit key should be 44 characters (32 bytes * 4/3 + padding)
    assertThat(password).hasSize(44);

    // Verify it's valid Base64 by decoding it (throws exception if invalid)
    assertThatCode(() -> Base64.getDecoder().decode(password))
        .doesNotThrowAnyException();

    // Verify decoded length is 32 bytes (256 bits)
    byte[] decoded = Base64.getDecoder().decode(password);
    assertThat(decoded).hasSize(32);
  }

  @Test
  public void testGenerateDeterministicPassword_differentDirectoriesProduceDifferentPasswords() throws Exception {
    File workDir1 = temporaryFolder.newFolder("sonatype-work-1");
    File workDir2 = temporaryFolder.newFolder("sonatype-work-2");

    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir1);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir2);

    assertThat(password1).isNotEqualTo(password2);
  }

  @Test
  public void testGenerateDeterministicPassword_handlesNonExistentDirectory() throws Exception {
    File nonExistentDir = new File(temporaryFolder.getRoot(), "non-existent");

    // Should not throw exception, should handle gracefully
    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(nonExistentDir);

    assertThat(password).isNotEmpty();
  }

  @Test
  public void testGenerateDeterministicPassword_handlesSpecialCharactersInPath() throws Exception {
    // Create directory with spaces and special characters
    File specialDir = temporaryFolder.newFolder("sonatype work & test");

    String password = FIPSKeystorePasswordGenerator.generateDeterministicPassword(specialDir);

    assertThat(password).isNotEmpty();
  }

  @Test
  public void testGenerateDeterministicPassword_usesAbsolutePath() throws Exception {
    File workDir = temporaryFolder.newFolder("sonatype-work");

    // Test that same directory accessed different ways produces same password
    File sameDirDifferentWay = new File(workDir.getAbsolutePath());

    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(sameDirDifferentWay);

    assertThat(password1).isEqualTo(password2);
  }

  @Test
  public void testGenerateDeterministicPassword_returnsSamePassword() throws Exception {
    File workDir = temporaryFolder.newFolder("sonatype-work");

    // Test multiple calls to ensure stability
    String password1 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password2 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);
    String password3 = FIPSKeystorePasswordGenerator.generateDeterministicPassword(workDir);

    assertThat(password1).isEqualTo(password2);
    assertThat(password2).isEqualTo(password3);
    assertThat(password1).isNotEmpty();
  }
}
