/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { selectTenantScmProviderTypes } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function ScmProviderOptions() {
  const providers = useSelector(selectTenantScmProviderTypes);

  return (
    <>
      <option value="">-- Not Configured --</option>
      {providers.map(({ name, value }) => (
        <option key={value} value={value}>
          {name}
        </option>
      ))}
    </>
  );
}
