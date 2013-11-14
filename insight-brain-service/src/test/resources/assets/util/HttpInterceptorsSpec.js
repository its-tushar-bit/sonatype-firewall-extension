describe('HttpInterceptors.js', function() {
  beforeEach(module('HttpInterceptors'), function($provide) {
    /*$provide.value('$modal', {
      open: function(){}
    });*/
  });

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
    
    expect($rootScope.requestQueue.length).toEqual(1);
  }));
});
