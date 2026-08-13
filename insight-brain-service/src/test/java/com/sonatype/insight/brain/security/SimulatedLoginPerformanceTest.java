/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.sun.management.ThreadMXBean;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.junit.jupiter.api.Test;

/**
 * Performance A/B for the {@link CheapSimulatedCredentialsMatcher} fix.
 *
 * <p>
 * Reproduces the incident login pattern: an LDAP-realm principal (a user token / basic-auth username the
 * Argon2 realms do not own) authenticating concurrently many times. Each attempt is declined by the realm,
 * which makes Shiro run {@code simulateFailedLogin}. With the default {@link PasswordMatcher} that pays a
 * full 64 MiB Argon2id verify per attempt (Shiro's {@link DefaultPasswordService} argon2id equals IQ's
 * non-FIPS hashing); with {@link CheapSimulatedCredentialsMatcher} it is a single SHA-256 pass. The test
 * asserts the fix cuts both CPU time and allocation by more than an order of magnitude for the same load.
 * </p>
 */
public class SimulatedLoginPerformanceTest
{
  private static final int THREADS = Math.min(8, Runtime.getRuntime().availableProcessors());

  private static final int LOGINS_PER_THREAD = 3;

  @Test
  public void testCheapSimulatedCredentialsCutCpuAndMemoryForDeclinedLogins() throws Exception {
    measure(argon2Matcher()); // warm up the JIT for both paths before measuring
    measure(cheapMatcher());

    Stats before = measure(argon2Matcher());
    Stats after = measure(cheapMatcher());

    int total = THREADS * LOGINS_PER_THREAD;
    System.out.printf("%nDeclined logins: %d concurrent (%d threads x %d)%n", total, THREADS, LOGINS_PER_THREAD);
    System.out.printf("BEFORE (Argon2id simulated credential): cpu=%7.1f ms  allocated=%8.1f MiB  wall=%6.1f ms%n",
        before.cpuMillis(), before.allocatedMiB(), before.wallMillis());
    System.out.printf("AFTER  (SHA-256 simulated credential):  cpu=%7.1f ms  allocated=%8.1f MiB  wall=%6.1f ms%n",
        after.cpuMillis(), after.allocatedMiB(), after.wallMillis());
    System.out.printf("Reduction: cpu %.0fx  memory %.0fx%n",
        (double) before.cpuNanos() / Math.max(1, after.cpuNanos()),
        (double) before.allocatedBytes() / Math.max(1, after.allocatedBytes()));

    // Sanity: the "before" path really did the memory-hard Argon2 work (>= 32 MiB per verify).
    assertThat(before.allocatedBytes()).isGreaterThan((long) total * 32 * 1024 * 1024);

    assertThat(after.allocatedBytes()).isLessThan(before.allocatedBytes() / 10);
    assertThat(after.cpuNanos()).isLessThan(before.cpuNanos() / 10);

    // Absolute bound, so the relative assertions above cannot pass while both paths are expensive.
    // A single SHA-256 pass allocates on the order of hundreds of bytes; Argon2id allocates 64 MiB.
    assertThat(after.allocatedBytes()).isLessThan((long) total * 1024 * 1024);
  }

  private static PasswordMatcher argon2Matcher() {
    PasswordMatcher matcher = new PasswordMatcher();
    matcher.setPasswordService(new DefaultPasswordService());
    return matcher;
  }

  private static PasswordMatcher cheapMatcher() {
    PasswordMatcher matcher = new CheapSimulatedCredentialsMatcher();
    matcher.setPasswordService(new DefaultPasswordService());
    return matcher;
  }

  private static Stats measure(CredentialsMatcher matcher) throws Exception {
    AuthenticatingRealm realm = new AuthenticatingRealm()
    {
      @Override
      protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        return null; // LDAP principal: not owned by this realm, so it declines and Shiro simulates
      }
    };
    realm.setCredentialsMatcher(matcher);
    realm.setAuthenticationCachingEnabled(false);
    realm.getAuthenticationInfo(token()); // populate the cached simulated credential once

    ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    threadBean.setThreadAllocatedMemoryEnabled(true);
    threadBean.setThreadCpuTimeEnabled(true);

    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch ready = new CountDownLatch(THREADS);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<long[]>> results = new ArrayList<>();
    for (int t = 0; t < THREADS; t++) {
      results.add(pool.submit(() -> {
        long id = Thread.currentThread().threadId();
        ready.countDown();
        go.await();
        long cpuStart = threadBean.getThreadCpuTime(id);
        long allocStart = threadBean.getThreadAllocatedBytes(id);
        for (int i = 0; i < LOGINS_PER_THREAD; i++) {
          realm.getAuthenticationInfo(token());
        }
        return new long[]{threadBean.getThreadCpuTime(id) - cpuStart,
          threadBean.getThreadAllocatedBytes(id) - allocStart};
      }));
    }

    ready.await();
    long wallStart = System.nanoTime();
    go.countDown();
    long cpuNanos = 0;
    long allocBytes = 0;
    for (Future<long[]> result : results) {
      long[] threadStats = result.get();
      cpuNanos += threadStats[0];
      allocBytes += threadStats[1];
    }
    long wallNanos = System.nanoTime() - wallStart;
    pool.shutdown();
    return new Stats(cpuNanos, allocBytes, wallNanos);
  }

  private static UsernamePasswordToken token() {
    return new UsernamePasswordToken("svc-automation-01", "not-the-real-password");
  }

  private record Stats(long cpuNanos, long allocatedBytes, long wallNanos)
  {
    double cpuMillis() {
      return cpuNanos / 1_000_000.0;
    }

    double allocatedMiB() {
      return allocatedBytes / (1024.0 * 1024.0);
    }

    double wallMillis() {
      return wallNanos / 1_000_000.0;
    }
  }
}
