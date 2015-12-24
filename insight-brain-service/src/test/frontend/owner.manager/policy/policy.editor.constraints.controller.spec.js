describe('policy.editor.constraints.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      $timeout,
      constraintStoreDefer;

  beforeEach(inject(function($q, _$timeout_, $controller, ConstraintStore) {
    $timeout = _$timeout_;

    constraintStoreDefer = $q.defer();

    spyOn(constraintStoreDefer.promise, 'then').andCallThrough();
    spyOn(ConstraintStore, 'get').andReturn(constraintStoreDefer.promise);

    vm = $controller('policy.editor.constraints.controller');
  }));

  it('Properly loads conditions', function() {
    var conditionTypeValues = {};
    ConditionTypeValueResourceMockData.getConditionValueTypeUrl().forEach(function(typeValue) {
      conditionTypeValues[typeValue.id] = typeValue;
    });

    resolveLoadData();

    Object.keys(vm.conditionTypes).forEach(function(type) {
      expect(vm.conditionTypes[type].valueType).toEqual(conditionTypeValues[vm.conditionTypes[type].valueTypeId]);
    });
  });

  it('Properly constructs condition string', function() {

    resolveLoadData();

    expect(vm.conditionString({
      conditionTypeId: 'Label',
      operator: 'is',
      value: '6be0f524314245c7aded40b3d4ac8112'
    })).toMatch('Label is App Component Label');

    expect(vm.conditionString({
      conditionTypeId: 'License Threat Group',
      operator: 'is not',
      value: 'd341ca90a4ea4971aa84376148892c7d'
    })).toMatch('License Threat Group is not Liberal');

    expect(vm.conditionString({
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: '730'
    })).toMatch('Age older than 2 Years');

    expect(vm.conditionString({
      conditionTypeId: 'SecurityVulnerabilityStatus',
      operator: 'is',
      value: 'ACKNOWLEDGED'
    })).toMatch('Security Vulnerability Status is Acknowledged');
  });

  function resolveLoadData() {
    constraintStoreDefer.resolve([
      PolicyResourceMockData.getConditionTypeUrl(), ConditionTypeValueResourceMockData.getConditionValueTypeUrl()
    ]);

    $timeout.flush();
  }
});
