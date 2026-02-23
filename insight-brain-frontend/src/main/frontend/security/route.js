/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';

// Administrator components
import AdministratorsConfig from 'MainRoot/configuration/administrators/config/AdministratorsConfig';
import AdministratorsEdit from 'MainRoot/configuration/administrators/edit/AdministratorsEdit';

// Role components
import RoleListContainer from './roleList/RoleListContainer';
import RoleEditorContainer from './roleEditor/RoleEditorContainer';

// User components
import UserAddContainer from './users/userConfiguration/UserAddContainer';
import UserEditContainer from './users/userConfiguration/UserEditContainer';
import UserManagementContainer from './users/UserManagementContainer';

// Administrator routes
router.stateRegistry.register({
  name: 'administrators',
  url: '/administrators',
  component: AdministratorsConfig,
  data: {
    title: 'Administrator Config',
  },
});

router.stateRegistry.register({
  name: 'administratorsEdit',
  url: '/administrators/{roleId}',
  component: AdministratorsEdit,
  data: {
    title: 'Administrator Edit',
    isDirty: ['administratorsConfig', 'isDirty'],
  },
});

// Role routes
router.stateRegistry.register({
  name: 'rolesList',
  url: '/roles',
  component: RoleListContainer,
  data: {
    title: 'Roles',
  },
});

router.stateRegistry.register({
  name: 'addRole',
  url: '/roles/_new_',
  component: RoleEditorContainer,
  data: {
    title: 'Create a role',
    isDirty: ['roleEditor', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'editRole',
  url: '/roles/{roleId}',
  component: RoleEditorContainer,
  data: {
    title: 'Edit a Role',
    isDirty: ['roleEditor', 'isDirty'],
  },
});

// User routes
router.stateRegistry.register({
  name: 'users',
  url: '/users',
  component: UserManagementContainer,
  data: {
    title: 'Users',
  },
});

router.stateRegistry.register({
  name: 'users.activity',
  url: '/activity',
  data: {
    title: 'User Activity',
    activeTab: 'activity',
  },
});

router.stateRegistry.register({
  name: 'userActivity',
  url: '/user-activity',
  component: UserManagementContainer,
  data: {
    title: 'User Activity',
    activeTab: 'activity',
  },
});

router.stateRegistry.register({
  name: 'createUser',
  url: '/users/_new_',
  component: UserAddContainer,
  data: {
    title: 'Add New User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'editUser',
  url: '/users/{userId}',
  component: UserEditContainer,
  data: {
    title: 'Edit User',
    isDirty: ['userConfiguration', 'isDirty'],
  },
});
