/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Card, Flex, Heading, Link, Text, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';

import '@radix-ui/themes/styles.css';

/**
 * P1-F15: generic "Coming Soon" placeholder for Nexus One Preview modules
 * that haven't been built natively yet.
 *
 * Rendered for every entry in `comingSoonModules.ts` until the corresponding
 * native Nexus One module ships. Visual contract:
 *
 *   - "Coming Soon" is the visual hero (size 6 / h1) — confident product
 *     framing, not an error.
 *   - The module name is secondary (a small label above the hero) so the
 *     user knows which module they tapped without it competing with the
 *     hero.
 *   - One paragraph describes what the module will do.
 *   - TWO Classic-IQ escape hatches:
 *       1. Primary: "Open in Classic (new tab)" — keeps Nexus One state
 *          intact in the user's current tab. Most users will prefer this.
 *       2. Secondary: "Continue in Classic" — navigates the current tab
 *          to Classic. For users who'd rather just leave Nexus One.
 *   - Subtle hourglass icon at the top — calm decoration, not the focal
 *     point.
 *
 * No backend dependency, no telemetry call, no Redux state. Pure
 * presentation. Typography sizes/weights/colors follow the Sonatype design
 * system typography skill: h1 size 6 for hero; size 2 body text; size 1
 * gray for secondary metadata.
 */

export interface ComingSoonPageProps {
  /** Module display name, e.g. "Reports". Shown as the eyebrow label
   *  above the "Coming Soon" hero. */
  readonly moduleName: string;

  /** One-sentence description of the module's eventual purpose. */
  readonly description: string;

  /** Full Classic IQ deep link (must already include `/assets/#`). */
  readonly classicHref: string;
}

export function ComingSoonPage({
  moduleName,
  description,
  classicHref,
}: ComingSoonPageProps): JSX.Element {
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
        // Render the Coming Soon page as a fixed-position overlay below
        // the TopNav and right of the LeftNav so it sits in the visible
        // viewport regardless of how Classic IQ's <UIView/> positions us
        // in the DOM tree. Without this wrapper UI-Router's React
        // adapter mounts us inside `#iq-footer-container` (which has its
        // own positioning), pushing the entire card ~816px down and out
        // of the viewport. Offsets come from `usePreviewShellOffsets`
        // so they react to LeftNav collapse — same pattern used by
        // every other Preview page.
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <Box
        data-testid="nosc-coming-soon-page"
        data-module-name={moduleName}
        style={{
          height: '100%',
          minHeight: '320px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '32px 16px',
          boxSizing: 'border-box',
        }}
      >
      <Card
        size="4"
        style={{
          maxWidth: '640px',
          width: '100%',
        }}
      >
        <Flex direction="column" gap="5" align="center" p="4">
          {/* Eyebrow: the module name as a small label above the hero so the
              user has unambiguous orientation ("oh, this is the Policies
              page") without the module name competing with the hero. */}
          <Badge
            size="2"
            color="gray"
            variant="soft"
            data-testid="nosc-coming-soon-eyebrow"
          >
            {moduleName}
          </Badge>

          {/* Hero — the actual headline the user reads. h1 size 6 per the
              Sonatype design system typography rules (one h1 per page,
              size 6 only). */}
          <Heading
            as="h1"
            size="6"
            align="center"
            style={{ marginBottom: 0 }}
          >
            Coming Soon
          </Heading>

          {/* Lead paragraph: what the module will do in Nexus One. size 2
              body text per typography skill. */}
          <Text
            size="2"
            align="center"
            style={{
              maxWidth: '440px',
              lineHeight: 1.5,
              color: 'var(--gray-12)',
            }}
          >
            {description}
          </Text>

          {/* Help text: gray, size 1, sets up the actions below. */}
          <Text
            size="1"
            align="center"
            color="gray"
            style={{ maxWidth: '440px', lineHeight: 1.5 }}
          >
            We&apos;re still building the Nexus One version of {moduleName}. In
            the meantime, do everything you need to in Classic IQ.
          </Text>

          {/* Action row — primary new-tab open is on the left because most
              users will want to preserve their Nexus One state. The
              "Continue in Classic" secondary navigates this tab. */}
          <Flex
            align="center"
            gap="3"
            mt="3"
            wrap="wrap"
            justify="center"
          >
            <Button
              asChild
              size="3"
              variant="solid"
              color="green"
              data-testid="nosc-coming-soon-classic-newtab-button"
            >
              <a
                href={classicHref}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`Open ${moduleName} in Classic IQ in a new tab`}
              >
                <Flex align="center" gap="2">
                  <ActionIcons.ExternalLink size={16} />
                  <span>Open in Classic (new tab)</span>
                </Flex>
              </a>
            </Button>

            <Button
              asChild
              size="3"
              variant="soft"
              color="gray"
              data-testid="nosc-coming-soon-classic-samewindow-button"
            >
              <a
                href={classicHref}
                aria-label={`Continue to ${moduleName} in Classic IQ in this tab`}
              >
                <Flex align="center" gap="2">
                  <ActionIcons.Swap size={16} />
                  <span>Continue in Classic</span>
                </Flex>
              </a>
            </Button>
          </Flex>

          {/* Footer: back-to-Home secondary link, very subtle. */}
          <Box mt="2">
            <Link
              href="#/home"
              size="1"
              color="gray"
              data-testid="nosc-coming-soon-back-home-link"
              aria-label="Back to Nexus One Home"
            >
              ← Back to Home
            </Link>
          </Box>
        </Flex>
      </Card>
      </Box>
    </Theme>
  );
}
