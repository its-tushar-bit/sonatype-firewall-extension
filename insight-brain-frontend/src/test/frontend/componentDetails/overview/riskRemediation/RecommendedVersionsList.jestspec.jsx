/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { mergeDeepRight } from 'ramda';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import { screen, fireEvent, within } from '@testing-library/react';
import { RecommendedVersionsList } from 'MainRoot/componentDetails/overview/riskRemediation/RecommendedVersionsList';
import userEvent from '@testing-library/user-event';
import { actions } from 'MainRoot/componentDetails/overview/overviewSlice';
import { getCreatePullRequestUrl } from 'MainRoot/util/CLMLocation';

describe('RecommendedVersionsList', () => {
  let minimalProps, renderComponent, handleCompareMock, axiosMock;
  let defaultPreloadedState;

  beforeEach(function () {
    handleCompareMock = jest.fn();
    axiosMock = axiosMockAdapter();

    defaultPreloadedState = {
      componentDetailsOverview: {
        versionExplorerData: {
          versions: [],
          remediation: {},
          automatedRemediationStatus: null,
        },
      },
      router: {
        currentParams: {
          scanId: 'scan-id',
          hash: 'hash',
        },
      },
      applicationReport: {
        metadata: {
          application: {
            id: 'appId',
          },
        },
        selectedReport: {
          allEntries: [
            {
              derivedComponentName: 'logback-access',
              componentIdentifier: {
                coordinates: {
                  artifactId: 'logback-access',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'ch.qos.logback',
                  version: '2.4.9',
                },
              },
              hash: 'hash',
              artifactId: 'logback-access',
              identificationSource: 'Sonatype',
            },
          ],
        },
      },
      createPRModal: {
        isModalOpen: false,
        targetVersion: null,
      },
    };

    minimalProps = {
      actualVersion: '2.4.9',
      handleCompare: handleCompareMock,
    };

    renderComponent = (props, preloadedState) =>
      render(<RecommendedVersionsList {...minimalProps} {...props} />, {
        preloadedState: preloadedState ? { ...defaultPreloadedState, ...preloadedState } : defaultPreloadedState,
      });
  });

  it('renders an "empty list" message when no versions are available', () => {
    const versionChanges = [];
    renderComponent({ versionChanges });
    expect(screen.getByText('There are no suggested versions for this component')).toBeInTheDocument();
  });

  it('renders an "empty list" message when version no remediation array is sent', () => {
    const versionChanges = [
      {
        id: 'no-versions-available',
        text: 'There are no suggested versions for this component',
      },
    ];
    renderComponent({ versionChanges });
    expect(screen.getByText('There are no suggested versions for this component')).toBeInTheDocument();
  });

  it('calls handleCompare on Compare button click', () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    renderComponent({ versionChanges });
    const compareBtn = screen.getByRole('button', { name: 'Compare' });

    fireEvent.click(compareBtn);

    expect(handleCompareMock).toHaveBeenCalledWith('2.4.10');
  });

  it('does not render an accordion with alternate versions if remediation array has only 1 valid remediation', () => {
    const versionChanges = [
      {
        id: 'next-no-violations-with-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: 'next-no-violations-with-dependencies',
        version: '2.4.11',
      },
    ];
    renderComponent({ versionChanges });

    const versionContainers = screen.getAllByRole('list');
    expect(versionContainers.length).toBe(1);

    expect(screen.queryByRole('group')).not.toBeInTheDocument();
    expect(screen.queryByText('Alternate Versions')).not.toBeInTheDocument();
  });

  it('renders an accordion with alternate versions if remediation array has more than 1 valid remediations', () => {
    const versionChanges = [
      {
        id: 'next-no-violations-with-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: 'next-no-violations-with-dependencies',
        version: '2.4.11',
      },
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];
    renderComponent({ versionChanges });

    const versionContainers = screen.getAllByRole('list');
    expect(versionContainers.length).toBe(2);

    const accordion = screen.getByRole('group');
    expect(accordion).toBeInTheDocument();
    expect(within(accordion).getByText('Alternate Versions')).toBeInTheDocument();
    expect(within(accordion).getByText('Version 2.4.10')).toBeInTheDocument();
  });

  it('renders version list items if remediation array with valid remediation is sent', () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
      {
        id: 'next-no-violation-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: 'next-non-failing-with-dependencies',
        version: '2.4.11',
      },
      {
        id: 'next-non-failing-with-dependencies',
        text: 'The current version has no policy violations for this component and its dependencies',
        type: 'next-non-failing-with-dependencies',
        version: '2.4.9',
      },
    ];
    renderComponent({ versionChanges });

    const listElements = screen
      .getAllByRole('listitem')
      .filter((element) => element.classList.contains('iq-version-item'));
    expect(listElements.length).toBe(2);

    const firstElement = listElements[0];
    expect(firstElement).toBeInTheDocument();

    expect(within(firstElement).getByText('Upgrade to 2.4.10')).toBeInTheDocument();
    expect(within(firstElement).getByText('Next version with no policy violation')).toBeInTheDocument();
    const firstElementCompareBtn = within(firstElement).getByRole('button', { name: 'Compare' });
    fireEvent.click(firstElementCompareBtn);
    expect(handleCompareMock).toHaveBeenCalledWith('2.4.10');

    const secondElement = listElements[1];
    expect(secondElement).toBeInTheDocument();
    expect(within(secondElement).getByText('Version 2.4.11')).toBeInTheDocument();
    expect(
      within(secondElement).getByText('Next version with no policy violations for this component and its dependencies')
    ).toBeInTheDocument();
    const secondElementCompareBtn = within(secondElement).getByRole('button', { name: 'Compare' });
    fireEvent.click(secondElementCompareBtn);
    expect(handleCompareMock).toHaveBeenCalledWith('2.4.11');
  });

  it('renders golden version list item with golden version text, image and checklist if version is golden', () => {
    const versionChanges = [
      {
        id: 'recommended-non-breaking-with-dependencies-version',
        text: 'No breaking changes, No policy violations for this component, No policy violations for its dependencies',
        type: 'recommended-non-breaking-with-dependencies',
        version: '2.5.0',
        isGolden: true,
      },
      {
        id: 'recommended-non-breaking--version',
        text: 'No breaking changes, No policy violations for this component',
        type: 'recommended-non-breaking',
        version: '2.4.12',
        isGolden: false,
      },
      {
        id: 'next-no-fail-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: 'next-non-failing-with-dependencies',
        version: '2.4.11',
        isGolden: false,
      },
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violations',
        type: 'next-no-violations',
        version: '2.4.10',
        isGolden: false,
      },
    ];
    renderComponent({ versionChanges });

    const versions = screen.getAllByRole('listitem').filter((version) => version.classList.contains('iq-version-item'));
    expect(versions.length).toBe(4);

    const firstVersion = versions[0];
    expect(within(firstVersion).getByText('Upgrade to 2.5.0')).toBeInTheDocument();
    expect(within(firstVersion).getByText('Golden Version')).toBeInTheDocument();
    expect(within(firstVersion).getByRole('img')).toBeInTheDocument();

    const goldenVersionChecklist = within(firstVersion).getAllByRole('listitem');
    expect(goldenVersionChecklist.length).toBe(3);
    expect(goldenVersionChecklist[0]).toHaveTextContent('No breaking changes');
    expect(goldenVersionChecklist[1]).toHaveTextContent('No policy violations for this component');
    expect(goldenVersionChecklist[2]).toHaveTextContent('No policy violations for its dependencies');

    const secondVersion = versions[1];
    expect(within(secondVersion).getByText('Version 2.4.12')).toBeInTheDocument();
    expect(within(secondVersion).queryByText('Golden Version')).not.toBeInTheDocument();
    expect(within(secondVersion).queryByRole('img')).not.toBeInTheDocument();

    const secondVersionChecklist = within(secondVersion).getAllByRole('listitem');
    expect(secondVersionChecklist.length).toBe(2);
    expect(secondVersionChecklist[0]).toHaveTextContent('No breaking changes');
    expect(secondVersionChecklist[1]).toHaveTextContent('No policy violations for this component');

    const thirdVersion = versions[2];
    expect(within(thirdVersion).getByText('Version 2.4.11')).toBeInTheDocument();
    expect(within(thirdVersion).queryByText('Golden Version')).not.toBeInTheDocument();
    expect(within(thirdVersion).queryByRole('img')).not.toBeInTheDocument();
    expect(within(thirdVersion).queryByRole('listitem')).not.toBeInTheDocument();
    expect(
      within(thirdVersion).getByText('Next version with no policy violations for this component and its dependencies')
    ).toBeInTheDocument();

    const fourthVersion = versions[3];
    expect(within(fourthVersion).getByText('Version 2.4.10')).toBeInTheDocument();
    expect(within(fourthVersion).queryByText('Golden Version')).not.toBeInTheDocument();
    expect(within(fourthVersion).queryByRole('img')).not.toBeInTheDocument();
    expect(within(fourthVersion).queryByRole('listitem')).not.toBeInTheDocument();
    expect(within(fourthVersion).getByText('Next version with no policy violations')).toBeInTheDocument();
  });

  it('renders PRStatus component for suggested version', () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
      {
        id: 'next-no-violation-dependencies-version',
        text: 'Next version with no policy violations for this component and its dependencies',
        type: 'next-non-failing-with-dependencies',
        version: '2.4.11',
      },
    ];

    const automatedRemediationStatus = {
      status: 'MANUAL_PULL_REQUEST_POSSIBLE',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    // Suggested version section
    const firstVersionItem = screen.getAllByRole('listitem')[0];
    expect(within(firstVersionItem).getByText('Create PR')).toBeVisible();

    // PRStatus should not appear in alternate versions section
    const secondVersionItem = screen.getAllByRole('listitem')[1];
    expect(within(secondVersionItem).queryByText('Create PR')).not.toBeInTheDocument();
  });

  it('calls the right action when Create PR button is clicked', async () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations-with-dependencies',
        version: '2.4.10',
      },
    ];

    const allVersions = [
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'logback-access',
            classifier: '',
            extension: 'jar',
            groupId: 'ch.qos.logback',
            version: '2.4.10',
          },
        },
      },
    ];

    const remediations = [
      {
        type: 'next-no-violations-with-dependencies',
        data: {
          component: {
            packageUrl: 'pkg:maven/ch.qos.logback/logback-access@2.4.10?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '2.4.10',
              },
            },
            displayName: 'ch.qos.logback : logback-access : 2.4.10',
          },
        },
      },
    ];

    const user = userEvent.setup();

    const automatedRemediationStatus = {
      status: 'MANUAL_PULL_REQUEST_POSSIBLE',
    };

    const preloadedState = mergeDeepRight(defaultPreloadedState, {
      componentDetailsOverview: {
        versionExplorerData: {
          versions: allVersions,
          remediation: {
            versionChanges: remediations,
          },
          automatedRemediationStatus: automatedRemediationStatus,
        },
      },
      applicationReport: {
        selectedReport: {
          allEntries: defaultPreloadedState.applicationReport.selectedReport.allEntries.map((entry) => ({
            ...entry,
            directDependency: true,
          })),
        },
      },
    });

    let { store } = renderComponent({ versionChanges, automatedRemediationStatus }, preloadedState);
    expect(store.getState().createPRModal.isModalOpen).toEqual(false);

    const createPRButton = screen.getByRole('button', { name: 'Create PR' });
    await user.click(createPRButton);

    expect(store.getState().createPRModal.isModalOpen).toEqual(true);
    expect(store.getState().createPRModal.currentVersion).toEqual('2.4.9');
    expect(store.getState().createPRModal.targetVersion).toEqual('2.4.10');
    expect(store.getState().createPRModal.isDirectDependency).toEqual(true);
  });

  it('renders an existing PR link when automatedRemediationStatus is PULL_REQUEST', async () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'PULL_REQUEST',
      url: 'https://github.com/repository/pull/123',
      pullRequestId: 123,
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    const viewPRLink = screen.getByRole('link', { name: 'PR #123' });
    expect(viewPRLink).toBeVisible();
    const href = viewPRLink.getAttribute('href');
    expect(href).toContain(automatedRemediationStatus.url);
  });

  it('renders loading spinner when automatedRemediationStatus is PULL_REQUEST_CREATION_PENDING', async () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'PULL_REQUEST_CREATION_PENDING',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    const loadingSpinner = screen.queryByRole('status');
    expect(loadingSpinner).toBeVisible();
    expect(loadingSpinner).toHaveTextContent('Creating PR…');
  });

  it('renders retry button when automatedRemediationStatus is PULL_REQUEST_CREATION_FAILED', async () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'PULL_REQUEST_CREATION_FAILED',
      reason: 'Network error',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    expect(retryButton).toBeVisible();
  });

  it('renders disabled Create PR button with tooltip when manual PRs are not possible due to SCM not configured', async () => {
    const user = userEvent.setup();
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'MANUAL_PULL_REQUEST_NOT_POSSIBLE',
      reason: 'SCM_NOT_CONFIGURED',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    const disabledButton = screen.getByRole('button', { name: 'Create PR' });
    expect(disabledButton).toBeVisible();
    expect(disabledButton).toHaveClass('disabled');
    await user.hover(disabledButton);
    const tooltip = await screen.findByRole('tooltip');
    expect(within(tooltip).getByText('Source Control is not configured')).toBeInTheDocument();
  });

  it('renders nothing when PR status is in hidden reasons list', async () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'MANUAL_PULL_REQUEST_NOT_POSSIBLE',
      reason: 'UNSUPPORTED_FORMAT',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    const createPRButton = screen.queryByRole('button', { name: 'Create PR' });
    expect(createPRButton).not.toBeInTheDocument();
  });

  it('starts polling when automatedRemediationStatus has PULL_REQUEST_CREATION_PENDING status', () => {
    const startPRStatusPollingSpy = jest.spyOn(actions, 'startPRStatusPolling').mockImplementation(({ id }) => {
      return {
        type: 'componentDetailsOverview/startPRStatusPolling',
        payload: { id },
        abort: jest.fn(),
      };
    });

    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'PULL_REQUEST_CREATION_PENDING',
      id: 'test-pr-id',
    };

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...defaultPreloadedState,
      }
    );

    expect(startPRStatusPollingSpy).toHaveBeenCalledWith({
      id: 'test-pr-id',
    });
  });

  it('starts polling when PR is created via retry', async () => {
    const startPRStatusPollingSpy = jest.spyOn(actions, 'startPRStatusPolling').mockImplementation(({ id }) => {
      return {
        type: 'componentDetailsOverview/startPRStatusPolling',
        payload: { id },
        abort: jest.fn(),
      };
    });

    axiosMock.onPost(getCreatePullRequestUrl()).reply(200, { id: 'id' });

    const versionChanges = [
      {
        id: 'next-no-violation-version',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
    ];

    const automatedRemediationStatus = {
      status: 'PULL_REQUEST_CREATION_FAILED',
      reason: 'Network error',
    };

    const preloadedState = mergeDeepRight(defaultPreloadedState, {
      applicationReport: {
        selectedReport: {
          allEntries: defaultPreloadedState.applicationReport.selectedReport.allEntries.map((entry) => ({
            ...entry,
            directDependency: true,
          })),
        },
      },
    });

    renderComponent(
      { versionChanges, automatedRemediationStatus },
      {
        ...preloadedState,
      }
    );

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    await userEvent.setup().click(retryButton);

    expect(axiosMock.history.post.length).toBe(1);
    expect(JSON.parse(axiosMock.history.post[0].data).targetVersion).toBe('2.4.10');
    expect(JSON.parse(axiosMock.history.post[0].data).isDirectDependency).toBe(true);
    expect(startPRStatusPollingSpy).toHaveBeenCalledWith({
      id: 'id',
    });
  });
});
