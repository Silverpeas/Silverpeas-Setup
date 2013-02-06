CREATE TABLE ST_Domain (
	id			int NOT NULL ,
	name			varchar (100) NOT NULL ,
	description		varchar (400) NULL ,
	propFileName		varchar (100) NOT NULL ,
	className		varchar (100) NOT NULL ,
	authenticationServer	varchar (100) NOT NULL ,
  theTimeStamp            varchar (100) DEFAULT('0') NOT NULL ,
  silverpeasServerURL     varchar (400) NULL
);

CREATE TABLE DomainToto_User (
	id		int NOT NULL,
	firstName	varchar (100) NULL ,
	lastName	varchar (100) NOT NULL ,
	phone		varchar (20) NULL ,
	homePhone	varchar (20) NULL ,
	cellPhone	varchar (20) NULL ,
	fax		varchar (20) NULL ,
	address		varchar (500) NULL ,
	title		varchar (100) NULL ,
	company		varchar (100) NULL ,
	position	varchar (100) NULL ,
	boss		varchar (100) NULL ,
	login		varchar (50) NOT NULL ,
	password	varchar (32) NULL ,
	passwordValid	char (1) DEFAULT ('Y') NOT NULL ,
	loginMail	varchar (100) NULL ,
	email		varchar (100) NULL 
);

CREATE TABLE DomainTiti_User (
	id		int NOT NULL,
	firstName	varchar (100) NULL ,
	lastName	varchar (100) NOT NULL ,
	phone		varchar (20) NULL ,
	homePhone	varchar (20) NULL ,
	cellPhone	varchar (20) NULL ,
	fax		varchar (20) NULL ,
	address		varchar (500) NULL ,
	title		varchar (100) NULL ,
	company		varchar (100) NULL ,
	position	varchar (100) NULL ,
	boss		varchar (100) NULL ,
	login		varchar (50) NOT NULL ,
	password	varchar (32) NULL ,
	passwordValid	char (1) DEFAULT ('Y') NOT NULL ,
	loginMail	varchar (100) NULL ,
	email		varchar (100) NULL 
);
