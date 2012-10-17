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

package org.silverpeas.version;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class JobBoxInfo implements Comparable {

  private static final String busBoxName = "Job'Bus & Job'Manager";

  private String name = null;
  private Set myPackages = null;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof JobBoxInfo)) {
      return false;
    }
    return getName().equalsIgnoreCase(((JobBoxInfo) o).getName());
  }

  @Override
  public int compareTo(Object o) {
    if (!(o instanceof JobBoxInfo)) {
      return -1;
    }
    if (getName().equalsIgnoreCase(busBoxName)) {
      if (equals(o)) {
        return 0;
      } else {
        return -1;
      }
    }
    JobBoxInfo jbo = (JobBoxInfo) o;
    if (getName().equals("")) {
      if (jbo.getName().equalsIgnoreCase(busBoxName)) {
        return 1;
      } else if (equals(o)) {
        return 0;
      } else {
        return -1;
      }
    }
    return getName().compareTo(jbo.getName());
  }

  public JobBoxInfo(String _name, Map _packages) {
    setName(_name);
    setPackages(_packages);
  }

  private void setName(String _name) {
    name = _name;
  }

  private void setPackages(Map _packages) {
    myPackages = new TreeSet();
    Iterator iPackage = _packages.values().iterator();
    PackageInfo info = null;
    while (iPackage.hasNext()) {
      info = (PackageInfo) iPackage.next();
      if (info.getBoxName().equals(getName())) {
        myPackages.add(info);
      }
    }
  }

  public String getName() {
    return name;
  }

  public Set getPackages() {
    return myPackages;
  }
}