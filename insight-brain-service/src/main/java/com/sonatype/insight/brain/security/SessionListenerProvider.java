/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.shiro.session.SessionListener;

/**
 * Translates a Sisu-managed list of session listeners into the type needed by Shiro.
 */
@Named
public class SessionListenerProvider
    implements Provider<Collection<SessionListener>>
{
  private final List<SessionListener> sessionListeners;

  @Inject
  public SessionListenerProvider(List<SessionListener> sessionListeners) {
    this.sessionListeners = sessionListeners;
  }

  @Override
  public Collection<SessionListener> get() {
    return sessionListeners;
  }
}
