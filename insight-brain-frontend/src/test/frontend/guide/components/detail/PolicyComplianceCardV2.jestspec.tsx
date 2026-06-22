/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ComponentProvider } from '@guide/ui-core';
import type { ComponentDetails } from '@guide/ui-core/types';
import { render, screen } from '../../test-utils';
import { mockComponentDetail } from 'TestRoot/guide/api/fixtures/componentDetailFixtures';
import { PolicyComplianceCardV2 } from 'GuideRoot/components/detail/PolicyComplianceCardV2';
import type { GuidePolicyCompliance } from 'GuideRoot/components/detail/policyComplianceTypes';

const activeCompliance: GuidePolicyCompliance = {
  compliant: true,
  complianceLevel: 'PASS',
  stage: 'release',
  ownerId: 'ROOT_ORGANIZATION_ID',
  summary: {
    highestThreatLevel: 7,
    worstAction: 'none',
    activeViolationCount: 2,
    waivedViolationCount: 0,
    violationCountsByCategory: { SECURITY: 0, LICENSE: 1, QUALITY: 1, OTHER: 0 },
  },
  violations: [
    {
      policyId: 'p-arch',
      policyName: 'Architecture-Quality',
      threatLevel: 1,
      actions: [],
      waived: false,
      constraintViolations: [
        {
          constraintId: 'c-old',
          constraintName: 'Version is old',
          reasons: [{ reason: 'Found component older than 5 years' }],
        },
      ],
    },
    {
      policyId: 'p-lic',
      policyName: 'License-Commercial',
      threatLevel: 7,
      actions: [],
      waived: false,
      constraintViolations: [
        {
          constraintId: 'c-comm',
          constraintName: 'License containing commercial terms detected',
          reasons: [
            { reason: "Found licenses in the 'Commercial' license threat group ('AcceleratXR-EULA')" },
          ],
        },
      ],
    },
  ],
};

const waivedCompliance: GuidePolicyCompliance = {
  compliant: true,
  complianceLevel: 'WARN',
  stage: 'release',
  ownerId: 'ROOT_ORGANIZATION_ID',
  summary: {
    highestThreatLevel: 10,
    worstAction: 'none',
    activeViolationCount: 0,
    waivedViolationCount: 1,
    violationCountsByCategory: { SECURITY: 1, LICENSE: 0, QUALITY: 0, OTHER: 0 },
  },
  violations: [
    {
      policyId: 'p-sec',
      policyName: 'Security-Critical',
      threatLevel: 10,
      actions: [],
      waived: true,
      waiver: {
        scopeOwnerType: 'organization',
        scopeOwnerId: 'ROOT_ORGANIZATION_ID',
        expiryTime: '2026-08-01T00:00:00Z',
        comment: 'Accepted risk',
      },
      constraintViolations: [
        {
          constraintId: 'c-cvss',
          constraintName: 'Critical risk CVSS score',
          reasons: [
            {
              reason: 'Found security vulnerability CVE-2021-44228 with severity >= 9 (severity = 10.0)',
              reference: { type: 'SECURITY_VULNERABILITY_REFID', value: 'CVE-2021-44228' },
            },
          ],
        },
      ],
    },
  ],
};

const emptyCompliance: GuidePolicyCompliance = {
  compliant: true,
  complianceLevel: 'PASS',
  stage: 'release',
  ownerId: 'ROOT_ORGANIZATION_ID',
  summary: {
    highestThreatLevel: 0,
    worstAction: 'none',
    activeViolationCount: 0,
    waivedViolationCount: 0,
    violationCountsByCategory: { SECURITY: 0, LICENSE: 0, QUALITY: 0, OTHER: 0 },
  },
  violations: [],
};

// One active + one waived violation, so `allWaived` (computed with `.every`) is false even though a
// violation is waived — guards against a regression to `.some` that would mislabel the heading.
const partiallyWaivedCompliance: GuidePolicyCompliance = {
  compliant: false,
  complianceLevel: 'FAIL',
  stage: 'release',
  ownerId: 'ROOT_ORGANIZATION_ID',
  violations: [
    {
      policyId: 'p-lic',
      policyName: 'License-Commercial',
      threatLevel: 7,
      actions: [],
      waived: false,
      constraintViolations: [
        {
          constraintId: 'c-comm',
          constraintName: 'License containing commercial terms detected',
          reasons: [{ reason: 'Found commercial license' }],
        },
      ],
    },
    {
      policyId: 'p-sec',
      policyName: 'Security-Critical',
      threatLevel: 10,
      actions: [],
      waived: true,
      waiver: {
        scopeOwnerType: 'organization',
        scopeOwnerId: 'ROOT_ORGANIZATION_ID',
        expiryTime: '2026-08-01T00:00:00Z',
        comment: 'Accepted risk',
      },
      constraintViolations: [
        {
          constraintId: 'c-cvss',
          constraintName: 'Critical risk CVSS score',
          reasons: [{ reason: 'Found security vulnerability CVE-2021-44228' }],
        },
      ],
    },
  ],
};

