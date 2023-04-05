/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const rootOrganizationLtgs = {
  ownerId: 'ROOT_ORGANIZATION_ID',
  ownerName: 'Root Organization',
  ownerType: 'organization',
  inherited: true,
  licenseThreatGroups: [
    {
      id: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
      name: 'New LTG',
      threatLevel: 7,
      licenses: [],
    },
    {
      id: '876e9a143d56451489adda40a2e5bafa',
      name: 'New LTG 2',
      threatLevel: 2,
      licenses: [],
    },
    {
      id: '03b9f5fa8c05429f9be3323d5dcd8017',
      name: 'New LTG Group Test 2',
      threatLevel: 1,
      licenses: [],
    },
    {
      id: '5aaba4215ee043d1b67e616f9be65d86',
      name: 'New LTG Level 3',
      threatLevel: 3,
      licenses: [],
    },
    {
      id: '6e6e32098eed4376b8674eaeb53a69cc',
      name: 'None Thread LTG Test',
      threatLevel: 0,
      licenses: [],
    },
    {
      id: 'd66ade37b0d14e698816e0cd6c582af6',
      name: 'Testing all the names 12341234',
      threatLevel: 10,
      licenses: [],
    },
    {
      id: 'ce2b5dd87e6f4d15a6df4754096cf7dd',
      name: 'This is new',
      threatLevel: 9,
      licenses: [],
    },
    {
      id: 'da680cc6e63541c0991c38f616b09212',
      name: 'aaaaaaaa',
      threatLevel: 5,
      licenses: [],
    },
    {
      id: '0eb52c770a1d495db2942fcc3009a0a9',
      name: 'lkisjha hjsufb hr hf',
      threatLevel: 5,
      licenses: [],
    },
  ],
};

const organizationWithoutLtgs = {
  ownerId: 'c01ec9400ccc406caecdf59852395979',
  ownerName: 'API Organization Test 3',
  ownerType: 'organization',
  licenseThreatGroups: [],
};

export const rootOrganizationLtgsByOwnerPayload = {
  ownerId: 'ROOT_ORGANIZATION_ID',
  ownerName: 'ROOT_ORGANIZATION',
  ownerType: 'organization',
  ltgs: {
    licenseThreatGroupsByOwner: [
      {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
            name: 'New LTG',
            threatLevel: 7,
            licenses: [],
          },
          {
            id: '876e9a143d56451489adda40a2e5bafa',
            name: 'New LTG 2',
            threatLevel: 2,
            licenses: [],
          },
          {
            id: '03b9f5fa8c05429f9be3323d5dcd8017',
            name: 'New LTG Group Test 2',
            threatLevel: 1,
            licenses: [],
          },
          {
            id: '5aaba4215ee043d1b67e616f9be65d86',
            name: 'New LTG Level 3',
            threatLevel: 3,
            licenses: [],
          },
          {
            id: '6e6e32098eed4376b8674eaeb53a69cc',
            name: 'None Thread LTG Test',
            threatLevel: 0,
            licenses: [],
          },
          {
            id: 'd66ade37b0d14e698816e0cd6c582af6',
            name: 'Testing all the names 12341234',
            threatLevel: 10,
            licenses: [],
          },
          {
            id: 'ce2b5dd87e6f4d15a6df4754096cf7dd',
            name: 'This is new',
            threatLevel: 9,
            licenses: [],
          },
          {
            id: 'da680cc6e63541c0991c38f616b09212',
            name: 'aaaaaaaa',
            threatLevel: 5,
            licenses: [],
          },
          {
            id: '0eb52c770a1d495db2942fcc3009a0a9',
            name: 'lkisjha hjsufb hr hf',
            threatLevel: 5,
            licenses: [],
          },
        ],
      },
    ],
  },
};

export const organizationWithoutLtgsByOwnerPayload = {
  ownerId: 'c01ec9400ccc406caecdf59852395979',
  ownerName: 'API Organization Test 3',
  ownerType: 'organization',
  ltgs: {
    licenseThreatGroupsByOwner: [organizationWithoutLtgs, rootOrganizationLtgs],
  },
};

