/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import {
  getApiV2ComponentDetailsUrl,
  getApplicationReportDeepLinkUrl,
  getComponentUsageApplicationsUrl,
  getComponentUsageOrganizationsUrl,
  getComponentUsageReportsUrl,
  getViolationsListUrl,
} from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

const COMPONENT_HASH = 'deadbeefcafebabe';

const HDS_RESPONSE = {
  componentDetails: [
    {
      matchState: 'exact',
      component: {
        hash: COMPONENT_HASH,
        displayName: 'log4j-core 2.14.1',
        packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
        componentIdentifier: {
          format: 'maven',
          coordinates: { groupId: 'org.apache.logging.log4j', artifactId: 'log4j-core', version: '2.14.1' },
        },
      },
      licenseData: {
        status: 'Open',
        declaredLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
      },
      securityData: {
        securityIssues: [{ reference: 'CVE-2021-44228', severity: 10 }],
      },
    },
  ],
};

describe('EstateComponentDetail', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  async function selectPathOption(triggerTestId: string, optionId: string): Promise<void> {
    const user = userEvent.setup();
    const trigger = screen.getByTestId(triggerTestId);
    await waitFor(() => expect(trigger).toBeEnabled());
    await user.click(trigger);
    const menu = await screen.findByRole('menu');
    await user.click(within(menu).getByTestId(`${triggerTestId}-option-${optionId}`));
  }

  function mockDefaultUsageEndpoints(): void {
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [],
      total: 0,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
  }

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
    installRadixJsdomShims();
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  beforeEach(() => {
    mockDefaultUsageEndpoints();
  });

  it('renders Iteration 1 tabs and never kitchen-sink tabs', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header')).toHaveTextContent('log4j-core 2.14.1');

    const tabList = screen.getByTestId('nosc-estate-component-tabs');
    expect(within(tabList).getByTestId('nosc-estate-component-tab-overview')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-vulnerabilities')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-violations')).toBeInTheDocument();
    expect(within(tabList).getByTestId('nosc-estate-component-tab-applications')).toBeInTheDocument();

    expect(within(tabList).queryByTestId('nosc-estate-component-tab-legal')).not.toBeInTheDocument();
    expect(within(tabList).queryByTestId('nosc-estate-component-tab-versions')).not.toBeInTheDocument();
    expect(within(tabList).queryByTestId('nosc-estate-component-tab-organizations')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Security Events')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Labels')).not.toBeInTheDocument();
    expect(within(tabList).queryByText('Audit Log')).not.toBeInTheDocument();
  });

  it('shows Overview cards Estate Usage and Identity without versions or remediation chrome', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-overview')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-violations-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-vulnerabilities-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-license-card')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-estate-usage')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-identity')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-package-url')).toHaveTextContent(
      'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1'
    );
    expect(screen.getByTestId('nosc-estate-component-overview-violations-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/violations`
    );
    expect(screen.getByTestId('nosc-estate-component-overview-applications-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/applications`
    );
    expect(screen.getByTestId('nosc-estate-component-overview-vulnerabilities-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/vulnerabilities`
    );
    expect(screen.queryByTestId('nosc-estate-component-overview-versions-link')).not.toBeInTheDocument();
    expect(screen.queryByText(/Catalog versions/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Explore versions/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Recommended/i)).not.toBeInTheDocument();
  });

  it('shows vulnerability threat bands and highest CVSS on Overview', async () => {
    const manyIssues = Array.from({ length: 7 }, (_, i) => ({
      reference: `CVE-2021-000${i}`,
      severity: i === 0 ? 9.8 : 5,
      threatCategory: i === 0 ? 'critical' : 'moderate',
    }));
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, {
      componentDetails: [
        {
          ...HDS_RESPONSE.componentDetails[0],
          securityData: { securityIssues: manyIssues },
        },
      ],
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-overview-threat-grid')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-threat-critical')).toHaveTextContent('1');
    expect(screen.getByTestId('nosc-estate-component-overview-highest-cvss')).toHaveTextContent('9.8');
    expect(screen.getByTestId('nosc-estate-component-overview-vulnerabilities-link')).toHaveAttribute(
      'href',
      `#/components/${COMPONENT_HASH}/vulnerabilities`
    );
  });

  it('degrades Overview/Legal on HDS failure without crashing the shell', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header-error')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-detail-page')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-tabs')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-overview-error')).toBeInTheDocument();

    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);
    await userEvent.click(screen.getByTestId('nosc-estate-component-header-retry'));

    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-header')).toHaveTextContent('log4j-core 2.14.1');
    });
  });

  it('keeps blast-radius counts and auto-selects org, app, and report in the Path switcher when HDS fails', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(500);
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      return [
        200,
        {
          organizations: [{ organizationId: 'org-1', organizationName: 'Engineering', applicationCount: 3 }],
          total: 3,
          page: body.page,
          pageSize: body.pageSize,
          hasNextPage: false,
        },
      ];
    });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      return [
        200,
        {
          applications: [
            {
              applicationId: 'app-1',
              applicationPublicId: 'webgoat',
              applicationName: 'WebGoat',
              organizationId: 'org-1',
              organizationName: 'Engineering',
            },
            {
              applicationId: 'app-missing',
              applicationPublicId: 'missing-id',
              applicationName: 'Missing ID',
              organizationId: 'org-1',
              organizationName: 'Engineering',
            },
          ],
          total: 3,
          page: body.page,
          pageSize: body.pageSize,
          hasNextPage: false,
        },
      ];
    });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [],
      total: 5,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByTestId('nosc-estate-component-header-error')).toBeInTheDocument();
    expect(await screen.findByTestId('nosc-estate-component-blast-radius-applications')).toHaveTextContent('3 Apps');
    expect(screen.getByTestId('nosc-estate-component-blast-radius-organizations')).toHaveTextContent('3 Organizations');
    expect(screen.getByTestId('nosc-estate-component-blast-radius-violations')).toHaveTextContent('5 Violations');

    await waitFor(() => {
      const organizationRequests = axiosMock.history.post.filter(
        (request) => request.url === getComponentUsageOrganizationsUrl()
      );
      expect(organizationRequests.length).toBeGreaterThanOrEqual(1);
      expect(JSON.parse(organizationRequests[0].data as string)).toMatchObject({
        componentHash: COMPONENT_HASH,
        page: 0,
        pageSize: 25,
      });
      expect(
        axiosMock.history.post
          .filter((request) => request.url === getViolationsListUrl())
          .some((request) => JSON.parse(request.data as string).pageSize === 1)
      ).toBe(true);
    });

    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-path-switcher-report-link')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    const applicationTrigger = screen.getByRole('button', { name: 'Application' });
    await user.click(applicationTrigger);
    const applicationMenu = await screen.findByRole('menu');
    expect(within(applicationMenu).getByText('Showing the first 2 of 3 applications.')).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');

    await waitFor(() => {
      expect(
        axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl()).length
      ).toBeGreaterThanOrEqual(1);
    });
    expect(
      JSON.parse(
        axiosMock.history.post.find((request) => request.url === getComponentUsageReportsUrl())?.data as string
      )
    ).toMatchObject({ componentHash: COMPONENT_HASH, applicationId: 'app-1' });

    expect(await screen.findByTestId('nosc-estate-component-path-switcher-report-link')).toHaveAttribute(
      'href',
      getApplicationReportDeepLinkUrl('webgoat', 'report-1')
    );
  });

  it('supports searchable Path selection without native selects', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [
        { organizationId: 'org-1', organizationName: 'Engineering' },
        { organizationId: 'org-2', organizationName: 'Platform' },
      ],
      total: 2,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      return [
        200,
        {
          applications: [
            {
              applicationId: 'app-1',
              applicationPublicId: 'webgoat',
              applicationName: 'WebGoat',
              organizationId: body.organizationId ?? 'org-1',
            },
            {
              applicationId: 'app-2',
              applicationPublicId: 'demo',
              applicationName: 'Demo App',
              organizationId: body.organizationId ?? 'org-2',
            },
          ],
          total: 2,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [
        { reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 },
        { reportId: 'report-2', stageTypeId: 'release', evaluationTime: 1_700_000_100_000 },
      ],
      total: 2,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);
    await screen.findByTestId('nosc-estate-component-header');
    await waitFor(() => {
      expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl()).length).toBeGreaterThanOrEqual(1);
    });

    expect(document.querySelector('select')).not.toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByTestId('nosc-estate-component-path-switcher-report-link')).toHaveAttribute(
        'href',
        getApplicationReportDeepLinkUrl('webgoat', 'report-1')
      );
    });
    await selectPathOption('nosc-estate-component-path-switcher-application', 'app-2');
    await selectPathOption('nosc-estate-component-path-switcher-report', 'report-2');

    expect(await screen.findByTestId('nosc-estate-component-path-switcher-report-link')).toHaveAttribute(
      'href',
      getApplicationReportDeepLinkUrl('demo', 'report-2')
    );
  });

  it('shows an error when reports fail to load after auto-selecting an application', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);
    axiosMock.onPost(getComponentUsageOrganizationsUrl()).reply(200, {
      organizations: [{ organizationId: 'org-1', organizationName: 'Engineering' }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'app-1',
          applicationPublicId: 'webgoat',
          applicationName: 'WebGoat',
          organizationId: 'org-1',
          organizationName: 'Engineering',
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getViolationsListUrl()).reply(200, {
      violations: [],
      total: 0,
      page: 0,
      pageSize: 1,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH);

    expect(await screen.findByText('Reports could not be loaded for this application.')).toBeInTheDocument();
    await waitFor(() => {
      expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl()).length).toBeGreaterThanOrEqual(1);
    });
  });

  it('navigates to Vulnerabilities tab via the tab strip', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, HDS_RESPONSE);

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH);
    await screen.findByTestId('nosc-estate-component-overview');

    await userEvent.click(screen.getByTestId('nosc-estate-component-tab-vulnerabilities'));

    await waitFor(() => {
      expect(router.globals.$current.name).toBe('nexusOneEstateComponentDetail.vulnerabilities');
    });
    expect(await screen.findByTestId('nosc-estate-component-vulnerabilities')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'CVE-2021-44228' })).toHaveAttribute(
      'href',
      vulnerabilityDetailHref({ vulnId: 'CVE-2021-44228', componentHash: COMPONENT_HASH })
    );
  });
});
