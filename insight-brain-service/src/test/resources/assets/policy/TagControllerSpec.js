describe('TagController.js', function() {

  var testScope, dialogScope, tags = [
    {id: 1, ownerId: 'bom-12345678', name: 'bom-12345678', description: 'foo'},
    {id: 2, ownerId: 'bom-12345678', name: 'bom-12345678', description: 'bar'}];

  beforeEach(module('Tags', function($provide) {
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
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return 'bom1-12345678';
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

  describe('Editing tests', function() {
    var scope,
      tagEditorController,
      tagController;

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMAppLocations) {
      scope = testScope.$new();
      testScope.alerts = [];
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getTagsUrl())).respond(tags);
      tagController = $controller('TagController', {$scope: testScope});
      tagEditorController = $controller('TagEditorController', {$scope: scope});
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('Can cancel edit', function() {
      spyOn(scope, '$emit');
      scope.cancelEditTag();
      expect(scope.$emit).toHaveBeenCalledWith('tags.cancelEditTag');
    });

    it('Can delete a tag', inject(function($httpBackend, CLMAppLocations) {
      expect(testScope.tags.length).toEqual(2);
      $httpBackend.expectDELETE(CLMAppLocations.getTagsUrl() + '/' +
        testScope.tags[0].id).respond(204);
      scope.deleteTag(testScope.tags[0], { stopPropagation : angular.noop });
      dialogScope.buttons[1].click()
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

      it('Existing Tag', function() {
        scope.editTag(testScope.tags[0]);
        scope.alerts.push({type: 'mock', 'msg': 'mock alert'});
        expect(scope.selectedTag.id).not.toBeUndefined();

        scope.cancelEditTag();
        expect(scope.selectedTag).toEqual(null);
        expect(scope.alerts.length).toEqual(0);
      });
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
        beforeEach(function() {
          scope.editTag(testScope.tags[0]);
          scope.selectedTag.name = 'foo';
          expect(scope.selectedTag.isDirty()).toEqual(true);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(true);
        });

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
        beforeEach(function() {
          scope.editTag(testScope.tags[0]);
          expect(scope.selectedTag.isDirty()).toEqual(false);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(false);
        });

        it('Edit Existing Attempted', function() {
          scope.editTag(testScope.tags[1]);
          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedTag.id).toEqual(testScope.tags[1].id);
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedTag.id).toBeDefined();
        });
      });

      it('Unmodified New Tag - Edit Existing Attempted', function() {
        scope.createNew();
        expect(scope.selectedTag.isDirty()).toEqual(false);

        var e = scope.$broadcast('pageChangeStarted', null);
        expect(e.defaultPrevented).toEqual(false);

        scope.editTag(testScope.tags[0]);
        expect(scope.alerts.length).toEqual(0);
        expect(scope.selectedTag.id).toEqual(testScope.tags[0].id);
      });
    });
  });
});
