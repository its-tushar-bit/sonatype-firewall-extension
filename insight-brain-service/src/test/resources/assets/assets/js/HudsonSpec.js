describe('Hudson module tests', function() {
  beforeEach(module('Hudson'));
  
  beforeEach(inject(function($httpBackend){
    $httpBackend.whenGET(new RegExp('/../../../crumbIssuer/api/xml.*')).respond(null);
  }));

  afterEach(inject(function($httpBackend){
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Validate post not altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/sonatype-clm-report/');
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
  
  it('Validate get not altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/sonatype-clm-report/');
    $httpBackend.expectGET('../brain/someurl').respond({});
    hudson.get('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate get altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/path/');
    $httpBackend.expectGET('../../../../../someurl').respond({});
    hudson.get('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate put not altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/sonatype-clm-report/');
    $httpBackend.expectPUT('../brain/someurl').respond({});
    hudson.put('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate put altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/path/');
    $httpBackend.expectPUT('../../../../../someurl').respond({});
    hudson.put('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate delete not altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/sonatype-clm-report/');
    $httpBackend.expectDELETE('../brain/someurl').respond({});
    hudson['delete']('../brain/someurl');
    $httpBackend.flush();
  }));
  
  it('Validate delete altered', inject(function(hudson, $location, $httpBackend) {
    $location.url('/path/');
    $httpBackend.expectDELETE('../../../../../someurl').respond({});
    hudson['delete']('../brain/someurl');
    $httpBackend.flush();
  }));
});