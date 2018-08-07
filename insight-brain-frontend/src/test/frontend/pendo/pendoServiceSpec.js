describe('pendoService', function() {
  var $httpBackend,
      CLMLocations;

  beforeEach(module('pendoModule'), module(function($provide) {
    var doc = {
      createElement: function() {
        return {};
      },
      getElementsByTagName: function() {
        return {
          parentNode: {
            insertBefore: function() {}
          }
        };
      }
    };
    $provide.value('$document', [doc]);
  }));
  beforeEach(inject(function(_$httpBackend_, _CLMLocations_, $window) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;

    $window.pendo = jasmine.createSpyObj('pendo', ['initialize']);
  }));

  afterEach(inject(function($window) {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();

    delete $window.pendo;
  }));

  it('initializes pendo when start is called', inject(function(pendoService, $window) {
    $httpBackend.expectGET(CLMLocations.getUserTelemetryConfig()).respond({ visitors: {}, account: {} });

    pendoService.start();

    $httpBackend.flush();

    expect($window.pendo.initialize).toHaveBeenCalledWith({
      account: {},
      visitors: {},
      disablePersistence: true,
      excludeAllText: true,
      excludeTitle: true,
      guides: {
        disabled: true
      },
      contentHost: CLMLocations.getUserTelemetryProxy(),
      dataHost: CLMLocations.getUserTelemetryProxy(),
      sanitizeUrl: jasmine.any(Function)
    });
  }));
});
