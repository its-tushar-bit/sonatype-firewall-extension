/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function orgAppPickerExampleController() {
  const vm = this;

  vm.organizations = [
    {id: 'fooOrg', name: 'Foo Org'},
    {id: 'barOrg', name: 'Bar Org'},
    {id: 'bazOrg', name: 'Baz Org'}
  ];

  vm.applications = [
    {id: 'fooApp1', name: 'Foo App 1', organizationId: 'fooOrg', organizationName: 'Foo Org'},
    {id: 'fooApp2', name: 'Foo App 2', organizationId: 'fooOrg', organizationName: 'Foo Org'},
    {id: 'bazApp', name: 'Baz App', organizationId: 'bazOrg', organizationName: 'Baz Org'}
  ];

  vm.selected = {
    organizations: new Set(['barOrg']),
    applications: new Set()
  };

  vm.selectOrgsAndApps = function(organizations, applications) {
    vm.selected = {
      organizations,
      applications
    };
  };
}
