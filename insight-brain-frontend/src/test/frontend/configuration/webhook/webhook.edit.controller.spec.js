import webhookModule from '../../../../main/frontend/configuration/webhook/webhook.module';
import webhookMockData from '../../stores/configuration/webhook/webhook.mock.data';

describe('webhook.edit.controller.spec.js', function() {
  beforeEach(angular.mock.module(webhookModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      scope,
      $httpBackend,
      $rootScope,
      CLMLocations,
      mockDeleteService,
      mockWebhookStore = StoreUtils().createMockStore('WebhookStore');

  beforeEach(inject(function(_$rootScope_, _$q_, $controller, _$httpBackend_, _CLMLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    scope = $rootScope.$new();
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('New Webhook', function() {

    beforeEach(inject(function($controller) {
      vm = $controller('webhook.edit.controller', {
        $scope: scope,
        isAuthorized: true,
        DeleteModalService: mockDeleteService
      });

      $httpBackend.expectGET(CLMLocations.getWebhookEventTypesUrl()).respond(webhookMockData.getWebhookEventTypes());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      $httpBackend.flush();
    }));

    it('Fetches the Webhook Event types', function() {
      expect(vm.webhookEventTypes).toEqual(webhookMockData.getWebhookEventTypes());
    });

    it('Creates a new Webhook from the WebhookStore', function() {
      expect(vm.dirtyWebhook).toBeDefined();
      expect(vm.dirtyWebhook.$new).toBe(true);
    });
  });

  describe('Invalid Webhook ID', function() {

    beforeEach(inject(function($controller) {
      vm = $controller('webhook.edit.controller', {
        $scope: scope,
        isAuthorized: true,
        $stateParams: {
          webhookId: 'invalid'
        },
        DeleteModalService: mockDeleteService
      });

      mockWebhookStore.rejectGetById('invalid');

      $httpBackend.expectGET(CLMLocations.getWebhookEventTypesUrl()).respond(webhookMockData.getWebhookEventTypes());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      $httpBackend.flush();
    }));

    it('Errors if no match found', function() {
      expect(vm.dirtyWebhook).toBeUndefined();
      expect(vm.loadError).toBeDefined();
    });

    it('does not prevent default on pageChangeStarted', function() {
      const evt = $rootScope.$broadcast('pageChangeStarted');

      expect(evt.defaultPrevented).toBe(false);
    });
  });

  describe('Edit Webhook', function() {
    var mockWebhook = ResourceUtils().createMockResource();
    var webhookId = '3ccc32c267474f5d8ef3ab5d6a9aab1d';

    var $timeout;

    beforeEach(inject(function($controller, _$timeout_, _$q_) {
      $timeout = _$timeout_;

      vm = $controller('webhook.edit.controller', {
        $scope: scope,
        isAuthorized: true,
        $stateParams: {
          webhookId: webhookId
        },
        DeleteModalService: mockDeleteService
      });

      mockWebhook.id = webhookId;
      mockWebhookStore.resolveGetById(mockWebhook);

      vm.webhookEditorMask = {wrap: SpecUtil.promiseWrapper(_$q_)};
      $httpBackend.expectGET(CLMLocations.getWebhookEventTypesUrl()).respond(webhookMockData.getWebhookEventTypes());
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      $httpBackend.flush();
      $timeout.flush();
    }));

    it('Fetches the Webhook Event types', function() {
      expect(vm.webhookEventTypes).toEqual(webhookMockData.getWebhookEventTypes());
    });

    it('Creates a dirtyWebhook', function() {
      expect(vm.dirtyWebhook).toBeDefined();
      expect(vm.dirtyWebhook.id).toEqual(webhookId);
    });

    it('Unsuccessful save sets error message', function() {
      vm.dirtyWebhook = mockWebhook;
      vm.saveWebhook();
      mockWebhook.rejectSave('dammit');
      $timeout.flush();
      expect(vm.submitError).toBe('dammit');
    });

    describe('Delete Webhook', function() {
      var deleteServiceResourceDefer,
          state,
          $timeout;

      beforeEach(inject(function(_$q_, $controller, $state, _$timeout_) {
        state = $state;
        $timeout = _$timeout_;
        deleteServiceResourceDefer = _$q_.defer();

        mockDeleteService = {
          deleteResource: function() {
            return deleteServiceResourceDefer.promise;
          }
        };

        vm = $controller('webhook.edit.controller', {
          $scope: scope,
          isAuthorized: true,
          $stateParams: {
            webhookId: webhookId
          },
          $state: state,
          DeleteModalService: mockDeleteService
        });

        vm.dirtyWebhook = mockWebhook;
      }));

      it('Navigates back to list view after delete', function() {
        $httpBackend.expectGET(CLMLocations.getWebhookEventTypesUrl()).respond(webhookMockData.getWebhookEventTypes());
        $httpBackend.flush();

        spyOn(state, 'go');

        vm.deleteWebhook();
        deleteServiceResourceDefer.resolve();
        $timeout.flush();
        // then
        expect(state.go).toHaveBeenCalledWith('webhooks.list');
        expect(mockWebhook.$revert).toHaveBeenCalled();
      });
    });

    describe('Navigation away from page', function() {
      beforeEach(inject(function() {
        vm.dirtyWebhook = mockWebhook;
        vm.dirtyWebhook.isDirty = angular.noop;
      }));

      it('shows no warning when webhook is not dirty', function() {
        spyOn(vm.dirtyWebhook, 'isDirty').and.returnValue(false);

        SpecUtil.expectStateChangeNotPrevented(scope);
        expect(vm.dirtyWebhook.isDirty).toHaveBeenCalled();
      });

      it('shows a warning when webhook is dirty', function() {
        spyOn(vm.dirtyWebhook, 'isDirty').and.returnValue(true);

        SpecUtil.expectStateChangePrevented(scope);
        expect(vm.dirtyWebhook.isDirty).toHaveBeenCalled();
      });
    });
  });
});
