/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NO_CHANGES_MESSAGE } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';

describe('innerSourceRepositoryBaseConfigurationsSelectors', function () {
  let selectAllowChange,
    selectEnabled,
    selectFormState,
    selectInheritedFromOrganizationName,
    selectInheritedFromOrgEnabled,
    selectInnerSourceRepositoryBaseConfigurationsSlice,
    selectIsDirty,
    selectOwnerDTO,
    selectOwnerPublicId,
    selectRepositoryConnections,
    selectRepositoryConnectionStatus,
    selectServerData,
    selectValidationErrors,
    selectLoading,
    selectLoadError,
    selectInnerSourceRepositoriesEnabled;
  beforeEach(() => {
    const module = require('inject-loader!../../../../src/main/frontend/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors')(
      {}
    );
    ({
      selectAllowChange,
      selectEnabled,
      selectFormState,
      selectInheritedFromOrganizationName,
      selectInheritedFromOrgEnabled,
      selectInnerSourceRepositoryBaseConfigurationsSlice,
      selectIsDirty,
      selectOwnerDTO,
      selectOwnerPublicId,
      selectRepositoryConnections,
      selectRepositoryConnectionStatus,
      selectServerData,
      selectValidationErrors,
      selectLoading,
      selectLoadError,
      selectInnerSourceRepositoriesEnabled,
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

  describe('selectIsDirty', () => {
    it('selects isDirty from the state', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          isDirty: true,
        },
      };
      expect(selectIsDirty(state)).toBeTrue();
    });
  });

  describe('selectValidationErrors', () => {
    it('selects null if the form is dirty', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          isDirty: true,
        },
      };
      expect(selectValidationErrors(state)).toBe(null);
    });

    it('selects the NO_CHANGES_MESSAGE if the form is not dirty', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          isDirty: false,
        },
      };
      expect(selectValidationErrors(state)).toBe(NO_CHANGES_MESSAGE);
    });

    it('selects the NO_CHANGES_MESSAGE if the form is not dirty', () => {});
  });

  describe('selectLoading', () => {
    it('selects `loading`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          loading: true,
        },
      };
      expect(selectLoading(state)).toBeTrue();
    });
  });

  describe('selectLoadError', () => {
    it('selects `loadError`', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          loadError: 'error',
        },
      };
      expect(selectLoadError(state)).toBe('error');
    });
  });

  describe('selectInnerSourceRepositoriesEnabled', () => {
    it('constructs `InnerSourceRepositoriesEnabled` with inheritedFromOrgEnabled true', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: {
              enabled: false,
              inheritedFromOrgEnabled: true,
              allowChange: false,
            },
          },
        },
      };
      expect(selectInnerSourceRepositoriesEnabled(state)).toBeTrue();
    });

    it('constructs `InnerSourceRepositoriesEnabled` with inheritedFromOrgEnabled false', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: {
              enabled: false,
              inheritedFromOrgEnabled: false,
              allowChange: true,
            },
          },
        },
      };
      expect(selectInnerSourceRepositoriesEnabled(state)).toBeFalse();
    });

    it('constructs `InnerSourceRepositoriesEnabled` with enabled and allowChange true', () => {
      const state = {
        innerSourceRepositoryBaseConfigurations: {
          serverData: {
            repositoryConnectionStatus: {
              enabled: true,
              inheritedFromOrgEnabled: false,
              allowChange: true,
            },
          },
        },
      };
      expect(selectInnerSourceRepositoriesEnabled(state)).toBeTrue();
    });
  });
});
