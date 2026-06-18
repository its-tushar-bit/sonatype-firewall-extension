/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Flex,
  IconButton,
  Separator,
  Tooltip,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { nexusOneToClassicUrl } from 'MainRoot/nexus-one/nexusOneToClassicUrl';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { SearchOmnibar } from 'MainRoot/nosc/search/SearchOmnibar';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { useLeftNavCollapsed } from 'MainRoot/nosc/shell/useLeftNavCollapsed';
import PreviewSolutionSwitcher from 'MainRoot/nosc/shell/PreviewSolutionSwitcher';
import PreviewSystemPreferencesMenu from 'MainRoot/nosc/shell/PreviewSystemPreferencesMenu';
import PreviewUserMenu from 'MainRoot/nosc/shell/PreviewUserMenu';
import lifecycleLogoLight from 'MainRoot/productIcons/sonatype-lifecycle-logo-nav.svg';
import lifecycleLogoDark from 'MainRoot/productIcons/sonatype-lifecycle-logo-nav-dark.svg';

/** Lifecycle wordmark SVG viewBox is 212×31 (~6.84:1). The <img> element
 *  needs both width and height set explicitly when the source SVG has
 *  no intrinsic width — otherwise flex layout collapses the image to 0px
 *  wide. Computed: 28 * 212 / 31 ≈ 191. */
const LOGO_HEIGHT = 28;
const LOGO_WIDTH = Math.round(LOGO_HEIGHT * (212 / 31));

/**
 * Nexus One Preview top navigation bar.
 *
 * Layout (left → right):
 *   [hamburger] [Lifecycle logo] ............ [search] ............
 *     [Switch-to-Classic pill]
 *     [Help] [Bell] [Theme toggle] [Settings] [Solution Switcher]
 *     [Avatar]
 *
 * Mirrors the Sonatype Nexus Repository / Guide TopNav pattern:
 *   - Far-left hamburger collapses the LeftNav (state shared with the
 *     LeftNav's own chevron via the `useLeftNavCollapsed` hook).
 *   - Real Sonatype Lifecycle wordmark (light/dark variants) clickable
 *     to Platform Home.
 *   - Center search omnibar.
 *   - Right cluster of icon-only buttons with tooltips, plus the
 *     Switch-to-Classic action as the leftmost item of the cluster so
 *     it stays prominent without dominating the visual weight.
 *
 * Solution Switcher dropdown reuses the SOLUTIONS registry shared with
 * Platform Home — the same 5 tiles render in both surfaces.
 *
 * Height: 56px (UX-F2-007).
 */

function readHashPath(): string {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  return rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
}

