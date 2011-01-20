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

package astex.anasurface;

import astex.*;

class Edge extends IntArray {
    /** Index of first sphere on edge. */
    public Vertex v0 = new Vertex();
    
    /** Index of second sphere on edge. */
    public Vertex v1 = new Vertex();

    /** Coordinates of center of arc. */
    public double cen[] = new double[3];

    /** Radius of arc. */
    public double r;

    /** Normal to edge plane, used for clipping. */
    public double n[] = new double[3];

    /** Probe face that this edge belongs to. */
    public Face probeFace = null;

    /** Constructor. */
    public Edge(){
	super();
    }

    public boolean isEdge(int i, int j){
        if((v0.i == i && v1.i == j) ||
           (v1.i == i && v0.i == j)){
            return true;
        }

        return false;
    }
}

