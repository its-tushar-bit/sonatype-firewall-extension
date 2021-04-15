/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from '../../../../main/frontend/configuration/gettingStarted/module';

describe('gettingStartedDocLink', function () {
  var vm, telemetryServiceMock;

  beforeEach(
    angular.mock.module(gettingStartedModule.name, function ($provide) {
      telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);

      $provide.service('gettingStartedUsageTelemetryService', function () {
        return telemetryServiceMock;
      });
    })
  );

  beforeEach(inject(function ($componentController) {
    vm = $componentController(
      'gettingStartedDocLink',
      {
        gettingStartedUsageTelemetryService: telemetryServiceMock,
      },
      {
        href: 'testLinkHref',
      }
    );
  }));

  describe('onClick()', function () {
    it('fires "LINK_CLICKED" telemetry event', function () {
      vm.onClick();
      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('LINK_CLICKED', {
        href: 'testLinkHref',
      });
    });
  });
});
