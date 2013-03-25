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
package org.silverpeas.passwordencryption;

import org.apache.commons.dbutils.DbUtils;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;
import org.silverpeas.util.file.FileUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.silverpeas.dbbuilder.dbbuilder_dl.DbBuilderDynamicPart;

/**
 * Loops over the users in the Silverpeas domain and for each encrypts their password with the last
 * cryptographic algorithm in use in Silverpeas to encrypt the passwords of users in SQL domains.
 * @author mmoquillon
 */
public class UserPasswordEncryption extends DbBuilderDynamicPart {

  private static final String PASSWORD_UPDATE =
      "UPDATE DomainSP_User SET password = ? WHERE id = ?";
  private final CryptographicFunction crypt = new Sha512Crypt();

  public UserPasswordEncryption() {
  }

  public void run() throws Exception {
    Connection connection = this.getConnection();
    ResultSet rs = null;
    Statement stmt = null;
    PreparedStatement stmtUpdate = null;
    String sClearPass;

    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT * FROM DomainSP_User");
      while (rs.next()) {
        sClearPass = rs.getString("password");
        if (sClearPass == null) {
          sClearPass = "";
        }
        stmtUpdate = connection.prepareStatement(PASSWORD_UPDATE);
        stmtUpdate.setString(1, crypt.encrypt(sClearPass));
        stmtUpdate.setInt(2, rs.getInt("id"));
        stmtUpdate.executeUpdate();
        stmtUpdate.close();
        stmtUpdate = null;
      } // while
    } catch (SQLException ex) {
      throw new Exception("Error during password encryption: " + ex.getMessage());

    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
      DbUtils.closeQuietly(stmtUpdate);
    }
  }
}
