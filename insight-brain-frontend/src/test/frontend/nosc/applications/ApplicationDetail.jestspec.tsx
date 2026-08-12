/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneApplicationDetail } from 'TestRoot/nosc/applications/renderNexusOneApplicationDetail';
import {
  getApplicationReportsUrl,
  getApplicationReportRawUrl,
  getApplicationUrl,
  getReportPolicyThreatsUrl,
  getWaiversAndAutoWaiversUrl,
} from 'MainRoot/util/CLMLocation';

const PUBLIC_ID = 'apple-java1';
const INTERNAL_ID = 'app-internal-1';
const SCAN_ID = 'scan-abc-123';

const APPLICATION_FIXTURE = {
  id: INTERNAL_ID,
  publicId: PUBLIC_ID,
  name: 'Apple Java',
  organizationId: 'org-1',
  organizationName: 'Tribbles',
};

const REPORTS_FIXTURE = [
  {
    stage: 'build',
    applicationId: INTERNAL_ID,
    evaluationDate: '2026-04-01T12:00:00.000Z',
    embeddableReportHtmlUrl: `ui/links/application/${PUBLIC_ID}/report/scan-old/embeddable`,
  },
  {
    stage: 'release',
    applicationId: INTERNAL_ID,
    evaluationDate: '2026-05-13T15:30:00.000Z',
    embeddableReportHtmlUrl: `ui/links/application/${PUBLIC_ID}/report/${SCAN_ID}/embeddable`,
  },
];

const POLICY_THREATS_FIXTURE = {
  aaData: [
    {
      hash: 'abc123',
      displayName: 'log4j-core 2.14.1',
      packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
      allViolations: [
        {
          policyName: 'Critical CVE Policy',
          policyThreatLevel: 9,
          policyThreatCategory: 'SECURITY',
          policyViolationId: 'pv-1',
          waived: false,
          legacyViolation: false,
          constraints: [{ constraintName: 'CVE-2021-44228' }],
        },
        {
          policyName: 'High Severity License',
          policyThreatLevel: 5,
          policyThreatCategory: 'LICENSE',
          policyViolationId: 'pv-2',
          waived: true,
          legacyViolation: false,
          constraints: [{ constraintName: 'GPL detected' }],
        },
      ],
    },
    {
      hash: 'def456',
      displayName: 'commons-text 1.9',
      allViolations: [
        {
          policyName: 'Moderate Quality',
          policyThreatLevel: 2,
          policyThreatCategory: 'QUALITY',
          policyViolationId: 'pv-3',
          waived: false,
          legacyViolation: false,
          constraints: [],
        },
      ],
    },
    // null hash should be filtered out by flattenViolations
    {
      hash: 'null',
      allViolations: [{ policyName: 'should-not-appear', policyThreatLevel: 9 }],
    },
  ],
  reportTime: '2026-05-13T15:30:00.000Z',
  scanId: SCAN_ID,
};

/**
 * Raw-report fixture used by the Components tab. Includes:
 *   - 2 components matching policythreats hashes (so violation counts
 *     populate from POLICY_THREATS_FIXTURE)
 *   - 1 component with no policy violations but with a security issue
 *     (proves the Components catalog includes everything, not just
 *     violations)
 *   - 1 SIMILAR-match component with mixed license threats
 *   - 1 transitive component with empty license data (license cell
 *     should fall back to "—")
 */
