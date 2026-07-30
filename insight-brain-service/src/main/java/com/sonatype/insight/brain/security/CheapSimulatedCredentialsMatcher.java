/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.security.SecureRandom;
import java.util.Optional;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.apache.shiro.lang.util.ByteSource;

/**
 * A {@link PasswordMatcher} whose <em>simulated</em> credentials use a single SHA-256 pass rather than the
 * realm's real hash algorithm.
 *
 * <p>
 * Since Shiro 2.1, {@code AuthenticatingRealm.simulateFailedLogin} runs the realm's credentials matcher
 * against a synthetic credential whenever the realm declines a token (i.e. {@code doGetAuthenticationInfo}
 * returns {@code null}), to equalise response time between existing and non-existent principals. With the
 * default {@link PasswordMatcher} that synthetic credential is Argon2id, so every declining realm pays a
 * full 64 MiB memory-hard verify. In a multi-realm chain that is one such verify per declining realm on
 * every request, which saturates the server under per-request basic-auth / token load.
 * </p>
 *
 * <p>
 * This matcher keeps the timing-equalisation behaviour but makes the synthetic credential an unmatchable
 * {@code $shiro1$SHA-256} hash of a random secret, which is orders of magnitude cheaper to verify. The real
 * verification path is unchanged. To restore Shiro's Argon2id simulation, drop this class and use
 * {@link PasswordMatcher} directly; to disable simulation entirely, override
 * {@code createSimulatedCredentials} to return {@link Optional#empty()} (note: that logs a warning per
 * request).
 * </p>
 */
class CheapSimulatedCredentialsMatcher
    extends PasswordMatcher
{
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Override
  public Optional<AuthenticationInfo> createSimulatedCredentials() {
    byte[] secret = new byte[32];
    SECURE_RANDOM.nextBytes(secret);

    // SHA-256 with a single iteration is fixed here rather than taken from FIPSConfig, because this hash
    // exists only to be compared against and discarded: the 256-bit random source is never retained, so
    // no submitted credential can match it and the iteration count protects nothing. SHA-256 is
    // FIPS 180-4 approved, so the simulated path remains valid under FIPS.
    DefaultHashService hashService = new DefaultHashService();
    hashService.setDefaultAlgorithmName(Sha256Hash.ALGORITHM_NAME);
    Hash hash = hashService.computeHash(new HashRequest.Builder()
        .setSource(ByteSource.Util.bytes(secret))
        .setAlgorithmName(Sha256Hash.ALGORITHM_NAME)
        .addParameter("SimpleHash.iterations", 1)
        .build());

    return Optional.of(new SimpleAuthenticationInfo("simulated", new Shiro1CryptFormat().format(hash), "simulated"));
  }
}
