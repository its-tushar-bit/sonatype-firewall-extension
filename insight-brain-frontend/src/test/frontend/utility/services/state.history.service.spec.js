describe('state.history.service.spec.js', function() {
  var $rootScope, StateHistoryService;

  beforeEach(module('utility.services'));

  beforeEach(inject([
    '$rootScope', 'state.history.service', function(_$rootScope_, _StateHistoryService_) {
      $rootScope = _$rootScope_;
      StateHistoryService = _StateHistoryService_;
    }
  ]));

  it('Adds state to history on stateChangeSuccess', function() {
    var newState = {name: 'new.state'};

    $rootScope.$broadcast('$stateChangeSuccess', undefined, undefined, newState);
    expect(StateHistoryService.getPreviousState()).toEqual(newState);

    var anotherState = {name: 'another.state'};
    $rootScope.$broadcast('$stateChangeSuccess', undefined, undefined, anotherState);
    expect(StateHistoryService.getPreviousState()).toEqual(anotherState);
  });
});
