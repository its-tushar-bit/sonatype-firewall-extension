/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import React2ShellPage from 'MainRoot/report/react2shell/React2ShellPage';
import userEvent from '@testing-library/user-event';
import { actions } from 'MainRoot/report/react2shell/react2ShellSlice';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('React2ShellPage', () => {
  let axiosMock;

  const mockApiResponse = {
    success: true,
    aggregates: {
      totalAffectedApplications: 5,
      affectedComponents: 10,
      violatingComponents: 3,
      activeWaivers: 2,
    },
    results: [
      {
        applicationName: 'Test App 1',
        applicationPublicId: 'app-1',
        applicationInternalId: 'internal-1',
        stage: 'build',
        reportId: 'report-1',
        componentDisplayName: 'test-component',
        packageUrl: 'pkg:npm/test@1.0.0',
        hash: 'abc123',
        cveId: 'CVE-2025-55182',
        cvssScore: 9.8,
        cvssVector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H',
        recommendedAction: 'Upgrade to version 2.0.0',
        activeWaiver: true,
        violating: false,
        evaluationDate: '2025-01-15T10:00:00Z',
        baseUrl: 'http://localhost',
      },
      {
        applicationName: 'Test App 2',
        applicationPublicId: 'app-2',
        applicationInternalId: 'internal-2',
        stage: 'develop',
        reportId: 'report-2',
        componentDisplayName: 'another-component',
        packageUrl: 'pkg:npm/another@2.0.0',
        hash: 'def456',
        cveId: 'CVE-2025-55182',
        cvssScore: 8.5,
        cvssVector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:N',
        recommendedAction: 'No fix available',
        activeWaiver: false,
        violating: true,
        evaluationDate: '2025-01-16T10:00:00Z',
        baseUrl: 'http://localhost',
      },
    ],
    pageNumber: 1,
    pageSize: 10,
    totalCount: 2,
  };

  const defaultPreloadedState = {
    react2Shell: {
      loading: false,
      sorting: false,
      error: null,
      aggregates: null,
      impactData: null,
      pagination: null,
      currentPage: 0,
      sortBy: null,
      sortOrder: 'asc',
    },
    router: {
      currentParams: {},
      currentState: { name: 'react2ShellReport' },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, mockApiResponse);

    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn((stateName, params) => {
        if (stateName === 'vulnerabilitySearchDetail') {
          return `#/vulnerability/${params?.id || 'unknown'}`;
        }
        return '#/mocked-route';
      }),
      get: jest.fn(),
      includes: jest.fn(),
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) => {
    return render(<React2ShellPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render page header with CVE ID', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /React2Shell Impact Report/i })).toBeInTheDocument();
    });
  });

  it('should render component and trigger data fetch', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /React2Shell Impact Report/i })).toBeInTheDocument();
  });

  it('should display loading state', () => {
    const loadingState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        loading: true,
      },
    };

    renderComponent(loadingState);

    expect(screen.getAllByText('Loading…').length).toBeGreaterThan(0);
  });

  it('should display summary metrics after loading', async () => {
    const withDataState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        aggregates: {
          affectedApplications: 5,
          affectedComponents: 10,
          violatingComponents: 3,
          activeWaivers: 2,
        },
      },
    };

    renderComponent(withDataState);

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('should display error message when data fetch fails', async () => {
    axiosMock.reset();
    axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(500, { message: 'Server error' });

    const errorState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        loading: false,
        error: null,
        aggregates: null,
      },
    };

    renderComponent(errorState);

    expect(screen.getByRole('heading', { name: /React2Shell Impact Report/i })).toBeInTheDocument();

    expect(screen.getByRole('heading', { name: /About the React2Shell Vulnerability/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('should display Impact Summary section when data is available', async () => {
    const withDataState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        aggregates: {
          affectedApplications: 5,
          affectedComponents: 10,
          violatingComponents: 3,
          activeWaivers: 2,
        },
        impactData: [
          {
            applicationName: 'Test App 1',
            component: 'test-component',
            version: '1.0.0',
            componentDisplayName: 'test-component',
            vulnerability: { cveId: 'CVE-2025-55182', cvssScore: 9.8 },
            recommendedAction: 'Upgrade to 2.0.0',
            activeWaiver: false,
            evaluation: 'Fail',
            evaluationDate: '2025-01-15T10:00:00Z',
          },
        ],
        pagination: {
          page: 1,
          pageSize: 10,
          totalItems: 1,
          totalPages: 1,
        },
      },
    };

    renderComponent(withDataState);

    expect(await screen.findByRole('heading', { name: /Impact Summary/i })).toBeInTheDocument();
  });

  it('should display pagination text', async () => {
    const paginatedResponse = {
      ...mockApiResponse,
      totalCount: 25,
      pageSize: 10,
    };
    axiosMock.reset();
    axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, paginatedResponse);

    const withPaginationState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        aggregates: {
          affectedApplications: 5,
          affectedComponents: 25,
          violatingComponents: 10,
          activeWaivers: 2,
        },
        impactData: [],
        pagination: {
          page: 1,
          pageSize: 10,
          totalItems: 25,
          totalPages: 3,
        },
      },
    };

    renderComponent(withPaginationState);

    expect(await screen.findByText(/Showing 1-10 of 25 affected components/i)).toBeInTheDocument();
  });

  it('should render pagination controls when multiple pages exist', async () => {
    const multiPageResponse = {
      ...mockApiResponse,
      totalCount: 150,
      pageSize: 50,
    };
    axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, multiPageResponse);

    const { store } = renderComponent();

    await waitFor(() => {
      const state = store.getState();
      expect(state.react2Shell.pagination).toBeTruthy();
      expect(state.react2Shell.pagination.totalPages).toBe(3);
      expect(state.react2Shell.loading).toBe(false);
      expect(state.react2Shell.aggregates).toBeTruthy();
    });

    expect(screen.queryByRole('navigation', { name: 'pagination' })).toBeInTheDocument();
  });

  it('should not render pagination controls when only one page exists', async () => {
    const singlePageState = {
      ...defaultPreloadedState,
      react2Shell: {
        ...defaultPreloadedState.react2Shell,
        aggregates: {
          affectedApplications: 5,
          affectedComponents: 5,
          violatingComponents: 2,
          activeWaivers: 1,
        },
        impactData: [],
        pagination: {
          page: 1,
          pageSize: 10,
          totalItems: 5,
          totalPages: 1,
        },
      },
    };

    renderComponent(singlePageState);

    expect(screen.queryByRole('navigation', { name: 'pagination' })).not.toBeInTheDocument();
  });

  it('should handle pagination changes', async () => {
    const multiPageResponse = {
      ...mockApiResponse,
      totalCount: 150,
      pageSize: 50,
    };
    axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, multiPageResponse);

    const { store } = renderComponent();

    await waitFor(() => {
      const state = store.getState();
      expect(state.react2Shell.pagination).toBeTruthy();
      expect(state.react2Shell.pagination.totalPages).toBe(3);
      expect(state.react2Shell.loading).toBe(false);
      expect(state.react2Shell.aggregates).toBeTruthy();
    });

    const paginationNav = screen.getByRole('navigation', { name: 'pagination' });
    expect(paginationNav).toBeInTheDocument();

    const { setPage } = actions;
    store.dispatch(setPage(1));

    expect(store.getState().react2Shell.currentPage).toBe(1);
  });

  describe('Header Section', () => {
    it('should render download CSV button with correct attributes', () => {
      renderComponent();

      const downloadButton = screen.getByRole('link', { name: /Download CSV/i });
      expect(downloadButton).toBeInTheDocument();
      expect(downloadButton).toHaveAttribute('download', 'react2shell-report.csv');
    });

    it('should display CVE ID in subtitle', () => {
      renderComponent();

      expect(screen.getByText(/Affected Components for CVE-2025-55182/i)).toBeInTheDocument();
    });
  });

  describe('About Section', () => {
    it('should render about vulnerability section', () => {
      renderComponent();

      expect(screen.getByRole('heading', { name: /About the React2Shell Vulnerability/i })).toBeInTheDocument();
      expect(
        screen.getByText(
          /React2Shell is a critical vulnerability affecting certain versions of React Server Components/i
        )
      ).toBeInTheDocument();
    });

    it('should render steps to evaluate and remediate section', () => {
      renderComponent();

      expect(screen.getByRole('heading', { name: /Steps to Evaluate & Remediate/i })).toBeInTheDocument();
      expect(screen.getByText(/Scan for Issues/i)).toBeInTheDocument();
      expect(screen.getByText(/Upgrade to a Fixed Version/i)).toBeInTheDocument();
      expect(screen.getByText(/Re-scan to Confirm/i)).toBeInTheDocument();
    });

    it('should render blog link with correct href', () => {
      renderComponent();

      const blogLink = screen.getByRole('link', {
        name: /Blog: Serious React2Shell Vulnerabilities Require Immediate Attention/i,
      });
      expect(blogLink).toBeInTheDocument();
      expect(blogLink).toHaveAttribute('href', 'https://links.sonatype.com/announcements/react2shell');
    });

    it('should render remediation guide link with correct href', () => {
      renderComponent();

      const remediationLink = screen.getByRole('link', { name: /Remediation Guide/i });
      expect(remediationLink).toBeInTheDocument();
      expect(remediationLink).toHaveAttribute('href', 'https://help.sonatype.com/en/find-and-fix-react2shell.html');
    });

    it('should render CVE link', async () => {
      renderComponent();

      await waitFor(
        () => {
          const cveLink = screen.getByRole('link', { name: 'CVE-2025-55182' });
          expect(cveLink).toBeInTheDocument();
        },
        { timeout: 3000 }
      );
    });

    it('should render cards as accessible regions', () => {
      renderComponent();

      expect(screen.getByRole('region', { name: 'About the React2Shell Vulnerability' })).toBeInTheDocument();
      expect(screen.getByRole('region', { name: 'Steps to Evaluate & Remediate' })).toBeInTheDocument();
    });
  });

  describe('Summary Tiles', () => {
    it('should render all four tiles with correct values and titles', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('5')).toBeInTheDocument();
      });
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();

      expect(screen.getByText('Affected Applications')).toBeInTheDocument();
      expect(screen.getByText('Affected Components')).toBeInTheDocument();
      expect(screen.getByText('Violating Components')).toBeInTheDocument();
      expect(screen.getByText('Active Waivers')).toBeInTheDocument();
    });

    it('should render tiles with zero values', async () => {
      axiosMock.reset();
      const zeroResponse = {
        ...mockApiResponse,
        aggregates: {
          totalAffectedApplications: 0,
          affectedComponents: 0,
          violatingComponents: 0,
          activeWaivers: 0,
        },
      };
      axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, zeroResponse);

      const zeroState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 0,
            affectedComponents: 0,
            violatingComponents: 0,
            activeWaivers: 0,
          },
        },
      };

      renderComponent(zeroState);

      await waitFor(() => {
        const zeroValues = screen.getAllByText('0');
        expect(zeroValues).toHaveLength(4);
      });
    });
  });

  describe('Info Banner', () => {
    it('should render info alert banner with correct message', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
        },
      };

      renderComponent(withDataState);

      expect(
        await screen.findByText(/If your violating component count is lower than your affected component count/i)
      ).toBeInTheDocument();
    });
  });

  describe('Impact Table', () => {
    const mockImpactData = [
      {
        applicationName: 'Test App 1',
        applicationPublicId: 'app-1',
        stage: 'build',
        reportId: 'report-1',
        componentDisplayName: 'test-component',
        version: '1.0.0',
        cveId: 'CVE-2025-55182',
        recommendedAction: 'Upgrade to version 2.0.0',
        activeWaiver: true,
        violating: false,
        evaluation: 'Waived',
        evaluationDate: '2025-01-15T10:00:00Z',
      },
      {
        applicationName: 'Test App 2',
        applicationPublicId: 'app-2',
        stage: 'develop',
        reportId: 'report-2',
        componentDisplayName: 'another-component',
        version: '2.0.0',
        cveId: 'CVE-2025-55182',
        recommendedAction: 'No fix available',
        activeWaiver: false,
        violating: true,
        evaluation: 'Fail',
        evaluationDate: '2025-01-16T10:00:00Z',
      },
    ];

    it('should render empty state when no data available', async () => {
      axiosMock.reset();
      const emptyResponse = {
        ...mockApiResponse,
        aggregates: {
          totalAffectedApplications: 5,
          affectedComponents: 0,
          violatingComponents: 0,
          activeWaivers: 0,
        },
        results: [],
        totalCount: 0,
      };
      axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents/).reply(200, emptyResponse);

      const emptyState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 0,
            violatingComponents: 0,
            activeWaivers: 0,
          },
          impactData: [],
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 0,
            totalPages: 1,
          },
        },
      };

      renderComponent(emptyState);

      await waitFor(() => {
        expect(screen.getByText(/No impact data available/i)).toBeInTheDocument();
      });
      expect(screen.getByText(/Run a scan to identify affected components/i)).toBeInTheDocument();
    });

    it('should render all table column headers', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('Application')).toBeInTheDocument();
      });
      expect(screen.getByText('Stage')).toBeInTheDocument();
      expect(screen.getByText('Component')).toBeInTheDocument();
      expect(screen.getByText('Version')).toBeInTheDocument();
      expect(screen.getByText('CVE ID')).toBeInTheDocument();
      expect(screen.getByText('Recommended Action')).toBeInTheDocument();
      expect(screen.getByText('Active Waiver')).toBeInTheDocument();
      expect(screen.getByText('Violating')).toBeInTheDocument();
      expect(screen.getByText('Evaluation')).toBeInTheDocument();
      expect(screen.getByText('Evaluation Date')).toBeInTheDocument();
    });

    it('should display component data correctly', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('Test App 1')).toBeInTheDocument();
      });
      expect(screen.getByText('Test App 2')).toBeInTheDocument();
      expect(screen.getByText('test-component')).toBeInTheDocument();
      expect(screen.getByText('another-component')).toBeInTheDocument();
      expect(screen.getByText('1.0.0')).toBeInTheDocument();
      expect(screen.getByText('2.0.0')).toBeInTheDocument();
    });

    it('should display stages correctly', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('build')).toBeInTheDocument();
      });
      expect(screen.getByText('develop')).toBeInTheDocument();
    });

    it('should display CVE IDs', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getAllByText('CVE-2025-55182')).toHaveLength(3);
      });
    });

    it('should display View links for reports', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        const viewLinks = screen.getAllByRole('link', { name: 'View' });
        expect(viewLinks).toHaveLength(2);
      });
    });

    it('should display boolean values correctly', async () => {
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      renderComponent(withDataState);

      await waitFor(() => {
        const yesValues = screen.getAllByText('Yes');
        const noValues = screen.getAllByText('No');

        expect(yesValues.length).toBeGreaterThan(0);
        expect(noValues.length).toBeGreaterThan(0);
      });
    });

    it('should handle column sorting when clicking sortable column header', async () => {
      const user = userEvent.setup();
      const sortedResponse = {
        ...mockApiResponse,
        results: mockImpactData,
      };

      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      const { store } = renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('Stage')).toBeInTheDocument();
      });

      // Mock the sort API call
      axiosMock.onGet(/\/api\/v2\/componentSearch\/cveAffectedComponents.*sortBy=stage/).reply(200, sortedResponse);

      const stageHeader = screen.getByText('Stage').closest('th');
      await user.click(stageHeader);

      await waitFor(() => {
        const state = store.getState();
        expect(state.react2Shell.sortBy).toBe('stage');
        expect(state.react2Shell.sortOrder).toBe('asc');
      });

      // Verify the API was called with sort parameters
      await waitFor(() => {
        const sortRequests = axiosMock.history.get.filter(
          (req) => req.url.includes('sortBy=stage') && req.url.includes('sortOrder=asc')
        );
        expect(sortRequests.length).toBeGreaterThan(0);
      });
    });

    it('should not allow sorting on Evaluation column', async () => {
      const user = userEvent.setup();
      const withDataState = {
        ...defaultPreloadedState,
        react2Shell: {
          ...defaultPreloadedState.react2Shell,
          aggregates: {
            affectedApplications: 5,
            affectedComponents: 10,
            violatingComponents: 3,
            activeWaivers: 2,
          },
          impactData: mockImpactData,
          pagination: {
            page: 1,
            pageSize: 10,
            totalItems: 2,
            totalPages: 1,
          },
        },
      };

      const { store } = renderComponent(withDataState);

      await waitFor(() => {
        expect(screen.getByText('Evaluation')).toBeInTheDocument();
      });

      const initialSortBy = store.getState().react2Shell.sortBy;
      const evaluationHeader = screen.getByText('Evaluation').closest('th');
      await user.click(evaluationHeader);

      const finalSortBy = store.getState().react2Shell.sortBy;
      expect(finalSortBy).toBe(initialSortBy);
    });
  });
});
