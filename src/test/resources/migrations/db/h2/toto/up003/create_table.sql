CREATE TABLE Preferences
(
    personId		int NOT NULL,
    language		varchar(2) NOT NULL DEFAULT 'fr'
);

ALTER TABLE Preferences
ADD CONSTRAINT FK_Person FOREIGN KEY(personId) REFERENCES Person(id);

