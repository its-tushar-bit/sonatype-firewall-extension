/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { NxTag } from '@sonatype/react-shared-components';
import { selectIsPro, selectIsEnterprise, selectTierLoading } from 'MainRoot/productFeatures/productTierSelectors';

export default function TierBadge() {
  const loading = useSelector(selectTierLoading);
  const isEnterprise = useSelector(selectIsEnterprise);
  const isPro = useSelector(selectIsPro);

  if (loading) return null;
  if (isEnterprise) return <NxTag className="iq-tier-badge">ENTERPRISE</NxTag>;
  if (isPro) return <NxTag className="iq-tier-badge">PRO</NxTag>;
  return null;
}
