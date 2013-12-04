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
  
  it('Validate that a GET request has timestamp param', inject(function($q, $http, $httpBackend, $rootScope) {
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/test')).respond(200);
    
    $http.get('/rest/test');
    
    $httpBackend.flush();
  }));
  
  it('Validate that a POST/PUT/DELETE request has no timestamp param', inject(function($q, $http, $httpBackend, $rootScope) {
    $httpBackend.expectPOST('/rest/test').respond(200);
    $httpBackend.expectPUT('/rest/test').respond(200);
    $httpBackend.expectDELETE('/rest/test').respond(200);
    
    $http.post('/rest/test');
    $http.put('/rest/test');
    $http['delete']('/rest/test');
    
    $httpBackend.flush();
  }));
});
