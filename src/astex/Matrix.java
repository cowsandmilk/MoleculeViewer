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

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;

/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 16-11-99 mjh
 *	created
 */

/**
 * A class for storing and manipulating a 4x4 matrix.
 */
public class Matrix extends Matrix4d {
    /** Default constructor. */
    public Matrix(){
	setIdentity();
    }
    
    public Matrix(Matrix m) {
	super(m);
    }

    /** Apply non uniform scale. */
    public void scale(double sx, double sy, double sz){ 
	m00 *= sx; m01 *= sy; m02 *= sz;
	m10 *= sx; m11 *= sy; m12 *= sz;
	m20 *= sx; m21 *= sy; m22 *= sz;
	m30 *= sx; m31 *= sy; m32 *= sz;
    }
    
    /** Translate the transformation matrix. */
    public void translate(double tx, double ty, double tz){
	m00 += m03*tx; m01 += m03*ty; m02 += m03*tz;
	m10 += m13*tx; m11 += m13*ty; m12 += m13*tz;
	m20 += m23*tx; m21 += m23*ty; m22 += m23*tz;
	m30 += m33*tx; m31 += m33*ty; m32 += m33*tz;
    }
    
    /** Rotate around x in degrees. */
    public void rotateXdegrees(double d){
	double r = d*Math.PI / 180.0;
	double c = Math.cos(r);
	double s = Math.sin(r);

	double t = 0.0;
	t = m01; m01 = t*c - m02*s; m02 = t*s + m02*c; 
	t = m11; m11 = t*c - m12*s; m12 = t*s + m12*c; 
	t = m21; m21 = t*c - m22*s; m22 = t*s + m22*c; 
	t = m31; m31 = t*c - m32*s; m32 = t*s + m32*c; 
    }

    /** Rotate around y in degrees. */
    public void rotateYdegrees(double d){
	double r = d*Math.PI / 180.0;
	double c = Math.cos(r);
	double s = Math.sin(r);

	double t = 0.0;
	t = m00; m00 = t*c + m02*s; m02 = m02*c - t*s;
	t = m10; m10 = t*c + m12*s; m12 = m12*c - t*s;
	t = m20; m20 = t*c + m22*s; m22 = m22*c - t*s;
	t = m30; m30 = t*c + m32*s; m32 = m32*c - t*s;
    }

    /** Rotate around Z in degrees. */
    public void rotateZdegrees(double d){
	double r = d*Math.PI / 180.0;

	Matrix m = new Matrix();
	m.rotateAroundVector(0., 0., 1., r);
	transform(m);
    }

    /** Transform by another matrix. */
    public void transform(Matrix m){
	double xx00 = m00, xx01 = m01, xx02 = m02, xx03 = m03;
	double xx10 = m10, xx11 = m11, xx12 = m12, xx13 = m13;
	double xx20 = m20, xx21 = m21, xx22 = m22, xx23 = m23;
	double xx30 = m30, xx31 = m31, xx32 = m32, xx33 = m33;
	
	m00 = xx00*m.m00 + xx01*m.m10 + xx02*m.m20 + xx03*m.m30;
	m01 = xx00*m.m01 + xx01*m.m11 + xx02*m.m21 + xx03*m.m31;
	m02 = xx00*m.m02 + xx01*m.m12 + xx02*m.m22 + xx03*m.m32;
	m03 = xx00*m.m03 + xx01*m.m13 + xx02*m.m23 + xx03*m.m33;
	
	m10 = xx10*m.m00 + xx11*m.m10 + xx12*m.m20 + xx13*m.m30;
	m11 = xx10*m.m01 + xx11*m.m11 + xx12*m.m21 + xx13*m.m31;
	m12 = xx10*m.m02 + xx11*m.m12 + xx12*m.m22 + xx13*m.m32;
	m13 = xx10*m.m03 + xx11*m.m13 + xx12*m.m23 + xx13*m.m33;
	
	m20 = xx20*m.m00 + xx21*m.m10 + xx22*m.m20 + xx23*m.m30;
	m21 = xx20*m.m01 + xx21*m.m11 + xx22*m.m21 + xx23*m.m31;
	m22 = xx20*m.m02 + xx21*m.m12 + xx22*m.m22 + xx23*m.m32;
	m23 = xx20*m.m03 + xx21*m.m13 + xx22*m.m23 + xx23*m.m33;
	
	m30 = xx30*m.m00 + xx31*m.m10 + xx32*m.m20 + xx33*m.m30;
	m31 = xx30*m.m01 + xx31*m.m11 + xx32*m.m21 + xx33*m.m31;
	m32 = xx30*m.m02 + xx31*m.m12 + xx32*m.m22 + xx33*m.m32;
	m33 = xx30*m.m03 + xx31*m.m13 + xx32*m.m23 + xx33*m.m33;
    }
    
