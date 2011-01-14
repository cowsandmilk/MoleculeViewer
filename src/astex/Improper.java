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

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 15-12-99 mjh
 *	created
 */

/**
 * A class for storing information about an improper torsion torsion.
 */
public class Improper {
	/** Default constructor. */
	public Improper(Atom a1, Atom a2, Atom a3, Atom a4){
	    firstAtom = a1;
	    secondAtom = a2;
	    thirdAtom = a3;
	    fourthAtom = a4;
	}

	/**
	 * Initialise an improper.
	 *
	 * This method is called to reset a improper that is reused,
	 * so all fields must get initialised here.
	 */

	/** The first atom in the improper. */
	private Atom firstAtom;

	/** The second atom in the improper. */
	private Atom secondAtom;

	/** The third atom in the improper. */
	private Atom thirdAtom;

	/** The fourth atom in the improper. */
	private Atom fourthAtom;

	/** Get the specified atom. */
	public Atom getAtom(int index){
		if(index == 0){
			return firstAtom;
		}else if(index == 1){
			return secondAtom;
		}else if(index == 2){
			return thirdAtom;
		}else if(index == 3){
			return fourthAtom;
		}else{
			return null;
		}
	}
}

