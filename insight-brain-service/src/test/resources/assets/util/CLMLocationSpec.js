describe('CLMLocation.js', function() {
  beforeEach(module('CLMLocation'), function($provide) {
    $provide.value('$window', {
    });
  });

  it('Test forceSuccess added to license upload', inject(function(CLMLocations, $window) {
    var formData = $window.FormData;
    $window.FormData = null;
    expect(CLMLocations.getLicenseUploadUrl()).toMatch(/.*forceSuccess=true/);
    $window.FormData = formData;
    expect(CLMLocations.getLicenseUploadUrl()).not.toMatch(/.*forceSuccess=true/);
  }));
});
