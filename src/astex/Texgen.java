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

import java.util.List;

/**
 * Class for generating various sorts of texture coordinates.
 */
public class Texgen {
    /**
     * Measure distance to nearest point in point list.
     * Texture coordinates are normalised by dividing by max.
     * This will make points further away than max invisible.
     *
     * If max is less than zero, then the tex coords are
     * normalised by the longest distance measured.
     * This has the effect that the tex coords will max at 1.0.
     */
    public static void distance(Tmesh tm, List<Atom> points, int uv){
	int tnp = tm.np;
	float tlocal[] = null;
	double dmax = 0.0;

	if(uv == Tmesh.UTexture){
	    tlocal = tm.u;
	}else if(uv == Tmesh.VTexture){
	    tlocal = tm.v;
	}else{
	    System.out.println("Texgen.distance: " +
			       "invalid texture coordinate " + uv);
	    return;
	}

	for(int i = 0; i < tnp; i++){
	    double tx = tm.x[i];
	    double ty = tm.y[i];
	    double tz = tm.z[i];
	    double dmin = 1.e10;

	    for(Point3d p : points){
		double dx = tx - p.x;
		double dy = ty - p.y;
		double dz = tz - p.z;
		double d2 = dx*dx + dy*dy + dz*dz;
		if(d2 < dmin){
		    dmin = d2;
		}
	    }

	    double d = Math.sqrt(dmin);
	    if(d > dmax){
		dmax = d;
	    }

	    tlocal[i] = (float)d;
	}

	if(Math.abs(dmax) > 1.e-3){
	    if(uv == Tmesh.UTexture){
		tm.setUOffset(0.0);
		tm.setUScale(1./dmax);
	    }else if(uv == Tmesh.VTexture){
		tm.setVOffset(0.0);
		tm.setVScale(1./dmax);
	    }
	}else{
	    Log.error("dmax was zero");
	}
    }

    /** Calculate pseudo curvature. */
    public static void curvature(Tmesh tm, List<Atom> points,
				 int uv, double maxd){
	int tnp = tm.np;
	float tlocal[] = null;
	double maxd2 = maxd*maxd;
	double min = 1.e10;
	double max = 0.0;

	if(uv == Tmesh.UTexture){
	    tlocal = tm.u;
	}else if(uv == Tmesh.VTexture){
	    tlocal = tm.v;
	}else{
	    System.out.println("Texgen.distance: " +
			       "invalid texture coordinate " + uv);
	    return;
	}

	for(int i = 0; i < tnp; i++){
	    double tx = tm.x[i];
	    double ty = tm.y[i];
	    double tz = tm.z[i];
	    double dtotal = 0.0;

	    for(Point3d p : points){
		double dx = tx - p.x;
		double dy = ty - p.y;
		double dz = tz - p.z;
		double d2 = dx*dx + dy*dy + dz*dz;
		if(d2 < maxd2){
		    d2 = Math.sqrt(d2);
		    dtotal += (maxd - d2);
		}
	    }

	    tlocal[i] = (float)dtotal;

	    if(dtotal > max){
		max = dtotal;
	    }
	    if(dtotal < min){
		min = dtotal;
	    }
	}

	for(int i = 0; i < tnp; i++){
	    tlocal[i] = (float)((tlocal[i] - min)/(max - min));
	}
    }

    /** Create rectangular texture coordinates. */
    public static void rectangular(Tmesh tm){
	int tnp = tm.np;
	double xmin =  1.e10;
	double xmax = -1.e10;
	double ymin =  1.e10;
	double ymax = -1.e10;

	for(int i = 0; i < tnp; i++){
	    if(tm.x[i] < xmin) xmin = tm.x[i];
	    if(tm.x[i] > xmax) xmax = tm.x[i];
	    if(tm.y[i] < ymin) ymin = tm.y[i];
	    if(tm.y[i] > ymax) ymax = tm.y[i];
	}

	double dx = xmax - xmin;
	double dy = ymax - ymin;

	for(int i = 0; i < tnp; i++){
	    tm.u[i] = (float)((tm.x[i] - xmin)/dx);
	    tm.v[i] = (float)((ymax - tm.y[i])/dy);
	}	
    }

    private static double x[] = null;
    private static double y[] = null;
    private static double z[] = null;
    private static double q[] = null;

    public enum MapFunc {
	Electrostatic, Lipophilicity
    }

