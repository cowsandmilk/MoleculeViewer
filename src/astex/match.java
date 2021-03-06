/*
 * This file is part of MoleculeViewer.
 *
 * MoleculeViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoleculeViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MoleculeViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex;

/* Matcher - Classes for matching and comparing (String) values.
 * 
 * Copyright (C) 1999  Oskar Liljeblad
 * Copyright (C) 1998  Timothy Gerard Endres (matchExprRecursor)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA  02111-1307  USA.
 *
 */

/** Instances of this class represents a wildcard expression,
 *  which can be matched against String objects.
 *
 *  This class also provides static methods for matching wildcard
 *  expressions.
 *
 *  The following matching "features" are supported:
 *  <p>
 *  <ul>
 *  <li>Asterisk (`*') for matching zero or more characters.
 *  <li>Question match (`?') for matching any single character.
 *  <li>Brackets (`[]') for matching any listed character.
 *      Character ranges with `-' are allowed. Trying to match
 *      `-', `[', or ']' in ranges is not recommended (arbitrary
 *      results). Characters cannot be escaped within brackets.
 *  <li>Escaping characters with backslash (`\'), removing their
 *      special meaning.
 *  </ul>
 *
 *  @author Timothy Gerard Endres (matchExprRecursor method)
 *  @author Oskar Liljeblad (everything else)
 */
class match {
    /** This flag represents the asterisk (`*') wildcard character. */
    private static final int ASTERISK = 1 << 0;
    /** This flag represents the question mark (`?') wildcard
     *  character.
     */
    private static final int QUESTION_MARK = 1 << 1;
    /** This flag represents the bracket (`[..]') matching feature. */
    private static final int BRACKETS = 1 << 2;
    /** This flag represents escaping feature, using backslash (`\'). */
    private static final int BACKSLASH = 1 << 3;
    /** The default matching constructs (<tt>ASTERISK</tt>,
     *  <tt>QUESTION_MARK</tt>, and <tt>BRACKETS</tt>).
     */
    private static final int DEFAULT = ASTERISK|QUESTION_MARK|BRACKETS|BACKSLASH;

    /** Return true if string matches pattern.  */
    public static boolean matches(String pattern, String string) {
        return matchExprRecursor(string, pattern, 0, 0, DEFAULT);
    }

    /**
     * An internal routine to implement expression matching.
     * This routine is based on a self-recursive algorithm.
     *
     * <p>
     * This method was copied from
     * {@link org.gjt.util.Globber org.gjt.util.Globber},
     * which is copyright (c) 1998 by Timothy Gerard Endres.
     * The method has been reformatted and slightly modified.
     *
     * <p>
     * @param string The string to be compared.
     * @param pattern The expression to compare <em>string</em> to.
     * @param sIdx The index of where we are in <em>string</em>.
     * @param pIdx The index of where we are in <em>pattern</em>.
     * @return <tt>true</tt> if <em>string</em> matched pattern, else false.
     */
    private static boolean matchExprRecursor(String string, String pattern,
            int sIdx, int pIdx, int types) {
        int pLen = pattern.length();
        int sLen = string.length();

        while (true) {
            if (pIdx >= pLen) {
                return (sIdx >= sLen);
            }
            if (sIdx >= sLen && pattern.charAt(pIdx) != '*') {
                return false;
            }

            /* Check for a '*' as the next pattern char.
             * This is handled by a recursive call for
             * each postfix of the name.
             */
            if ((types & ASTERISK) != 0 && pattern.charAt(pIdx) == '*') {
                pIdx++;

                if (pIdx >= pLen)
                    return true;

                while (true) {
                    if (matchExprRecursor(string, pattern, sIdx, pIdx, types))
                        return true;
                    if (sIdx >= sLen)
                        return false;

                    sIdx++;
                }
            }

            /* Check for '?' as the next pattern char.
             * This matches the current character.
             */
            if ((types & QUESTION_MARK) != 0 && pattern.charAt(pIdx) == '?') {
                pIdx++;
                sIdx++;
                continue;
            }

            /* Check for '[' as the next pattern char.
             * This is a list of acceptable characters,
             * which can include character ranges.
             */
            if ((types & BRACKETS) != 0 && pattern.charAt(pIdx) == '[') {
                for (pIdx++ ; ; pIdx++) {
                    if (pIdx >= pLen || pattern.charAt(pIdx) == ']')
                        return false;

                    if (pattern.charAt(pIdx) == string.charAt(sIdx)) {
                        /* Increase so that the for loop below won't stop at
                         * `]' of this is what we matched.
                         *                                  - Oskar
                         */
                        pIdx++;
                        break;
                    }

                    /* If `]' was the first matching character
                     *
                     */
                    if (pIdx < (pLen-1) && pattern.charAt(pIdx+1) == '-') {
                        
                        if (pIdx >= (pLen-2) || pattern.charAt(pIdx+2) == ']')
                            return false;

                        char chStr = string.charAt(sIdx);
                        char chPtn = pattern.charAt(pIdx);
                        char chPtn2 = pattern.charAt(pIdx+2);

                        if ((chPtn <= chStr) && (chPtn2 >= chStr))
                            break;
                        if ((chPtn >= chStr) && (chPtn2 <= chStr))
                            break;

                        pIdx += 2;
                    }
                }

                for ( ; pattern.charAt(pIdx) != ']' ; pIdx++) {
                    if (pIdx >= pLen) {
                        pIdx--;
                        break;
                    }
                }

                pIdx++;
                sIdx++;
                continue;
            }

            /* Check for backslash escapes
             * We just skip over them to match the next char.
             */
            if ((types & BACKSLASH) != 0 && pattern.charAt(pIdx) == '\\') {
                pIdx++;
                if (pIdx >= pLen)
                    return false;
            }

            if(pIdx < pLen && sIdx < sLen &&
               pattern.charAt(pIdx) != string.charAt(sIdx))
                return false;

            pIdx++;
            sIdx++;
        }
    }
}
