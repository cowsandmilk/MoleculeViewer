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

import astex.generic.*;
import java.awt.Color;
import java.util.*;

public class Distance extends Generic {
	/** Properties for a Distance object. */
	public static final String Mode       = "mode";
	public static final String Visible    = "visible";
	public static final String Format     = "format";
	public static final String Color      = "color";
	public static final String LabelColor = "labelcolor";
	public static final String On         = "on";
	public static final String Off        = "off";
	public static final String Radius     = "radius";

	/** The pair distance mode. */
	public static final int Pairs = 1;

	/** The centroid distance mode. */
	public static final int Centroids = 2;

	public static Distance createDistanceMonitor(Atom a0, Atom a1){
		String monitorAtomLabel = "%a %r%c";
		Distance d = new Distance();
		d.group0.add(a0);
		d.group1.add(a1);

		d.setString(Name,
				a0.generateLabel(monitorAtomLabel) + "-" +
				a1.generateLabel(monitorAtomLabel));
		d.setString(Format, "%.2fA");

		d.setDouble(On, 0.2);
		d.setDouble(Off, 0.2);
		d.setDouble(Radius, -1.0);

		d.setInteger(Mode, Pairs);

		d.setBoolean(Visible, true);

		d.set(Color, new Color(Color32.white));
		d.set(LabelColor, new Color(Color32.white));

		return d;
	}

	/** Atoms at one end of the distance. */
	public List<Point3d> group0 = new ArrayList<Point3d>(10);

	/** Atoms at the other end of the distance. */
	public List<Point3d> group1 = new ArrayList<Point3d>(10);

	/** Calculate the center of group 0. */
	public Point3d getCenter0(){
		return getCenter(group0);
	}

	/** Calculate the center of group 1. */
	public Point3d getCenter1(){
		return getCenter(group1);
	}

	/** Calculate the center of the group. */
	public Point3d getCenter(List<Point3d> d){
		Point3d p = new Point3d(0,0,0);

		if(d.isEmpty()){
			return p;
		}

		for(Point3d a : d){
			p.x += a.x;
			p.y += a.y;
			p.z += a.z;
		}

		int n = d.size();
		p.x /= (double)n;
		p.y /= (double)n;
		p.z /= (double)n;
		return p;
	}

	/** Is this distance valid. */
	public boolean valid(){
		if(!getBoolean(Visible, false)){
			return false;
		}

		// both ends contains some atoms
		if(group0.isEmpty() || group1.isEmpty()){
			return false;
		}

		// and all atoms are displayed
		for(Point3d p : group0){
			Atom a = (Atom) p;
			if(!a.isDisplayed())
				return false;

			Molecule mol = a.getMolecule();
			if(!mol.getDisplayed()){
				return false;
			}
		}

		for(Point3d p : group1){
			Atom a = (Atom) p;
			if(!a.isDisplayed())
				return false;

			Molecule mol = a.getMolecule();
			if(!mol.getDisplayed()){
				return false;
			}
		}

		return true;
	}
}