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

import javax.vecmath.Vector3d;


/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 01-11-99 mjh
 *	created
 */

/**
 * A class for manipulating 3d points and vectors.
 */
public class Point3d extends Vector3d implements Cloneable {

    /**
     * Default constructor.
     *
     * The x, y and z coordinates are set to 0.0
     */
    public Point3d(){
	super();
    }

    /**
     * Constructor which allows the x, y, and z coordinates to be specified.
     */
    public Point3d(double xx, double yy, double zz){
	super(xx,yy,zz);
    }

    /**
     * Construct a 2D point with specified x and y coordinates.
     *
     * The z coordinates is set to 0.0
     */
    public Point3d(double xx, double yy){
	this(xx, yy, 0.0);
    }

    /**
     * Clone method
     */
    @Override
    public Point3d clone() {
	return (Point3d) super.clone();
    }

    /** Set the specified component. */
    public double get(int i){
	switch(i){
	case 0: return x;
	case 1: return y;
	case 2: return z;
	default:
	    System.out.println("Point3d.get: can't get component "+ i);
	}
	return Double.MAX_VALUE;
    }

    /** Set the specified component. */
    public void set(int i, double v){
	switch(i){
	case 0: x = v; return;
	case 1: y = v; return;
	case 2: z = v; return;
	default: System.out.println("Point3d.set: can't set component " + i);
	}
    }

    /**
     * Set the x, y and z coordinates to 0.0
     *
     * Transformed and screen space coordinates are not affected.
     */
    public void zero(){
	set(0.0, 0.0, 0.0);
    }

    /** Return the x coordinate. */
    public double getX(){
	return x;
    }

    /** Return the y coordinate. */
    public double getY(){
	return y;
    }

    /** Return the z coordinate. */
    public double getZ(){
	return z;
    }

    /** Set the x coordinate. */
    public void setX(double xx){
	x = xx;
    }

    /** Set the y coordinate. */
    public void setY(double yy){
	y = yy;
    }

    /** Set the x coordinate. */
    public void setZ(double zz){
	z = zz;
    }

    /**
     * Construct a point with the x, y and z coordinates equal to the
     * midpoint of two other Point3ds.
     */
    public static Point3d mid(Point3d pmin, Point3d pmax){
	Point3d middle = new Point3d();
	middle.x = 0.5 * (pmin.x + pmax.x);
	middle.y = 0.5 * (pmin.y + pmax.y);
	middle.z = 0.5 * (pmin.z + pmax.z);

	return middle;
    }

    /**
     * Construct a point with the x, y and z coordinates equal to the
     * midpoint of two other Point3ds.
     */
    public static void mid(Point3d pmid, Point3d pmin, Point3d pmax){
	pmid.x = 0.5 * (pmin.x + pmax.x);
	pmid.y = 0.5 * (pmin.y + pmax.y);
	pmid.z = 0.5 * (pmin.z + pmax.z);
    }

