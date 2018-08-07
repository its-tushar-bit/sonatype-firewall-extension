describe('CLMLocation.js', function() {
  var CLMLocations,
      $window;

  beforeEach(module('CLMLocation', function($provide) {
    $provide.value('$window', {
    });
  }));

  beforeEach(inject(function(_CLMLocations_, _$window_) {
    CLMLocations = _CLMLocations_;
    $window = _$window_;
  }));

  it('Test noFormData added to license upload', function() {
    var formData = $window.FormData || 'mock';
    $window.FormData = null;
    expect(CLMLocations.getLicenseUploadUrl()).toMatch(/.*noFormData=true/);
    $window.FormData = formData;
    expect(CLMLocations.getLicenseUploadUrl()).not.toMatch(/.*noFormData=true/);
  });

  // map of user-telemetry method names and their respective unique postfixes
  var userTelemetryLocations = {
    getUserTelemetryConfig: 'config',
    getUserTelemetryJavascript: 'javascript',
    getUserTelemetryProxy: 'events'
  };

  for (var methodName in userTelemetryLocations) {
    var postfix = userTelemetryLocations[methodName];

    describe(methodName, function() {
      beforeEach(inject(function(BaseUrl) {
        spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
      }));

      it('returns the expected path', function() {
        expect(CLMLocations[methodName]()).toBe('http://localhost/rest/user-telemetry/' + postfix);
      });

      it('returns the expected rm path when clmEndpoint.type is "rm"', function() {
        $window.clmEndpoint = { type: 'rm' };

        expect(CLMLocations[methodName]()).toBe('http://localhost/rest/rm/user-telemetry/' + postfix);
      });
    });
  }
});
