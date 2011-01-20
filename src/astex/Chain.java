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

import java.util.*;

/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 08-11-99 mjh
 *	created
 */

/**
 * A class for storing a group of atoms that form part
 * of a protein chain.
 */
public class Chain implements Selectable {
    /** Dynamic array of residues. */
    private List<Residue> residues = new ArrayList<Residue>(100);

    /** Undefined residue name. */
    private static final String undefinedChainName = "X";

    /** Parent molecule. */
    private Molecule parent;
	
    /** Set the parent molecule. */
    public void setParent(Molecule molecule){
	parent = molecule;
    }

    /** Get the parent. */
    public Molecule getParent(){
	return parent;
    }

    /** Chain name. */
    private String name = null;

    /** Set the chain name. */
    public void setName(String newName){
	name = newName;
    }

    /** Get the chain name. */
    public String getName(){
	if(name == null)
	    return undefinedChainName;

        return name;
    }

    /**
     * Return the number of atoms in the molecule.
     */
    public int getResidueCount(){
	return residues.size();
    }

    /**
     * Return the specified atom.
     */
    public Residue getResidue(int index){
	return residues.get(index);
    }

    /** The current residue. */
    private Residue currentResidue;

    /** Add a residue to the chain. */
    public Residue addResidue(){
	currentResidue = new Residue();
	currentResidue.setParent(this);

	residues.add(currentResidue);

	return currentResidue;
    }
	
    /** Remove a residue from the chain. */
    public void removeResidue(Residue res){
	residues.remove(res);
    }

    /** Return the current residue. */
    public Residue getCurrentResidue(){
	if(currentResidue == null){
	    addResidue();
	}
		
	return currentResidue;
    }

    public String selectStatement(){
	Molecule mol = getParent();
	String molSelect = mol.selectStatement();
	return "chain '" + getName() + "' and " + molSelect;
    }

    /** Apply a selection recursively. */
    public int select(int state){
	int selectCount = 0;
	for(int r = 0; r < getResidueCount(); r++){
	    Residue residue = getResidue(r);
	    selectCount += residue.select(state);
	}

	return selectCount;
    }
}
