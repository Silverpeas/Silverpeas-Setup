CREATE TABLE Person
(
	id					int NOT NULL,
	firstName		varchar(50) NOT NULL,
	lastName		varchar(50) NOT NULL
);

ALTER TABLE Person
		ADD CONSTRAINT PK_Person PRIMARY KEY(id);

CREATE INDEX IDX_Person ON Person (firstName,lastName);