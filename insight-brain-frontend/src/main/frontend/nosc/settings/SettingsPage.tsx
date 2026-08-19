/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { Box, Flex, Text, TextField, Theme } from '@radix-ui/themes';
import { Card, PageHeading, SectionHeading, tokens } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { comingSoonStateName } from 'MainRoot/nosc/comingSoon';
import { SETTINGS_ITEM_SHOW_IF } from 'MainRoot/nosc/settings/settingsGating';
import { useSettingsGatingContext } from 'MainRoot/nosc/settings/useSettingsGatingContext';
import {
  SETTINGS_PAGE_ITEMS,
  SETTINGS_PAGE_SECTIONS,
  type SettingsPageItem,
} from 'MainRoot/nosc/settings/settingsPageItems';
import styles from 'MainRoot/nosc/settings/SettingsPage.module.css';

const SEARCH_LABEL = 'Search settings';

function matchesQuery(item: SettingsPageItem, query: string): boolean {
  if (!query) return true;
  return `${item.label} ${item.description}`.toLowerCase().includes(query);
}

/**
 * A single settings row, rendered as a native anchor so it is keyboard-operable
 * and announced as a link for free (no manual role/tabIndex/key handling). Its
 * `href` targets either the item's embedded in-shell state or the generic
 * `/coming-soon/settings` placeholder, as resolved by the caller.
 */
function SettingsRow({
  item,
  href,
}: {
  readonly item: SettingsPageItem;
  readonly href: string;
}): JSX.Element {
  return (
    <a href={href} className={styles.settingsRow} aria-label={`${item.label}: ${item.description}`}>
      <Flex align="center" justify="between" gap={tokens.space.compact}>
        <Flex direction="column" gap={tokens.space.tight}>
          <Text as="div" {...tokens.typography.label} weight="bold">
            {item.label}
          </Text>
          <Text as="div" {...tokens.typography.description} style={{ lineHeight: 1.4 }}>
            {item.description}
          </Text>
        </Flex>
        <ActionIcons.ChevronRight size={tokens.icon.iconButtons} color="var(--gray-9)" aria-hidden />
      </Flex>
    </a>
  );
}

export default function SettingsPage(): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  const gatingContext = useSettingsGatingContext();
  const [search, setSearch] = useState('');

  // Rows whose admin page is already embedded in the shell link straight to that
  // in-shell state (item.stateName); every other row falls back to the generic
  // /coming-soon/settings placeholder until its native page is ported (CLM-42469).
  const { href: hrefFromStateName } = useRouterState();
  const placeholderHref = hrefFromStateName(comingSoonStateName('settings'));
  const rowHref = (item: SettingsPageItem): string =>
    item.stateName ? hrefFromStateName(item.stateName) : placeholderHref;

  const query = search.trim().toLowerCase();

  const visibleSections = SETTINGS_PAGE_SECTIONS.map((section) => ({
    ...section,
    items: SETTINGS_PAGE_ITEMS.filter(
      (item) =>
        item.section === section.id && matchesQuery(item, query) && SETTINGS_ITEM_SHOW_IF[item.id](gatingContext),
    ),
  })).filter((section) => section.items.length > 0);

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      hasBackground={false}
      data-testid="nosc-settings-page"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <Box px={tokens.space.page} py={tokens.space.page} style={{ maxWidth: '720px', margin: '0 auto' }}>
        <PageHeading as="h1">Settings</PageHeading>
        <Text as="p" {...tokens.typography.description} mb={tokens.space.item}>
          Manage your profile, tokens, and administrative settings.
        </Text>

        <TextField.Root
          aria-label={SEARCH_LABEL}
          placeholder={SEARCH_LABEL}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size={tokens.containerSizes.settings}
          mb={tokens.space.section}
        >
          <TextField.Slot>
            <ActionIcons.Search size={tokens.icon.buttons} />
          </TextField.Slot>
        </TextField.Root>

        {visibleSections.length === 0 ? (
          <Card size={tokens.containerSizes.settings}>
            <Text as="div" {...tokens.typography.description} align="center" style={{ padding: 'var(--space-4)' }}>
              No settings match “{search.trim()}”. Try another search term.
            </Text>
          </Card>
        ) : (
          <Flex direction="column" gap={tokens.space.section}>
            {visibleSections.map((section) => (
              <Box key={section.id}>
                <SectionHeading as="h2">{section.label}</SectionHeading>
                <Card mt={tokens.space.compact} variant="surface" style={{ padding: 0, overflow: 'hidden' }}>
                  {section.items.map((item) => (
                    <SettingsRow key={item.id} item={item} href={rowHref(item)} />
                  ))}
                </Card>
              </Box>
            ))}
          </Flex>
        )}
      </Box>
    </Theme>
  );
}
