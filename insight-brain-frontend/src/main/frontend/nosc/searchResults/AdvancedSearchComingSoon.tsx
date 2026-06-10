/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Button, Card, Flex, Heading, Text, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useCurrentStateAndParams } from '@uirouter/react';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';

/**
 * /preview/search route component.
 *
 * Renders a Coming Soon panel for the Nexus One Advanced Search page.
 * The omnibar handles real typeahead today; the full Advanced Search
 * results page (with filter sidebar, sort, pagination across all entity
 * types) is deferred to Phase 1.5.
 *
 * Users who pressed Enter in the omnibar without selecting a row land
 * here. The escape hatch points to Classic's existing /advancedSearch
 * page so they can complete the workflow today.
 */
/**
 * Build the Classic Advanced Search URL via bundleIndexUrl so it honors the
 * deployment context path / MTIQ tenant prefix instead of assuming the app is
 * served from root. The omnibar query (if any) is carried through as `?q=` so
 * the user's search term isn't dropped when they continue into Classic.
 */
function classicAdvancedSearchHref(query: string): string {
  const path = query ? `/advancedSearch?q=${encodeURIComponent(query)}` : '/advancedSearch';
  return bundleIndexUrl('classic', path);
}

export function AdvancedSearchComingSoon(): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  const { params } = useCurrentStateAndParams();
  const query = typeof params?.q === 'string' ? params.q : '';
  const classicAdvancedSearchUrl = classicAdvancedSearchHref(query);

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
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
        data-testid="nosc-advanced-search-coming-soon"
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
        <Card size="4" style={{ maxWidth: '640px', width: '100%' }}>
          <Flex direction="column" gap="5" align="center" p="4">
            <Badge size="2" color="gray" variant="soft">
              Advanced Search
            </Badge>
            <Heading as="h1" size="6" align="center">
              Coming Soon
            </Heading>
            <Text size="3" color="gray" align="center">
              The full Advanced Search experience — filter sidebar,
              cross-entity tabs, and pagination — is being built natively
              in Nexus One. Use the search bar above for typeahead, or
              continue in Classic for the full results page.
            </Text>
            <Flex gap="3" mt="2" wrap="wrap" justify="center">
              <Button asChild size="3" variant="solid" color="green" highContrast>
                <a
                  href={classicAdvancedSearchUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="Open Advanced Search in Classic IQ in a new tab"
                  data-testid="nosc-advanced-search-classic-newtab"
                >
                  <Flex align="center" gap="2">
                    <ActionIcons.ExternalLink size={16} />
                    <span>Open in Classic (new tab)</span>
                  </Flex>
                </a>
              </Button>
              <Button asChild size="3" variant="soft" color="gray">
                <a
                  href={classicAdvancedSearchUrl}
                  aria-label="Continue to Advanced Search in Classic IQ in this tab"
                  data-testid="nosc-advanced-search-classic-samewindow"
                >
                  <Flex align="center" gap="2">
                    <ActionIcons.Swap size={16} />
                    <span>Continue in Classic</span>
                  </Flex>
                </a>
              </Button>
            </Flex>
          </Flex>
        </Card>
      </Box>
    </Theme>
  );
}
