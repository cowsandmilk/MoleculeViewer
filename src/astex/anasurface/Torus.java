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

package astex.anasurface;

import java.util.*;

class Torus {
    /** Index of first atom. */
    public int i;

    /** Index of second atom. */
    public int j;

    /** Torus center. */
    public double tij[] = new double[3];

    /** Torus axis unit vector. */
    public double uij[] = new double[3];

    /** Contact circle on i. */
    public double cij[] = new double[3];

    /** Contact circle on j. */
    public double cji[] = new double[3];

    /** Radius of contact circle on i. */
    public double rcij = 0.0;

    /** Radius of contact circle on j. */
    public double rcji = 0.0;

    /** Torus radius. */
    public double rij = 0.0;

    /** Perpendicular to torus axis. */
    public double uijnorm[] = new double[3];

    /** Second perpendicular to torus axis. */
    public double uijnorm2[] = new double[3];

    /** Constructor. */
    public Torus(int ai, int aj){
	i = ai;
	j = aj;
    }

    /** List of faces for this torus. */
    public List<Edge> edges = new ArrayList<Edge>(10);

    /** Does the torus self intersect. */
    public boolean selfIntersects = false;
}
