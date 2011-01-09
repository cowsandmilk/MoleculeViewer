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


/*
	AtomicElement class

	used by PTable class for storing element info
*/

public class AtomicElement {
	/** The maximum number of valence states that any of our atoms exist in. */
	
	public String symbol;

	public int atomicNumber;
	public double mass;

	public AtomicElement(int an, String s, double m){
		atomicNumber = an;
		symbol = s;
		
		mass = m;
	}
}
