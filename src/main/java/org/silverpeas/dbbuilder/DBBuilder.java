/**
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.dbbuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.silverpeas.applicationbuilder.AppBuilderException;
import org.silverpeas.dbbuilder.sql.ConnectionFactory;
import org.silverpeas.dbbuilder.sql.FileInformation;
import org.silverpeas.dbbuilder.sql.InstallSQLInstruction;
import org.silverpeas.dbbuilder.sql.MetaInstructions;
import org.silverpeas.dbbuilder.sql.RemoveSQLInstruction;
import org.silverpeas.dbbuilder.sql.SQLInstruction;
import org.silverpeas.dbbuilder.sql.UninstallInformations;
import org.silverpeas.dbbuilder.sql.UninstallSQLInstruction;
import org.silverpeas.dbbuilder.util.Action;
import org.silverpeas.dbbuilder.util.CommandLineParameters;
import org.silverpeas.dbbuilder.util.Configuration;
import org.silverpeas.dbbuilder.util.DatabaseType;
import org.silverpeas.util.Console;
import org.silverpeas.util.file.FileUtil;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.jdom.Element;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static java.io.File.separatorChar;
import static org.silverpeas.dbbuilder.DBBuilderItem.*;
import static org.silverpeas.dbbuilder.util.Action.*;

public class DBBuilder {

  public static final ResourceBundle messages = java.util.ResourceBundle.getBundle("messages");
  // Version application
  public static final String DBBuilderAppVersion = messages.getString("silverpeas.version");
  // Fichier log
  protected static Console console;
  static public final String CREATE_TABLE_TAG = "create_table";
  static public final String CREATE_INDEX_TAG = "create_index";
  static public final String CREATE_CONSTRAINT_TAG = "create_constraint";
  static public final String CREATE_DATA_TAG = "init";
  static public final String DROP_TABLE_TAG = "drop_table";
  static public final String DROP_INDEX_TAG = "drop_index";
  static public final String DROP_CONSTRAINT_TAG = "drop_constraint";
  static public final String DROP_DATA_TAG = "clean";
  public static final String[] TAGS_TO_MERGE_4_INSTALL = {CREATE_TABLE_TAG, CREATE_INDEX_TAG,
    CREATE_CONSTRAINT_TAG, CREATE_DATA_TAG};
  public static final String[] TAGS_TO_MERGE_4_UNINSTALL = {DROP_CONSTRAINT_TAG, DROP_INDEX_TAG,
    DROP_DATA_TAG, DROP_TABLE_TAG};
  public static final String[] TAGS_TO_MERGE_4_ALL = {DROP_CONSTRAINT_TAG, DROP_INDEX_TAG,
    DROP_DATA_TAG, DROP_TABLE_TAG, CREATE_TABLE_TAG, CREATE_INDEX_TAG, CREATE_CONSTRAINT_TAG,
    CREATE_DATA_TAG};
  public static final String[] TAGS_TO_MERGE_4_OPTIMIZE = {DROP_INDEX_TAG, CREATE_INDEX_TAG};
  protected static final String FIRST_DBCONTRIBUTION_FILE = "dbbuilder-contribution.xml";
  protected static final String MASTER_DBCONTRIBUTION_FILE = "master-contribution.xml";
  protected static final String REQUIREMENT_TAG = "requirement"; // pré requis à vérifier pour
  // prise en comptes
  protected static final String DEPENDENCY_TAG = "dependency"; // ordonnancement à vérifier pour
  // prise en comptes
  protected static final String FILE_TAG = "file";
  protected static final String FILENAME_ATTRIB = "name";
  protected static final String PRODUCT_TAG = "product";
  protected static final String PRODUCTNAME_ATTRIB = "name";
  // mes variables rajoutée
  private static Properties dbBuilderResources = new Properties();
  protected static final String DBBUILDER_MODULE = "dbbuilder";
  // Params
  private static CommandLineParameters params = null;

  /**
   * @param args
   * @see
   */
  public static void main(String[] args) {
    ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(
        "classpath:/spring-jdbc-datasource.xml");
    try {
      // Ouverture des traces
      Date startDate = new Date();
      System.out.println(MessageFormat.format(messages.getString("dbbuilder.start"),
          DBBuilderAppVersion, startDate));
      console = new Console(DBBuilder.class);
      console.printMessage("*************************************************************");
      console.printMessage(MessageFormat.format(messages.getString("dbbuilder.start"),
          DBBuilderAppVersion, startDate));
      // Lecture des variables d'environnement à partir de dbBuilderSettings
      dbBuilderResources = FileUtil.loadResource(
          "/org/silverpeas/dbBuilder/settings/dbBuilderSettings.properties");
      // Lecture des paramètres d'entrée
      params = new CommandLineParameters(console, args);

      if (params.isSimulate() && DatabaseType.ORACLE == params.getDbType()) {
        throw new Exception(messages.getString("oracle.simulate.error"));
      }
      console.printMessage(messages.getString("jdbc.connection.configuration"));
      console.printMessage(ConnectionFactory.getConnectionInfo());
      console.printMessage("\tAction        : " + params.getAction());
      console.printMessage("\tVerbose mode  : " + params.isVerbose());
      console.printMessage("\tSimulate mode : " + params.isSimulate());
      if (Action.ACTION_CONNECT == params.getAction()) {
        // un petit message et puis c'est tout
        console.printMessage(messages.getString("connection.success"));
        System.out.println(messages.getString("connection.success"));
      } else {
        // Modules en place sur la BD avant install
        console.printMessage("DB Status before build :");
        List<String> packagesIntoDB = checkDBStatus();
        // initialisation d'un vecteur des instructions SQL à passer en fin d'upgrade
        // pour mettre à niveau les versions de modules en base
        MetaInstructions sqlMetaInstructions = new MetaInstructions();
        File dirXml = new File(params.getDbType().getDBContributionDir());
        DBXmlDocument destXml = loadMasterContribution(dirXml);
        UninstallInformations processesToCacheIntoDB = new UninstallInformations();

        File[] listeFileXml = dirXml.listFiles();
        Arrays.sort(listeFileXml);

        List<DBXmlDocument> listeDBXmlDocument = new ArrayList<DBXmlDocument>(listeFileXml.length);
        int ignoredFiles = 0;
        // Ouverture de tous les fichiers de configurations
        console.printMessage(messages.getString("ignored.contribution"));

        for (File xmlFile : listeFileXml) {
          if (xmlFile.isFile() && "xml".equals(FileUtil.getExtension(xmlFile))
              && !(FIRST_DBCONTRIBUTION_FILE.equalsIgnoreCase(xmlFile.getName()))
              && !(MASTER_DBCONTRIBUTION_FILE.equalsIgnoreCase(xmlFile.getName()))) {
            DBXmlDocument fXml = new DBXmlDocument(dirXml, xmlFile.getName());
            fXml.load();
            // vérification des dépendances et prise en compte uniquement si dependences OK
            if (hasUnresolvedRequirements(listeFileXml, fXml)) {
              console.printMessage('\t' + xmlFile.getName() + " (because of unresolved requirements).");
              ignoredFiles++;
            } else if (ACTION_ENFORCE_UNINSTALL == params.getAction()) {
              console.printMessage('\t' + xmlFile.getName() + " (because of "
                  + ACTION_ENFORCE_UNINSTALL + " mode).");
              ignoredFiles++;
            } else {
              listeDBXmlDocument.add(fXml);
            }
          }
        }
        if (0 == ignoredFiles) {
          console.printMessage("\t(none)");
        }

        // prépare une HashMap des modules présents en fichiers de contribution
        Map packagesIntoFile = new HashMap();
        int j = 0;
        console.printMessage(messages.getString("merged.contribution"));
        console.printMessage(params.getAction().toString());
        if (ACTION_ENFORCE_UNINSTALL != params.getAction()) {
          console.printMessage('\t' + FIRST_DBCONTRIBUTION_FILE);
          j++;
        }
        for (DBXmlDocument currentDoc : listeDBXmlDocument) {
          console.printMessage('\t' + currentDoc.getName());
          j++;
        }
        if (0 == j) {
          console.printMessage("\t(none)");
        }
        // merge des diffrents fichiers de contribution éligibles :
        console.printMessage("Build decisions are :");
        // d'abord le fichier dbbuilder-contribution ...
        DBXmlDocument fileXml;
        if (ACTION_ENFORCE_UNINSTALL != params.getAction()) {
          try {
            fileXml = new DBXmlDocument(dirXml, FIRST_DBCONTRIBUTION_FILE);
            fileXml.load();
          } catch (Exception e) {
            // contribution de dbbuilder non trouve -> on continue, on est certainement en train
            // de desinstaller la totale
            fileXml = null;
          }
          if (null != fileXml) {
            DBBuilderFileItem dbbuilderItem = new DBBuilderFileItem(fileXml);
            packagesIntoFile.put(dbbuilderItem.getModule(), null);
            mergeActionsToDo(dbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
          }
        }

        // ... puis les autres
        for (DBXmlDocument currentDoc : listeDBXmlDocument) {
          DBBuilderFileItem tmpdbbuilderItem = new DBBuilderFileItem(currentDoc);
          packagesIntoFile.put(tmpdbbuilderItem.getModule(), null);
          mergeActionsToDo(tmpdbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
        }

        // ... et enfin les pièces BD à désinstaller
        // ... attention, l'ordonnancement n'étant pas dispo, on les traite dans
        // l'ordre inverse pour faire passer busCore a la fin, de nombreuses contraintes
        // des autres modules referencant les PK de busCore
        List<String> itemsList = new ArrayList<String>();

        boolean foundDBBuilder = false;
        for (String dbPackage : packagesIntoDB) {
          if (!packagesIntoFile.containsKey(dbPackage)) {
            // Package en base et non en contribution -> candidat à desinstallation
            if (DBBUILDER_MODULE.equalsIgnoreCase(dbPackage)) {
              foundDBBuilder = true;
            } else if (ACTION_ENFORCE_UNINSTALL == params.getAction()) {
              if (dbPackage.equals(params.getModuleName())) {
                itemsList.add(0, dbPackage);
              }
            } else {
              itemsList.add(0, dbPackage);
            }
          }
        }

        if (foundDBBuilder) {
          if (ACTION_ENFORCE_UNINSTALL == params.getAction()) {
            if (DBBUILDER_MODULE.equals(params.getModuleName())) {
              itemsList.add(itemsList.size(), DBBUILDER_MODULE);
            }
          } else {
            itemsList.add(itemsList.size(), DBBUILDER_MODULE);
          }
        }
        for (String item : itemsList) {
          console.printMessage("**** Treating " + item + " ****");
          DBBuilderDBItem tmpdbbuilderItem = new DBBuilderDBItem(item);
          mergeActionsToDo(tmpdbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
        }
        destXml.setName("res.txt");
        destXml.save();
        console.printMessage("Build parts are :");
        // Traitement des pièces sélectionnées
        // remarque : durant cette phase, les erreurs sont traitées -> on les catche en
        // retour sans les retraiter
        if (ACTION_INSTALL == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_INSTALL);
        } else if (ACTION_UNINSTALL == params.getAction() || ACTION_ENFORCE_UNINSTALL == params.
            getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_UNINSTALL);
        } else if (ACTION_OPTIMIZE == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_OPTIMIZE);
        } else if (ACTION_ALL == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_ALL);
        }
        // Modules en place sur la BD en final
        console.printMessage("Finally DB Status :");
        checkDBStatus();
      }
      Date endDate = new Date();
      console.printMessage(MessageFormat.format(messages.getString("dbbuilder.success"), endDate));
      System.out.println("*******************************************************************");
      System.out.println(MessageFormat.format(messages.getString("dbbuilder.success"), endDate));
    } catch (Exception e) {
      e.printStackTrace();
      console.printError(e.getMessage(), e);
      Date endDate = new Date();
      console.printError(MessageFormat.format(messages.getString("dbbuilder.failure"), endDate));
      System.out.println("*******************************************************************");
      System.out.println(MessageFormat.format(messages.getString("dbbuilder.failure"), endDate));
      System.exit(1);
    } finally {
      springContext.close();
      console.close();
    }
  } // main

  @SuppressWarnings("unchecked")
  private static boolean hasUnresolvedRequirements(File[] listeFileXml, DBXmlDocument fXml) {
    Element root = fXml.getDocument().getRootElement();
    List<Element> listeDependencies = root.getChildren(REQUIREMENT_TAG);
    if (null != listeDependencies) {
      for (Element eltDependencies : listeDependencies) {
        List<Element> listeDependencyFiles = eltDependencies.getChildren(FILE_TAG);
        for (Element eltDependencyFile : listeDependencyFiles) {
          String name = eltDependencyFile.getAttributeValue(FILENAME_ATTRIB);
          boolean found = false;
          for (int i = 0; i < listeFileXml.length; i++) {
            File f = listeFileXml[i];
            if (f.getName().equals(name)) {
              found = true;
              i = listeFileXml.length;
            }
          }
          if (!found) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static Properties getdbBuilderResources() {
    return dbBuilderResources;
  }

  public static void mergeActionsToDo(DBBuilderItem pdbbuilderItem, DBXmlDocument xmlFile,
      UninstallInformations processesToCacheIntoDB, MetaInstructions sqlMetaInstructions) {

    String package_name = pdbbuilderItem.getModule();
    String versionDB;
    String versionFile;
    try {
      versionDB = pdbbuilderItem.getVersionFromDB();
      versionFile = pdbbuilderItem.getVersionFromFile();
    } catch (Exception e) {
      console.printError("", e);
      return;
    }
    String[] tags_to_merge = null;
    List<VersionTag> blocks_merge = new ArrayList<VersionTag>();

    if (pdbbuilderItem instanceof org.silverpeas.dbbuilder.DBBuilderFileItem) {
      DBBuilderFileItem dbbuilderItem = (DBBuilderFileItem) pdbbuilderItem;
      int iversionDB = -1;
      if (!versionDB.equals(NOTINSTALLED)) {
        iversionDB = Integer.parseInt(versionDB);
      }
      int iversionFile = Integer.parseInt(versionFile);
      if (iversionDB == iversionFile) {
        if (params.getAction().isMigration()) {
          console.printMessage('\t' + package_name + " is up to date with version " + versionFile
              + '.');
        } else {
          console.printMessage('\t' + package_name + " is up to date with version " + versionFile
              + " and will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = Collections.singletonList(new VersionTag(CURRENT_TAG, versionFile));
        }
      } else if (iversionDB > iversionFile) {
        console.printMessage('\t' + package_name
            + " will be ignored because this package is newer into DB than installed files.");
      } else {
        if (params.getAction().isMigration()) {
          if (-1 == iversionDB) {
            console.printMessage('\t' + package_name + " will be installed with version "
                + versionFile + '.');
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;
            blocks_merge = Collections.singletonList( new VersionTag(CURRENT_TAG, versionFile));
            // module nouvellement installé -> il faut stocker en base sa procedure de uninstall
            processesToCacheIntoDB.addInformation(dbbuilderItem.getModule(), package_name,
                dbbuilderItem.getFileXml());
            // inscription du module en base
            sqlMetaInstructions.addInstruction(dbbuilderItem.getModule(),
                new InstallSQLInstruction(versionFile, package_name));
          } else {
            console.printMessage('\t' + package_name + " will be upgraded from " + versionDB
                + " to " + versionFile + '.');
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;
            for (int i = 0; i < iversionFile - iversionDB; i++) {
              int currentVersion = iversionDB + i;
              String sversionFile = "000" + currentVersion;
              sversionFile = sversionFile.substring(sversionFile.length() - 3);
              VersionTag version = new VersionTag(PREVIOUS_TAG, sversionFile);
              blocks_merge.add(version);
               sqlMetaInstructions.addInstruction(dbbuilderItem.getModule(),
                new UninstallSQLInstruction(version.getResultingVersion(), package_name));
            }
            // module upgradé -> il faut stocker en base sa nouvelle procedure de uninstall
            processesToCacheIntoDB.addInformation(dbbuilderItem.getModule(), package_name,
                dbbuilderItem.getFileXml());
          }
        } else if (ACTION_OPTIMIZE == params.getAction()) {
          console.printMessage('\t' + package_name + " will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = Collections.singletonList(new VersionTag(CURRENT_TAG, versionFile));
        }

        // construction du xml global des actions d'upgrade de la base
        if (!blocks_merge.isEmpty() && null != tags_to_merge) {
          try {
            xmlFile.mergeWith(pdbbuilderItem, tags_to_merge, blocks_merge);
          } catch (Exception e) {
            console.printError("Error with " + pdbbuilderItem.getModule() + ' ' + e.getMessage(), e);
          }
        }
      }

    } else if (pdbbuilderItem instanceof org.silverpeas.dbbuilder.DBBuilderDBItem) {

      if (ACTION_UNINSTALL == params.getAction() || ACTION_ALL == params.getAction()
          || ACTION_ENFORCE_UNINSTALL == params.getAction()) {
        console.printMessage('\t' + package_name + " will be uninstalled.");
        tags_to_merge = TAGS_TO_MERGE_4_UNINSTALL;
        // desinscription du module de la base
        if (!DBBUILDER_MODULE.equalsIgnoreCase(package_name)) {
          System.out.println("delete from SR_");
          sqlMetaInstructions.addInstruction(pdbbuilderItem.getModule(), new RemoveSQLInstruction(
              package_name));
        }
        // construction du xml global des actions d'upgrade de la base
        if (null != tags_to_merge) {
          try {
            xmlFile.mergeWith(pdbbuilderItem, tags_to_merge, null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      console.printMessage("");
      console.printMessage("*** AVERTISSEMENT ***");
      console.printMessage("\t Le Module " + package_name
          + " est présent en BD mais n'a pas de scripts SQL fichiers");
      console.printMessage("");
      console.printMessage("*** AVERTISSEMENT ***");
      console.printMessage("Le Module " + package_name
          + " est présent en BD mais n'a pas de scripts SQL fichiers");
    }

  }

  private static void processDB(DBXmlDocument xmlFile, UninstallInformations processesToCacheIntoDB,
      MetaInstructions sqlMetaInstructions, String[] tagsToProcess) throws Exception {
    Element root = xmlFile.getDocument().getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> modules = root.getChildren(DBXmlDocument.ELT_MODULE);
    for (Element module : modules) {
      Connection connection = null;
      try {
        connection = ConnectionFactory.getConnection();
        connection.setAutoCommit(false);
        processSQLFiles(connection, module, tagsToProcess, sqlMetaInstructions);
        cacheIntoDb(connection, processesToCacheIntoDB.getInformations(module.getAttributeValue(
            DBXmlDocument.ATT_MODULE_ID)));
        if (params.isSimulate()) {
          DbUtils.rollback(connection);
        } else {
          connection.commit();
        }
      } catch (Exception e) {
        DbUtils.rollback(connection);
        throw e;
      } finally {
        DbUtils.closeQuietly(connection);
      }
    }
    console.printMessage("DB Status after build :");
    checkDBStatus();
  }

  // liste des packages en base
  private static List<String> checkDBStatus() {
    List<String> packagesIntoDB = new ArrayList<String>();
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      connection = ConnectionFactory.getConnection();
      stmt = connection.createStatement();
      rs = stmt.executeQuery("select SR_PACKAGE, SR_VERSION from SR_PACKAGES order by SR_PACKAGE");
      while (rs.next()) {
        String srPackage = rs.getString("SR_PACKAGE");
        String srVersion = rs.getString("SR_VERSION");
        console.printMessage('\t' + srPackage + " v. " + srVersion);
        packagesIntoDB.add(srPackage);
      }
    } catch (SQLException sqlex) {
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
      DbUtils.closeQuietly(connection);
    }
    return packagesIntoDB;
  }

  private static void processSQLFiles(final Connection connection, final Element moduleRoot,
      final String[] tagsToProcess, final MetaInstructions metaInstructions) throws Exception {
    // piece de DB Builder
    DBBuilderPiece dbBuilderPiece = null;
    for (int i = 0; i < tagsToProcess.length; i++) {
      int nbFiles = 0;
      // liste des pièces correspondant au i-eme tag a traiter
      final List<Element> listeTags = (List<Element>) moduleRoot.getChildren(tagsToProcess[i]);
      for (Element eltTag : listeTags) {
        final String nomTag = eltTag.getName();
        // -------------------------------------
        // TRAITEMENT DES PIECES DE TYPE DB
        // -------------------------------------
        final List<Element> listeRowFiles = (List<Element>) eltTag.getChildren(ROW_TAG);
        for (Element eltFile : listeRowFiles) {
          String name = eltFile.getAttributeValue(FILENAME_ATTRIB);
          String value = eltFile.getAttributeValue(FILETYPE_ATTRIB);
          Integer order = new Integer(eltFile.getAttributeValue(DBORDER_ATTRIB));
          String delimiter = eltFile.getAttributeValue(FILEDELIMITER_ATTRIB);
          String skeepdelimiter = eltFile.getAttributeValue(FILEKEEPDELIMITER_ATTRIB);
          String dbprocname = eltFile.getAttributeValue(FILEDBPROCNAME_ATTRIB);
          boolean keepdelimiter = "YES".equals(skeepdelimiter);
          console.printMessage('\t' + tagsToProcess[i] + " : internal-id : " + name + "\t type : "
              + value);
          nbFiles++;
          if (FILEATTRIBSTATEMENT_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderSingleStatementPiece(console, name, name + '(' + order
                + ')', nomTag, order.intValue(), params.isVerbose());
          } else if (FILEATTRIBSEQUENCE_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderMultipleStatementPiece(console, name, name + '(' + order
                + ')', nomTag, order.intValue(), params.isVerbose(), delimiter, keepdelimiter);
          } else if (FILEATTRIBDBPROC_VALUE.equals(value)) {
            // piece de type Database Procedure
            dbBuilderPiece = new DBBuilderDBProcPiece(console, name, name + '(' + order + ')',
                nomTag, order.intValue(), params.isVerbose(), dbprocname);

          }
          if (null != dbBuilderPiece) {
            dbBuilderPiece.executeInstructions(connection);
          }
        }

        // -------------------------------------
        // TRAITEMENT DES PIECES DE TYPE FICHIER
        // -------------------------------------
        final List<Element> listeFiles = (List<Element>) eltTag.getChildren(FILE_TAG);
        for (Element eltFile : listeFiles) {
          String name = getCleanPath(eltFile.getAttributeValue(FILENAME_ATTRIB));
          String value = eltFile.getAttributeValue(FILETYPE_ATTRIB);
          String delimiter = eltFile.getAttributeValue(FILEDELIMITER_ATTRIB);
          String skeepdelimiter = eltFile.getAttributeValue(FILEKEEPDELIMITER_ATTRIB);
          String dbprocname = eltFile.getAttributeValue(FILEDBPROCNAME_ATTRIB);
          boolean keepdelimiter = (null != skeepdelimiter && skeepdelimiter.equals("YES"));
          String classname = eltFile.getAttributeValue(FILECLASSNAME_ATTRIB);
          String methodname = eltFile.getAttributeValue(FILEMETHODNAME_ATTRIB);
          console.printMessage('\t' + tagsToProcess[i] + " : name : " + name + "\t type : " + value);
          nbFiles++;
          if (FILEATTRIBSTATEMENT_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderSingleStatementPiece(console, Configuration.
                getPiecesFilesDir() + separatorChar + name, nomTag, params.isVerbose());
          } else if (FILEATTRIBSEQUENCE_VALUE.equals(value)) {
            dbBuilderPiece = new DBBuilderMultipleStatementPiece(console, Configuration.
                getPiecesFilesDir() + separatorChar + name, nomTag, params.isVerbose(), delimiter,
                keepdelimiter);
          } else if (FILEATTRIBDBPROC_VALUE.equals(value)) {
            // piece de type Database Procedure
            dbBuilderPiece = new DBBuilderDBProcPiece(console, Configuration.getPiecesFilesDir()
                + separatorChar + name, nomTag, params.isVerbose(), dbprocname);
          } else if (FILEATTRIBJAVALIB_VALUE.equals(value)) {
            // piece de type Java invoke
            dbBuilderPiece = new DBBuilderDynamicLibPiece(console, Configuration.getPiecesFilesDir()
                + separatorChar + name, nomTag, params.isVerbose(), classname, methodname);
          }
          if (null != dbBuilderPiece) {
            dbBuilderPiece.executeInstructions(connection);
          }
        }
      }
      if (0 == nbFiles) {
        console.printMessage('\t' + tagsToProcess[i] + " : (none)");
      }
    }
    final List<SQLInstruction> sqlMetaInstructions = metaInstructions.getInstructions(
        moduleRoot.getAttributeValue(DBXmlDocument.ATT_MODULE_ID));
    // Mise à jour des versions en base
    if (sqlMetaInstructions.isEmpty()) {
      console.printMessage("\tdbbuilder meta base maintenance : (none)");
    } else {
      console.printMessage("\tdbbuilder meta base maintenance :");
      for (SQLInstruction instruction : sqlMetaInstructions) {
        instruction.execute(connection);
      }
    }
  }

  protected static void cacheIntoDb(Connection connection, List<FileInformation> informations)
      throws Exception {
    // ------------------------------------------------------
    // ETAPE 2 : CACHE EN BASE DES PROCESS DE DESINSTALLATION
    // ------------------------------------------------------
    console.printMessage(System.getProperty("line.separator") + "Uninstall stored parts are :");
    String[] tagsToProcessU = TAGS_TO_MERGE_4_UNINSTALL;
    for (FileInformation information : informations) {
      String pName = information.getSrPackage();
      DBXmlDocument xFile = information.getDocument();
      // Get the root element
      Element rootU = xFile.getDocument().getRootElement();
      int nbFilesU = 0;
      // piece de DB Builder
      DBBuilderPiece pU;
      for (int i = 0; i < tagsToProcessU.length; i++) {
        // liste des pièces correspondant au i-eme tag a traiter
        List<Element> listeTagsCU = rootU.getChildren(DBBuilderFileItem.CURRENT_TAG);
        for (Element eltTagCU : listeTagsCU) {
          List listeTagsU = eltTagCU.getChildren(tagsToProcessU[i]);
          Iterator iterTagsU = listeTagsU.iterator();
          while (iterTagsU.hasNext()) {
            Element eltTagU = (Element) iterTagsU.next();
            List listeFilesU = eltTagU.getChildren(DBBuilderFileItem.FILE_TAG);
            Iterator iterFilesU = listeFilesU.iterator();
            int iFile = 1;
            while (iterFilesU.hasNext()) {
              Element eltFileU = (Element) iterFilesU.next();
              String nameU = getCleanPath(eltFileU.getAttributeValue(
                  DBBuilderFileItem.FILENAME_ATTRIB));
              String valueU = eltFileU.getAttributeValue(DBBuilderFileItem.FILETYPE_ATTRIB);
              String delimiterU = eltFileU.getAttributeValue(DBBuilderFileItem.FILEDELIMITER_ATTRIB);
              String skeepdelimiterU = eltFileU.getAttributeValue(
                  DBBuilderFileItem.FILEKEEPDELIMITER_ATTRIB);
              String dbprocnameU = eltFileU.getAttributeValue(
                  DBBuilderFileItem.FILEDBPROCNAME_ATTRIB);
              boolean keepdelimiterU = (null != skeepdelimiterU && skeepdelimiterU.equals("YES"));
              console.printMessage('\t' + tagsToProcessU[i] + " : name : " + nameU + "\t type : "
                  + valueU);
              if (valueU.equals(FILEATTRIBSTATEMENT_VALUE)) {
                // piece de type Single Statement
                pU = new DBBuilderSingleStatementPiece(console, Configuration.getPiecesFilesDir()
                    + separatorChar + nameU, tagsToProcessU[i], params.isVerbose());
                pU.cacheIntoDB(connection, pName, iFile);
              } else if (valueU.equals(FILEATTRIBSEQUENCE_VALUE)) {
                // piece de type Single Statement
                pU = new DBBuilderMultipleStatementPiece(console, Configuration.getPiecesFilesDir()
                    + separatorChar + nameU, tagsToProcessU[i], params.isVerbose(), delimiterU,
                    keepdelimiterU);
                pU.cacheIntoDB(connection, pName, iFile);
              } else if (valueU.equals(FILEATTRIBDBPROC_VALUE)) {
                // piece de type Database Procedure
                pU = new DBBuilderDBProcPiece(console, Configuration.getPiecesFilesDir()
                    + separatorChar + nameU, tagsToProcessU[i], params.isVerbose(), dbprocnameU);
                pU.cacheIntoDB(connection, pName, iFile);
              }
              iFile++;
              nbFilesU++;
            }
          }
        }
        if (0 == nbFilesU) {
          console.printMessage('\t' + tagsToProcessU[i] + " : (none)");
        }
      }
    }
  }

  private static String getCleanPath(String name) {
    String path = name.replace('/', separatorChar);
    return path.replace('\\', separatorChar);
  }

  private static DBXmlDocument loadMasterContribution(File dirXml) throws IOException,
      AppBuilderException {
    DBXmlDocument destXml = new DBXmlDocument(dirXml, MASTER_DBCONTRIBUTION_FILE);
    destXml.setOutputEncoding(CharEncoding.UTF_8);
    if (!destXml.getPath().exists()) {
      destXml.getPath().createNewFile();
      BufferedWriter destXmlOut = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(destXml.getPath(), false), Charsets.UTF_8));
      try {
        destXmlOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        destXmlOut.newLine();
        destXmlOut.write("<allcontributions>");
        destXmlOut.newLine();
        destXmlOut.write("</allcontributions>");
        destXmlOut.newLine();
        destXmlOut.flush();
      } finally {
        IOUtils.closeQuietly(destXmlOut);
      }
    }
    destXml.load();
    return destXml;
  }

  

  private DBBuilder() {
  }
}