// A waived violation whose waiver detail is null — a valid payload since `waiver` is
// `GuideWaiverInfo | null | undefined`. The Waived badge must still render without a scope/expiry row.
const waivedNullWaiverCompliance: GuidePolicyCompliance = {
  compliant: true,
  complianceLevel: 'WARN',
  stage: 'release',
  ownerId: 'ROOT_ORGANIZATION_ID',
  violations: [
    {
      policyId: 'p-sec',
      policyName: 'Security-Critical',
      threatLevel: 10,
      actions: [],
      waived: true,
      waiver: null,
      constraintViolations: [
        {
          constraintId: 'c-cvss',
          constraintName: 'Critical risk CVSS score',
          reasons: [{ reason: 'Found security vulnerability CVE-2021-44228' }],
        },
      ],
    },
  ],
};

const badgeOnlyCompliance: GuidePolicyCompliance = {
  compliant: false,
  complianceLevel: 'FAIL',
};

function componentWith(compliance: GuidePolicyCompliance | undefined): ComponentDetails {
  return { ...mockComponentDetail, policyCompliance: compliance } as unknown as ComponentDetails;
}

function renderCard(compliance: GuidePolicyCompliance | undefined) {
  return render(
    <ComponentProvider
      component={componentWith(compliance)}
      vulnerabilityCount={0}
      versionsCount={0}
      dependencyCount={0}
    >
      <PolicyComplianceCardV2 />
    </ComponentProvider>
  );
}

describe('PolicyComplianceCardV2', () => {
  it('renders the heading, badge, and policy-context link', () => {
    renderCard(activeCompliance);
    expect(screen.getByText('Policy Compliance')).toBeInTheDocument();
    expect(screen.getByText('Compliant')).toBeInTheDocument();
    // Label carries the evaluated owner and stage: activeCompliance.stage === 'release'.
    const link = screen.getByRole('link', {
      name: 'Root Organization · Release Stage · via Lifecycle',
    });
    // Stable server-side links indirection (/ui/links/...) that 302-redirects into Lifecycle.
    expect(link).toHaveAttribute('href', '/ui/links/organization/ROOT_ORGANIZATION_ID/management');
    expect(link).toHaveAttribute('data-accent-color', 'blue');
    // Opens Lifecycle in a new tab so the Guide SPA session is preserved.
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('lists active violations with policy name, constraint, and reason', () => {
    renderCard(activeCompliance);
    expect(screen.getByText('Violations (2)')).toBeInTheDocument();
    expect(screen.getByText('Architecture-Quality')).toBeInTheDocument();
    expect(screen.getByText('Version is old')).toBeInTheDocument();
    expect(screen.getByText(/Found component older than 5 years/)).toBeInTheDocument();
    expect(screen.getByText('License-Commercial')).toBeInTheDocument();
  });

  it('sorts violations by threat level descending (highest first)', () => {
    // The payload lists Architecture-Quality (threat 1) before License-Commercial (threat 7);
    // the card must render the higher-threat violation first.
    renderCard(activeCompliance);
    const licName = screen.getByText('License-Commercial');
    const archName = screen.getByText('Architecture-Quality');
    // archName follows licName in document order => License-Commercial rendered first.
    expect(licName.compareDocumentPosition(archName) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('renders a waived violation with Waived badge, scope, and expiry', () => {
    renderCard(waivedCompliance);
    expect(screen.getByText('Violations (1 — all waived)')).toBeInTheDocument();
    expect(screen.getByText('Security-Critical')).toBeInTheDocument();
    expect(screen.getByText('Waived')).toBeInTheDocument();
    expect(screen.getByText('Root Organization')).toBeInTheDocument();
    expect(screen.getByText(/Expires:/)).toBeInTheDocument();
  });

  it('does not mark the heading "all waived" when only some violations are waived', () => {
    renderCard(partiallyWaivedCompliance);
    // `allWaived` must be false (one active violation), so no " — all waived" suffix.
    expect(screen.getByText('Violations (2)')).toBeInTheDocument();
    expect(screen.queryByText(/all waived/)).not.toBeInTheDocument();
    expect(screen.getByText('Waived')).toBeInTheDocument();
  });

  it('renders the Waived badge but no scope/expiry row when the waiver detail is null', () => {
    renderCard(waivedNullWaiverCompliance);
    expect(screen.getByText('Security-Critical')).toBeInTheDocument();
    expect(screen.getByText('Waived')).toBeInTheDocument();
    // The scope/expiry row is gated on `violation.waiver`, which is null here.
    expect(screen.queryByText(/Expires:/)).not.toBeInTheDocument();
  });

  it('shows "No policy violations" when the list is empty', () => {
    renderCard(emptyCompliance);
    expect(screen.getByText('No policy violations')).toBeInTheDocument();
    expect(screen.queryByText(/Violations \(/)).not.toBeInTheDocument();
  });

  it('omits the violations section for a badge-only response but still shows the badge', () => {
    renderCard(badgeOnlyCompliance);
    expect(screen.getByText('Non-Compliant')).toBeInTheDocument();
    expect(screen.queryByText('No policy violations')).not.toBeInTheDocument();
    expect(screen.queryByText(/Violations \(/)).not.toBeInTheDocument();
    // badge-only payload carries no stage, so the context label drops the stage segment.
    expect(
      screen.getByRole('link', { name: 'Root Organization · via Lifecycle' })
    ).toBeInTheDocument();
  });

  it('renders nothing when the component has no policy compliance data', () => {
    renderCard(undefined);
    expect(screen.queryByText('Policy Compliance')).not.toBeInTheDocument();
  });
});
