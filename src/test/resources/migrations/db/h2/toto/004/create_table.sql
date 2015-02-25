CREATE TABLE Person
(
	id					int NOT NULL,
	firstName		varchar(50) NOT NULL,
	lastName		varchar(50) NOT NULL,
	age					int
);

CREATE TABLE Preferences
(
	personId		int NOT NULL,
	language		varchar(2) NOT NULL DEFAULT 'fr'
);

ALTER TABLE Person
ADD CONSTRAINT PK_Person PRIMARY KEY(id);

ALTER TABLE Preferences
ADD CONSTRAINT FK_Person FOREIGN KEY(personId) REFERENCES Person(id);

CREATE INDEX IDX_Person ON Person (firstName,lastName);