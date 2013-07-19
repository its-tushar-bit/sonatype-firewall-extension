describe('LabelController.js', function() {

  var labelTemplate = {id: null, ownerId: null, label: '', labelLowercase: null, color: null};
  function toRegExp(url) {
    return new RegExp(url + '\\?timestamp=[0-9]+')
  }
  var LabelMockData = {
    getLabels : function(){
      return [angular.copy(labelTemplate)]
    }
  }
  //var LabelStore = {
  //  create: function(){
  //    return angular.copy(labelTemplate);
  //  },
  //  get: function(){}
  //}
  ////provide a mocked LabelStore impl
  //beforeEach(module('Labels',function($provide) {
  //  $provide.value('LabelStore', LabelStore);
  //}));

  beforeEach(module('Labels'));

  beforeEach(inject(function ($rootScope) {
    testScope = $rootScope.$new();
  }));

  afterEach(function() {
    if (testScope) {
      testScope.$destroy();
    }
  });

  describe('LabelController itemLabel tests', function () {
    var scope,
        compileInput,
        setInput;

    beforeEach(inject(function ($rootScope, $compile, $sniffer) {
      var inputElement;
      scope = testScope;
      compileInput = function (input) {
        inputElement = angular.element(input);
        var formElement = angular.element("<form name='form'></form>");
        formElement.append(inputElement);
        $compile(formElement)(scope);
      };
      setInput = function (val) {
        inputElement.val(val);

        var evt = document.createEvent('HTMLEvents');
        evt.initEvent(($sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
        inputElement[0].dispatchEvent(evt);
      };
    }));

    it('Test No Spaces', function () {
      compileInput("<input type='text' maxlength='50' name='label' ng-model='label'  item-label />");
      setInput('foo');
      expect(scope.form.$invalid).toEqual(false);
      expect(scope.form.label.$error.invalid).toEqual(false);

      setInput('foo bar');
      expect(scope.form.$invalid).toEqual(true);
      expect(scope.form.label.$error.invalid).toEqual(true);
    });

    it('Test Non Empty', function () {
      compileInput("<input type='text' maxlength='50' name='label' ng-model='label'  item-label />");
      setInput('foo');
      expect(scope.form.$invalid).toEqual(false);
      expect(scope.form.label.$error.empty).toEqual(false);

      setInput('');
      expect(scope.form.$invalid).toEqual(true);
      expect(scope.form.label.$error.empty).toEqual(true);
    });

    it('Test Duplicate', function () {
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

  describe('Editing tests', function(){
    var scope, labelEditController, labelController;
    beforeEach(inject(function ($rootScope, $controller, $httpBackend, CLMAppLocations, $state) {
      scope = testScope;
      scope.alerts = [];
      $httpBackend.whenGET(toRegExp(CLMAppLocations.getLabelsUrl())).respond(LabelMockData.getLabels());
      labelController = $controller('LabelController', {$scope: scope});
      labelEditController = $controller('LabelEditorController', {$scope: scope});
      $httpBackend.flush();
    }));

    it('Can set color', function(){
      scope.click();
      var color = scope.colors[0];
      scope.setColor(color)
      expect(scope.selectedLabel.color).toEqual(color);
    });

    it('Can cancel edit', function(){
      spyOn(scope, '$emit');
      scope.cancelEditLabel();
      expect(scope.$emit).toHaveBeenCalledWith('labels.cancelEditLabel');
    });

    it('Can deselect properly', function(){
      scope.click();
      scope.alerts.push({type:'mock', 'msg': 'mock alert'});
      expect(scope.selectedLabel).not.toBeUndefined();
      expect(scope.label).not.toBeUndefined();
      expect(scope.alerts.length).toEqual(1);

      scope.cancelEditLabel();

      expect(scope.selectedLabel).toBeUndefined();
      expect(scope.label).toBeUndefined();
      expect(scope.alerts.length).toEqual(0);
    });

    it('Can edit a label with or without an id', function(){
      scope.editLabel();
      expect(scope.selectedLabel).toEqual(labelTemplate);
    });

    it('Can delete a label', inject(function($httpBackend, CLMAppLocations){
      scope.click();
      var selectedLabel = scope.selectedLabel;
      spyOn(selectedLabel, '$delete');
      try {
        scope.deleteLabel(selectedLabel);
      }
      catch (e) {
        //missing $delete method but checking to ensure that it does
        //get called
      }
      expect(selectedLabel.$delete).toHaveBeenCalled()
    }));

    it('Results in an error if attempting to add a new Label while editing an existing one', function(){
      scope.click();
      scope.selectedLabel.id = 'test';
      expect(scope.alerts.length).toEqual(0);
      scope.click();
      expect(scope.alerts.length).toEqual(1);
    });
  });
});
