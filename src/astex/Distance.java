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
import java.awt.*;

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
	public DynamicArray group0 = new DynamicArray();

	/** Atoms at the other end of the distance. */
	public DynamicArray group1 = new DynamicArray();

	/** Calculate the center of group 0. */
	public Point3d getCenter0(){
		Point3d p = new Point3d();
		getCenter(p, group0);
		return p;
	}

	/** Calculate the center of group 1. */
	public Point3d getCenter1(){
		Point3d p = new Point3d();
		getCenter(p, group1);
		return p;
	}

	/** Calculate the center of the group. */
	public void getCenter(Point3d p, DynamicArray d){
		p.zero();
		int n = d.size();

		if(n == 0){
			return;
		}

		for(int i = 0; i < n; i++){
			Point3d a = (Point3d)d.get(i);
			p.x += a.x;
			p.y += a.y;
			p.z += a.z;
		}

		p.x /= (double)n;
		p.y /= (double)n;
		p.z /= (double)n;
	}

	/** Is this distance valid. */
	public boolean valid(){
		if(!getBoolean(Visible, false)){
			return false;
		}

		int ngroup0 = group0.size();
		int ngroup1 = group1.size();

		// both ends contains some atoms
		if(ngroup0 == 0 || ngroup1 == 0){
			return false;
		}

		// and all atoms are displayed
		for(int i = 0; i < ngroup0; i++){
			Atom a = (Atom)group0.get(i);
			if(!a.isDisplayed())
				return false;

			Molecule mol = a.getMolecule();
			if(!mol.getDisplayed()){
				return false;
			}
		}

		for(int i = 0; i < ngroup1; i++){
			Atom a = (Atom)group1.get(i);
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