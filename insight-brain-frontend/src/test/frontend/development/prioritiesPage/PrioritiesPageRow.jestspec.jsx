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

  const minimalProps = {
    component: mockData,
    href: '#testHref',
  };

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    renderComponent = (preloadedState) =>
      render(<PrioritiesPageRow {...minimalProps} />, {
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
    expect(link).toHaveAttribute('href', minimalProps.href);
    expect(link).toHaveTextContent(mockData.displayName);
  });

  it('does not make network requests if developerBulkRecommendations feature flag is enabled', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(0);
  });

  it('makes network requests only if developerBulkRecommendations feature flag is disabled', () => {
    const preloadedState = mergeDeepRight(defaultPreloadedState, {
      productFeatures: {
        productFeatures: {
          'developer-bulk-recommendations': false,
        },
      },
    });

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
    if (mockData.securityReachable) {
      expect(reachabilityCell).toHaveTextContent(mockData.securityReachable ? 'Detected' : 'Not detected');
    }

    const suggestedFixCell = cells[4];
    expect(suggestedFixCell).toHaveTextContent(`Upgrade to ${mockData.remediationVersion}`);
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
        applicationReport: {
          recommendations: {
            [mockData.componentHash]: {
              loading: true,
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
                  versionChanges: [
                    {
                      type: 'next-no-violations',
                      data: {
                        component: {
                          componentIdentifier: {
                            coordinates: {
                              version: '4.5.6',
                            },
                          },
                        },
                      },
                    },
                  ],
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
        const suggestedVersionChange = {
          type: 'recommended-non-breaking-with-dependencies',
          data: {
            component: {
              componentIdentifier: {
                coordinates: {
                  version: '4.5.6',
                },
              },
            },
          },
        };

        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  suggestedVersionChange: suggestedVersionChange,
                  versionChanges: [suggestedVersionChange],
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

      it('renders "Investigate" for the recommendation if there is not a recommended version', async () => {
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
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[4];
        await waitFor(() => expect(cell).toHaveTextContent('Investigate'));
      });

      it('renders "Investigate" for the recommendation if the current version is the recommendation', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {
                  versionChanges: [
                    {
                      type: 'next-no-violations',
                      data: {
                        component: {
                          componentIdentifier: {
                            coordinates: {
                              version: mockData.componentIdentifier.coordinates.version,
                            },
                          },
                        },
                      },
                    },
                  ],
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[4];
        await waitFor(() => expect(cell).toHaveTextContent('Investigate'));
      });

      it('does not render a "Next Step" cell if the manual pull requests feature flag is disabled', async () => {
        const preloadedState = mergeDeepRight(asyncRecPreloadedState, {
          productFeatures: {
            productFeatures: {
              'manual-pull-requests': false,
            },
          },
          prioritiesPage: {
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {},
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_POSSIBLE',
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.queryAllByRole('cell')[5];
        expect(cell).toBeUndefined();
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
            recommendations: {
              [mockData.componentHash]: {
                loading: false,
                error: null,
                remediation: {},
                automatedRemediationStatus: {
                  status: 'MANUAL_PULL_REQUEST_POSSIBLE',
                },
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        const button = within(cell).getByRole('button');
        expect(button).not.toHaveClass('disabled');
        expect(button).toHaveTextContent('Create PR');

        await user.hover(button);
        await expect(screen.findByRole('tooltip')).rejects.toThrow();
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
                remediation: {},
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
                remediation: {},
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

      it('does not render a "Create PR" button if a manual pull request is not possible', async () => {
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
                remediation: {},
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
        expect(cell).toHaveTextContent('—');
        const button = within(cell).queryByRole('button');
        expect(button).toBeNull();
      });

      it('does not render a "Create PR" button if a manual pull request is not possible due to no data being returned', async () => {
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
                remediation: {},
              },
            },
          },
        });
        renderComponent(preloadedState);

        const cell = screen.getAllByRole('cell')[5];
        expect(cell).toHaveTextContent('—');
        const button = within(cell).queryByRole('button');
        expect(button).toBeNull();
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
    securityReachable: faker.datatype.boolean(),
    remediationType: 'next-non-failing',
    remediationVersion: '1.0',
  };
}
