import changeDefaultAdminPasswordNoticeModule from '../../../main/frontend/changeDefaultAdminPasswordNotice/module';

describe('changeDefaultAdminPasswordNotice component', function() {
  beforeEach(angular.mock.module(changeDefaultAdminPasswordNoticeModule.name));

  var vm,
      $scope,
      currentUserDeferred,
      shouldDisplayDefaultPasswordWarningDeferred;

  beforeEach(inject(
      function($q, _$httpBackend_, $rootScope, $componentController, defaultAdminPasswordChangedService) {
        $scope = $rootScope.$new();
        currentUserDeferred = $q.defer();
        shouldDisplayDefaultPasswordWarningDeferred = $q.defer();

        spyOn(defaultAdminPasswordChangedService, 'shouldDisplayDefaultPasswordWarning')
            .and.returnValue(shouldDisplayDefaultPasswordWarningDeferred.promise);

        vm = $componentController('changeDefaultAdminPasswordNotice', {
          'CurrentUser': currentUserDeferred.promise,
          $scope: $scope
        });
      }
  ));

  describe('$onInit', function() {

    it('sets shouldDisplayNotice and isDefaultUser flags to true based on supplied data', function() {
      shouldDisplayDefaultPasswordWarningDeferred.resolve(true);
      currentUserDeferred.resolve({username: 'admin'});

      vm.$onInit();

      $scope.$digest();

      expect(vm.shouldDisplayNotice).toBe(true);
      expect(vm.isDefaultUser).toBe(true);
    });

    it('sets shouldDisplayNotice and isDefaultUser flags to false based on supplied data', function() {
      shouldDisplayDefaultPasswordWarningDeferred.resolve(false);
      currentUserDeferred.resolve({username: 'foo'});

      vm.$onInit();

      $scope.$digest();

      expect(vm.shouldDisplayNotice).toBe(false);
      expect(vm.isDefaultUser).toBe(false);
    });
  });
});
