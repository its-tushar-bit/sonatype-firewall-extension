/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function OwnerDetailTreeViewDirective() {
  return {
    controller: 'OwnerDetailTreeViewController',
    controllerAs: 'vm',
    templateUrl: 'owner.manager/navigation/owner.detail.tree.view.directive.html'
  };
}
OwnerDetailTreeViewDirective.$inject = [];
