/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import webhookModule from '../../../../main/frontend/configuration/webhook/webhook.module';
import webhookMockData from '../../stores/configuration/webhook/webhook.mock.data';

describe('webhook.list.controller.spec.js', function() {

  beforeEach(angular.mock.module(webhookModule.name, function($provide) {
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
    $httpBackend.expectGET(CLMLocations.getWebhooksUrl()).respond(webhookMockData.getWebhooks());
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
    $httpBackend.flush();

    expect(vm.webhooks.count).toEqual(webhookMockData.getWebhooks().count);
    expect(vm.webhooks[0].id).toEqual(webhookMockData.getWebhooks()[0].id);
  });

  it('Missing Webhooks', function() {
    $httpBackend.expectGET(CLMLocations.getWebhooksUrl()).respond(400, 'Bad Request');
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
    $httpBackend.flush();

    expect(vm.loadError).toBeDefined();
  });
});
