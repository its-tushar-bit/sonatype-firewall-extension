/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import ArtifactoryRepositoryBaseConfigurations from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryBaseConfigurations';

router.stateRegistry.register({
  name: 'artifactoryRepositoryBaseConfigurations',
  abstract: true,
  url: '/management/edit',
  component: ArtifactoryRepositoryBaseConfigurations,
  data: {
    title: 'Artifactory Repository Configurations',
    isDirty: ['artifactoryRepositoryBaseConfigurations', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'artifactoryRepositoryBaseConfigurations.organization',
  url: '/organization/{organizationId}/artifactoryRepositoryBaseConfigurations',
});

router.stateRegistry.register({
  name: 'artifactoryRepositoryBaseConfigurations.application',
  url: '/application/{applicationId}/artifactoryRepositoryBaseConfigurations',
});