export const organizationWithMultipleLtgsByOwnerPayload = {
  ownerId: 'c01ec9400ccc406caecdf59852395979',
  ownerName: 'API Organization Test 3',
  ownerType: 'organization',
  ltgs: {
    licenseThreatGroupsByOwner: [
      {
        ownerId: 'c01ec9400ccc406caecdf59852395979',
        ownerName: 'API Organization Test 3',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: '96b97b20eca2450283146990160a3534',
            name: 'ltg3.1',
            threatLevel: 1,
            licenses: [],
          },
        ],
      },
      {
        ownerId: '1c431c69fa6c400088b9470ee9d1514e',
        ownerName: 'API Organization Test 2',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'b0c3a4c7eaa74f398c98e4f037035d94',
            name: 'LTG 2',
            threatLevel: 1,
            licenses: [],
          },
          {
            id: 'bbabd15004814321aabd9eb4961614b0',
            name: 'LTG 2.1',
            threatLevel: 0,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'adbf8ae5db8a421881f0c2faf20df9ba',
        ownerName: 'API Organization Test',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'eb85ded649c74d418c28f939a99e604f',
            name: 'TLG 1',
            threatLevel: 5,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'b2bfb6ac354944ba843610cc67596a62',
        ownerName: 'New Organization',
        ownerType: 'organization',
        licenseThreatGroups: [],
      },
      rootOrganizationLtgs,
    ],
  },
};

export const applicationWithoutLtgsByOwnerPayload = {
  ownerId: '1b9c6bb18fbc4824864de00567990b6d',
  ownerName: 'Final Org 1',
  ownerType: 'application',
  ltgs: {
    licenseThreatGroupsByOwner: [
      {
        ownerId: '1b9c6bb18fbc4824864de00567990b6d',
        ownerName: 'Final Org 1',
        ownerType: 'application',
        licenseThreatGroups: [],
      },
      {
        ownerId: 'c01ec9400ccc406caecdf59852395979',
        ownerName: 'API Organization Test 3',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: '96b97b20eca2450283146990160a3534',
            name: 'ltg3.1',
            threatLevel: 1,
            licenses: [],
          },
        ],
      },
      {
        ownerId: '1c431c69fa6c400088b9470ee9d1514e',
        ownerName: 'API Organization Test 2',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'b0c3a4c7eaa74f398c98e4f037035d94',
            name: 'LTG 2',
            threatLevel: 1,
            licenses: [],
          },
          {
            id: 'bbabd15004814321aabd9eb4961614b0',
            name: 'LTG 2.1',
            threatLevel: 0,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'adbf8ae5db8a421881f0c2faf20df9ba',
        ownerName: 'API Organization Test',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'eb85ded649c74d418c28f939a99e604f',
            name: 'TLG 1',
            threatLevel: 5,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'b2bfb6ac354944ba843610cc67596a62',
        ownerName: 'New Organization',
        ownerType: 'organization',
        licenseThreatGroups: [],
      },
      rootOrganizationLtgs,
    ],
  },
};

export const applicationWithLtgsByOwnerPayload = {
  ownerId: '1b9c6bb18fbc4824864de00567990b6d',
  ownerName: 'Final Org 1',
  ownerType: 'application',
  ltgs: {
    licenseThreatGroupsByOwner: [
      {
        ownerId: '1b9c6bb18fbc4824864de00567990b6d',
        ownerName: 'Final Org 1',
        ownerType: 'application',
        licenseThreatGroups: [
          {
            id: '96b97b09ab36100283146990160a3534',
            name: 'application ltg.1',
            threatLevel: 1,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'c01ec9400ccc406caecdf59852395979',
        ownerName: 'API Organization Test 3',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: '96b97b20eca2450283146990160a3534',
            name: 'ltg3.1',
            threatLevel: 1,
            licenses: [],
          },
        ],
      },
      {
        ownerId: '1c431c69fa6c400088b9470ee9d1514e',
        ownerName: 'API Organization Test 2',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'b0c3a4c7eaa74f398c98e4f037035d94',
            name: 'LTG 2',
            threatLevel: 1,
            licenses: [],
          },
          {
            id: 'bbabd15004814321aabd9eb4961614b0',
            name: 'LTG 2.1',
            threatLevel: 0,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'adbf8ae5db8a421881f0c2faf20df9ba',
        ownerName: 'API Organization Test',
        ownerType: 'organization',
        licenseThreatGroups: [
          {
            id: 'eb85ded649c74d418c28f939a99e604f',
            name: 'TLG 1',
            threatLevel: 5,
            licenses: [],
          },
        ],
      },
      {
        ownerId: 'b2bfb6ac354944ba843610cc67596a62',
        ownerName: 'New Organization',
        ownerType: 'organization',
        licenseThreatGroups: [],
      },
      rootOrganizationLtgs,
    ],
  },
};

