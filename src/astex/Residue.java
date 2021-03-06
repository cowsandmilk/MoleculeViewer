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

/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 08-11-99 mjh
 *	created
 */

import astex.generic.*;

import java.awt.Color;
import java.util.*;

/**
 * A class for storing a group of atoms that form part
 * of a protein residue.
 */
public class Residue extends Generic implements Selectable {
    /** Default constructor. */
    public Residue(){
        set(ResidueColor, Color.white);
        set(Torsions, Boolean.FALSE);
        set(TorsionRadius, Double.valueOf(0.4));
        set(TorsionGreek, Boolean.TRUE);
        set(TorsionFormat, "<3d=true,size=0.3>%t %.1f");
    }

    /** Dynamic array of atoms. */
    private List<Atom> atoms = new ArrayList<Atom>(6);

    /** Undefined residue number. */
    public static final int undefinedResidueNumber = -9999;

    /** Undefined residue name. */
    private static final String undefinedResidueName = "XXX";

    /** Parent chain. */
    private Chain parent = null;
	
    /** Set the parent chain. */
    public void setParent(Chain chain){
	parent = chain;
    }

    /** Get the parent. */
    public Chain getParent(){
	return parent;
    }

    /** Insertion code. */
    private char insertionCode = ' ';

    /** Set the insertion code. */
    public void setInsertionCode(char code){
	insertionCode = code;
    }

    /** Get the insertion code. */
    public char getInsertionCode(){
	return insertionCode;
    }

    /** Residue name. */
    private String name = null;

    /** Set the residue name. */
    public void setName(String newName){
	name = newName;

        String colorName = Settings.getString("residue", name + ".color", "0x000000");

        int c = Color32.getColorFromName(colorName);

        Color color = new Color(c);

        set(ResidueColor, color);
    }

    /** Get the residue name. */
    public String getName(){
	if(name == null)
	    return undefinedResidueName;

        return name;
    }
    
    public enum SS {
	Undefined, Sheet, Helix, Helix310, Turn, Coil
    }

    /** The secondary structure type. */
    private SS secondaryStructure = SS.Coil;
    
    /**
     * Get the value of secondaryStructure.
     * @return value of secondaryStructure.
     */
    public SS getSecondaryStructure() {
	return secondaryStructure;
    }
    
    /**
     * Set the value of secondaryStructure.
     * @param v  Value to assign to secondaryStructure.
     */
    public void setSecondaryStructure(SS  v) {
	this.secondaryStructure = v;
    }
    
    /** Residue number. */
    private int number = undefinedResidueNumber;

    /** Sequential sequence number. */
    private int sequentialNumber = undefinedResidueNumber;

    public void setSequentialNumber(int sNumber){
	sequentialNumber = sNumber;
    }

    /** Set the residue number. */
    public void setNumber(int newNumber){
	number = newNumber;
    }

    /** Get the sequential residue number. */
    public int getSequentialNumber(){
	if(sequentialNumber == undefinedResidueNumber)
	    return 1;

	return sequentialNumber;
    }

    /** Get the residue number. */
    public int getNumber(){
	if(number == undefinedResidueNumber)
	    return 1;

        return number;
    }

    /** Add an atom to the list. */
    public void addAtom(Atom atom){
	atoms.add(atom);
    }

    /** Remove an atom from the residue. */
    public void removeAtom(Atom atom){
	atoms.remove(atom);

	if(atoms.isEmpty()){
	    Chain chain = getParent();

	    chain.removeResidue(this);
	}
    }

    /**
     * Return the number of atoms in the molecule.
     */
    public int getAtomCount(){
	return atoms.size();
    }

    /**
     * Return the specified atom.
     */
    public Atom getAtom(int index){
	return atoms.get(index);
    }

    /** Return the atom with the given name. */
    public Atom getAtom(String nm){
	return getAtom(nm, 'A');
    }

    /** Return the atom with the given name. */
    private Atom getAtom(String nm, char code){
	for(Atom a : atoms){
	    // atom has the appropriate name
	    // and insertion code ' ' or 'A'
	    if(a.getAtomLabel().equals(nm) &&
	       (a.getInsertionCode() == ' ' ||
		a.getInsertionCode() == code)){
		return a;
	    }
	}

	return null;
    }

    /** Default to alternate location 'A'. */
    public Atom findAtom(String name){
	return findAtom(name, 'A');

    }

    /**
     * Similar to getAtom(String name) but will search in
     * the previous residue if the name ends with '-' or
     * the next residue if the name ends with '+'.
     *
     * Maybe this should be rolled into one function but
     * prefer this way in case names genuinely end
     * with + or -. This method is only used by torsion
     * angle search functions.
     */
    private Atom findAtom(String name, char code){
	// atom is in previous or next residue
	// we need to go up to the chain and find the
	// relevant residue
	Residue r = this;
	
	if(name.endsWith("-") || name.endsWith("+")){
	    Chain chain = getParent();
	    int residueCount = chain.getResidueCount();
	    int residuePos = -1;

	    for(int i = 0; i < residueCount; i++){
		Residue res = chain.getResidue(i);
		if(res == this){
		    residuePos = i;
		    break;
		}
	    }

	    if(residuePos != -1){
		if(name.endsWith("-") && residuePos > 0){
		    r = chain.getResidue(residuePos - 1);
		}else if(name.endsWith("+") && residuePos < residueCount - 1){
		    r = chain.getResidue(residuePos + 1);
		}else{
		    r = null;
		}
	    }else{
		r = null;
	    }

	    // now we have found the residue, remove the
	    // '-' or the '+' character so that we will
	    // find it
	    name = name.substring(0, name.length() - 1);
	}

	if(r == null)
	    return null;

	return r.getAtom(name, code);
    }

    /** Is this residue a standard amino acid. */
    public boolean isStandardAminoAcid(){
	return Selection.aminoacidNames.contains(name.trim());
    }

    /** Is this residue an ion. */
    public boolean isIon(){
	return Selection.ionNames.contains(name.trim());
    }

    /** Is this a solvent residue. */
    public boolean isSolvent(){
	return Selection.solventNames.contains(name.trim());
    }

    /** Is this a solvent residue. */
    public boolean isNucleicAcid(){
	return Selection.dnaNames.contains(name.trim());
    }

    public String selectStatement(){
	Chain chain = getParent();
	String chainSelect = chain.selectStatement();
	StringBuilder command = new StringBuilder(16);

	command.append("residue ").append(getNumber());

	command.append(" and name '").append(getName()).append("'");

	char insertionCode = getInsertionCode();

	if(insertionCode != ' '){
	    command.append(" and insertion '").append(insertionCode).append("'");
	}

	return command.append(" and ").append(chainSelect).toString();
    }

    /** Apply a selection recursively. */
    public int select(int state){
	int selectCount = 0;
	for(Atom atom: atoms){
	    selectCount += atom.select(state);
	}

	return selectCount;
    }

    /** Print out a residue. */
    @Override
    public String toString(){
	Chain c = getParent();

	return c.getName() + ":" + getNumber();
    }

    public static final String ResidueColor  = "color";
    private static final String Torsions      = "torsions";
    private static final String TorsionRadius = "torsionRadius";
    public static final String TorsionGreek  = "torsionGreek";
    public static final String TorsionFormat = "torsionFormat";
}
