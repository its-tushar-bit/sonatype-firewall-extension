describe('CLMLocation.js', function() {
  beforeEach(module('CLMLocation'), function($provide){
    $provide.value('$window', {
    });
  });

  it('Test forceSuccess added to license upload', inject(function (CLMLocations, $window) {
    delete $window.FormData;
    expect(CLMLocations.getLicenseUploadUrl()).toMatch(/.*forceSuccess=true/);
    $window.FormData = 'mock';
    expect(CLMLocations.getLicenseUploadUrl()).not.toMatch(/.*forceSuccess=true/);
  }));
});