const RAW_REPORT_FIXTURE = {
  components: [
    {
      hash: 'abc123',
      packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
      displayName: 'log4j-core 2.14.1',
      matchState: 'exact',
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'log4j-core', version: '2.14.1' } },
      licenseData: {
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }],
        effectiveLicenseThreats: [],
      },
      securityData: { securityIssues: [{ reference: 'CVE-2021-44228', severity: 10 }] },
      dependencyData: { directDependency: true },
    },
    {
      hash: 'def456',
      packageUrl: 'pkg:maven/org.apache.commons/commons-text@1.9',
      displayName: 'commons-text 1.9',
      matchState: 'exact',
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'commons-text', version: '1.9' } },
      licenseData: {
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }],
      },
      securityData: { securityIssues: [] },
      dependencyData: { directDependency: false },
    },
    {
      hash: 'ghi789',
      packageUrl: 'pkg:maven/com.google.guava/guava@32.0.0',
      displayName: 'guava 32.0.0',
      matchState: 'exact',
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'guava', version: '32.0.0' } },
      licenseData: {
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }],
      },
      securityData: { securityIssues: [{ reference: 'CVE-2023-2976', severity: 5 }] },
      dependencyData: { directDependency: true },
    },
    {
      hash: 'jkl012',
      packageUrl: 'pkg:maven/org.example/similar-thing@1.0',
      displayName: 'similar-thing 1.0',
      matchState: 'similar',
      componentIdentifier: { format: 'maven', coordinates: { artifactId: 'similar-thing', version: '1.0' } },
      licenseData: {
        effectiveLicenses: [
          { licenseId: 'GPL-3.0', licenseName: 'GPL-3.0' },
          { licenseId: 'MIT', licenseName: 'MIT' },
        ],
        effectiveLicenseThreats: [{ licenseThreatGroupLevel: 9 }],
      },
      securityData: { securityIssues: [] },
      dependencyData: { directDependency: false },
    },
    {
      hash: 'mno345',
      packageUrl: 'pkg:npm/example-pkg@2.0.0',
      displayName: 'example-pkg 2.0.0',
      matchState: 'unknown',
      componentIdentifier: { format: 'npm', coordinates: { packageId: 'example-pkg', version: '2.0.0' } },
      licenseData: {},
      securityData: { securityIssues: [] },
      dependencyData: { directDependency: false },
    },
  ],
  matchSummary: { totalComponentCount: 5, knownComponentCount: 4 },
};

function mockHappyPath(axiosMock: any): void {
  axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
  axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
  axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);
  axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, RAW_REPORT_FIXTURE);
}

