/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { faker } from '@faker-js/faker';
import { mergeDeepRight } from 'ramda';
import userEvent from '@testing-library/user-event';
import { render, screen, within, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { getVersionGraphUrl } from 'MainRoot/util/CLMLocation';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { dependencyTypeMap } from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';

const publicAppId = 'testPublicAppId';
const scanId = 'testScanId';
const stageId = 'build';

const mockData = generateMockData();

describe('PrioritiesPageRow', () => {
  let renderComponent, axiosMock;

  const defaultPreloadedState = {
    router: {
      currentParams: {
        publicAppId,
        scanId,
      },
      currentState: {
        name: 'prioritiesPageFromDashboard',
      },
    },
    productFeatures: {
      productFeatures: {
        'developer-bulk-recommendations': true,
      },
    },
    applicationReport: {
      metadata: {
        stageId: 'build',
      },
      recommendations: {},
    },
  };

  const bulkOffState = mergeDeepRight(defaultPreloadedState, {
    productFeatures: {
      productFeatures: {
        'developer-bulk-recommendations': false,
      },
    },
  });

  const minimalProps = {
    component: mockData,
    componentHref: '#testHref',
    violationsHref: '#testViolationsHref',
    latestBuildPrioritiesHref: '#testPrioritiesHref',
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    renderComponent = (preloadedState, componentOverride) =>
      render(<PrioritiesPageRow {...minimalProps} component={componentOverride ?? minimalProps.component} />, {
        preloadedState: preloadedState || defaultPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });
  });

  it('renders a row', () => {
    renderComponent();

    const row = screen.getByRole('row');
    expect(row).toBeInTheDocument();
  });

  it('renders a link for the component with the specified href', () => {
    renderComponent();

    const link = within(screen.getAllByRole('cell')[1]).getByRole('link');
    expect(link).toHaveAttribute('href', minimalProps.componentHref);
    expect(link).toHaveTextContent(mockData.displayName);
  });

  it('does not make network requests if developerBulkRecommendations feature flag is enabled', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(0);
  });

  it('makes network requests only if developerBulkRecommendations feature flag is disabled', () => {
    const preloadedState = bulkOffState;

    const requestData = {
      clientType: 'ci',
      ownerType: 'application',
      ownerId: publicAppId,
      matchState: 'exact',
      proprietary: 'false',
      identificationSource: 'Sonatype',
      componentIdentifier: mockData.componentIdentifier
        ? stringifyComponentIdentifier(mockData.componentIdentifier, 'exact')
        : null,
      hash: mockData.componentHash,
      scanId,
      displayName: mockData.displayName,
      stageId,
      dependencyType: dependencyTypeMap[mockData.dependencyType],
    };
    renderComponent(preloadedState);
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getVersionGraphUrl(requestData));
  });

  it('renders correct data', () => {
    renderComponent();

    const row = screen.getByRole('row');
    expect(row).toBeInTheDocument();

    const cells = within(row).getAllByRole('cell');

    const priorityCell = cells[0];
    expect(priorityCell).toHaveTextContent(mockData.priority);

    const componentCell = cells[1];
    expect(componentCell).toHaveTextContent(mockData.displayName);
    if (mockData.dependencyType === 'Direct') {
      expect(componentCell).toHaveTextContent(/^D/);
    } else if (mockData.dependencyType === 'Transitive') {
      expect(componentCell).toHaveTextContent(/^T/);
    } else if (mockData.dependencyType === 'Inner Source') {
      expect(componentCell).toHaveTextContent(/^IS/);
    } else {
      // NOTE: assumes displayName has no regex special chars
      expect(componentCell).toHaveTextContent(new RegExp(`^${mockData.displayName}$`));
    }

    const buildActionCell = cells[2];
    if (mockData.action !== 'none') {
      expect(buildActionCell).toHaveTextContent(mockData.action);
    }

    const reachabilityCell = cells[3];
    expect(reachabilityCell).toHaveTextContent(
      mockData.securityReachable == null ? '-' : mockData.securityReachable ? 'Reachable' : 'Not Reachable'
    );

    const suggestedFixCell = cells[4];
    expect(suggestedFixCell).toHaveTextContent(`Upgrade to ${mockData.remediationVersion}`);
  });

  it('renders "Waived" in build action column', async () => {
    const renderComponent = (preloadedState, props = minimalProps) =>
      render(<PrioritiesPageRow {...props} />, {
        preloadedState: preloadedState || defaultPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });
    const allViolationsWaivedComponentMockData = mergeDeepRight(minimalProps, {
      component: {
        isAllViolationsWaived: true,
      },
    });

    const allViolationsWaivedContainer = renderComponent(defaultPreloadedState, allViolationsWaivedComponentMockData)
      .container;
    const allViolationsWaivedCell = within(allViolationsWaivedContainer).getAllByRole('cell')[2];
    await waitFor(() => expect(allViolationsWaivedCell).toHaveTextContent('Waived'));
  });

  it('renders "Resolve on default branch" in suggested remediation column', async () => {
    const renderComponent = (preloadedState, props = minimalProps) =>
      render(<PrioritiesPageRow {...props} />, {
        preloadedState: preloadedState || defaultPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });
    const allViolationsWaivedComponentMockData = mergeDeepRight(minimalProps, {
      component: {
        hasSameViolationsOnMain: true,
      },
    });

    renderComponent(defaultPreloadedState, allViolationsWaivedComponentMockData);

    const row = screen.getByRole('row');
    const cells = within(row).getAllByRole('cell');

    expect(cells[4]).toHaveTextContent(/resolve on default branch/i);
    expect(cells[5]).toHaveTextContent(/go to build stage/i);
    expect(screen.getByRole('link', { name: /go to build stage/i })).toHaveAttribute('href', '#testPrioritiesHref');
  });

  describe('async recommendations', () => {
    const asyncRecPreloadedState = mergeDeepRight(defaultPreloadedState, {
      productFeatures: {
        productFeatures: {
          'developer-bulk-recommendations': false,
        },
      },
    });

    const renderComponent = (preloadedState, props = minimalProps) =>
      render(<PrioritiesPageRow {...props} />, {
        preloadedState: preloadedState || asyncRecPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

    it('renders a loading spinner for the recommendation before it has loaded', () => {
      const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
        prioritiesPage: {
          recommendations: {
            [mockData.componentHash]: {
              loading: true,
              error: null,
              remediation: null,
            },
          },
        },
      });

      renderComponent(preloadedState);

      const cell = screen.getAllByRole('cell')[4];
      expect(within(cell).getByRole('status')).toHaveTextContent('Loading…');
    });

    describe('once loaded', () => {
      // Note: recommendatation processing logic is complex. This test covers only one case
      it('renders "Upgrade to {version}" for the recommendation', () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  type: 'next-no-violations',
                  version: '4.5.6',
                  isGolden: false,
                  breakingChangesCount: 0,
                },
              },
            },
          },
        });

        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[4];
        expect(cell).toHaveTextContent('Upgrade to 4.5.6');
      });

      it('renders an image named "Golden Version" if the recommendation is non-breaking with dependencies', () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  type: 'recommended-non-breaking-with-dependencies',
                  version: '4.5.6',
                  isGolden: true,
                  breakingChangesCount: 0,
                },
              },
            },
          },
        });

        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[4];
        expect(cell).toHaveTextContent('Upgrade to 4.5.6');
        expect(within(cell).getByRole('img', { name: 'Golden Version' })).toBeInTheDocument();
      });

      it('renders "Waive violations" for the recommendation if non-reachable and no recommended version', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'developer-bulk-recommendations': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: null,
              },
            },
          },
        });

        const nonReachableComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            securityReachable: false,
            remediationType: null,
            remediationVersion: null,
          },
        });

        const nonReachableContainer = renderComponent(preloadedState, nonReachableComponentMockData).container;
        const nonReachableCell = within(nonReachableContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(nonReachableCell).toHaveTextContent('Waive violations'));
      });

      it('renders "waived violations" for the recommendation if all violations are waived', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'developer-bulk-recommendations': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: null,
              },
            },
          },
        });

        const waivedViolationsComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            isAllViolationsWaived: true,
            waivedViolationsCount: 3,
            hasAutoWaiver: true,
          },
        });

        const waivedViolationsContainer = renderComponent(preloadedState, waivedViolationsComponentMockData).container;
        const waivedViolationsCell = within(waivedViolationsContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(waivedViolationsCell).toHaveTextContent('3 waived violations'));
      });

      it('renders "Investigate" for the recommendation if component is unknown', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: null,
              },
            },
          },
        });

        const nullComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            componentIdentifier: null,
          },
        });

        const unknownComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            dependencyType: 'Unknown',
          },
        });

        const nullContainer = renderComponent(preloadedState, nullComponentMockData).container;
        const unknownContainer = renderComponent(preloadedState, unknownComponentMockData).container;

        const nullCell = within(nullContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(nullCell).toHaveTextContent('Investigate'));

        const unknownCell = within(unknownContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(unknownCell).toHaveTextContent('Investigate'));
      });

      it('renders "Investigate" for the recommendation if reachable and there is not a recommended version', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: null,
              },
            },
          },
        });

        const reachableComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            securityReachable: true,
          },
        });

        const reachableComponentContainer = renderComponent(preloadedState, reachableComponentMockData).container;
        const reachableComponentCell = within(reachableComponentContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(reachableComponentCell).toHaveTextContent('Investigate'));
      });

      it('renders "Investigate" for the recommendation if reachable and current version is the recommendation', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  type: 'next-no-violations',
                  version: mockData.componentIdentifier.coordinates.version,
                  isGolden: false,
                  breakingChangesCount: 0,
                },
              },
            },
          },
        });

        const reachableComponentMockData = mergeDeepRight(minimalProps, {
          component: {
            securityReachable: true,
          },
        });

        const reachableComponentContainer = renderComponent(preloadedState, reachableComponentMockData).container;
        const reachableComponentCell = within(reachableComponentContainer).getAllByRole('cell')[4];
        await waitFor(() => expect(reachableComponentCell).toHaveTextContent('Investigate'));
      });

      it('renders a "Create PR" button if a manual pull request is possible', async () => {
        const user = userEvent.setup();

        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': true,
            },
          },
          prioritiesPage: {
            priorities: [mockData],
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  version: '4.5.6',
                  type: 'next-no-violations',
                  isGolden: false,
                  breakingChangesCount: 0,
                },
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_POSSIBLE',
                },
                componentDisplayName: mockData.displayName,
              },
            },
            visibleCreatePRModalComponentHash: null,
            branchName: 'main',
          },
          createPRModal: {
            isModalOpen: false,
            name: null,
            fullName: null,
            currentVersion: null,
            targetVersion: null,
            breakingChangesCount: null,
            defaultBranch: null,
            scanId: null,
            identificationSource: null,
            componentHash: null,
            componentIdentifier: {},
            submitMaskState: null,
            error: null,
          },
          applicationReport: {
            metadata: {
              application: {
                id: 'test-app-id',
              },
              stageId: 'build',
            },
          },
        });

        const { store } = renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        const button = within(cell).getByRole('button');

        await user.click(button);

        await waitFor(() => {
          const state = store.getState();
          expect(state.prioritiesPage.visibleCreatePRModalComponentHash).toBe(mockData.componentHash);
          expect(state.createPRModal.isModalOpen).toBe(true);
        });

        await waitFor(() => {
          expect(screen.getByRole('dialog')).toBeInTheDocument();
        });

        expect(screen.getByText('Create Pull Request')).toBeInTheDocument();
        expect(screen.getByText(`Bump ${mockData.displayName} to 4.5.6`)).toBeInTheDocument();
        expect(screen.getByText('4.5.6')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create' })).toBeInTheDocument();
      });

      it('renders a disabled "Create PR" button if a manual pull request is possible but disabled', async () => {
        const user = userEvent.setup();

        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  version: '4.5.6',
                  type: 'next-no-violations',
                  isGolden: false,
                  breakingChangesCount: 0,
                },
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_NOT_POSSIBLE',
                  reason: 'CONFIGURATION_DISABLED',
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        const button = within(cell).getByRole('button');
        expect(button).toHaveClass('disabled');
        expect(button).toHaveTextContent('Create PR');

        await user.hover(button);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Manual Pull Requests are disabled');
      });

      it('renders a disabled "Create PR" button if a manual pull request is possible but SCM is not configured', async () => {
        const user = userEvent.setup();

        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  version: '4.5.6',
                  type: 'next-no-violations',
                  isGolden: false,
                  breakingChangesCount: 0,
                },
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_NOT_POSSIBLE',
                  reason: 'SCM_NOT_CONFIGURED',
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        const button = within(cell).getByRole('button');
        expect(button).toHaveClass('disabled');
        expect(button).toHaveTextContent('Create PR');

        await user.hover(button);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Source Control is not configured');
      });

      it('renders "view violations" link if a manual pull request is not possible', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  version: '4.5.6',
                  type: 'next-no-violations',
                  isGolden: false,
                  breakingChangesCount: 0,
                },
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_NOT_POSSIBLE',
                  reason: 'UNSUPPORTED_STAGE',
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        expect(cell).toHaveTextContent('View Violations');
        const link = within(cell).getByRole('link', { name: 'View Violations' });
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', minimalProps.violationsHref);
        const button = within(cell).queryByRole('button');
        expect(button).toBeNull();
      });

      it('renders "view violations" link if a manual pull request is not possible due to no data being returned', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': true,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: [],
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        expect(cell).toHaveTextContent('View Violations');
        const link = within(cell).getByRole('link', { name: 'View Violations' });
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', minimalProps.violationsHref);
        const button = within(cell).queryByRole('button');
        expect(button).toBeNull();
      });
    });
  });

  describe('dependency type is set correctly to send PR creation request', () => {
    const asyncRecPreloadedState = mergeDeepRight(defaultPreloadedState, {
      productFeatures: {
        productFeatures: {
          'developer-bulk-recommendations': false,
        },
      },
    });

    const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
      productFeatures: {
        productFeatures: {
          'manual-pull-requests': true,
        },
      },
      prioritiesPage: {
        priorities: [mockData],
        recommendations: {
          [mockData.componentHash]: {
            loading: false,
            error: null,
            remediation: {
              version: '4.5.6',
              type: 'next-no-violations',
              isGolden: false,
              breakingChangesCount: 0,
            },
            automatedRemediationStatus: {
              status: 'MANUAL_PULL_REQUEST_POSSIBLE',
            },
            componentDisplayName: mockData.displayName,
          },
        },
        visibleCreatePRModalComponentHash: null,
        branchName: 'main',
      },
      applicationReport: {
        metadata: {
          application: {
            id: 'test-app-id',
          },
          stageId: 'build',
        },
      },
    });

    it('sets direct dependency type correctly', async () => {
      const user = userEvent.setup();
      const directDependencyMockData = {
        ...mockData,
        dependencyType: 'Direct',
      };

      const props = {
        ...minimalProps,
        component: directDependencyMockData,
      };

      const { store } = render(<PrioritiesPageRow {...props} />, {
        preloadedState: preloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const cell = screen.getAllByRole('cell')[5];
      const button = within(cell).getByRole('button');

      await user.click(button);

      await waitFor(() => {
        const state = store.getState();
        expect(state.prioritiesPage.visibleCreatePRModalComponentHash).toBe(mockData.componentHash);
        expect(state.createPRModal.isModalOpen).toBe(true);
        expect(state.createPRModal.isDirectDependency).toBe(true);
      });

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    it('sets inner source direct dependency type correctly', async () => {
      const user = userEvent.setup();
      const directDependencyMockData = {
        ...mockData,
        dependencyType: 'Inner Source Direct',
      };

      const props = {
        ...minimalProps,
        component: directDependencyMockData,
      };

      const { store } = render(<PrioritiesPageRow {...props} />, {
        preloadedState: preloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const cell = screen.getAllByRole('cell')[5];
      const button = within(cell).getByRole('button');

      await user.click(button);

      await waitFor(() => {
        const state = store.getState();
        expect(state.prioritiesPage.visibleCreatePRModalComponentHash).toBe(mockData.componentHash);
        expect(state.createPRModal.isModalOpen).toBe(true);
        expect(state.createPRModal.isDirectDependency).toBe(true);
      });

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });
  });

  describe('inner source dependency types', () => {
    it('renders Inner Source dependency type indicator correctly', () => {
      const innerSourceMockData = {
        ...mockData,
        dependencyType: 'Inner Source',
      };

      const props = {
        ...minimalProps,
        component: innerSourceMockData,
      };

      render(<PrioritiesPageRow {...props} />, {
        preloadedState: defaultPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const componentCell = screen.getAllByRole('cell')[1];
      expect(componentCell).toHaveTextContent(/IS/);
    });

    it('renders Inner Source Direct dependency type indicator correctly', () => {
      const innerSourceDirectMockData = {
        ...mockData,
        dependencyType: 'Inner Source Direct',
      };

      const props = {
        ...minimalProps,
        component: innerSourceDirectMockData,
      };

      render(<PrioritiesPageRow {...props} />, {
        preloadedState: defaultPreloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const componentCell = screen.getAllByRole('cell')[1];
      expect(componentCell).toHaveTextContent(/^DIS/);
    });

    it('correctly maps Inner Source dependency types', () => {
      expect(dependencyTypeMap['Inner Source']).toBe('inner-source');
      expect(dependencyTypeMap['Inner Source Direct']).toBe('direct');
      expect(dependencyTypeMap['Inner Source Transitive']).toBe('transitive');
    });
  });

  describe('inner source remediation types', () => {
    const asyncRecPreloadedState = mergeDeepRight(defaultPreloadedState, {
      productFeatures: {
        productFeatures: {
          'developer-bulk-recommendations': false,
        },
      },
    });

    it('renders "Upgrade to {version}" for innersource-latest-non-breaking recommendation', () => {
      const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
        prioritiesPage: {
          recommendations: {
            [mockData.componentHash]: {
              loading: false,
              error: null,
              remediation: {
                type: 'innersource-latest-non-breaking',
                version: '1.2.3',
                isGolden: false,
                breakingChangesCount: 0,
              },
            },
          },
        },
      });

      render(<PrioritiesPageRow {...minimalProps} />, {
        preloadedState: preloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const cell = screen.getAllByRole('cell')[4];
      expect(cell).toHaveTextContent('Upgrade to 1.2.3');
    });

    it('renders "Upgrade to {version}" for innersource-latest recommendation', () => {
      const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
        prioritiesPage: {
          recommendations: {
            [mockData.componentHash]: {
              loading: false,
              error: null,
              remediation: {
                type: 'innersource-latest',
                version: '2.0.0',
                isGolden: false,
                breakingChangesCount: 3,
              },
            },
          },
        },
      });

      render(<PrioritiesPageRow {...minimalProps} />, {
        preloadedState: preloadedState,
        container: document.body.appendChild(
          document.createElement('table').appendChild(document.createElement('tbody'))
        ),
      });

      const cell = screen.getAllByRole('cell')[4];
      expect(cell).toHaveTextContent('Upgrade to 2.0.0');
    });
  });

  describe('version-scoring fetch gate on remediationType (CLM-40771)', () => {
    it('does NOT fire version-scoring when remediationType is null and bulk flag is OFF', async () => {
      axiosMock.reset();
      renderComponent(bulkOffState, { ...mockData, remediationType: null, remediationVersion: null });

      await waitFor(() => {
        const versionScoringCalls = axiosMock.history.get.filter((c) => /allVersions/.test(c.url));
        expect(versionScoringCalls).toHaveLength(0);
      });
    });

    it('DOES fire version-scoring when remediationType is actionable and bulk flag is OFF', async () => {
      axiosMock.reset();
      axiosMock.onGet(/allVersions/).reply(200, {});
      renderComponent(bulkOffState, { ...mockData, remediationType: 'next-non-failing', remediationVersion: '1.5' });

      await waitFor(() => {
        const calls = axiosMock.history.get.filter((c) => /allVersions/.test(c.url));
        expect(calls.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('does NOT fire version-scoring when bulk flag is ON, regardless of remediationType', async () => {
      axiosMock.reset();
      renderComponent(defaultPreloadedState, {
        ...mockData,
        remediationType: 'next-non-failing',
        remediationVersion: '1.5',
      });

      await waitFor(() => {
        const calls = axiosMock.history.get.filter((c) => /allVersions/.test(c.url));
        expect(calls).toHaveLength(0);
      });
    });

    // Regression for the missing-dependency bug: the gate effect reads remediationType
    // but the deps array originally only listed isDeveloperBulkRecommendationsEnabled.
    // A row that mounted as non-actionable (null) and was later upgraded to actionable
    // would never fire version-scoring because the effect's closure captured the stale
    // null. With remediationType in the deps array, the upgrade triggers a re-run.
    it('fires version-scoring after remediationType flips from null to actionable on re-render', async () => {
      axiosMock.reset();
      axiosMock.onGet(/allVersions/).reply(200, {});

      const { rerender } = renderComponent(bulkOffState, {
        ...mockData,
        remediationType: null,
        remediationVersion: null,
      });

      await waitFor(() => {
        const initialCalls = axiosMock.history.get.filter((c) => /allVersions/.test(c.url));
        expect(initialCalls).toHaveLength(0);
      });

      rerender(
        <PrioritiesPageRow
          {...minimalProps}
          component={{ ...mockData, remediationType: 'next-non-failing', remediationVersion: '1.5' }}
        />
      );

      await waitFor(() => {
        const calls = axiosMock.history.get.filter((c) => /allVersions/.test(c.url));
        expect(calls.length).toBeGreaterThanOrEqual(1);
      });
    });
  });
});

function generateMockData() {
  const hasFail = faker.datatype.boolean();
  const componentHash = faker.git.commitSha();

  return {
    displayName: faker.lorem.word(1),
    componentIdentifier: {
      format: faker.datatype.string(),
      coordinates: {
        artifactId: faker.datatype.string(),
        classifier: faker.datatype.string(),
        extension: faker.datatype.string(),
        groupId: faker.datatype.string(),
        version: '0.5',
      },
    },
    componentHash,
    dependencyType: faker.helpers.arrayElement(['Direct', 'Transitive', 'Inner Source']),
    hasFailActionOnComponent: hasFail,
    action: hasFail ? 'fail' : faker.helpers.arrayElement(['none', 'warn']),
    highestThreat: faker.datatype.number({ min: 0, max: 10 }),
    highestThreatPolicyName: faker.lorem.slug(),
    highestThreatPolicyConstraintName: faker.lorem.sentence(),
    priority: 1,
    securityReachable: faker.helpers.arrayElement([true, false, null]),
    remediationType: 'next-non-failing',
    remediationVersion: '1.0',
    hasExpiredWaiver: false,
    hasSoonToExpireWaiver: false,
    isAllViolationsWaived: false,
    waiverExpirationDetails: null,
    waivedViolationsCount: null,
    hasAutoWaiver: false,
  };
}
