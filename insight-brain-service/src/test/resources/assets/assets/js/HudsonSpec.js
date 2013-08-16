describe('Hudson module tests', function() {
  beforeEach(module('Hudson'));
  
  beforeEach(inject(function($httpBackend){
    $httpBackend.expectGET(new RegExp('/../../../crumbIssuer/api/xml.*')).respond(null);
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Validate post not altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/sonatype-clm-report/');
    $httpBackend.expectGET(new RegExp('/../../../crumbIssuer/api/xml.*')).respond(null);
    $httpBackend.expectPOST('../brain/someurl').respond({});
    hudson.post('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate post altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/path/');
    $httpBackend.expectPOST('../../../../../someurl').respond({});
    hudson.post('../brain/someurl');
    $httpBackend.flush();
  }));
});