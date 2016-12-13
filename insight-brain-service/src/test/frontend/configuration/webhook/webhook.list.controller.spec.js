describe('webhook.list.controller.spec.js', function() {

  beforeEach(module('webhook.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      $httpBackend,
      $rootScope,
      CLMLocations;

  beforeEach(inject(function(_$rootScope_, $controller, _$httpBackend_, _CLMLocations_) {
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
        CLMLocations = _CLMLocations_;
        scope = $rootScope.$new();

        vm = $controller('webhook.list.controller', {
          isAuthorized: true
        });
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Properly Loads Webhooks', function() {
    $httpBackend.expectGET(CLMLocations.getWebhooksUrl()).respond(WebhookMockData.getWebhooks());
    $httpBackend.flush();

    expect(vm.webhooks.count).toEqual(WebhookMockData.getWebhooks().count);
    expect(vm.webhooks[0].id).toEqual(WebhookMockData.getWebhooks()[0].id);
  });

  it('Missing Webhooks', function() {
    $httpBackend.expectGET(CLMLocations.getWebhooksUrl()).respond(400, 'Bad Request');
    $httpBackend.flush();

    expect(vm.loadError).toBeDefined();
  });
});
