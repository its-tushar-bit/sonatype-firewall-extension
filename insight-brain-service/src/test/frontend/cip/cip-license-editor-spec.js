(function() {
  SpecUtil.setupProviders('app1', 'org1');
  function getAppliedLicenseOverrides(appStatus, appLicense, orgStatus, orgLicense) {
    var overrides = {
      licenseOverridesByOwner: [
        {
          ownerId: 'app1',
          ownerName: 'Application',
          ownerType: 'application',
          licenseOverride: {
            id: 'app1override',
            ownerId: 'app1',
            componentIdentifier : {
              format : 'maven',
              coordinates : {
                artifactId : 'artifactid',
                groupId : 'org.groupid',
                version : '1'
              }
            },
            groupId: 'org.groupid',
            artifactId: 'artifactid',
            version: '1',
            status: appStatus,
            licenseIds: [appLicense],
            comment: ''
          }
        },
        {
          ownerId: 'org1',
          ownerName: 'Organization',
          ownerType: 'organization',
          licenseOverride: {
            id: 'org1override',
            ownerId: 'org1',
            componentIdentifier : {
              format : 'maven',
              coordinates : {
                artifactId : 'artifactid',
                groupId : 'org.groupid',
                version : '1'
              }
            },
            groupId: 'org.groupid',
            artifactId: 'artifactid',
            version: '1',
            status: orgStatus,
            licenseIds: [orgLicense],
            comment: ''
          }
        },
        {
          ownerId: 'root-organization',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          licenseOverride: null
        }
      ]
    };
    if (appStatus === null) {
      overrides.licenseOverridesByOwner[0].licenseOverride = null;
    }
    if (orgStatus === undefined) {
      overrides.licenseOverridesByOwner.pop();
    }
    else if (orgStatus === null) {
      overrides.licenseOverridesByOwner[1].licenseOverride = null;
    }
    return overrides;
  }

  function getLicenseWithThreats(declared, observed, selected) {
    var x = {
      declaredlicenses : [],
      observedlicenses : [],
      selectableLicenses : selected ? selected : [{ licenseId : 'AFL-1.2' }]
    };
    x.declaredlicenses.push({
      threat: 4,
      license: declared ? declared : LicenseGroupMockData.getLicensesData()[0]
    });
    x.observedlicenses.push({
      threat: 9,
      license: observed ? observed : LicenseGroupMockData.getLicensesData()[1]
    });
    return x;
  }

  describe('CIP License Editor', function() {
    beforeEach(module('LicenseEditor', 'ApplicationIdProvider', function($provide) {
      $provide.value('SelectedComponent', {
        componentIdentifier : {
          format : 'maven',
          coordinates : {
            artifactId : 'artifactid',
            groupId : 'org.groupid',
            version : '1'
          }
        },
        groupId: 'org.groupid',
        artifactId: 'artifactid',
        version: '1'
      });
      $provide.value('DataView', {
        updateItem: $.noop
      });
    }));

    var scope;

    beforeEach(inject(function($rootScope) {
      scope = $rootScope.$new();
      InsightDatatable.getLicenseThreatLevelFromArray = $.noop;
      Insight.updateSummary = $.noop;
    }));

    afterEach(function() {
      scope.$destroy();
      delete InsightDatatable.getLicenseThreatLevelFromArray;
      delete Insight.updateSummary;
    });

    describe('App+Org with Overrides', function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides('ACKNOWLEDGED', null, 'OVERRIDDEN', "AFL-1.2"));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('Submit disabled when expected', function() {
        expect(scope.isSubmitEnabled()).toBeFalsy();
        scope.licenseEditorForm = {};
        expect(scope.isSubmitEnabled()).toBeFalsy();
        scope.licenseEditorForm.$invalid = true;
        scope.saving = true;
        scope.licenseEditorForm.$dirty = true;
        expect(scope.isSubmitEnabled()).toBeFalsy();
        scope.saving = false;
        expect(scope.isSubmitEnabled()).toBeFalsy();
        scope.licenseEditorForm.$invalid = false;
        expect(scope.isSubmitEnabled()).toBeTruthy();
      });

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'ACKNOWLEDGED',
          licenseIds: []
        });

        expect(scope.canInherit()).toBeTruthy();
        expect(scope.getInheritableStatus()).toEqual('Overridden');
      });

      it('Delete', inject(function($httpBackend) {
        $httpBackend.expectDELETE(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/app1override')).respond(204);
        scope.$apply(function() {
          scope.override.status = 'DELETE';
          scope.save();
        });
        $httpBackend.flush();

        expect(scope.override).toEqual({
          ownerId: 'org1',
          status: 'OVERRIDDEN',
          licenseIds: ['AFL-1.2']
        });
        expect(scope.canInherit()).toBeTruthy();
        expect(scope.getInheritableStatus()).toEqual('Open');
      }));

      describe('getLicenseThreatClass', function () {
        it('Unspecified', function () {
          expect(scope.getLicenseThreatClass(undefined)).toEqual('unspecified');
          expect(scope.getLicenseThreatClass(null)).toEqual('unspecified');
        });
        it('Critical', function () {
          expect(scope.getLicenseThreatClass(10)).toEqual('critical');
          expect(scope.getLicenseThreatClass(9)).toEqual('critical');
          expect(scope.getLicenseThreatClass(8)).toEqual('critical');
        });
        it('Severe', function () {
          expect(scope.getLicenseThreatClass(7)).toEqual('severe');
          expect(scope.getLicenseThreatClass(6)).toEqual('severe');
          expect(scope.getLicenseThreatClass(5)).toEqual('severe');
          expect(scope.getLicenseThreatClass(4)).toEqual('severe');
        });
        it('Moderate', function () {
          expect(scope.getLicenseThreatClass(3)).toEqual('moderate');
          expect(scope.getLicenseThreatClass(2)).toEqual('moderate');
          expect(scope.getLicenseThreatClass(1)).toEqual('moderate');
        });
        it('None', function () {
          expect(scope.getLicenseThreatClass(0)).toEqual('none');
        });
      });
    });

    describe("No Overrides", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides(null, null, null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OPEN',
          licenseIds: []
        });
        expect(scope.canInherit()).toBeTruthy();
      });

      it('Add Org', inject(function($httpBackend, SelectedComponent) {
        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1')).respond(function(method, url, data,
                                                                                                      headers)
        {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: ''
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });
        scope.$apply(function() {
          scope.override.ownerId = 'org1';
        });
        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });
        scope.save();
        $httpBackend.flush();
      }));

      it('Add App', inject(function($httpBackend, SelectedComponent) {
        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1')).respond(function(method, url, data,
                                                                                                     headers)
        {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'app1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment : ''
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });
        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });
        scope.save();
        $httpBackend.flush();
      }));
    });

    describe("Status Selection", function () {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides(null, null, null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
        
        scope.$apply(function() {
          scope.override.status = 'OVERRIDDEN';
        });
        scope.$apply(function() {
          scope.override.licenseIds = ['AFL-1.2'];
        });
      }));
      it('License IDs Cleared on OPEN', inject(function() {
        scope.$apply(function() {
          scope.override.status = 'OPEN';
        });
        expect(scope.override.status).toBe('OPEN');
        expect(scope.override.licenseIds.length).toBe(0);
      }));
      it('License IDs Cleared on ACKNOWLEDGED', inject(function() {
        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });
        expect(scope.override.status).toBe('ACKNOWLEDGED');
        expect(scope.override.licenseIds.length).toBe(0);
      }));
      it('License IDs Cleared on CONFIRMED', inject(function() {
        scope.$apply(function() {
          scope.override.status = 'CONFIRMED';
        });
        expect(scope.override.status).toBe('CONFIRMED');
        expect(scope.override.licenseIds.length).toBe(0);
      }));
      it('License IDs Cleared on OVERRIDDEN', inject(function() {
        // To trigger watched event we have to use a different default selection.
        scope.$apply(function() {
          scope.override.status = 'SELECTED';
        });
        scope.$apply(function() {
          scope.override.licenseIds = ['AFL-1.2'];
        });
        scope.$apply(function() {
          scope.override.status = 'OVERRIDDEN';
        });
        expect(scope.override.status).toBe('OVERRIDDEN');
        expect(scope.override.licenseIds.length).toBe(0);
      }));
      it('License IDs Cleared on SELECTED', inject(function() {
        scope.$apply(function() {
          scope.override.status = 'SELECTED';
        });
        expect(scope.override.status).toBe('SELECTED');
        expect(scope.override.licenseIds.length).toBe(0);
      }));
    });
    
    describe("Effective license updated", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides(null, null, null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));
      afterEach(inject(function($httpBackend, SelectedComponent) {
        scope.$apply(function() {
          scope.override.status = 'OVERRIDDEN';
        });
        scope.$apply(function() {
          scope.override.licenseIds = ['AFL-1.2'];
        });
        expect(SelectedComponent.effectiveLicenses).toBeUndefined();
        scope.save();
        $httpBackend.flush();
        expect(SelectedComponent.effectiveLicenses).toEqual(['AFL-1.2']);
      }));

      it('Add Org', inject(function($httpBackend, SelectedComponent) {
        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1')).respond(function(method, url, data,
                                                                                                                         headers)
        {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'OVERRIDDEN',
            licenseIds: ['AFL-1.2'],
            comment: ''
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });
        scope.$apply(function() {
          scope.override.ownerId = 'org1';
        });
      }));

      it('Add App', inject(function($httpBackend, SelectedComponent) {
        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1')).respond(function(method, url, data,
                                                                                                                        headers)
        {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'app1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'OVERRIDDEN',
            licenseIds: ['AFL-1.2'],
            comment : ''
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });
      }));
    });

    describe('Root Overrides', function () {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        var applied = getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', "AFL"),
            licenseOverridesByOwner = applied.licenseOverridesByOwner;
        licenseOverridesByOwner[2].licenseOverride = licenseOverridesByOwner[1].licenseOverride;
        licenseOverridesByOwner[1].licenseOverride = licenseOverridesByOwner[0].licenseOverride = null;

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(applied);

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Cannot Inherit', function () {
        expect(scope.override.ownerId).toEqual('root-organization');
        expect(scope.canInherit()).toBeFalsy();
      });
    });

    describe("Org Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', "AFL"));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'org1',
          status: 'OVERRIDDEN',
          licenseIds: ['AFL']
        });
        expect(scope.canInherit()).toBeTruthy();
      });

      it('Add Application', inject(function($httpBackend, SelectedComponent) {
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });

        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OPEN',
          licenseIds: []
        });
        expect(scope.canInherit()).toBeTruthy();

        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });

        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1')).respond(function(method, url, data,
                                                                                                     headers)
        {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'app1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment : ''
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });
        scope.save();
        $httpBackend.flush();

        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'ACKNOWLEDGED',
          licenseIds: []
        });
        expect(scope.canInherit()).toBeTruthy();
      }));
    });

    describe("App Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides('OVERRIDDEN', "AFL-1.2", null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OVERRIDDEN',
          licenseIds: ['AFL-1.2']
        });
        expect(scope.canInherit()).toBeTruthy();
        expect(scope.getInheritableStatus()).toEqual('Open');
      });

      it('Add Organization Override', inject(function($httpBackend, SelectedComponent) {
        scope.$apply(function() {
          scope.override.ownerId = 'org1';
        });

        expect(scope.override).toEqual({
          ownerId: 'org1',
          status: 'OPEN',
          licenseIds: []
        });
        expect(scope.canInherit()).toBeTruthy();

        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });

        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1')).respond(function(method, url, data,
                                                                                                      headers)
        {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment : ''
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });
        scope.save();
        $httpBackend.flush();

        // View is reset to show the app + Inherit option
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OVERRIDDEN',
          licenseIds: ['AFL-1.2']
        });
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });
        expect(scope.canInherit()).toBeTruthy();
      }));
    });

    describe("No Organization - App Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
            respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL'));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
            encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OVERRIDDEN',
          licenseIds: ['AFL']
        });
        expect(scope.canInherit()).toBeTruthy();
      });

      it('Add Application', inject(function($httpBackend, SelectedComponent) {
        expect(scope.canInherit()).toBeTruthy();

        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });
        expect(scope.override.licenseIds).toEqual([]);

        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1')).respond(function(method, url, data,
                                                                                                     headers)
        {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'app1',
            componentIdentifier: SelectedComponent.componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment : ''
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });
        scope.save();
        $httpBackend.flush();

        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'ACKNOWLEDGED',
          licenseIds: []
        });
        expect(scope.canInherit()).toBeTruthy();
      }));
    });

    describe('Synthetic Unknown Licenses', function () {
      function setup(declared, observed, selected) {
        inject(function ($controller, $httpBackend, SelectedComponent) {
          $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true')).respond(LicenseGroupMockData.getLicensesData());

          $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1?componentIdentifier=' +
                  encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).
                  respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL'));

          $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/componentDetails/application/app1/licenses?componentIdentifier=' +
                  encodeURIComponent(JSON.stringify(SelectedComponent.componentIdentifier)))).respond(getLicenseWithThreats(declared, observed, selected));

          $controller('LicenseEditorController', {
            $scope: scope
          });
          $httpBackend.flush();
        });
      }

      describe('Selected', function () {
        it('Synthetic Ignored', inject(function ($httpBackend, SelectedComponent) {
          setup({ licenseId : 'AFL-1.2' }, { licenseId : 'No-Sources' }, [{ licenseId : 'AFL-1.2' }]);
          expect(scope.selectableLicenses['AFL-1.2']).toBeTruthy();
          expect(scope.selectableLicenses['No-Sources']).toBeFalsy();
        }));
      });
    });
  });
}());
