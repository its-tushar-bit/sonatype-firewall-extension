/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../../../main/frontend/components/module';

describe('iqOrgAppPickerAngular', function() {

  var getVm, onChange;

  var organizations = [
    {id: 'fooOrg', name: 'Foo Org'},
    {id: 'barOrg', name: 'Bar Org'},
    {id: 'bazOrg', name: 'Baz Org'}
  ];

  var applications = [
    {id: 'fooApp1', name: 'Foo App 1', organizationId: 'fooOrg'},
    {id: 'fooApp2', name: 'Foo App 2', organizationId: 'fooOrg'},
    {id: 'barApp1', name: 'Bar App 1', organizationId: 'barOrg'},
    {id: 'barApp2', name: 'Bar App 2', organizationId: 'barOrg'}
  ];

  beforeEach(angular.mock.module(componentsModule.name));

  beforeEach(inject(function($componentController) {
    onChange = jasmine.createSpy('onChange');
    getVm = function(selectedOrganizations, selectedApplications) {
      return $componentController('iqOrgAppPickerAngular', null, {
        onChange: onChange,
        organizations: organizations,
        applications: applications,
        selectedOrganizations: selectedOrganizations,
        selectedApplications: selectedApplications
      });
    };
  }));

  describe('$onChanges()', function() {
    it('sets provided selected Orgs and Apps', function() {
      var selectedOrganizations = new Set(['fooOrg']);
      var selectedApplications = new Set(['fooApp1', 'fooApp2']);

      var vm = getVm();
      vm.$onChanges({
        providedSelectedOrganizations: {currentValue: selectedOrganizations},
        providedSelectedApplications: {currentValue: selectedApplications}
      });

      expect(vm.selectedOrganizations).toBe(selectedOrganizations);
      expect(vm.selectedApplications).toBe(selectedApplications);
    });

    it('converts provided selected Orgs and Apps objects to Sets', function() {
      var vm = getVm();
      vm.$onChanges({
        providedSelectedOrganizations: {currentValue: {fooOrg: true}},
        providedSelectedApplications: {currentValue: {fooApp1: true, fooApp2: true}}
      });

      expect(vm.selectedOrganizations).toEqual(new Set(['fooOrg']));
      expect(vm.selectedApplications).toEqual(new Set(['fooApp1', 'fooApp2']));
    });
  });

  describe('onSelectedOrganizationsChange()', function() {
    describe('when all orgs are selected (all)', function() {
      it('selects all apps', function() {
        //starts with nothing selected.
        var selectedOrganizations = new Set();
        var selectedApplications = new Set();

        var vm = getVm(selectedOrganizations, selectedApplications);
        // selects all Orgs
        var newSelectedOrganizations = new Set(['fooOrg', 'barOrg', 'bazOrg']);
        //expect all Apps to be selected.
        var expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

        vm.onSelectedOrganizationsChange(newSelectedOrganizations);
        expect(onChange).toHaveBeenCalledWith({
          selectedOrganizations: newSelectedOrganizations,
          selectedApplications: expectedSelectedApplications
        });
      });
    });

    describe('when all orgs are deselected (none)', function() {
      it('deselects all apps', function() {
        var selectedOrganizations = new Set(['fooOrg', 'barOrg', 'bazOrg']);
        var selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

        var vm = getVm(selectedOrganizations, selectedApplications);
        var newSelectedOrganizations = new Set();
        var expectedSelectedApplications = new Set();

        vm.onSelectedOrganizationsChange(newSelectedOrganizations);
        expect(onChange).toHaveBeenCalledWith({
          selectedOrganizations: newSelectedOrganizations,
          selectedApplications: expectedSelectedApplications
        });
      });
    });

    describe('when an org is selected', function() {
      it('selects related apps', function() {
        var selectedOrganizations = new Set();
        var selectedApplications = new Set(['fooApp2', 'barApp1']);

        var vm = getVm(selectedOrganizations, selectedApplications);

        var newSelectedOrganizations = new Set(['fooOrg']);
        var expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);

        vm.onSelectedOrganizationsChange(newSelectedOrganizations);
        expect(onChange).toHaveBeenCalledWith({
          selectedOrganizations: newSelectedOrganizations,
          selectedApplications: expectedSelectedApplications
        });
      });
    });

    describe('when an org is toggled to be deselected', function() {
      describe('when all related apps are selected', function() {
        it('deselects related apps', function() {
          var selectedOrganizations = new Set(['fooOrg']);
          var selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1']);

          var vm = getVm(selectedOrganizations, selectedApplications);

          var newSelectedOrganizations = new Set();
          var expectedSelectedApplications = new Set(['barApp1']);
          vm.onSelectedOrganizationsChange(newSelectedOrganizations, 'fooOrg');

          expect(onChange).toHaveBeenCalledWith({
            selectedOrganizations: newSelectedOrganizations,
            selectedApplications: expectedSelectedApplications
          });
        });
      });
    });

    describe('when an org is not selected and was not toggled', function() {
      describe('when not all related apps are selected', function() {
        it('does not deselect related apps', function() {
          var selectedOrganizations = new Set();
          var selectedApplications = new Set(['fooApp1']);

          var vm = getVm(selectedOrganizations, selectedApplications);

          var newSelectedOrganizations = new Set(['barOrg']);
          var expectedSelectedApplications = new Set(['fooApp1', 'barApp1', 'barApp2']);

          vm.onSelectedOrganizationsChange(newSelectedOrganizations, 'barOrg');
          expect(onChange).toHaveBeenCalledWith({
            selectedOrganizations: newSelectedOrganizations,
            selectedApplications: expectedSelectedApplications
          });
        });
      });

      // this is to fix CLM-8852
      describe('when all related apps are selected', function() {
        it('does not deselect related apps', function() {
          var selectedApplications = new Set(['fooApp1', 'fooApp2']);
          var vm = getVm(new Set(), selectedApplications);

          var newSelectedOrganizations = new Set(['barOrg']);
          var expectedSelectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);

          vm.onSelectedOrganizationsChange(newSelectedOrganizations, 'barOrg');
          expect(onChange).toHaveBeenCalledWith({
            selectedOrganizations: newSelectedOrganizations,
            selectedApplications: expectedSelectedApplications
          });
        });
      });
    });
  });

  describe('onSelectedApplicationsChange()', function() {
    it('does not select an org when all related apps are selected', function() {
      var selectedApplications = new Set(['fooApp1']);
      var selectedOrganizations = new Set();

      var vm = getVm(selectedOrganizations, selectedApplications);

      var newSelectedApplications = new Set(['fooApp1', 'fooApp2']);
      var expectedSelectedOrganizations = new Set();

      vm.onSelectedApplicationsChange(newSelectedApplications);
      expect(onChange).toHaveBeenCalledWith({
        selectedOrganizations: expectedSelectedOrganizations,
        selectedApplications: newSelectedApplications
      });
    });

    it('deselects an org if not all related apps are selected', function() {
      var selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      var selectedOrganizations = new Set(['fooOrg', 'barOrg']);

      var vm = getVm(selectedOrganizations, selectedApplications);

      var newSelectedApplications = new Set(['fooApp1', 'barApp1', 'barApp2']);
      var expectedSelectedOrganizations = new Set(['barOrg']);

      vm.onSelectedApplicationsChange(newSelectedApplications);
      expect(onChange).toHaveBeenCalledWith({
        selectedOrganizations: expectedSelectedOrganizations,
        selectedApplications: newSelectedApplications
      });
    });

    it('deselects an org if all related apps are deselected', function() {
      var selectedApplications = new Set(['fooApp1', 'fooApp2', 'barApp1', 'barApp2']);
      var selectedOrganizations = new Set(['fooOrg', 'barOrg']);

      var vm = getVm(selectedOrganizations, selectedApplications);

      var newSelectedApplications = new Set(['barApp1', 'barApp2']);
      var expectedSelectedOrganizations = new Set(['barOrg']);

      vm.onSelectedApplicationsChange(newSelectedApplications);
      expect(onChange).toHaveBeenCalledWith({
        selectedOrganizations: expectedSelectedOrganizations,
        selectedApplications: newSelectedApplications
      });
    });

    it('does not deselect an org if it has no apps', function() {
      var selectedApplications = new Set(['fooApp1']);
      var selectedOrganizations = new Set(['bazOrg']);

      var vm = getVm(selectedOrganizations, selectedApplications);

      var newSelectedApplications = new Set();
      var expectedSelectedOrganizations = new Set(['bazOrg']);

      vm.onSelectedApplicationsChange(newSelectedApplications);
      expect(onChange).toHaveBeenCalledWith({
        selectedOrganizations: expectedSelectedOrganizations,
        selectedApplications: newSelectedApplications
      });
    });
  });
});
