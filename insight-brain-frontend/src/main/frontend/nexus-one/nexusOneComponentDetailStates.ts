/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ReactStateDeclaration } from '@uirouter/react';
import ComponentDetail from 'MainRoot/nosc/components/detail/ComponentDetail';

export const NEXUS_ONE_COMPONENT_DETAIL_STATE = 'nexusOneComponentDetail';

const COMPONENT_DETAIL_TITLE = 'Nexus One — Component Detail';

export function nexusOneComponentDetailStates(): ReactStateDeclaration[] {
  return [
    {
      name: NEXUS_ONE_COMPONENT_DETAIL_STATE,
      // More specific than Application Detail's /components tab route.
      url: '/applications/{publicId}/components/{componentHash}?scanId',
      component: ComponentDetail,
      data: { title: COMPONENT_DETAIL_TITLE },
    },
  ];
}
