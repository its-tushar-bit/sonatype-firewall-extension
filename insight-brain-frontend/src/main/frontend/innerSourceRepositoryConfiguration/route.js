/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';

router.stateRegistry.register({
  name: 'repositoryBaseConfigurations',
  abstract: true,
  url: '/management/edit',
  component: InnerSourceRepositoryBaseConfigurations,
  data: {
    title: 'Repository Configurations',
    isDirty: ['innerSourceRepositoryBaseConfigurations', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'repositoryBaseConfigurations.organization',
  url: '/organization/{organizationId}/repositoryBaseConfigurations',
});

router.stateRegistry.register({
  name: 'repositoryBaseConfigurations.application',
  url: '/application/{applicationId}/repositoryBaseConfigurations',
});
