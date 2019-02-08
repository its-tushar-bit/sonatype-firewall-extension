/* eslint no-console: "off" */

export default function iqPolicyThreatLevelSliderController() {
  const vm = this;

  vm.selectedRange = [3, 8];

  vm.onChange = function(selectedRange) {
    console.log(selectedRange);
    vm.selectedRange = selectedRange;
  };
}
