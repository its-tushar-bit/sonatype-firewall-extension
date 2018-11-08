import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipTabPanel', function() {
  let $componentController;

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function(_$componentController_) {
    $componentController = _$componentController_;
  }));

  it('sets the initial value of vm.selectedTab to componentInfo', function() {
    const controller = $componentController('cipTabPanel');

    expect(controller.selectedTab).toBe('componentInfo');
  });

  describe('isComponentUnknown', function() {
    it('returns true iff the matchState field of the selectedComponent is "unknown"', function() {
      function createControllerWithMatchState(matchState) {
        return $componentController('cipTabPanel', null, { selectedComponent: { matchState } });
      }

      expect(createControllerWithMatchState('unknown').isComponentUnknown()).toBe(true);
      expect(createControllerWithMatchState('exact').isComponentUnknown()).toBe(false);
      expect(createControllerWithMatchState('similar').isComponentUnknown()).toBe(false);
      expect(createControllerWithMatchState(undefined).isComponentUnknown()).toBe(false);
    });
  });
});
