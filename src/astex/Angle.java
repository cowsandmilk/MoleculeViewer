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
 * 14-12-99 mjh
 *	created
 */

/**
 * A class for storing information about a bond angle.
 */
public class Angle {
	/** Default constructor. */
	public Angle(Atom first, Atom second, Atom third){
	    firstAtom  = first;
	    secondAtom = second;
	    thirdAtom  = third;
	}

	/** The first atom in the angle. */
	private Atom firstAtom;

	/** The second atom in the angle. */
	private Atom secondAtom;

	/** The third atom in the angle. */
	private Atom thirdAtom;
}
