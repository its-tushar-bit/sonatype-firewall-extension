/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.test.InjectedTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class PasswordHandlerTest
    extends InjectedTest
{
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
}
