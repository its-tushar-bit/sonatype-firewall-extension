/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import { render, screen, axiosMockAdapter, fireEvent, within } from 'TestRoot/SpecUtil';

import { getApplicationSummaryUrl, getPermissionContextTestUrl } from 'MainRoot/util/CLMLocation';

describe('ActionDropdown', () => {
  let axiosMock, defaultPreloadedState;
  const permissions = ['WRITE', 'EVALUATE_APPLICATION'];

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      router: {
        currentParams: { applicationPublicId: '4', '#': null },
        currentState: {
          data: { title: 'Application Management', viewportSized: true },
          name: 'management.view.application',
          url: '/application/{applicationPublicId}',
        },
      },
      orgsAndPolicies: {
        stages: {
          dashboard: {
            stageTypes: [
              {
                shortName: 'Source',
                stageName: 'Source',
                stageTypeId: 'source',
              },
              { shortName: 'Build', stageName: 'Build', stageTypeId: 'build' },
              {
                shortName: 'Stage',
                stageName: 'Stage Release',
                stageTypeId: 'stage-release',
              },
              {
                shortName: 'Release',
                stageName: 'Release',
                stageTypeId: 'release',
              },
              {
                shortName: 'Operate',
                stageName: 'Operate',
                stageTypeId: 'operate',
              },
            ],
          },
        },
        legacyViolations: {
          data: {
            enabled: true,
          },
        },
        root: {
          selectedOwner: {
            id: 'a28',
            publicId: '4',
            name: '44App',
            organizationId: 'cb53',
            organizationName: '4 org',
            contact: null,
          },
        },
        ownerActions: {
          actionDropdown: {
            loading: false,
            loadError: null,
            applicationSummary: {
              id: 'a28',
              publicId: '4',
              name: '44App',
              organizationId: 'cb53',
              organizationName: '4 org',
              policyEvaluations: {},
              policyEvaluationsResults: {},
              contact: null,
              hasPendingSourceControlPolicyEvaluation: false,
            },
          },
        },
      },
      productFeatures: {
        productFeatures: {
          'cli-integration': true,
          'policy-grandfathering': true,
        },
      },
    };

    axiosMock.onGet(getApplicationSummaryUrl(4)).reply(200, {
      contact: null,
      hasPendingSourceControlPolicyEvaluation: false,
      id: 'a28',
      name: '44App',
      organizationId: 'cb53',
      organizationName: '4 org',
      policyEvaluations: {},
      policyEvaluationsResults: {},
      publicId: '4',
    });

    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'a28'), permissions)
      .reply(200, ['WRITE', 'EVALUATE_APPLICATION']);

    SpecUtil.requestIdleCallbackInvokeImmediate();
  });

  let renderComponent = (preloadedState) =>
    render(<ActionDropdown />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });

  it('renders the component', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: 'Actions' })).toBeVisible();
  });

  describe('Action Dropdown is open', () => {
    it('on App level', async () => {
      renderComponent();
      const actionButton = screen.getByRole('button', { name: 'Actions' });
      fireEvent.click(actionButton);
      const dropdownButtons = await screen.findAllByRole('button');
      dropdownButtons.forEach((button) => expect(button).toBeVisible());
      const buttonNames = [
        'Actions',
        'App ID to Clipboard',
        'Select Contact',
        'Edit App Name / Icon',
        'Change App ID',
        'Move 44App',
        'Delete 44App',
        'Legacy existing violations',
        'Revoke legacy status',
        'Evaluate a File',
        'View source report',
        'View build report',
        'View stage report',
        'View release report',
        'View operate report',
      ];
      dropdownButtons.forEach((button, ind) => {
        expect(button.textContent).toBe(buttonNames[ind]);
      });

      // Check API calls on App level
      expect(axiosMock.history.put.length).toBe(1);
      expect(axiosMock.history.get.length).toBe(1);
    });

    it('on Org level', async () => {
      renderComponent({
        router: {
          currentParams: { '#': null, organizationId: 'cb53' },
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'cb53',
              parentOrganizationId: 'ROOT_ORGANIZATION_ID',
              name: '4 org',
              nameLowercaseNoWhitespace: '4org',
              legacyViolationEnabled: true,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          ownerActions: {
            loading: false,
            loadError: null,
            applicationSummary: null,
          },
        },
      });
      const actionButton = screen.getByRole('button', { name: 'Actions' });
      fireEvent.click(actionButton);
      const dropdownButtons = await screen.findAllByRole('button');
      dropdownButtons.forEach((button) => expect(button).toBeVisible());
      const buttonNames = [
        'Actions',
        'Org ID to Clipboard',
        'Edit Org Name / Icon',
        'Move 4 org',
        'Import Policies',
        'Delete 4 org',
      ];
      dropdownButtons.forEach((button, ind) => {
        expect(button.textContent).toBe(buttonNames[ind]);
      });
      // there should be no API calls on Org level
      expect(axiosMock.history.put.length).toBe(0);
      expect(axiosMock.history.get.length).toBe(0);
    });

    it('on RootOrg level', async () => {
      renderComponent({
        router: {
          currentParams: { '#': null, organizationId: 'ROOT_ORGANIZATION_ID' },
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'ROOT_ORGANIZATION_ID',
              parentOrganizationId: null,
              name: 'Root Organization',
              nameLowercaseNoWhitespace: 'rootorganization',
              legacyViolationEnabled: false,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          ownerActions: {
            loading: false,
            loadError: null,
            applicationSummary: null,
          },
        },
      });
      const actionButton = screen.getByRole('button', { name: 'Actions' });
      fireEvent.click(actionButton);
      const dropdownButtons = await screen.findAllByRole('button');
      dropdownButtons.forEach((button) => expect(button).toBeVisible());
      const buttonNames = ['Actions', 'Org ID to Clipboard', 'Edit Org Name / Icon', 'Import Policies'];
      dropdownButtons.forEach((button, ind) => {
        expect(button.textContent).toBe(buttonNames[ind]);
      });
      // there should be no API calls on RootOrg level
      expect(axiosMock.history.put.length).toBe(0);
      expect(axiosMock.history.get.length).toBe(0);
    });
  });

  describe('Change App ID', () => {
    describe('when there are correct permissions', () => {
      it('renders an enabled button without tooltips', async () => {
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const changeAppIDButton = await screen.findByRole('button', { name: 'Change App ID' });
        expect(changeAppIDButton).not.toHaveClassName('disabled');
        fireEvent.mouseOver(changeAppIDButton);
        const tooltip = screen.queryByRole('tooltip');
        expect(tooltip).not.toBeInTheDocument();
      });
    });

    describe('when there are no correct permissions', () => {
      it('renders a disabled button with tooltip "insufficient permissions"', async () => {
        axiosMock
          .onPut(getPermissionContextTestUrl('application', 'a28'), permissions)
          .reply(200, ['EVALUATE_APPLICATION']);
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const changeAppIDButton = await screen.findByRole('button', { name: 'Change App ID' });
        expect(changeAppIDButton).toHaveClassName('disabled');
        fireEvent.mouseOver(changeAppIDButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText('Insufficient permissions to change App ID')).toBeInTheDocument();
      });
    });

    describe('when there is error in retrieving permissions', () => {
      it('renders a disabled button with tooltip "insufficient permissions"', async () => {
        axiosMock.onPut(getPermissionContextTestUrl('application', 'a28'), permissions).reply(500, 'Error');
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const changeAppIDButton = await screen.findByRole('button', { name: 'Change App ID' });
        expect(changeAppIDButton).toHaveClassName('disabled');
        fireEvent.mouseOver(changeAppIDButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText('Insufficient permissions to change App ID')).toBeInTheDocument();
      });
    });
  });

  describe('Legacy button', () => {
    describe('when legacy policies are supported and enabled', () => {
      it('renders an enabled button without tooltips', async () => {
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const legacyButton = await screen.findByRole('button', { name: 'Legacy existing violations' });
        expect(legacyButton).not.toHaveClassName('disabled');
        fireEvent.mouseOver(legacyButton);
        const tooltip = screen.queryByRole('tooltip');
        expect(tooltip).not.toBeInTheDocument();
      });
    });

    describe('when legacy policies are not supported', () => {
      it('renders a disabled button with tooltip "not supported by license"', async () => {
        renderComponent({
          router: {
            currentParams: { applicationPublicId: '4', '#': null },
            currentState: {
              data: { title: 'Application Management', viewportSized: true },
              name: 'management.view.application',
              url: '/application/{applicationPublicId}',
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'a28',
                publicId: '4',
                name: '44App',
                organizationId: 'cb53',
                organizationName: '4 org',
                contact: null,
              },
            },
          },
          productFeatures: {
            productFeatures: {
              'policy-grandfathering': false,
            },
          },
        });
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const legacyButton = await screen.findByRole('button', { name: 'Legacy existing violations' });
        expect(legacyButton).toHaveClassName('disabled');
        fireEvent.mouseOver(legacyButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText('Legacy Violations are not supported by your license')).toBeInTheDocument();
      });
    });

    describe('when legacy policy is supported but not enabled', () => {
      it('renders a disabled button with tooltip "not enabled"', async () => {
        renderComponent({
          router: {
            currentParams: { applicationPublicId: '4', '#': null },
            currentState: {
              data: { title: 'Application Management', viewportSized: true },
              name: 'management.view.application',
              url: '/application/{applicationPublicId}',
            },
          },
          legacyViolations: {
            data: {
              enabled: false,
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'a28',
                publicId: '4',
                name: '44App',
                organizationId: 'cb53',
                organizationName: '4 org',
                contact: null,
              },
            },
          },
          productFeatures: {
            productFeatures: {
              'policy-grandfathering': true,
            },
          },
        });
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const legacyButton = await screen.findByRole('button', { name: 'Legacy existing violations' });
        expect(legacyButton).toHaveClassName('disabled');
        fireEvent.mouseOver(legacyButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(
          within(tooltip).getByText('Legacy Violations are not enabled for this application.')
        ).toBeInTheDocument();
      });
    });
  });

  describe('Revoke Legacy Status button', () => {
    describe('when legacy status is supported', () => {
      it('renders an enabled button without tooltips', async () => {
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const revokeButton = await screen.findByRole('button', { name: 'Revoke legacy status' });
        expect(revokeButton).not.toHaveClassName('disabled');
        fireEvent.mouseOver(revokeButton);
        const tooltip = screen.queryByRole('tooltip');
        expect(tooltip).not.toBeInTheDocument();
      });
    });

    describe('when legacy status is not supported', () => {
      it('renders a disabled button with tooltip "not supported by license"', async () => {
        renderComponent({
          router: {
            currentParams: { applicationPublicId: '4', '#': null },
            currentState: {
              data: { title: 'Application Management', viewportSized: true },
              name: 'management.view.application',
              url: '/application/{applicationPublicId}',
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'a28',
                publicId: '4',
                name: '44App',
                organizationId: 'cb53',
                organizationName: '4 org',
                contact: null,
              },
            },
          },
          productFeatures: {
            productFeatures: {
              'policy-grandfathering': false,
            },
          },
        });
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const revokeButton = await screen.findByRole('button', { name: 'Revoke legacy status' });
        expect(revokeButton).toHaveClassName('disabled');
        fireEvent.mouseOver(revokeButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText('Legacy Violations are not supported by your license')).toBeInTheDocument();
      });
    });
  });

  describe('Evaluate a File', () => {
    describe('when evaluation is supported and has permissions', () => {
      it('renders an enabled button without tooltips', async () => {
        renderComponent();
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const evaluateButton = await screen.findByRole('button', { name: 'Evaluate a File' });
        expect(evaluateButton).not.toHaveClassName('disabled');
        fireEvent.mouseOver(evaluateButton);
        const tooltip = screen.queryByRole('tooltip');
        expect(tooltip).not.toBeInTheDocument();
      });
    });

    describe('when evaluation is not supported', () => {
      it('renders a disabled button with tooltip "not supported by license"', async () => {
        renderComponent({
          router: {
            currentParams: { applicationPublicId: '4', '#': null },
            currentState: {
              data: { title: 'Application Management', viewportSized: true },
              name: 'management.view.application',
              url: '/application/{applicationPublicId}',
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'a28',
                publicId: '4',
                name: '44App',
                organizationId: 'cb53',
                organizationName: '4 org',
                contact: null,
              },
            },
          },
          productFeatures: {
            productFeatures: {
              'cli-integration': false,
            },
          },
        });
        const actionButton = screen.getByRole('button', { name: 'Actions' });
        fireEvent.click(actionButton);
        const evaluateButton = await screen.findByRole('button', { name: 'Evaluate a File' });
        expect(evaluateButton).toHaveClassName('disabled');
        fireEvent.mouseOver(evaluateButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText('Evaluate application is not supported by your license.')).toBeInTheDocument();
      });
    });

    describe('when evaluation is supported', () => {
      describe('when there are no correct permissions', () => {
        it('renders a disabled button with tooltip "insufficient permissions"', async () => {
          axiosMock.onPut(getPermissionContextTestUrl('application', 'a28'), permissions).reply(200, ['WRITE']);
          renderComponent();
          const actionButton = screen.getByRole('button', { name: 'Actions' });
          fireEvent.click(actionButton);
          const evaluateButton = await screen.findByRole('button', { name: 'Evaluate a File' });
          expect(evaluateButton).toHaveClassName('disabled');
          fireEvent.mouseOver(evaluateButton);
          const tooltip = await screen.findByRole('tooltip');
          expect(within(tooltip).getByText('Insufficient permissions to evaluate application')).toBeInTheDocument();
        });
      });

      describe('when there is error in retrieving permissions', () => {
        it('renders a disabled button with tooltip "insufficient permissions"', async () => {
          axiosMock.onPut(getPermissionContextTestUrl('application', 'a28'), permissions).reply(500, 'Error');
          renderComponent();
          const actionButton = screen.getByRole('button', { name: 'Actions' });
          fireEvent.click(actionButton);
          const evaluateButton = await screen.findByRole('button', { name: 'Evaluate a File' });
          expect(evaluateButton).toHaveClassName('disabled');
          fireEvent.mouseOver(evaluateButton);
          const tooltip = await screen.findByRole('tooltip');
          expect(within(tooltip).getByText('Insufficient permissions to evaluate application')).toBeInTheDocument();
        });
      });
    });
  });

  it('View reports buttons (links)', () => {
    renderComponent({
      router: {
        currentParams: { applicationPublicId: '4', '#': null },
        currentState: {
          data: { title: 'Application Management', viewportSized: true },
          name: 'management.view.application',
          url: '/application/{applicationPublicId}',
        },
      },
      orgsAndPolicies: {
        stages: {
          dashboard: {
            stageTypes: [
              {
                shortName: 'Source',
                stageName: 'Source',
                stageTypeId: 'source',
              },
              { shortName: 'Build', stageName: 'Build', stageTypeId: 'build' },
              {
                shortName: 'Stage',
                stageName: 'Stage Release',
                stageTypeId: 'stage-release',
              },
              {
                shortName: 'Release',
                stageName: 'Release',
                stageTypeId: 'release',
              },
              {
                shortName: 'Operate',
                stageName: 'Operate',
                stageTypeId: 'operate',
              },
            ],
          },
        },
        root: {
          selectedOwner: {
            id: 'a28',
            publicId: '4',
            name: '44App',
            organizationId: 'cb53',
            organizationName: '4 org',
            contact: null,
          },
        },
        ownerActions: {
          actionDropdown: {
            loading: false,
            loadError: null,
            applicationSummary: {
              id: 'a28',
              publicId: '4',
              name: '44App',
              organizationId: 'cb53',
              organizationName: '4 org',
              policyEvaluations: {
                source: {
                  scanId: '0',
                },
                build: {
                  scanId: '1',
                },
                'stage-release': {
                  scanId: '2',
                },
                release: {
                  scanId: '3',
                },
                operate: {
                  scanId: '4',
                },
              },
              policyEvaluationsResults: {},
              contact: null,
              hasPendingSourceControlPolicyEvaluation: false,
            },
          },
        },
      },
    });
    const actionButton = screen.getByRole('button', { name: 'Actions' });
    fireEvent.click(actionButton);
    const sourceReport = screen.getByRole('button', { name: 'View source report' });
    const buildReport = screen.getByRole('button', { name: 'View build report' });
    const stageReport = screen.getByRole('button', { name: 'View stage report' });
    const releaseReport = screen.getByRole('button', { name: 'View release report' });
    const operateReport = screen.getByRole('button', { name: 'View operate report' });

    expect(sourceReport).not.toHaveClassName('disabled');
    expect(buildReport).not.toHaveClassName('disabled');
    expect(stageReport).not.toHaveClassName('disabled');
    expect(releaseReport).not.toHaveClassName('disabled');
    expect(operateReport).not.toHaveClassName('disabled');

    expect(sourceReport).toBeVisible();
    expect(buildReport).toBeVisible();
    expect(stageReport).toBeVisible();
    expect(releaseReport).toBeVisible();
    expect(operateReport).toBeVisible();
  });
});
