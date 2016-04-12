describe('policy.violations.controller', function() {
  var scope,
      policyViolationsSpy;

  beforeEach(module('cip.policy.violations', function($provide) {
    var component = {
      componentIdentifier: {
        groupId: 'tomcat',
        artifactId: 'catalina',
        version: '5.0.28',
        extension: 'jar'
      },
      pathname: '/foo/bar.jar',
      hash: 'abcd'
    };
    $provide.value('SelectedComponent', {
      get: function() {
        return component;
      },
      set: angular.noop
    });

    $provide.value('OwnerContext', {
      ownerId: 'repository-id',
      ownerType: 'repository'
    });
  }));

  beforeEach(inject(function($controller, $rootScope) {
    scope = $rootScope.$new();
    policyViolationsSpy = jasmine.createSpy('violationsresponse');

    $controller('PolicyViolationsController', {
      $scope: scope,
      PolicyViolations: {
        get: function() {
          return {
            then: policyViolationsSpy
          }
        }
      }
    });
    scope.$digest();
  }));

  it('error', inject(function(PolicyViolations) {
    expect(policyViolationsSpy).toHaveBeenCalled();

    policyViolationsSpy.calls[0].args[1].call(null, {status: 404, data: 'failure'})

    expect(scope.error).toEqual('failure');
  }));
});