    /**
     * Return another point, which is at the mid point of two points.
     */
    public static Point3d unitVector(Point3d p1, Point3d p2){
	/* Make a unit vector from p1 to p2. */

	Point3d unit = new Point3d(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	unit.normalize();

	return unit;
    }

    /** Set first point to be unitVector from p1 to p2. */
    public static void unitVector(Point3d up12, Point3d p1, Point3d p2){
	/* Make a unit vector from p1 to p2. */

	up12.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	up12.normalize();
    }

    /**
     * Generate a vector from the first point to the second.
     *
     * The vector does not have a length of 1.
     */
    public static Point3d vector(Point3d p1, Point3d p2){
	/* Make a vector from p1 to p2. */

	Point3d unit = new Point3d(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);

	return unit;
    }

    /**
     * Construct a unit vector that is perpendicular to the vector.
     *
     * If p is the null vector then (1.,1.,.1) is returned.
     */
    public static Point3d normalToLine(Point3d p){
	/*
	 * Construct a unit vector that is perpendicular to
	 * the vector described by p.  If p is the null
	 * vector then (1.,1.,.1) is returned.
	 */

	Point3d normal = new Point3d(1., 1., 1.);

	if(p.x != 0.) normal.x = (p.z + p.y) / -p.x;
	else if(p.y != 0.) normal.y = (p.x + p.z) / -p.y;
	else if(p.z != 0.) normal.z = (p.x + p.y) / -p.z;

	normal.normalize();

	return normal;
    }

    /**
     * Construct a unit vector that is perpendicular to the vector.
     */
    public static void normalToLine(Point3d p, Point3d n){
	/*
	 * Construct a unit vector that is perpendicular to
	 * the vector described by p.  If p is the null
	 * vector then (1.,1.,.1) is returned.
	 */

	n.set(1., 1., 1.);

	if(p.x != 0.) n.x = (p.z + p.y) / -p.x;
	else if(p.y != 0.) n.y = (p.x + p.z) / -p.y;
	else if(p.z != 0.) n.z = (p.x + p.y) / -p.z;

	n.normalize();
    }

    /** Return cross product with c. */
    public Point3d cross(Point3d c){

	Point3d a = new Point3d();
	a.cross(this, c);
	
	a.normalize();

	return a;
    }

    /** Set a to cross product of b and c. */
    public static void cross(Point3d a, Point3d b, Point3d c){

	a.cross(b,c);

	a.normalize();
    }

    /** Set a to cross product of b and c. */
    public static void crossNoNormalise(Point3d a, Point3d b, Point3d c){
	a.cross(b,c);
    }

    /** Generate cross product for double[] vectors. */
    public static void cross(double a[], double b[], double c[]){
	a[0] = (b[1] * c[2]) - (b[2] * c[1]);
	a[1] = (b[2] * c[0]) - (b[0] * c[2]);
	a[2] = (b[0] * c[1]) - (b[1] * c[0]);
    }

    /**
     * Return the distance to the specified point.
     */
    public double distance(Point3d p){
	double dx = p.x - x;
	double dy = p.y - y;
	double dz = p.z - z;

	return(Math.sqrt(dx*dx + dy*dy + dz*dz));
    }

    /** Return the square of the distance to the specified point. */
    public double distanceSq(Point3d p){
	double dx = p.x - x;
	double dy = p.y - y;
	double dz = p.z - z;

	return(dx*dx + dy*dy + dz*dz);
    }

    /** Scale a vector by the amount specified for each coordinate. */
    public void divide(double s){
	scale(1/s);
    }

    /** Calculate the angle between the 3 points. */
    public static double angle(Point3d a, Point3d b, Point3d c){
	double xba = a.x - b.x, yba = a.y - b.y, zba = a.z - b.z;
	double xbc = c.x - b.x, ybc = c.y - b.y, zbc = c.z - b.z;

	double ba = Math.sqrt(xba*xba + yba*yba + zba*zba);
	double bc = Math.sqrt(xbc*xbc + ybc*ybc + zbc*zbc);

	double dot = xba*xbc + yba*ybc + zba*zbc;

	dot /= (ba * bc);

	return Math.acos(dot);
    }

    /** Calculate the angle in degrees. */
    public static double angleDegrees(Point3d a, Point3d b, Point3d c){
	return 180.0 * angle(a, b, c) / Math.PI;
    }

    /** Calculate the torsion angle between the 4 points. */
    public static double torsion(Point3d p1, Point3d p2,
				 Point3d p3, Point3d p4){
	Point3d v1, v2, v3;
	Point3d n1, n2;
	double angle;
		
	/* generate vectors between points (and normalise) */
	v1 = unitVector(p1, p2);
	v2 = unitVector(p2, p3);
	v3 = unitVector(p3, p4);
		
	/* form xprods and normalise */
	n1 = v1.cross(v2); n1.normalize();
	n2 = v2.cross(v3); n2.normalize();
		
	/*
	 * get the angle between xprods and figure out whether to negate it
	 *
	 * if n1 points in the opposite direction to v3 the angle should be
	 * negated
	 */

	double dot = n1.dot(n2);

	// ensure that the dot product lies
	// within -1.0/+1.0
	if(dot > 1.0) dot = 1.0;
	else if(dot < -1.0) dot = -1.0;

        angle = Math.acos(dot);
	
	if(n1.dot(v3) < 0.0) angle = -angle;
		
	return angle;
    }

    /** Calculate torsion in degrees. */
    public static double torsionDegrees(Point3d p1, Point3d p2,
					Point3d p3, Point3d p4){
	return 180.0 * torsion(p1, p2, p3, p4) / Math.PI;
    }


    /** Transform this atom to screen coordinates. */
    public void transform(Matrix m){
	double xx = x*m.m00 + y*m.m10 + z*m.m20 + m.m30;
	double yy = x*m.m01 + y*m.m11 + z*m.m21 + m.m31;
	double zz = x*m.m02 + y*m.m12 + z*m.m22 + m.m32;

	x = xx;
	y = yy;
	z = zz;
    }

    /** Return a string representation of this point. */
    @Override
    public String toString(){
	return
	    String.format("%8.3f", x) +
	    String.format("%8.3f", y) +
	    String.format("%8.3f", z);
    }
}
