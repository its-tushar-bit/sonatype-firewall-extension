describe('CLMLocation.js', function() {
  beforeEach(module('CLMLocation'), function($provide) {
    $provide.value('$window', {
    });
  });

  it('Test noFormData added to license upload', inject(function(CLMLocations, $window) {
    var formData = $window.FormData || 'mock';
    $window.FormData = null;
    expect(CLMLocations.getLicenseUploadUrl()).toMatch(/.*noFormData=true/);
    $window.FormData = formData;
    expect(CLMLocations.getLicenseUploadUrl()).not.toMatch(/.*noFormData=true/);
  }));
});
