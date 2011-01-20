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

import astex.generic.*;

/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 21-02-00 mjh
 *	add method getUnitVector() that returns unit vector from firstAtom
 *	to secondAtom
 * 17-02-00 mjh
 *	add ringBond status for the attributes. Add a toString() method.
 * 06-11-99 mjh
 *	add some more bond style definitions
 * 28-10-99 mjh
 *	created
 */

public class Bond extends Generic {
    private static final String BondOrder = "bondorder";

    /** Default constructor. */
    public Bond(Atom a1, Atom a2){
	setBondOrder(SingleBond);
	setBondColor(Color32.white);
	setBondWidth(1);
	setStickWidth(0.12);
	setCylinderWidth(0.15);
	attributes = 0;
	firstAtom = a1;
	secondAtom = a2;
    }

    @Override
    public Object set(Object key, Object property){
        String name = (String)key;

        if(name.equals(BondOrder)){
            setBondOrder(((Integer)property).intValue());
        }else{
            super.set(key, property);
        }

        // should return old value
        return null;
    }

    @Override
    public Object get(Object key, Object def){
        if(key.equals(BondOrder))
            return Integer.valueOf(bondOrder);

        return super.get(key, def);
    }

    /** The first atom in the bond. */
    private Atom firstAtom;

    /** The second atom in the bond. */
    private Atom secondAtom;

    /** The bond order of the bond. */
    private int bondOrder;

    private byte radii[] = new byte[4];

    /** The ideal bond length for this bond. */
    private float idealBondLength = -1.0f;

    /** Constant that defines a single bond. */
    public static final int SingleBond = 1;

    /** Constant that defines a single bond. */
    public static final int DoubleBond = 2;

    /** Constant that defines a single bond. */
    public static final int TripleBond = 3;

    /** Constant that defines an aromatic bond. */
    public static final int AromaticBond = 4;

    /** Constant that defines an aromatic bond. */
    public static final int AmideBond = 5;

    /** Constant that defines a single or double bond. */
    public static final int SingleOrDoubleBond = 6;

    /** Constant that defines a single or aromatic bond. */
    public static final int SingleOrAromaticBond = 7;

    /** Constant that defines a double or aromatic bond. */
    public static final int DoubleOrAromaticBond = 8;

    /** Constant that defines any bond. */
    public static final int AnyBond = 9;

    private static final double bondScale = 2./128.;

    /* Various attributes of the bond. */
	
    /** The integer that stores the attributes. */
    public int attributes          = 0;

    /** Is the bond to be drawn wide. */
    private static int wideBond     = 1;

    /** Is the bond in a ring of any description. */
    private static int ringBond     = 2;

    /** Did the bond come from a CONECT record. */
    private static int ExplicitBond = 4;

    /** Is bond an explicit bond. */
    public boolean isExplicitBond(){
	return (attributes & ExplicitBond) != 0;
    }

    /** Set whether this bond was an explicit bond. */
    public void setExplicitBond(boolean e){
	if(e){
	    attributes |= ExplicitBond;
	}else{
	    attributes &= ~ExplicitBond;
	}
    }

    /**
     * Get the value of bondWidth.
     * @return value of bondWidth.
     */
    public int getBondWidth() {
	return radii[0];
    }
    
    /**
     * Set the value of bondWidth.
     * @param v  Value to assign to bondWidth.
     */
    public void setBondWidth(int  v) {
	this.radii[0] = (byte)v;
    }

    /**
     * Get the value of stickWidth.
     * @return value of stickWidth.
     */
    public double getStickWidth() {
	return radii[1] * bondScale;
    }
    
    /**
     * Set the value of stickWidth.
     * @param v  Value to assign to stickWidth.
     */
    public void setStickWidth(double  v) {
	this.radii[1] = (byte)(v/bondScale);
    }
    
    /**
     * Get the value of cylinderWidth.
     * @return value of cylinderWidth.
     */
    public double getCylinderWidth() {
	return radii[2] * bondScale;
   }
    
    /**
     * Set the value of cylinderWidth.
     * @param v  Value to assign to cylinderWidth.
     */
    public void setCylinderWidth(double  v) {
	this.radii[2] = (byte)(v/bondScale);
    }

