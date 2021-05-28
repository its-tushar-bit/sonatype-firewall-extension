/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global CLM */
describe('CIP Policy Waiver tests', function () {
  var _scope, _viewScope;

  beforeEach(angular.mock.module('cip.policy.violations', 'TestComponentProvider', 'ui.router'));

  beforeEach(
    angular.mock.module('PermissionServiceModule', function ($provide) {
      $provide.service('PermissionService', function () {
        return {
          isAuthorized: function () {
            return true;
          },
        };
      });
    })
  );

  beforeEach(function () {
    window.CLM = {
      path: '../brain/',
    };
  });

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  afterEach(function () {
    _scope.$destroy();
  });

  describe('PolicyViolationsController', function () {
    // setup our http backend to return what we want
    beforeEach(inject(function ($rootScope) {
      _scope = $rootScope.$new();
    }));

    describe('Current JSON data', function () {
      // setup our http backend to return what we want
      beforeEach(inject(function ($rootScope, $controller, $httpBackend) {
        $httpBackend.expectGET(SpecUtil.toRegExp('policythreats.json')).respond({
          version: 1,
          aaData: [
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'bsh',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'bsh',
                  version: '1.3.0',
                },
              },
              hash: '3102cdd0edd5a05afe00',
              policyId: 'policyId',
              policyName: 'name',
              threatLevel: 5,
              activeViolations: [
                {
                  policyId: 'policyId',
                  policyName: 'name',
                  policyThreatLevel: 5,
                  policyViolationId: 'pv1',
                  actions: [
                    {
                      actionType: '1',
                      actionSummary: 'This is an action',
                    },
                    {
                      actionType: '2',
                      actionSummary: 'This is another action',
                    },
                  ],
                  constraints: [
                    {
                      constraintId: 'c7ad07e00c4948c59651cce82163e50a',
                      constraintName: 'test3',
                      constraintOperator: 'AND',
                      conditions: [
                        {
                          conditionType: 'AgeInDays',
                          conditionSummary: 'Age older than 1825',
                          conditionReason: 'Age was 7 years, 8 months and 17 days',
                        },
                        {
                          conditionType: 'AgeInDays',
                          conditionSummary: 'Age older than 730',
                          conditionReason: 'Age was 7 years, 8 months and 17 days',
                        },
                      ],
                    },
                  ],
                },
              ],
            },
            {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'bsh',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'bsh',
                  version: '1.3.0',
                },
              },
              hash: '3102cdd0edd5a05afe00',
              policyId: 'policyId2',
              policyName: 'name2',
              threatLevel: 7,
              activeViolations: [
                {
                  policyId: 'policyId2',
                  policyName: 'name2',
                  policyThreatLevel: 7,
                  policyViolationId: 'pv2',
                  actions: [
                    {
                      actionType: '1',
                      actionSummary: 'This is an action',
                    },
                    {
                      actionType: '1',
                      actionSummary: 'This is an action',
                    },
                  ],
                  constraints: [
                    {
                      constraintId: 'd7ad07e00c4948c59651cce82163e50a',
                      constraintName: 'test4',
                      constraintOperator: 'AND',
                      conditions: [
                        {
                          conditionType: 'AgeInDays',
                          conditionSummary: 'Age older than 1825',
                          conditionReason: 'Age was 7 years, 8 months and 17 days',
                        },
                        {
                          conditionType: 'AgeInDays',
                          conditionSummary: 'Age older than 730',
                          conditionReason: 'Age was 7 years, 8 months and 17 days',
                        },
                      ],
                    },
                  ],
                },
              ],
            },
          ],
        });

        $controller('PolicyViolationsController', {
          $scope: _scope,
        });

        $httpBackend.flush();
      }));

      it('Validate loaded', function () {
        //policyId2 first as we need to account for sorting
        expect(_scope.processedPolicyAlerts).toEqual([
          {
            id: 'policyId2',
            name: 'name2',
            policyViolationId: 'pv2',
            threatLevel: 7,
            hash: '3102cdd0edd5a05afe00',
            constraints: [
              {
                constraintId: 'd7ad07e00c4948c59651cce82163e50a',
                constraintName: 'test4',
                constraintOperator: 'AND',
                conditions: [
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 1825',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 730',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                ],
              },
            ],
            constraintFactsJson: undefined,
            actions: [
              {
                actionType: '1',
                actionSummary: 'This is an action',
              },
            ],
          },
          {
            id: 'policyId',
            name: 'name',
            policyViolationId: 'pv1',
            threatLevel: 5,
            hash: '3102cdd0edd5a05afe00',
            constraints: [
              {
                constraintId: 'c7ad07e00c4948c59651cce82163e50a',
                constraintName: 'test3',
                constraintOperator: 'AND',
                conditions: [
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 1825',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 730',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                ],
              },
            ],
            constraintFactsJson: undefined,
            actions: [
              {
                actionType: '1',
                actionSummary: 'This is an action',
              },
              {
                actionType: '2',
                actionSummary: 'This is another action',
              },
            ],
          },
        ]);
      });

      it('Open Add Waiver', inject(function (Modal) {
        var modalSpy = spyOn(Modal, 'open');
        _scope.waiveComponent(_scope.processedPolicyAlerts[0]);

        expect(modalSpy).toHaveBeenCalledWith({
          templateUrl: 'add-waiver-modal-tmpl',
          controller: 'AddWaiverController',
          backdrop: 'static',
          keyboard: false,
          resolve: {
            policy: jasmine.any(Function),
          },
        });
      }));

      it('Open View Waiver', inject(function (Modal) {
        var modalSpy = spyOn(Modal, 'open');
        _scope.viewWaivers();

        expect(modalSpy).toHaveBeenCalledWith({
          templateUrl: 'view-waivers-modal-tmpl',
          controller: 'ViewWaiverController',
          backdrop: 'static',
          keyboard: false,
        });
      }));

      it('Open Request Waiver', inject(function (Modal) {
        var modalSpy = spyOn(Modal, 'open');
        _scope.requestWaiver(_scope.processedPolicyAlerts[0]);

        expect(modalSpy).toHaveBeenCalledWith({
          template: jasmine.any(String),
          controller: 'RequestWaiverController',
          backdrop: 'static',
          keyboard: false,
          resolve: {
            policy: jasmine.any(Function),
          },
        });
      }));
    });

    describe('Legacy JSON data', function () {
      // setup our http backend to return what we want
      beforeEach(inject(function ($rootScope, $controller, $httpBackend) {
        $httpBackend.expectGET(SpecUtil.toRegExp('policythreats.json')).respond({});
        $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/policy/actionType')).respond([
          {
            id: '1',
            summary: 'test',
          },
        ]);
        $httpBackend.expectGET(SpecUtil.toRegExp('policyalerts.json')).respond({
          aaData: [
            {
              trigger: {
                policyId: 'policyId',
                policyName: 'name',
                policyViolationId: 'pv1',
                threatLevel: 5,
                componentFacts: [
                  {
                    groupId: 'bsh',
                    artifactId: 'bsh',
                    version: '1.3.0',
                    hash: '3102cdd0edd5a05afe00',
                    constraintFacts: [
                      {
                        constraintId: 'c7ad07e00c4948c59651cce82163e50a',
                        constraintName: 'test3',
                        operatorName: 'AND',
                        conditionFacts: [
                          {
                            conditionTypeId: 'AgeInDays',
                            summary: 'Age older than 1825',
                            reason: 'Age was 7 years, 8 months and 17 days',
                          },
                          {
                            conditionTypeId: 'AgeInDays',
                            summary: 'Age older than 730',
                            reason: 'Age was 7 years, 8 months and 17 days',
                          },
                        ],
                      },
                    ],
                  },
                ],
              },
              actions: [
                {
                  actionTypeId: '1',
                },
              ],
            },
          ],
        });

        $controller('PolicyViolationsController', {
          $scope: _scope,
          PolicyViolationData: {
            hash: '3102cdd0edd5a05afe00',
            appId: 'bom1-12345678',
          },
        });

        $httpBackend.flush();
      }));

      it('Validate loaded', function () {
        expect(_scope.processedPolicyAlerts).toEqual([
          {
            id: 'policyId',
            name: 'name',
            policyViolationId: 'pv1',
            threatLevel: 5,
            groupId: 'bsh',
            artifactId: 'bsh',
            version: '1.3.0',
            hash: '3102cdd0edd5a05afe00',
            constraints: [
              {
                constraintId: 'c7ad07e00c4948c59651cce82163e50a',
                constraintName: 'test3',
                constraintOperator: 'AND',
                conditions: [
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 1825',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                  {
                    conditionType: 'AgeInDays',
                    conditionSummary: 'Age older than 730',
                    conditionReason: 'Age was 7 years, 8 months and 17 days',
                  },
                ],
              },
            ],
            actions: [
              {
                actionSummary: 'test',
              },
            ],
          },
        ]);
      });
    });
  });

  describe('AddWaiverController', function () {
    // setup our http backend to return what we want
    beforeEach(inject(function ($rootScope, $controller, $httpBackend) {
      _scope = $rootScope.$new();
      _scope.$close = angular.noop;
      _scope.$dismiss = angular.noop;

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp('../brain/rest/policyWaiver/application/bom1-12345678/applicable/context/policyId')
        )
        .respond({
          id: 'orgId',
          name: 'org',
          type: 'organization',
          children: [
            {
              id: 'bom1-12345678',
              name: 'app',
              type: 'application',
              children: null,
            },
          ],
        });

      $controller('AddWaiverController', {
        $scope: _scope,
        PolicyViolationData: {
          hash: '3102cdd0edd5a05afe00',
          appId: 'bom1-12345678',
        },
        policy: {
          id: 'policyId',
          constraintFactsJson: 'constraint-facts-json',
        },
      });
      $httpBackend.flush();
    }));

    it('Test waive policy at org level', inject(function ($httpBackend) {
      var modalSpy = spyOn(_scope, '$close');

      _scope.$apply(function () {
        _scope.waiver.ownerId = 'orgId';
        _scope.waiver.comment = 'this is my comment!';
        _scope.owner.type = 'organization';
      });

      $httpBackend
        .expectPOST(SpecUtil.toRegExp('../brain/rest/policyWaiver/organization/orgId'), {
          hash: '3102cdd0edd5a05afe00',
          ownerId: 'orgId',
          policyId: 'policyId',
          comment: 'this is my comment!',
          constraintFactsJson: 'constraint-facts-json',
        })
        .respond({});

      expect(_scope.legacyReport).toBe(false);

      _scope.acceptWaiveComponent();

      $httpBackend.flush();
      expect(modalSpy).toHaveBeenCalled();
    }));

    it('Test waive policy at app level', inject(function ($httpBackend) {
      var modalSpy = spyOn(_scope, '$close');

      _scope.$apply(function () {
        _scope.waiver.ownerId = 'bom1-12345678';
        _scope.waiver.comment = 'this is my comment!';
        _scope.owner.type = 'application';
      });

      $httpBackend
        .expectPOST(SpecUtil.toRegExp('../brain/rest/policyWaiver/application/bom1-12345678'), {
          hash: '3102cdd0edd5a05afe00',
          ownerId: 'bom1-12345678',
          policyId: 'policyId',
          comment: 'this is my comment!',
          constraintFactsJson: 'constraint-facts-json',
        })
        .respond({});

      expect(_scope.legacyReport).toBe(false);

      _scope.acceptWaiveComponent();

      $httpBackend.flush();
      expect(modalSpy).toHaveBeenCalled();
    }));
  });

  describe('Legacy AddWaiverController', function () {
    // setup our http backend to return what we want
    beforeEach(inject(function ($rootScope, $controller, $httpBackend) {
      _scope = $rootScope.$new();
      _scope.$close = angular.noop;
      _scope.$dismiss = angular.noop;

      const expectedUrl = '../brain/rest/policyWaiver/application/bom1-12345678/applicable/context/policyId';
      $httpBackend.expectGET(SpecUtil.toRegExp(expectedUrl)).respond({
        id: 'orgId',
        name: 'org',
        type: 'organization',
        children: [
          {
            id: 'bom1-12345678',
            name: 'app',
            type: 'application',
            children: null,
          },
        ],
      });

      $controller('AddWaiverController', {
        $scope: _scope,
        PolicyViolationData: {
          hash: '3102cdd0edd5a05afe00',
          appId: 'bom1-12345678',
        },
        policy: {
          id: 'policyId',
        },
      });
      $httpBackend.flush();
    }));

    it('Test waive policy without constraintFactsJson present', function () {
      _scope.$apply(function () {
        _scope.waiver.ownerId = 'orgId';
        _scope.waiver.comment = 'this is my comment!';
        _scope.owner.type = 'organization';
      });

      expect(_scope.legacyReport).toBe(true);
    });
  });

  describe('ViewWaiverController', function () {
    // setup our http backend to return what we want
    beforeEach(inject(function ($rootScope, $controller) {
      _viewScope = $rootScope.$new();
      $controller('ViewWaiverController', {
        $scope: _viewScope,
        global: {},
      });
    }));

    it('Validate data in scope', inject(function ($httpBackend) {
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/bom1-12345678/component/3102cdd0edd5a05afe00')
        )
        .respond({
          waiversByOwner: [
            {
              ownerId: 'bom1-12345678',
              ownerName: 'ownerName',
              ownerType: 'application',
              waivers: [
                {
                  id: 'id',
                  hash: '1234',
                  policyId: 'policyId',
                  policyName: 'policyName',
                  constraintId: null,
                  ownerId: 'bom1-12345678',
                  comment: 'some comment',
                  createTime: 1375366539817,
                },
              ],
            },
          ],
        });
      $httpBackend.flush();

      expect(_viewScope.waivers.length).toEqual(1);
      expect(_viewScope.waivers[0].id).toEqual('id');
      expect(_viewScope.waivers[0].hash).toEqual('1234');
      expect(_viewScope.waivers[0].policyId).toEqual('policyId');
      expect(_viewScope.waivers[0].policyName).toEqual('policyName');
      expect(_viewScope.waivers[0].constraintId).toEqual(null);
      expect(_viewScope.waivers[0].ownerId).toEqual('bom1-12345678');
      expect(_viewScope.waivers[0].comment).toEqual('some comment');
      expect(_viewScope.waivers[0].createTime).toEqual(1375366539817);
    }));

    it('Validate delete waiver', inject(function ($httpBackend) {
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/bom1-12345678/component/3102cdd0edd5a05afe00')
        )
        .respond({
          waiversByOwner: [
            {
              ownerId: 'bom1-12345678',
              ownerName: 'ownerName',
              ownerType: 'application',
              waivers: [
                {
                  id: 'id',
                  hash: '1234',
                  policyId: 'policyId',
                  policyName: 'policyName',
                  constraintId: null,
                  ownerId: 'bom1-12345678',
                  comment: 'some comment',
                  createTime: 1375366539817,
                },
              ],
            },
          ],
        });
      $httpBackend.flush();

      _viewScope.remove(_viewScope.waivers[0]);
      expect(_viewScope.confirmDelete).toEqual(_viewScope.waivers[0]);

      $httpBackend
        .expectDELETE(SpecUtil.toRegExp(CLM.path + 'api/v2/policyWaivers/application/bom1-12345678/id'))
        .respond(200);
      _viewScope.removeWaiver();
      $httpBackend.flush();
      expect(_viewScope.confirmDelete).toEqual(null);
      expect(_viewScope.waivers.length).toEqual(0);
    }));
  });

  describe('RequestWaiverController', function () {
    beforeEach(inject(function ($rootScope, $controller) {
      _scope = $rootScope.$new();
      _scope.$close = angular.noop;
      _scope.$dismiss = angular.noop;

      $controller('RequestWaiverController', {
        $scope: _scope,
        policy: {
          id: '123',
          name: 'License-Banned',
          threatLevel: 10,
          policyViolationId: 'v1',
          hash: '4abc6',
          actions: [],
          constraints: [
            {
              constraintId: 'c1',
              constraintName: 'License not approved in any situation',
              constraintOperator: 'OR',
              conditions: [
                {
                  conditionReason: 'Condition Reason #1',
                  conditionSummary: 'Condition Summary',
                  conditionTriggerReference: null,
                  conditionType: 'Condition Type',
                },
                {
                  conditionReason: 'Condition Reason #2',
                  conditionSummary: 'Condition Summary',
                  conditionTriggerReference: null,
                  conditionType: 'Condition Type',
                },
              ],
            },
          ],
          constraintFactsJson: 'constraint-facts-json',
        },
      });
    }));

    it('Validates data in scope', function () {
      expect(_scope.policy.id).toEqual('123');
      expect(_scope.policy.policyViolationId).toEqual('v1');
      expect(_scope.policy.name).toEqual('License-Banned');
      expect(_scope.policy.threatLevel).toEqual(10);
      expect(_scope.conditionReasons).toEqual(['Condition Reason #1', 'Condition Reason #2']);
      expect(_scope.getThreatColor(_scope.policy.threatLevel)).toEqual('red');
    });
  });
});
