/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Heading, Link, Text, Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';

import '@radix-ui/themes/styles.css';

/**
 * P1-F15: generic "Coming Soon" placeholder for Nexus One modules that have
 * not been built natively yet.
 *
 * Shell contract: users stay in NOUX. TopNav owns the only Classic shell
 * toggle — this page must not deep-link out of the nexus-one bundle.
 *
 * Visual contract:
 *   - "Coming Soon" is the visual hero (size 6 / h1).
 *   - The module name is a small eyebrow label above the hero.
 *   - One paragraph describes what the module will do.
 *   - Back to Home is the only action.
 */

export interface ComingSoonPageProps {
  /** Module display name, e.g. "Enterprise Reporting". */
  readonly moduleName: string;

  /** One-sentence description of the module's eventual purpose. */
  readonly description: string;
}

export function ComingSoonPage({
  moduleName,
  description,
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
          <Badge
            size="2"
            color="gray"
            variant="soft"
            data-testid="nosc-coming-soon-eyebrow"
          >
            {moduleName}
          </Badge>

          <Heading
            as="h1"
            size="6"
            align="center"
            style={{ marginBottom: 0 }}
          >
            Coming Soon
          </Heading>

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

          <Text
            size="1"
            align="center"
            color="gray"
            style={{ maxWidth: '440px', lineHeight: 1.5 }}
          >
            We&apos;re still building {moduleName} for Nexus One.
          </Text>

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
