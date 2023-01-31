/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import com.google.inject.Binder;
import com.google.inject.Key;
import org.eclipse.sisu.wire.LocatorWiring;
import org.eclipse.sisu.wire.WireModule.Strategy;
import org.eclipse.sisu.wire.Wiring;

public class IgnoreBannedImplementationStrategy
    implements Strategy
{
  private final BannedImplementationService banned;

  public IgnoreBannedImplementationStrategy(final BannedImplementationService banned) {
    this.banned = banned;
  }

  @Override
  public Wiring wiring(final Binder binder) {
    return new Wiring()
    {
      private final LocatorWiring locatorWiring = new LocatorWiring(binder);

      @Override
      public boolean wire(final Key<?> key) {
        if (banned.isBanned(key.getTypeLiteral().getRawType())) {
          return false;
        }
        return locatorWiring.wire(key);
      }
    };
  }
}
