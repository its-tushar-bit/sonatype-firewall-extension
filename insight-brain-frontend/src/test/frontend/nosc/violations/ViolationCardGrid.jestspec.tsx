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
  threatCategory: 'security',
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
  threatCategory: 'quality',
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
  threatCategory: 'security',
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
  threatCategory: 'security',
  policyName: 'Security - High',
  componentName: 'guava',
  componentIdentifier: { format: 'maven', coordinates: { version: '30.1-jre' } },
  state: 'OPEN',
  waivedWithAutoWaiver: false,
};

function renderGrid(
  rows: ReadonlyArray<ViolationRow>,
  overrides: Partial<React.ComponentProps<typeof ViolationCardGrid>> = {},
) {
  return render(
    <Theme>
      <ViolationCardGrid violations={rows} {...overrides} />
    </Theme>,
  );
}

describe('ViolationCardGrid (CLM-42259)', () => {
  it('renders one card per violation row', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO]);
    expect(screen.getAllByTestId('violation-card')).toHaveLength(2);
  });

  it('shows component name with version, type+severity policy, org, app, and stage', () => {
    renderGrid([OPEN_CRITICAL]);
    const card = screen.getByTestId('violation-card');
    expect(within(card).getByText('log4j-core : 2.14.0')).toBeInTheDocument();
    expect(within(card).getByTestId('violation-card-policy')).toHaveTextContent('Security-Critical');
    expect(within(card).queryByText('Security - Critical')).not.toBeInTheDocument();
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

  it('shows the manual "Waiver Applied" indicator only on manually-waived cards (CLM-42261)', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_AUTO, WAIVED_MANUAL]);
    // The manual and auto pills are mutually exclusive: only the manually-waived row carries the
    // standard "Waiver Applied" pill; the auto-waived row gets the distinct auto pill, the open none.
    const waiverBadges = screen.getAllByTestId('violation-card-waiver');
    expect(waiverBadges).toHaveLength(1);
    expect(waiverBadges[0]).toHaveTextContent('Waiver Applied');
  });

  it('shows a distinct "Auto-waived" tag on auto-waived cards instead of "Waiver Applied" (CLM-42261)', () => {
    renderGrid([WAIVED_AUTO, WAIVED_MANUAL]);
    const [autoCard, manualCard] = screen.getAllByTestId('violation-card');
    // Auto-waived card: the distinct solid auto pill, and NOT the manual "Waiver Applied" indicator.
    expect(within(autoCard).getByTestId('violation-card-auto-waiver')).toHaveTextContent('Auto-waived');
    expect(within(autoCard).queryByTestId('violation-card-waiver')).not.toBeInTheDocument();
    // Manually-waived card: the standard "Waiver Applied" pill, and NOT the auto pill.
    expect(within(manualCard).getByTestId('violation-card-waiver')).toHaveTextContent('Waiver Applied');
    expect(within(manualCard).queryByTestId('violation-card-auto-waiver')).not.toBeInTheDocument();
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

  it('renders absolute + relative first seen when a timestamp is present, and omits it otherwise', () => {
    renderGrid([OPEN_CRITICAL, WAIVED_MANUAL]);
    const withTime = screen.getByTestId('violation-card-first-seen');
    const absolute = new Intl.DateTimeFormat(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(new Date(FIXED_FIRST_SEEN));
    expect(withTime).toHaveTextContent(`first seen ${absolute} ·`);
    expect(withTime).toHaveTextContent(/ago$/);
    // WAIVED_MANUAL has no firstOccurredTime, so only one card shows the line.
    expect(screen.getAllByTestId('violation-card-first-seen')).toHaveLength(1);
  });

  it('renders absolute + "just now" for a sub-minute timestamp', () => {
    // Any sub-minute offset yields the same relative output, so this Date.now()-relative fixture is not
    // time-sensitive; it pins the formatTimeAgo "seconds ago" → "just now" contract the card depends on.
    const justNowMs = Date.now() - 5_000;
    const justNow: ViolationRow = { ...OPEN_CRITICAL, firstOccurredTime: justNowMs };
    renderGrid([justNow]);
    const absolute = new Intl.DateTimeFormat(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(new Date(justNowMs));
    expect(screen.getByTestId('violation-card-first-seen')).toHaveTextContent(
      `first seen ${absolute} · just now`,
    );
  });

  it('links the whole card to the embedded detail route with an accessible label', () => {
    renderGrid([OPEN_CRITICAL]);
    const link = screen.getByTestId('violation-card-link');
    expect(link).toHaveAttribute('href', '#/violations/pv-1');
    // Accessible name leads with the state and references the policy, component, application, and
    // threat level so screen readers announce the same context/severity the card shows visually.
    expect(link).toHaveAccessibleName(
      'Open violation for Security-Critical on log4j-core : 2.14.0 in Apple - Java, threat level 10',
    );
  });

  describe('Legal list props (CLM-43207)', () => {
    it('uses getCardHref when provided and hides state / waiver badges', () => {
      const getCardHref = jest.fn(() => '#/legal/component/abc123');
      renderGrid([OPEN_CRITICAL, WAIVED_AUTO], { getCardHref, hideStateBadges: true });

      expect(getCardHref).toHaveBeenCalled();
      const links = screen.getAllByTestId('violation-card-link');
      expect(links[0]).toHaveAttribute('href', '#/legal/component/abc123');
      expect(screen.queryByText('Open')).not.toBeInTheDocument();
      expect(screen.queryByText('Waived')).not.toBeInTheDocument();
      expect(screen.queryByTestId('violation-card-waiver')).not.toBeInTheDocument();
      expect(screen.queryByTestId('violation-card-auto-waiver')).not.toBeInTheDocument();
    });

    it('omits the OPEN/WAIVED prefix from the card aria-label when state badges are hidden', () => {
      renderGrid([OPEN_CRITICAL], { hideStateBadges: true });
      expect(screen.getByTestId('violation-card-link')).toHaveAccessibleName(
        'Security - Critical on log4j-core : 2.14.0 in Apple - Java, threat level 10',
      );
    });
  });
});
