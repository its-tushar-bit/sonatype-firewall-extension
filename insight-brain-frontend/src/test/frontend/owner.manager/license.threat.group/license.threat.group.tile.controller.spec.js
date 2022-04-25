/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/license.threat.group/license.threat.group.tile.controller';

describe('license.threat.group.tile.controller', () => {
  var vm, scope, EventNameConstant;
  let currentLTG = {
    id: 'c1411f13f1e045959895a6b8686cd2df',
    name: 'Development Inc LTG1',
    threatLevel: 10,
    licenses: [
      {
        id: '33312eb53be24f199d55acee1db74621',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SAP-TOU',
      },
      {
        id: '71b5f83927944b56bfc6ab203232cf8b',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SATA',
      },
      {
        id: '2f315618a15243518547a8657dce17b8',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SautinSoft-Document-.Net-LA',
      },
      {
        id: 'e210c1aa054b4d0ca8f87bd667a4fdf4',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SautinSoft-Excel-to-PDF-.Net-LA',
      },
    ],
  };
  let nextLTG = {
    id: 'fd043f475d644ee69fe0ddc971b75f90',
    name: 'Remove Me 9',
    threatLevel: 9,
    licenses: [
      {
        id: '809aa71f5d684a318010e7efdbc4a0a8',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'fd043f475d644ee69fe0ddc971b75f90',
        licenseId: 'ACM-JTF-SLA',
      },
      {
        id: '5bab376ff9fa4a01ae79787c2f3212d3',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'fd043f475d644ee69fe0ddc971b75f90',
        licenseId: 'Accruent-TOU',
      },
      {
        id: '7a24cf88058b4b21bf40e8bfdd758638',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'fd043f475d644ee69fe0ddc971b75f90',
        licenseId: 'Accusoft-SLA',
      },
    ],
  };
  let dirtyLTG = {
    id: 'c1411f13f1e045959895a6b8686cd2df',
    name: 'Development Inc LTG1',
    threatLevel: 10,
    licenses: [
      {
        id: '33312eb53be24f199d55acee1db74621',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SAP-TOU',
      },
      {
        id: '71b5f83927944b56bfc6ab203232cf8b',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SATA',
      },
      {
        id: '2f315618a15243518547a8657dce17b8',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SautinSoft-Document-.Net-LA',
      },
      {
        id: 'e210c1aa054b4d0ca8f87bd667a4fdf4',
        ownerId: '48951e9ed78946a6a5308420b5b533a8',
        licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        licenseId: 'SautinSoft-Excel-to-PDF-.Net-LA',
      },
    ],
    pickedLicenses: [
      {
        id: '0BSD',
        shortDisplayName: '0BSD',
        longDisplayName: 'BSD Zero Clause License',
        fullDisplayName: '(0BSD) BSD Zero Clause License',
        picked: false,
        index: 0,
      },
      {
        id: '10tec-Company-License-Agreement',
        shortDisplayName: '10tec-Company-License-Agreement',
        longDisplayName: '10tec Company License Agreement',
        fullDisplayName: '(10tec-Company-License-Agreement) 10tec Company License Agreement',
        picked: false,
        index: 1,
      },
      {
        id: '2KSYS-EULA',
        shortDisplayName: '2KSYS-EULA',
        longDisplayName: '2KSYS End User License Agreement',
        fullDisplayName: '(2KSYS-EULA) 2KSYS End User License Agreement',
        picked: false,
        index: 2,
      },
      {
        id: 'AAL',
        shortDisplayName: 'AAL',
        longDisplayName: 'Attribution Assurance License',
        fullDisplayName: '(AAL) Attribution Assurance License',
        picked: false,
        index: 3,
      },
    ],
  };
  let availableLicense = [
    {
      id: '0BSD',
      shortDisplayName: '0BSD',
      longDisplayName: 'BSD Zero Clause License',
      fullDisplayName: '(0BSD) BSD Zero Clause License',
      picked: false,
      index: 0,
    },
    {
      id: '10tec-Company-License-Agreement',
      shortDisplayName: '10tec-Company-License-Agreement',
      longDisplayName: '10tec Company License Agreement',
      fullDisplayName: '(10tec-Company-License-Agreement) 10tec Company License Agreement',
      picked: false,
      index: 1,
    },
  ];
  let applicableLTGs = [
    {
      ownerId: '48951e9ed78946a6a5308420b5b533a8',
      ownerName: 'Development Inc',
      ownerType: 'organization',
      licenseThreatGroups: [
        {
          id: 'c1411f13f1e045959895a6b8686cd2df',
          name: 'Development Inc LTG1',
          threatLevel: 10,
          licenses: [
            {
              id: '33312eb53be24f199d55acee1db74621',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SAP-TOU',
            },
            {
              id: '71b5f83927944b56bfc6ab203232cf8b',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SATA',
            },
            {
              id: '2f315618a15243518547a8657dce17b8',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SautinSoft-Document-.Net-LA',
            },
            {
              id: 'e210c1aa054b4d0ca8f87bd667a4fdf4',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
              licenseId: 'SautinSoft-Excel-to-PDF-.Net-LA',
            },
          ],
        },
        {
          id: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          name: 'Development Inc LTG2',
          threatLevel: 2,
          licenses: [
            {
              id: 'abb7a802aaa1473e8479f0cf2867d087',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZPL-1.0',
            },
            {
              id: '016877be5fba4b109d1253eee640be7d',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZPL-1.1',
            },
            {
              id: 'e1813eaa7d6c433f818123a8e74c25b8',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZPL-2.0',
            },
            {
              id: '63f7276c682743be86f9a2154ba85d89',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZPL-2.1',
            },
            {
              id: 'e9e5af1604db46fb98c88825ff9ceb31',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZPL-UNSPECIFIED',
            },
            {
              id: 'f144e4e7fcc4454686fc45544ff7ada1',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'ZZZ-Projects-LA',
            },
            {
              id: 'cd65c8e98fd84b85a497caea11429fb4',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
              licenseId: 'Zuora-Inc-DTLA',
            },
          ],
        },
        {
          id: '1ef15f07b23c4a0ab524060e44ca5827',
          name: 'Development Inc LTG3',
          threatLevel: 8,
          licenses: [
            {
              id: '20f2378d232e40ac8f1577d267de0175',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
              licenseId: 'ACM-JTF-SLA',
            },
            {
              id: '78e4e93c1fd642c9b94436900c309553',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
              licenseId: 'Accruent-TOU',
            },
            {
              id: 'b4b91adfa64042a7b2bd60f8abc98b0a',
              ownerId: '48951e9ed78946a6a5308420b5b533a8',
              licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
              licenseId: 'Accusoft-SLA',
            },
          ],
        },
      ],
      inherited: false,
    },
    {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      licenseThreatGroups: [
        {
          id: '4da8a978f07249289a690a47898eaa68',
          name: 'Banned',
          threatLevel: 10,
          licenses: [
            {
              id: '600e9d43a69947d39da37d14adbc61a3',
              ownerId: 'ROOT_ORGANIZATION_ID',
              licenseThreatGroupId: '4da8a978f07249289a690a47898eaa68',
              licenseId: 'AGPL-1.0',
            },
            {
              id: '1dc24723460e457487224cadfaec00b0',
              ownerId: 'ROOT_ORGANIZATION_ID',
              licenseThreatGroupId: '4da8a978f07249289a690a47898eaa68',
              licenseId: 'AGPL-1.0-or-later',
            },
            {
              id: '33b5b0eb6560499ba7db85491df48f74',
              ownerId: 'ROOT_ORGANIZATION_ID',
              licenseThreatGroupId: '4da8a978f07249289a690a47898eaa68',
              licenseId: 'AGPL-2.0',
            },
          ],
        },
      ],
      inherited: true,
    },
  ];
  let siblings = [
    {
      id: 'c1411f13f1e045959895a6b8686cd2df',
      name: 'Development Inc LTG1',
      threatLevel: 10,
      licenses: [
        {
          id: '33312eb53be24f199d55acee1db74621',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
          licenseId: 'SAP-TOU',
        },
        {
          id: '71b5f83927944b56bfc6ab203232cf8b',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
          licenseId: 'SATA',
        },
        {
          id: '2f315618a15243518547a8657dce17b8',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
          licenseId: 'SautinSoft-Document-.Net-LA',
        },
        {
          id: 'e210c1aa054b4d0ca8f87bd667a4fdf4',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
          licenseId: 'SautinSoft-Excel-to-PDF-.Net-LA',
        },
      ],
    },
    {
      id: '6e3bf2bf8d1449c181d93cf1af3a93a0',
      name: 'Development Inc LTG2',
      threatLevel: 2,
      licenses: [
        {
          id: 'abb7a802aaa1473e8479f0cf2867d087',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZPL-1.0',
        },
        {
          id: '016877be5fba4b109d1253eee640be7d',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZPL-1.1',
        },
        {
          id: 'e1813eaa7d6c433f818123a8e74c25b8',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZPL-2.0',
        },
        {
          id: '63f7276c682743be86f9a2154ba85d89',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZPL-2.1',
        },
        {
          id: 'e9e5af1604db46fb98c88825ff9ceb31',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZPL-UNSPECIFIED',
        },
        {
          id: 'f144e4e7fcc4454686fc45544ff7ada1',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'ZZZ-Projects-LA',
        },
        {
          id: 'cd65c8e98fd84b85a497caea11429fb4',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '6e3bf2bf8d1449c181d93cf1af3a93a0',
          licenseId: 'Zuora-Inc-DTLA',
        },
      ],
    },
    {
      id: '1ef15f07b23c4a0ab524060e44ca5827',
      name: 'Development Inc LTG3',
      threatLevel: 8,
      licenses: [
        {
          id: '20f2378d232e40ac8f1577d267de0175',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
          licenseId: 'ACM-JTF-SLA',
        },
        {
          id: '78e4e93c1fd642c9b94436900c309553',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
          licenseId: 'Accruent-TOU',
        },
        {
          id: 'b4b91adfa64042a7b2bd60f8abc98b0a',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: '1ef15f07b23c4a0ab524060e44ca5827',
          licenseId: 'Accusoft-SLA',
        },
      ],
    },
    {
      id: 'b548512a7a924d80b6c53b98b427be59',
      name: 'Intento',
      threatLevel: 10,
      licenses: [
        {
          id: '300b1e6cba0b42688b386cfcd0a8db99',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'b548512a7a924d80b6c53b98b427be59',
          licenseId: 'Abstyles',
        },
        {
          id: '01770300bf4e4b03b6f9df75bc2ba1f7',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'b548512a7a924d80b6c53b98b427be59',
          licenseId: 'AcceleratXR-EULA',
        },
        {
          id: 'c51e2e98abe546c18008aa6d4545f26d',
          ownerId: '48951e9ed78946a6a5308420b5b533a8',
          licenseThreatGroupId: 'b548512a7a924d80b6c53b98b427be59',
          licenseId: 'Accruent-TOU',
        },
      ],
    },
  ];

  let mockState = {
    router: {
      currentState: {
        name: 'management.edit.organization.create-license-threat-group',
        url: '/licenseThreatGroup',
        data: {
          title: 'Organization License Threat Group',
        },
      },
      currentParams: {
        '#': null,
        organizationId: '48951e9ed78946a6a5308420b5b533a8',
      },
    },
    orgsAndPolicies: {
      root: {
        ownerName: 'owner',
      },
      licenseThreatGroups: {
        loadError: null,
        submitError: null,
        errorState: null,
        deleting: false,
        success: null,
        loading: false,
        isDirty: false,
        applicableLicenseThreatGroups: applicableLTGs,
        availableLicenses: availableLicense,
        currentLicenseThreatGroup: currentLTG,
        nextLicenseThreatGroup: nextLTG,
        dirtyLTG: dirtyLTG,
        siblings: siblings,
      },
    },
  };

  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(($rootScope, $injector, $controller) => {
    scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');

    vm = $controller('LicenseThreatGroupTileController', {
      $scope: scope,
    });
    scope.vm = vm;
  }));

  describe('mapStateToThis', () => {
    it('sets ownerName, loading, error and applicableLicenseGroups', () => {
      const output = mapStateToThis(mockState);

      expect(output.ownerName).toBe('owner');
      expect(output.loading).toBeFalse();
      expect(output.error).toBeNull();
      expect(output.applicableLicenseGroups).toEqual(
        mockState.orgsAndPolicies.licenseThreatGroups.applicableLicenseThreatGroups
      );
    });
  });

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadApplicableLicenseGroups', () => {
      expect(vm.loadApplicableLicenseGroups).toHaveBeenCalled();
    });
  });

  describe('$destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('broadcast events', () => {
    it('calls loadApplicableLicenseGroups on policy.imported event', () => {
      expect(vm.loadApplicableLicenseGroups).toHaveBeenCalledTimes(1);
      scope.$emit('policy.imported');
      expect(vm.loadApplicableLicenseGroups).toHaveBeenCalledTimes(2);
    });

    it('calls loadApplicableLicenseGroups on broadcasted owner summary reload event', () => {
      expect(vm.loadApplicableLicenseGroups).toHaveBeenCalledTimes(1);
      scope.$emit(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);
      expect(vm.loadApplicableLicenseGroups).toHaveBeenCalledTimes(2);
    });

    it('calls updateOwnerName on broadcasted policy.imported event', () => {
      expect(vm.updateOwnerName).not.toHaveBeenCalled();
      scope.$emit(EventNameConstant.OWNER_UPDATED, { name: 'Bob' });
      expect(vm.updateOwnerName).toHaveBeenCalledTimes(1);
    });
  });

  describe('edit license threat group', () => {
    it('calls goToEditLTG if LTG can be edited', () => {
      expect(vm.goToEditLTG).not.toHaveBeenCalled();
      vm.editLTG('ltgId', false);
      expect(vm.goToEditLTG).toHaveBeenCalledTimes(1);
    });

    it('does not call goToEditLTG if label can not be edited', () => {
      expect(vm.goToEditLTG).not.toHaveBeenCalled();
      vm.editLTG('ltgId', true);
      expect(vm.goToEditLTG).not.toHaveBeenCalled();
    });
  });
});
