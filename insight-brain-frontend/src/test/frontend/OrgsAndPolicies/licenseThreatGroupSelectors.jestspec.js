/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectIsLoading,
  selectLicenseThreatGroupLoadError,
  selectLicenseThreatGroupSubmitError,
  selectLicenseThreatGroupIsDirty,
  selectLicenseThreatGroupIsEditMode,
  selectLicenseThreatGroupId,
  selectCurrentLicenseThreatGroup,
  selectNextLicenseThreatGroup,
  selectApplicableLicenseThreatGroup,
  selectLicenseThreatGroupSiblings,
  selectAvailableLicenses,
  selectDirtyLicenseThreatGroup,
  selectSubmitMaskState,
  selectDeleteMaskState,
  selectDeleteError,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';

describe('orgsAndPoliciesLabelsSelectors', () => {
  let mockState;

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
      fullDisplayName: '(0BSD) BSD Zero Clause License',
    },
    {
      id: '10tec-Company-License-Agreement',
      fullDisplayName: '(10tec-Company-License-Agreement) 10tec Company License Agreement',
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

  beforeEach(() => {
    mockState = {
      router: {
        currentParams: {
          licenseThreatGroupId: 'c1411f13f1e045959895a6b8686cd2df',
        },
      },
      orgsAndPolicies: {
        licenseThreatGroups: {
          loadError: 'loadError',
          submitError: 'submitError',
          deleteError: 'deleteError',
          deleteMaskState: null,
          submitMaskState: false,
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
  });

  describe('selectIsLoading', () => {
    it('returns true if loading', () => {
      mockState.orgsAndPolicies.licenseThreatGroups.loading = true;
      expect(selectIsLoading(mockState)).toBe(true);
    });

    it('returns false if not loading', () => {
      expect(selectIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectDeleteError', () => {
    it('returns deleteError', () => {
      expect(selectDeleteError(mockState)).toBe('deleteError');
    });
  });

  describe('selectDeleteMaskState', () => {
    it('returns deleteMaskState', () => {
      expect(selectDeleteMaskState(mockState)).toBeNull();
    });
  });

  describe('selectSubmitMaskState', () => {
    it('returns submitMaskState', () => {
      expect(selectSubmitMaskState(mockState)).toBe(false);
    });
  });

  describe('selectLicenseThreatGroupLoadError', () => {
    it('returns loadError', () => {
      expect(selectLicenseThreatGroupLoadError(mockState)).toBe('loadError');
    });
  });

  describe('selectLicenseThreatGroupSubmitError', () => {
    it('returns submitError', () => {
      expect(selectLicenseThreatGroupSubmitError(mockState)).toBe('submitError');
    });
  });

  describe('selectLicenseThreatGroupIsDirty', () => {
    it('returns isDirty', () => {
      expect(selectLicenseThreatGroupIsDirty(mockState)).toBe(false);
    });
  });

  describe('selectLicenseThreatGroupIsEditMode', () => {
    it('returns true if in edit mode', () => {
      expect(selectLicenseThreatGroupIsEditMode(mockState)).toBe(true);
    });

    it('returns false if not in edit mode', () => {
      mockState.router.currentParams = {};
      expect(selectLicenseThreatGroupIsEditMode(mockState)).toBe(false);
    });
  });

  describe('selectLicenseThreatGroupId', () => {
    it('returns id', () => {
      expect(selectLicenseThreatGroupId(mockState)).toBe('c1411f13f1e045959895a6b8686cd2df');
    });
  });

  describe('selectCurrentLicenseThreatGroup', () => {
    it('returns current LTG', () => {
      const result = selectCurrentLicenseThreatGroup(mockState);
      expect(result).toEqual(currentLTG);
    });
  });

  describe('selectNextLicenseThreatGroup', () => {
    it('returns next LTG', () => {
      const result = selectNextLicenseThreatGroup(mockState);
      expect(result).not.toBeNull();
      expect(result).toEqual(nextLTG);
    });
  });

  describe('selectApplicableLicenseThreatGroup', () => {
    it('returns applicable LTG', () => {
      const result = selectApplicableLicenseThreatGroup(mockState);
      expect(result).not.toBeNull();
      expect(result).toEqual(applicableLTGs);
    });
  });

  describe('selectLicenseThreatGroupSiblings', () => {
    it('returns all siblings', () => {
      const result = selectLicenseThreatGroupSiblings(mockState);
      expect(result).not.toBeNull();
      expect(result).toEqual(siblings);
    });
  });

  describe('selectAvailableLicenses', () => {
    it('returns all license', () => {
      const result = selectAvailableLicenses(mockState);
      expect(result).not.toBeNull();
      expect(result).toEqual(availableLicense);
    });
  });

  describe('selectDirtyLicenseThreatGroup', () => {
    it('returns dirty ltg', () => {
      const result = selectDirtyLicenseThreatGroup(mockState);
      expect(result).not.toBeNull();
      expect(result).toEqual(dirtyLTG);
    });
  });
});
