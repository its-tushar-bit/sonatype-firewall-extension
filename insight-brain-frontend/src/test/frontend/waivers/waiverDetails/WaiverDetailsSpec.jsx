/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import { getWaiverDetailsUrl, deleteWaiverUrl } from 'MainRoot/util/CLMLocation';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import WaiverDetails from 'MainRoot/waivers/waiverDetails/WaiverDetails';

describe('When the WaiverDetailsPage', function () {
  let axiosMock,
    expectedWaiverDetailsUrl,
    renderComponent,
    waiverDetails,
    allComponentsWaiverDetails,
    allVersionsWaiverDetails,
    unknownComponentWaiverDetails,
    expectedDeleteWaiverUrl;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    const ownerType = 'owner-type';
    const ownerId = 'owner-id';
    const waiverId = 'waiver-id';

    waiverDetails = {
      comment: 'a comment',
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      createTime: '08/18/2022',
      creatorName: 'test creator',
      expiryTime: '08/18/2023',
      policyName: 'test policy',
      policyWaiverId: 'b0fb538f851d473090489436b96e7a16',
      scopeOwnerId: 'ROOT_ORGANIZATION',
      scopeOwnerName: 'root org',
      scopeOwnerType: 'root_organization',
      vulnerabilityId: 'CVE-2013-7285',
      associatedPackageUrl: 'a/package/url',
      matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
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
    };

    allComponentsWaiverDetails = {
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      matcherStrategy: waiverMatcherStrategy.ALL_COMPONENTS,
      displayName: {
        parts: [],
      },
    };

    unknownComponentWaiverDetails = {
      constraintFacts: [
        { constraintName: 'test constraint', conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }] },
      ],
      matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
    };

    // Required to the render function in order for it to supercede
    // what's in the redux state so we don't have to mock the store/selectors
    const preloadedState = {
      router: {
        currentParams: {
          ownerType,
          ownerId,
          waiverId,
        },
      },
    };

    expectedWaiverDetailsUrl = getWaiverDetailsUrl(ownerType, ownerId, waiverId);
    expectedDeleteWaiverUrl = deleteWaiverUrl('organization', waiverDetails.scopeOwnerId, waiverDetails.policyWaiverId);
    // Ensure render function includes the router currentParams we will need
    renderComponent = () => render(<WaiverDetails />, { preloadedState });
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

      expect(await screen.findByText('test policy')).toBeVisible();
      expect(await screen.findByText('test constraint')).toBeVisible();
      expect(await screen.findByText('See Security Vulnerability Details')).toBeVisible();
      expect(await screen.findByText('reason 1')).toBeVisible();
      expect(await screen.findByText('reason 2')).toBeVisible();
      expect(await screen.findByText('Root Organization')).toBeVisible();
      expect(await screen.findByText('08/18/2022')).toBeVisible();
      expect(await screen.findByText('08/18/2023')).toBeVisible();
      expect(await screen.findByText('a comment')).toBeVisible();
      expect(await screen.findByText('test creator')).toBeVisible();
      expect(await screen.findByText('*Indicates the component name when the waiver was created')).toBeVisible();
      expect(await screen.findByText('test-group:test-artifact:1.2.3')).toBeVisible();
    });

    it('it opens vulnerability details modal upon clicking vulnerability link', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, waiverDetails);
      renderComponent();

      const vulnerabilityLink = await screen.findByText('See Security Vulnerability Details');
      fireEvent.click(vulnerabilityLink);

      expect(await screen.findByRole('dialog')).toBeVisible();
      expect(await screen.findByText('Vulnerability Information')).toBeVisible();
    });

    it('shows disclaimer text if waiver was scoped to all versions of a component', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, allVersionsWaiverDetails);
      renderComponent();

      expect(await screen.findByText('*Indicates the component name when the waiver was created')).toBeVisible();
    });

    it('does not show disclaimer text if waiver was scoped to all components', async function () {
      axiosMock.onGet(expectedWaiverDetailsUrl).reply(200, allComponentsWaiverDetails);
      renderComponent();

      expect(await screen.findByText('All components')).toBeVisible();
      // findByText can only be used to query for presence of an element
      await waitFor(() => {
        expect(screen.queryByText('*Indicates the component name when the waiver was created')).not.toBeInTheDocument();
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
  });

  describe('when pressing the delete button', () => {
    it('successfully delete a waiver', async () => {
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

      fireEvent.click(deleteWaiverModalButton);

      expect(await screen.findByText('Success!')).toBeVisible();
    });
  });
});