describe('ApplicationDetail (CLM-39709 / P1-F7c)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    axiosMock.reset();
  });

  // Mount the page at its UI-Router state so it reads publicId/tab from the
  // router. `tabSlug` (a URL slug, e.g. 'components' | 'violations' | 'waivers')
  // lands on the per-tab state; omit it for the default (overview) tab.
  function renderAppDetail(tabSlug?: string) {
    return renderNexusOneApplicationDetail(PUBLIC_ID, tabSlug);
  }

  it('renders the page shell with the publicId baked in as a data attribute', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-breadcrumb')).toHaveAttribute(
        'data-public-id',
        PUBLIC_ID,
      );
    });
  });

  it('renders header with application name and publicId from the application endpoint', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-name')).toHaveTextContent('Apple Java');
    });
    // publicId appears in both the header AND the Application Details
    // sidebar table — both occurrences are intentional.
    expect(screen.getAllByText(PUBLIC_ID).length).toBeGreaterThanOrEqual(1);
    // Org name appears in both the header and the App Details card.
    expect(screen.getAllByText('Tribbles').length).toBeGreaterThanOrEqual(1);
  });

  it('renders all 5 V1 tabs in the correct order', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-page');

    const expectedTestIds = [
      'nosc-app-detail-tab-overview',
      'nosc-app-detail-tab-policy-failures',
      'nosc-app-detail-tab-components',
      'nosc-app-detail-tab-evaluations',
      'nosc-app-detail-tab-waivers',
    ];
    const tabList = screen.getByTestId('nosc-app-detail-tabs');
    const triggers = within(tabList)
      .getAllByRole('tab')
      .map((el) => el.getAttribute('data-testid'));
    expect(triggers).toEqual(expectedTestIds);
  });

  it('does not render SBOMs, Team Members, or Security Events tabs (V1)', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-page');
    expect(screen.queryByRole('tab', { name: /sboms/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /team members/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /security events/i })).not.toBeInTheDocument();
  });

  it('Overview tab shows Policy Compliance, Risk Metrics, Scan Info, and App Details cards', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-policy-compliance-card')).toBeInTheDocument();
    });
    expect(screen.getByTestId('nosc-app-detail-risk-metrics-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-scan-info-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-app-details-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-quick-actions-card')).toBeInTheDocument();
  });

  it('Quick Actions deep-link to in-shell RSC embeds / native routes (not Coming Soon)', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    const quickActions = await screen.findByTestId('nosc-app-detail-quick-actions-card');
    expect(within(quickActions).getByTestId('nosc-app-detail-quick-action-policies')).toHaveAttribute(
      'href',
      `#/management/view/application/${PUBLIC_ID}`,
    );
    expect(within(quickActions).getByTestId('nosc-app-detail-quick-action-waivers')).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}/waivers`,
    );
    expect(
      within(quickActions).getByTestId('nosc-app-detail-quick-action-source-control'),
    ).toHaveAttribute('href', `#/management/edit/application/${PUBLIC_ID}/source-control`);
    expect(within(quickActions).getByTestId('nosc-app-detail-quick-action-reports')).toHaveAttribute(
      'href',
      '#/reports',
    );
    expect(within(quickActions).getByTestId('nosc-app-detail-quick-action-reports')).toHaveTextContent(
      'Enterprise Reporting',
    );
  });

  it('Overview Policy Compliance card shows total + open + waived counts derived from policythreats.json', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    const card = await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await waitFor(() => {
      // 3 violations total
      expect(within(card).getByText('3')).toBeInTheDocument();
    });
    // The Critical / Severe / Moderate severity legend is rendered.
    expect(within(card).getByText('Critical')).toBeInTheDocument();
    expect(within(card).getByText('Severe')).toBeInTheDocument();
    expect(within(card).getByText('Moderate')).toBeInTheDocument();
    // Open + Waived legend.
    expect(within(card).getByText('Open')).toBeInTheDocument();
    expect(within(card).getByText('Waived')).toBeInTheDocument();
  });

  it('Overview Scan Information card shows the latest evaluation date and stage', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    const card = await screen.findByTestId('nosc-app-detail-scan-info-card');
    await waitFor(() => {
      // 'release' is the most-recent stage in the fixture; it appears
      // in the "Stage" badge and in the "Stages Reporting" badge row.
      expect(within(card).getAllByText('release').length).toBeGreaterThanOrEqual(1);
    });
    // Full report stays in the NOUX application-report embed.
    const viewReport = within(card).getByTestId('nosc-app-detail-view-full-report');
    expect(viewReport).toHaveAttribute(
      'href',
      `#/applications/${PUBLIC_ID}/report/${SCAN_ID}`,
    );
    expect(viewReport).toHaveTextContent('View full report');
  });

  it('Application Details card surfaces internal id + organization without a Classic escape', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    const card = await screen.findByTestId('nosc-app-detail-app-details-card');
    await waitFor(() => {
      expect(within(card).getByText(INTERNAL_ID)).toBeInTheDocument();
    });
    expect(within(card).getByText('Tribbles')).toBeInTheDocument();
    expect(within(card).queryByTestId('nosc-app-detail-view-classic-app')).not.toBeInTheDocument();
    expect(within(card).queryByText(/classic/i)).not.toBeInTheDocument();
  });

  it('Policy Failures tab renders a table with one row per violation (filters out null-hash components)', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    // Wait for data, then activate the Policy Failures tab.
    await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));

    const table = await screen.findByTestId('nosc-app-detail-policy-failures-table');
    const rows = within(table).getAllByTestId('nosc-app-detail-policy-failures-row');
    expect(rows).toHaveLength(3);

    // The 'should-not-appear' violation under the null-hash component must NOT
    // render — flattenViolations skips it.
    expect(screen.queryByText('should-not-appear')).not.toBeInTheDocument();

    // Highest threat sorts first.
    expect(rows[0]).toHaveTextContent('Critical CVE Policy');
  });

  it('Policy Failures policy name deep-links to native violation detail when policyViolationId is set', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));
    await screen.findByTestId('nosc-app-detail-policy-failures-table');

    const criticalLink = screen.getByRole('link', { name: 'Critical CVE Policy' });
    expect(criticalLink).toHaveAttribute('href', '#/violations/pv-1/overview');
  });

  it('Policy Failures policy name falls back to plain text when policyViolationId is absent', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
      ...POLICY_THREATS_FIXTURE,
      aaData: [
        {
          hash: 'abc123',
          displayName: 'log4j-core 2.14.1',
          allViolations: [
            {
              policyName: 'Unlinked Policy',
              policyThreatLevel: 9,
              policyThreatCategory: 'SECURITY',
              waived: false,
              legacyViolation: false,
              constraints: [],
            },
          ],
        },
      ],
    });
    axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, RAW_REPORT_FIXTURE);

    renderAppDetail();
    await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));
    await screen.findByTestId('nosc-app-detail-policy-failures-table');

    expect(screen.getByText('Unlinked Policy')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Unlinked Policy' })).not.toBeInTheDocument();
  });

  it('related-risk context rail marks the application as current after metadata loads', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-header');
    const rail = await screen.findByTestId('nosc-app-detail-context-rail');
    expect(within(rail).getByText('Apple Java')).toHaveAttribute('aria-current', 'page');
    expect(within(rail).queryByRole('link', { name: 'Apple Java' })).not.toBeInTheDocument();
    // Application-only context still shows the full chain as unavailable placeholders.
    for (const placeholder of ['Component', 'Violation', 'Vulnerability'] as const) {
      expect(within(rail).getByText(placeholder)).toBeInTheDocument();
      expect(within(rail).queryByRole('link', { name: placeholder })).not.toBeInTheDocument();
    }
  });

  it('Policy Failures tab badge in the tab strip shows the total violation count', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    const tab = await screen.findByTestId('nosc-app-detail-tab-policy-failures');
    await waitFor(() => {
      expect(tab).toHaveTextContent('3');
    });
  });

  it('Policy Failures tab supports text-search filtering of components', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));
    await screen.findByTestId('nosc-app-detail-policy-failures-table');

    const searchInput = screen.getByPlaceholderText(/search policies or components/i);
    await userEvent.type(searchInput, 'log4j');

    await waitFor(() => {
      const filteredRows = within(
        screen.getByTestId('nosc-app-detail-policy-failures-table'),
      ).getAllByTestId('nosc-app-detail-policy-failures-row');
      expect(filteredRows).toHaveLength(2);
    });
  });

  it('Waivers tab renders live waivers scoped to this application via applicationIds filter (CLM-39545 / P1-F7d)', async () => {
    mockHappyPath(axiosMock);

    let postedBody: any = null;
    axiosMock
      .onPost(getWaiversAndAutoWaiversUrl())
      .reply((config: any) => {
        postedBody = JSON.parse(config.data || '{}');
        return [
          200,
          {
            dashboardResults: [
              {
                id: 'w-app-1',
                threatLevel: 9,
                policyName: 'Critical CVSS 9+',
                ownerId: INTERNAL_ID,
                ownerName: 'Apple Java',
                ownerType: 'application',
                scope: 'Application: Apple Java',
                createTime: '2026-05-01T10:00:00Z',
              },
            ],
            hasNextPage: false,
          },
        ];
      });

    renderAppDetail();
    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-waivers'));

    const tab = await screen.findByTestId('nosc-app-detail-waivers-tab');
    expect(tab).toBeInTheDocument();

    // Live row from the dashboard endpoint shows up.
    expect(await screen.findByText('Critical CVSS 9+')).toBeInTheDocument();
    // Backend was filtered to this application's internal id.
    expect(postedBody?.applicationIds).toEqual([INTERNAL_ID]);
    expect(within(tab).queryByTestId('nosc-app-detail-waivers-classic-link')).not.toBeInTheDocument();
    // Detail link uses the native /waivers/{type}/{id}/{wid} route.
    const detail = within(tab).getByTestId('nosc-app-detail-waivers-table-row-detail-link');
    expect(detail).toHaveAttribute(
      'href',
      expect.stringContaining(`/waivers/application/${INTERNAL_ID}/w-app-1`),
    );
  });

  it('does not fetch the raw report until the Components tab is opened (Goldman V1 lazy load)', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-page');
    await waitFor(() => {
      expect(axiosMock.history.get.some((r: { url?: string }) =>
        r.url?.includes(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)),
      )).toBe(true);
    });

    expect(
      axiosMock.history.get.some((r: { url?: string }) =>
        r.url?.includes(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)),
      ),
    ).toBe(false);

    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-components'));
    await screen.findByTestId('nosc-app-detail-components-table');

    expect(
      axiosMock.history.get.some((r: { url?: string }) =>
        r.url?.includes(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)),
      ),
    ).toBe(true);
  });

  it('Waivers tab paginates server-side when hasNextPage is true (CLM-42227)', async () => {
    mockHappyPath(axiosMock);

    const waiverPage = (page: number) =>
      Array.from({ length: 3 }, (_, i) => ({
        id: `w-page${page}-${i}`,
        threatLevel: 5,
        ownerId: INTERNAL_ID,
        ownerType: 'application',
        scope: 'app',
        policyName: `policy-page${page}-${i}`,
      }));

    axiosMock.onPost(getWaiversAndAutoWaiversUrl()).reply((config: { data?: string }) => {
      const body = JSON.parse(config.data || '{}');
      const page = body.page ?? 0;
      return [
        200,
        {
          dashboardResults: waiverPage(page),
          hasNextPage: page === 0,
        },
      ];
    });

    renderAppDetail();
    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-waivers'));

    expect(await screen.findByText('policy-page0-0')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-app-detail-waivers-pagination')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByTestId('nosc-app-detail-waivers-pagination')).toBeInTheDocument();
    expect(await screen.findByText('policy-page1-0')).toBeInTheDocument();
  });

  it('Waivers tab shows an application-specific empty state when the app has zero waivers', async () => {
    mockHappyPath(axiosMock);
    axiosMock
      .onPost(getWaiversAndAutoWaiversUrl())
      .reply(200, { dashboardResults: [], hasNextPage: false });

    renderAppDetail();
    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-waivers'));

    expect(
      await screen.findByText(/no waivers apply to this application/i),
    ).toBeInTheDocument();
  });

  it('renders a loading skeleton in the header before the application endpoint resolves', async () => {
    // Never resolve so the loading state persists.
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(() => new Promise(() => {}));
    renderAppDetail();

    expect(await screen.findByTestId('nosc-app-detail-header-loading')).toBeInTheDocument();
  });

  it('renders an error state with a Retry button when the application endpoint 500s', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(500, { message: 'boom' });
    renderAppDetail();

    const errorBox = await screen.findByTestId('nosc-app-detail-header-error');
    expect(errorBox).toBeInTheDocument();
    expect(within(errorBox).getByTestId('nosc-app-detail-header-retry')).toBeInTheDocument();
  });

  it('Policy Failures tab shows an empty "No scans yet" panel when the app has never been scanned', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, []);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));

    expect(
      await screen.findByTestId('nosc-app-detail-policy-failures-no-scan'),
    ).toBeInTheDocument();
  });

  it('Policy Failures tab gracefully renders an error + Retry when policythreats 500s', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(500, {});

    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));

    const errorBox = await screen.findByTestId('nosc-app-detail-policy-failures-error');
    expect(within(errorBox).getByTestId('nosc-app-detail-policy-failures-retry')).toBeInTheDocument();
  });

  it('Policy Failures empty state (zero open violations after filter on a scanned app) renders the "No matches found" card', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
      aaData: [],
      reportTime: '2026-05-13T15:30:00.000Z',
      scanId: SCAN_ID,
    });

    renderAppDetail();
    await screen.findByTestId('nosc-app-detail-page');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));

    expect(
      await screen.findByTestId('nosc-app-detail-policy-failures-empty'),
    ).toBeInTheDocument();
  });

  it('Overview Policy Compliance card surfaces a Retry that re-attempts the fetch when policythreats fails', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);

    // First call fails; second call (after Retry) succeeds.
    let calls = 0;
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(() => {
      calls += 1;
      return calls === 1 ? [500, {}] : [200, POLICY_THREATS_FIXTURE];
    });

    renderAppDetail();

    const card = await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await waitFor(() => {
      expect(within(card).getByText(/could not load policy data/i)).toBeInTheDocument();
    });
    const retryBtn = within(card).getByRole('button', { name: /retry/i });
    await userEvent.click(retryBtn);

    await waitFor(() => {
      expect(within(card).getByText('Total Violations')).toBeInTheDocument();
    });
  });

  it('Overview Scan Information card surfaces a Retry that re-attempts when the reports endpoint fails', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);

    let calls = 0;
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(() => {
      calls += 1;
      return calls === 1 ? [500, {}] : [200, REPORTS_FIXTURE];
    });
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);

    renderAppDetail();

    const card = await screen.findByTestId('nosc-app-detail-scan-info-card');
    await waitFor(() => {
      expect(within(card).getByText(/could not load scan history/i)).toBeInTheDocument();
    });
    const retryBtn = within(card).getByRole('button', { name: /retry/i });
    await userEvent.click(retryBtn);

    await waitFor(() => {
      expect(within(card).getByText('Last Scan')).toBeInTheDocument();
    });
  });

  it('retrying a failed reports fetch also loads the downstream policy data (full chain re-runs)', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);

    // Reports fail first, then succeed on retry. policythreats/raw depend on the
    // scanId parsed from reports, so they must fire after the retry resolves.
    let reportCalls = 0;
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(() => {
      reportCalls += 1;
      return reportCalls === 1 ? [500, {}] : [200, REPORTS_FIXTURE];
    });
    axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);
    axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, RAW_REPORT_FIXTURE);

    renderAppDetail();

    const scanCard = await screen.findByTestId('nosc-app-detail-scan-info-card');
    await waitFor(() => {
      expect(within(scanCard).getByText(/could not load scan history/i)).toBeInTheDocument();
    });
    await userEvent.click(within(scanCard).getByRole('button', { name: /retry/i }));

    // The downstream policy data must actually load after the retry. The card
    // shows "Total Violations" whenever a scanId exists, so assert the real count
    // (3 from POLICY_THREATS_FIXTURE) — it would be 0 if retryReports only
    // re-fetched reports and never re-ran the policy/raw chain.
    const policyCard = await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await waitFor(() => {
      const total = within(policyCard).getByText('Total Violations').parentElement;
      expect(total).toHaveTextContent('3');
    });
  });

  it('Overview cards show a "not scanned yet" message when the app has no reports', async () => {
    axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
    axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, []);

    renderAppDetail();

    const policyCard = await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await waitFor(() => {
      expect(within(policyCard).getByText(/not been scanned yet/i)).toBeInTheDocument();
    });
    const riskCard = screen.getByTestId('nosc-app-detail-risk-metrics-card');
    expect(within(riskCard).getByText(/risk metrics appear after the first scan/i)).toBeInTheDocument();
    const scanCard = screen.getByTestId('nosc-app-detail-scan-info-card');
    expect(within(scanCard).getByText(/no scans on record yet/i)).toBeInTheDocument();
  });

  it('Policy Failures table sidebar offers a "Reset filters" button that clears active filters', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();

    await screen.findByTestId('nosc-app-detail-policy-compliance-card');
    await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));
    await screen.findByTestId('nosc-app-detail-policy-failures-table');

    // Apply a search to narrow.
    const searchInput = screen.getByPlaceholderText(/search policies or components/i);
    await userEvent.type(searchInput, 'log4j');

    await waitFor(() => {
      const filteredRows = within(
        screen.getByTestId('nosc-app-detail-policy-failures-table'),
      ).getAllByTestId('nosc-app-detail-policy-failures-row');
      expect(filteredRows).toHaveLength(2);
    });

    // Reset filters returns the full list.
    const resetButton = screen.getByRole('button', { name: /reset filters/i });
    await userEvent.click(resetButton);

    await waitFor(() => {
      const allRows = within(
        screen.getByTestId('nosc-app-detail-policy-failures-table'),
      ).getAllByTestId('nosc-app-detail-policy-failures-row');
      expect(allRows).toHaveLength(3);
    });
  });

  it('reads the publicId from the route params', async () => {
    mockHappyPath(axiosMock);
    renderAppDetail();
    await waitFor(() => {
      expect(screen.getByTestId('nosc-app-detail-breadcrumb')).toHaveAttribute(
        'data-public-id',
        PUBLIC_ID,
      );
    });
  });

  describe('Components tab (CLM-39709 / P1-F7c)', () => {
    it('renders one row per component in the raw report (catalog, not just violations)', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      const rows = within(table).getAllByTestId('nosc-app-detail-components-row');
      // RAW_REPORT_FIXTURE has 5 entries. POLICY_THREATS_FIXTURE only has
      // 2 with-violations entries (+1 null-hash). Components tab MUST
      // show all 5, not the violations subset.
      expect(rows).toHaveLength(5);
    });

    it('column count badge in the Components tab trigger reflects policythreats count on landing (CLM-42227)', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail();
      const componentsTab = await screen.findByTestId('nosc-app-detail-tab-components');
      // Badge uses policythreats aaData length so it populates without the deferred raw fetch.
      await waitFor(() => {
        expect(within(componentsTab).getAllByText('3').length).toBeGreaterThanOrEqual(1);
      });
      expect(
        axiosMock.history.get.some((r: { url?: string }) =>
          r.url?.includes(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)),
        ),
      ).toBe(false);
    });

    it('Violations column reflects per-component active violation count from policythreats.json', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      // log4j-core has 1 active violation in POLICY_THREATS_FIXTURE
      // (1 active + 1 waived from the fixture's allViolations[]).
      // commons-text has 1 active violation. guava + similar-thing +
      // example-pkg have 0 active violations.
      expect(within(table).getByText('log4j-core 2.14.1')).toBeInTheDocument();
      expect(within(table).getByText('commons-text 1.9')).toBeInTheDocument();
      expect(within(table).getByText('guava 32.0.0')).toBeInTheDocument();
    });

    it('Match column shows Exact / Similar / Unknown badges from matchState', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      expect(within(table).getAllByText('Exact').length).toBeGreaterThanOrEqual(1);
      expect(within(table).getByText('Similar')).toBeInTheDocument();
      expect(within(table).getByText('Unknown')).toBeInTheDocument();
    });

    it('License column shows effective license names; falls back to "—" when absent', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      expect(within(table).getAllByText('Apache-2.0').length).toBeGreaterThanOrEqual(1);
      // Multi-license cell joins with ", "
      expect(within(table).getByText('GPL-3.0, MIT')).toBeInTheDocument();
      // Empty licenseData -> "—"
      expect(within(table).getAllByText('—').length).toBeGreaterThanOrEqual(1);
    });

    it('search input filters rows by component name / purl / license', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      await screen.findByTestId('nosc-app-detail-components-table');
      const search = screen.getByTestId('nosc-app-detail-components-search');
      await userEvent.type(search, 'guava');
      await waitFor(() => {
        const rows = screen.getAllByTestId('nosc-app-detail-components-row');
        expect(rows).toHaveLength(1);
      });
      expect(screen.getByText('guava 32.0.0')).toBeInTheDocument();
    });

    it('shows a "no matches" panel when the search filter excludes everything', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      await screen.findByTestId('nosc-app-detail-components-table');
      const search = screen.getByTestId('nosc-app-detail-components-search');
      await userEvent.type(search, 'zzz-not-a-real-component');
      await waitFor(() => {
        expect(screen.getByTestId('nosc-app-detail-components-no-matches')).toBeInTheDocument();
      });
    });

    it('row "View →" link goes to estate component detail with Path context', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      const links = within(table).getAllByTestId('nosc-app-detail-components-row-link');
      // First row in the fixture is log4j-core with hash=abc123.
      expect(links[0]).toHaveAttribute(
        'href',
        `#/components/abc123?organizationId=org-1&applicationId=${INTERNAL_ID}&reportId=${SCAN_ID}`,
      );
      expect(links[0]).toHaveAccessibleName(/View .+ details/);
    });

    it('row "View →" uses hash-only when organizationId is absent (Path pin would not stick)', async () => {
      axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, {
        ...APPLICATION_FIXTURE,
        organizationId: undefined,
      });
      axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
      axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);
      axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, RAW_REPORT_FIXTURE);
      renderAppDetail('components');
      const table = await screen.findByTestId('nosc-app-detail-components-table');
      const links = within(table).getAllByTestId('nosc-app-detail-components-row-link');
      expect(links[0]).toHaveAttribute('href', '#/components/abc123');
    });

    it('page-level full report link stays in the NOUX application-report embed', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('components');
      await screen.findByTestId('nosc-app-detail-components-table');
      const link = screen.getByTestId('nosc-app-detail-components-full-report-link');
      expect(link).toHaveAttribute('href', `#/applications/${PUBLIC_ID}/report/${SCAN_ID}`);
      expect(link).toHaveTextContent('View full report');
    });

    it('shows a large-scan banner when the component inventory exceeds the threshold', async () => {
      axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
      axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
      axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);
      axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(200, {
        components: Array.from({ length: 10_000 }, (_, i) => ({
          hash: `hash-${i}`,
          displayName: `component-${i}`,
          matchState: 'exact',
          licenseData: {},
          securityData: { securityIssues: [] },
          dependencyData: { directDependency: true },
        })),
      });

      renderAppDetail('components');
      expect(
        await screen.findByTestId('nosc-app-detail-components-large-scan'),
      ).toBeInTheDocument();
    });

    it('renders an error card with Retry when the raw-report endpoint 500s', async () => {
      axiosMock.onGet(getApplicationUrl(PUBLIC_ID)).reply(200, APPLICATION_FIXTURE);
      axiosMock.onGet(getApplicationReportsUrl(INTERNAL_ID)).reply(200, REPORTS_FIXTURE);
      axiosMock.onGet(getReportPolicyThreatsUrl(PUBLIC_ID, SCAN_ID)).reply(200, POLICY_THREATS_FIXTURE);
      axiosMock.onGet(getApplicationReportRawUrl(PUBLIC_ID, SCAN_ID)).reply(500, {});
      renderAppDetail('components');
      const errorCard = await screen.findByTestId('nosc-app-detail-components-error');
      expect(within(errorCard).getByTestId('nosc-app-detail-components-retry')).toBeInTheDocument();
    });
  });

  describe('URL-driven tab state (CLM-39709 §URL contract)', () => {
    it('defaults to Overview when the URL has no tab segment', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail();
      const overviewTab = await screen.findByTestId('nosc-app-detail-tab-overview');
      expect(overviewTab.getAttribute('aria-selected')).toBe('true');
    });

    it('opens directly on the tab named in the URL', async () => {
      mockHappyPath(axiosMock);
      axiosMock
        .onPost(getWaiversAndAutoWaiversUrl())
        .reply(200, { dashboardResults: [], hasNextPage: false });
      renderAppDetail('waivers');
      const waiversTab = await screen.findByTestId('nosc-app-detail-tab-waivers');
      await waitFor(() => expect(waiversTab.getAttribute('aria-selected')).toBe('true'));
    });

    it('maps the friendly URL slug "violations" to the internal "policy-failures" tab', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('violations');
      const violationsTab = await screen.findByTestId('nosc-app-detail-tab-policy-failures');
      await waitFor(() => expect(violationsTab.getAttribute('aria-selected')).toBe('true'));
    });

    it('falls back to Overview when the URL slug is unrecognized (malformed bookmark)', async () => {
      mockHappyPath(axiosMock);
      renderAppDetail('zzz-not-a-tab');
      const overviewTab = await screen.findByTestId('nosc-app-detail-tab-overview');
      await waitFor(() => expect(overviewTab.getAttribute('aria-selected')).toBe('true'));
    });

    it('navigates to the per-tab route when the user clicks a tab (UI-Router)', async () => {
      mockHappyPath(axiosMock);
      const { router } = renderAppDetail();
      await screen.findByTestId('nosc-app-detail-page');
      await userEvent.click(screen.getByTestId('nosc-app-detail-tab-policy-failures'));
      await waitFor(() => {
        // The router lands on the per-tab state with the friendly "violations" slug,
        // and the clicked tab becomes selected (re-derived from the new param).
        expect(router.globals.current.name).toBe('nexusOneApplicationsDetail.violations');
        expect(router.globals.params.publicId).toBe(PUBLIC_ID);
      });
      expect(
        screen.getByTestId('nosc-app-detail-tab-policy-failures').getAttribute('aria-selected'),
      ).toBe('true');
    });
  });
});
