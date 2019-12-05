import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('notification.webhook.service.spec', function() {

  var notificationWebhookService;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(inject([
    'notification.webhook.service',
    function(NotificationWebhookService) {
      notificationWebhookService = NotificationWebhookService;
    }
  ]));

  it('gets data properly', inject(function($httpBackend, CLMContextLocations) {
    var webhooks = [
      {
        id: 'webhook1',
        url: 'url1'
      }
    ];
    $httpBackend.expectGET(CLMContextLocations.getNotificationWebhooksUrl()).respond(webhooks);
    notificationWebhookService.get().then(function(results) {
      expect(results).toEqual(webhooks);
    });
  }));
});
