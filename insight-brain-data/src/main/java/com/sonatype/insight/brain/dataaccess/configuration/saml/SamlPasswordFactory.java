/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.PasswordHandler;
import org.sonatype.licensing.util.LicensingUtil;

@Named
@Singleton
public class SamlPasswordFactory
{
  private final PasswordHandler passwordHandler;

  @Inject
  public SamlPasswordFactory(final PasswordHandler passwordHandler) {
    this.passwordHandler = passwordHandler;
  }

  public char[] decryptPassword(char[] password) {
    if (FIPSModeDetector.isEnabled()) {
      return passwordHandler.decryptPassword(password);
    }

    return unobfuscatePassword(String.valueOf(password));
  }

  public char[] encryptPassword(char[] password) {
    if (FIPSModeDetector.isEnabled()) {
      return passwordHandler.encryptPassword(password);
    }

    return obfuscatePassword(String.valueOf(password));
  }

  private char[] unobfuscatePassword(final String obfuscatePassword) {
    // The obfuscated password is stored as a comma-separated list of long values, for ex:
    // B262BEF4066834E2, 1E31D4FF44C663F0, 2AF7E801C69AC83C
    long[] obfuscated = Stream.of(obfuscatePassword.split(",")).mapToLong(s -> Long.parseUnsignedLong(s, 16)).toArray();
    return LicensingUtil.unobfuscate(obfuscated).toCharArray();
  }

  private char[] obfuscatePassword(final String unobfuscatePassword) {
    // ObfuscatedString.obfuscate returns a string that can be pasted directly into Java code, for ex:
    // new ObfuscatedString(new long[] {0xB262BEF4066834E2L, 0x1E31D4FF44C663F0L, 0x2AF7E801C69AC83CL}).toString() /* =>
    // "qwedqwdeq" */
    // We only need the long values in between curly braces.
    String obfuscated = LicensingUtil.obfuscate(unobfuscatePassword);
    return obfuscated.substring(obfuscated.indexOf('{') + 1, obfuscated.indexOf('}')) //
        .replace("0x", "") //
        .replace("L", "") //
        .replace(" ", "") //
        .toCharArray();
  }
}

