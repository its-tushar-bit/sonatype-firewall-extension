import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import TagResourceMockData from '../mock.data/tag.resource.mock.data';

describe('application.category.tile.controller.app.spec.js', function() {
  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  function createTests(type, storeName, owner) {
    var vm,
        scope,
        $httpBackend,
        $rootScope,
        $timeout,
        isApp = type === 'application',
        CLMLocations,
        EventNameConstant,
        mockCLMAppLocations,
        mockApplicationStore = StoreUtils().createMockStore('ApplicationStore');

    beforeEach(inject(function(_$rootScope_, $controller, $injector, _$httpBackend_, _$timeout_, _CLMLocations_) {
      $rootScope = _$rootScope_;
      $httpBackend = _$httpBackend_;
      $timeout = _$timeout_;
      CLMLocations = _CLMLocations_;
      scope = $rootScope.$new();
      EventNameConstant = $injector.get('event.name.constant');

      mockCLMAppLocations = {
        isApplication: function() {
          return isApp;
        },
        getEntityId: function() {
          return isApp ? owner.publicId : owner.id;
        }
      };

      vm = $controller('ApplicationCategoryTileControllerApp', {
        CLMAppLocations: mockCLMAppLocations,
        $scope: scope
      });
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    if (isApp) {
      it('Properly Loading Applied Categories and Application', function() {
        var mockAppliedTags = TagResourceMockData.getApplicationTagUrl();

        expectLoadAndReturnProperData();

        expect(vm.ownerName).toEqual(owner.name);
        expect(vm.appliedCategories.length).toEqual(mockAppliedTags.length);
        expect(vm.areAnyCategoriesDefined).toBeFalsy();
        vm.appliedCategories.forEach(function(category, index) {
          expect(category).toEqual(mockAppliedTags[index]);
        });
      });

      it('Missing App Info', function() {
        mockApplicationStore.resolveGet([{}, {}]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.error).toEqual('Could not find an application with ID ' + owner.publicId + '.');
      });

      it('Missing Categories', function() {
        mockApplicationStore.resolveGet([owner]);
        $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(400,
            'Bad Request');
        $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond(
            []);
        $timeout.flush();
        $httpBackend.flush();

        expect(vm.error).toBeDefined();
      });

      it('Reloads on broadcasted owner summary reload event', function() {
        expectLoadAndReturnProperData();

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

        expectLoadAndReturnProperData();
      });

      it('Updates Owner name on broadcasted updated owner event', function() {
        expectLoadAndReturnProperData();

        expect(vm.ownerName).not.toEqual('Bob');

        $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, {name: 'Bob'});

        expect(vm.ownerName).toEqual('Bob');
      });
    }

    function expectLoadAndReturnProperData() {
      mockApplicationStore.resolveGet([owner]);
      $httpBackend.expectGET(CLMLocations.getApplicationTagUrl(mockCLMAppLocations.getEntityId())).respond(TagResourceMockData.getApplicationTagUrl());
      $httpBackend.expectGET(CLMLocations.getApplicableOrganizationTags(mockCLMAppLocations.getEntityId())).respond([]);
      $timeout.flush();
      $httpBackend.flush();
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
