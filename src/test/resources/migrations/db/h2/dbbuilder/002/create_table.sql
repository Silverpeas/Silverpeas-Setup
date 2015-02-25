/**
 * Table for managing the version of each available module in Silverpeas.
 */
create table SR_PACKAGES
(
    SR_PACKAGE  VARCHAR(32) NOT NULL PRIMARY KEY,
    SR_VERSION  CHAR(3)     NOT NULL
);




