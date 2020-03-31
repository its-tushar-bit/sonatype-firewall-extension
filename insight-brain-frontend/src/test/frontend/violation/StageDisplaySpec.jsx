/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faExclamationCircle, faExclamationTriangle, faSquare } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../enzymeUtils';

describe('StageDisplay', function() {
  let terseAgoMock,
      StageDisplay,
      stateGetMock,
      stateHrefMock,
      minimalProps,
      getShallowComponent;

  beforeEach(function() {
    terseAgoMock = jasmine.createSpy('terseAgo').and.returnValue('5d');
    stateGetMock = jasmine.createSpy('$state.get').and.returnValue('theState');
    stateHrefMock = jasmine.createSpy('$state.href').and.returnValue('#/foo');
    minimalProps = {
      $state: {
        get: stateGetMock,
        href: stateHrefMock
      },
      stageType: { shortName: 'Build' },
      stageData: null,
      applicationPublicId: 'app1'
    };

    StageDisplay = require('inject-loader!../../../main/frontend/violation/StageDisplay')({
      '../util/CommonServices': { terseAgo: terseAgoMock }
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(StageDisplay, minimalProps);
  });

  it('renders a span with the iq-violation-details__stage class', function() {
    expect(getShallowComponent()).toMatchSelector('span.iq-violation-details__stage');
    expect(getShallowComponent({
      stageData: {
        mostRecentEvaluationTime: 0,
        mostRecentScanId: 'scan1',
        actionTypeId: null
      }
    })).toMatchSelector('span.iq-violation-details__stage');
  });

  describe('when stageData is not provided', function() {
    it('adds the iq-violation-details__stage--unused class', function() {
      expect(getShallowComponent()).toHaveClassName('iq-violation-details__stage--unused');
    });

    it('renders a square icon followed by the stage shortName', function() {
      const component = getShallowComponent(),
          icon = component.find(NxFontAwesomeIcon);

      // toHaveText includes the component name for components
      expect(component).toHaveText('<NxFontAwesomeIcon />Build');
      expect(icon).toHaveProp('icon', faSquare);
    });
  });

  describe('when stageData is provided', function() {
    const getComponentWithData = (props, actionTypeId = null) => getShallowComponent({
      ...props,
      stageData: {
        mostRecentEvaluationTime: 50000,
        mostRecentScanId: 'scan1',
        actionTypeId
      }
    });

    it('does not set the iq-violation-details__stage--unused class', function() {
      expect(getComponentWithData()).not.toHaveClassName('iq-violation-details__stage--unused');
    });

    it('renders a link computed via $state showing the stage shortName and how long ago it was', function() {
      const component = getComponentWithData(),
          link = component.find('a');

      expect(stateGetMock).toHaveBeenCalledWith('applicationReport');
      expect(stateHrefMock).toHaveBeenCalledWith('theState', { publicId: 'app1', scanId: 'scan1' });
      expect(terseAgoMock).toHaveBeenCalledWith(50000);

      expect(link).toHaveProp('href', '#/foo');
      expect(link).toHaveText('Build 5d');
    });

    describe('when the actionTypeId is null', function() {
      it('renders no icon', function() {
        expect(getComponentWithData().find(NxFontAwesomeIcon)).not.toExist();
      });
    });

    describe('when the actionTypeId is warn', function() {
      it('renders an exclamation triangle icon', function() {
        expect(getComponentWithData({}, 'warn').find(NxFontAwesomeIcon)).toHaveProp('icon', faExclamationTriangle);
      });

      it('sets the iq-violation-details__stage-action and iq-violation-details__stage-action--warn classes on the icon',
          function() {
            expect(getComponentWithData({}, 'warn').find(NxFontAwesomeIcon))
                .toHaveClassName('iq-violation-details__stage-action');

            expect(getComponentWithData({}, 'warn').find(NxFontAwesomeIcon))
                .toHaveClassName('iq-violation-details__stage-action--warn');
          }
      );
    });

    describe('when the actionTypeId is fail', function() {
      it('renders an exclamation circle icon', function() {
        expect(getComponentWithData({}, 'fail').find(NxFontAwesomeIcon)).toHaveProp('icon', faExclamationCircle);
      });

      it('sets the iq-violation-details__stage-action and iq-violation-details__stage-action--fail classes on the icon',
          function() {
            expect(getComponentWithData({}, 'fail').find(NxFontAwesomeIcon))
                .toHaveClassName('iq-violation-details__stage-action');

            expect(getComponentWithData({}, 'fail').find(NxFontAwesomeIcon))
                .toHaveClassName('iq-violation-details__stage-action--fail');
          }
      );
    });
  });
});
