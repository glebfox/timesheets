# Spent time

Source: `datatype/`, `app/TimeParser.java`, the `datatype.spentTime.*` keys of the message bundles

A duration in whole hours and minutes: the time spent in a [time entry](time-entry.md). It is stored as a total number of minutes.

## Values

- Hours are zero or more, with no upper bound; minutes are 0–59. Any other combination is rejected.
- Two values are equal when their hours and minutes are equal.
- Values add up and compare exactly to the minute: three times 20 minutes is one hour.
- As decimal hours, a value is truncated to two decimals: 20 minutes is 0.33.

## Display

`<hours>h <minutes>m` in English and `<hours>ч <minutes>м` in Russian, minutes always with two digits: `4h 30m`, `100h 05m`.

## Input

Text is read by the first rule that matches:

1. Hours and minutes separated by a colon or a space: `4:30`, `4 30`. Minutes above 59 are rejected.
2. A bare number: up to 24 it is hours (`4` is 4h, `24` is 24h), above 24 it is minutes (`30` is 30m, `90` is 1h 30m).
3. A number followed by an hours unit and/or a number followed by a minutes unit, found anywhere in the text, in either order, spaces optional: `4h30m`, `4hr 30min`, `4hours`, `30m`. English units are `h`, `hr`, `hour` and `m`, `min`, `minutes`; in the Russian locale `ч` and `м`, `мин` are accepted as well. The rest of the text is ignored, so `about 2h` is 2h and `4 mango` is 4m; a negative number counts as zero. Minutes above 59 are rejected here too, so `90m` fails while `90` is accepted.
4. Text with no such number fails to parse.

## Not covered

- Russian units: the tests run in the English locale.
- How the time-entry form reports `4:75` or `90m`: these fail as invalid values rather than as parse errors; not checked live.
