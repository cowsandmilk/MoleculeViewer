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
import java.util.*;

/* Copyright Astex Technology Ltd. 1999 */

/*
 * 02-11-99 mjh
 *	created
 */

/**
 * Class for holding information about ring systems in molecular structures.
 */
public class Ring extends Generic {
    /** The atoms in the ring. */
    private List<Atom> atoms = new ArrayList<Atom>(6);

    /** The bonds in the ring. */
    private List<Bond> bonds = new ArrayList<Bond>(6);

    /**
     * Private default constructor.
     */
    public Ring(){
    }

    /**
     * Add an atom to the ring.
     */
    public void addAtom(Atom a){
	atoms.add(a);
    }

    /**
     * Add a bond to the ring.
     */
    public void addBond(Bond b){
	bonds.add(b);
    }

    /**
     * Get a bond.
     */
    private Bond getBond(int index){
	return bonds.get(index);
    }

    /** Return the atom count. */
    public int getAtomCount(){
	return atoms.size();
    }

    /** Return the bond count. */
    private int getBondCount(){
	return bonds.size();
    }

    /**
     * Does this ring contain the specified atom?
     */
    private boolean contains(Atom queryAtom){
	for(Atom atom: atoms){
	    if(atom == queryAtom){
		return true;
	    }
	}

	return false;
    }

    /**
     * Does this ring contain the specified bond?
     */
    public boolean contains(Bond queryBond){
	for(Bond bond: bonds){
	    if(bond == queryBond){
		return true;
	    }
	}

	return false;
    }

    /** Is this ring aromatic? */
    public boolean isAromatic(){
	int atomCount = getAtomCount();
		
	if(atomCount < 5 || atomCount > 6){
	    return false;
	}

	// should always be the same as atomCount...
	int bondCount = getBondCount();
	boolean allAromatic = true;

	for(Bond bond: bonds){
	    if(bond.getBondOrder() != Bond.AromaticBond){
		allAromatic = false;
		break;
	    }
	}

	// if all the bonds were actually marked as aromatic
	// we can return true
	if(allAromatic){
	    return true;
	}

	// count the hetero atoms in the ring.

	int heteroAtomCount = 0;

	for(Atom atom: atoms){
	    if(atom.getElement() != PeriodicTable.CARBON){
		heteroAtomCount++;
	    }
	}

	// otherwise we can look for a set of alternating
	// double and single bonds

	if(bondCount == 6){
	    allAromatic = true;

	    for(int i = 0; i < bondCount; i++){
		Bond bond = getBond(i);
		Bond nextBond = getBond(i == bondCount - 1 ? 0 : i + 1);
		int bondOrder = bond.getBondOrder();
		int nextBondOrder = nextBond.getBondOrder();

		if((bondOrder == Bond.SingleBond &&
		    nextBondOrder == Bond.DoubleBond) ||
		   (nextBondOrder == Bond.SingleBond &&
		    bondOrder == Bond.DoubleBond)){
		    continue;
		}else{
		    allAromatic = false;
		    break;
		}
	    }

	    if(allAromatic){
		return true;
	    }
	}else if(atomCount == 5){
	    int doubleBondCount = 0;
	    for(Bond bond: bonds){
		if(bond.getBondOrder() == Bond.DoubleBond){
		    doubleBondCount++;
		}
	    }

	    if(heteroAtomCount > 0 && doubleBondCount > 1){
		return true;
	    }
	}

	return false;
    }

    /** Get the ring center. */
    public Point3d getRingCenter(){
	Point3d center = new Point3d();

	int atomCount = getAtomCount();

	assert(atomCount > 0);
	for(Atom atom: atoms){
	    center.add(atom);
	}

	center.divide(atomCount);

	return center;
    }
}

