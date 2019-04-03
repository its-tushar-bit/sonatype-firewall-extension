import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import RepositoriesResourceMockData from '../mock.data/repositories.resource.mock.data';

describe('repositories.configuration.tile.controller.spec.js', function() {
  var vm,
      $httpBackend,
      CLMLocations,
      deleteServiceResourceDefer,
      mockDeleteService;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($q, $controller, _$httpBackend_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;

    deleteServiceResourceDefer = $q.defer();
    mockDeleteService = {
      deleteCustom: function() {
        return deleteServiceResourceDefer.promise;
      }
    };

    vm = $controller('repositories.configuration.tile.controller', {
      DeleteModalService: mockDeleteService
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loading Repositories', function() {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(
        RepositoriesResourceMockData.getRepositoriesUrl());
    $httpBackend.flush();

    expect(vm.repositories).toEqual(RepositoriesResourceMockData.getRepositoriesUrl().repositories);
    expect(vm.error).toBeUndefined();
  });

  it('Missing Repositories', function() {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.error).toBeDefined();
  });

  it('Properly Remove a Repository', inject(function($timeout) {
    $httpBackend.expectGET(CLMLocations.getRepositoriesUrl())
        .respond(RepositoriesResourceMockData.getRepositoriesUrl());
    $httpBackend.flush();

    var mockRepositories = RepositoriesResourceMockData.getRepositoriesUrl().repositories;
    expect(vm.repositories).toEqual(mockRepositories);
    vm.removeRepository(mockRepositories[1]);
    deleteServiceResourceDefer.resolve();
    $timeout.flush();

    expect(vm.repositories).toEqual([mockRepositories[0]]);
  }));
});
