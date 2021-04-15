/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from '../../../../main/frontend/configuration/gettingStarted/module';

describe('gettingStartedUsageTelemetryService', function () {
  var gettingStartedUsageTelemetryService, telemetryServiceMock, $ngReduxMock;

  beforeEach(
    angular.mock.module(gettingStartedModule.name, function ($provide) {
      telemetryServiceMock = jasmine.createSpyObj('telemetryService', [
        'submitData',
      ]);
      $ngReduxMock = jasmine.createSpyObj('$ngRedux', ['getState']);

      $provide.service('telemetryService', function () {
        return telemetryServiceMock;
      });

      $provide.service('$ngRedux', function () {
        return $ngReduxMock;
      });
    })
  );

  beforeEach(inject(function ($injector) {
    gettingStartedUsageTelemetryService = $injector.get(
      'gettingStartedUsageTelemetryService'
    );
  }));

  describe('submitData', function () {
    var reduxState;
    beforeEach(function () {
      reduxState = {
        router: {
          prevState: {
            name: '',
          },
        },
      };
    });

    it('sets pageNavigatedFrom attribute to empty string if prevState is empty', function () {
      $ngReduxMock.getState.and.returnValue(reduxState);

      gettingStartedUsageTelemetryService.submitData('testAction');

      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith(
        'GETTING_STARTED_USAGE',
        {
          action: 'testAction',
          pageNavigatedFrom: '',
        },
        undefined
      );
    });

    it('sets pageNavigatedFrom attribute to "systemMenu" if prevState is not empty', function () {
      reduxState.router.prevState.name = 'somePrevState';
      $ngReduxMock.getState.and.returnValue(reduxState);

      gettingStartedUsageTelemetryService.submitData('testAction');

      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith(
        'GETTING_STARTED_USAGE',
        {
          action: 'testAction',
          pageNavigatedFrom: 'systemMenu',
        },
        undefined
      );
    });

    it('uses provided attributes', function () {
      $ngReduxMock.getState.and.returnValue(reduxState);

      gettingStartedUsageTelemetryService.submitData('testAction', {
        pageNavigatedFrom: 'productlicense',
        foo: 'bar',
      });

      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith(
        'GETTING_STARTED_USAGE',
        {
          action: 'testAction',
          pageNavigatedFrom: 'productlicense',
          foo: 'bar',
        },
        undefined
      );
    });

    it('passes sync flag to telemetryService', function () {
      $ngReduxMock.getState.and.returnValue(reduxState);

      gettingStartedUsageTelemetryService.submitData('testAction', null, true);

      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith(
        'GETTING_STARTED_USAGE',
        {
          action: 'testAction',
          pageNavigatedFrom: '',
        },
        true
      );
    });
  });
});
