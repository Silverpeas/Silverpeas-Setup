/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.helpbuilder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.silverpeas.installedtree.DirectoryLocator;
import org.silverpeas.version.ApplicationInfo;
import org.silverpeas.version.JobBoxInfo;
import org.silverpeas.version.PackageInfo;

public class TemplateBasedBuilder {

  protected static final int BUFSIZE = 16384;
  protected char[] data = new char[BUFSIZE];

  private String masterTemplateFileName = null;
  public static final String masterTemplateSuffix = ".master";
  public static final String boxesContentsPlaceHolder = "@boxes-contents@";

  private String boxTemplateFileName = null;
  public static final String boxTemplateSuffix = ".box";
  public static final String boxNamePlaceHolder = "@box-name@";
  public static final String boxSpecialNamePlaceHolder = "@box-specialname@";
  public static final String packagesContentsPlaceHolder = "@packages-contents@";

  private String packageTemplateFileName = null;
  public static final String packageTemplateSuffix = ".package";
  public static final String packageNamePlaceHolder = "@package-name@";
  public static final String packageShortNamePlaceHolder = "@package-shortname@";
  public static final String packageDisplayNamePlaceHolder = "@package-displayname@";
  public static final String packageVersionPlaceHolder = "@package-version@";
  public static final String packageBuildPlaceHolder = "@package-build@";
  public static final String packageReleasePlaceHolder = "@package-release@";
  public static final String packageKeyPlaceHolder = "@package-key@";

  private static String propertiesPath = null;

  private String packageTemplate = null;
  private String boxTemplate = null;
  private String masterTemplate = null;

  private String targetFileName = null;
  public String targetFileContents = null;

  public TemplateBasedBuilder(String _targetFileName) {
    targetFileName = _targetFileName;
    masterTemplateFileName = _targetFileName + masterTemplateSuffix;
    boxTemplateFileName = _targetFileName + boxTemplateSuffix;
    packageTemplateFileName = _targetFileName + packageTemplateSuffix;
  }

  public String getTargetFileName() {
    return targetFileName;
  }

  public void writeInDirectory(String _targetDirectory) throws IOException,
      Exception {
    Reader srcText = new StringReader(getTargetContents());
    FileWriter out = new FileWriter(new File(_targetDirectory, targetFileName));
    int charsRead;
    while ((charsRead = srcText.read(data, 0, BUFSIZE)) > 0) {
      out.write(data, 0, charsRead);
    }
    out.close();
    srcText.close();
  }

  public String getTargetContents() throws IOException, Exception {

    if (targetFileContents == null) {

      Iterator iBox = ApplicationInfo.getJobBoxes().iterator();
      Iterator iPackage = null;
      JobBoxInfo boxInfo = null;
      PackageInfo packInfo = null;
      String master = null;
      String tmpBoxes = "";
      String tmpStr = null;

      while (iBox.hasNext()) {
        boxInfo = (JobBoxInfo) iBox.next();
        iPackage = boxInfo.getPackages().iterator();
        String tmpPackages = "";

        while (iPackage.hasNext()) {
          packInfo = (PackageInfo) iPackage.next();
          if (isPackagePertinent(packInfo)) {
            tmpPackages += getPackageContents(packInfo);
          }
        } // while (packages)

        if (!tmpPackages.trim().equals("")) {
          tmpStr = getBoxTemplate();
          tmpStr = setTemplateValue(tmpStr, boxNamePlaceHolder, boxInfo
              .getName());

          tmpStr = setTemplateValue(tmpStr, packagesContentsPlaceHolder,
              tmpPackages);
          tmpBoxes += tmpStr;
        }

      } // while (boxes)

      master = getMasterTemplate();

      targetFileContents = setTemplateValue(master, boxesContentsPlaceHolder,
          tmpBoxes);
    } // if

    return targetFileContents;
  }

  protected boolean isPackagePertinent(PackageInfo packInfo) throws Exception {
    return true;
  }

  protected String getPackageContents(PackageInfo packInfo) throws Exception {
    String tmpStr = getPackageTemplate();
    // NAME
    tmpStr = setTemplateValue(tmpStr, packageNamePlaceHolder, packInfo
        .getName());
    // DISPLAY NAME
    String displayName = null;
    if (packInfo.getDisplayName().trim().equals("")) {
      displayName = packInfo.getName();
    } else {
      displayName = packInfo.getDisplayName();
    }
    tmpStr = setTemplateValue(tmpStr, packageDisplayNamePlaceHolder,
        displayName);
    // SHORT NAME
    String shortName = null;
    if (packInfo.getShortName().trim().equals("")) {
      shortName = packInfo.getName();
    } else {
      shortName = packInfo.getShortName();
    }
    tmpStr = setTemplateValue(tmpStr, packageShortNamePlaceHolder, shortName);
    // VERSION
    tmpStr = setTemplateValue(tmpStr, packageVersionPlaceHolder, packInfo
        .getVersion());
    // BUILD
    tmpStr = setTemplateValue(tmpStr, packageBuildPlaceHolder, packInfo
        .getBuildTag());
    // RELEASE
    tmpStr = setTemplateValue(tmpStr, packageReleasePlaceHolder, packInfo
        .getReleaseId());
    // KEY
    tmpStr = setTemplateValue(tmpStr, packageKeyPlaceHolder, packInfo
        .getLicenceKey());
    return tmpStr;
  }

  private String getPackageTemplate() throws Exception {
    if (packageTemplate == null) {
      packageTemplate = getTemplate(packageTemplateFileName);
    }
    return new String(packageTemplate);
  }

  protected String getBoxTemplate() throws Exception {
    if (boxTemplate == null) {
      boxTemplate = getTemplate(boxTemplateFileName);
    }
    return new String(boxTemplate);
  }

  protected String getMasterTemplate() throws Exception {
    if (masterTemplate == null) {
      masterTemplate = getTemplate(masterTemplateFileName);
    }
    return new String(masterTemplate);
  }

  protected String setTemplateValue(String template, String placeHolder,
      String value) {
    String templateBuf = template;
    StringBuffer buf = new StringBuffer(templateBuf);
    int iPlaceHolder = templateBuf.indexOf(placeHolder);
    while (iPlaceHolder != -1) {
      buf.replace(iPlaceHolder, iPlaceHolder + placeHolder.length(), value);
      templateBuf = buf.toString();
      buf = new StringBuffer(templateBuf);
      iPlaceHolder = templateBuf.indexOf(placeHolder);
    }
    return templateBuf;
  }

  private String getTemplate(String templateFileName) throws Exception {
    File templateFile = new File(getMyPropertiesPath(), templateFileName);
    long templateFileSize = templateFile.length();
    if (templateFileSize > Integer.MAX_VALUE) {
      throw new Exception("the template file " + templateFileName
          + " is too big for a string");
    }
    byte[] data = new byte[(int) templateFile.length()];
    DataInputStream in = new DataInputStream(new FileInputStream(templateFile));
    in.readFully(data);
    return new String(data);
  }

  private String getMyPropertiesPath() {
    if (propertiesPath == null) {
      propertiesPath = DirectoryLocator.getPropertiesHome() + File.separator
          + getClass().getPackage().getName().replace('.', File.separatorChar);
    }
    return propertiesPath;
  }

}
