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
package org.silverpeas.util;

public class StringUtil {

  public static final int RECURSION_THRESHOLD = 10;

  /**
   * Search a string for all instances of a substring and replace it with another string. Amazing
   * that this is not a method of java.lang.String since I use it all the time.
   * @param search Substring to search for
   * @param replace String to replace it with
   * @param source String to search through
   * @return The source with all instances of <code>search</code> replaced by <code>replace</code>
   */
  public static String sReplace(String search, String replace, String source) {
    String returnString;
    String origSource = source;
    int spot = source.indexOf(search);
    if (-1 < spot) {
      returnString = "";
    } else {
      returnString = source;
    }
    while (-1 < spot) {
      if (spot == source.length() + 1) {
        returnString = returnString + source.substring(0, source.length() - 1).concat(replace);
        source = "";
      } else if (0 < spot) {
        returnString = returnString + source.substring(0, spot) + replace;
        source = source.substring(spot + search.length(), source.length());
      } else {
        returnString = returnString + replace;
        source = source.substring(spot + search.length(), source.length());
      }
      spot = source.indexOf(search);
    }
    if (!source.equals(origSource)) {
      return returnString + source;
    } else {
      return returnString;
    }
  }

  /**
   * Match a file glob style expression without ranges. '*' matches zero or more chars. '?' matches
   * any single char.
   * @param pattern A glob-style pattern to match
   * @param input The string to match
   * @return whether or not the string matches the pattern.
   */
  public static boolean match(String pattern, String input) {
    int patternIndex = 0;
    int inputIndex = 0;
    int patternLen = pattern.length();
    int inputLen = input.length();
    int[] stack = new int[100];
    int stacktop = 0;

    for (;;) {
      if (patternIndex == patternLen) {
        if (inputIndex == inputLen) {
          return true;
        }

      } else {
        char patternChar = pattern.charAt(patternIndex);

        if (inputIndex < inputLen) {
          if ('*' == patternChar) {
            stack[stacktop++] = patternIndex;
            stack[stacktop++] = inputIndex + 1;
            patternIndex++;
            continue;

          } else if ('?' == patternChar
              || patternChar == input.charAt(inputIndex)) {
            patternIndex++;
            inputIndex++;
            continue;
          }

        } else if ('*' == patternChar) {
          patternIndex++;
          continue;
        }
      }

      if (0 == stacktop) {
        return false;
      }

      inputIndex = stack[--stacktop];
      patternIndex = stack[--stacktop];
    }
  }

  public static interface KeyFinder {

    String lookupString(String key);
  }

  /**
   * This looks up {% %} delimted keys in a string and replaces them. This is used by resource
   * catalog, TreeConfig, and several other components.
   */
  public static String lookupKeysInString(String str, KeyFinder finder) {
    return lookupKeysInString(str, 0, finder);
  }

  public static String lookupKeysInString(String str, int recurselvl,
      KeyFinder finder) {
    if (RECURSION_THRESHOLD < recurselvl) {
      throw new RuntimeException("Recursion Threshold reached");
    }

    // this is where all those years of c/c++ pay off in java
    // boolean foundkey = false;

    char[] sb = str.toCharArray();
    int len = sb.length;

    // now go through the string looking for "{%"
    StringBuffer newsb = null;

    int lastKeyEnd = 0;

    for (int i = 0; i < len; i++) {
      char c = sb[i];
      if (('{' == c) && (i + 2 < len) && ('%' == sb[i + 1])) {
        // we got a potential key

        int endkey = -1;
        StringBuilder key = new StringBuilder();
        for (int j = i + 2; j + 1 < len && 0 > endkey; j++) {
          if ('%' == sb[j] && '}' == sb[j + 1]) {
            endkey = j - 1;
          } else {
            key.append(sb[j]);
          }
        }
        if (0 < endkey) {
          String val = finder.lookupString(key.toString());
          String s = lookupKeysInString(val, recurselvl + 1, finder);
          if (null != s) {
            if (null == newsb) {
              newsb = new StringBuffer(len);
              for (int k = 0; k < i; k++) {
                newsb.append(sb[k]);
              }
            } else {
              for (int k = lastKeyEnd + 1; k < i; k++) {
                newsb.append(sb[k]);
              }
            }
            newsb.append(s);
            i = endkey + 2;
            lastKeyEnd = i;

          }
        }
      }
    }
    if (0 == lastKeyEnd && null == newsb) {
      return str;
    }
    if (0 < lastKeyEnd && lastKeyEnd + 1 < len) {
      for (int k = lastKeyEnd + 1; k < len; k++) {
        newsb.append(sb[k]);
      }
    }
    return newsb.toString();
  }

  public static boolean isDefined(String parameter) {
    return (parameter != null && !parameter.trim().isEmpty() && !"null".equalsIgnoreCase(parameter));
  }

  public static boolean isLong(String id) {
    try {
      Long.parseLong(id);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * @param text
   * @return a String with all quotes replaced by spaces
   */
  public static String escapeQuote(String text) {
    return text.replaceAll("'", " ");
  }

  private StringUtil() {
  }
}
