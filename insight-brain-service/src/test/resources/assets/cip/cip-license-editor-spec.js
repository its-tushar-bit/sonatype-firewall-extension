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
            groupId: 'org.groupid',
            artifactId: 'artifactid',
            version: '1',
            status: appStatus,
            licenseId: appLicense,
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
            groupId: 'org.groupid',
            artifactId: 'artifactid',
            version: '1',
            status: orgStatus,
            licenseId: orgLicense,
            comment: ''
          }
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

  function getLicenseWithThreats() {
    var x = {
      declaredlicenses: [],
      observedlicenses: []
    };
    x.declaredlicenses.push({
      threat: 4,
      license: LicenseGroupMockData.getLicensesData()[0]
    });
    x.observedlicenses.push({
      threat: 9,
      license: LicenseGroupMockData.getLicensesData()[1]
    });
    return x;
  }

  describe('CIP License Editor', function() {
    beforeEach(module('LicenseEditor', 'ApplicationIdProvider', function($provide) {
      $provide.value('SelectedComponent', {
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
    }));

    afterEach(function() {
      scope.$destroy();
    });

    describe('App+Org with Overrides', function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' +
            SelectedComponent.version)).respond(getAppliedLicenseOverrides('ACKNOWLEDGED', null, 'OVERRIDDEN', "AFL"));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/component/details/licenses/app1?artifactId=' +
            SelectedComponent.artifactId +
            '&groupId=' + SelectedComponent.groupId + '&version=' +
            SelectedComponent.version)).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'ACKNOWLEDGED',
          licenseId: null
        });

        expect(scope.statuses.length).toEqual(6);
        expect(scope.statuses[5]).toEqual({
          value: 'DELETE',
          label: 'Inherit Status (Overridden)'
        });
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
          licenseId: 'AFL'
        });
        expect(scope.statuses.length).toEqual(5);
      }));
    });

    describe("No Overrides", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' +
            SelectedComponent.version)).respond(getAppliedLicenseOverrides(null, null, null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/component/details/licenses/app1?artifactId=' +
            SelectedComponent.artifactId +
            '&groupId=' + SelectedComponent.groupId + '&version=' +
            SelectedComponent.version)).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OPEN',
          licenseId: null
        });
        expect(scope.statuses.length).toEqual(5);
      });

      it('Add Org', inject(function($httpBackend, SelectedComponent) {
        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1')).respond(function(method, url, data,
                                                                                                      headers)
        {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'org1',
            artifactId: SelectedComponent.artifactId,
            groupId: SelectedComponent.groupId,
            version: SelectedComponent.version,
            status: 'ACKNOWLEDGED',
            licenseId: null,
            comment : ''
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
            artifactId: SelectedComponent.artifactId,
            groupId: SelectedComponent.groupId,
            version: SelectedComponent.version,
            status: 'ACKNOWLEDGED',
            licenseId: null,
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

    describe("Org Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' +
            SelectedComponent.version)).respond(getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', "AFL"));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/component/details/licenses/app1?artifactId=' +
            SelectedComponent.artifactId +
            '&groupId=' + SelectedComponent.groupId + '&version=' +
            SelectedComponent.version)).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'org1',
          status: 'OVERRIDDEN',
          licenseId: 'AFL'
        });
        expect(scope.statuses.length).toEqual(5);
      });

      it('Add Application', inject(function($httpBackend, SelectedComponent) {
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });

        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OPEN',
          licenseId: null
        });
        expect(scope.statuses.length).toEqual(5);

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
            artifactId: SelectedComponent.artifactId,
            groupId: SelectedComponent.groupId,
            version: SelectedComponent.version,
            status: 'ACKNOWLEDGED',
            licenseId: null,
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
          licenseId: null
        });
        expect(scope.statuses.length).toEqual(6);
      }));
    });

    describe("App Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' +
            SelectedComponent.version)).respond(getAppliedLicenseOverrides('OVERRIDDEN', "AFL", null, null));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/component/details/licenses/app1?artifactId=' +
            SelectedComponent.artifactId +
            '&groupId=' + SelectedComponent.groupId + '&version=' +
            SelectedComponent.version)).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OVERRIDDEN',
          licenseId: 'AFL'
        });
        expect(scope.statuses.length).toEqual(6);
        expect(scope.statuses[5]).toEqual({ value: 'DELETE', label: 'Inherit Status (Open)' });
      });

      it('Add Organization Override', inject(function($httpBackend, SelectedComponent) {
        scope.$apply(function() {
          scope.override.ownerId = 'org1';
        });

        expect(scope.override).toEqual({
          ownerId: 'org1',
          status: 'OPEN',
          licenseId: null
        });
        expect(scope.statuses.length).toEqual(5);

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
            artifactId: SelectedComponent.artifactId,
            groupId: SelectedComponent.groupId,
            version: SelectedComponent.version,
            status: 'ACKNOWLEDGED',
            licenseId: null,
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
          licenseId: 'AFL'
        });
        scope.$apply(function() {
          scope.override.ownerId = 'app1';
        });
        expect(scope.statuses.length).toEqual(6);
      }));
    });

    describe("No Organization - App Overridden", function() {
      beforeEach(inject(function($controller, $httpBackend, SelectedComponent) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license')).respond(LicenseGroupMockData.getLicensesData());

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1/applied/' +
            SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' +
            SelectedComponent.version)).respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL'));

        $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/ci/component/details/licenses/app1?artifactId=' +
            SelectedComponent.artifactId +
            '&groupId=' + SelectedComponent.groupId + '&version=' +
            SelectedComponent.version)).respond(getLicenseWithThreats());

        $controller('LicenseEditorController', {
          $scope: scope
        });
        $httpBackend.flush();
      }));

      it('Default Selection', function() {
        expect(scope.override).toEqual({
          ownerId: 'app1',
          status: 'OVERRIDDEN',
          licenseId: 'AFL'
        });
        expect(scope.statuses.length).toEqual(5);
      });

      it('Add Application', inject(function($httpBackend, SelectedComponent) {
        expect(scope.statuses.length).toEqual(5);

        scope.$apply(function() {
          scope.override.status = 'ACKNOWLEDGED';
        });
        expect(scope.override.licenseId).toEqual(null);

        $httpBackend.expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/app1')).respond(function(method, url, data,
                                                                                                     headers)
        {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'app1',
            artifactId: SelectedComponent.artifactId,
            groupId: SelectedComponent.groupId,
            version: SelectedComponent.version,
            status: 'ACKNOWLEDGED',
            licenseId: null,
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
          licenseId: null
        });
        expect(scope.statuses.length).toEqual(5);
      }));
    });
  });
}());