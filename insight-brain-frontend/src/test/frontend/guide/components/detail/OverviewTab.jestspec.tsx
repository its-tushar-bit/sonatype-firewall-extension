/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Tabs } from '@radix-ui/themes';
import { ComponentProvider } from '@guide/ui-core';
import type { ComponentDetails } from '@guide/ui-core/types';
import { render, screen } from '../../test-utils';
import { mockComponentDetail } from 'TestRoot/guide/api/fixtures/componentDetailFixtures';
import { OverviewTab } from 'GuideRoot/components/detail/OverviewTab';
import type { GuidePolicyCompliance } from 'GuideRoot/components/detail/policyComplianceTypes';

const passCompliance: GuidePolicyCompliance = {
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

function renderOverview() {
  const component = { ...mockComponentDetail, policyCompliance: passCompliance } as unknown as ComponentDetails;
  return render(
    <Tabs.Root value="overview">
      <ComponentProvider
        component={component}
        vulnerabilityCount={0}
        versionsCount={0}
        dependencyCount={0}
      >
        <OverviewTab />
      </ComponentProvider>
    </Tabs.Root>,
    { routerOptions: { initialEntries: ['/component/npm/lodash/4.17.21'] } }
  );
}

describe('OverviewTab', () => {
  it('renders Vulnerabilities, License, and the V2 Policy Compliance card', () => {
    renderOverview();
    // Use heading roles: "License" also appears as a data-label inside the cards, so getByText
    // would be ambiguous. Headings disambiguate.
    expect(screen.getByRole('heading', { name: 'Vulnerabilities' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'License' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Policy Compliance' })).toBeInTheDocument();
    // Unique to PolicyComplianceCardV2: the old ui-core PolicyComplianceCard renders no such link,
    // so this assertion is what makes the test red before the layout swap and green after.
    expect(screen.getByRole('link', { name: /via Lifecycle/ })).toBeInTheDocument();
  });
});
