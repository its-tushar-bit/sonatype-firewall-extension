/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;

public class FipsCipherTest
{
  private final FipsCipher fipsCipher = new FipsCipher();

  @BeforeAll
  public static void setUp() {
    insertBouncyCastleFipsProvider();
  }

  @AfterAll
  public static void tearDown() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testEncryptDecrypt() {
    String key = "shesoldseashells";
    String plaintext = "thisismypassword";

    String decrypted = fipsCipher.decryptDecorated(fipsCipher.encryptAndDecorate(plaintext, key), key);
    assertThat(decrypted).isEqualTo(plaintext);

    plaintext = "";
    decrypted = fipsCipher.decryptDecorated(fipsCipher.encryptAndDecorate(plaintext, key), key);
    assertThat(decrypted).isEqualTo(plaintext);

    plaintext = "    ";
    decrypted = fipsCipher.decryptDecorated(fipsCipher.encryptAndDecorate(plaintext, key), key);
    assertThat(decrypted).isEqualTo(plaintext);

    plaintext = null;
    decrypted = fipsCipher.decryptDecorated(fipsCipher.encryptAndDecorate(plaintext, key), key);
    assertThat(decrypted).isNull();

    plaintext = "{}";
    decrypted = fipsCipher.decryptDecorated(plaintext, key);
    assertThat(decrypted).isEmpty();

    decrypted = fipsCipher.decryptDecorated(fipsCipher.encryptAndDecorate(plaintext, key), key);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  public void testIsEncryptedString() {
    assertThat(fipsCipher.isEncryptedString("")).isFalse();
    assertThat(fipsCipher.isEncryptedString("{}")).isFalse();
    assertThat(fipsCipher.isEncryptedString("{thisismatching}")).isTrue();
    assertThat(fipsCipher.isEncryptedString("thisisnotmatching")).isFalse();
    assertThat(fipsCipher.isEncryptedString("thisisnotmatching{thisismatching}thisisnotmatching")).isTrue();
  }
}
