describe('gettingStartedDocLink', function() {
  var vm, telemetryServiceMock;

  beforeEach(module('gettingStartedModule', function($provide) {
    telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);

    $provide.service('gettingStartedUsageTelemetryService', function() {
      return telemetryServiceMock;
    });
  }));

  beforeEach(inject(function($componentController) {
    vm = $componentController('gettingStartedDocLink', {
      gettingStartedUsageTelemetryService: telemetryServiceMock
    }, {
      href: 'testLinkHref'
    });
  }));

  describe('onClick()', function() {
    it('fires "LINK_CLICKED" telemetry event', function() {
      vm.onClick();
      expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('LINK_CLICKED', {
        href: 'testLinkHref'
      });
    });
  });
});
