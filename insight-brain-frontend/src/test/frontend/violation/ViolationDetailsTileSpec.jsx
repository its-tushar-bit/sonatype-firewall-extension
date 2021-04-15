/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import ViolationDetailsSubtitle from '../../../main/frontend/violation/ViolationDetailsSubtitle';
import StageDisplay from '../../../main/frontend/violation/StageDisplay';
import { pathSet } from '../../../main/frontend/util/jsUtil';
import { NxButton } from '@sonatype/react-shared-components';
import ActiveWaiversIndicator from '../../../main/frontend/violation/ActiveWaiversIndicator';

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

    getOwnerImageUrlMock = jasmine
      .createSpy('getOwnerImageUrl')
      .and.returnValue('/rest/icon');
    stateGetMock = jasmine.createSpy('$state.get').and.returnValue('theState');
    stateHrefMock = jasmine.createSpy('$state.href').and.returnValue('#/foo');
    minimalProps = {
      $state: {
        get: stateGetMock,
        href: stateHrefMock,
        params: {
          id: 'selectedViolationId',
          type: 'violation',
          sidebarReference: 'filter',
        },
      },
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
      },
      stageTypes: [
        { stageTypeId: 'build', shortName: 'Build' },
        { stageTypeId: 'stage-release', shortName: 'Stage' },
        { stageTypeId: 'release', shortName: 'Release' },
      ],
      applicationPublicId: 'app1',
      stateGo: stateGoMock,
      activeWaivers: [],
    };

    ViolationDetailsTile = require('inject-loader!../../../main/frontend/violation/ViolationDetailsTile')(
      {
        '../util/CommonServices': { timeAgo: timeAgoMock },
        '../util/CLMContextLocation': {
          getOwnerImageUrl: getOwnerImageUrlMock,
        },
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(
      ViolationDetailsTile,
      minimalProps
    );
  });

  it('renders an nx-tile with the iq-violation-details class', function () {
    expect(getShallowComponent()).toMatchSelector(
      '.nx-tile.iq-violation-details'
    );
  });

  describe('header', function () {
    it('renders a ViolationExclamation and "Violation of (policyName)"', function () {
      const header = getShallowComponent().find(
          '.nx-tile-header .nx-tile-header__title h2'
        ),
        exclamation = header.find(ViolationExclamation);

      expect(exclamation).toExist();
      expect(header).toHaveText('<ViolationExclamation />Violation of pol');
    });

    it('appends "Policy no longer exists" to the title when policyOwner prop has null ownerId', function () {
      const component = getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner', 'ownerId'],
            null,
            minimalProps
          )
        ),
        header = component.find('.nx-tile-header .nx-tile-header__title'),
        texts = header.find('span');

      expect(texts.at(0)).toHaveText('Violation of pol');
      expect(texts.at(1)).toHaveText('Policy no longer exists');
    });

    it('sets the correct threatLevelCategory on the ViolationExclamation', function () {
      expect(getShallowComponent().find(ViolationExclamation)).toHaveProp(
        'threatLevelCategory',
        'critical'
      );
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
        getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner', 'ownerId'],
            null,
            minimalProps
          )
        ).find(ViolationExclamation)
      ).toHaveProp('threatLevelCategory', 'disabled');
    });

    it('renders the policy name in an <em>', function () {
      expect(
        getShallowComponent().find('.nx-tile-header__title h2 em')
      ).toHaveText('pol');
    });

    it('renders a ViolationDetailsSubtitle', function () {
      const subtitle = getShallowComponent()
        .find('.nx-tile-header')
        .find(ViolationDetailsSubtitle);

      expect(subtitle).toExist();
      expect(subtitle).toHaveProp(
        'organizationName',
        minimalProps.violationDetails.organizationName
      );
      expect(subtitle).toHaveProp(
        'applicationName',
        minimalProps.violationDetails.applicationName
      );
      expect(subtitle).toHaveProp(
        'displayName',
        minimalProps.violationDetails.displayName
      );
      expect(subtitle).toHaveProp(
        'filenames',
        minimalProps.violationDetails.filenames
      );
    });

    it('renders an nx-tile__actions section with an action button', function () {
      const actions = getShallowComponent().find('.nx-tile__actions'),
        button = actions.find(NxButton);

      expect(actions).toExist();
      expect(button).toExist();
      expect(button.text()).toContain('Manage Waivers');

      button.simulate('click');
      expect(stateGoMock).toHaveBeenCalledWith('listWaivers', {
        violationId: 'selectedViolationId',
        type: 'violation',
        sidebarReference: 'filter',
      });
    });

    describe('active waivers counter', function () {
      it('renders as inactive when there are no active waivers for the violation', function () {
        const componentWithZeroWaivers = getShallowComponent(),
          waiverIndicator = componentWithZeroWaivers.find(
            ActiveWaiversIndicator
          );

        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('noOfWaivers', 0);
        expect(waiverIndicator.html()).toContain('0');
        expect(waiverIndicator.html()).toContain('Active Waivers');
        expect(waiverIndicator.html()).toContain(
          'iq-waiver-indicator--inactive'
        );
      });

      it('renders as active and singular when there is only one active waiver for the violation', function () {
        const componentWithOneWaiver = getShallowComponent({
            activeWaivers: ['an active waiver'],
          }),
          waiverIndicator = componentWithOneWaiver.find(ActiveWaiversIndicator);

        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('noOfWaivers', 1);
        expect(waiverIndicator.html()).toContain('1');
        expect(waiverIndicator.html()).toContain('Active Waiver');
        expect(waiverIndicator.html()).not.toContain(
          'iq-waiver-indicator--inactive'
        );
      });

      it('renders as active and plural when there is more than one active waiver for the violation', function () {
        const componentWithSeveralWaiver = getShallowComponent({
            activeWaivers: [
              'an active waiver',
              'and an another one',
              'and another one bites the dust!',
            ],
          }),
          waiverIndicator = componentWithSeveralWaiver.find(
            ActiveWaiversIndicator
          );
        expect(waiverIndicator).toExist();
        expect(waiverIndicator).toHaveProp('noOfWaivers', 3);
        expect(waiverIndicator.html()).toContain('3');
        expect(waiverIndicator.html()).toContain('Active Waivers');
        expect(waiverIndicator.html()).not.toContain(
          'iq-waiver-indicator--inactive'
        );
      });

      it('does not render nx-tile__actions section when policyOwner prop has null ownerId', function () {
        const component = getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner', 'ownerId'],
            null,
            minimalProps
          )
        );
        expect(component.find('.nx-tile__actions')).not.toExist();
      });

      it('does not render active waiver indicator when policyOwner prop has null ownerId', function () {
        const component = getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner', 'ownerId'],
            null,
            minimalProps
          )
        );
        expect(component.find(ActiveWaiversIndicator)).not.toExist();
      });
    });
  });

  describe('left content', function () {
    it('is a dl', function () {
      expect(
        getShallowComponent().find('.iq-violation-details__left-details')
      ).toMatchSelector('dl');
    });
  });

  describe('right content', function () {
    it('is a dl', function () {
      expect(
        getShallowComponent().find('.iq-violation-details__right-details')
      ).toMatchSelector('dl');
    });
  });

  describe('threat level section', function () {
    it('contains a dt and a dd with the threat level', function () {
      const component = getShallowComponent().find(
        '.iq-violation-details__threat-level'
      );

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
      const component = getShallowComponent().find(
        '.iq-violation-details__policy-type'
      );

      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('Security');
    });
  });

  describe('first reported section', function () {
    it('contains a dt and a dd with the first reported time displayed in relative terms', function () {
      const component = getShallowComponent().find(
        '.iq-violation-details__first-reported'
      );

      expect(dateCreatorMock).toHaveBeenCalledWith('2020-03-02T16:53:33.263Z');
      expect(timeAgoMock).toHaveBeenCalledWith(mockDate);
      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('1 weeks ago');
    });
  });

  describe('first reported section', function () {
    it('contains a dt and a dd with the highest mostRecentEvaluationTime time displayed in relative terms', function () {
      const component = getShallowComponent().find(
        '.iq-violation-details__last-reported'
      );

      expect(dateCreatorMock).toHaveBeenCalledWith('2020-03-04T16:53:33.263Z');
      expect(timeAgoMock).toHaveBeenCalledWith(mockDate);
      expect(component.find('dt')).toExist();
      expect(component.find('dd')).toHaveText('2 days ago');
    });
  });

  describe('stages section', function () {
    it('contains a dt and for each stage in stageTypes a dd containins a StageDisplay', function () {
      const component = getShallowComponent().find(
          '.iq-violation-details__stages'
        ),
        dds = component.find('dd');

      expect(component.find('dt')).toExist();
      expect(dds.length).toBe(3);

      expect(dds.at(0).key()).toBe('build');
      expect(dds.at(0).children()).toMatchSelector(StageDisplay);
      expect(dds.at(0).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(0).children()).toHaveProp(
        'stageType',
        minimalProps.stageTypes[0]
      );
      expect(dds.at(0).children()).toHaveProp(
        'stageData',
        minimalProps.violationDetails.stageData.build
      );
      expect(dds.at(0).children()).toHaveProp('applicationPublicId', 'app1');

      expect(dds.at(1).key()).toBe('stage-release');
      expect(dds.at(1).children()).toMatchSelector(StageDisplay);
      expect(dds.at(1).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(1).children()).toHaveProp(
        'stageType',
        minimalProps.stageTypes[1]
      );
      expect(dds.at(1).children()).toHaveProp(
        'stageData',
        minimalProps.violationDetails.stageData['stage-release']
      );
      expect(dds.at(1).children()).toHaveProp('applicationPublicId', 'app1');

      expect(dds.at(2).key()).toBe('release');
      expect(dds.at(2).children()).toMatchSelector(StageDisplay);
      expect(dds.at(2).children()).toHaveProp('$state', minimalProps.$state);
      expect(dds.at(2).children()).toHaveProp(
        'stageType',
        minimalProps.stageTypes[2]
      );
      expect(dds.at(2).children()).toHaveProp(
        'stageData',
        minimalProps.violationDetails.stageData.release
      );
      expect(dds.at(2).children()).toHaveProp('applicationPublicId', 'app1');
    });
  });

  describe('policy owner section', function () {
    it('contains a dt and a dd with the owner icon and link', function () {
      const component = getShallowComponent().find(
        '.iq-violation-details__policy-owner'
      );

      expect(component.find('dt')).toExist();
      expect(component.find('dd img')).toExist();
      expect(component.find('dd a')).toExist();
    });

    it('sets the iq-violation-details__policy-owner-icon class on the icon', function () {
      expect(
        getShallowComponent().find('.iq-violation-details__policy-owner img')
      ).toHaveClassName('iq-violation-details__policy-owner-icon');
    });

    it('displays the ownerName within the link', function () {
      expect(
        getShallowComponent().find('.iq-violation-details__policy-owner a')
      ).toHaveText('polOwner');
    });

    describe('when the policy owner is an org', function () {
      it('sets the link href using the management.view.organization state and the org id', function () {
        expect(
          getShallowComponent().find('.iq-violation-details__policy-owner a')
        ).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith(
          'management.view.organization'
        );
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          organizationId: '1234',
        });
      });

      it('sets the icon URL using CLMContextLocation.getOwnerImageUrl', function () {
        expect(
          getShallowComponent().find('.iq-violation-details__policy-owner img')
        ).toHaveProp('src', '/rest/icon');

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
        expect(
          getComponentWithAppProps().find(
            '.iq-violation-details__policy-owner a'
          )
        ).toHaveProp('href', '#/foo');
        expect(stateGetMock).toHaveBeenCalledWith(
          'management.view.application'
        );
        expect(stateHrefMock).toHaveBeenCalledWith('theState', {
          applicationPublicId: 'app2',
        });
      });

      it('sets the icon URL using CLMContextLocation.getOwnerImageUrl', function () {
        expect(
          getComponentWithAppProps().find(
            '.iq-violation-details__policy-owner img'
          )
        ).toHaveProp('src', '/rest/icon');

        expect(getOwnerImageUrlMock).toHaveBeenCalledWith({
          publicId: 'app2',
          id: '1234',
        });
      });
    });

    describe('when policyOwner prop has null ownerId', function () {
      it('renders "Policy no longer exists" message', function () {
        const component = getShallowComponent(
          pathSet(
            ['violationDetails', 'policyOwner', 'ownerId'],
            null,
            minimalProps
          )
        );
        expect(
          component.find('.iq-violation-details__policy-owner dd')
        ).toHaveText('Policy no longer exists');
      });
    });
  });
});
