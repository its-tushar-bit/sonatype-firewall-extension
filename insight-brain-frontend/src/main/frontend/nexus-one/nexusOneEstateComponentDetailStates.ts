/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import EstateComponentDetail from 'MainRoot/nosc/components/detail/estate/EstateComponentDetail';
import {
  EstateComponentApplicationsRoute,
  EstateComponentLegalRoute,
  EstateComponentOrganizationsRoute,
  EstateComponentOverviewRoute,
  EstateComponentVulnerabilitiesRoute,
  EstateComponentViolationsRoute,
} from 'MainRoot/nosc/components/detail/estate/estateComponentTabRoutes';
import {
  estateComponentDetailStateNameForTab,
  NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE,
} from 'MainRoot/nosc/components/detail/estate/estateComponentDetailUtils';

/**
 * UI-Router state declarations for estate (hash-primary) Component Detail (CLM-43961).
 * Path {@code /components/{componentHash}} is distinct from the list at {@code /components?...}
 * and from app-scoped {@code /applications/{publicId}/components/{componentHash}}.
 */
const ESTATE_COMPONENT_DETAIL_TITLE = 'Nexus One — Component';

export { NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE };

export function nexusOneEstateComponentDetailStates(): ReactStateDeclaration[] {
  return [
    {
      name: NEXUS_ONE_ESTATE_COMPONENT_DETAIL_PARENT_STATE,
      url: '/components/{componentHash}',
      abstract: true,
      component: EstateComponentDetail,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('overview'),
      url: '',
      component: EstateComponentOverviewRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('legal'),
      url: '/legal',
      component: EstateComponentLegalRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('vulnerabilities'),
      url: '/vulnerabilities',
      component: EstateComponentVulnerabilitiesRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('violations'),
      url: '/violations',
      component: EstateComponentViolationsRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('applications'),
      url: '/applications',
      component: EstateComponentApplicationsRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
    {
      name: estateComponentDetailStateNameForTab('organizations'),
      url: '/organizations',
      component: EstateComponentOrganizationsRoute,
      data: { title: ESTATE_COMPONENT_DETAIL_TITLE },
    },
  ];
}
