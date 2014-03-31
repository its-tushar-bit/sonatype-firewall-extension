var clmEndpoint, clmEndpointTemplate;

clmEndpoint = clmEndpointTemplate = {
  openView : angular.noop,
  type : 'ide'
};
(function () {
  'use strict';

  describe('CIP Tests', function () {
    beforeEach(module('CIP'));

    afterEach(function () {
      clmEndpoint = angular.copy(clmEndpointTemplate);
      document.cookie = 'clmAppId=; expires=Thu, 01-Jan-70 00:00:01 GMT;';
    });


    describe('GAV', function () {
      it('setGAV', inject(function (GAV) {
        GAV.set({
          id : 'setGAV'
        });
        expect(GAV.get()).toEqual({ id : 'setGAV' });
        expect(GAV.getSelected()).toEqual({ id : 'setGAV' });
      }));

      it('Selected', inject(function (GAV) {
        var gav = {
          groupId : 'gid',
          artifactId : 'aid',
          version : '1',
          hash : '12345678901234567890',
          matchState : 'similar'
        }, sel = {
          groupId : 'gid',
          artifactId : 'aid',
          version : '2'
        };
        GAV.set(gav);
        GAV.setSelected(sel);
        expect(GAV.get()).toEqual(gav);
        expect(GAV.getSelected()).toEqual(sel);
        GAV.setSelected(angular.extend({}, sel, { version : gav.version }));
        expect(GAV.getSelected()).toEqual(gav);
      }));

      it('Insight.setGAV', inject(function (GAV) {
        spyOn(GAV, 'set').andCallThrough();
        Insight.setGav({
          id : 'setGAV2'
        });

        expect(GAV.set).toHaveBeenCalledWith({ id : 'setGAV2' });
        expect(GAV.get()).toEqual({ id : 'setGAV2' });
      }));

      it('Insight.clearGAV', inject(function (GAV) {
        GAV.set({
          id : 'clearGAV'
        });
        spyOn(GAV, 'set').andCallThrough();

        Insight.clearGav();

        expect(GAV.set).toHaveBeenCalledWith(null);
        expect(GAV.get()).toEqual(null);
        expect(GAV.getSelected()).toEqual(null);
      }));
    });

    describe('State', function () {
      describe('Insight.setError', function () {
        it('Invalid AppID', inject(function (State) {
          Insight.setError({
            errorCode : 404
          });
          expect(State.get()).toEqual('invalid-appid');
          expect(State.getArgs()).toEqual({
            errorCode : 404
          });
        }));
        it('Invalid Credentials', inject(function (State) {
          Insight.setError({
            errorCode : 401
          });
          expect(State.get()).toEqual('invalid-credentials');
          expect(State.getArgs()).toEqual({
            errorCode : 401
          });
        }));
        it('Failure', inject(function (State) {
          Insight.setError({
            errorCode : 444
          });
          expect(State.get()).toEqual('failure');
          expect(State.getArgs()).toEqual({
            errorCode : 444
          });
        }));
      });

      it('Insight.setFiltered', inject(function (State) {
        Insight.setFiltered('foo');
        expect(State.get()).toEqual('filtered');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setPending', inject(function (State) {
        Insight.setPending('foo');
        expect(State.get()).toEqual('pending');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setUnassigned', inject(function (State) {
        Insight.setUnassigned('foo');
        expect(State.get()).toEqual('unassigned');
        expect(State.getArgs()).toEqual('foo');
      }));
    });

    describe('SelectedApp', function () {
      describe('IDE Mode', function () {
        it ('Retrieves from GAV', inject(function (SelectedApp, GAV) {
          GAV.set({
            appId : 'foo'
          });
          expect(SelectedApp.get()).toEqual('foo');
        }));

        it ('Doesn\'t Persist', inject(function (SelectedApp, GAV) {
          SelectedApp.set('foo');
          expect(SelectedApp.get()).toBeFalsy();
        }));

        it ('Doesn\'t Use Cookie', inject(function (SelectedApp) {
          document.cookie = 'clmAppId=bar';
          expect(SelectedApp.get()).toBeFalsy();
        }));
      });

      describe('RM Mode', function () {
        var oldVal = null;
        beforeEach(function () {
          oldVal = clmEndpoint.selectApplication;
          clmEndpoint.selectApplication = true;
        });
        afterEach(function () {
          clmEndpoint.selectApplication = oldVal;
        });
        it ('Loads from cookie', inject(function (SelectedApp) {
          document.cookie = 'clmAppId=bar';
          expect(SelectedApp.get()).toEqual('bar');
        }));

        it ('Saves to cookie', inject(function (SelectedApp) {
          SelectedApp.set('save');
          expect(document.cookie).toEqual('clmAppId=save');
        }));
      });
    });

    function createApplicationsTests(type) {
      return function () {
        describe('Applications', function () {
          beforeEach(function () {
            clmEndpoint.type = type;
          });

          it('Success', inject(function (Applications, $httpBackend, $rootScope) {
            var applications = null;

            Applications.get().then(function (data) {
              applications = data;
            });

            $httpBackend.expectGET().respond({
              'myAppId' : 'My First App'
            });
            $httpBackend.flush();
            $rootScope.$apply();
            expect(applications).toEqual({ 'myAppId' : 'My First App' });
          }));

          it('Error', inject(function (Applications, $httpBackend, $rootScope) {
            var error = null;

            Applications.get().then(angular.noop, function (data) {
              error = data;
            });

            $httpBackend.expectGET().respond(404, 'fail');
            $httpBackend.flush();
            $rootScope.$apply();
            expect(error).toEqual(['fail', 404, jasmine.any(Function), jasmine.any(Object)]);
          }));
        });
      };
    }
    describe('IDE', createApplicationsTests('ide'));
    describe('Nexus', createApplicationsTests('nexus'));

    describe('ComponentController', function () {
      var scope = null;

      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('ComponentController', {
          $scope : scope
        });
      }));

      it('Http Requests', inject(function ($httpBackend) {
        var gav = {
          groupId : 'foo',
          artifactId : 'bar',
          version : '1',
          proprietary : true
        };
        clmEndpoint.selectApplication = true;

        Insight.setGav(gav);

        $httpBackend.verifyNoOutstandingRequest();

        Insight.clearGav();
        scope.$apply(function () {
          document.cookie = 'clmAppId=myFirstApp';
        });
        $httpBackend.verifyNoOutstandingRequest();

        spyOn(Brain[clmEndpoint.type], 'getComponentDetailsListUrl').andReturn('foo');
        $httpBackend.expectGET(Brain[clmEndpoint.type].getComponentDetailsListUrl(angular.extend({ appId : 'myFirstApp' }, gav))).respond({ list: [ {} ] });
        Insight.setGav(gav);
        $httpBackend.flush();
        expect(scope.componentDetailsList).not.toBeNull();
        expect(scope.componentDetailsList.length).toEqual(1);
        expect(scope.componentDetailsList[0].proprietary).toEqual(true);
      }));
    });

    describe('DetailsController', function () {
      var scope = null;

      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('DetailsController', {
          $scope : scope
        });
      }));

      afterEach(function () {
        scope.$destroy();
      });

      it('Http Requests', inject(function ($httpBackend, GAV) {
        var gav = {
          groupId : 'foo',
          artifactId : 'bar',
          version : '1',
          proprietary : true
        };
        clmEndpoint.selectApplication = true;

        Insight.setGav(gav);

        $httpBackend.verifyNoOutstandingRequest();

        Insight.clearGav();
        scope.$apply(function () {
          document.cookie = 'clmAppId=myFirstApp';
        });
        $httpBackend.verifyNoOutstandingRequest();

        spyOn(Brain[clmEndpoint.type], 'getArtifactInfoUrl').andReturn('foo');
        $httpBackend.expectGET(Brain[clmEndpoint.type].getArtifactInfoUrl(angular.extend({ appId : 'myFirstApp' }, gav))).respond({ securityVulnerabilities : [], policyAlerts: [] });
        Insight.setGav(angular.extend({ matchState : 'similar' }, gav));
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getArtifactInfoUrl).toHaveBeenCalledWith({
          groupId : 'foo',
          artifactId : 'bar',
          version : '1',
          appId : 'myFirstApp',
          matchState : 'similar',
          proprietary : true
        });

        // Another version selected
        $httpBackend.expectGET(Brain[clmEndpoint.type].getArtifactInfoUrl(angular.extend({}, gav, {
          appId: 'myFirstApp',
          version: '2'
        }))).respond({
          securityVulnerabilities: [],
          policyAlerts: []
        });
        scope.$apply(function () {
          GAV.setSelected(angular.extend({}, gav, { version : '2' }));
        });
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getArtifactInfoUrl).toHaveBeenCalledWith({
          groupId : 'foo',
          artifactId : 'bar',
          version : '2',
          appId : 'myFirstApp',
          proprietary : true
        });

        // Unknown GAV
        scope.$apply(function () {
          GAV.setSelected({ matchState : 'unknown' });
        });
        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('isManual', inject(function ($httpBackend) {
        expect(scope.isManual()).toBeFalsy();

        scope.componentDetails = {
          identificationSource : 'Manual'
        };
        expect(scope.isManual()).toBeTruthy();

        scope.componentDetails = {
          identificationSource : 'Sonatype'
        };
        expect(scope.isManual()).toBeFalsy();
      }));

      it('canMigrate', inject(function (GAV) {
        var gav = {
              groupId : 'foo',
              artifactId : 'bar',
              version : '1'
            },
            selected = angular.copy(gav);

        expect(scope.canMigrate()).toBeFalsy();

        spyOn(GAV, 'get').andReturn(gav);
        spyOn(GAV, 'getSelected').andReturn(selected);

        expect(scope.canMigrate()).toBeFalsy();

        selected.version = '2';
        expect(scope.canMigrate()).toBeTruthy();
      }));

      it('getMaximumSeverity', inject(function($httpBackend, GAV) {
        scope.componentDetails = {
          securityVulnerabilities : []
        };
        expect(scope.getMaximumSeverity()).toEqual('NA');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : null }]
        };
        expect(scope.getMaximumSeverity()).toEqual('Unscored');

        var gav = {
           groupId : 'groupId',
           artifactId : 'artifactId',
           version : 'version',
           appId : 'appId'
        };

        spyOn(Brain[clmEndpoint.type], 'getArtifactInfoUrl').andReturn('foo');
        $httpBackend.expectGET(Brain[clmEndpoint.type].getArtifactInfoUrl(gav)).respond({
          securityVulnerabilities : [{ severity : null }, { severity : 2 }, { severity : 9 }, { severity : 8 }],
          policyAlerts: []
        });

        scope.$apply(function () {
          GAV.set(gav);
        });
        $httpBackend.flush();

        expect(scope.getMaximumSeverity()).toEqual(9);
      }));

      it('getColorClass', inject(function($httpBackend, GAV) {
        scope.componentDetails = {
          securityVulnerabilities : []
        };
        expect(scope.getColorClass()).toEqual(' unspecified');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : null }]
        };
        expect(scope.getColorClass()).toEqual(' moderate');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 0 }]
        };
        expect(scope.getColorClass()).toEqual(' moderate');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 4 }]
        };
        expect(scope.getColorClass()).toEqual(' severe');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 8 }]
        };
        expect(scope.getColorClass()).toEqual(' critical');
      }));

      it('calculates highestPolicyThreat', inject(function($httpBackend, GAV) {
        var gav = {
          groupId : 'groupId',
          artifactId : 'artifactId',
          version : 'version',
          appId : 'appId'
        };
        expect(scope.highestPolicyThreat).toEqual(null);

        spyOn(Brain[clmEndpoint.type], 'getArtifactInfoUrl').andReturn('foo');
        $httpBackend.expectGET(Brain[clmEndpoint.type].getArtifactInfoUrl(gav)).respond({
          securityVulnerabilities : [],
          policyAlerts: [{
            trigger: {
              policyName: 'foo',
              threatLevel: 1
            }
           }, {
            trigger: {
              policyName: 'bar',
              threatLevel: 10
            }
          }]
        });

        scope.$apply(function () {
          GAV.set(gav);
        });
        $httpBackend.flush();

        expect(scope.highestPolicyThreat.level).toEqual(10);
        expect(scope.highestPolicyThreat.violatedPolicies).toEqual(2);
      }));
    });

    describe('graph', function () {
      var scope = null,
          parentScope = null;

      beforeEach(inject(function ($compile, $rootScope, GAV) {
        spyOn(Insight, 'ComponentInformation').andReturn(undefined);

        parentScope = $rootScope.$new();
        parentScope.componentDetails = [{
          "version": "sources",
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.0.4",
          "popularity": 4,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.0.6",
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.1.9",
          "popularity": 13,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.1.31",
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.1.34",
          "popularity": 0,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "4.1.36",
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "version": "5.0.16",
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.0.18",
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.0.28",
          "popularity": 67,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.4",
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.7-alpha",
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.7",
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.8-alpha",
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.9-alpha",
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.9",
          "popularity": 8,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.12",
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.15",
          "popularity": 14,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["LIBERAL"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "version": "5.5.23",
          "popularity": 100,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["LIBERAL"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"],
          "website": "http://tomcat.apache.org/"
        }];

        GAV.set({
          version : '5.0.24'
        });

        $compile('<div graph="componentDetails"></div>')(parentScope);
        parentScope.$apply();
      }));

      afterEach(function () {
        parentScope.$destroy();
      });

      it('Version Click', inject(function (GAV) {
         Insight.ComponentInformation.mostRecentCall.args[0].versionClick('5.5.23');
         expect(GAV.getSelected()).toEqual({
           "version": "5.5.23",
           "popularity": 100,
           "majorRevisionStep": false,
           "observedLicenseThreats": ["LIBERAL"],
           "declaredLicenseThreats": ["LIBERAL"],
           "effectiveLicenseThreat": "LIBERAL",
           "securityThreats": ["Moderate", "Severe"],
           "website": "http://tomcat.apache.org/"
         });
      }));

      it('Double Version Click', inject(function ($rootScope) {
        var version = null;
        parentScope.$on('viewDetails', function (event, v) {
          version = v;
        });
        Insight.ComponentInformation.mostRecentCall.args[0].versionDblClick('5.5.23');

        expect(version).toEqual('5.5.23');
      }));
    });
  });
}());