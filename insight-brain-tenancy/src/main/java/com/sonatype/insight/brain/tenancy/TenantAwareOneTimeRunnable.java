/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Tags;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.cloneTenant;

public class TenantAwareOneTimeRunnable
    implements Runnable, HasTags
{
  /**
   * Thread-local flag to skip Subject propagation. Set by executors (like ResettingThreadPoolExecutor)
   * that intentionally clear the Shiro context and want tasks to run as "system" without a user identity.
   */
  private static final ThreadLocal<Boolean> SKIP_SUBJECT_PROPAGATION = new ThreadLocal<>();

  /**
   * Called by executors in beforeExecute() to signal that Subject should not be propagated to tasks.
   * This allows async tasks to run as "system" instead of inheriting the submitting user's identity.
   */
  public static void setSkipSubjectPropagation(boolean skip) {
    if (skip) {
      SKIP_SUBJECT_PROPAGATION.set(Boolean.TRUE);
    }
    else {
      SKIP_SUBJECT_PROPAGATION.remove();
    }
  }

  /**
   * Returns true if Subject propagation should be skipped for the current thread.
   */
  public static boolean isSkipSubjectPropagation() {
    return Boolean.TRUE.equals(SKIP_SUBJECT_PROPAGATION.get());
  }

  private final Runnable wrapped;

  private final Tenant tenant;

  /*
    In Shiro 2.0.4+ a change removed the InheritableThreadLocal approach from ThreadContext
    (it was causing nasty classloader/thread-local cleanup problems in app servers when apps spawned threads)
  */
  private final Subject subject;

  private boolean previouslyRun = false;

  public TenantAwareOneTimeRunnable(Runnable wrapped) {
    this(wrapped, TenantThreadLocal.getTenant());
  }

  TenantAwareOneTimeRunnable(Runnable wrapped, Tenant tenant) {
    this.wrapped = wrapped;
    this.tenant = cloneTenant(tenant);
    this.subject = ThreadContext.getSecurityManager() != null ? SecurityUtils.getSubject() : null;
  }

  @Override
  public void run() {
    if (previouslyRun) {
      /*
        This is to fail fast. The request will fail when the wrapped runnable is called and gets the tenant anyway but
        by failing fast we get a better stack trace, making it easier to find and resolve the problem.
       */
      throw new RuntimeException("TenantAwareOneTimeRunnable cannot be reused");
    }

    previouslyRun = true;

    // Check if the executor signaled that Subject propagation should be skipped.
    // This is set by executors like ResettingThreadPoolExecutor that want tasks to run as "system".
    final boolean skipSubject = Boolean.TRUE.equals(SKIP_SUBJECT_PROPAGATION.get());

    TenantThreadLocal.runAsWithoutValidation(tenant, (Supplier<Void>) () -> {
      try {
        if (subject != null && !skipSubject) {
          subject.associateWith(wrapped).run();
        }
        else {
          wrapped.run();
        }

        return null;
      }
      finally {
        TenantThreadLocal.invalidateTenant();
      }
    });
  }

  @Override
  public Tags getTags() {
    if (wrapped instanceof HasTags hasTags) {
      return hasTags.getTags();
    }
    return Tags.empty();
  }
}