export function TopNav(): JSX.Element {
  const [hashPath, setHashPath] = useState<string>(readHashPath());
  const { effectiveTheme, toggleTheme } = useNoscTheme();
  const [collapsed, setCollapsed] = useLeftNavCollapsed();

  useEffect(() => {
    const onHashChange = (): void => setHashPath(readHashPath());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  const classicHref = nexusOneToClassicUrl(hashPath) ?? '/dashboard/violations';
  const isDark = effectiveTheme === 'dark';
  const themeAriaLabel = isDark ? 'Switch to light mode' : 'Switch to dark mode';
  const collapseAriaLabel = collapsed ? 'Expand navigation' : 'Collapse navigation';

  const handleSwitchToClassic = (): void => {
    window.location.assign(bundleIndexUrl('classic', classicHref));
  };

  const handleLogoClick = (): void => {
    window.location.assign(bundleIndexUrl('nexus-one', '/home'));
  };

  return (
    <Box
      asChild
      style={{
        height: '56px',
        borderBottom: '1px solid var(--gray-a5)',
        backgroundColor: 'var(--color-panel-solid)',
        position: 'sticky',
        top: 0,
        zIndex: 10,
      }}
    >
      <header data-testid="nexus-one-top-nav">
        <Flex height="100%" align="center" px="3" gap="3">
          {/* ─── Far left: hamburger toggle for the LeftNav. ─── */}
          <Tooltip content={collapseAriaLabel} side="bottom" align="center">
            <IconButton
              variant="ghost"
              size="3"
              color="gray"
              aria-label={collapseAriaLabel}
              onClick={() => setCollapsed(!collapsed)}
              data-testid="nexus-one-top-nav-hamburger"
            >
              <ActionIcons.Menu size={20} />
            </IconButton>
          </Tooltip>

          {/* ─── Lifecycle wordmark, clickable, navigates to Platform Home. ─── */}
          <button
            type="button"
            onClick={handleLogoClick}
            aria-label="Sonatype Lifecycle — go to Platform Home"
            data-testid="nexus-one-top-nav-brand"
            style={{
              display: 'flex',
              alignItems: 'center',
              background: 'transparent',
              border: 'none',
              padding: '4px 6px',
              margin: '-4px -6px',
              borderRadius: 'var(--radius-2)',
              cursor: 'pointer',
              transition: 'background-color 120ms ease-out',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--gray-a3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
            }}
          >
            <img
              src={isDark ? lifecycleLogoDark : lifecycleLogoLight}
              alt="Sonatype Lifecycle"
              width={LOGO_WIDTH}
              height={LOGO_HEIGHT}
              style={{ display: 'block' }}
            />
          </button>

          {/* ─── Center: global search omnibar. ─── */}
          <Box flexGrow="1" data-testid="nexus-one-top-nav-search-slot">
            <Flex justify="center">
              <SearchOmnibar />
            </Flex>
          </Box>


          {/* ─── Right cluster ─── */}
          <Flex align="center" gap="2">
            {/* Switch to Classic — leftmost item of the cluster, kept
                prominent because Preview users hit unfinished pages and
                need the escape hatch one click away.
                Rendered as a labeled solid pill (not an icon-only button)
                so it stays legible in both light and dark modes — the
                previous icon-only treatment vanished against the dark
                panel and was caught by user feedback. */}
            <Tooltip
              content={`Switch to Classic UI (${classicHref})`}
              side="bottom"
              align="center"
            >
              <Button
                variant="solid"
                size="2"
                color="green"
                highContrast
                aria-label={`Switch to Classic UI (${classicHref})`}
                onClick={handleSwitchToClassic}
                data-testid="nexus-one-top-nav-classic-toggle"
              >
                <ActionIcons.Swap size={16} />
                Switch to Classic UI
              </Button>
            </Tooltip>

            {/* Help. Placeholder until the help system ships in IQ. */}
            <Tooltip content="Help" side="bottom" align="center">
              <IconButton
                variant="ghost"
                size="2"
                color="gray"
                aria-label="Help"
                data-testid="nexus-one-top-nav-help"
              >
                <ActionIcons.Help size={18} />
              </IconButton>
            </Tooltip>

            {/* Notifications bell. Placeholder. */}
            <Tooltip content="Notifications" side="bottom" align="center">
              <IconButton
                variant="ghost"
                size="2"
                color="gray"
                aria-label="Notifications"
                data-testid="nexus-one-top-nav-notifications"
              >
                <ActionIcons.Bell size={18} />
              </IconButton>
            </Tooltip>

            {/* Theme toggle (sun/moon). */}
            <Tooltip content={themeAriaLabel} side="bottom" align="center">
              <IconButton
                variant="ghost"
                size="2"
                color="gray"
                aria-label={themeAriaLabel}
                onClick={toggleTheme}
                data-testid="nexus-one-top-nav-theme-toggle"
              >
                {isDark ? <ActionIcons.Moon size={18} /> : <ActionIcons.Sun size={18} />}
              </IconButton>
            </Tooltip>

            {/* System Preferences (gear). Reuses Classic's
                SystemPreferencesMenu for the dropdown items + license
                gating; injects a "Nexus One UI" menu entry at the top
                that opens the existing #/previewUiSettings page. Same
                wrap-Classic trade-off Stage 2 made for the Solution
                Switcher (since Radix-rewritten in Stage 3-A). Follow-up
                Epic: PreviewSystemPreferencesMenu Radix rewrite. */}
            <PreviewSystemPreferencesMenu />

            {/* Solution Switcher — Radix-native dropdown that reads
                the same `state.solutionSwitcher` Redux slice Classic
                populates, so the user's actual licensed Sonatype
                solutions appear (not a hard-coded list). Keeps Nexus
                One in lockstep with Classic without re-fetching. */}
            <PreviewSolutionSwitcher />

            <Separator orientation="vertical" size="2" />

            {/* User account dropdown (current user + Log Out). */}
            <PreviewUserMenu />
          </Flex>
        </Flex>
      </header>
    </Box>
  );
}
