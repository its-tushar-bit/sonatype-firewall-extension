describe('webhook.list.controller.spec.js', function() {

  beforeEach(module('webhook.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      $httpBackend,
      CLMLocations;

  beforeEach(inject(function($controller, _$httpBackend_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;

    vm = $controller('webhook.list.controller', {
      isAuthorized: true
    });
  }));

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