    /** Transform a point by the current matrix. */
    public void transform(Point3d p){
	double x = p.x, y = p.y, z = p.z;
	p.x = x*m00 + y*m10 + z*m20 + m30;
	p.y = x*m01 + y*m11 + z*m21 + m31;
	p.z = x*m02 + y*m12 + z*m22 + m32;
    }

    /** Transform a point by the inverse matrix (assumes rotation matrix) */
    public void transformByInverse(Point3d p){
	double x = p.x, y = p.y, z = p.z;
	// don't need translation part here.
	p.x = x*m00 + y*m01 + z*m02;
	p.y = x*m10 + y*m11 + z*m12;
	p.z = x*m20 + y*m21 + z*m22;
    }
    
    /** Rotate around a line. */
    public void rotateAroundVector(double x, double y, double z,
				   double theta){
	double d = x*x + y*y + z*z;

	if(d > 1.e-3){
	    d = Math.sqrt(d);
	    x /= d;
	    y /= d;
	    z /= d;
	}else{
	    System.out.println("rotateAroundVector: direction is zero length");
	    return;
	}

	double s = Math.sin(theta);
	double c = Math.cos(theta);
	double t = 1.0 - c;

	setIdentity();
	
	m00 = t * x * x + c;	/* leading diagonal */
	m11 = t * y * y + c;
	m22 = t * z * z + c;
	
	m10 = t * x * y + s * z;	/* off diagonal elements */
	m20 = t * x * z - s * y;
	
	m01 = t * x * y - s * z;
	m21 = t * y * z + s * x;
	
	m02 = t * x * z + s * y;
	m12 = t * y * z - s * x;
    }
    
    /** A format object for printing matrices. */
    private static Format f6 = new Format("%11.6f");
    
    /** Print the matrix. */
    public void print(String message){
	System.out.println(message);
	System.out.println(f6.format(m00) + " " + f6.format(m01) +
			   " " + f6.format(m02) + " " + f6.format(m03));
	System.out.println(f6.format(m10) + " " + f6.format(m11) +
			   " " + f6.format(m12) + " " + f6.format(m13));
	System.out.println(f6.format(m20) + " " + f6.format(m21) +
			   " " + f6.format(m22) + " " + f6.format(m23));
	System.out.println(f6.format(m30) + " " + f6.format(m31) + 
			   " " + f6.format(m32) + " " + f6.format(m33));
    }

    public String returnScript(){
	StringBuilder command = new StringBuilder("matrix ");
	command.append(String.format(" %g %g %g %g", m00, m01, m02, m03));
	command.append(String.format(" %g %g %g %g", m10, m11, m12, m13));
	command.append(String.format(" %g %g %g %g", m20, m21, m22, m23));
	command.append(String.format(" %g %g %g %g", m30, m31, m32, m33));
	command.append(";");

	return command.toString();
    }

    public boolean isIdentity(double tol){
	if(Math.abs(m00 - 1.0) > tol) return false;
	if(Math.abs(m01)       > tol) return false;
	if(Math.abs(m02)       > tol) return false;
	if(Math.abs(m03)       > tol) return false;
	if(Math.abs(m10)       > tol) return false;
	if(Math.abs(m11 - 1.0) > tol) return false;
	if(Math.abs(m12)       > tol) return false;
	if(Math.abs(m13)       > tol) return false;
	if(Math.abs(m20)       > tol) return false;
	if(Math.abs(m21)       > tol) return false;
	if(Math.abs(m22 - 1.0) > tol) return false;
	if(Math.abs(m23)       > tol) return false;
	if(Math.abs(m30)       > tol) return false;
	if(Math.abs(m31)       > tol) return false;
	if(Math.abs(m32)       > tol) return false;
	if(Math.abs(m33 - 1.0) > tol) return false;

	return true;
    }

    /** Interpolate a new matrix. */
    public static Matrix interpolate(Matrix MS, Matrix MF, double frac){
	Matrix MI = new Matrix();

	interpolate(MS, MF, frac, MI);

	return MI;
    }

    /** Interpolate a new matrix. */
    private static void interpolate(Matrix MS, Matrix MF, double frac, Matrix MI){
	Quat4d qS = new Quat4d();
	Quat4d qF = new Quat4d();

	qS.set(MS);
	qF.set(MF);

	qF.interpolate(qS, frac);

	MI.set(qF);
    }
}