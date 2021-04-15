/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function NumberInputWithStringValue() {
  return {
    require: 'ngModel',
    link: NumberInputWithStringValueLink,
  };

  function NumberInputWithStringValueLink(scope, element, attrs, ngModelController) {
    ngModelController.$parsers.push(parseToString);
    ngModelController.$formatters.push(formatToNumber);

    function parseToString(value) {
      return value === null ? undefined : '' + value;
    }

    function formatToNumber(value) {
      return parseFloat(value);
    }
  }
}
