/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getNLevelOrgUrl,
  getAddIconUrl,
  getOrganizationUrl,
  getApplicationSummaryUrl,
  getApplicationsUrl,
  getOrganizationsUrl,
  getRepositoryManagerUrl,
  getRepositoryManagerById,
} from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { nxTextInputStateHelpers, nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

// Import SpecUtil for jasmine compatibility layer
import 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;
const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

const APPS = [
  {
    id: 'applicationOneID',
    publicId: 'applicationOnePublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationOneName',
  },
  {
    id: 'applicationTwoID',
    publicId: 'applicationTwoPublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationTwoName',
  },
  {
    id: 'applicationThreeID',
    publicId: 'applicationThreePublicID',
    organizationId: 'organizationTwoID',
    name: 'ApplicationThreeName',
  },
];

const ORGS = [
  {
    id: 'organizationOneID',
    name: 'OrganizationOneName',
  },
  {
    id: 'organizationTwoID',
    name: 'OrganizationTwoName',
  },
];

const createOrgState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: false,
        ownerIconType: '',
        isApplication: false,
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState(''),
        appId: rscInitialState(''),
      },
    },
  },
};

const createAppState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'organizationOneID',
        name: 'OrganizationOneName',
        type: 'organization',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isApplication: true,
        isEditMode: false,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState(''),
        appId: rscInitialState(''),
      },
    },
  },
};

const editOrgState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'organizationOneID',
        name: 'OrganizationOneName',
        type: 'organization',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('newOrgName'),
        appId: rscInitialState(''),
      },
    },
  },
};

const editAppState = {
  router: {
    currentState: {
      name: 'management.view.application',
    },
    currentParams: {
      applicationPublicId: 'applicationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'applicationOneID',
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
        name: 'ApplicationOneName',
        type: 'application',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('newAppName'),
        appId: rscInitialState(''),
      },
    },
  },
};

const editRepositoryManagerNameState = {
  router: {
    currentParams: { '#': null, repositoryManagerId: 'repositoryManagerId' },
    currentState: {
      name: 'management.view.repository_manager',
      url: '/repository_manager/{repositoryManagerId}',
    },
  },
  orgsAndPolicies: {
    root: {
      selectedOwner: {
        id: 'repositoryManagerId',
        parentOrganizationId: 'REPOSITORY_CONTAINER_ID',
        name: 'oldName',
        type: 'repository_manager',
      },
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('newRepositoryManagerName'),
        appId: rscInitialState(''),
      },
    },
  },
};

