/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, render, fireEvent, within } from 'TestRoot/SpecUtil';

import { pathSet } from 'MainRoot/util/jsUtil';
import ViolationDetailsTile from 'MainRoot/violation/ViolationDetailsTile';
import * as commonServices from 'MainRoot/util/CommonServices';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';

describe('ViolationDetailsTile', function () {
  let mockTimeAgo, stateGetMock, stateHrefMock, minimalProps, stateGoMock, goToWaiversMock, renderComponent;

  beforeEach(function () {
    let mockTimeAgoCallCounter = 0;

    mockTimeAgo = jest.spyOn(commonServices, 'timeAgo').mockImplementation(function () {
      mockTimeAgoCallCounter++;

      return {
        age: mockTimeAgoCallCounter,
        qualifier: (mockTimeAgoCallCounter % 2 ? 'weeks' : 'days') + ' ago',
      };
    });

    jest.fn('timeAgo').mockImplementation(function () {
      mockTimeAgoCallCounter++;
      console.log('got here');

      return {
        age: mockTimeAgoCallCounter,
        qualifier: (mockTimeAgoCallCounter % 2 ? 'weeks' : 'days') + ' ago',
      };
    });
    stateGoMock = jest.fn('stateGo').mockImplementation(() => {});
    goToWaiversMock = jest.fn('stateGo').mockImplementation(() => {});
    stateGetMock = jest.fn('$state.get').mockReturnValue('theState');
    stateHrefMock = jest.fn('$state.href').mockReturnValue('#/foo');

    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      get: stateGetMock,
      href: stateHrefMock,
    });

    minimalProps = {
      selectedViolationId: 'selectedViolationId',
      violationDetails: {
        policyViolationId: 'policyViolationId',
        policyName: 'pol',
        policyThreatCategory: 'security',
        policyOwner: {
          ownerName: 'polOwner',
          ownerType: 'organization',
          ownerId: '1234',
        },
        threatLevel: 8,
        openTime: '2020-03-02T16:53:33.263Z',
        stageData: {
          build: {
            mostRecentEvaluationTime: '2020-03-04T16:53:33.263Z',
            mostRecentScanId: 'scan2',
            actionTypeId: null,
          },
          'stage-release': {
            mostRecentEvaluationTime: '2020-03-03T16:53:33.263Z',
            mostRecentScanId: 'scan1',
            actionTypeId: 'fail',
          },
        },
        applicationPublicId: 'app1',
        organizationName: 'Org 1',
        applicationName: 'App 1',
        displayName: { foo: 'bar', parts: [{ value: 'foo' }] },
        filenames: ['/foo/bar'],
        waived: true,
      },
      stageTypes: [
        { stageTypeId: 'build', shortName: 'Build' },
        { stageTypeId: 'stage-release', shortName: 'Stage' },
        { stageTypeId: 'release', shortName: 'Release' },
      ],
      applicationPublicId: 'app1',
      stateGo: stateGoMock,
      goToWaivers: goToWaiversMock,
      isFromPolicyViolations: false,
      activeWaivers: [],
      isFirewallContext: false,
      policyDetail: {
        policyViolationId: '02a6107559a94c39b04d4ec8374b9508',
        policyId: 'd98fb873ed1f48e5b00316d8acddbc0f',
        policyName: 'Security-Medium',
        policyOwner: {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
        },
        policyThreatLevel: 7,
        policyThreatCategory: 'SECURITY',
        constraints: [
          {
            constraintId: 'c6436a5a051046b1ba2aa94e9fd82a51',
            constraintName: 'Medium risk CVSS score',
            constraintOperator: 'AND',
            conditions: [
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity >= 4',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity >= 4 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
              {
                conditionType: 'SecurityVulnerabilitySeverity',
                conditionSummary: 'Security Vulnerability Severity < 7',
                conditionReason: 'Found security vulnerability CVE-2012-2098 with severity < 7 (severity = 5.0)',
                conditionTriggerReference: {
                  value: 'CVE-2012-2098',
                  type: 'SECURITY_VULNERABILITY_REFID',
                },
              },
            ],
          },
        ],
        policyActionTypeId: null,
        lastReported: '2022-08-10T13:35:40.641+03:00',
      },
      hasPermissionForAppWaivers: true,
      constraintViolations: [
        {
          constraintName: 'Constraint 1',
          reasons: [],
          conditions: [],
        },
        {
          constraintName: 'Constraint 2',
          reasons: [],
          conditions: [],
        },
      ],
    };

    renderComponent = (props, preloadedState) =>
      render(<ViolationDetailsTile {...minimalProps} {...props} />, preloadedState ? { preloadedState } : undefined);
  });

  describe('header', function () {
    it('renders a NxPolicyViolationIndicator and "Violation of (policyName)"', function () {
      renderComponent();
      expect(screen.getByText('critical')).toBeInTheDocument();
      expect(screen.getByRole('img', { name: 'threat level critical' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /Violation of pol/i })).toBeInTheDocument();
    });

    it('appends "Policy no longer exists" to the title when policyOwner prop has null ownerId', function () {
      renderComponent(pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps));
      expect(screen.getByRole('heading', { name: /Violation of pol/i })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /Policy no longer exists/i })).toBeInTheDocument();
    });

    describe('sets the correct threatLevelCategory on the NxPolicyViolationIndicator', function () {
      it('when threatLevelCategory = 8', () => {
        renderComponent();
        expect(screen.getByText('critical')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'threat level critical' })).toBeInTheDocument();
      });

      it('when threatLevelCategory = 7', () => {
        renderComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 7,
          },
        });
        expect(screen.getByText('severe')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'threat level severe' })).toBeInTheDocument();
      });

      it('when threatLevelCategory = 3', () => {
        renderComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 3,
          },
        });
        expect(screen.getByText('moderate')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'threat level moderate' })).toBeInTheDocument();
      });

      it('when threatLevelCategory = 1', () => {
        renderComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 1,
          },
        });
        expect(screen.getByText('low')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'threat level low' })).toBeInTheDocument();
      });

      it('when threatLevelCategory = 0', () => {
        renderComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 0,
          },
        });
        expect(screen.getByText('none')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'threat level none' })).toBeInTheDocument();
      });
    });

    it('renders a ViolationDetailsSubtitle', function () {
      renderComponent();
      expect(screen.getByText('Org 1')).toBeInTheDocument();
      expect(screen.getByText('App 1')).toBeInTheDocument();
      expect(screen.getByText('foo')).toBeInTheDocument();
    });

    it('renders an Add Waiver menu button that navigates to the Add Waiver page when clicked', function () {
      renderComponent();
      const addWaiverButton = screen.getByRole('button', { name: /Add Waiver/i });
      expect(addWaiverButton).toBeInTheDocument();
      fireEvent.click(addWaiverButton);
      expect(stateGoMock).toHaveBeenCalledWith('addWaiver', {
        violationId: 'selectedViolationId',
      });
    });
  });

  describe('When hasPermissionForAppWaivers is false', () => {
    it('renders an Request Waiver menu button that navigates to the Add Waiver page when clicked', function () {
      renderComponent({
        ...minimalProps,
        hasPermissionForAppWaivers: false,
      });
      const requestWaiverButton = screen.getByRole('button', { name: /Request Waiver/i });
      expect(requestWaiverButton).toBeInTheDocument();
      fireEvent.click(requestWaiverButton);

      expect(stateGoMock).toHaveBeenCalledWith('requestWaiver', {
        violationId: 'selectedViolationId',
      });
    });
  });

  it('hide header subtitle if isFirewallContext active', () => {
    renderComponent({ isFirewallContext: true });
    expect(screen.queryByText('Org 1')).not.toBeInTheDocument();
    expect(screen.queryByText('App 1')).not.toBeInTheDocument();
    expect(screen.queryByText('foo')).not.toBeInTheDocument();
  });

  describe('threat level section', function () {
    it('contains a term and definition with the threat level', function () {
      renderComponent();

      expect(screen.getByText('Threat Level')).toBeInTheDocument();

      const threatLevel = screen.getByRole('definition', { name: 'Threat Level' });
      expect(threatLevel).toBeInTheDocument();
      expect(threatLevel).toHaveTextContent('8');
    });

    describe('sets the iq-threat-level class on the dd and a modifier class based on the threat level category', function () {
      let renderComponentByThreatLevel;

      const threatLevelsToTest = [7, 3, 1, 0];
      const threatLevelClassMap = {
        7: 'severe',
        3: 'moderate',
        1: 'low',
        0: 'none',
      };

      beforeEach(() => {
        renderComponentByThreatLevel = (threatLevel) => {
          renderComponent({
            ...minimalProps,
            violationDetails:
              threatLevel == null
                ? minimalProps.violationDetails
                : {
                    ...minimalProps.violationDetails,
                    threatLevel,
                  },
          });
        };
      });

      threatLevelsToTest.forEach((threatLevel) => {
        it(`sets the iq-threat-level class based on the threat level category when threatLevel = ${threatLevel}`, () => {
          renderComponentByThreatLevel(threatLevel);
          const element = screen.getByRole('definition', { name: 'Threat Level' });
          expect(element.classList).toContain('iq-threat-level');
          expect(element.classList).toContain(`iq-threat-level--${threatLevelClassMap[threatLevel]}`);
        });
      });
    });
  });

  describe('policy type section', function () {
    it('contains a term and definition with the policyThreatCategory capitalized', function () {
      renderComponent();

      expect(screen.getByText('Policy Type')).toBeInTheDocument();

      const policyType = screen.getByRole('definition', { name: 'Policy Type' });
      expect(policyType).toBeInTheDocument();
      expect(policyType).toHaveTextContent('Security');
    });
  });

  describe('first reported section', function () {
    it('contains a dt and a dd with the first reported time displayed in relative terms', function () {
      renderComponent();
      expect(mockTimeAgo).toHaveBeenCalled();

      const firstReported = screen.getByRole('definition', { name: 'First Reported' });
      expect(firstReported).toBeInTheDocument();
      expect(firstReported).toHaveTextContent('1 weeks ago');
    });

    it('hidden first report section if isFirewallContext is true', () => {
      renderComponent({ isFirewallContext: true });
      expect(screen.queryByRole('definition', { name: 'First Reported' })).not.toBeInTheDocument();
    });

    it('show first report section if isFirewallContext is false', () => {
      renderComponent({ isFirewallContext: false });

      const firstReported = screen.getByRole('definition', { name: 'First Reported' });
      expect(firstReported).toBeInTheDocument();
    });
  });

  describe('second reported section', function () {
    it('contains a dt and a dd with the highest mostRecentEvaluationTime time displayed in relative terms', function () {
      renderComponent();
      expect(mockTimeAgo).toHaveBeenCalled();

      const lastReported = screen.getByRole('definition', { name: 'Last Reported' });
      expect(lastReported).toBeInTheDocument();
      expect(lastReported).toHaveTextContent('2 days ago');
    });
  });

  describe('stages section', function () {
    it('contains a dt and for each stage in stageTypes a dd containins a StageDisplay', function () {
      renderComponent();

      const stages = screen.getAllByRole('definition', { name: 'Stages' });

      expect(stages).toHaveLength(3);
      expect(stages[0]).toHaveTextContent('Build');
      expect(stages[1]).toHaveTextContent('Stage');
      expect(stages[2]).toHaveTextContent('Release');
    });

    it('hide stage section if isFirewallContext is true', () => {
      renderComponent({ isFirewallContext: true });
      expect(screen.queryByRole('definition', { name: 'Stages' })).not.toBeInTheDocument();
    });

    it('show stage section if isFirewallContext is false', () => {
      renderComponent({ isFirewallContext: false });

      const stages = screen.getAllByRole('definition', { name: 'Stages' });
      expect(stages).toHaveLength(3);
    });
  });

  describe('policy owner section', function () {
    it('contains a dt and a dd with a link', function () {
      renderComponent();

      const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });
      expect(policyOwner).toBeInTheDocument();
      expect(policyOwner).toHaveTextContent('polOwner');
      expect(within(policyOwner).getByRole('link', { name: 'polOwner' })).toBeInTheDocument();
    });

    describe('when the policy owner is an org', function () {
      it('sets the link href using the management.view.organization state and the org id', function () {
        renderComponent();

        const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

        const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
        expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

        fireEvent.click(policyOwnerLink);
        expect(stateGetMock).toHaveBeenCalledWith('management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });
    });

    describe('when the policy owner is an org and comes from sbomManager', function () {
      it('sets the link href using the sbomManager.management.view.organization state and the org id', function () {
        const sbomManagerState = {
          router: { currentState: { name: 'sbomManager.management.view.organization' }, currentParams: {} },
        };
        renderComponent({}, sbomManagerState);

        const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

        const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
        expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

        fireEvent.click(policyOwnerLink);
        expect(stateGetMock).toHaveBeenCalledWith('sbomManager.management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });
    });

    describe('when the policy owner is an org and comes from firewall', function () {
      it('sets the link href using the firewall.management.view.organization state and the org id', function () {
        const firewallState = {
          router: { currentState: { name: 'firewall.management.view.organization' }, currentParams: {} },
        };
        renderComponent({}, firewallState);

        const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

        const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
        expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

        fireEvent.click(policyOwnerLink);
        expect(stateGetMock).toHaveBeenCalledWith('firewall.management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });
    });
  });

  describe('when the policy owner is an app', function () {
    const renderComponentWithAppProps = (additionalProps) =>
      renderComponent({
        ...pathSet(
          ['violationDetails', 'policyOwner'],
          {
            ownerName: 'polOwner',
            ownerType: 'application',
            ownerId: '1234',
            ownerPublicId: 'app2',
          },
          minimalProps
        ),
        ...additionalProps,
      });

    it('sets the link href using the management.view.application state and the app public id', function () {
      renderComponentWithAppProps();
      const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

      const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
      expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

      fireEvent.click(policyOwnerLink);

      expect(stateGetMock).toHaveBeenCalledWith('management.view.application');
      expect(stateHrefMock).toHaveBeenCalledWith('theState', {
        applicationPublicId: 'app2',
      });
    });

    it('sets the link href using the sbomManager.management.view.application state and the app public id', function () {
      const sbomManagerState = {
        router: { currentState: { name: 'sbomManager.management.view.application' }, currentParams: {} },
      };
      renderComponent(
        {
          ...pathSet(
            ['violationDetails', 'policyOwner'],
            {
              ownerName: 'polOwner',
              ownerType: 'application',
              ownerId: '1234',
              ownerPublicId: 'app2',
            },
            minimalProps
          ),
        },
        sbomManagerState
      );
      const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

      const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
      expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

      fireEvent.click(policyOwnerLink);

      expect(stateGetMock).toHaveBeenCalledWith('sbomManager.management.view.application');
      expect(stateHrefMock).toHaveBeenCalledWith('theState', {
        applicationPublicId: 'app2',
      });
    });

    it('sets the link href using the firewall.management.view.application state and the app public id', function () {
      const firewallState = {
        router: { currentState: { name: 'firewall.management.view.application' }, currentParams: {} },
      };
      renderComponent(
        {
          ...pathSet(
            ['violationDetails', 'policyOwner'],
            {
              ownerName: 'polOwner',
              ownerType: 'application',
              ownerId: '1234',
              ownerPublicId: 'app2',
            },
            minimalProps
          ),
        },
        firewallState
      );
      const policyOwner = screen.getByRole('definition', { name: 'Policy Owner' });

      const policyOwnerLink = within(policyOwner).getByRole('link', { name: 'polOwner' });
      expect(policyOwnerLink).toHaveAttribute('href', '#/foo');

      fireEvent.click(policyOwnerLink);

      expect(stateGetMock).toHaveBeenCalledWith('firewall.management.view.application');
      expect(stateHrefMock).toHaveBeenCalledWith('theState', {
        applicationPublicId: 'app2',
      });
    });
  });

  describe('when policyOwner prop has null ownerId', function () {
    it('renders "Policy no longer exists" message', function () {
      renderComponent(pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps));
      expect(screen.getByRole('heading', { name: /Policy no longer exists/i })).toBeInTheDocument();
    });
  });

  describe('policy violations constraint info', function () {
    it('renders policy violation constraint info component', function () {
      renderComponent();

      expect(screen.getByRole('heading', { name: 'Policy Constraint' })).toBeInTheDocument();
      expect(screen.getByRole('list')).toBeInTheDocument();
    });
  });

  describe('reachability analysis section', () => {
    it('is not rendered if violationDetails does not contain reachabilityStatus', () => {
      renderComponent();
      expect(screen.queryByRole('heading', { name: 'Reachability Analysis' })).not.toBeInTheDocument();
    });

    it('renders "Reachable" if reachabilityStatus = "REACHABLE"', () => {
      renderComponent(pathSet(['violationDetails', 'reachabilityStatus'], 'REACHABLE', minimalProps));
      expect(screen.getByRole('heading', { name: 'Reachability Analysis' })).toBeInTheDocument();
      expect(screen.getByText('Reachable')).toBeInTheDocument();
    });

    it('renders "Not reachable" if reachabilityStatus = "NON_REACHABLE"', () => {
      renderComponent(pathSet(['violationDetails', 'reachabilityStatus'], 'NON_REACHABLE', minimalProps));
      expect(screen.getByRole('heading', { name: 'Reachability Analysis' })).toBeInTheDocument();
      expect(screen.getByText('Not reachable')).toBeInTheDocument();
    });

    it('is not rendered if reachabilityStatus is blank', () => {
      renderComponent(pathSet(['violationDetails', 'reachabilityStatus'], '', minimalProps));
      expect(screen.queryByRole('heading', { name: 'Reachability Analysis' })).not.toBeInTheDocument();
    });

    it('is not rendered if reachabilityStatus is null', () => {
      renderComponent(pathSet(['violationDetails', 'reachabilityStatus'], null, minimalProps));
      expect(screen.queryByRole('heading', { name: 'Reachability Analysis' })).not.toBeInTheDocument();
    });

    it('renders "Not reachable" if reachabilityStatus has any other string', () => {
      renderComponent(pathSet(['violationDetails', 'reachabilityStatus'], 'some_string', minimalProps));
      expect(screen.getByText('Not reachable')).toBeInTheDocument();
    });

    describe('evidence accordion', () => {
      const reachableProps = () => ({
        ...pathSet(['violationDetails', 'reachabilityStatus'], 'REACHABLE', minimalProps),
      });

      function evidenceState(evidence, overrides = {}) {
        return {
          reachabilityEvidence: {
            loading: false,
            loadError: null,
            evidence,
            isOpen: false,
            currentRequestId: null,
            ...overrides,
          },
        };
      }

      function getPathTrace(pathIndex = 0) {
        const traces = screen.getAllByRole('region', { name: /call path trace/i });
        return traces[pathIndex]?.textContent;
      }

      function renderWithEvidence(response, extraProps = {}) {
        renderComponent({ ...reachableProps(), ...extraProps }, evidenceState(response, { isOpen: true }));
      }

      it('does not render evidence accordion when NON_REACHABLE', () => {
        renderComponent(
          pathSet(['violationDetails', 'reachabilityStatus'], 'NON_REACHABLE', minimalProps),
          evidenceState({ paths: [{ segments: [{ type: 'method', method: 'com/A.b()V', filePath: '/a.jar', component: null }] }], truncated: false })
        );
        expect(screen.queryByRole('button', { name: /reachability evidence/i })).not.toBeInTheDocument();
      });

      it('does not render evidence accordion when evidence is null (still loading)', () => {
        renderComponent(reachableProps(), evidenceState(null));
        expect(screen.queryByRole('button', { name: /reachability evidence/i })).not.toBeInTheDocument();
      });

      it('does not render evidence accordion when evidence has empty paths (404 sentinel)', () => {
        renderComponent(reachableProps(), evidenceState({ paths: [], truncated: false }));
        expect(screen.queryByRole('button', { name: /reachability evidence/i })).not.toBeInTheDocument();
      });

      it('renders evidence accordion when evidence has paths', () => {
        renderComponent(
          reachableProps(),
          evidenceState({ paths: [{ segments: [{ type: 'method', method: 'com/A.b()V', filePath: '/a.jar', component: null }] }], truncated: false })
        );
        expect(screen.getByRole('button', { name: /reachability evidence/i })).toBeInTheDocument();
      });

      it('shows error accordion when there is a load error', () => {
        renderComponent(
          reachableProps(),
          evidenceState(null, { loadError: 'Failed to load reachability evidence', isOpen: true })
        );
        expect(screen.getByRole('button', { name: /reachability evidence/i })).toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveTextContent(/Failed to load reachability evidence/);
      });

      it('renders a single-method path', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/example/App.run()V', filePath: '/app.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('App.run()');
      });

      it('renders a direct call chain in one component (no gaps, no elision)', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/Main.main([Ljava/lang/String;)V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/app/Service.process()V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/app/Dao.query()V', filePath: '/app.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('Main.main()Service.process()Dao.query()');
      });

      it('renders intra-component gaps as "..."', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/A.start()V', filePath: '/app.jar', component: null },
            { type: 'gap' },
            { type: 'method', method: 'com/app/B.middle()V', filePath: '/app.jar', component: null },
            { type: 'gap' },
            { type: 'method', method: 'com/app/C.end()V', filePath: '/app.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('A.start()...B.middle()...C.end()');
      });

      it('renders cross-component elision as "... N more components ..."', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/Entry.start()V', filePath: '/app.jar', component: null },
            { type: 'elided', count: 5 },
            { type: 'method', method: 'com/vuln/Sink.exec()V', filePath: '/vuln.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('Entry.start()... 5 more components ...Sink.exec()');
      });

      it('renders multiple boundary crossings as separate sections labeled by component or filePath', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/Controller.handle()V', filePath: '/app.jar', component: 'pkg:maven/com.app/app@1.0' },
            { type: 'method', method: 'com/lib/Service.call()V', filePath: '/lib.jar', component: null },
            { type: 'method', method: 'com/data/Repo.find()V', filePath: '/data.jar', component: 'pkg:maven/com.data/repo@1.0' },
            { type: 'method', method: 'com/vuln/Parser.parse()V', filePath: '/vuln.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('Controller.handle()Service.call()Repo.find()Parser.parse()');
        expect(screen.getByRole('group', { name: 'pkg:maven/com.app/app@1.0' })).toHaveTextContent('Controller.handle()');
        expect(screen.getByRole('group', { name: '/lib.jar' })).toHaveTextContent('Service.call()');
        expect(screen.getByRole('group', { name: 'pkg:maven/com.data/repo@1.0' })).toHaveTextContent('Repo.find()');
        expect(screen.getByRole('group', { name: '/vuln.jar' })).toHaveTextContent('Parser.parse()');
      });

      it('groups consecutive same-filePath methods in one section', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/A.one()V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/app/A.two()V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/app/A.three()V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/lib/B.start()V', filePath: '/lib.jar', component: null },
            { type: 'method', method: 'com/lib/B.end()V', filePath: '/lib.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('A.one()A.two()A.three()B.start()B.end()');
        const appSection = screen.getByRole('group', { name: '/app.jar' });
        const libSection = screen.getByRole('group', { name: '/lib.jar' });
        expect(appSection.textContent).toBe('A.one()A.two()A.three()');
        expect(libSection.textContent).toBe('B.start()B.end()');
      });

      it('renders a complex path with gaps, elision, and multiple components', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/Entry.main([Ljava/lang/String;)V', filePath: '/app.jar', component: null },
            { type: 'gap' },
            { type: 'method', method: 'com/app/Entry.init()V', filePath: '/app.jar', component: null },
            { type: 'elided', count: 12 },
            { type: 'method', method: 'com/vuln/Jackson.deserialize(Ljava/io/InputStream;)Ljava/lang/Object;', filePath: '/jackson.jar', component: null },
            { type: 'gap' },
            { type: 'method', method: 'com/vuln/Jackson.readValue()V', filePath: '/jackson.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe(
          'Entry.main()...Entry.init()... 12 more components ...Jackson.deserialize()...Jackson.readValue()'
        );
        expect(screen.getByRole('group', { name: '/app.jar' })).toBeInTheDocument();
        expect(screen.getByRole('group', { name: '/jackson.jar' })).toBeInTheDocument();
      });

      it('renders multiple paths with correct headings and traces', () => {
        renderWithEvidence({
          paths: [
            { segments: [
              { type: 'method', method: 'com/app/Main.main([Ljava/lang/String;)V', filePath: '/app.jar', component: null },
              { type: 'method', method: 'com/vuln/Lib.bad()V', filePath: '/lib.jar', component: null },
            ] },
            { segments: [
              { type: 'method', method: 'com/app/Test.run()V', filePath: '/app.jar', component: null },
              { type: 'gap' },
              { type: 'method', method: 'com/app/Test.call()V', filePath: '/app.jar', component: null },
              { type: 'method', method: 'com/vuln/Lib.bad()V', filePath: '/lib.jar', component: null },
            ] },
          ],
          truncated: true,
        });

        expect(screen.getByRole('heading', { name: 'Path 1' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Path 2' })).toBeInTheDocument();
        expect(getPathTrace(0)).toBe('Main.main()Lib.bad()');
        expect(getPathTrace(1)).toBe('Test.run()...Test.call()Lib.bad()');
      });

      it('renders a fully-elided middle with boundaries on both sides', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/A.first()V', filePath: '/app.jar', component: null },
            { type: 'method', method: 'com/app/A.second()V', filePath: '/app.jar', component: null },
            { type: 'elided', count: 20 },
            { type: 'method', method: 'com/vuln/Z.penultimate()V', filePath: '/vuln.jar', component: null },
            { type: 'method', method: 'com/vuln/Z.last()V', filePath: '/vuln.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('A.first()A.second()... 20 more components ...Z.penultimate()Z.last()');
      });

      it('renders consecutive elision segments', () => {
        renderWithEvidence({
          paths: [{ segments: [
            { type: 'method', method: 'com/app/A.entry()V', filePath: '/a.jar', component: null },
            { type: 'elided', count: 3 },
            { type: 'method', method: 'com/mid/B.mid()V', filePath: '/b.jar', component: null },
            { type: 'elided', count: 7 },
            { type: 'method', method: 'com/vuln/C.sink()V', filePath: '/c.jar', component: null },
          ] }],
          truncated: true,
        });

        expect(getPathTrace()).toBe('A.entry()... 3 more components ...B.mid()... 7 more components ...C.sink()');
      });
    });
  });
});
