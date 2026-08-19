/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { RefObject } from 'react';
import { Flex, IconButton, Avatar, Tooltip } from '@radix-ui/themes';
import { Menu, LogOut } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';
import { SearchWithSuggestions } from '@guide/ui-core';
import { GuideLogo } from './GuideLogo';
import { ThemeToggle } from './ThemeToggle';
import { ProductSwitcher } from './ProductSwitcher/ProductSwitcher';
import { useAuth } from '../auth/AuthProvider';
import { searchAll } from '../api/searchBackend';
import type { GlobalSearchFilters, GlobalSearchOptions } from '@guide/ui-core/types';
import styles from './TopNavigation.module.css';
import { useFeatureFlags } from '../feature-flags/FeatureFlagProvider';
import { FEATURE_FLAGS } from '../feature-flags/featureFlags';

interface TopNavigationProps {
  onSidebarToggle: () => void;
  sidebarToggleRef?: RefObject<HTMLButtonElement | null>;
}

function suggestionSearch(
  query: string,
  filters?: GlobalSearchFilters,
  options?: GlobalSearchOptions
) {
  // SearchWithSuggestions still uses the legacy { query, filters, options } shape
  // (defined in @guide/ui-core). Convert to URLSearchParams so we can call the
  // wired GET /api/v2/guide/global/search endpoint. Keys mirror the SearchPage
  // filter overrides — byEcosystem -> formats, byLastUpdated -> publishedWindow.
  const params = new URLSearchParams();
  if (query) params.set('query', query);
  if (filters?.formats) {
    const values = Array.isArray(filters.formats) ? filters.formats : [filters.formats];
    for (const v of values) params.append('formats', v);
  }
  if (filters?.lastUpdated) params.set('publishedWindow', filters.lastUpdated);
  if (filters?.latestStable) params.set('latestStable', filters.latestStable);
  if (options?.offset !== undefined) params.set('offset', String(options.offset));
  if (options?.limit !== undefined) params.set('limit', String(options.limit));
  if (options?.sortField) params.set('sortField', options.sortField);
  if (options?.sortOrder) params.set('sortOrder', options.sortOrder);
  return searchAll(params);
}

function getInitials(displayName?: string, username?: string): string {
  const name = displayName || username || '';
  if (!name) return '?';

  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

export function TopNavigation({ onSidebarToggle, sidebarToggleRef }: TopNavigationProps) {
  const { user, logout } = useAuth();
  const { isLoading, isFeatureEnabled } = useFeatureFlags();
  const showSearch = !isLoading && isFeatureEnabled(FEATURE_FLAGS.AI_DEVELOPER);

  return (
    <div className={styles.root}>
      <div className={styles.logo}>
        <IconButton
          ref={sidebarToggleRef}
          variant="outline"
          color="gray"
          size={tokens.sizes.caption}
          aria-label="Toggle sidebar"
          onClick={onSidebarToggle}
        >
          <Menu size={20} />
        </IconButton>

        <GuideLogo />
      </div>

      {showSearch && (
        <div className={styles.search}>
          <div className={styles.searchInner}>
            <SearchWithSuggestions
              placeholder="Search components and vulnerabilities..."
              size="2"
              searchFunction={suggestionSearch}
              formAction="/search"
            />
          </div>
        </div>
      )}

      <div className={styles.actions}>
        <Flex align="center" gap={tokens.space.item} justify="end">
          <ThemeToggle />
          <ProductSwitcher />
          <Avatar
            size={tokens.sizes.caption}
            radius="full"
            color="gray"
            fallback={getInitials(user?.displayName, user?.username)}
          />
          <Tooltip content="Log out" side="bottom">
            <IconButton
              variant="outline"
              size={tokens.sizes.caption}
              color="gray"
              aria-label="Log out"
              onClick={() => { void logout(); }}
            >
              <LogOut size={16} />
            </IconButton>
          </Tooltip>
        </Flex>
      </div>
    </div>
  );
}