describe('ownerModal actions', () => {
  let mock;

  beforeEach(() => {
    jest.useFakeTimers();
    mock = axiosMockAdapter();
  });

  afterEach(function () {
    jest.useRealTimers();
  });

  it('handles create new organization', (done) => {
    const store = SpecUtil.mockReduxStore(createOrgState);
    mock.onPost(getNLevelOrgUrl()).reply(200, {
      data: {
        id: 'organizationThreeID',
        name: 'OrganizationThreeName',
      },
    });

    mock.onGet(getOrganizationUrl(createOrgState.router.currentParams.organizationId)).reply(200, {
      data: {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
      },
    });

    store.dispatch(actions.createNewOwner()).then(() => {
      expect(mock.history.post.length).toBe(1);
      expect(mock.history.post[0].url).toBe(getNLevelOrgUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/createOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerSideNav/updateOwnersMapWithNewEntry',
        'ownerActions/ownerModal/createOwner/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(9);
      expect(actions).toHaveActionType('ownerActions/ownerModal/closeModal');

      done();
    });
  });

  it('handles create new application', (done) => {
    const store = SpecUtil.mockReduxStore(createAppState);
    mock.onPost(getApplicationsUrl()).reply(200, {
      data: {
        id: 'applicationFourID',
        name: 'applicationFourName',
        publicId: 'applicationFourPublicId',
        organizationId: 'organizationOneID',
      },
    });

    mock.onGet(getOrganizationUrl(createAppState.router.currentParams.organizationId)).reply(200, {
      data: {
        id: 'organizationThreeID',
        name: 'OrganizationThreeName',
      },
    });

    store.dispatch(actions.createNewOwner()).then(() => {
      expect(mock.history.post.length).toBe(1);
      expect(mock.history.post[0].url).toBe(getApplicationsUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/createOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerSideNav/updateOwnersMapWithNewEntry',
        'ownerActions/ownerModal/createOwner/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(9);
      expect(actions).toHaveActionType('ownerActions/ownerModal/closeModal');

      done();
    });
  });

  it('handles edit current organization', (done) => {
    const store = SpecUtil.mockReduxStore(editOrgState);
    mock.onPut(getOrganizationsUrl()).reply(200, {
      data: {
        id: 'organizationOneID',
        name: 'newOwnerName',
      },
    });
    mock.onPost(getAddIconUrl('organization', 'organizationOneID')).reply(200, {});

    mock.onGet(getOrganizationUrl(editOrgState.router.currentParams.organizationId)).reply(200, {
      data: {
        id: 'organizationOneID',
        name: 'newOwnerName',
      },
    });

    store.dispatch(actions.editCurrentOwner()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getOrganizationsUrl());

      const actions = store.getActions();
      expect(actions.length).toBe(7);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
        'ownerActions/ownerModal/closeModal',
      ]);

      done();
    });
  });

  it('handles edit current application', (done) => {
    const store = SpecUtil.mockReduxStore(editAppState);
    mock.onPut(getApplicationsUrl()).reply(200, {
      id: 'applicationOneID',
      publicId: 'applicationOnePublicID',
      organizationId: 'organizationOneID',
      name: 'newAppName',
    });
    mock.onPost(getAddIconUrl('application', 'applicationOneID')).reply(200, {});

    mock.onGet(getApplicationSummaryUrl(editAppState.router.currentParams.applicationPublicId)).reply(200, {
      data: {
        id: 'applicationOneID',
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
        name: 'newAppName',
      },
    });

    store.dispatch(actions.editCurrentOwner()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getApplicationsUrl());

      const actions = store.getActions();

      expect(actions.length).toBe(7);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
        'ownerActions/ownerModal/closeModal',
      ]);

      done();
    });
  });

  it('handles edit current repository manager name', (done) => {
    const store = SpecUtil.mockReduxStore(editRepositoryManagerNameState);
    mock.onPut(getRepositoryManagerUrl('repositoryManagerId', 'newRepositoryManagerName')).reply(200, {
      id: 'repositoryManagerId',
      name: 'newRepositoryManagerName',
    });
    mock.onPost(getAddIconUrl('repository_manager', 'repositoryManagerId')).reply(200, {});

    mock.onGet(getRepositoryManagerById('repositoryManagerId')).reply(200, {
      data: {
        id: 'applicationOneID',
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
        name: 'newAppName',
      },
    });

    store.dispatch(actions.editCurrentOwner()).then(() => {
      expect(mock.history.put.length).toBe(1);
      expect(mock.history.put[0].url).toBe(getRepositoryManagerUrl('repositoryManagerId', 'newRepositoryManagerName'));

      const actions = store.getActions();

      expect(actions.length).toBe(7);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
      ]);

      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      expect(actions.length).toBe(8);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/ownerModal/editCurrentOwner/pending',
        'ownerActions/updateOwner/pending',
        'orgsAndPolicies/loadSelectedOwner/pending',
        'orgsAndPolicies/setShowLimitedFirewallAccessAlert',
        'orgsAndPolicies/loadSelectedOwner/fulfilled',
        'ownerActions/updateOwner/fulfilled',
        'ownerActions/ownerModal/editCurrentOwner/fulfilled',
        'ownerActions/ownerModal/closeModal',
      ]);

      done();
    });
  });
});
