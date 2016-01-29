describe('RepositoryManagerConfigurationController', function() {

  beforeEach(module('repository.manager.module'));

  var vm,
      scope,
      $httpBackend,
      CLMLocations,
      repositories = {
        "repositories": [
          {
            "oldestEvalTimestamp": null,
            "managerInstanceId": "8e697b0e28824da89e4fa4683ef3c79d",
            "repository": {
              "id": "814df83cf2e0405f9c0ac2ccee1ce0f2",
              "repositoryManagerId": "faffa3b770c8469cb5fa641787f5fe62",
              "publicId": "central",
              "enabled": true,
              "quarantineEnabled": false,
              "format": "maven2"
            }
          }, {
            "oldestEvalTimestamp": null,
            "managerInstanceId": "8e697b0e28824da89e4fa4683ef3c79d",
            "repository": {
              "id": "92f285961341420ea435ecb32733c0ef",
              "repositoryManagerId": "faffa3b770c8469cb5fa641787f5fe62",
              "publicId": "apache-snapshots",
              "enabled": true,
              "quarantineEnabled": false,
              "format": "maven2"
            }
          }
        ]
      };

  beforeEach(inject(function($rootScope, $controller, _$httpBackend_, _CLMLocations_) {
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    vm = $controller('repository.manager.configuration.controller', {$scope: scope});
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  it('loads repositories', function() {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(repositories);
    $httpBackend.flush();
    expect(vm.repositories.length).toBe(2);
    expect(vm.repositories[0]).toEqual(repositories.repositories[0]);
    expect(vm.repositories[1]).toEqual(repositories.repositories[1]);
  });

  it('shows error on load failure', function() {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();
    expect(vm.error[0]).toBe('Bad Request');
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(repositories);
    vm.doLoad();
    $httpBackend.flush();
    expect(vm.error).toBe(undefined);
  });

  it('can delete a repository', inject(function(Dialog) {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(repositories);
    $httpBackend.flush();
    expect(vm.repositories.length).toEqual(2);
    spyOn(Dialog, 'open');
    vm.viewRemoveRepository(vm.repositories[0]);
    expect(vm.repositories[0].repository.id).toEqual(repositories.repositories[0].repository.id);
    expect(Dialog.open).toHaveBeenCalledWith({
      title: 'Delete Repository',
      body: 'Are you sure you want to delete the Repository with ID "' + repositories.repositories[0].repository.publicId + '"? This action is not reversible.',
      buttons : [{
        name : 'Cancel',
        type : 'cancel'
      }, {
        name : 'Delete',
        type : 'danger',
        click : jasmine.any(Function)
      }]
    });
    $httpBackend.expectDELETE(CLMLocations.getRepositoryInfoUrl(vm.repositories[0].repository.id)).respond(204);
    Dialog.open.mostRecentCall.args[0].buttons[1].click();
    $httpBackend.flush();
    expect(vm.repositories.length).toEqual(1);
    expect(vm.repositories[0]).toEqual(repositories.repositories[1]);
  }));

  it('can view repository deletion errors', inject(function(Dialog, ErrorDialog) {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(repositories);
    $httpBackend.flush();
    expect(vm.repositories.length).toEqual(2);
    spyOn(Dialog, 'open');
    var errorSpy = spyOn(ErrorDialog, 'open');
    vm.viewRemoveRepository(vm.repositories[0]);
    expect(vm.repositories[0].repository.id).toEqual(repositories.repositories[0].repository.id);
    expect(Dialog.open).toHaveBeenCalledWith({
      title: 'Delete Repository',
      body: 'Are you sure you want to delete the Repository with ID "' + repositories.repositories[0].repository.publicId + '"? This action is not reversible.',
      buttons : [{
        name : 'Cancel',
        type : 'cancel'
      }, {
        name : 'Delete',
        type : 'danger',
        click : jasmine.any(Function)
      }]
    });
    $httpBackend.expectDELETE(CLMLocations.getRepositoryInfoUrl(vm.repositories[0].repository.id)).respond(404, 'Bad Request');
    Dialog.open.mostRecentCall.args[0].buttons[1].click();
    $httpBackend.flush();
    expect(errorSpy).toHaveBeenCalledWith('Bad Request');
    expect(vm.repositories.length).toEqual(2);
    expect(vm.repositories[0]).toEqual(repositories.repositories[0]);
    expect(vm.repositories[1]).toEqual(repositories.repositories[1]);
  }));

});
