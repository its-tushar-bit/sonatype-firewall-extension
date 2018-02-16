/* eslint no-console: "off" */
export default function iqTreeViewRadioSelectExampleController() {
  const vm = this;

  vm.ages = [
    {id: 1, name: 'past 24 hours'},
    {id: 7, name: 'past 7 days'},
    {id: 30, name: 'past 30 days'},
    {id: 90, name: 'past 90 days'},
    {id: 365, name: 'past 12 months'},
    {id: null, name: 'all time'}
  ];

  vm.selectedId = undefined;

  vm.selectAge = function(selectedId) {
    console.log('onChange', selectedId);

    // update iqTreeViewRadioSelect component with new state
    vm.selectedId = selectedId;
  };
}
