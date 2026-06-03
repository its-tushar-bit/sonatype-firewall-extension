/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme, Card, Heading, Text, Flex, Box, Code } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';

import '@radix-ui/themes/styles.css';

/**
 * CLM-39545 / P1-F2 (integration MVP): friendly "this Preview page
 * hasn't been built yet" placeholder rendered in the page area when
 * the user is at a /preview/<route> URL that Classic's router cannot
 * resolve. Replaces Classic's red unrecoverable-error fallback for
 * preview routes only — Classic routes still get Classic's error.
 *
 * Each Phase-1 epic that ships a real Preview page replaces this for
 * its specific route. The placeholder is the default for routes that
 * haven't shipped yet.
 */
const ROUTE_TO_EPIC: Record<string, string> = {
  '/dashboard': 'P1-F6 (CLM-39641) — Dashboard',
  '/applications': 'P1-F7 (CLM-39709) — Applications',
  '/search': 'P1-F5 (CLM-39549) — Global Search',
  '/ui-settings': 'P1-F4 (CLM-39606) — Settings + Preview Toggle',
  '/home': 'P1-F14 (CLM-39608) — Platform Home',
};

function epicForRoute(route: string): string {
  if (ROUTE_TO_EPIC[route]) return ROUTE_TO_EPIC[route];
  const comingSoon = route.match(/^\/coming-soon\/([^/]+)/)?.[1];
  if (comingSoon) {
    return `P1-F15 — Coming Soon (${comingSoon})`;
  }
  const seg = route.match(/^\/([^/]+)/)?.[1];
  if (seg) {
    const fuzzy = `/${seg}`;
    if (ROUTE_TO_EPIC[fuzzy]) return ROUTE_TO_EPIC[fuzzy];
  }
  return 'a Phase-1 epic — see docs/superpowers/CLM-39545/INITIATIVE.md §3';
}

export interface PreviewPagePlaceholderProps {
  /** Current preview route, e.g. '/preview/dashboard'. */
  route: string;
}

export function PreviewPagePlaceholder({ route }: PreviewPagePlaceholderProps) {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  return (
    <Theme appearance={effectiveTheme} accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      <Box
        style={{
          position: 'fixed',
          ...offsets,
          right: 0,
          bottom: 0,
          padding: '32px',
          overflowY: 'auto',
          backgroundColor: 'var(--gray-1)',
          zIndex: 8,
        }}
        data-testid="nexus-one-preview-page-placeholder"
      >
        <Flex justify="center">
          <Card style={{ maxWidth: '560px', width: '100%' }}>
            <Flex direction="column" gap="3" p="4">
              <Heading size="5">Preview page not built yet</Heading>
              <Text size="3" color="gray">
                This Preview route is reserved for a Phase-1 epic that hasn't shipped yet.
              </Text>
              <Box
                style={{
                  borderLeft: '3px solid var(--accent-9)',
                  paddingLeft: '12px',
                  marginTop: '8px',
                }}
              >
                <Text as="div" size="2" color="gray">
                  Route: <Code>{route}</Code>
                </Text>
                <Text as="div" size="2" color="gray" mt="1">
                  Owner: <strong>{epicForRoute(route)}</strong>
                </Text>
              </Box>
              <Text size="2" color="gray" mt="2">
                For now, you can navigate back to <Code>/assets/#/</Code> for the existing Classic UI (auto-routes to your tenant's default landing page).
              </Text>
            </Flex>
          </Card>
        </Flex>
      </Box>
    </Theme>
  );
}
