describe('LabelController.js', function() {

  var LabelMockData = {
    getLabels: function() {
      return LabelMockData.getApplicableLabels().labelsByOwner[0].labels;
    },
    getApplicableLabels: function() {
      return {
        "labelsByOwner": [
          {
            "ownerId": "appownerid",
            "ownerName": "appname",
            "ownerType": "application",
            "labels": [
              {
                "id": "applabelid",
                "ownerId": "appownerid",
                "label": "AppLabel",
                "labelLowercase": "applabel",
                "color": "red"
              },
              {
                "id": "applabelid_01",
                "ownerId": "appownerid",
                "label": "AnotherAppLabel",
                "labelLowercase": "anotherapplabel",
                "color": "red"
              }
            ]
          },
          {
            "ownerId": "orgownerid",
            "ownerName": "orgname",
            "ownerType": "organization",
            "labels": [
              {
                "id": "orglabelid",
                "ownerId": "orgownerid",
                "label": "OrgLabel",
                "labelLowercase": "orglabel",
                "color": "red"
              }
            ]
          }
        ]
      };
    }
  };

  beforeEach(module('Labels', function($provide) {
    $provide.factory('hudson', [
      '$http', function($http) {
        return $http;
      }
    ]);

    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
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

  describe('LabelController itemLabel tests', function() {
    var scope,
        compileInput,
        setInput;

    beforeEach(inject(function($rootScope, $compile, $sniffer) {
      var inputElement;
      scope = testScope;
      compileInput = function(input) {
        inputElement = angular.element(input);
        var formElement = angular.element("<form name='form'></form>");
        formElement.append(inputElement);
        $compile(formElement)(scope);
      };
      setInput = function(val) {
        inputElement.val(val);

        var evt = document.createEvent('HTMLEvents');
        evt.initEvent(($sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
        inputElement[0].dispatchEvent(evt);
      };
    }));

    it('Test No Spaces', function() {
      compileInput("<input type='text' maxlength='50' name='label' ng-model='label'  item-label />");
      setInput('foo');
      expect(scope.form.$invalid).toEqual(false);
      expect(scope.form.label.$error.invalid).toEqual(false);

      setInput('foo bar');
      expect(scope.form.$invalid).toEqual(true);
      expect(scope.form.label.$error.invalid).toEqual(true);
    });

    it('Test Duplicate', function() {
      scope.selectedLabel = {};
      compileInput("<input type='text' maxlength='50' name='label' ng-model='selectedLabel.label'  item-label />");
      scope.labels = [
        { id: 'bar', label: 'bar' }
      ];
      setInput('foo');
      expect(scope.form.$invalid).toEqual(false);
      expect(scope.form.label.$error.duplicate).toEqual(false);

      setInput('bar');
      expect(scope.form.$invalid).toEqual(true);
      expect(scope.form.label.$error.duplicate).toEqual(true);
    });
  });

  describe('Editing tests', function() {
    var scope,
        labelEditController,
        labelController;

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMAppLocations, $state) {
      scope = testScope.$new();
      testScope.alerts = [];
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLabelsUrl())).respond(LabelMockData.getLabels());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicableLabelsUrl())).respond(LabelMockData.getApplicableLabels());
      labelController = $controller('LabelController', {$scope: testScope});
      labelEditController = $controller('LabelEditorController', {$scope: scope});
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
      expect(scope.selectedLabel.color).toEqual(color);
    });

    it('Can cancel edit', function() {
      spyOn(scope, '$emit');
      scope.cancelEditLabel();
      expect(scope.$emit).toHaveBeenCalledWith('labels.cancelEditLabel');
    });

    it('Can delete a label', inject(function($httpBackend, CLMAppLocations) {
      $httpBackend.expectDELETE(CLMAppLocations.getLabelsUrl() + '/' +
          testScope.applicableLabels[0].labels[0].id).respond(204);
      scope.deleteLabel(testScope.applicableLabels[0].labels[0]);
      $httpBackend.flush();
      expect(testScope.applicableLabels[0].labels.length).toEqual(1);
    }));

    describe('Cancel Deselects', function() {
      it('New Label', function() {
        scope.createNew();
        scope.alerts.push({type: 'mock', 'msg': 'mock alert'});
        expect(scope.selectedLabel).not.toBeUndefined();

        scope.cancelEditLabel();
        expect(scope.selectedLabel).toEqual(null);
        expect(scope.alerts.length).toEqual(0);
      });

      it('Existing Label', function() {
        scope.editLabel(true, testScope.applicableLabels[0].labels[0]);
        scope.alerts.push({type: 'mock', 'msg': 'mock alert'});
        expect(scope.selectedLabel.id).not.toBeUndefined();

        scope.cancelEditLabel();
        expect(scope.selectedLabel).toEqual(null);
        expect(scope.alerts.length).toEqual(0);
      });
    });

    describe('Dirty Checks', function() {
      describe('Dirty New Label', function() {
        beforeEach(function() {
          scope.createNew();
          scope.selectedLabel.name = 'foo';
          expect(scope.selectedLabel.isDirty()).toEqual(true);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(true);
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedLabel.name).toEqual('foo');
        });

        it('Edit Existing Attempted', function() {
          scope.editLabel(true, testScope.applicableLabels[0].labels[0]);
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedLabel.name).toEqual('foo');
        });
      });

      describe('Dirty Existing Label', function() {
        beforeEach(function() {
          scope.editLabel(true, testScope.applicableLabels[0].labels[0]);
          scope.selectedLabel.name = 'foo';
          expect(scope.selectedLabel.isDirty()).toEqual(true);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(true);
        });

        it('Edit Existing Attempted', function() {
          scope.editLabel(true, testScope.applicableLabels[0].labels[1]);
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedLabel.name).toEqual('foo');
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(1);
          expect(scope.selectedLabel.name).toEqual('foo');
        });
      });

      describe('Unmodified Existing Label', function() {
        beforeEach(function() {
          scope.editLabel(true, testScope.applicableLabels[0].labels[0]);
          expect(scope.selectedLabel.isDirty()).toEqual(false);

          var e = scope.$broadcast('pageChangeStarted', null);
          expect(e.defaultPrevented).toEqual(false);
        });

        it('Edit Existing Attempted', function() {
          scope.editLabel(true, testScope.applicableLabels[0].labels[1]);
          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedLabel.id).toEqual(testScope.applicableLabels[0].labels[1].id);
        });

        it('Create New Attempted', function() {
          scope.createNew();
          expect(scope.alerts.length).toEqual(0);
          expect(scope.selectedLabel.id).toBeDefined();
        });
      });

      it('Unmodified New Label - Edit Existing Attempted', function() {
        scope.createNew();
        expect(scope.selectedLabel.isDirty()).toEqual(false);

        var e = scope.$broadcast('pageChangeStarted', null);
        expect(e.defaultPrevented).toEqual(false);

        scope.editLabel(true, testScope.applicableLabels[0].labels[0]);
        expect(scope.alerts.length).toEqual(0);
        expect(scope.selectedLabel.id).toEqual(testScope.applicableLabels[0].labels[0].id);
      });
    });
  });
});
