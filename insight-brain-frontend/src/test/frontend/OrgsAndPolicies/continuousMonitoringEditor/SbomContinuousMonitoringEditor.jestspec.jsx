/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, screen } from '@testing-library/react';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import SbomContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/continuousMonitoringEditor/SbomContinuousMonitoringEditor';
import {
  getApplicablePolicyMonitoringUrl,
  getCliStageUrl,
  getPolicyMonitoringUrl,
  getSbomStageUrl,
} from 'MainRoot/util/CLMLocation';

describe('SbomContinuousMonitoringEditor', () => {
  let store, renderPage;
  const axiosMock = axiosMockAdapter();

  beforeEach(() => {
    store = {
      router: {
        currentParams: {
          organizationId: 'org',
          ownerType: 'organization',
          ownerId: 'org',
          currentState: { name: 'organization' },
        },
      },
      orgsAndPolicies: {
        policyMonitoring: {
          loading: false,
          loadError: null,
          submitError: null,
          isDirty: false,
          submitMaskState: null,
          stages: { sbom: { stageTypes: [{ stageTypeId: 'stageOne', stageName: 'Stage One' }] } },
          monitoredStage: null,
          originalStage: null,
          actionStages: null,
          policyMonitoringByOwner: null,
        },
      },
    };

    axiosMock.onGet(getSbomStageUrl()).reply(200, [{ stageTypeId: 'stageOne', stageName: 'Stage One' }]);
    axiosMock.onGet(getCliStageUrl()).reply(200, [{ stageTypeId: 'stageTwo', stageName: 'Stage Two' }]);

    renderPage = (additionalState = {}) =>
      render(<SbomContinuousMonitoringEditor />, { preloadedState: { ...store, ...additionalState } });
  });

  describe('SBOM Continuous Monitoring Editor', () => {
    describe('with only Root Organization settings', () => {});

    it('shows CM enabled when it is enabled on the Root Org', async () => {
      axiosMock
        .onGet(
          getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId)
        )
        .reply(200, {
          policyMonitoringByOwner: [
            { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
          ],
        });
      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(screen.getByText('Disable continuous monitoring for SBOM Manager')).toBeInTheDocument();
      expect(screen.getByText('Enabled')).toBeVisible();
    });

    it('shows CM disabled when it is disabled on the Root Org', async () => {
      axiosMock
        .onGet(
          getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId)
        )
        .reply(200, {
          policyMonitoringByOwner: [
            { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageTwo' }] },
          ],
        });
      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(screen.getByText('Enable continuous monitoring for SBOM Manager')).toBeInTheDocument();
      expect(screen.getByText('Disabled')).toBeVisible();
    });
  });

  describe('with sub organization settings', () => {
    it('shows CM enabled when it is enabled on the Root Org', async () => {
      axiosMock
        .onGet(
          getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId)
        )
        .reply(200, {
          policyMonitoringByOwner: [
            { ownerName: 'organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
            { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
          ],
        });
      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(
        screen.getByText(
          "Continuous Monitoring is up and running at Root Organization, so this means it's active for this organization and all its dependents."
        )
      ).toBeInTheDocument();
      const toggle = screen.getByRole('switch');
      expect(toggle).toBeDisabled();
      expect(screen.getByText('Enabled')).toBeVisible();
    });

    it('shows CM disabled when it is disabled on the Root Org', async () => {
      axiosMock
        .onGet(
          getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId)
        )
        .reply(200, {
          policyMonitoringByOwner: [
            { ownerName: 'organization', policyMonitorings: [{ stageTypeId: 'otherStage' }] },
            { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'otherStage' }] },
          ],
        });
      renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(
        screen.getByText(
          'Continuous Monitoring is currently disabled at the root organization. Would you like to enable it for this organization and all its dependents?'
        )
      ).toBeInTheDocument();
      expect(screen.getByText('Disabled')).toBeVisible();
    });
  });

  describe('with application settings', () => {
    it('shows CM enabled when it is enabled on the parent organization', async () => {
      axiosMock.onGet(getApplicablePolicyMonitoringUrl('application', 'appId')).reply(200, {
        policyMonitoringByOwner: [
          { ownerName: 'application', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
          { ownerName: 'organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
          { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
        ],
      });
      renderPage({
        router: {
          currentState: { name: 'sbomManager.management.edit.application.monitor-policy' },
          currentParams: {
            ownerType: 'application',
            applicationPublicId: 'appId',
            applicationId: 'appId',
          },
        },
      });

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(
        screen.getByText(
          "Continuous Monitoring is up and running at organization, so this means it's active for this application."
        )
      ).toBeInTheDocument();
      const toggle = screen.getByRole('switch');
      expect(toggle).toBeDisabled();
      expect(screen.getByText('Enabled')).toBeVisible();
    });

    it('shows CM disabled when it is disabled on the parent organization', async () => {
      axiosMock.onGet(getApplicablePolicyMonitoringUrl('application', 'appId')).reply(200, {
        policyMonitoringByOwner: [
          { ownerName: 'application', policyMonitorings: [] },
          { ownerName: 'organization', policyMonitorings: [] },
          { ownerName: 'Root Organization', policyMonitorings: [] },
        ],
      });
      renderPage({
        router: {
          currentState: { name: 'sbomManager.management.edit.application.monitor-policy' },
          currentParams: {
            ownerType: 'application',
            applicationPublicId: 'appId',
            applicationId: 'appId',
          },
        },
      });

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(
        screen.getByText(
          'Continuous Monitoring is currently disabled at the root organization. Would you like to enable it for this application?'
        )
      ).toBeInTheDocument();
      const toggle = screen.getByRole('switch');
      expect(toggle).toBeEnabled();
      expect(screen.getByText('Disabled')).toBeVisible();
    });

    it('shows CM enabled when it is enabled on the application and disabled on the parent organization', async () => {
      axiosMock.onGet(getApplicablePolicyMonitoringUrl('application', 'appId')).reply(200, {
        policyMonitoringByOwner: [
          { ownerName: 'application', policyMonitorings: [{ stageTypeId: 'stageOne' }] },
          { ownerName: 'organization', policyMonitorings: [] },
          { ownerName: 'Root Organization', policyMonitorings: [] },
        ],
      });
      renderPage({
        router: {
          currentState: { name: 'sbomManager.management.edit.application.monitor-policy' },
          currentParams: {
            ownerType: 'application',
            applicationPublicId: 'appId',
            applicationId: 'appId',
          },
        },
      });

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
      expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
      expect(screen.getByText('Disable continuous monitoring for SBOM Manager')).toBeInTheDocument();
      const toggle = screen.getByRole('switch');
      expect(toggle).toBeEnabled();
      expect(screen.getByText('Enabled')).toBeVisible();
    });
  });

  it('updates the CM status when saving changes', async () => {
    axiosMock
      .onGet(getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId))
      .replyOnce(200, {
        policyMonitoringByOwner: [
          { ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'otherStage' }] },
        ],
      });
    axiosMock
      .onGet(getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId))
      .reply(200, {
        policyMonitoringByOwner: [{ ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] }],
      });
    axiosMock
      .onPut(getPolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId))
      .reply(200, {
        id: 'any',
        ownerId: store.router.currentParams.ownerId,
        stageTypeId: 'stageOne',
      });
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('Enable continuous monitoring for SBOM Manager')).toBeInTheDocument();
    expect(screen.getByText('Disabled')).toBeVisible();
    const toggle = screen.getByRole('switch');
    const updateButton = screen.getByText('Update');
    expect(toggle).toBeVisible();
    expect(toggle).not.toBeChecked();
    fireEvent.click(toggle);
    expect(toggle).toBeChecked();
    fireEvent.click(updateButton);
    await waitFor(() => expect(screen.queryByText('Saving…')).toBeNull());
    expect(screen.getByText('Disable continuous monitoring for SBOM Manager')).toBeInTheDocument();
    expect(screen.getByText('Enabled')).toBeVisible();
  });

  it('shows error message when no changes to save', async () => {
    axiosMock
      .onGet(getApplicablePolicyMonitoringUrl(store.router.currentParams.ownerType, store.router.currentParams.ownerId))
      .reply(200, {
        policyMonitoringByOwner: [{ ownerName: 'Root Organization', policyMonitorings: [{ stageTypeId: 'stageOne' }] }],
      });
    renderPage();

    await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

    expect(screen.getByText('Continuous Monitoring')).toBeInTheDocument();
    expect(screen.getByText('Configure Continuous monitoring')).toBeInTheDocument();
    expect(screen.getByText('Enabled')).toBeVisible();
    const updateButton = screen.getByText('Update');
    fireEvent.click(updateButton);
    await waitFor(() =>
      expect(screen.getByText('There were validation errors. There are no changes to save.')).toBeVisible()
    );
  });
});
