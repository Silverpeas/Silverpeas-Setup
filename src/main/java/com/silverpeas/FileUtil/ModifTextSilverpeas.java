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
 * FLOSS exception.  You should have recieved a copy of the text describing
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
package com.silverpeas.FileUtil;

/**
 * Titre : modification de fichier txt
 * Description :modification de fichier txt propre a silverpeas : remplace la valeur de la cle par la valeur souhaitee
 * Copyright :    Copyright (c) 2001
 * Soci�t� :
 * @author STR
 * @version 1.0
 */

import java.io.*;
import java.io.Reader.*;
import java.util.*;


public class ModifTextSilverpeas extends ModifFile {


/**
 * @constructor prend en param�tre le chemin du fichier � modifier
 */
  public ModifTextSilverpeas(String path) throws Exception{
	super.setPath(path);
  }

/**
 * ajoute des modifications sous forme de tableau de chaine "key=replace"
 */
  public void setModif(String str[]) throws Exception{
	  for (int i=0;i<str.length;i++){
		this.addModification(str[i]);
	  }
  }

/**
 * analyse une ligne du fichier
 */
  protected void analyseLigne(DataOutput out,String pBuf) throws Exception {

	String tmpStr = pBuf.trim();
	ElementModif em;

	Iterator i = listeModifications.iterator();

	while (i.hasNext()){
	  em=((ElementModif)i.next());
	  int egal = tmpStr.indexOf("=");
	  //if (tmpStr.startsWith(em.getSearch())){
	  if ((egal!=-1) && (tmpStr.substring(0, egal).trim().equalsIgnoreCase(em.getSearch()))) {

		// ici nouveaut� t003 : avant, la substitution n'�tait pas op�r�e si valeur avant �tait vide :
		// if ((egal!=-1) && (egal+1!=tmpStr.length()) && (!tmpStr.substring(egal+1, tmpStr.length()).trim().equalsIgnoreCase(em.getModif())) ) {
		// maintenant, on op�re m�me si val d'origine est vide
		if ((egal!=-1) && (!tmpStr.substring(egal+1, tmpStr.length()).trim().equalsIgnoreCase(em.getModif())) ) {

			if (!isModified) {
				isModified=true;
				BackupFile bf = new BackupFile(new File(path));
				bf.makeBackup();
			}
			tmpStr = new String(em.getSearch() + "=" + em.getModif());
		}
	  }
	}
	try{
		out.writeBytes(tmpStr);
		out.writeBytes(System.getProperty("line.separator"));
	} catch (Exception e) {
	  throw new Exception("erreur lors de la modification du fichier :" + path);
	}
  }

  /**
   * lance la modification du fichier Attention la modification s'effectue par ligne du fichier
   */
  public void executeModification() throws Exception{

	DataInput  dataInput;
	DataOutput  dataOutput;
	String line;

	File tmpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "ModifText.sh");
	File inFile = new File(path);

	FileInputStream dis=new FileInputStream(path);
	FileOutputStream dos=new FileOutputStream(tmpFile);

	dataInput= new DataInputStream(dis);
	dataOutput = new DataOutputStream(dos);

	while((line=dataInput.readLine())!=null){
	  analyseLigne(dataOutput,line);
	}

	dis.close();
	dos.close();
	inFile.delete();
        FileUtil.copyFile(tmpFile, inFile);
        tmpFile.delete();

        //Specifique Linux/Solaris: on met le fichier de script en executable
        if (path.endsWith(".sh") || path.endsWith(".csh") || path.endsWith(".ksh"))
        {
          String[] commande = new String[3];
          commande[0] = "/bin/chmod";
          commande[1] = "755";
          commande[2] = new String (path);
          Runtime.getRuntime().exec (commande);
        }
  }



/*
  public static void main(String[] args){
	  ModifText mp ;
	  String[] strs = {"comments=ouais","beanId=12385"};
	  System.out.println("test des modification de fichier XML");
	  try{
		mp = new ModifText("c:\\toto\\SilverpeasSettings.xml");
		mp.createFileBak(null);
		mp.addModification("%AUTHENTIFICATION%","c:\toto\titi");
		mp.executeModification();


	  }catch(Exception e){
		e.printStackTrace();

	  }
  }
*/


}