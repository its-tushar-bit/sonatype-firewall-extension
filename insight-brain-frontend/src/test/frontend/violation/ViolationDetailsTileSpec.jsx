/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ViolationDetailsSubtitle from 'MainRoot/violation/ViolationDetailsSubtitle';
import StageDisplay from 'MainRoot/violation/StageDisplay';
import { pathSet } from 'MainRoot/util/jsUtil';
import { NxTextLink, NxPolicyViolationIndicator } from '@sonatype/react-shared-components';
import PolicyViolationConstraintInfo from 'MainRoot/violation/PolicyViolationConstraintInfo';
import AddOrRequestWaiverButton from 'MainRoot/waivers/AddOrRequestWaiverButton';

describe('ViolationDetailsTile', function () {
  let timeAgoMock,
    mockDate,
    dateCreatorMock,
    ViolationDetailsTile,
    stateGetMock,
    stateHrefMock,
    minimalProps,
    stateGoMock,
    goToWaiversMock,
    getShallowComponent,
    getMountedComponent,
    getMountedComponentWithAutoClean,
    mountedComponent;

  beforeEach(function () {
    let timeAgoCallCounter = 0;
    timeAgoMock = jasmine.createSpy('timeAgo').and.callFake(function () {
      timeAgoCallCounter++;
      return {
        age: timeAgoCallCounter,
        qualifier: (timeAgoCallCounter % 2 ? 'weeks' : 'days') + ' ago',
      };
    });

    mockDate = new Date();
    dateCreatorMock = spyOn(window, 'Date').and.returnValue(mockDate);
    stateGoMock = jasmine.createSpy('stateGo');
    goToWaiversMock = jasmine.createSpy('stateGo');
    stateGetMock = jasmine.createSpy('$state.get').and.returnValue('theState');
    stateHrefMock = jasmine.createSpy('$state.href').and.returnValue('#/foo');
    minimalProps = {
      $state: {
        get: stateGetMock,
        href: stateHrefMock,
        params: {
          type: 'violation',
          sidebarReference: 'filter',
        },
      },
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
        },
        {
          constraintName: 'Constraint 2',
          reasons: [],
        },
      ],
    };

    ViolationDetailsTile = require('inject-loader!../../../main/frontend/violation/ViolationDetailsTile')({
      '../utilAngular/CommonServices': { timeAgo: timeAgoMock },
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(ViolationDetailsTile, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ViolationDetailsTile, minimalProps);
    getMountedComponentWithAutoClean = (additionalProps) => {
      mountedComponent = getMountedComponent(additionalProps);
      return mountedComponent;
    };
  });

  afterEach(() => {
    mountedComponent?.unmount();
    mountedComponent = null;
  });

  it('renders an nx-tile with the iq-violation-details class', function () {
    expect(getShallowComponent()).toMatchSelector('.nx-tile.iq-violation-details');
  });

  describe('header', function () {
    it('renders a NxPolicyViolationIndicator and "Violation of (policyName)"', function () {
      const header = getMountedComponentWithAutoClean();
      const policyViolationIndicator = header.find(NxPolicyViolationIndicator);
      expect(policyViolationIndicator.exists()).toBe(true);
      expect(header.text().trim()).toContain('Violation of pol');
    });

    it('appends "Policy no longer exists" to the title when policyOwner prop has null ownerId', function () {
      const component = getMountedComponentWithAutoClean(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        ),
        header = component.find('.nx-tile-header .nx-tile-header__title'),
        texts = header.find('span');
      expect(texts.at(0)).toHaveText('Violation of pol');
      expect(texts.at(3)).toHaveText('Policy no longer exists');
    });

    it('sets the correct threatLevelCategory on the NxPolicyViolationIndicator', function () {
      expect(getMountedComponentWithAutoClean().find(NxPolicyViolationIndicator)).toHaveProp(
        'threatLevelCategory',
        'critical'
      );
      expect(
        getMountedComponentWithAutoClean({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 7,
          },
        }).find(NxPolicyViolationIndicator)
      ).toHaveProp('threatLevelCategory', 'severe');
      expect(
        getMountedComponentWithAutoClean({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 3,
          },
        }).find(NxPolicyViolationIndicator)
      ).toHaveProp('threatLevelCategory', 'moderate');
      expect(
        getMountedComponentWithAutoClean({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 1,
          },
        }).find(NxPolicyViolationIndicator)
      ).toHaveProp('threatLevelCategory', 'low');
      expect(
        getMountedComponentWithAutoClean({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 0,
          },
        }).find(NxPolicyViolationIndicator)
      ).toHaveProp('threatLevelCategory', 'none');
      expect(
        getMountedComponentWithAutoClean(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        ).find(NxPolicyViolationIndicator)
      ).toHaveProp('threatLevelCategory', null);
    });

    it('renders the policy name in an <em>', function () {
      expect(getMountedComponentWithAutoClean().find('.nx-tile-header__title h2 em')).toHaveText('pol');
    });

    it('renders a ViolationDetailsSubtitle', function () {
      const subtitle = getShallowComponent().find('.nx-tile-header').find(ViolationDetailsSubtitle);

      expect(subtitle).toExist();
      expect(subtitle).toHaveProp('organizationName', minimalProps.violationDetails.organizationName);
      expect(subtitle).toHaveProp('applicationName', minimalProps.violationDetails.applicationName);
      expect(subtitle).toHaveProp('displayName', minimalProps.violationDetails.displayName);
      expect(subtitle).toHaveProp('filenames', minimalProps.violationDetails.filenames);
    });

    it('renders an Add Waiver menu button that navigates to the Add Waiver page when clicked', function () {
      const addWaiverButton = getShallowComponent()
        .find(AddOrRequestWaiverButton)
        .dive()
        .find('#violation-page-add-waiver');

      expect(addWaiverButton).toExist();

      addWaiverButton.simulate('click');
      expect(stateGoMock).toHaveBeenCalledWith('addWaiver', {
        violationId: 'selectedViolationId',
      });
    });

    describe('When hasPermissionForAppWaivers is false', () => {
      beforeEach(() => {
        getShallowComponent = enzymeUtils.getShallowComponent(ViolationDetailsTile, {
          ...minimalProps,
          hasPermissionForAppWaivers: false,
        });
      });

      it('renders an Request Waiver menu button that navigates to the Add Waiver page when clicked', function () {
        const mountedComponent = getShallowComponent();
        const requestWaiverButton = mountedComponent
          .find(AddOrRequestWaiverButton)
          .dive()
          .find('#violation-page-request-waiver');

        expect(requestWaiverButton).toExist();

        requestWaiverButton.at(0).simulate('click');
        expect(stateGoMock).toHaveBeenCalledWith('requestWaiver', {
          violationId: 'selectedViolationId',
        });
      });
    });

    it('show header subtitle if isFirewallContext not active', () => {
      const component = getShallowComponent({ isFirewallContext: false });
      expect(component.find(ViolationDetailsSubtitle)).toExist();
    });

    it('hide header subtitle if isFirewallContext active', () => {
      const component = getShallowComponent({ isFirewallContext: true });
      expect(component.find(ViolationDetailsSubtitle)).not.toExist();
    });
  });

  describe('left content', function () {
    it('is a dl', function () {
      expect(getShallowComponent().find('.iq-violation-details__left-details')).toMatchSelector('dl');
    });
  });

  describe('right content', function () {
    it('is a dl', function () {
      expect(getShallowComponent().find('.iq-violation-details__right-details')).toMatchSelector('dl');
    });
  });

  describe('threat level section', function () {
    it('contains a dt and a dd with the threat level', function () {
      const component = getShallowComponent().find('.iq-violation-details__threat-level');

      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('8');
    });

    it('sets the iq-threat-level class on the dd and a modifier class based on the threat level category', function () {
      const getDd = (threatLevel) =>
        getShallowComponent({
          ...minimalProps,
          violationDetails:
            threatLevel == null
              ? minimalProps.violationDetails
              : {
                  ...minimalProps.violationDetails,
                  threatLevel,
                },
        }).find('.iq-violation-details__threat-level dd');

      expect(getDd()).toHaveClassName('iq-threat-level');
      expect(getDd()).toHaveClassName('iq-threat-level--critical');
      expect(getDd(7)).toHaveClassName('iq-threat-level--severe');
      expect(getDd(3)).toHaveClassName('iq-threat-level--moderate');
      expect(getDd(1)).toHaveClassName('iq-threat-level--low');
      expect(getDd(0)).toHaveClassName('iq-threat-level--none');
    });
  });

  describe('policy type section', function () {
    it('contains a dt and a dd with the policyThreatCategory capitalized', function () {
      const component = getShallowComponent().find('.iq-violation-details__policy-type');

      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('Security');
    });
  });

  describe('first reported section', function () {
    it('contains a dt and a dd with the first reported time displayed in relative terms', function () {
      const component = getShallowComponent().find('.iq-violation-details__first-reported');

      expect(dateCreatorMock).toHaveBeenCalledWith('2020-03-02T16:53:33.263Z');
      expect(timeAgoMock).toHaveBeenCalledWith(mockDate);
      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('1 weeks ago');
    });

    it('hidden first report section if isFirewallContext is true', () => {
      const component = getShallowComponent({ isFirewallContext: true });
      expect(component.find('.iq-violation-details__first-reported')).not.toExist();
    });

    it('show first report section if isFirewallContext is false', () => {
      const component = getShallowComponent({ isFirewallContext: false });
      expect(component.find('.iq-violation-details__first-reported')).toExist();
    });
  });

  describe('second reported section', function () {
    it('contains a dt and a dd with the highest mostRecentEvaluationTime time displayed in relative terms', function () {
      const component = getShallowComponent().find('.iq-violation-details__last-reported');

      expect(dateCreatorMock).toHaveBeenCalledWith('2020-03-04T16:53:33.263Z');
      expect(timeAgoMock).toHaveBeenCalledWith(mockDate);
      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('2 days ago');
    });
  });

  describe('stages section', function () {
    it('contains a dt and for each stage in stageTypes a dd containins a StageDisplay', function () {
      const component = getShallowComponent().find('.iq-violation-details__stages'),
        dds = component.find('dd');

      expect(component.find('dt')).toExist();
      expect(dds.length).toBe(3);

      expect(dds.at(0).key()).toBe('build');
      expect(dds.at(0).children()).toMatchSelector(StageDisplay);
      expect(dds.at(0).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(0).children()).toHaveProp('stageType', minimalProps.stageTypes[0]);
      expect(dds.at(0).children()).toHaveProp('stageData', minimalProps.violationDetails.stageData.build);
      expect(dds.at(0).children()).toHaveProp('applicationPublicId', 'app1');

      expect(dds.at(1).key()).toBe('stage-release');
      expect(dds.at(1).children()).toMatchSelector(StageDisplay);
      expect(dds.at(1).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(1).children()).toHaveProp('stageType', minimalProps.stageTypes[1]);
      expect(dds.at(1).children()).toHaveProp('stageData', minimalProps.violationDetails.stageData['stage-release']);
      expect(dds.at(1).children()).toHaveProp('applicationPublicId', 'app1');

      expect(dds.at(2).key()).toBe('release');
      expect(dds.at(2).children()).toMatchSelector(StageDisplay);
      expect(dds.at(2).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(2).children()).toHaveProp('stageType', minimalProps.stageTypes[2]);
      expect(dds.at(2).children()).toHaveProp('stageData', minimalProps.violationDetails.stageData.release);
      expect(dds.at(2).children()).toHaveProp('applicationPublicId', 'app1');
    });

    it('hide stage section if isFirewallContext is true', () => {
      const component = getShallowComponent({ isFirewallContext: true });
      expect(component.find('.iq-violation-details__stages')).not.toExist();
    });

    it('show stage section if isFirewallContext is false', () => {
      const component = getShallowComponent({ isFirewallContext: false });
      expect(component.find('.iq-violation-details__stages')).toExist();
    });
  });

  describe('policy owner section', function () {
    it('contains a dt and a dd with a link', function () {
      const component = getShallowComponent().find('.iq-violation-details__policy-owner');

      expect(component.find('dt')).toExist();
      expect(component.find('dd').find(NxTextLink)).toExist();
    });

    it('displays the ownerName within the link', function () {
      expect(getShallowComponent().find('.iq-violation-details__policy-owner').find(NxTextLink)).toHaveText('polOwner');
    });

    describe('when the policy owner is an org', function () {
      it('sets the link href using the management.view.organization state and the org id', function () {
        expect(getShallowComponent().find('.iq-violation-details__policy-owner').find(NxTextLink)).toHaveProp(
          'href',
          '#/foo'
        );
        expect(stateGetMock).toHaveBeenCalledWith('management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });
    });

    describe('when the policy owner is an org and comes from sbomManager', function () {
      it('sets the link href using the sbomManager.management.view.organization state and the org id', function () {
        expect(
          getShallowComponent({ isSbomManager: true }).find('.iq-violation-details__policy-owner').find(NxTextLink)
        ).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith('sbomManager.management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });
    });

    describe('when the policy owner is an app', function () {
      const getComponentWithAppProps = (additionalProps) =>
        getShallowComponent({
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
        expect(getComponentWithAppProps().find('.iq-violation-details__policy-owner').find(NxTextLink)).toHaveProp(
          'href',
          '#/foo'
        );
        expect(stateGetMock).toHaveBeenCalledWith('management.view.application');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          applicationPublicId: 'app2',
        });
      });

      it('sets the link href using the sbomManager.management.view.application state and the app public id', function () {
        expect(
          getComponentWithAppProps({ isSbomManager: true }).find('.iq-violation-details__policy-owner').find(NxTextLink)
        ).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith('sbomManager.management.view.application');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          applicationPublicId: 'app2',
        });
      });
    });

    describe('when policyOwner prop has null ownerId', function () {
      it('renders "Policy no longer exists" message', function () {
        const component = getShallowComponent(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        );
        expect(component.find('.iq-violation-details__policy-owner dd')).toHaveText('Policy no longer exists');
      });
    });
  });

  describe('policy violations constraint info', function () {
    it('renders policy violation constraint info component with the right props', function () {
      const policyViolationConstraintInfo = getShallowComponent().find(PolicyViolationConstraintInfo);

      expect(policyViolationConstraintInfo).toExist();
      expect(policyViolationConstraintInfo).toHaveProp('isFirewallContext', minimalProps.isFirewallContext);
      expect(policyViolationConstraintInfo).toHaveProp('constraintViolations', minimalProps.constraintViolations);
    });
  });
});
