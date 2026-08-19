/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneEstateComponentDetail } from 'TestRoot/nosc/components/detail/estate/renderNexusOneEstateComponentDetail';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import { estateComponentDetailStateNameForTab } from 'MainRoot/nosc/components/detail/estate/estateComponentDetailUtils';
import {
  getApiV2ComponentDetailsUrl,
  getApplicationReportDeepLinkUrl,
  getComponentUsageApplicationsUrl,
  getComponentUsageReportsUrl,
} from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

const COMPONENT_HASH = 'usage-hash-1';
const SECOND_COMPONENT_HASH = 'usage-hash-2';

describe('EstateComponentApplicationsTab', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    installRadixJsdomShims();
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('links application rows to application detail when publicId is present', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build', 'release'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(await screen.findByTestId('nosc-estate-component-applications-table')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-estate-component-applications-row-link')).toHaveAttribute(
      'href',
      '#/applications/webgoat-app'
    );
    expect(screen.getByText('Engineering')).toBeInTheDocument();
    expect(screen.getByText('build')).toBeInTheDocument();
  });

  it('renders View reports button as disabled when applicationId is absent', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    expect(screen.getByRole('button', { name: 'View reports for WebGoat' })).toBeDisabled();
    expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(0);
  });

  it('loads reports only after expanding a row and links them with applicationPublicId', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(0);

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    await waitFor(() => {
      expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(1);
    });
    expect(
      JSON.parse(
        axiosMock.history.post.find((request) => request.url === getComponentUsageReportsUrl())?.data as string
      )
    ).toMatchObject({ componentHash: COMPONENT_HASH, applicationId: 'internal-app-id' });
    expect(await screen.findByRole('link', { name: 'Open build report' })).toHaveAttribute(
      'href',
      getApplicationReportDeepLinkUrl('webgoat-app', 'report-1')
    );
  });

  it('passes an AbortSignal to reports requests and aborts when the expanded app collapses', async () => {
    let reportSignal: AbortSignal | undefined;
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply((config) => {
      reportSignal = config.signal;
      return [
        200,
        {
          reports: [],
          total: 0,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));

    await waitFor(() => {
      expect(reportSignal).toBeDefined();
    });
    expect(reportSignal?.aborted).toBe(false);

    await userEvent.click(screen.getByRole('button', { name: 'Hide reports for WebGoat' }));

    expect(reportSignal?.aborted).toBe(true);
  });

  it('aborts the expanded reports request when the component hash changes', async () => {
    let reportSignal: AbortSignal | undefined;
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply((config) => {
      reportSignal = config.signal;
      return [
        200,
        {
          reports: [],
          total: 0,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));

    await waitFor(() => {
      expect(reportSignal).toBeDefined();
    });

    await act(async () => {
      await router.stateService.go(estateComponentDetailStateNameForTab('applications'), {
        componentHash: SECOND_COMPONENT_HASH,
      });
    });

    await waitFor(() => {
      expect(reportSignal?.aborted).toBe(true);
    });
  });

  it('clears expanded reports when the component hash changes', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const applicationRequestHashes: string[] = [];
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      applicationRequestHashes.push(body.componentHash);
      return [
        200,
        {
          applications: [
            {
              applicationId: 'internal-app-id',
              applicationPublicId: 'webgoat-app',
              applicationName: 'WebGoat',
              organizationName: 'Engineering',
              stageTypeIds: ['build'],
              lastSeenTime: 1_700_000_000_000,
            },
          ],
          total: 1,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    expect(await screen.findByRole('link', { name: 'Open build report' })).toBeInTheDocument();

    await router.stateService.go(estateComponentDetailStateNameForTab('applications'), {
      componentHash: SECOND_COMPONENT_HASH,
    });

    await waitFor(() => {
      expect(applicationRequestHashes).toContain(SECOND_COMPONENT_HASH);
    });
    expect(screen.queryByTestId('nosc-estate-component-applications-reports-row')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'View reports for WebGoat' })).toBeInTheDocument();
    expect(axiosMock.history.post.filter((request) => request.url === getComponentUsageReportsUrl())).toHaveLength(1);
  });

  it('issues a second POST with page=1 when Next is clicked', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number }> = [];
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      return [
        200,
        {
          applications: [
            {
              applicationPublicId: `app-${body.page}`,
              applicationName: `App ${body.page}`,
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(
      screen
        .getByTestId('nosc-estate-component-applications-pagination')
        .querySelector('button[aria-label="Next page"]') as HTMLElement
    );

    await waitFor(() => {
      expect(bodies.some((b) => b.page === 1)).toBe(true);
    });
  });

  it('shows an honest empty state when no readable applications contain the hash', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(await screen.findByTestId('nosc-estate-component-applications-empty')).toHaveTextContent(
      'This component was not found in any readable applications.'
    );
  });

  it('shows error + Retry when the where-used applications request fails', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(500);

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');

    expect(await screen.findByTestId('nosc-estate-component-applications-error')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('resets applications page to 0 when the component hash changes', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    const bodies: Array<{ page?: number; componentHash?: string }> = [];
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply((config) => {
      const body = JSON.parse(config.data as string);
      bodies.push(body);
      return [
        200,
        {
          applications: [
            {
              applicationId: 'internal-app-id',
              applicationPublicId: `app-${body.componentHash}-${body.page}`,
              applicationName: `App ${body.page}`,
            },
          ],
          total: 40,
          page: body.page,
          pageSize: 25,
          hasNextPage: body.page === 0,
        },
      ];
    });

    const { router } = renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(
      screen
        .getByTestId('nosc-estate-component-applications-pagination')
        .querySelector('button[aria-label="Next page"]') as HTMLElement
    );
    await waitFor(() => {
      expect(bodies.some((body) => body.componentHash === COMPONENT_HASH && body.page === 1)).toBe(true);
    });

    await act(async () => {
      await router.stateService.go(estateComponentDetailStateNameForTab('applications'), {
        componentHash: SECOND_COMPONENT_HASH,
      });
    });

    await waitFor(() => {
      expect(bodies.some((body) => body.componentHash === SECOND_COMPONENT_HASH && body.page === 0)).toBe(true);
    });
    expect(bodies.filter((body) => body.componentHash === SECOND_COMPONENT_HASH).at(-1)?.page).toBe(0);
  });

  it('retries a failed reports load from the Retry reports control', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    let reportCalls = 0;
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(() => {
      reportCalls += 1;
      if (reportCalls === 1) {
        return [500];
      }
      return [
        200,
        {
          reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
          total: 1,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    expect(await screen.findByText(/Reports could not be loaded/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Retry reports for WebGoat' }));
    expect(await screen.findByRole('link', { name: 'Open build report' })).toBeInTheDocument();
    expect(reportCalls).toBe(2);
  });

  it('shows an empty reports state when the expanded application has no reports', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [],
      total: 0,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    expect(await screen.findByText('No reports found for this application.')).toBeInTheDocument();
  });

  it('shows truncation when reports total exceeds the returned page', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(200, {
      reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
      total: 40,
      page: 0,
      pageSize: 25,
      hasNextPage: true,
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    expect(await screen.findByText('Showing the first 1 of 40 reports.')).toBeInTheDocument();
  });

  it('refetches reports after collapsing an in-flight load', async () => {
    axiosMock.onPost(getApiV2ComponentDetailsUrl()).reply(200, { componentDetails: [] });
    axiosMock.onPost(getComponentUsageApplicationsUrl()).reply(200, {
      applications: [
        {
          applicationId: 'internal-app-id',
          applicationPublicId: 'webgoat-app',
          applicationName: 'WebGoat',
          organizationName: 'Engineering',
          stageTypeIds: ['build'],
          lastSeenTime: 1_700_000_000_000,
        },
      ],
      total: 1,
      page: 0,
      pageSize: 25,
      hasNextPage: false,
    });
    let reportCalls = 0;
    axiosMock.onPost(getComponentUsageReportsUrl()).reply(() => {
      reportCalls += 1;
      if (reportCalls === 1) {
        return new Promise((resolve) => {
          window.setTimeout(() => {
            resolve([
              200,
              {
                reports: [],
                total: 0,
                page: 0,
                pageSize: 25,
                hasNextPage: false,
              },
            ]);
          }, 5_000);
        });
      }
      return [
        200,
        {
          reports: [{ reportId: 'report-1', stageTypeId: 'build', evaluationTime: 1_700_000_000_000 }],
          total: 1,
          page: 0,
          pageSize: 25,
          hasNextPage: false,
        },
      ];
    });

    renderNexusOneEstateComponentDetail(COMPONENT_HASH, 'applications');
    await screen.findByTestId('nosc-estate-component-applications-table');

    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));
    await waitFor(() => {
      expect(reportCalls).toBe(1);
    });
    expect(screen.getByText('Loading reports...')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Hide reports for WebGoat' }));
    await userEvent.click(screen.getByRole('button', { name: 'View reports for WebGoat' }));

    await waitFor(() => {
      expect(reportCalls).toBe(2);
    });
    expect(await screen.findByRole('link', { name: 'Open build report' })).toBeInTheDocument();
  });
});
