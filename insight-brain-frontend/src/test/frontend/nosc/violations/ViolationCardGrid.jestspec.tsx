/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import ViolationCardGrid from 'MainRoot/nosc/violations/ViolationCardGrid';
import { ViolationRow } from 'MainRoot/nosc/violations/violationListTypes';

// A fixed past epoch (2023-11-14) rather than Date.now()-relative math, so the "first seen … ago"
// assertion stays clock-independent (CLAUDE.md flags time-dependent tests without a mocked clock).
const FIXED_FIRST_SEEN = 1_700_000_000_000;

const OPEN_CRITICAL: ViolationRow = {
  policyViolationId: 'pv-1',
  threatLevel: 10,
  severity: 'critical',
  policyName: 'Security - Critical',
  organizationName: 'Java-team',
  applicationName: 'Apple - Java',
  componentName: 'log4j-core',
  componentVersion: '2.14.0',
  stage: 'Build',
  state: 'OPEN',
  waivedWithAutoWaiver: false,
  firstOccurredTime: FIXED_FIRST_SEEN,
};

const WAIVED_AUTO: ViolationRow = {
  policyViolationId: 'pv-2',
  threatLevel: 3,
  severity: 'moderate',
  policyName: 'Quality - Standards',
  organizationName: 'Platform',
  applicationName: 'Cherry - Platform',
  componentName: 'busybox',
  componentVersion: '1.33',
  stage: 'Build',
  state: 'WAIVED',
  waivedWithAutoWaiver: true,
};

const WAIVED_MANUAL: ViolationRow = {
  policyViolationId: 'pv-3',
  threatLevel: 5,
  severity: 'severe',
  policyName: 'Security - High',
  componentName: 'jackson-databind',
  componentVersion: '2.9.9',
  state: 'WAIVED',
  waivedWithAutoWaiver: false,
};

// No top-level componentVersion — exercises the componentDisplay fallback to identifier coordinates.
const OPEN_COORDS_ONLY: ViolationRow = {
  policyViolationId: 'pv-4',
  threatLevel: 7,
  severity: 'severe',
  policyName: 'Security - High',
  componentName: 'guava',
  componentIdentifier: { format: 'maven', coordinates: { version: '30.1-jre' } },
  state: 'OPEN',
  waivedWithAutoWaiver: false,
};

function renderGrid(rows: ReadonlyArray<ViolationRow>) {
  return render(
    <Theme>
      <ViolationCardGrid violations={rows} />
    </Theme>,
  );
}

describe('ViolationCardGrid (CLM-42259)', () => {
  it('renders one card per violation row', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO]);
    expect(screen.getAllByTestId('violation-card')).toHaveLength(2);
  });

  it('shows component name with version, policy, org, app, and stage', () => {
    renderGrid([OPEN_CRITICAL]);
    const card = screen.getByTestId('violation-card');
    expect(within(card).getByText('log4j-core : 2.14.0')).toBeInTheDocument();
    expect(within(card).getByText('Security - Critical')).toBeInTheDocument();
    expect(within(card).getByText('Java-team')).toBeInTheDocument();
    expect(within(card).getByText('Apple - Java')).toBeInTheDocument();
    expect(within(card).getByText('Build')).toBeInTheDocument();
  });

  it('falls back to identifier coordinates for the version when componentVersion is absent', () => {
    renderGrid([OPEN_COORDS_ONLY]);
    const card = screen.getByTestId('violation-card');
    expect(within(card).getByText('guava : 30.1-jre')).toBeInTheDocument();
  });

  it('renders the numeric threat badge', () => {
    renderGrid([OPEN_CRITICAL]);
    const badge = screen.getByTestId('violation-threat-badge');
    expect(badge).toHaveTextContent('10');
  });

  it('keys the left border color to severity (critical differs from moderate)', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO]);
    const [critical, moderate] = screen.getAllByTestId('violation-card-link');
    // The left-border color is driven by data-threat-color (see ViolationCardGrid.scss): critical
    // (>=8) is red, moderate (2-3) is yellow — the two must differ by severity band.
    expect(critical).toHaveAttribute('data-threat-color', 'red');
    expect(moderate).toHaveAttribute('data-threat-color', 'yellow');
    expect(critical).toHaveClass('violation-card-link');
  });

  it('shows the state badge for open and waived rows', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO]);
    expect(screen.getByText('Open')).toBeInTheDocument();
    expect(screen.getByText('Waived')).toBeInTheDocument();
  });

  it('shows a generic "Waiver Applied" indicator on every waived card', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO, WAIVED_MANUAL]);
    // Both waived rows (auto + manual) carry the generic indicator; the open row does not.
    expect(screen.getAllByTestId('violation-card-waiver')).toHaveLength(2);
  });

  it('additionally tags auto-waived rows (auto-vs-manual refinement lands in CLM-42261)', () => {
    renderGrid([WAIVED_AUTO, WAIVED_MANUAL]);
    expect(screen.getAllByTestId('violation-card-auto-waiver')).toHaveLength(1);
  });

  it('does not show the auto-waiver badge on an OPEN row with a stale waivedWithAutoWaiver flag', () => {
    const openWithStaleAutoFlag: ViolationRow = {
      ...OPEN_CRITICAL,
      policyViolationId: 'pv-stale-auto',
      state: 'OPEN',
      waivedWithAutoWaiver: true,
    };
    renderGrid([openWithStaleAutoFlag]);
    expect(screen.queryByTestId('violation-card-auto-waiver')).not.toBeInTheDocument();
    expect(screen.queryByTestId('violation-card-waiver')).not.toBeInTheDocument();
    // The accessible name must stay consistent with the (absent) visual badges.
    expect(screen.getByTestId('violation-card-link')).not.toHaveAccessibleName(
      expect.stringContaining('auto-waived'),
    );
  });

  it('renders "first seen … ago" when a first-seen timestamp is present, and omits it otherwise', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_MANUAL]);
    const withTime = screen.getByTestId('violation-card-first-seen');
    expect(withTime).toHaveTextContent(/first seen .* ago/);
    // WAIVED_MANUAL has no firstOccurredTime, so only one card shows the line.
    expect(screen.getAllByTestId('violation-card-first-seen')).toHaveLength(1);
  });

  it('renders "first seen just now" for a sub-minute timestamp', () => {
    // Any sub-minute offset yields the same output, so this Date.now()-relative fixture is not
    // time-sensitive; it pins the formatTimeAgo "seconds ago" → "just now" contract the card depends on.
    const justNow: ViolationRow = { ...OPEN_CRITICAL, firstOccurredTime: Date.now() - 5_000 };
    renderGrid([justNow]);
    expect(screen.getByTestId('violation-card-first-seen')).toHaveTextContent('first seen just now');
  });

  it('links the whole card to the embedded detail route with an accessible label', () => {
    renderGrid([OPEN_CRITICAL]);
    const link = screen.getByTestId('violation-card-link');
    expect(link).toHaveAttribute('href', '#/violations/pv-1');
    // Accessible name leads with the state and references the policy, component, application, and
    // threat level so screen readers announce the same context/severity the card shows visually.
    expect(link).toHaveAccessibleName(
      'Open violation for Security - Critical on log4j-core : 2.14.0 in Apple - Java, threat level 10',
    );
  });
});
