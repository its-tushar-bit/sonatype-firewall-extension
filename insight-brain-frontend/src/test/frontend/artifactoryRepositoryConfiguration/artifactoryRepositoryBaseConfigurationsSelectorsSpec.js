/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getInitialState } from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsTestData';
import { getOriginalValues } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsUtil';
import { NO_CHANGES_MESSAGE } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';

describe('artifactoryRepositoryBaseConfigurationsSelectors', function () {
  let spyGetOriginalValues,
    selectAllowChange,
    selectEnabled,
    selectFormState,
    selectInheritedFromOrganizationName,
    selectInheritedFromOrgEnabled,
    selectArtifactoryRepositoryBaseConfigurationsSlice,
    selectIsDirty,
    selectOriginalValues,
    selectOwnerDTO,
    selectOwnerPublicId,
    selectArtifactoryConnection,
    selectArtifactoryConnectionStatus,
    selectServerData,
    selectValidationErrors,
    selectLoading,
    selectLoadError,
    selectEditLink,
    selectArtifactoryRepositoriesEnabled;
  beforeEach(() => {
    spyGetOriginalValues = jasmine.createSpy('getOriginalValues').and.callFake((serverData) => {
      return getOriginalValues(serverData);
    });
    const module = require('inject-loader!../../../../src/main/frontend/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors')(
      {
        './artifactoryRepositoryBaseConfigurationsUtil': {
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
      selectArtifactoryRepositoryBaseConfigurationsSlice,
      selectIsDirty,
      selectOriginalValues,
      selectOwnerDTO,
      selectOwnerPublicId,
      selectArtifactoryConnection,
      selectArtifactoryConnectionStatus,
      selectServerData,
      selectValidationErrors,
      selectLoading,
      selectLoadError,
      selectEditLink,
      selectArtifactoryRepositoriesEnabled,
    } = module);
  });

  describe('selectArtifactoryRepositoryBaseConfigurationsSlice', () => {
    it('selects `artifactoryRepositoryBaseConfigurationsSlice`', () => {
      const state = { artifactoryRepositoryBaseConfigurations: 'someArtifactoryRepositoryBaseConfigurations' };
      expect(selectArtifactoryRepositoryBaseConfigurationsSlice(state)).toBe(
        'someArtifactoryRepositoryBaseConfigurations'
      );
    });
  });

  describe('selectFormState', () => {
    it('selects `formState`', () => {
      const state = { artifactoryRepositoryBaseConfigurations: { formState: 'someFormState' } };
      expect(selectFormState(state)).toBe('someFormState');
    });
  });

  describe('selectServerData', () => {
    it('selects `serverData`', () => {
      const state = { artifactoryRepositoryBaseConfigurations: { serverData: 'someServerData' } };
      expect(selectServerData(state)).toBe('someServerData');
    });
  });

  describe('selectOwnerDTO', () => {
    it('selects `ownerDTO`', () => {
      const state = { artifactoryRepositoryBaseConfigurations: { serverData: { ownerDTO: 'someOwnerDTO' } } };
      expect(selectOwnerDTO(state)).toBe('someOwnerDTO');
    });
  });

  describe('selectOwnerPublicId', () => {
    it('selects `ownerPublicId`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: { serverData: { ownerDTO: { ownerPublicId: 'someOwnerPublicId' } } },
      };
      expect(selectOwnerPublicId(state)).toBe('someOwnerPublicId');
    });
  });

  describe('selectArtifactoryConnectionStatus', () => {
    it('selects `artifactoryConnectionStatus`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: { artifactoryConnectionStatus: 'someArtifactoryConnectionStatus' },
        },
      };
      expect(selectArtifactoryConnectionStatus(state)).toBe('someArtifactoryConnectionStatus');
    });
  });

  describe('selectInheritedFromOrganizationName', () => {
    it('selects `inheritedFromOrganizationName`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: { inheritedFromOrganizationName: 'someInheritedFromOrganizationName' },
          },
        },
      };
      expect(selectInheritedFromOrganizationName(state)).toBe('someInheritedFromOrganizationName');
    });
  });

  describe('selectEnabled', () => {
    it('selects `enabled`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: { enabled: 'someEnabled' },
          },
        },
      };
      expect(selectEnabled(state)).toBe('someEnabled');
    });
  });

  describe('selectInheritedFromOrgEnabled', () => {
    it('selects `inheritedFromOrgEnabled`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: { inheritedFromOrgEnabled: 'someInheritedFromOrgEnabled' },
          },
        },
      };
      expect(selectInheritedFromOrgEnabled(state)).toBe('someInheritedFromOrgEnabled');
    });
  });

  describe('selectAllowChange', () => {
    it('selects `allowChange`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: { allowChange: 'someAllowChange' },
          },
        },
      };
      expect(selectAllowChange(state)).toBe('someAllowChange');
    });
  });

  describe('selectArtifactoryConnection', () => {
    it('selects `artifactoryConnection`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: { artifactoryConnection: 'someArtifactoryConnection' },
        },
      };
      expect(selectArtifactoryConnection(state)).toBe('someArtifactoryConnection');
    });
  });

  describe('selectOriginalValues', () => {
    it('selects the result of calling `getOriginalValues`', () => {
      spyGetOriginalValues.and.returnValue('result');
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: 'someArtifactoryConnectionStatus',
          },
        },
      };
      expect(selectOriginalValues(state)).toBe('result');
      expect(spyGetOriginalValues).toHaveBeenCalledWith('someArtifactoryConnectionStatus');
    });
  });

  describe('selectIsDirty', () => {
    it('selects false if the form has not been changed', () => {
      spyGetOriginalValues.and.returnValue({
        enabled: null,
        allowOverride: true,
      });
      const state = {
        artifactoryRepositoryBaseConfigurations: {
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
        artifactoryRepositoryBaseConfigurations: {
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
        artifactoryRepositoryBaseConfigurations: {
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
        artifactoryRepositoryBaseConfigurations: {
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
        artifactoryRepositoryBaseConfigurations: {
          formState: {
            enabled: null,
            allowOverride: true,
          },
        },
      };
      expect(selectValidationErrors(state)).toBe(NO_CHANGES_MESSAGE);
    });

    it('selects the NO_CHANGES_MESSAGE if the form is not dirty', () => {});
  });

  describe('selectLoading', () => {
    it('selects `loading`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          loading: true,
        },
      };
      expect(selectLoading(state)).toBeTrue();
    });
  });

  describe('selectLoadError', () => {
    it('selects `loadError`', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          loadError: 'error',
        },
      };
      expect(selectLoadError(state)).toBe('error');
    });
  });

  describe('selectEditLink', () => {
    it('forms the editLink with application', () => {
      const state = {
        router: {
          currentState: {
            name: 'management.view.application',
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'id',
            },
          },
        },
      };
      expect(selectEditLink(state)).toBe(`artifactoryRepositoryBaseConfigurations.application({applicationId:'id'})`);
    });
    it('forms the editLink with organization', () => {
      const state = {
        router: {
          currentState: {
            name: 'management.view.organization',
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'id',
            },
          },
        },
      };
      expect(selectEditLink(state)).toBe(`artifactoryRepositoryBaseConfigurations.organization({organizationId:'id'})`);
    });
  });

  describe('selectArtifactoryRepositoriesEnabled', () => {
    it('constructs `ArtifactoryRepositoriesEnabled` with inheritedFromOrgEnabled true', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: {
              enabled: false,
              inheritedFromOrgEnabled: true,
              allowChange: false,
            },
          },
        },
      };
      expect(selectArtifactoryRepositoriesEnabled(state)).toBeTrue();
    });

    it('constructs `ArtifactoryRepositoriesEnabled` with inheritedFromOrgEnabled false', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: {
              enabled: false,
              inheritedFromOrgEnabled: false,
              allowChange: true,
            },
          },
        },
      };
      expect(selectArtifactoryRepositoriesEnabled(state)).toBeFalse();
    });

    it('constructs `ArtifactoryRepositoriesEnabled` with enabled and allowChange true', () => {
      const state = {
        artifactoryRepositoryBaseConfigurations: {
          serverData: {
            artifactoryConnectionStatus: {
              enabled: true,
              inheritedFromOrgEnabled: false,
              allowChange: true,
            },
          },
        },
      };
      expect(selectArtifactoryRepositoriesEnabled(state)).toBeTrue();
    });
  });
});
