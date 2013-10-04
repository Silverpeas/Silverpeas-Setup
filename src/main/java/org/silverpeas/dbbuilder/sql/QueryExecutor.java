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
package org.silverpeas.dbbuilder.sql;

import org.apache.commons.dbutils.DbUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ehugonnet
 */
public class QueryExecutor {

  public static void executeUpdate(Connection connection, String query) throws SQLException {
    Statement stmt = connection.createStatement();
    try {
      stmt.executeUpdate(query);
    } catch (SQLException e) {
      throw e;
    } finally {
      DbUtils.close(stmt);
    }
  }

  public static void executeProcedure(Connection connection, String procedureName,
      DbProcParameter[] dbProcParameters) throws SQLException {
    StringBuilder preparedStatement;
    int i;
    if (dbProcParameters == null || dbProcParameters.length == 0) {
      preparedStatement = new StringBuilder("{call " + procedureName + '}');
    } else {
      preparedStatement = new StringBuilder("{call " + procedureName + '(');
      for (i = 0; i < dbProcParameters.length; i++) {
        preparedStatement.append('?');
        if (i != (dbProcParameters.length - 1)) {
          preparedStatement.append(',');
        }
      }
      preparedStatement.append(")}");
    }
    CallableStatement call = connection.prepareCall(preparedStatement.toString());
    if (dbProcParameters != null) {
      for (i = 0; i < dbProcParameters.length; i++) {
        DbProcParameter dbPP = dbProcParameters[i];
        call.setObject(i + 1, dbPP.getParameterValue(), dbPP.getParameterType());
        if (dbPP.getIsOutParameter()) {
          call.registerOutParameter(i + 1, dbPP.getParameterType());
        }
      }
    }
    call.execute();
    if (dbProcParameters != null) {
      for (i = 0; i < dbProcParameters.length; i++) {
        if (dbProcParameters[i].getIsOutParameter()) {
          dbProcParameters[i].setParameterValue(call.getObject(i + 1));
        }
      }
    }
    call.close();
  }

  public static List<Map<String, Object>> executeLoopQuery(Connection connection, String query,
      Object[] parameters) throws Exception {
    Statement stmt = null;
    PreparedStatement pstmt = null;
    ArrayList array = new ArrayList();
    ResultSet results = null;
    try {
      if (parameters == null) {
        stmt = connection.createStatement();
        results = stmt.executeQuery(query);
      } else {
        pstmt = connection.prepareStatement(query);
        for (int i = 0; i < parameters.length; i++) {
          pstmt.setObject(i + 1, parameters[i]);
        }
        results = pstmt.executeQuery();
      }
      ResultSetMetaData meta = results.getMetaData();
      // Tant qu'on a des enregistrements dans le result set
      while (results.next()) {
        // Stockage d'un enregistrement
        HashMap<String, Object> h = new HashMap<String, Object>(meta.getColumnCount());
        // Pour chaque colonne du result set
        for (int i = 1; i <= meta.getColumnCount(); i++) {
          Object ob = results.getObject(i);
          h.put(meta.getColumnLabel(i).toUpperCase(), ob);
        }
        array.add(h);
      }
    } catch (SQLException sqlex) {
    } finally {
      DbUtils.closeQuietly(results);
      DbUtils.closeQuietly(stmt);
      DbUtils.closeQuietly(pstmt);
    }
    return array;
  }

  private QueryExecutor() {
  }
}
