/**
 * Copyright (C) 2000 - 2012 Silverpeas
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
/**
 * Titre : dbBuilder Description : Builder des BDs Silverpeas Copyright : Copyright (c) 2001 Société
 * : Silverpeas
 *
 * @author ATH
 * @version 1.0 Modifications: 11/2004 - DLE - Modification ordre de passage des scripts (init après
 * contraintes)
 */
package org.silverpeas.dbbuilder;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.CharEncoding;
import org.jdom.Element;
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
import org.silverpeas.util.file.FileUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.io.File.separatorChar;
import static org.silverpeas.dbbuilder.Console.NEW_LINE;
import static org.silverpeas.dbbuilder.DBBuilderItem.*;
import static org.silverpeas.dbbuilder.util.Action.*;

/**
 * @Description :
 * @Copyright : Copyright (c) 2001
 * @Société : Silverpeas
 * @author STR
 * @version 1.0
 */
public class DBBuilder {

  public final static Date TODAY = new java.util.Date();
  // Version application
  public static final String DBBuilderAppVersion = "V5";
  // Fichier log
  protected static Console log;
  static public final String CREATE_TABLE_TAG = "create_table";
  static public final String CREATE_INDEX_TAG = "create_index";
  static public final String CREATE_CONSTRAINT_TAG = "create_constraint";
  static public final String CREATE_DATA_TAG = "init";
  static public final String DROP_TABLE_TAG = "drop_table";
  static public final String DROP_INDEX_TAG = "drop_index";
  static public final String DROP_CONSTRAINT_TAG = "drop_constraint";
  static public final String DROP_DATA_TAG = "clean";
  private static final String[] TAGS_TO_MERGE_4_INSTALL = {
      DBBuilderFileItem.CREATE_TABLE_TAG,
      DBBuilderFileItem.CREATE_INDEX_TAG,
      DBBuilderFileItem.CREATE_CONSTRAINT_TAG,
      DBBuilderFileItem.CREATE_DATA_TAG };
  private static final String[] TAGS_TO_MERGE_4_UNINSTALL = {
      DBBuilderFileItem.DROP_CONSTRAINT_TAG,
      DBBuilderFileItem.DROP_INDEX_TAG,
      DBBuilderFileItem.DROP_DATA_TAG,
      DBBuilderFileItem.DROP_TABLE_TAG };
  private static final String[] TAGS_TO_MERGE_4_ALL = {
      DBBuilderFileItem.DROP_CONSTRAINT_TAG,
      DBBuilderFileItem.DROP_INDEX_TAG,
      DBBuilderFileItem.DROP_DATA_TAG,
      DBBuilderFileItem.DROP_TABLE_TAG,
      DBBuilderFileItem.CREATE_TABLE_TAG,
      DBBuilderFileItem.CREATE_INDEX_TAG,
      DBBuilderFileItem.CREATE_CONSTRAINT_TAG,
      DBBuilderFileItem.CREATE_DATA_TAG };
  private static final String[] TAGS_TO_MERGE_4_OPTIMIZE = {
      DBBuilderFileItem.DROP_INDEX_TAG,
      DBBuilderFileItem.CREATE_INDEX_TAG };
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
    Logger.getLogger("org.springframework").setLevel(Level.SEVERE);
    new ClassPathXmlApplicationContext("classpath:/spring-jdbc-datasource.xml");
    try {
      // Ouverture des traces
      System.out.println("Start Database build using Silverpeas DBBuilder v. "
          + DBBuilderAppVersion + " (" + TODAY + ").");
      log = new Console("DBBuilder.log");
      printMessageln(NEW_LINE + "*************************************************************");
      printMessageln("Start Database Build using Silverpeas DBBuilder v. " + DBBuilderAppVersion
          + " (" + TODAY + ").");
      // Lecture des variables d'environnement à partir de dbBuilderSettings
      dbBuilderResources = FileUtil.loadResource(
          "/org/silverpeas/dbBuilder/settings/dbBuilderSettings.properties");
      // Lecture des paramètres d'entrée
      params = new CommandLineParameters(args);

      if (params.isSimulate() && DatabaseType.ORACLE == params.getDbType()) {
        throw new Exception("Simulate mode is not allowed for Oracle target databases.");
      }

      printMessageln(NEW_LINE);
      printMessageln("Parameters are :");
      printMessage(ConnectionFactory.getConnectionInfo());
      printMessageln(NEW_LINE);
      printMessageln("\tAction        : " + params.getAction());
      printMessageln("\tVerbose mode  : " + params.isVerbose());
      printMessageln("\tSimulate mode : " + params.isSimulate());
      if (Action.ACTION_CONNECT == params.getAction()) {
        // un petit message et puis c'est tout
        printMessageln(NEW_LINE);
        printMessageln("Connection to database successfull.");
        System.out.println(NEW_LINE + "Connection to database successfull.");
      } else {
        // Modules en place sur la BD avant install
        printMessageln(NEW_LINE + "DB Status before build :");
        List<String> packagesIntoDB = checkDBStatus();
        // initialisation d'un vecteur des instructions SQL à passer en fin d'upgrade
        // pour mettre à niveau les versions de modules en base
        MetaInstructions sqlMetaInstructions = new MetaInstructions();
        File dirXml = new File(params.getDbType().getDBContributionDir());
        DBXmlDocument destXml = new DBXmlDocument(dirXml, MASTER_DBCONTRIBUTION_FILE);
        if (!destXml.getPath().exists()) {
          destXml.getPath().createNewFile();
          BufferedWriter destXmlOut = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(destXml.getPath(), false), CharEncoding.UTF_8));
          destXmlOut.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
          destXmlOut.newLine();
          destXmlOut.write("<allcontributions>");
          destXmlOut.newLine();
          destXmlOut.write("</allcontributions>");
          destXmlOut.newLine();
          destXmlOut.flush();
          destXmlOut.close();
        }
        destXml.load();
        UninstallInformations processesToCacheIntoDB = new UninstallInformations();

