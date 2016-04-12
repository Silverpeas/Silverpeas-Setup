# Silverpeas-Setup
Silverpeas Setup is a project dedicated to the configuration of both Silverpeas and the JEE application server into which Silverpeas has to be deployed.

For Silverpeas 5, Silverpeas Setup was a Java library invoked through a shell script at Silverpeas installation. In Silverpeas 5, the installation process was made up of several tools:
* Maven to download and unpack Silverpeas artifacts from the Silverpeas Nexus,
* one script to build the final EAR and to deploy it into a JBoss server,
* one script that invokes the Silverpeas Setup library to configure both Silverpeas and the JBoss application server,
* and finally one last script that invokes the Silverpeas Setup library to migrate the database used by Silverpeas to an upper version.

Since Silverpeas 6, Silverpeas Setup is now a Gradle plugin that is used directly by the Silverpeas installation program that is itself a Gradle script. All is now performed by both the Gradle script and this Gradle plugin.