    /** Generate potential. */
    public static synchronized void property_map(Tmesh tm,
						 List<Atom> atoms,
						 int uv,
						 double maxd,
						 boolean absolute,
						 MapFunc func){
	int atomCount = atoms.size();
	float tlocal[] = null;
	double min =  1.0e10;
	double max = -1.0e10;
	double maxd2 = maxd * maxd;

	double halfWidth = 1.;
	double dCutoff = 3.0;
	double topPart = Math.exp(-halfWidth * dCutoff) + 1.0;

	if(uv == Tmesh.UTexture){
	    tlocal = tm.u;
	}else if(uv == Tmesh.VTexture){
	    tlocal = tm.v;
	}
	
	int arraySize = atomCount * 2;

	if(x == null || x.length < arraySize){
	    x = new double[arraySize];
	    y = new double[arraySize];
	    z = new double[arraySize];
	    q = new double[arraySize];
	}

	int na = 0;
	int amideProtons = 0;

	for(Atom a : atoms){
	    String name = a.getAtomLabel();

	    if(func == MapFunc.Electrostatic && "N".equals(name)){
		Point3d nh = getAmideHydrogen(a);
		// nh will be null if we already had an nh
		if(nh != null){
		    x[na] = nh.x;
		    y[na] = nh.y;
		    z[na] = nh.z;
		    // this is the charmm19 value of
		    // the amide proton charge.
		    // should be customisable somehow...
		    q[na] = +0.25;
		    na++;
		    amideProtons++;
		}
	    }

	    // get the property value...
	    double qa = a.getPartialCharge();

	    // scale charge by occupancy
	    // to allow for multiple positions.
	    if(func == MapFunc.Electrostatic){
		qa *= a.getOccupancy();
	    }
	    // only considering atoms with non-zero charge

	    if(Math.abs(qa) > 1.e-3){
		x[na] = a.x;
		y[na] = a.y;
		z[na] = a.z;
		q[na] = qa;
		na++;
	    }
	}

	// build the lattice
	Lattice l = new Lattice(maxd);

	for(int ia = 0; ia < na; ia++){
	    l.add(ia, x[ia], y[ia], z[ia]);
	}

	if(func == MapFunc.Electrostatic){
	    System.out.println("added " + amideProtons + " amide protons");
	}

	double totalCharge = 0.0;
	
	for(int i = 0; i < na; i++){
	    totalCharge += q[i];
	}
	
	int tnp = tm.np;

	for(int i = 0; i < tnp; i++){
	    double tx = tm.x[i];
	    double ty = tm.y[i];
	    double tz = tm.z[i];
	    double dtotal = 0.0;
	    double norm = 0.0;

	    for(int j = 0; j < na; j++){
		double dx = tx - x[j];
		double dy = ty - y[j];
		double dz = tz - z[j];
		double d2 = dx*dx + dy*dy + dz*dz;

		if(d2 < maxd2){
		    if(func == MapFunc.Electrostatic){
			dtotal += q[j]/d2;
		    }else if(func == MapFunc.Lipophilicity){
			double d = Math.sqrt(d2);
			double gd = topPart /
			    (Math.exp(halfWidth * (d - dCutoff)) + 1.0);


			dtotal += q[j] * gd;
			norm += gd;
		    }
		}
	    }

	    if(func == MapFunc.Lipophilicity){
		dtotal /= norm;
	    }

	    tlocal[i] = (float)dtotal;

	    if(dtotal > max){
		max = dtotal;
	    }
	    if(dtotal < min){
		min = dtotal;
	    }
	}

	FILE.out.print("property min %.3f ", min);
	FILE.out.print("max %.3f\n", max);

	if(uv == Tmesh.UTexture){
	    if(func == MapFunc.Lipophilicity){
		tm.setUOffset(min);
		tm.setUScale(1.0/(max - min));
	    }else{
		tm.setUOffset(-0.5);
	    }
	}else{
	    if(func == MapFunc.Lipophilicity){
		tm.setVOffset(min);
		tm.setVScale(1.0/(max - min));
	    }else{
		tm.setVOffset(-0.5);
	    }
	}
    }

    /** Generate an amide hydrogen position. */
    private static Point3d getAmideHydrogen(Atom N){
	if(N == null){
	    return null;
	}

	Atom H = N.getBondedAtom("H");

	if(H != null){
	    return null;
	}

	Atom CA = N.getBondedAtom("CA");
	Atom C = N.getBondedAtom("C");

	if(N == null || CA == null || C == null){
	    return null;
	}

	Point3d hpos = new Point3d();
	hpos.set(N);
	hpos.sub(C);
	hpos.add(N);
	hpos.sub(CA);

	hpos.normalize();
	hpos.scale(1.04);

	hpos.add(N);

	return hpos;
    }
}
