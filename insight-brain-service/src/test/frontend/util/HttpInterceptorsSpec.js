describe('HttpInterceptors.js', function() {
  var scope;
  
  beforeEach(module('UnauthenticatedResponseHttpInterceptor', function($provide) {
    $provide.value('$modalInstance', {
      close: function() {}
    });
    $provide.value('$modal', {
      open: function(config) {
        scope.$close = function() {
        };
        inject(function($controller) {
          $controller(config.controller, {
            $scope: scope
          });
        });
        return {
          result: {
            then: function(success, failure) {
              success();
            }
          }
        };
      }
    });
  }));
  
  beforeEach(inject(function($rootScope) {
    scope = $rootScope.$new();
  }));
  
  it('Validate that a failed request is in the queue', inject(function($q, $http, $httpBackend, $rootScope) {
    $httpBackend.expectPOST('test').respond(401);
    
    var success = false;
    var error = false;
    $http.post('test').success(function(){
      success = true;
    }).error(function(){
      error = true;
    });
    
    $httpBackend.flush();
    
    expect(scope.getRequestQueue().length).toEqual(1);
  }));
  
  it('Validate that a GET/POST/PUT/DELETE request has a timestamp param', inject(function($q, $http, $httpBackend, $rootScope) {
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPUT(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectDELETE(SpecUtil.toRegExp('/rest/test')).respond(200);
    
    $http.get('/rest/test');
    $http.post('/rest/test');
    $http.put('/rest/test');
    $http['delete']('/rest/test');
    
    $httpBackend.flush();
  }));
  
  it('Validate that /rest/ and .json paths contains cachebuster, others ignored', inject(function($http, $httpBackend){
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    $httpBackend.expectPOST(SpecUtil.toRegExp('/test/rest/test')).respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp('test.json')).respond(200);
    $httpBackend.expectGET('/unrest/test').respond(200);
    $httpBackend.expectPOST('/test/unrest/test').respond(200);
    $httpBackend.expectGET('test.notjson').respond(200);
    
    $http.get('/rest/test');
    $http.post('/test/rest/test');
    $http.get('test.json');
    $http.get('/unrest/test');
    $http.post('/test/unrest/test');
    $http.get('test.notjson');
    
    $httpBackend.flush();
  }));
});
