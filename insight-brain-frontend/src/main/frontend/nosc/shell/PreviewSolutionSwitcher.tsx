/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import {
  Box,
  DropdownMenu,
  Flex,
  IconButton,
  Skeleton,
  Text,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
// Reuse the upstream hook that Classic's <SolutionSwitcher> uses
// internally to get the same partitioning + label/icon mapping logic.
// Returns { mySonatypeSolutions, exploreSolutions } where:
//   - mySonatypeSolutions = the user's licensed solutions with names +
//     icons looked up from SolutionNameMap / SolutionLogoMap
//   - exploreSolutions = the default list MINUS licensed MINUS Developer
//     (no marketing link), pointing at sonatype.com marketing pages
// Same business rules as Classic; one source of truth.
import useSolutionSwitcher from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/useSolutionSwitcher';
import type { SolutionListItemData } from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/types';
import {
  LicensedSolution,
  selectLicensedSolutions,
  selectSolutionSwitcherError,
  selectSolutionSwitcherLoading,
} from 'MainRoot/nosc/shell/previewSolutionSwitcherSelectors';
import ShellDropdownRoot from 'MainRoot/nosc/shell/ShellDropdownRoot';

// Product icons — reuse the SAME SVG assets the Classic
// SolutionSwitcher uses. Light/dark variants picked at render time
// by useNoscTheme(). Flat string-literal imports so esbuild can
// statically bundle each SVG.
import developerLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/developer-color-icon.svg';
import developerDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/developer-color-reversed-icon.svg';
import lifecycleLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/lifecycle-color-icon.svg';
import lifecycleDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/lifecycle-color-reversed-icon.svg';
import repoLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/repository-color-icon.svg';
import repoDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/repository-color-reversed-icon.svg';
import firewallLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/firewall-color-icon.svg';
import firewallDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/firewall-color-reversed-icon.svg';
import sbomLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/sbom-color-icon.svg';
import sbomDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/sbom-color-reversed-icon.svg';
import guideLight from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/light/guide-color-icon.svg';
import guideDark from '@sonatype/solution-switcher-react-component/components/SolutionSwitcher/images/dark/guide-color-reversed-icon.svg';

const ICON_MAP: Record<LicensedSolution['id'], { light: string; dark: string }> = {
  developer: { light: developerLight, dark: developerDark },
  lifecycle: { light: lifecycleLight, dark: lifecycleDark },
  repo: { light: repoLight, dark: repoDark },
  firewall: { light: firewallLight, dark: firewallDark },
  sbom: { light: sbomLight, dark: sbomDark },
  guide: { light: guideLight, dark: guideDark },
};

const MENU_WIDTH = 320;
const ICON_PX = 24;
const SKELETON_ROW_COUNT = 5;

function SolutionMenuRow({
  solution,
  theme,
}: {
  solution: SolutionListItemData;
  theme: 'light' | 'dark';
}): JSX.Element {
  const iconSrc = ICON_MAP[solution.id]?.[theme];
  // Pick the first instance's url if the solution has multiple instances
  // (multi-instance customers), else use the solution's own url. Empty
  // url is acceptable for solutions without a marketing/landing link.
  const href = solution.url ?? solution.instances?.[0]?.url ?? '#';
  return (
    <DropdownMenu.Item asChild>
      <a
        href={href}
        data-testid={`nosc-solution-switcher-item-${solution.id}`}
        style={{ textDecoration: 'none', color: 'inherit' }}
      >
        <Flex align="center" gap="3" width="100%">
          {iconSrc ? (
            <img
              src={iconSrc}
              width={ICON_PX}
              height={ICON_PX}
              alt=""
              aria-hidden="true"
            />
          ) : (
            <Box width={`${ICON_PX}px`} height={`${ICON_PX}px`} />
          )}
          <Text size="2">{solution.name}</Text>
        </Flex>
      </a>
    </DropdownMenu.Item>
  );
}

function SkeletonRows(): JSX.Element {
  return (
    <>
      {Array.from({ length: SKELETON_ROW_COUNT }).map((_, i) => (
        <Box
          key={i}
          px="3"
          py="2"
          data-testid="nosc-solution-switcher-skeleton-row"
        >
          <Flex align="center" gap="3">
            <Skeleton width={`${ICON_PX}px`} height={`${ICON_PX}px`} />
            <Skeleton width="120px" height="14px" />
          </Flex>
        </Box>
      ))}
    </>
  );
}

/**
 * Radix-native replacement for the Classic
 * @sonatype/solution-switcher-react-component dropdown, rendered in
 * the Preview TopNav. Consumes the SAME `state.solutionSwitcher`
 * Redux slice the Classic SolutionSwitcherContainer dispatches into,
 * so the two surfaces stay in lockstep without a parallel fetch.
 *
 * UX contract documented in:
 *   docs/superpowers/specs/2026-05-16-phase-1.5-nexus-one-preview-reskin-design.md §3.2
 */
export default function PreviewSolutionSwitcher(): JSX.Element {
  const solutions = useSelector(selectLicensedSolutions);
  const loading = useSelector(selectSolutionSwitcherLoading);
  const error = useSelector(selectSolutionSwitcherError);
  const { effectiveTheme: theme } = useNoscTheme();
  // Apply Classic's exact partitioning rules: licensed → My Solutions,
  // default-list minus licensed minus Developer → Explore.
  const { mySonatypeSolutions, exploreSolutions } = useSolutionSwitcher({
    licensedSolutions: solutions,
  });

  return (
    <Flex align="center" data-testid="nexus-one-top-nav-solution-switcher">
      <ShellDropdownRoot>
        <DropdownMenu.Trigger>
          <IconButton
            variant="ghost"
            size="2"
            color="gray"
            aria-label="Solution switcher"
          >
            <ActionIcons.SolutionSwitcher size={18} />
          </IconButton>
        </DropdownMenu.Trigger>

        <DropdownMenu.Content
          align="end"
          sideOffset={6}
          style={{ width: MENU_WIDTH }}
        >
          {loading ? (
            <SkeletonRows />
          ) : error ? (
            <Box px="3" py="2">
              <Text size="2" color="red" data-testid="nosc-solution-switcher-error">
                {error}
              </Text>
            </Box>
          ) : (
            <>
              {mySonatypeSolutions.length > 0 && (
                <DropdownMenu.Label>MY SONATYPE SOLUTIONS</DropdownMenu.Label>
              )}
              {mySonatypeSolutions.map((solution) => (
                <SolutionMenuRow key={solution.id} solution={solution} theme={theme} />
              ))}
              {exploreSolutions.length > 0 && (
                <>
                  {mySonatypeSolutions.length > 0 && <DropdownMenu.Separator />}
                  <DropdownMenu.Label>EXPLORE</DropdownMenu.Label>
                  {exploreSolutions.map((solution) => (
                    <SolutionMenuRow key={solution.id} solution={solution} theme={theme} />
                  ))}
                </>
              )}
              {mySonatypeSolutions.length === 0 && exploreSolutions.length === 0 && (
                <Box px="3" py="2">
                  <Text size="2" color="gray">
                    No solutions available
                  </Text>
                </Box>
              )}
            </>
          )}
        </DropdownMenu.Content>
      </ShellDropdownRoot>
    </Flex>
  );
}