        File[] listeFileXml = dirXml.listFiles();
        Arrays.sort(listeFileXml);

        List<DBXmlDocument> listeDBXmlDocument = new ArrayList<DBXmlDocument>(listeFileXml.length);

        // Ouverture de tous les fichiers de configurations
        printMessageln(NEW_LINE);
        printMessageln("Ignored contribution files are :");
        int ignoredFiles = 0;

        for (File xmlFile : listeFileXml) {
          if (xmlFile.isFile() && "xml".equals(FileUtil.getExtension(xmlFile))
              && !(FIRST_DBCONTRIBUTION_FILE.equalsIgnoreCase(xmlFile.getName()))
              && !(MASTER_DBCONTRIBUTION_FILE.equalsIgnoreCase(xmlFile.getName()))) {
            DBXmlDocument fXml = new DBXmlDocument(dirXml, xmlFile.getName());
            fXml.load();
            // vérification des dépendances
            // & prise en compte uniquement si dependences OK
            if (hasUnresolvedRequirements(listeFileXml, fXml)) {
              printMessageln('\t' + xmlFile.getName() + " (because of unresolved requirements).");
              ignoredFiles++;
            } else if (ACTION_ENFORCE_UNINSTALL == params.getAction()) {
              printMessageln('\t' + xmlFile.getName() + " (because of " + ACTION_ENFORCE_UNINSTALL
                  + " mode).");
              ignoredFiles++;
            } else {
              listeDBXmlDocument.add(fXml);
            }
          }
        }
        if (0 == ignoredFiles) {
          printMessageln("\t(none)");
        }