    int bondColor;
    
    /**
     * Get the value of bondColor.
     * @return value of bondColor.
     */
    public int getBondColor() {
	return bondColor;
    }
    
    /**
     * Set the value of bondColor.
     * @param v  Value to assign to bondColor.
     */
    public void setBondColor(int  v) {
	this.bondColor = v;
    }

    /** Set the bond to be wide or not. */
    public void setWideBond(boolean wide){
	if(wide){
	    attributes |= wideBond;
	}else{
	    attributes &= ~wideBond;
	}
    }

    /** Set the bond ring status. */
    public void setRingBond(boolean ring){
	if(ring){
	    attributes |= ringBond;
	}else{
	    attributes &= ~ringBond;
	}
    }

    /** Is the bond wide. */
    public boolean isWideBond(){
	if(firstAtom.isWide() && secondAtom.isWide()){
	    return true;
	}

	return false;
    }

    /** Is the bond in a ring. */
    public boolean isRingBond(){
	return (attributes & ringBond) != 0;
    }

    /** Set the bond order. */
    public void setBondOrder(int order){
	bondOrder = order;
    }

    /** Get the bond order. */
    public int getBondOrder(){
	return bondOrder;
    }

    /** Is the bond a query bond. */
    public boolean isQueryBond(){
	return (bondOrder > TripleBond && bondOrder <= AnyBond);
    }

    /** Set the first atom in the bond. */
    public void setFirstAtom(Atom newFirstAtom){
	firstAtom = newFirstAtom;
    }

    /** Set the second atom in the bond. */
    public void setSecondAtom(Atom newSecondAtom){
	secondAtom = newSecondAtom;
    }

    /** Return the first atom in a bond. */
    public Atom getFirstAtom(){
	return firstAtom;
    }

    /** Return the second atom in a bond. */
    public Atom getSecondAtom(){
	return secondAtom;
    }

    /** Get the specified atom. */
    public Atom getAtom(int index){
	if(index == 0)
	    return firstAtom;
	if(index == 1)
	    return secondAtom;

        return null;
    }

    /**
     * Given one atom in a bond return the other atom from the bond.
     *
     * If the atom isn't in the bond return null.
     */
    public Atom getOtherAtom(Atom knownAtom){
	if(knownAtom == firstAtom)
	    return secondAtom;
	if(knownAtom == secondAtom)
	    return firstAtom;

        return null;
    }

    /** Return a smiles style symbol for the bond. */
    public String getBondSymbol(){
	if(bondOrder == SingleBond)
	    return "-";
	if(bondOrder == DoubleBond)
	    return "=";
	if(bondOrder == TripleBond)
	    return "#";
	if(bondOrder == AromaticBond)
	    return ":";

        return "-";
    }

    /** Is this a terminal bond. */
    public boolean isTerminalBond(){
	if(firstAtom.getBondCount() == 1)
	    return true;
	if(secondAtom.getBondCount() == 1)
	    return true;

	return false;
    }

    /** Is this bond non rotatable. */
    public boolean isNonRotatable(){
	return (bondOrder == Bond.DoubleBond || bondOrder == Bond.AromaticBond);
    }

    /** Set the ideal bond length. */
    public void setIdealBondLength(double d){
	idealBondLength = (float)d;
    }

    /** Get the ideal bond length. */
    public double getIdealBondLength(){
	if(idealBondLength < 0.0f){
	    idealBondLength = (float)firstAtom.distance(secondAtom);
	}

	return idealBondLength;
    }

    /** Get the actual bond length. */
    public double getBondLength(){
	if(firstAtom != null && secondAtom != null)
	    return firstAtom.distance(secondAtom);
	
	return -1.0;
    }

    /** String representation of the bond. */
    @Override
    public String toString(){
	int firstId = firstAtom.getId();
	int secondId = secondAtom.getId();

	return
	    firstAtom.getAtomSymbol() + firstId +
	    getBondSymbol() +
	    secondAtom.getAtomSymbol() + secondId;
    }

    /** Get unit vector. */
    public Point3d getUnitVector(){
	return Point3d.unitVector(firstAtom, secondAtom);
    }
}
