/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;

import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PasswordHandlerTest
    extends BrainInjectedTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Inject
  private PasswordHandler pwHandler;

  @Test
  public void testEncryptPasswordDecryptPassword() {
    char[] pw = "password".toCharArray();
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isEqualTo(pw);
  }

  @Test
  public void testEncryptPasswordDecryptPassword_EmptyString() {
    char[] pw = "".toCharArray();
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isEqualTo(pw);
  }

  @Test
  public void testEncryptPasswordDecryptPassword_BlankString() {
    char[] pw = "    ".toCharArray();
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isEqualTo(pw);
  }

  @Test
  public void testEncryptPasswordDecryptPassword_DecoratorString() {
    char[] pw = "{}".toCharArray();
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isEqualTo(pw);
  }

  @Test
  public void testEncryptPasswordDecryptPassword_SmileyFace() {
    char[] pw = "\uD83D\uDE0A".toCharArray();
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isEqualTo(pw);
  }

  @Test
  public void testEncryptPasswordDecryptPassword_NullValue() {
    char[] pw = null;
    assertThat(pwHandler.decryptPassword(pwHandler.encryptPassword(pw))).isNull();
  }

  @Test
  public void testEncryptPassword_isEncrypted() {
    char[] pw = "secret-password".toCharArray();
    char[] encryptedPassword = pwHandler.encryptPassword(pw);
    assertThat(pwHandler.isEncrypted(encryptedPassword)).isTrue();
  }

  @Test
  public void testEncryptPassword_isEncrypted_empty() {
    char[] pw = "".toCharArray();
    assertThat(pwHandler.isEncrypted(pw)).isFalse();
  }

  @Test
  public void testEncryptPassword_isEncrypted_notEncrypted() {
    char[] pw = "secret-password".toCharArray();
    assertThat(pwHandler.isEncrypted(pw)).isFalse();
  }

  @Test
  public void testEncryptPassword_isEncrypted_null() {
    char[] pw = null;
    assertThat(pwHandler.isEncrypted(pw)).isFalse();
  }

  @Test
  public void testEncryptPassword_isEncrypted_stringNull() {
    String pw = null;
    assertThat(pwHandler.isEncrypted(pw)).isFalse();
  }

  @Test
  public void testEncryptPassword_FipsMode() {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(FIPSModeDetector.isEnabled()).isTrue();

    String password = "thisisthepassword";
    PasswordHandler nonFipsPasswordHandler = new PasswordHandler(new TestEncryptionKeyStore());

    assertThatThrownBy(() -> nonFipsPasswordHandler.decryptPassword(nonFipsPasswordHandler.encryptPassword(password)))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(InvalidKeyException.class)
        .hasMessageContaining("AES key must be of length 128, 192, or 256");

    PasswordHandler fipsPasswordHandler = new PasswordHandler(new TestFipsEncryptionKeyStore());
    assertThat(fipsPasswordHandler.decryptPassword(fipsPasswordHandler.encryptPassword(password)))
        .isEqualTo(password);
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testEncryptPassword_FipsMode_NonStandardInput() {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(FIPSModeDetector.isEnabled()).isTrue();

    PasswordHandler passwordHandler = new PasswordHandler(new TestFipsEncryptionKeyStore());

    String password = "";
    assertThat(passwordHandler.decryptPassword(passwordHandler.encryptPassword(password)))
        .isEqualTo(password);

    password = "    ";
    assertThat(passwordHandler.decryptPassword(passwordHandler.encryptPassword(password)))
        .isEqualTo(password);

    password = null;
    assertThat(passwordHandler.decryptPassword(passwordHandler.encryptPassword(password)))
        .isEqualTo(password);

    password = "\t&#128507;U+1F605的字δБ";
    assertThat(passwordHandler.decryptPassword(passwordHandler.encryptPassword(password)))
        .isEqualTo(password);
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testIsEncrypted_FipsMode() {
    insertBouncyCastleFipsProvider();
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    assertThat(FIPSModeDetector.isEnabled()).isTrue();

    PasswordHandler passwordHandler = new PasswordHandler(new TestFipsEncryptionKeyStore());

    String password = "thisisthepassword";
    String encryptedPassword = passwordHandler.encryptPassword(password);
    assertThat(passwordHandler.isEncrypted(encryptedPassword)).isTrue();

    encryptedPassword = "{%this$is*(going_to)match}";
    assertThat(passwordHandler.isEncrypted(encryptedPassword)).isTrue();
    removeBouncyCastleFipsProvider();

    encryptedPassword = "%this$is*not(going_to)match";
    assertThat(passwordHandler.isEncrypted(encryptedPassword)).isFalse();
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testEncryptPassword_FipsMode_Throws_NoSuchProviderException() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    assertThatThrownBy(() -> new PasswordHandler(new TestFipsEncryptionKeyStore()).encryptPassword("password"))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(NoSuchProviderException.class)
        .hasMessageContaining("No such provider: BCFIPS");
  }
}
