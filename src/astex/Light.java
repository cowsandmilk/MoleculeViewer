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

/**
 * Store the data for a light source.
 */
class Light {
    /** Position of the light. */
    public double pos[] = new double[3];

    /** Packed diffuse colour. */
    public int diffuse;

    /** Packed specular colour. */
    public int specular;
    
    /** Sheen of highlight. */
    public double power = 50.0;

    /** Is the light on? */
    public boolean on = true;

    /** Default constructor. */
    public Light(boolean onOff, double x, double y, double z,
		 int d, int s, double pow){
	on = onOff;
	pos[0] = x;
	pos[1] = y;
	pos[2] = z;

	normalisePos();

	diffuse = d;
	specular = s;
	power = pow;
    }

    public void normalisePos(){
	double norm = pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2];
	norm = Math.sqrt(norm);
	pos[0] /= norm;
	pos[1] /= norm;
	pos[2] /= norm;
    }
}