export const nLevelOrgWithInheritedLTGs = [
  {
    ownerId: '1c431c69fa6c400088b9470ee9d1514e',
    ownerName: 'API Organization Test 2',
    ownerType: 'organization',
    inherited: false,
    licenseThreatGroups: [
      {
        id: 'b0c3a4c7eaa74f398c98e4f037035d94',
        name: 'LTG 2',
        threatLevel: 0,
        licenses: [],
      },
      {
        id: 'bbabd15004814321aabd9eb4961614b0',
        name: 'LTG 2.1',
        threatLevel: 1,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'b2bfb6ac354944ba843610cc67596a62',
    ownerName: 'New Organization',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [],
  },
  {
    ownerId: 'ROOT_ORGANIZATION_ID',
    ownerName: 'Root Organization',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
        name: 'New LTG',
        threatLevel: 7,
        licenses: [],
      },
    ],
  },
];

export const nLevelAppWithLTGs = [
  {
    ownerId: '1b9c6bb18fbc4824864de00567990b6d',
    ownerName: 'Final Org 1',
    ownerType: 'application',
    inherited: false,
    licenseThreatGroups: [
      {
        id: '96b97b09ab36100283146990160a3534',
        name: 'application ltg.1',
        threatLevel: 1,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'c01ec9400ccc406caecdf59852395979',
    ownerName: 'API Organization Test 3',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: '96b97b20eca2450283146990160a3534',
        name: 'ltg3.1',
        threatLevel: 1,
        licenses: [],
      },
    ],
  },
  {
    ownerId: '1c431c69fa6c400088b9470ee9d1514e',
    ownerName: 'API Organization Test 2',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: 'b0c3a4c7eaa74f398c98e4f037035d94',
        name: 'LTG 2',
        threatLevel: 1,
        licenses: [],
      },
      {
        id: 'bbabd15004814321aabd9eb4961614b0',
        name: 'LTG 2.1',
        threatLevel: 0,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'adbf8ae5db8a421881f0c2faf20df9ba',
    ownerName: 'API Organization Test',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: 'eb85ded649c74d418c28f939a99e604f',
        name: 'TLG 1',
        threatLevel: 5,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'b2bfb6ac354944ba843610cc67596a62',
    ownerName: 'New Organization',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [],
  },
];

export const nLevelAppWithNoLTGs = [
  {
    ownerId: '1b9c6bb18fbc4824864de00567990b6d',
    ownerName: 'Final Org 1',
    ownerType: 'application',
    licenseThreatGroups: [],
    inherited: false,
  },
  {
    ownerId: 'c01ec9400ccc406caecdf59852395979',
    ownerName: 'API Organization Test 3',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: '96b97b20eca2450283146990160a3534',
        name: 'ltg3.1',
        threatLevel: 1,
        licenses: [],
      },
    ],
  },
  {
    ownerId: '1c431c69fa6c400088b9470ee9d1514e',
    ownerName: 'API Organization Test 2',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: 'b0c3a4c7eaa74f398c98e4f037035d94',
        name: 'LTG 2',
        threatLevel: 1,
        licenses: [],
      },
      {
        id: 'bbabd15004814321aabd9eb4961614b0',
        name: 'LTG 2.1',
        threatLevel: 0,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'adbf8ae5db8a421881f0c2faf20df9ba',
    ownerName: 'API Organization Test',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [
      {
        id: 'eb85ded649c74d418c28f939a99e604f',
        name: 'TLG 1',
        threatLevel: 5,
        licenses: [],
      },
    ],
  },
  {
    ownerId: 'b2bfb6ac354944ba843610cc67596a62',
    ownerName: 'New Organization',
    ownerType: 'organization',
    inherited: true,
    licenseThreatGroups: [],
  },
];

export const nLevelFormattedLTGsExpected = [
  {
    emptyMessage: 'No API Organization Test 2 threat groups defined',
    headerTitle: 'Local to API Organization Test 2',
    inherited: false,
    sortedThreatGroups: [
      {
        id: 'b0c3a4c7eaa74f398c98e4f037035d94',
        name: 'LTG 2',
        threatLevel: 0,
        licenses: [],
        inherited: false,
      },
      {
        id: 'bbabd15004814321aabd9eb4961614b0',
        name: 'LTG 2.1',
        threatLevel: 1,
        licenses: [],
        inherited: false,
      },
    ],
  },
  {
    emptyMessage: 'No Root Organization threat groups defined',
    headerTitle: 'Local to Root Organization',
    inherited: true,
    sortedThreatGroups: [
      {
        id: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
        name: 'New LTG',
        threatLevel: 7,
        licenses: [],
        inherited: true,
      },
    ],
  },
];
