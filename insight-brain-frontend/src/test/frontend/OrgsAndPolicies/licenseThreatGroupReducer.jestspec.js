/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';

const newLTG = {
  id: '4db724d5c1d14784aa6ca600773f877f',
  ownerId: '48951e9ed78946a6a5308420b5b533a8',
  name: 'saving4',
  nameLowercaseNoWhitespace: 'saving4',
  threatLevel: 9,
  licenseIds: ['0BSD', '10tec-Company-License-Agreement'],
};

const updatedLTG = {
  id: '4db724d5c1d14784aa6ca600773f877f',
  ownerId: '48951e9ed78946a6a5308420b5b533a8',
  name: 'updatedName',
  nameLowercaseNoWhitespace: 'updatedName',
  threatLevel: 7,
  licenseIds: ['0BSD'],
};

const availableLicenses = [
  {
    id: '0BSD',
    fullDisplayName: '(0BSD) BSD Zero Clause License',
  },
  {
    id: '10tec-Company-License-Agreement',
    fullDisplayName: '(10tec-Company-License-Agreement) 10tec Company License Agreement',
  },
];

describe('licenseThreatGroup reducer', () => {
  describe('licenseThreatGroup/setLicenseThreatGroupName', () => {
    it('sets name to dirtyLTG, validationError and isDirty property', () => {
      const state = Object.freeze({
        siblings: [],
        dirtyLTG: {
          id: null,
          name: {
            trimmedValue: '',
            value: '',
            validationError: null,
          },
          threatLevel: 5,
          licenses: null,
        },
        currentLicenseThreatGroup: null,
        isDirty: false,
        validationError: null,
      });

      const { isDirty, dirtyLTG, validationError } = reducer(state, {
        type: 'licenseThreatGroup/setLicenseThreatGroupName',
        payload: 'LTG Name',
      });

      expect(isDirty).toBe(true);
      expect(dirtyLTG.name.trimmedValue).toBe('LTG Name');
      expect(validationError).toBeNull();
    });

    it('sets validationError if name fails validation', () => {
      const state = Object.freeze({
        siblings: [],
        dirtyLTG: {
          id: null,
          name: {
            trimmedValue: 'name',
            value: 'name',
            validationError: null,
          },
          threatLevel: 5,
          licenses: null,
        },
        currentLicenseThreatGroup: null,
        isDirty: false,
        validationError: null,
      });

      const { dirtyLTG, validationError } = reducer(state, {
        type: 'licenseThreatGroup/setLicenseThreatGroupName',
        payload: '',
      });

      expect(dirtyLTG.name.trimmedValue).toBe('');
      expect(validationError).toBe('Unable to save: fields with invalid or missing data');
    });

    it('sets validationError if name fails duplication validation', () => {
      const state = Object.freeze({
        siblings: [
          {
            id: '201',
            name: 'duplicate',
          },
        ],
        dirtyLTG: {
          id: '200',
          name: {
            trimmedValue: 'name',
            value: 'name',
            validationError: null,
          },
          threatLevel: 5,
          licenses: null,
        },
        currentLicenseThreatGroup: null,
        isDirty: false,
        validationError: null,
      });

      const { dirtyLTG, validationError } = reducer(state, {
        type: 'licenseThreatGroup/setLicenseThreatGroupName',
        payload: 'duplicate',
      });

      expect(dirtyLTG.name.trimmedValue).toBe('duplicate');
      expect(validationError).toBe('Unable to save: fields with invalid or missing data');
    });
  });

  describe('licenseThreatGroup/setLicenseThreatGroupThreatLevel', () => {
    it('sets threat level to dirtyLTG and isDirty property', () => {
      const state = Object.freeze({
        dirtyLTG: {
          id: null,
          name: {
            trimmedValue: 'name',
            value: 'name',
          },
          threatLevel: 5,
          licenses: null,
          licenseIds: [],
        },
        currentLicenseThreatGroup: null,
        isDirty: false,
      });

      const { isDirty, dirtyLTG } = reducer(state, {
        type: 'licenseThreatGroup/setLicenseThreatGroupThreatLevel',
        payload: 10,
      });

      expect(isDirty).toBe(true);
      expect(dirtyLTG.threatLevel).toBe(10);
    });
  });

  describe('licenseThreatGroup/setPickedLicenses', () => {
    it('sets picked licenses to dirtyLTG and isDirty property', () => {
      const state = Object.freeze({
        dirtyLTG: {
          id: null,
          name: {
            trimmedValue: 'name',
            value: 'name',
          },
          threatLevel: 5,
          licenseIds: [],
        },
        currentLicenseThreatGroup: null,
        isDirty: false,
      });

      const { isDirty, dirtyLTG } = reducer(state, {
        type: 'licenseThreatGroup/setPickedLicenses',
        payload: ['License 1', 'License 2'],
      });

      expect(isDirty).toBe(true);
      expect(dirtyLTG.licenseIds).toEqual(['License 1', 'License 2']);
    });
  });

  describe('licenseThreatGroup/resetIsDirty', () => {
    it('resets isDirty property', () => {
      const state = Object.freeze({ isDirty: true });

      const { isDirty } = reducer(state, {
        type: 'licenseThreatGroup/resetIsDirty',
      });

      expect(isDirty).toBe(false);
    });
  });

  describe('loadLicenseThreatGroups', () => {
    describe('licenseThreatGroup/loadLicenseThreatGroups/pending', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: false,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroups/pending',
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(true);
      });
    });

    describe('licenseThreatGroup/loadLicenseThreatGroups/fulfilled', () => {
      it('sets loadError property to null, licenseGroup property to what is on payload and loading property to false', () => {
        const state = Object.freeze({
          loading: true,
          loadError: 'error',
          licenseGroup: null,
        });

        const { loading, loadError, licenseGroup } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroups/fulfilled',
          payload: [
            { id: 1, name: 'name1' },
            { id: 2, name: 'name2' },
          ],
        });

        expect(loading).toBe(false);
        expect(loadError).toBeNull();
        expect(licenseGroup).not.toBeNull();
      });
    });

    describe('licenseThreatGroup/loadLicenseThreatGroups/rejected', () => {
      it('sets loadError property to payload and loading property to false', () => {
        const state = Object.freeze({
          loadError: null,
          loading: true,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroups/rejected',
          payload: 'Error',
        });

        expect(loadError).toBe('Error');
        expect(loading).toBe(false);
      });
    });
  });

  describe('loadApplicableLicenseThreatGroups', () => {
    describe('licenseThreatGroup/loadApplicableLicenseThreatGroups/pending', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: false,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadApplicableLicenseThreatGroups/pending',
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(true);
      });
    });

    describe('licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: true,
          applicableLicenseThreatGroups: null,
        });

        const { loadError, loading, applicableLicenseThreatGroups } = reducer(state, {
          type: 'licenseThreatGroup/loadApplicableLicenseThreatGroups/fulfilled',
          payload: {
            licenseThreatGroupsByOwner: [
              {
                ownerId: '48951e9ed78946a6a5308420b5b533a8',
                ownerName: 'Development Inc',
                ownerType: 'organization',
                licenseThreatGroups: [
                  {
                    id: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                    name: '1',
                    threatLevel: 9,
                    licenses: [
                      {
                        id: '4c935956ef6b4089bbf58e477d1bce40',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '0BSD',
                      },
                      {
                        id: '34014f4a25434ffd81aad093743bca62',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '10tec-Company-License-Agreement',
                      },
                      {
                        id: '14d88bfc25864d58b0987a73338e4f99',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '2KSYS-EULA',
                      },
                      {
                        id: 'bc75e14ded3a4abca1d3ab0c48dde223',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: 'AAL',
                      },
                    ],
                  },
                  {
                    id: '78b93efd075443bf91bcd804894ce5ea',
                    name: '111',
                    threatLevel: 10,
                    licenses: [
                      {
                        id: '5daf2005910f4e26a4be037ac89594e4',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'AcceleratXR-EULA',
                      },
                      {
                        id: 'a09b54fde06d41ec9befa59720e636cc',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'Accruent-TOU',
                      },
                      {
                        id: '5a9799a975624d07819fc711bdd8089a',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'Accusoft-SLA',
                      },
                    ],
                  },
                  {
                    id: '091e3dc38b4c47a5a98073a2dce26547',
                    name: 'Remove 8',
                    threatLevel: 8,
                    licenses: [],
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
                    id: '6c69144f6c964638bb8c21704ddc83d0',
                    name: 'Sonatype Informational',
                    threatLevel: 0,
                    licenses: [
                      {
                        id: 'cb4344aec1824ed289b86b8ae953bf31',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: '6c69144f6c964638bb8c21704ddc83d0',
                        licenseId: 'Not-Supported',
                      },
                    ],
                  },
                  {
                    id: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                    name: 'Sonatype Special Licenses',
                    threatLevel: 5,
                    licenses: [
                      {
                        id: '741c242935494362840f6c4050f55125',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'No-Source-License',
                      },
                      {
                        id: '8f956306c1114b508399856f5d20922c',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'No-Sources',
                      },
                      {
                        id: 'a89670f4deae400f95643178721fb6fa',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'Not-Declared',
                      },
                      {
                        id: '3a21fa2fdbc64f38915e47f2cf36bf13',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'UNSPECIFIED',
                      },
                    ],
                  },
                ],
                inherited: true,
              },
            ],
          },
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(false);
        expect(applicableLicenseThreatGroups).not.toBeNull();
      });
    });

    describe('licenseThreatGroup/loadApplicableLicenseThreatGroups/rejected', () => {
      it('sets loadError property to payload and loading property to false', () => {
        const state = Object.freeze({
          loadError: null,
          loading: true,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadApplicableLicenseThreatGroups/rejected',
          payload: 'Error',
        });

        expect(loadError).toBe('Error');
        expect(loading).toBe(false);
      });
    });
  });

  describe('loadLicensesByLicenseThreatGroup', () => {
    describe('licenseThreatGroup/loadLicensesByLicenseThreatGroup/pending', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: false,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicensesByLicenseThreatGroup/pending',
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(true);
      });
    });

    describe('licenseThreatGroup/loadLicensesByLicenseThreatGroup/fulfilled', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: true,
          applicableLicenseThreatGroups: null,
        });

        const { loadError, loading, applicableLicenseThreatGroups } = reducer(state, {
          type: 'licenseThreatGroup/loadLicensesByLicenseThreatGroup/fulfilled',
          payload: {
            licenseThreatGroupsByOwner: [
              {
                ownerId: '48951e9ed78946a6a5308420b5b533a8',
                ownerName: 'Development Inc',
                ownerType: 'organization',
                licenseThreatGroups: [
                  {
                    id: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                    name: '1',
                    threatLevel: 9,
                    licenses: [
                      {
                        id: '4c935956ef6b4089bbf58e477d1bce40',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '0BSD',
                      },
                      {
                        id: '34014f4a25434ffd81aad093743bca62',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '10tec-Company-License-Agreement',
                      },
                      {
                        id: '14d88bfc25864d58b0987a73338e4f99',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: '2KSYS-EULA',
                      },
                      {
                        id: 'bc75e14ded3a4abca1d3ab0c48dde223',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: 'd9e902f36c0e4b38b63f0d2c2c4b4adc',
                        licenseId: 'AAL',
                      },
                    ],
                  },
                  {
                    id: '78b93efd075443bf91bcd804894ce5ea',
                    name: '111',
                    threatLevel: 10,
                    licenses: [
                      {
                        id: '5daf2005910f4e26a4be037ac89594e4',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'AcceleratXR-EULA',
                      },
                      {
                        id: 'a09b54fde06d41ec9befa59720e636cc',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'Accruent-TOU',
                      },
                      {
                        id: '5a9799a975624d07819fc711bdd8089a',
                        ownerId: '48951e9ed78946a6a5308420b5b533a8',
                        licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                        licenseId: 'Accusoft-SLA',
                      },
                    ],
                  },
                  {
                    id: '091e3dc38b4c47a5a98073a2dce26547',
                    name: 'Remove 8',
                    threatLevel: 8,
                    licenses: [],
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
                    id: '6c69144f6c964638bb8c21704ddc83d0',
                    name: 'Sonatype Informational',
                    threatLevel: 0,
                    licenses: [
                      {
                        id: 'cb4344aec1824ed289b86b8ae953bf31',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: '6c69144f6c964638bb8c21704ddc83d0',
                        licenseId: 'Not-Supported',
                      },
                    ],
                  },
                  {
                    id: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                    name: 'Sonatype Special Licenses',
                    threatLevel: 5,
                    licenses: [
                      {
                        id: '741c242935494362840f6c4050f55125',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'No-Source-License',
                      },
                      {
                        id: '8f956306c1114b508399856f5d20922c',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'No-Sources',
                      },
                      {
                        id: 'a89670f4deae400f95643178721fb6fa',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'Not-Declared',
                      },
                      {
                        id: '3a21fa2fdbc64f38915e47f2cf36bf13',
                        ownerId: 'ROOT_ORGANIZATION_ID',
                        licenseThreatGroupId: 'ad9b62d94c7e46ea8f9ee1d636fd25a6',
                        licenseId: 'UNSPECIFIED',
                      },
                    ],
                  },
                ],
                inherited: true,
              },
            ],
          },
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(false);
        expect(applicableLicenseThreatGroups).not.toBeNull();
      });
    });

    describe('licenseThreatGroup/loadLicensesByLicenseThreatGroup/rejected', () => {
      it('sets loadError property to payload and loading property to false', () => {
        const state = Object.freeze({
          loadError: null,
          loading: true,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicensesByLicenseThreatGroup/rejected',
          payload: 'Error',
        });

        expect(loadError).toBe('Error');
        expect(loading).toBe(false);
      });
    });
  });

  describe('loadLicenseThreatGroupEditor', () => {
    describe('licenseThreatGroup/loadLicenseThreatGroupEditor/pending', () => {
      it('sets loadError property to null and loading property to true', () => {
        const state = Object.freeze({
          loadError: 'error',
          loading: false,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroupEditor/pending',
        });

        expect(loadError).toBeNull();
        expect(loading).toBe(true);
      });
    });

    describe('licenseThreatGroup/loadLicenseThreatGroupEditor/fulfilled', () => {
      it('sets loadError property to null, licenseGroup property to what is on payload and loading property to false', () => {
        const state = Object.freeze({
          loading: true,
          loadError: 'error',
          applicableLicenseThreatGroups: null,
        });

        const {
          loading,
          loadError,
          availableLicenses,
          currentLicenseThreatGroup,
          nextLicenseThreatGroup,
          siblings,
          dirtyLTG,
        } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroupEditor/fulfilled',
          payload: {
            availableLicenses: [
              {
                id: '0BSD',
                fullDisplayName: '(0BSD) BSD Zero Clause License',
              },
              {
                id: '10tec-Company-License-Agreement',
                fullDisplayName: '(10tec-Company-License-Agreement) 10tec Company License Agreement',
              },
            ],
            currentLicenseThreatGroup: {
              id: '78b93efd075443bf91bcd804894ce5ea',
              name: '1111',
              threatLevel: 10,
              licenses: [
                {
                  id: 'd84c442831b94ca196b70f7373077fd4',
                  ownerId: '48951e9ed78946a6a5308420b5b533a8',
                  licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                  licenseId: '0BSD',
                },
              ],
            },
            nextLicenseThreatGroup: {
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
              ],
            },
            siblings: [
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
                ],
              },
            ],
            dirtyLTG: {
              id: '78b93efd075443bf91bcd804894ce5ea',
              name: '1111',
              threatLevel: 10,
              licenseIds: ['0BSD', '10tec-Company-License-Agreement'],
              licenses: [
                {
                  id: 'd84c442831b94ca196b70f7373077fd4',
                  ownerId: '48951e9ed78946a6a5308420b5b533a8',
                  licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                  licenseId: '0BSD',
                },
                {
                  id: 'b89fd63c5ccb494496793a30d0a93b74',
                  ownerId: '48951e9ed78946a6a5308420b5b533a8',
                  licenseThreatGroupId: '78b93efd075443bf91bcd804894ce5ea',
                  licenseId: '10tec-Company-License-Agreement',
                },
              ],
            },
          },
        });

        expect(loading).toBe(false);
        expect(loadError).toBeNull();
        expect(availableLicenses).not.toBeNull();
        expect(currentLicenseThreatGroup).not.toBeNull();
        expect(nextLicenseThreatGroup).not.toBeNull();
        expect(siblings).not.toBeNull();
        expect(dirtyLTG).not.toBeNull();
      });
    });

    describe('licenseThreatGroup/loadLicenseThreatGroupEditor/rejected', () => {
      it('sets loadError property to payload and loading property to false', () => {
        const state = Object.freeze({
          loadError: null,
          loading: true,
        });

        const { loadError, loading } = reducer(state, {
          type: 'licenseThreatGroup/loadLicenseThreatGroupEditor/rejected',
          payload: 'Error',
        });

        expect(loadError).toBe('Error');
        expect(loading).toBe(false);
      });
    });
  });

  describe('saveLicenseThreatGroup', () => {
    describe('licenseThreatGroup/saveLicenseThreatGroup/pending', () => {
      it('sets submitError property to null', () => {
        const state = Object.freeze({
          submitError: 'error',
          isDirty: true,
        });

        const { submitError, isDirty } = reducer(state, {
          type: 'licenseThreatGroup/saveLicenseThreatGroup/pending',
        });

        expect(submitError).toBeNull();
        expect(isDirty).toBe(true);
      });
    });

    describe('licenseThreatGroup/saveLicenseThreatGroup/fulfilled with isEditMode false', () => {
      it('sets state to payload, submit error property to null and isDirty to false', () => {
        const state = Object.freeze({
          availableLicenses: availableLicenses,
          currentLicenseThreatGroup: null,
          siblings: [],
          dirtyLTG: newLTG,
          isDirty: true,
          submitError: 'Error',
        });

        const { siblings, dirtyLTG, submitError, isDirty } = reducer(state, {
          type: 'licenseThreatGroup/saveLicenseThreatGroup/fulfilled',
          payload: {
            licenseThreatGroup: newLTG,
            isEditMode: false,
            licenseIds: newLTG.licenseIds,
          },
        });

        expect(submitError).toBeNull();
        expect(isDirty).toBe(false);
        expect(siblings.length).toBe(1);
        expect(siblings[0]).toEqual(newLTG);
        expect(dirtyLTG).toEqual({
          id: null,
          licenses: [],
          name: {
            isPristine: true,
            trimmedValue: '',
            validationErrors: null,
            value: '',
          },
          threatLevel: 5,
          licenseIds: [],
        });
      });
    });

    describe('licenseThreatGroup/saveLicenseThreatGroup/fulfilled with isEditMode true', () => {
      it('sets state to payload, submit error property to null and isDirty to false', () => {
        const state = Object.freeze({
          availableLicenses: availableLicenses,
          currentLicenseThreatGroup: newLTG,
          siblings: [newLTG],
          dirtyLTG: {
            ...newLTG,
            name: {
              isPristine: true,
              value: 'savings',
              trimmedValue: 'savings',
              validationErrors: null,
            },
          },
          isDirty: true,
          submitError: 'Error',
        });

        const { currentLicenseThreatGroup, siblings, dirtyLTG, submitError, isDirty } = reducer(state, {
          type: 'licenseThreatGroup/saveLicenseThreatGroup/fulfilled',
          payload: {
            licenseThreatGroup: updatedLTG,
            isEditMode: true,
            licenseIds: ['0BSD'],
          },
        });

        expect(submitError).toBeNull();
        expect(isDirty).toBe(false);
        expect(siblings.length).toBe(1);
        expect(siblings[0]).toEqual(updatedLTG);
        expect(dirtyLTG).toEqual({
          ...updatedLTG,
          name: {
            isPristine: true,
            value: 'updatedName',
            trimmedValue: 'updatedName',
            validationErrors: null,
          },
        });
        expect(currentLicenseThreatGroup).toEqual(updatedLTG);
      });
    });

    describe('licenseThreatGroup/saveLicenseThreatGroup/rejected', () => {
      it('sets submitError property to payload', () => {
        const state = Object.freeze({
          submitError: null,
          isDirty: true,
        });

        const { submitError, isDirty } = reducer(state, {
          type: 'licenseThreatGroup/saveLicenseThreatGroup/rejected',
          payload: 'Error',
        });

        expect(submitError).toBe('Error');
        expect(isDirty).toBe(true);
      });
    });
  });

  describe('deleteLicenseThreatGroup', () => {
    describe('licenseThreatGroup/deleteLicenseThreatGroup/pending', () => {
      it('sets submitError property to null', () => {
        const state = Object.freeze({
          deleteError: 'false',
          deleteMaskState: true,
        });

        const { deleteError, deleteMaskState } = reducer(state, {
          type: 'licenseThreatGroup/deleteLicenseThreatGroup/pending',
        });

        expect(deleteError).toBeNull();
        expect(deleteMaskState).toBe(false);
      });
    });

    describe('licenseThreatGroup/deleteLicenseThreatGroup/fulfilled', () => {
      it('sets state to payload, submit error property to null and isDirty to false', () => {
        const state = Object.freeze({
          availableLicenses: availableLicenses,
          currentLicenseThreatGroup: newLTG,
          siblings: [newLTG],
          dirtyLTG: {
            ...newLTG,
            name: {
              isPristine: true,
              value: 'savings',
              trimmedValue: 'savings',
              validationErrors: null,
            },
          },
          isDirty: true,
        });

        const { siblings, dirtyLTG, isDirty } = reducer(state, {
          type: 'licenseThreatGroup/deleteLicenseThreatGroup/fulfilled',
          payload: {
            id: '4db724d5c1d14784aa6ca600773f877f',
          },
        });

        expect(isDirty).toBe(false);
        expect(siblings.length).toBe(0);
        expect(dirtyLTG).toEqual({
          id: null,
          name: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          threatLevel: 5,
          licenses: [],
          licenseIds: [],
        });
      });
    });

    describe('licenseThreatGroup/deleteLicenseThreatGroup/rejected', () => {
      it('sets errorState property to payload, and deleting to false', () => {
        const state = Object.freeze({
          deleteError: null,
          deleteMaskState: true,
        });

        const { deleteError, deleteMaskState } = reducer(state, {
          type: 'licenseThreatGroup/deleteLicenseThreatGroup/rejected',
          payload: 'Error',
        });

        expect(deleteError).toBe('Error');
        expect(deleteMaskState).toBeNull();
      });
    });
  });
});
