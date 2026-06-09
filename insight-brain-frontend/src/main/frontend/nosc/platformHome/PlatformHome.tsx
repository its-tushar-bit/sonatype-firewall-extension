/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Card, Flex, Grid, Heading, Text, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { SOLUTIONS, Solution } from 'MainRoot/nosc/platformHome/solutions';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';

import '@radix-ui/themes/styles.css';

/**
 * P1-F14 / CLM-39608. The Nexus One Platform Home: a 5-tile grid of
 * Sonatype solutions rendered as the entry point of the Preview shell.
 *
 * Hard-aligned with the canonical Nexus One UX prototype at
 * apps/nexusone-ux-prototype/src/app/platform/page.tsx (Sonatype design
 * system reference). Visual contract:
 *
 *   - Page background = Radix theme `--color-page-background` (auto
 *     light/dark via the wrapping `<Theme appearance>`). NO explicit
 *     backgroundColor override.
 *   - Cards = bare `<Card onClick>` — no `asChild`, no transparent-
 *     background button hack. Radix's default Card surface adapts to
 *     the theme appearance, so dark-mode cards are dark with light text
 *     instead of white-with-dark-text-on-dark-bg (the previous bug).
 *   - All typography (Heading, Text) uses Radix defaults. Subtitles use
 *     `color="gray"` which Radix maps to `--gray-11` — readable on both
 *     theme appearances.
 *   - "Coming soon" / "External" badges render at top-right of the card
 *     via absolute positioning, identical to the prototype's
 *     tier-badge slot.
 *
 * IQ-specific divergences from the prototype:
 *   - Tier-badge logic NOT ported (IQ has no per-product tier system
 *     today; entitlements are at the product-license level).
 *   - 4 of 5 tiles navigate inside the IQ SPA via window.location;
 *     only `internal: false` tiles open in a new tab.
 *   - Solutions registry lives in `nosc/platformHome/solutions.ts`
 *     and is shared with the TopNav SolutionSwitcher source-of-truth.
 *
 * Reachable via:
 *   - LeftNav top entry "Home"
 *   - Sonatype logo click in the Preview TopNav
 *   - Direct URL /preview/home
 */
export function PlatformHome(): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      data-testid="nexus-one-page-surface"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        // Let Radix Theme paint its own background — it picks the right
        // page-background var for light vs dark automatically. The
        // previous explicit `var(--color-page-background)` on a child
        // Box left a lighter rectangle visible inside the dark page.
      }}
    >
      <Box p="6" data-testid="platform-home-page">
        <Flex direction="column" gap="6" style={{ maxWidth: '1280px', margin: '0 auto' }}>
          {/* Header — mirrors prototype exactly. */}
          <Box>
            <Heading as="h1" size="8" mb="2">
              Nexus One
            </Heading>
            <Text as="p" size="3" color="gray">
              Your unified Sonatype platform
            </Text>
          </Box>

          {/* Solutions Section — mirrors prototype exactly. */}
          <Box>
            <Heading as="h2" size="6" mb="4">
              Solutions
            </Heading>

            <Grid columns={{ initial: '1', sm: '2', md: '3', lg: '5' }} gap="4">
              {SOLUTIONS.map((solution) => (
                <SolutionTile key={solution.id} solution={solution} />
              ))}
            </Grid>
          </Box>
        </Flex>
      </Box>
    </Theme>
  );
}

interface SolutionTileProps {
  readonly solution: Solution;
}

/**
 * One product tile. Card-as-clickable-surface pattern from the prototype:
 *   - Whole card is clickable (cursor: pointer + onClick).
 *   - Inner Button stops propagation and re-fires the same handler so
 *     keyboard users get a focusable target with a clear label.
 *
 * Internal solutions ("Open" / "Preview (placeholder)") navigate via
 * window.location to stay inside the IQ SPA. External solutions
 * ("Visit") open in a new tab so the user keeps the IQ session intact.
 */
function SolutionTile({ solution }: SolutionTileProps): JSX.Element {
  const handleClick = (e?: React.MouseEvent): void => {
    if (e) e.stopPropagation();
    if (solution.internal) {
      window.location.assign(bundleIndexUrl('nexus-one', solution.href));
    } else {
      window.open(solution.href, '_blank', 'noopener,noreferrer');
    }
  };

  const buttonLabel =
    solution.internal && solution.inIQToday
      ? 'Open'
      : solution.internal
        ? 'Preview (placeholder)'
        : 'Visit';

  const cornerBadge = !solution.internal ? (
    <Badge color="gray" size="1" variant="soft">
      sonatype.com
    </Badge>
  ) : !solution.inIQToday ? (
    <Badge color="gray" size="1" variant="soft">
      Coming soon
    </Badge>
  ) : null;

  return (
    <Card
      data-testid={`platform-home-tile-${solution.id}`}
      data-external={String(!solution.internal)}
      style={{
        cursor: 'pointer',
        position: 'relative',
        height: '100%',
      }}
      onClick={handleClick}
    >
      {cornerBadge && (
        <Box style={{ position: 'absolute', top: 12, right: 12 }}>{cornerBadge}</Box>
      )}

      <Box p="4" style={{ height: '100%' }}>
        <Flex
          direction="column"
          gap="4"
          align="center"
          style={{ textAlign: 'center', height: '100%' }}
        >
          {/* Logo */}
          <Box
            style={{
              width: 64,
              height: 64,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <img src={solution.icon} alt={solution.name} width={64} height={64} />
          </Box>

          {/* Product Name + Description */}
          <Box style={{ flex: 1 }}>
            <Heading as="h3" size="4" mb="2">
              {solution.name.startsWith('Sonatype ') ? (
                <>
                  Sonatype
                  <br />
                  {solution.name.replace('Sonatype ', '')}
                </>
              ) : (
                solution.name
              )}
            </Heading>
            <Text as="p" size="2" color="gray" style={{ minHeight: 40 }}>
              {solution.description}
            </Text>
          </Box>

          {/* Action Button — mirrors prototype: solid for entitled,
              outline for external. The card itself is clickable, so
              this is mostly a clear visual affordance and a keyboard-
              focusable target with a labeled action. */}
          <Button
            variant={solution.internal ? 'solid' : 'outline'}
            size="2"
            style={{ width: '100%' }}
            onClick={handleClick}
            aria-label={`${buttonLabel} — ${solution.name}`}
          >
            {buttonLabel}
            {!solution.internal && <ActionIcons.ExternalLink size={14} />}
          </Button>
        </Flex>
      </Box>
    </Card>
  );
}
