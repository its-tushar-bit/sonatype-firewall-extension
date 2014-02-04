describe('TagController.js', function() {

  var testScope, dialogScope, bomId = 'bom1-12345678', tags = [
    {id: 1, ownerId: bomId, name: bomId, description: 'foo'},
    {id: 2, ownerId: bomId, name: bomId, description: 'bar'}],
    organizationTags = [
     {
       id: "tagid1",
       organizationId: bomId,
       name: "TagOne",
       nameLowercaseNoWhitespace: "tagone",
       description: "Tag One Description"
     }, {
        id: "tagid2",
        organizationId: bomId,
        name: "TagTwo",
        nameLowercaseNoWhitespace: "tagtwo",
        description: "Tag Two Description"
      }
    ], applicationTags = [
      {
        id: "tagid1",
        organizationId: bomId,
        name: "TagOne",
        nameLowercaseNoWhitespace: "tagone",
        description: "Tag One Description"
      }
    ];

  beforeEach(module('Tags', 'HttpInterceptors', function($provide) {
    $provide.value('$modal', {
      open: function(config) {
        dialogScope = testScope.$new();
        dialogScope.$close = function() {
        };
        inject(function($controller) {
          $controller(config.controller, {
            $scope: dialogScope
          });
        });
        return {
          result: {
            then: function(success, failure) {
              success();
            }
          }
        };
      }
    });

    $provide.value('ApplicationId', {
      encoded: function() {
        return bomId;
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return bomId;
      }
    });
    $provide.value('$state', {
      current: {
        name: 'organization'
      }
    });
  }));

  beforeEach(inject(function($rootScope) {
    testScope = $rootScope.$new();
  }));

  afterEach(function() {
    if (testScope) {
      testScope.$destroy();
    }
  });

  describe('Applying tags to an Application', function() {
    var scope, tagApplicationController;

    beforeEach(inject(function($controller, $httpBackend, CLMLocations, $state) {
      scope = testScope.$new();

      selectedApplication = {
        publicId: 'applicationPublicId',
        organizationId: bomId
      };
      $state.current.name = 'management.organization';
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationTagUrl(bomId))).respond(angular.copy(organizationTags));
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationTagUrl('applicationPublicId'))).respond(angular.copy(applicationTags));
      tagApplicationController = $controller('TagApplicationController', { $scope: scope, selectedApplication: selectedApplication });
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('Loads tags', function() {
      var loadedTags = [{
        id: 'tagid1',
        organizationId: bomId,
        name: 'TagOne',
        nameLowercaseNoWhitespace: 'tagone',
        description: 'Tag One Description',
        isApplied: true
      }, {
        id: 'tagid2',
        organizationId: bomId,
        name: 'TagTwo',
        nameLowercaseNoWhitespace: 'tagtwo',
        description: 'Tag Two Description',
        isApplied: false
      }];
      expect(scope.tags).toEqual(loadedTags);
    });

    it('Applies a tag', inject(function($httpBackend, CLMLocations) {
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getApplicationTagUrl('applicationPublicId'))).respond();
      scope.toggleApply(scope.tags[1]);
      $httpBackend.flush();

      expect(scope.tags[1].isApplied).toBe(true);
    }));

    it('Detaches a tag', inject(function($httpBackend, CLMLocations) {
      $httpBackend.expectDELETE(SpecUtil.toRegExp(CLMLocations.getDeleteApplicationTagUrl('applicationPublicId', scope.tags[0].id))).respond();
      scope.toggleApply(scope.tags[0]);
      $httpBackend.flush();

      expect(scope.tags[0].isApplied).toBe(false);
    }));
  });

  describe('Editing tests for Organization Tags', function() {
    var scope,
      tagEditorController,
      tagController,
      secondAppId = bomId + '-2',
      appliedTags = [
        {
          applicationId: bomId,
          id: 'appliedTag1',
          tagId: 1
        },
        {
          applicationId: secondAppId,
          id: 'appliedTag2',
          tagId: 1
        }
      ],
      applications = [
        {
          "id": bomId,
          "name": "applicationName",
          "publicId": bomId,
          "organizationId": bomId
        },
        {
          "id": secondAppId,
          "name": "applicationName2",
          "publicId": secondAppId,
          "organizationId": bomId
        }
      ],
      appliedPolicyTags = [
        {
          id: 'appliedPolicyTagId',
          tagId: appliedTags[0].id,
          policyId: PolicyMockData.getPolicyData()[0].id
        }
      ];

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMAppLocations, CLMLocations) {
      scope = testScope.$new();
      testScope.alerts = [];
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getTagsUrl())).respond(tags);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationAppliedTagUrl(bomId))).respond(appliedTags);
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getApplicationsUrl())).respond(applications);
      tagController = $controller('TagController', {$scope: testScope});
      tagEditorController = $controller('TagEditorController', {$scope: scope});
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('Can set color', function() {
      scope.createNew();
      var color = scope.colors[0];
      scope.setColor(color)
      expect(scope.selectedTag.color).toEqual(color);
    });

    it('Can cancel edit', function() {
      spyOn(scope, '$emit');
      scope.cancelEditTag();
      expect(scope.$emit).toHaveBeenCalledWith('tags.cancelEditTag');
    });

    it('Can delete a tag', inject(function($httpBackend, CLMAppLocations) {
      expect(testScope.tags.length).toEqual(2);
      $httpBackend.expectDELETE(SpecUtil.toRegExp(CLMAppLocations.getTagsUrl() + '/' +
        testScope.tags[0].id)).respond(204);
      scope.deleteTag(testScope.tags[0], { stopPropagation : angular.noop });
      expect(dialogScope.body).toContain('It is in use by the following applications: applicationName, applicationName2.');
      dialogScope.buttons[1].click();
      $httpBackend.flush();
      expect(testScope.tags.length).toEqual(1);
    }));

    it('Can save a new Tag', inject(function($httpBackend, CLMAppLocations) {
      scope.createNew();
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getTagsUrl())).respond(204);
      scope.saveTag();
      $httpBackend.flush();
    }));

    describe('Cancel Deselects', function() {
      it('New Tag', function() {
        scope.createNew();
        scope.alerts.push({type: 'mock', 'msg': 'mock alert'});
        expect(scope.selectedTag).not.toBeUndefined();

        scope.cancelEditTag();
        expect(scope.selectedTag).toEqual(null);
        expect(scope.alerts.length).toEqual(0);
      });

      it('Existing Tag', inject(function($httpBackend, CLMAppLocations) {
        scope.editTag(testScope.tags[0]);

        scope.alerts.push({type: 'mock', 'msg': 'mock alert'});
        expect(scope.selectedTag.id).not.toBeUndefined();

        scope.cancelEditTag();
        expect(scope.selectedTag).toEqual(null);
        expect(scope.alerts.length).toEqual(0);
      }));
    });

    describe('Dirty Checks', function() {
      describe('Dirty New Tag', function() {
        beforeEach(function() {
          scope.createNew();
          scope.selectedTag.name = 'foo';
          expect(scope.selectedTag.isDirty()).toEqual(true);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(true);
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedTag.name).toEqual('foo');
        });

        it('Edit Existing Attempted', function() {
          scope.editTag(testScope.tags[0]);
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedTag.name).toEqual('foo');
        });
      });

      describe('Dirty Existing Tag', function() {
        beforeEach(inject(function($httpBackend, CLMAppLocations) {
          scope.editTag(testScope.tags[0]);

          scope.selectedTag.name = 'foo';
          expect(scope.selectedTag.isDirty()).toEqual(true);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(true);
        }));

        it('Edit Existing Attempted', function() {
          scope.editTag(testScope.tags[1]);
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedTag.name).toEqual('foo');
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedTag.name).toEqual('foo');
        });
      });

      describe('Unmodified Existing Tag', function() {
        beforeEach(inject(function($httpBackend, CLMAppLocations) {
          scope.editTag(testScope.tags[0]);

          expect(scope.selectedTag.isDirty()).toEqual(false);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(false);
        }));

        it('Edit Existing Attempted', inject(function($httpBackend, CLMAppLocations) {
          scope.editTag(testScope.tags[1]);

          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedTag.id).toEqual(testScope.tags[1].id);
        }));

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedTag.id).toBeDefined();
        });
      });

      it('Unmodified New Tag - Edit Existing Attempted', inject(function($httpBackend, CLMAppLocations) {
        scope.createNew();
        expect(scope.selectedTag.isDirty()).toEqual(false);

        var e = scope.$broadcast('pageChangeStarted', null);
        expect(e.defaultPrevented).toEqual(false);

        scope.editTag(testScope.tags[0]);

        expect(scope.alerts.length).toEqual(0);
        expect(scope.selectedTag.id).toEqual(testScope.tags[0].id);
      }));
    });
  });
});
