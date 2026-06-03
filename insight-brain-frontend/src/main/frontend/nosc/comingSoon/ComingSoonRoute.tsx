/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Box, Text } from '@radix-ui/themes';
import { ComingSoonPage } from 'MainRoot/nosc/comingSoon/ComingSoonPage';
import {
  COMING_SOON_MODULES,
  ComingSoonModuleSlug,
} from 'MainRoot/nosc/comingSoon/comingSoonModules';

/**
 * P1-F15: UI-Router-mountable wrapper around <ComingSoonPage>.
 *
 * The single registered component used by every Coming Soon route in
 * configuration/route.js. It reads the current Preview path slug out of
 * window.location.hash and looks up the matching module in the registry.
 *
 * Why this indirection instead of per-route component factories: UI-Router's
 * React adapter does not reliably accept anonymous components produced by
 * IIFEs in a loop body — the route gets registered but the component never
 * mounts. Sharing one named component that does its own slug-to-module
 * lookup is the recommended pattern and is what nexus-internal uses for the
 * equivalent placeholder hub there.
 *
 * Subscribes to `hashchange` so the displayed module updates when the user
 * navigates between two different Coming Soon URLs without a full reload.
 */

function readSlugFromHash(): string | null {
  if (typeof window === 'undefined') return null;
  const hash = window.location.hash || '';
  const match = hash.match(/^#?\/coming-soon\/([a-z0-9-]+)$/i);
  if (!match) return null;
  return match[1];
}

export function ComingSoonRoute(): JSX.Element {
  const [slug, setSlug] = useState<string | null>(readSlugFromHash());

  useEffect(() => {
    const onHashChange = (): void => setSlug(readSlugFromHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  // Defensive: if for any reason the slug isn't in the registry (route
  // mis-registration, race during navigation), show a neutral fallback
  // rather than crashing. Should never happen in production because every
  // registered route's URL came from comingSoonHref(slug).
  if (!slug || !(slug in COMING_SOON_MODULES)) {
    return (
      <Box p="6" data-testid="nosc-coming-soon-route-fallback">
        <Text size="2" color="gray">
          Loading Preview module…
        </Text>
      </Box>
    );
  }

  const mod = COMING_SOON_MODULES[slug as ComingSoonModuleSlug];
  return (
    <ComingSoonPage
      moduleName={mod.label}
      description={mod.description}
      classicHref={mod.classicHref}
    />
  );
}
