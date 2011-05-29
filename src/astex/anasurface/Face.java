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

import java.util.*;

/**
 * A face on the molecular surface.
 * Can be either convex, saddle or concave.
 * It contains a list of ordered edges that
 * are the boundary of the face.
 *
 * Each is processed in slightly different ways.
 */

class Face extends Stack<Edge> {
    /** Type of face. */
    public Type type;

    /** Intersection status. */
    public int intersection = 0;

    public static final int ProbeIntersection = 1;

    public enum Type {
	Convex, Saddle, Concave, Undefined
    }

    /** Interpolation start on i. */
    double iij[] = null;

    /** Interpolation start on j. */
    double iji[] = null;

    /** Skip triangulation for this face. */
    public boolean skip = false;

    /** Sphere centre if Convex/Concave. */
    public double cen[] = new double[3];

    /** Sphere radius if Convex/Concave. */
    public double r = -1.0;

    /** Start angle for toroidal edges. */
    public double startAngle = 0.0;

    /** Stop angle for toroidal edges. */
    public double stopAngle = 0.0;

    /** Torus that this face belongs to for Saddle type. */
    public Torus torus = null;

    /** Constructor. */
    public Face(Type t){
	super();
	type = t;

	if(type == Type.Saddle){
	    iij = new double[3];
	    iji = new double[3];
	}
    }

    public boolean add(Edge e){
	if(type == Type.Concave && e.probeFace == null){
	    e.probeFace = this;
	}

	return super.add(e);
    }

    /** Is this face valid? */
    public boolean isValid(){
	Edge previous = get(size() - 1);

	for(Edge e : this){
	    if(e.v0 != previous.v1){
		System.out.println("face error");

		return false;
	    }

	    previous = e;
	}

	return true;
    }

    public void print(String s){
	System.out.println(s + " " + size() + " edges");
	for(Edge e : this){
	    System.out.println("v0.vi " + e.v0.vi + " v1.vi " + e.v1.vi);
	}
    }
}
