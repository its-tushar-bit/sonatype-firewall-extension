describe('repository.policy.violations.service', function () {
  var service;

  beforeEach(module('component.information.panel', function($provide) {
    var component = {
      componentIdentifier: {
        groupId: 'tomcat',
        artifactId: 'catalina',
        version: '5.0.28',
        extension: 'jar'
      },
      pathname: 'foo/1.0/bar.jar',
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

  it('success', inject(function ($httpBackend, PolicyViolations, SelectedComponent) {
    var successSpy = jasmine.createSpy('success');

    $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/repositories/repository-id/report/policyThreat/'
            + SelectedComponent.get().pathname)).respond(200, {activePolicyViolations: []});

    PolicyViolations.get().then(successSpy, angular.noop);
    $httpBackend.flush();
    expect(successSpy).toHaveBeenCalledWith([]);
  }));

  it('error', inject(function ($httpBackend, PolicyViolations, SelectedComponent) {
    var failureSpy = jasmine.createSpy('failure');

    $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/repositories/repository-id/report/policyThreat/'
            + SelectedComponent.get().pathname)).respond(404, 'failure');

    PolicyViolations.get().then(angular.noop, failureSpy);
    $httpBackend.flush();
    expect(failureSpy).toHaveBeenCalled();
  }));
});
