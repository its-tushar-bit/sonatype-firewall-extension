/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipAudit', function() {
  let createController, $httpBackend;

  const backendData = {
    aaData: [{
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-core',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      },
      status: 'Overridden',
      overriddenLicenses: ['Adobe'],
      comment: 'asdf',
      time: 1541714592539,
      user: 'admin',
      ip: '127.0.0.1',
      where: null,
      filename: 'licenses.json'
    }, {
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-core',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      },
      status: 'Open',
      overriddenLicenses: null,
      comment: '',
      time: 1541780519903,
      user: 'fooUser',
      ip: '127.0.0.1',
      where: null,
      filename: 'licenses.json'
    }, {
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-core',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      },
      status: 'Not Applicable',
      comment: 'N/A',
      source: 'sonatype',
      reference: 'sonatype-2017-0355',
      time: 1541779185472,
      ip: '127.0.0.1',
      where: null,
      filename: 'security.json'
    }, {
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-core',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      },
      status: 'Deleted',
      comment: '',
      source: 'sonatype',
      reference: '2017-0355',
      time: 1541780480275,
      user: 'admin',
      ip: '127.0.0.1',
      where: null,
      filename: 'security.json'
    }]
  };

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function($componentController, _$httpBackend_, $rootScope) {
    $httpBackend = _$httpBackend_;
    createController = function(scanId, applicationPublicId, component) {
      return $componentController('cipAudit', { $scope: $rootScope.$new() },
          { scanId, applicationPublicId, component });
    };
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('sets a default sort of "-time"', function() {
    const controller = createController();

    expect(controller.sort).toBe('-time');
  });

  function initControllerWithData() {
    const controller = createController('scanId', 'appId', {
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-core',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      }
    });

    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/report/appId/scanId/auditLog/licenses.json+security.json' +
        '?key=%7B%22hash%22%3A%22d20be6a5ddd6f8cfd36e%22%2C%22componentIdentifier%22%3A%7B%22format%22%3A' +
        '%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22jackson-core%22%2C%22classifier%22%3A%22%22%2C' +
        '%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22com.fasterxml.jackson.core%22%2C%22version%22%3A%22' +
        '2.2.2%22%7D%7D%7D'))
        .respond(backendData);

    controller.$onInit();

    $httpBackend.flush();

    return controller;
  }

  it('fetches, processes, and sorts the audit records from the backend', function() {
    const controller = initControllerWithData();

    expect(controller.error).not.toBeDefined();
    expect(controller.auditRecords).toEqual([{
      user: 'fooUser',
      action: 'Reopened',
      detail: 'License Analysis',
      time: 1541780519903,
      comment: ''
    }, {
      user: 'admin',
      action: 'Deleted',
      detail: 'Vulnerability sonatype-2017-0355',
      time: 1541780480275,
      comment: ''
    }, {
      user: 'anonymous',
      action: 'Ignored',
      detail: 'Vulnerability sonatype-2017-0355',
      time: 1541779185472,
      comment: 'N/A'
    }, {
      user: 'admin',
      action: 'Overrode',
      detail: 'License as Adobe',
      time: 1541714592539,
      comment: 'asdf'
    }]);
  });

  describe('onSortChange', function() {
    it('updates vm.sort and sorts the records', function() {
      const controller = initControllerWithData();

      controller.onSortChange(['user']);

      expect(controller.auditRecords.length).toBe(4);
      expect(controller.auditRecords[2]).toEqual({
        user: 'anonymous',
        action: 'Ignored',
        detail: 'Vulnerability sonatype-2017-0355',
        time: 1541779185472,
        comment: 'N/A'
      });
      expect(controller.auditRecords[3]).toEqual({
        user: 'fooUser',
        action: 'Reopened',
        detail: 'License Analysis',
        time: 1541780519903,
        comment: ''
      });

      // these two records sort the same, so we can't guarantee what order they'll be in, but based on the
      // expectations above they have to be in indexes 0 and 1
      expect(controller.auditRecords).toContain({
        user: 'admin',
        action: 'Deleted',
        detail: 'Vulnerability sonatype-2017-0355',
        time: 1541780480275,
        comment: ''
      });
      expect(controller.auditRecords).toContain({
        user: 'admin',
        action: 'Overrode',
        detail: 'License as Adobe',
        time: 1541714592539,
        comment: 'asdf'
      });

      controller.onSortChange(['-user']);

      expect(controller.auditRecords.length).toBe(4);
      expect(controller.auditRecords[1]).toEqual({
        user: 'anonymous',
        action: 'Ignored',
        detail: 'Vulnerability sonatype-2017-0355',
        time: 1541779185472,
        comment: 'N/A'
      });
      expect(controller.auditRecords[0]).toEqual({
        user: 'fooUser',
        action: 'Reopened',
        detail: 'License Analysis',
        time: 1541780519903,
        comment: ''
      });

      // these two records sort the same, so we can't guarantee what order they'll be in, but based on the
      // expectations above they have to be in indexes 2 and 3
      expect(controller.auditRecords).toContain({
        user: 'admin',
        action: 'Deleted',
        detail: 'Vulnerability sonatype-2017-0355',
        time: 1541780480275,
        comment: ''
      });
      expect(controller.auditRecords).toContain({
        user: 'admin',
        action: 'Overrode',
        detail: 'License as Adobe',
        time: 1541714592539,
        comment: 'asdf'
      });
    });
  });

  it('updates vm.auditRecords in response to changes to vm.applicationPublicId', inject(function($rootScope) {
    const controller = initControllerWithData();

    expect(controller.auditRecords).not.toEqual([]);

    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/report/appId2/scanId/auditLog/licenses.json+security.json' +
        '?key=%7B%22hash%22%3A%22d20be6a5ddd6f8cfd36e%22%2C%22componentIdentifier%22%3A%7B%22format%22%3A' +
        '%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22jackson-core%22%2C%22classifier%22%3A%22%22%2C' +
        '%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22com.fasterxml.jackson.core%22%2C%22version%22%3A%22' +
        '2.2.2%22%7D%7D%7D'))
        .respond(200);

    controller.applicationPublicId = 'appId2';

    $rootScope.$digest();
    expect(controller.auditRecords).not.toBeDefined();

    $httpBackend.flush();
    $rootScope.$digest();

    expect(controller.auditRecords).toEqual([]);
    expect(controller.error).not.toBeDefined();
  }));

  it('updates vm.auditRecords in response to changes to vm.scanId', inject(function($rootScope) {
    const controller = initControllerWithData();

    expect(controller.auditRecords).not.toEqual([]);

    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/report/appId/scanId2/auditLog/licenses.json+security.json' +
        '?key=%7B%22hash%22%3A%22d20be6a5ddd6f8cfd36e%22%2C%22componentIdentifier%22%3A%7B%22format%22%3A' +
        '%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22jackson-core%22%2C%22classifier%22%3A%22%22%2C' +
        '%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22com.fasterxml.jackson.core%22%2C%22version%22%3A%22' +
        '2.2.2%22%7D%7D%7D'))
        .respond(200);

    controller.scanId = 'scanId2';

    $rootScope.$digest();
    expect(controller.auditRecords).not.toBeDefined();

    $httpBackend.flush();
    $rootScope.$digest();

    expect(controller.auditRecords).toEqual([]);
    expect(controller.error).not.toBeDefined();
  }));

  it('updates vm.auditRecords in response to changes to vm.component', inject(function($rootScope) {
    const controller = initControllerWithData();

    expect(controller.auditRecords).not.toEqual([]);

    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/report/appId/scanId/auditLog/licenses.json+security.json' +
        '?key=%7B%22hash%22%3A%22d20be6a5ddd6f8cfd36e%22%2C%22componentIdentifier%22%3A%7B%22format%22%3A' +
        '%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22jackson-additional%22%2C' +
        '%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A' +
        '%22com.fasterxml.jackson.core%22%2C%22version%22%3A%222.2.2%22%7D%7D%7D'))
        .respond(200);

    controller.component = {
      hash: 'd20be6a5ddd6f8cfd36e',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'jackson-additional',
          classifier: '',
          extension: 'jar',
          groupId: 'com.fasterxml.jackson.core',
          version: '2.2.2'
        }
      }
    };

    $rootScope.$digest();
    expect(controller.auditRecords).not.toBeDefined();

    $httpBackend.flush();
    $rootScope.$digest();

    expect(controller.auditRecords).toEqual([]);
    expect(controller.error).not.toBeDefined();
  }));

  it('sets vm.error and empties vm.auditRecords when the backend returns an error', inject(function($rootScope) {
    const controller = initControllerWithData();

    expect(controller.auditRecords).not.toEqual([]);

    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/report/appId/scanId2/auditLog/licenses.json+security.json' +
        '?key=%7B%22hash%22%3A%22d20be6a5ddd6f8cfd36e%22%2C%22componentIdentifier%22%3A%7B%22format%22%3A' +
        '%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22jackson-core%22%2C%22classifier%22%3A%22%22%2C' +
        '%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22com.fasterxml.jackson.core%22%2C%22version%22%3A%22' +
        '2.2.2%22%7D%7D%7D'))
        .respond(500);

    controller.scanId = 'scanId2';

    $httpBackend.flush();
    $rootScope.$digest();

    expect(controller.auditRecords).toEqual([]);
    expect(controller.error).toBeDefined();
  }));
});
