/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getInitialState } from 'TestRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsTestData';
import { getOriginalValues } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsUtil';
import { NO_CHANGES_MESSAGE } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';

describe('innerSourceRepositoryBaseConfigurationsSelectors', function () {
  let spyGetOriginalValues,
    selectAllowChange,
    selectEnabled,
    selectFormState,
    selectInheritedFromOrganizationName,
    selectInheritedFromOrgEnabled,
    selectInnerSourceRepositoryBaseConfigurationsSlice,
    selectIsDirty,
    selectOriginalValues,
    selectOwnerDTO,
    selectOwnerPublicId,
    selectRepositoryConnections,
    selectRepositoryConnectionStatus,
    selectServerData,
    selectValidationErrors;

  beforeEach(() => {
    spyGetOriginalValues = jasmine.createSpy('getOriginalValues').and.callFake((serverData) => {
      return getOriginalValues(serverData);
    });
    const module = require('inject-loader!../../../../src/main/frontend/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors')(
      {
        'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsUtil': {
          initialState: getInitialState(),
          getOriginalValues: spyGetOriginalValues,
        },
      }
    );
    ({
      selectAllowChange,
      selectEnabled,
      selectFormState,
      selectInheritedFromOrganizationName,
      selectInheritedFromOrgEnabled,
      selectInnerSourceRepositoryBaseConfigurationsSlice,
      selectIsDirty,
      selectOriginalValues,
      selectOwnerDTO,
      selectOwnerPublicId,
      selectRepositoryConnections,
      selectRepositoryConnectionStatus,
      selectServerData,
      selectValidationErrors,
    } = module);
  });

  describe('selectInnerSourceRepositoryBaseConfigurationsSlice', () => {
    it('selects `innerSourceRepositoryBaseConfigurationsSlice`', () => {
      const state = { innerSourceRepositoryBaseConfigurations: 'someInnerSourceRepositoryBaseConfigurations' };
      expect(selectInnerSourceRepositoryBaseConfigurationsSlice(state)).toBe(
        'someInnerSourceRepositoryBaseConfigurations'
      );
    });
  });

  describe('selectFormState', () => {
    it('selects `formState`', () => {
      const state = { innerSourceRepositoryBaseConfigurations: { formState: 'someFormState' } };
      expect(selectFormState(state)).toBe('someFormState');
    });
  });

  describe('selectServerData', () => {
    it('selects `serverData`', () => {
      const state = { innerSourceRepositoryBaseConfigurations: { serverData: 'someServerData' } };
      expect(selectServerData(state)).toBe('someServerData');
    });
  });

  describe('selectOwnerDTO', () => {
    it('selects `ownerDTO`', () => {
      const state = { innerSourceRepositoryBaseConfigurations: { serverData: { ownerDTO: 'someOwnerDTO' } } };
      expect(selectOwnerDTO(state)).toBe('someOwnerDTO');
    });
  });

  describe('selectOwnerPublicId', () => {
    it('selects `ownerPublicId`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: { serverData: { ownerDTO: { ownerPublicId: 'someOwnerPublicId' } } },
      };
      expect(selectOwnerPublicId(state)).toBe('someOwnerPublicId');
    });
  });

  describe('selectRepositoryConnectionStatus', () => {
    it('selects `repositoryConnectionStatus`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: { repositoryConnectionStatus: 'someRepositoryConnectionStatus' },
        },
      };
      expect(selectRepositoryConnectionStatus(state)).toBe('someRepositoryConnectionStatus');
    });
  });

  describe('selectInheritedFromOrganizationName', () => {
    it('selects `inheritedFromOrganizationName`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: { inheritedFromOrganizationName: 'someInheritedFromOrganizationName' },
          },
        },
      };
      expect(selectInheritedFromOrganizationName(state)).toBe('someInheritedFromOrganizationName');
    });
  });

  describe('selectEnabled', () => {
    it('selects `enabled`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: { enabled: 'someEnabled' },
          },
        },
      };
      expect(selectEnabled(state)).toBe('someEnabled');
    });
  });

  describe('selectInheritedFromOrgEnabled', () => {
    it('selects `inheritedFromOrgEnabled`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: { inheritedFromOrgEnabled: 'someInheritedFromOrgEnabled' },
          },
        },
      };
      expect(selectInheritedFromOrgEnabled(state)).toBe('someInheritedFromOrgEnabled');
    });
  });

  describe('selectAllowChange', () => {
    it('selects `allowChange`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: { allowChange: 'someAllowChange' },
          },
        },
      };
      expect(selectAllowChange(state)).toBe('someAllowChange');
    });
  });

  describe('selectRepositoryConnections', () => {
    it('selects `repositoryConnections`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: { repositoryConnections: 'someRepositoryConnections' },
        },
      };
      expect(selectRepositoryConnections(state)).toBe('someRepositoryConnections');
    });
  });

  describe('selectOriginalValues', () => {
    it('selects the result of calling `getOriginalValues`', () => {
      spyGetOriginalValues.and.returnValue('result');
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: 'someRepositoryConnectionStatus',
          },
        },
      };
      expect(selectOriginalValues(state)).toBe('result');
      expect(spyGetOriginalValues).toHaveBeenCalledWith('someRepositoryConnectionStatus');
    });
  });

  describe('selectIsDirty', () => {
    it('selects false if the form has not been changed', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          formState: {
            enabled: null,
            allowOverride: true,
          },
        },
      };
      expect(selectIsDirty(state)).toBe(false);
    });

    it('selects true if enabled has changed', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          formState: {
            enabled: true,
            allowOverride: true,
          },
        },
      };
      expect(selectIsDirty(state)).toBe(true);
    });

    it('selects true if allowOverride has changed', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          formState: {
            enabled: null,
            allowOverride: false,
          },
        },
      };
      expect(selectIsDirty(state)).toBe(true);
    });
  });

  describe('selectValidationErrors', () => {
    it('selects null if the form is dirty', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          formState: {
            enabled: true,
            allowOverride: true,
          },
        },
      };
      expect(selectValidationErrors(state)).toBe(null);
    });

    it('selects the NO_CHANGES_MESSAGE if the form is not dirty', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          formState: {
            enabled: null,
            allowOverride: true,
          },
        },
      };
      expect(selectValidationErrors(state)).toBe(NO_CHANGES_MESSAGE);
    });
  });
});
