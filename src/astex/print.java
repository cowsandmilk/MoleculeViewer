/*
 * This file is part of OpenAstexViewer.
 *
 * OpenAstexViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAstexViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAstexViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex;



/**
 * print class 
 * <p>
 * This class implements f() method which allows emulation of 
 * C style printf() routine
 *
 * @author mikeh
 * @version 1.0
 */
/**
	03-01-99 tmm	made class public (required for move to packages)
						made methods public
	06-09-98 tmm	added javadoc header comments/change comment section
	03-01-98 mh		created
*/

public class print {
    /*
     * Utility functions for printing.
     */
    
    public static void f(String output){
        System.out.println(output);
    }
}