        // prépare une HashMap des modules présents en fichiers de contribution
        Map packagesIntoFile = new HashMap();
        int j = 0;
        printMessageln(NEW_LINE);
        printMessageln("Merged contribution files are :");
        printMessageln(params.getAction().toString());
        if (ACTION_ENFORCE_UNINSTALL != params.getAction()) {
          printMessageln('\t' + FIRST_DBCONTRIBUTION_FILE);
          j++;
        }
        for (DBXmlDocument currentDoc : listeDBXmlDocument) {
          printMessageln('\t' + currentDoc.getName());
          j++;
        }
        if (0 == j) {
          printMessageln("\t(none)");
        }
        // merge des diffrents fichiers de contribution éligibles :
        printMessageln(NEW_LINE);
        printMessageln("Build decisions are :");
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
          printMessageln("**** Treating " + item + " ****");
          DBBuilderDBItem tmpdbbuilderItem = new DBBuilderDBItem(item);
          mergeActionsToDo(tmpdbbuilderItem, destXml, processesToCacheIntoDB, sqlMetaInstructions);
        }

        // Trace
        destXml.setName("res.txt");
        destXml.save();

        printMessageln(NEW_LINE + "Build parts are :");

        // Traitement des pièces sélectionnées
        // remarque : durant cette phase, les erreurs sont traitées -> on les catche en
        // retour sans les retraiter
        if (ACTION_INSTALL == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_INSTALL);
        } else if (ACTION_UNINSTALL == params.getAction()
            || ACTION_ENFORCE_UNINSTALL == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_UNINSTALL);
        } else if (ACTION_OPTIMIZE == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_OPTIMIZE);
        } else if (ACTION_ALL == params.getAction()) {
          processDB(destXml, processesToCacheIntoDB, sqlMetaInstructions, TAGS_TO_MERGE_4_ALL);
        } else if (ACTION_STATUS == params.getAction()) {
          // nothing to do
        } else if (ACTION_CONSTRAINTS_INSTALL == params.getAction()) {
          // nothing to do
        } else if (ACTION_CONSTRAINTS_UNINSTALL == params.getAction()) {
          // nothing to do
        }
        // Modules en place sur la BD en final
        printMessageln(NEW_LINE + "Finally DB Status :");
        checkDBStatus();

      }

      printMessageln(NEW_LINE);
      printMessageln("Database build SUCCESSFULL (" + TODAY + ").");
      System.out.println(NEW_LINE + "Database Build SUCCESSFULL (" + TODAY + ").");

    } catch (Exception e) {
      e.printStackTrace();
      printError(e.getMessage(), e);
      printMessageln(e.getMessage());
      printMessageln(NEW_LINE);
      printMessageln("Database Build FAILED (" + TODAY + ").");
      System.out.println(NEW_LINE + "Database Build FAILED (" + TODAY + ").");
    } finally {
      log.close();
    }
  } // main

  // ---------------------------------------------------------------------
  public static void printError(String errMsg, Exception ex) {
    log.printError(errMsg, ex);
  }

  public static void printError(String errMsg) {
    log.printError(errMsg);
  }

  public static void printMessageln(String msg) {
    log.printMessageln(msg);
  }

  public static void printMessage(String msg) {
    log.printMessage(msg);
  }

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

  private static void mergeActionsToDo(DBBuilderItem pdbbuilderItem, DBXmlDocument xmlFile,
      UninstallInformations processesToCacheIntoDB, MetaInstructions sqlMetaInstructions) {

    String package_name = pdbbuilderItem.getModule();
    String versionDB;
    String versionFile;
    try {
      versionDB = pdbbuilderItem.getVersionFromDB();
      versionFile = pdbbuilderItem.getVersionFromFile();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    String[] tags_to_merge = null;
    VersionTag[] blocks_merge = null;

    if (pdbbuilderItem instanceof org.silverpeas.dbbuilder.DBBuilderFileItem) {
      DBBuilderFileItem dbbuilderItem = (DBBuilderFileItem) pdbbuilderItem;
      int iversionDB = -1;
      if (!versionDB.equals(NOTINSTALLED)) {
        iversionDB = Integer.parseInt(versionDB);
      }
      int iversionFile = Integer.parseInt(versionFile);
      if (iversionDB == iversionFile) {
        if (ACTION_INSTALL == params.getAction() || ACTION_UNINSTALL == params.getAction()
            || ACTION_STATUS == params.getAction()
            || ACTION_CONSTRAINTS_INSTALL == params.getAction()
            || ACTION_CONSTRAINTS_UNINSTALL == params.getAction()) {
          printMessageln('\t' + package_name + " is up to date with version " + versionFile + '.');
        } else {
          printMessageln('\t' + package_name + " is up to date with version " + versionFile
              + " and will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = new VersionTag[1];
          blocks_merge[0] = new VersionTag(CURRENT_TAG, versionFile);
        }
      } else if (iversionDB > iversionFile) {
        printMessageln('\t' + package_name
            + " will be ignored because this package is newer into DB than installed files.");
      } else {
        if (ACTION_INSTALL == params.getAction() || ACTION_ALL == params.getAction()
            || ACTION_STATUS == params.getAction()
            || ACTION_CONSTRAINTS_INSTALL == params.getAction()
            || ACTION_CONSTRAINTS_UNINSTALL == params.getAction()) {
          if (-1 == iversionDB) {
            printMessageln('\t' + package_name + " will be installed with version " + versionFile +
                '.');
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;
            blocks_merge = new VersionTag[1];
            blocks_merge[0] = new VersionTag(CURRENT_TAG, versionFile);
            // module nouvellement installé -> il faut stocker en base sa procedure de uninstall
            processesToCacheIntoDB.addInformation(dbbuilderItem.getModule(), package_name,
                dbbuilderItem.getFileXml());
            // inscription du module en base
            sqlMetaInstructions.addInstruction(dbbuilderItem.getModule(),
                new InstallSQLInstruction(versionFile, package_name));
          } else {
            printMessageln('\t' + package_name + " will be upgraded from " + versionDB + " to "
                + versionFile + '.');
            tags_to_merge = TAGS_TO_MERGE_4_INSTALL;

            blocks_merge = new VersionTag[iversionFile - iversionDB];
            for (int i = 0; i < iversionFile - iversionDB; i++) {
              String sversionFile = "000" + (iversionDB + i);
              sversionFile = sversionFile.substring(sversionFile.length() - 3);
              blocks_merge[i] = new VersionTag(PREVIOUS_TAG, sversionFile);
            }
            // module upgradé -> il faut stocker en base sa nouvelle procedure de uninstall
            processesToCacheIntoDB.addInformation(dbbuilderItem.getModule(), package_name,
                dbbuilderItem.getFileXml());

            // desinscription du module en base
            sqlMetaInstructions.addInstruction(dbbuilderItem.getModule(),
                new UninstallSQLInstruction(versionFile, package_name));
          }
        } else if (ACTION_OPTIMIZE == params.getAction()) {
          printMessageln('\t' + package_name + " will be optimized.");
          tags_to_merge = TAGS_TO_MERGE_4_OPTIMIZE;
          blocks_merge = new VersionTag[1];
          blocks_merge[0] = new VersionTag(CURRENT_TAG, versionFile);
        }

        // construction du xml global des actions d'upgrade de la base
        if (null != blocks_merge && null != tags_to_merge) {
          try {
            xmlFile.mergeWith(pdbbuilderItem, tags_to_merge, blocks_merge);
          } catch (Exception e) {
            printMessage("Error with " + pdbbuilderItem.getModule() + ' ' + e.getMessage());
            e.printStackTrace();
          }
        }
      }

    } else if (pdbbuilderItem instanceof org.silverpeas.dbbuilder.DBBuilderDBItem) {

      if (ACTION_UNINSTALL == params.getAction() || ACTION_ALL == params.getAction()
          || ACTION_ENFORCE_UNINSTALL == params.getAction()) {
        printMessageln('\t' + package_name + " will be uninstalled.");
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
      printMessageln("");
      printMessageln("*** AVERTISSEMENT ***");
      printMessageln("\t Le Module " + package_name
          + " est présent en BD mais n'a pas de scripts SQL fichiers");
      printMessageln("");
      System.out.println("");
      System.out.println("*** AVERTISSEMENT ***");
      System.out.println("Le Module " + package_name
          + " est présent en BD mais n'a pas de scripts SQL fichiers");
    }

  }

  private static void processDB(DBXmlDocument xmlFile,
      UninstallInformations processesToCacheIntoDB,
      MetaInstructions sqlMetaInstructions, String[] tagsToProcess) throws Exception {
    // ------------------------------------------
    // ETAPE 1 : TRAITEMENT DES ACTIONS D'UPGRADE
    // ------------------------------------------
    // Get the root element
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
          connection.rollback();
        } else {
          connection.commit();
        }
      } catch (Exception e) {
        try {
          if (null != connection) {
            connection.rollback();
          }
        } catch (SQLException sqlex) {
        }
        throw e;
      } finally {
        try {
          if (null != connection) {
            connection.close();
          }
        } catch (SQLException sqlex) {
        }
      }
    }
    printMessageln("DB Status after build :");
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
        printMessageln('\t' + srPackage + " v. " + srVersion);
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
          printMessageln('\t' + tagsToProcess[i] + " : internal-id : " + name + "\t type : "
              + value);
          nbFiles++;
          if (FILEATTRIBSTATEMENT_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderSingleStatementPiece(name, name + '(' + order + ')',
                nomTag, order.intValue(), params.isVerbose());
          } else if (FILEATTRIBSEQUENCE_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderMultipleStatementPiece(name, name + '(' + order + ')',
                nomTag, order.intValue(), params.isVerbose(), delimiter, keepdelimiter);
          } else if (FILEATTRIBDBPROC_VALUE.equals(value)) {
            // piece de type Database Procedure
            dbBuilderPiece = new DBBuilderDBProcPiece(name, name + '(' + order + ')', nomTag,
                order.intValue(), params.isVerbose(), dbprocname);

          }
          if (null != dbBuilderPiece) {
            dbBuilderPiece.setConsole(log);
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
          printMessageln('\t' + tagsToProcess[i] + " : name : " + name + "\t type : " + value);
          nbFiles++;
          if (FILEATTRIBSTATEMENT_VALUE.equals(value)) {
            // piece de type Single Statement
            dbBuilderPiece = new DBBuilderSingleStatementPiece(Configuration.getPiecesFilesDir()
                + separatorChar + name, nomTag, params.isVerbose());
          } else if (FILEATTRIBSEQUENCE_VALUE.equals(value)) {
            dbBuilderPiece =
                new DBBuilderMultipleStatementPiece(
                Configuration.getPiecesFilesDir() + separatorChar + name,
                nomTag, params.isVerbose(), delimiter, keepdelimiter);
          } else if (FILEATTRIBDBPROC_VALUE.equals(value)) {
            // piece de type Database Procedure
            dbBuilderPiece =
                new DBBuilderDBProcPiece(
                Configuration.getPiecesFilesDir() + separatorChar + name, nomTag,
                params.isVerbose(), dbprocname);
          } else if (FILEATTRIBJAVALIB_VALUE.equals(value)) {
            // piece de type Java invoke
            dbBuilderPiece =
                new DBBuilderDynamicLibPiece(
                Configuration.getPiecesFilesDir() + separatorChar + name,
                nomTag, params.isVerbose(), classname, methodname);
          }
          if (null != dbBuilderPiece) {
            dbBuilderPiece.setConsole(log);
            dbBuilderPiece.executeInstructions(connection);
          }
        }
      }
      if (0 == nbFiles) {
        printMessageln('\t' + tagsToProcess[i] + " : (none)");
      }
    }
    final List<SQLInstruction> sqlMetaInstructions = metaInstructions.getInstructions(
        moduleRoot.getAttributeValue(DBXmlDocument.ATT_MODULE_ID));
    // Mise à jour des versions en base
    if (sqlMetaInstructions.isEmpty()) {
      printMessageln("\tdbbuilder meta base maintenance : (none)");
    } else {
      printMessageln("\tdbbuilder meta base maintenance :");
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
    printMessageln(System.getProperty("line.separator") + "Uninstall stored parts are :");
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
              String delimiterU =
                  eltFileU.getAttributeValue(DBBuilderFileItem.FILEDELIMITER_ATTRIB);
              String skeepdelimiterU =
                  eltFileU.getAttributeValue(DBBuilderFileItem.FILEKEEPDELIMITER_ATTRIB);
              String dbprocnameU =
                  eltFileU.getAttributeValue(DBBuilderFileItem.FILEDBPROCNAME_ATTRIB);
              boolean keepdelimiterU = (null != skeepdelimiterU && skeepdelimiterU.equals("YES"));
              printMessageln('\t' + tagsToProcessU[i] + " : name : " + nameU + "\t type : "
                  + valueU);
              if (valueU.equals(FILEATTRIBSTATEMENT_VALUE)) {
                // piece de type Single Statement
                pU =
                    new DBBuilderSingleStatementPiece(Configuration.getPiecesFilesDir()
                    + separatorChar
                    + nameU, tagsToProcessU[i], params.isVerbose());
                pU.cacheIntoDB(connection, pName, iFile);
              } else if (valueU.equals(FILEATTRIBSEQUENCE_VALUE)) {
                // piece de type Single Statement
                pU =
                    new DBBuilderMultipleStatementPiece(Configuration.getPiecesFilesDir()
                    + separatorChar
                    + nameU, tagsToProcessU[i], params.isVerbose(), delimiterU,
                    keepdelimiterU);
                pU.cacheIntoDB(connection, pName, iFile);
              } else if (valueU.equals(FILEATTRIBDBPROC_VALUE)) {
                // piece de type Database Procedure
                pU = new DBBuilderDBProcPiece(
                    Configuration.getPiecesFilesDir() + separatorChar + nameU,
                    tagsToProcessU[i], params.isVerbose(), dbprocnameU);
                pU.cacheIntoDB(connection, pName, iFile);
              }
              iFile++;
              nbFilesU++;
            }
          }
        }
        if (0 == nbFilesU) {
          printMessageln('\t' + tagsToProcessU[i] + " : (none)");
        }
      }
    }
  }

  private static String getCleanPath(String name) {
    String path = name.replace('/', separatorChar);
    return path.replace('\\', separatorChar);
  }

  private DBBuilder() {
  }
}
