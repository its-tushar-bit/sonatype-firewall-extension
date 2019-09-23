import mainHeaderModule from '../../../../main/frontend/mainHeader/module';

describe('userDetailsModal', function() {
  let vm;

  beforeEach(angular.mock.module(mainHeaderModule.name));

  beforeEach(inject(function($componentController) {
    vm = $componentController('userDetailsModal');
    vm.currentUser = {
      groups: ['Lima', 'Bravo', 'Xray', 'Oscar']
    };
  }));

  describe('getGroups', function() {
    it('formats the current user group array into an ordered string', function() {
      expect(vm.getGroups()).toBe('Bravo, Lima, Oscar, Xray');
    });
  });
});
