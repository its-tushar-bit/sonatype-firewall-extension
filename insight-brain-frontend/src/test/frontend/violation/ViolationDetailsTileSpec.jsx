/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import ViolationDetailsSubtitle from 'MainRoot/violation/ViolationDetailsSubtitle';
import StageDisplay from 'MainRoot/violation/StageDisplay';
import { pathSet } from 'MainRoot/util/jsUtil';
import { NxStatefulSegmentedButton } from '@sonatype/react-shared-components';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';

describe('ViolationDetailsTile', function () {
  let timeAgoMock,
    mockDate,
    dateCreatorMock,
    getOwnerImageUrlMock,
    ViolationDetailsTile,
    stateGetMock,
    stateHrefMock,
    minimalProps,
    stateGoMock,
    goToWaiversMock,
    getShallowComponent;

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
    getOwnerImageUrlMock = jasmine.createSpy('getOwnerImageUrl').and.returnValue('/rest/icon');
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
        displayName: { foo: 'bar' },
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
      activeWaivers: [],
      isPolicyPopoverShown: false,
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
    };

    ViolationDetailsTile = require('inject-loader!../../../main/frontend/violation/ViolationDetailsTile')({
      '../utilAngular/CommonServices': { timeAgo: timeAgoMock },
      '../utilAngular/CLMContextLocation': {
        getOwnerImageUrl: getOwnerImageUrlMock,
      },
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(ViolationDetailsTile, minimalProps);
  });

  it('renders an nx-tile with the iq-violation-details class', function () {
    expect(getShallowComponent()).toMatchSelector('.nx-tile.iq-violation-details');
  });

  describe('header', function () {
    it('renders a ViolationExclamation and "Violation of (policyName)"', function () {
      const header = getShallowComponent().find('.nx-tile-header .nx-tile-header__title h2'),
        exclamation = header.find(ViolationExclamation);

      expect(exclamation).toExist();
      expect(header).toHaveText('<ViolationExclamation />Violation of pol');
    });

    it('appends "Policy no longer exists" to the title when policyOwner prop has null ownerId', function () {
      const component = getShallowComponent(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        ),
        header = component.find('.nx-tile-header .nx-tile-header__title'),
        texts = header.find('span');

      expect(texts.at(0)).toHaveText('Violation of pol');
      expect(texts.at(1)).toHaveText('Policy no longer exists');
    });

    it('sets the correct threatLevelCategory on the ViolationExclamation', function () {
      expect(getShallowComponent().find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'critical');
      expect(
        getShallowComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 7,
          },
        }).find(ViolationExclamation)
      ).toHaveProp('threatLevelCategory', 'severe');
      expect(
        getShallowComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 3,
          },
        }).find(ViolationExclamation)
      ).toHaveProp('threatLevelCategory', 'moderate');
      expect(
        getShallowComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 1,
          },
        }).find(ViolationExclamation)
      ).toHaveProp('threatLevelCategory', 'low');
      expect(
        getShallowComponent({
          violationDetails: {
            ...minimalProps.violationDetails,
            threatLevel: 0,
          },
        }).find(ViolationExclamation)
      ).toHaveProp('threatLevelCategory', 'none');
      expect(
        getShallowComponent(pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)).find(
          ViolationExclamation
        )
      ).toHaveProp('threatLevelCategory', 'disabled');
    });

    it('renders the policy name in an <em>', function () {
      expect(getShallowComponent().find('.nx-tile-header__title h2 em')).toHaveText('pol');
    });

    it('renders a ViolationDetailsSubtitle', function () {
      const subtitle = getShallowComponent().find('.nx-tile-header').find(ViolationDetailsSubtitle);

      expect(subtitle).toExist();
      expect(subtitle).toHaveProp('organizationName', minimalProps.violationDetails.organizationName);
      expect(subtitle).toHaveProp('applicationName', minimalProps.violationDetails.applicationName);
      expect(subtitle).toHaveProp('displayName', minimalProps.violationDetails.displayName);
      expect(subtitle).toHaveProp('filenames', minimalProps.violationDetails.filenames);
    });

    it('renders an nx-tile__actions section with a Manage Waivers button', function () {
      const actions = getShallowComponent().find('.nx-tile__actions'),
        manageWaiversButton = actions.find(NxStatefulSegmentedButton);

      expect(actions).toExist();
      expect(manageWaiversButton).toExist();
      expect(manageWaiversButton).toHaveProp('buttonContent', 'Manage Waivers');

      manageWaiversButton.simulate('click');
      expect(stateGoMock).toHaveBeenCalledWith('listWaivers', {
        violationId: 'selectedViolationId',
        type: 'violation',
        sidebarReference: 'filter',
      });
      expect(goToWaiversMock).toHaveBeenCalledTimes(0);
    });

    it('renders an nx-tile__actions section with a Manage Waivers button calling the goToWaivers func', function () {
      const actions = getShallowComponent({
          isFromPolicyViolations: true,
        }).find('.nx-tile__actions'),
        manageWaiversButton = actions.find(NxStatefulSegmentedButton);

      expect(actions).toExist();
      expect(manageWaiversButton).toExist();
      expect(manageWaiversButton).toHaveProp('buttonContent', 'Manage Waivers');

      manageWaiversButton.simulate('click');
      expect(goToWaiversMock).toHaveBeenCalledWith('selectedViolationId');
      expect(stateGoMock).toHaveBeenCalledTimes(0);
    });

    it('renders a Manage Waivers segmented button with Add Waiver and Request Waiver menu buttons', function () {
      const manageWaiversButton = getShallowComponent().find(NxStatefulSegmentedButton),
        addWaiverButton = manageWaiversButton.children().at(0),
        requestWaiverButton = manageWaiversButton.children().at(1);

      expect(manageWaiversButton).toExist();
      expect(addWaiverButton).toExist();
      expect(requestWaiverButton).toExist();
      expect(addWaiverButton.text()).toContain('Add Waiver');
      expect(requestWaiverButton.text()).toContain('Request Waiver');
    });

    it('renders an Add Waiver menu button that navigates to the Add Waiver page when clicked', function () {
      const manageWaiversButton = getShallowComponent().find(NxStatefulSegmentedButton),
        addWaiverButton = manageWaiversButton.children().at(0);

      expect(addWaiverButton).toExist();

      addWaiverButton.simulate('click');
      expect(stateGoMock).toHaveBeenCalledWith('addWaiver', {
        violationId: 'selectedViolationId',
      });
    });

    it('renders an Add Waiver menu button that navigates to the Request Waiver page when clicked', function () {
      const manageWaiversButton = getShallowComponent().find(NxStatefulSegmentedButton),
        requestWaiverButton = manageWaiversButton.children().at(1);

      expect(requestWaiverButton).toExist();

      requestWaiverButton.simulate('click');
      expect(stateGoMock).toHaveBeenCalledWith('requestWaiver', {
        violationId: 'selectedViolationId',
      });
    });

    it('show header subtitle if isPolicyPopoverShown not active', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: false });
      expect(component.find(ViolationDetailsSubtitle)).toExist();
    });

    it('hide header subtitle if isPolicyPopoverShown active', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: true });
      expect(component.find(ViolationDetailsSubtitle)).not.toExist();
    });

    describe('active waivers counter', function () {
      it('renders as inactive when there are no active waivers for the violation', function () {
        const componentWithZeroWaivers = getShallowComponent(),
          waiverIndicator = componentWithZeroWaivers.find(ActiveWaiversIndicator);

        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('activeWaiverCount', 0);
        expect(waiverIndicator).toHaveProp('waived', true);
      });

      it('renders as active and singular when there is only one active waiver for the violation', function () {
        const componentWithOneWaiver = getShallowComponent({
            activeWaivers: ['an active waiver'],
            isFromPolicyViolations: true,
          }),
          waiverIndicator = componentWithOneWaiver.find(ActiveWaiversIndicator);

        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('activeWaiverCount', 1);
        expect(waiverIndicator).toHaveProp('waived', true);
        expect(waiverIndicator).toHaveProp('showUnapplied', true);
      });

      it('renders as active and unapplied when waived is false and showUnapplied is set', function () {
        const componentWithOneWaiver = getShallowComponent({
            activeWaivers: ['an active waiver'],

            violationDetails: { ...minimalProps.violationDetails, waived: false },
          }),
          waiverIndicator = componentWithOneWaiver.find(ActiveWaiversIndicator);

        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('activeWaiverCount', 1);
        expect(waiverIndicator).toHaveProp('waived', false);
      });

      it('renders as active and plural when there is more than one active waiver for the violation', function () {
        const componentWithSeveralWaiver = getShallowComponent({
            activeWaivers: ['an active waiver', 'and an another one', 'and another one bites the dust!'],
          }),
          waiverIndicator = componentWithSeveralWaiver.find(ActiveWaiversIndicator);
        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('activeWaiverCount', 3);
        expect(waiverIndicator).toHaveProp('waived', true);
      });

      it('does not render nx-tile__actions section when policyOwner prop has null ownerId', function () {
        const component = getShallowComponent(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        );
        expect(component.find('.nx-tile__actions')).not.toExist();
      });

      it('does not render active waiver indicator when policyOwner prop has null ownerId', function () {
        const component = getShallowComponent(
          pathSet(['violationDetails', 'policyOwner', 'ownerId'], null, minimalProps)
        );
        expect(component.find(ActiveWaiversIndicator)).not.toExist();
      });
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

    it('hidden first report section if isPolicyPopoverShown is true', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: true });
      expect(component.find('.iq-violation-details__first-reported')).not.toExist();
    });

    it('show first report section if isPolicyPopoverShown is false', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: false });
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

    it('hide stage section if isPolicyPopoverShown is true', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: true });
      expect(component.find('.iq-violation-details__stages')).not.toExist();
    });

    it('show stage section if isPolicyPopoverShown is false', () => {
      const component = getShallowComponent({ isPolicyPopoverShown: false });
      expect(component.find('.iq-violation-details__stages')).toExist();
    });
  });

  describe('policy owner section', function () {
    it('contains a dt and a dd with the owner icon and link', function () {
      const component = getShallowComponent().find('.iq-violation-details__policy-owner');

      expect(component.find('dt')).toExist();
      expect(component.find('dd img')).toExist();
      expect(component.find('dd a')).toExist();
    });

    it('sets the iq-violation-details__policy-owner-icon class on the icon', function () {
      expect(getShallowComponent().find('.iq-violation-details__policy-owner img')).toHaveClassName(
        'iq-violation-details__policy-owner-icon'
      );
    });

    it('displays the ownerName within the link', function () {
      expect(getShallowComponent().find('.iq-violation-details__policy-owner a')).toHaveText('polOwner');
    });

    describe('when the policy owner is an org', function () {
      it('sets the link href using the management.view.organization state and the org id', function () {
        expect(getShallowComponent().find('.iq-violation-details__policy-owner a')).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith('management.view.organization');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });

      it('sets the icon URL using CLMContextLocation.getOwnerImageUrl', function () {
        expect(getShallowComponent().find('.iq-violation-details__policy-owner img')).toHaveProp('src', '/rest/icon');

        expect(getOwnerImageUrlMock).toHaveBeenCalledWith({
          publicId: undefined,
          id: '1234',
        });
      });
    });

    describe('when the policy owner is an app', function () {
      const getComponentWithAppProps = () =>
        getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner'],
            {
              ownerName: 'polOwner',
              ownerType: 'application',
              ownerId: '1234',
              ownerPublicId: 'app2',
            },
            minimalProps
          )
        );

      it('sets the link href using the management.view.application state and the app public id', function () {
        expect(getComponentWithAppProps().find('.iq-violation-details__policy-owner a')).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith('management.view.application');
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          applicationPublicId: 'app2',
        });
      });

      it('sets the icon URL using CLMContextLocation.getOwnerImageUrl', function () {
        expect(getComponentWithAppProps().find('.iq-violation-details__policy-owner img')).toHaveProp(
          'src',
          '/rest/icon'
        );

        expect(getOwnerImageUrlMock).toHaveBeenCalledWith({
          publicId: 'app2',
          id: '1234',
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
});
