/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const licenseState = {
  component: {
    licenseLegalData: {
      effectiveLicenses: ['GPL', 'GPL-2', 'GPL or GPL-2'],
      declaredLicenses: ['GPL'],
      observedLicenses: ['GPL-2'],
      effectiveLicenseStatus: 'Selected',
    },
  },
  hash: 'fooHash',
  componentIdentifier: 'fooComponentIdentifier',
  ownerType: 'organization',
  ownerId: 'org',
  licenseLegalMetadata: [
    {
      licenseId: 'GPL',
      licenseName: 'GPL',
      isMulti: false,
      obligations: [
        {
          name: 'Inclusion of License',
          obligationTexts: ['distribute a copy of this License along with the Library'],
        },
      ],
      threatGroup: { name: 'Weak', threatLevel: 2 },
      licenseText:
        'GPL long text here also with obligations to distribute a copy of this License along with the Library' +
        'and copyright this whenever it is convenient',
    },
    {
      licenseId: 'GPL-2',
      licenseName: 'GPL-2',
      isMulti: false,
      obligations: [
        {
          name: 'Inclusion of License',
          obligationTexts: ['distribute a copy of this License along with the Library'],
        },
        {
          name: 'Inclusion of Copyright',
          obligationTexts: ['copyright this'],
        },
      ],
      threatGroup: { name: 'Weak', threatLevel: 2 },
      licenseText:
        'GPL 2.0 long text here including obligations to distribute a copy of this License along with the Library' +
        'and copyright this whenever it is convenient',
    },
    {
      licenseId: 'GPL or GPL-2',
      licenseName: 'GPL or GPL-2',
      isMulti: true,
      obligations: [],
      threatGroup: null,
      licenseText: null,
    },
  ],
  componentLicenseDetails: {
    licenseIndex: '1',
    selectedLicense: 'GPL-2',
  },
  $state: {
    get: () => '',
    href: () => '',
  },
};
