/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const createRepo = (postfix) => {
  return {
    httpCloneUrl: `url-${postfix}`,
    namespace: `ns-${postfix}`,
    project: `prj-${postfix}`,
    description: `desc-${postfix}`,
    isSelected: false,
    isImported: false,
  };
};

const createOrg = (postfix) => {
  return {
    organization: {
      name: `org-${postfix}`,
      id: `id-${postfix}`,
    },
  };
};

const createOrgWithToken = (postfix, value = null, parentValue = null, parentName = null) => {
  return {
    organization: {
      name: `org-${postfix}`,
      id: `id-${postfix}`,
    },
    sourceControl: {
      token: {
        value,
        parentValue,
        parentName,
      },
    },
  };
};

export { createRepo, createOrg, createOrgWithToken };
