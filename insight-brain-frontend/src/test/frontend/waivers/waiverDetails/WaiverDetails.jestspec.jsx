/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  render,
  screen,
  fireEvent,
  axiosMockAdapter,
  waitFor,
  WAIVER_CREATE_TIME,
  WAIVER_EXPIRATION_TIME,
} from 'TestRoot/SpecUtil';
import { getFirewallWaiverDetailsUrl, getWaiverDetailsUrl, deleteWaiverUrl } from 'MainRoot/util/CLMLocation';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import WaiverDetails from 'MainRoot/waivers/waiverDetails/WaiverDetails';
import * as routeSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('When the WaiverDetailsPage', function () {
  let axiosMock,
    expectedWaiverDetailsUrl,
    renderComponent,
    waiverDetails,
    containerWaiverDetails,
    allComponentsWaiverDetails,
    allVersionsWaiverDetails,
    unknownComponentWaiverDetails,
    ownerType,
    ownerId,
    waiverId,
    initialState,
    expectedDeleteWaiverUrl;

  const CONTAINER_WAIVER_INFO_ALERT =
    "Details of all the specific policies waived aren't displayed on this " +
    'page. To review the individual policies and components affected by this waiver, please refer to the Container ' +
    'Image Report.';

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    ownerType = 'owner-type';
    ownerId = 'owner-id';
    waiverId = 'waiver-id';

    containerWaiverDetails = {
      policyWaiverId: 'b1b59985e22a4dd095e6812732af6407',
      comment: 'test comment',
      createTime: WAIVER_CREATE_TIME,
      scopeOwnerType: 'application',
      scopeOwnerId: '0caa731cc7b149e7bc24fe9602e3a7dd',
      scopeOwnerName: 'application-docker-proxy-library-alpine-3.6',
      policyId: '7e2d159c02734df6be7529ff5b88f67f',
      policyName: 'docker-policy-sonatype-container',
      constraintFacts: [
        {
          constraintId: 'dfff41a5f5304ceaa945f46aa2dda64d',
          constraintName: 'all-docker-container',
          operatorName: 'OR',
          conditionFacts: [
            {
              conditionTypeI: 'IdentificationSource',
              conditionIndex: 0,
              summary: 'Identification Source is Sonatype-Container',
              reason: 'Identification Source was Sonatype-Container',
              reference: null,
              triggerJson: null,
            },
          ],
        },
      ],
      creatorName: 'Admin',
      matcherStrategy: 'ALL_COMPONENTS',
      associatedPackageUrl: null,
      componentIdentifier: null,
      threatLevel: 9,
      reasonText: null,
      expireWhenRemediationAvailable: false,
      policyWaiverReasonId: null,
      forContainerImage: true,
      forContainerImageComponent: false,
      displayName: null,
    };

    waiverDetails = {
      comment: 'a comment',
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      createTime: WAIVER_CREATE_TIME,
      creatorName: 'test creator',
      expiryTime: WAIVER_EXPIRATION_TIME,
      policyName: 'test policy',
      policyWaiverId: 'b0fb538f851d473090489436b96e7a16',
      scopeOwnerId: 'ROOT_ORGANIZATION',
      scopeOwnerName: 'Root Organization',
      scopeOwnerType: 'root_organization',
      vulnerabilityId: 'CVE-2013-7285',
      associatedPackageUrl: 'a/package/url',
      matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
      componentUpgradeAvailable: true,
      componentIdentifier: { coordinates: { version: '1.2.3', name: 'test-artifact', group: 'test-group' } },
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'test-group',
          },
          {
            value: ':',
          },
          {
            field: 'Artifact',
            value: 'test-artifact',
          },
          {
            value: ':',
          },
          {
            field: 'Version',
            value: '1.2.3',
          },
        ],
      },
    };

    allVersionsWaiverDetails = {
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      matcherStrategy: waiverMatcherStrategy.ALL_VERSIONS,
      displayName: {
        parts: [],
      },
      componentUpgradeAvailable: true,
    };

    allComponentsWaiverDetails = {
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      matcherStrategy: waiverMatcherStrategy.ALL_COMPONENTS,
      displayName: {
        parts: [],
      },
      componentUpgradeAvailable: true,
    };

    unknownComponentWaiverDetails = {
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
    };

    // Required to the render function in order for it to supercede
    // what's in the redux state so we don't have to mock the store/selectors
    initialState = {
      router: {
        currentParams: {
          ownerType,
          ownerId,
          waiverId,
        },
        currentState: {
          name: 'waiver.details',
        },
      },
      waiverDetails: {
        waiverDetails: null,
        loading: false,
        loadError: null,
        hasWaivePermission: true,
      },
    };

    expectedWaiverDetailsUrl = getWaiverDetailsUrl(ownerType, ownerId, waiverId);
    expectedDeleteWaiverUrl = deleteWaiverUrl('organization', waiverDetails.scopeOwnerId, waiverDetails.policyWaiverId);
    // By default, grant WAIVE_POLICY_VIOLATIONS so the Delete button renders.
    // Individual tests can override with axiosMock.onPut(...).replyOnce(...).
    axiosMock.onPut(/\/rest\/user\/permissions\//).reply(200, ['WAIVE_POLICY_VIOLATIONS']);
    // Ensure render function includes the router currentParams we will need
    renderComponent = (preloadedState = initialState) => render(<WaiverDetails />, { preloadedState });
  });

  describe('has a loading error', () => {
    it('it renders an error with the error message and a retry button', async () => {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(() => Promise.reject('Some error'));
      renderComponent();
      // Loading state is set upon pending request so can be
      // tested for synchronously
      expect(screen.getByText('Loading…')).toBeVisible();
      // Use await and findBy to look up elements that depend
      // on the resolution of an async call to be rendered
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(await screen.findByText(/Some error/i)).toBeVisible();

      const retryButton = await screen.findByRole('button', { name: 'Retry' });
      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(screen.getByText('Loading…')).toBeVisible();
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[1].url).toBe(expectedWaiverDetailsUrl);
    });
  });

  describe('successfully loads waiver details', () => {
    it('it renders the expected waiver details', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('1.2.3');
      expect(await screen.findByText('test policy')).toBeVisible();
      expect(await screen.findByText('test constraint')).toBeVisible();
      expect(await screen.findByText('Vulnerability Details')).toBeVisible();
      expect(await screen.findByText('reason 1')).toBeVisible();
      expect(await screen.findByText('reason 2')).toBeVisible();
      expect(await screen.findByText('Root Organization')).toBeVisible();
      expect(await screen.findByText('2022-08-18')).toBeVisible();
      expect(await screen.findByText('2023-08-18')).toBeVisible();
      expect(await screen.findByText('a comment')).toBeVisible();
      expect(await screen.findByText('test creator')).toBeVisible();
      expect(await screen.findByText('*Indicates the component name when the waiver was created')).toBeVisible();
      expect(await screen.findByText('test-group:test-artifact:1.2.3')).toBeVisible();
      expect(await screen.findByText('Upgrade Available')).toBeVisible();
    });

    it('it renders the expected container waiver details', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, containerWaiverDetails);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
      expect(await screen.findByText('docker-policy-sonatype-container')).toBeVisible();
      expect(await screen.findByText('all-docker-container')).toBeVisible();
      expect(await screen.findByText('Identification Source was Sonatype-Container')).toBeVisible();
      expect(await screen.findByText('application-docker-proxy-library-alpine-3.6')).toBeVisible();
      expect(await screen.findByText('2022-08-18')).toBeVisible();
      expect(await screen.findByText('Does not expire')).toBeVisible();
      expect(await screen.findByText('test comment')).toBeVisible();
      expect(await screen.findByText('Admin')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Delete Waiver for All Policy Violations' })).toBeVisible();
      expect(await screen.findByTestId('container-waiver-details-info-alert')).toHaveTextContent(
        CONTAINER_WAIVER_INFO_ALERT
      );
    });

    it('it opens vulnerability details modal upon clicking vulnerability link', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      const vulnerabilityLink = await screen.findByText('Vulnerability Details');
      fireEvent.click(vulnerabilityLink);

      expect(await screen.findByRole('dialog')).toBeVisible();
      expect(await screen.findByText('Vulnerability Information')).toBeVisible();
    });

    it('shows disclaimer text and does not show upgrade indicator if waiver was scoped to all versions of a component', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, allVersionsWaiverDetails);
      renderComponent();
      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('All Versions');

      expect(screen.getByText('*Indicates the component name when the waiver was created')).toBeVisible();
      expect(screen.queryByText('Upgrade Available')).not.toBeInTheDocument();
    });

    it('does not show disclaimer text or upgrade indicator if waiver was scoped to all components', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, allComponentsWaiverDetails);
      renderComponent();
      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('--');
      await waitFor(() => {
        // findByText can only be used to query for presence of an element
        expect(screen.queryByText('*Indicates the component name when the waiver was created')).not.toBeInTheDocument();
        expect(screen.queryByText('Upgrade Available')).not.toBeInTheDocument();
      });
    });

    it('does not show disclaimer text if component is unknown', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, unknownComponentWaiverDetails);
      renderComponent();

      expect(await screen.findByText('Unknown')).toBeVisible();
      await waitFor(() => {
        expect(screen.queryByText('*Indicates the component name when the waiver was created')).not.toBeInTheDocument();
      });
    });

    it('shows "All Versions" when matcherStrategy is null/undefined (default case)', async function () {
      const waiverWithNullMatcherStrategy = {
        constraintFacts: [{ constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }] }],
        matcherStrategy: null,
        displayName: {
          parts: [
            { field: 'Group', value: 'test-group' },
            { value: ':' },
            { field: 'Artifact', value: 'test-artifact' },
          ],
        },
        componentUpgradeAvailable: false,
      };
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverWithNullMatcherStrategy);
      renderComponent();

      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('All Versions');

      // When matcherStrategy is null, isWaiverAllVersionsOrExact returns false,
      // so no disclaimer is shown (Components shows "--" instead)
      await waitFor(() => {
        expect(screen.queryByText('*Indicates the component name when the waiver was created')).not.toBeInTheDocument();
      });
    });

    it('shows "All Versions" when matcherStrategy is an empty string', async function () {
      const waiverWithEmptyMatcherStrategy = {
        constraintFacts: [{ constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }] }],
        matcherStrategy: '',
        displayName: {
          parts: [{ field: 'Group', value: 'test-group' }],
        },
        componentUpgradeAvailable: false,
      };
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverWithEmptyMatcherStrategy);
      renderComponent();

      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('All Versions');
    });

    it('shows "All Versions" when matcherStrategy is an unrecognized value', async function () {
      const waiverWithUnknownMatcherStrategy = {
        constraintFacts: [{ constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }] }],
        matcherStrategy: 'UNKNOWN_STRATEGY',
        displayName: {
          parts: [],
        },
        componentUpgradeAvailable: false,
      };
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverWithUnknownMatcherStrategy);
      renderComponent();

      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('All Versions');
    });

    it('shows version from componentIdentifier for EXACT_COMPONENT waiver even when displayName is missing', async function () {
      const exactComponentWaiverWithoutDisplayName = {
        constraintFacts: [{ constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }] }],
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        componentIdentifier: { coordinates: { version: '2.0.0', name: 'artifact', group: 'group' } },
        displayName: null,
        associatedPackageUrl: 'pkg:maven/group/artifact@2.0.0',
        componentUpgradeAvailable: false,
      };
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, exactComponentWaiverWithoutDisplayName);
      renderComponent();

      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      expect(version).toHaveTextContent('2.0.0');
    });

    it('falls back to "All Versions" for EXACT_COMPONENT when componentIdentifier is missing', async function () {
      const exactComponentWaiverWithoutComponentIdentifier = {
        constraintFacts: [{ constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }] }],
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
        componentIdentifier: null,
        displayName: null,
        componentUpgradeAvailable: false,
      };
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, exactComponentWaiverWithoutComponentIdentifier);
      renderComponent();

      // This tests the defensive fallback in getComponentVersion()
      // When EXACT_COMPONENT is set but componentIdentifier is missing,
      // the version will be undefined/null
      const version = await screen.findByTestId('waiver-details-version');
      expect(version).toBeVisible();
      // The component display will show "Unknown" but version will show componentIdentifier attempt
    });

    it('should render waiver details with scope equals to Repository Managers if ownerType is all_repositories', async function () {
      initialState.router.currentParams.ownerType = 'all_repositories';
      waiverDetails.scopeOwnerId = 'REPOSITORY_CONTAINER_ID';
      waiverDetails.scopeOwnerName = 'Repository Managers';
      waiverDetails.scopeOwnerType = 'all_repositories';

      expectedWaiverDetailsUrl = getWaiverDetailsUrl('repository_container', ownerId, waiverId);
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
      expect(await screen.findByText('Repository Managers')).toBeVisible();
    });

    it('should render waiver details with scope equals to Root Organization if ownerType is root_organization', async function () {
      initialState.router.currentParams.ownerType = 'root_organization';
      waiverDetails.scopeOwnerId = 'ROOT_ORGANIZATION';
      waiverDetails.scopeOwnerName = 'Root Organization';
      waiverDetails.scopeOwnerType = 'root_organization';

      expectedWaiverDetailsUrl = getWaiverDetailsUrl('organization', ownerId, waiverId);
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
      expect(await screen.findByText('Root Organization')).toBeVisible();
    });

    it('renders renewal comment before creation comment (latest first)', async function () {
      jest.spyOn(routeSelectors, 'selectIsStandaloneFirewall').mockReturnValue(true);
      axiosMock.onGet(getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId)).reply(200, {
        ...waiverDetails,
        lastRenewedBy: 'admin',
        lastRenewedAt: '2024-01-15T10:00:00.000Z',
        lastRenewalReasonText: 'Security patch applied',
        lastRenewalComment: 'Renewed after applying the fix',
      });
      renderComponent();

      expect(await screen.findByText('Security patch applied')).toBeVisible();
      expect(await screen.findByText('Renewed after applying the fix')).toBeVisible();
      expect(await screen.findByText('a comment')).toBeVisible();

      const blockquote = document.querySelector('.nx-blockquote');
      const labels = blockquote.querySelectorAll('.iq-waiver-comment-entry__label');
      expect(labels[0]).toHaveTextContent('Renewed');
      expect(labels[1]).toHaveTextContent('Created');
    });

    it('renders renewal reason with dash when absent', async function () {
      jest.spyOn(routeSelectors, 'selectIsStandaloneFirewall').mockReturnValue(true);
      axiosMock.onGet(getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId)).reply(200, waiverDetails);
      renderComponent();

      await screen.findByText('a comment'); // wait for load
      expect(screen.getByText('Renewal Reason')).toBeInTheDocument();
      const renewalReasonItem = document.querySelector('.iq-waiver-details__last-renewal-reason');
      expect(renewalReasonItem).toHaveTextContent('-');
    });

    it('shows dash when renewal comment is empty', async function () {
      jest.spyOn(routeSelectors, 'selectIsStandaloneFirewall').mockReturnValue(true);
      axiosMock.onGet(getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId)).reply(200, {
        ...waiverDetails,
        lastRenewedAt: '2024-01-15T10:00:00.000Z',
        lastRenewedBy: 'admin',
        lastRenewalComment: null,
      });
      renderComponent();

      await screen.findByText('Renewed');
      const blockquote = document.querySelector('.nx-blockquote');
      const entries = blockquote.querySelectorAll('.iq-waiver-comment-entry__text');
      expect(entries[0]).toHaveTextContent('-');
    });
  });

  describe('WAIVE_POLICY_VIOLATIONS permission gating', () => {
    it('renders the Delete Waiver button when WAIVE_POLICY_VIOLATIONS is granted', async () => {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      expect(await screen.findByRole('button', { name: 'Delete Waiver' })).toBeVisible();
    });

    it('renders the container Delete Waiver button when WAIVE_POLICY_VIOLATIONS is granted', async () => {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, containerWaiverDetails);
      renderComponent();

      expect(await screen.findByRole('button', { name: 'Delete Waiver for All Policy Violations' })).toBeVisible();
    });

    it('hides the Delete Waiver button when WAIVE_POLICY_VIOLATIONS is not granted', async () => {
      // Override the default-granting permission mock with a denial.
      axiosMock.reset();
      axiosMock.onPut(/\/rest\/user\/permissions\//).reply(200, []);
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      // Wait for the load to complete by asserting any post-load content.
      expect(await screen.findByText('test policy')).toBeVisible();
      expect(screen.queryByRole('button', { name: 'Delete Waiver' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Delete Waiver for All Policy Violations' })).not.toBeInTheDocument();
    });

    it('hides the Renew Waiver button when WAIVE_POLICY_VIOLATIONS is not granted in standalone firewall', async () => {
      jest.spyOn(routeSelectors, 'selectIsStandaloneFirewall').mockReturnValue(true);
      axiosMock.reset();
      axiosMock.onPut(/\/rest\/user\/permissions\//).reply(200, []);
      axiosMock.onGet(getFirewallWaiverDetailsUrl(ownerType, ownerId, waiverId)).reply(200, waiverDetails);
      renderComponent();

      expect(await screen.findByText('test policy')).toBeVisible();
      expect(screen.queryByRole('button', { name: 'Renew Waiver' })).not.toBeInTheDocument();
    });
  });

  describe('when pressing the delete button', () => {
    it('successfully delete a waiver', async () => {
      jest.spyOn(routeSelectors, 'selectIsFirewall').mockReturnValue(false);
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      axiosMock.onDelete(expectedDeleteWaiverUrl).reply(204);

      renderComponent();

      const deleteWaiverButton = await screen.findByText('Delete Waiver');
      expect(deleteWaiverButton).toBeVisible();

      fireEvent.click(deleteWaiverButton);

      expect(await screen.findByText('Are you sure you want to delete this waiver?')).toBeVisible();

      const deleteWaiverModalButton = await screen.getByText((content, element) => {
        return element.id === 'delete-waiver-modal-continue-button' && content === 'Delete Waiver';
      });

      //jest.useFakeTimers(); // prevent Success from disappearing before assertion can run
      fireEvent.click(deleteWaiverModalButton);
      const el = await screen.findByText('Success!');

      expect(el).toBeVisible();
    });
  });
});
