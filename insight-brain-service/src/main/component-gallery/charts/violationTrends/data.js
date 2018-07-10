import {range} from 'ramda';

function getRandomInt(max) {
  return Math.floor(Math.random() * Math.floor(max));
}

const generate12WeekData = (max) => range(1, 13).map(i => ({week: i, violations: getRandomInt(max)}));

export const security = {
  discovered: generate12WeekData(20),
  fixed: generate12WeekData(15),
  waived: generate12WeekData(4)
};

export const license = {
  discovered: generate12WeekData(15),
  fixed: generate12WeekData(20),
  waived: generate12WeekData(2)
};

export const quality = {
  discovered: generate12WeekData(10),
  fixed: generate12WeekData(10),
  waived: generate12WeekData(0)
};

export const other = {
  discovered: generate12WeekData(10),
  fixed: generate12WeekData(5),
  waived: generate12WeekData(5)
};
