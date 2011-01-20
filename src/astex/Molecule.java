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

/* Copyright Astex Technology Ltd. 1999 */
/* Copyright David Hall, Boston University, 2011 */

/*
 * 07-07-04 mjh
 *	add ability to handle CONECT record bonds
 *	don't connect atoms if they both had bonds
 *	specified by CONECT records (use Atom.ConectRecords);
 * 21-08-02 mjh
 *	implement linked cell algorithm for connectivity
 *	determination. much faster especially for big
 *	structures.
 * 08-08-02 mjh
 *	improve performance of loading big .mol2 files where
 *	there are explicit bonds. the caching was only working
 *	about half the time, new method addBondFromIds() will
 *	retrieve the atoms on the basis of id, but cache the
 *	first atom index as this gives good search performance.
 * 19-06-00 mjh
 *	reintroduce generation of all ring impropers, due to 
 *	concerns about how effectively the current scheme
 *	maintains planarity
 * 17-02-00 mjh
 *	add method markRingBonds() that will mark ring bonds by
 *	constructing the minimum spanning tree.
 * 15-12-99 mjh
 *	make 4 membered rings only generate one improper. saves us
 *	3 impropers over the simple minded way. also only generate
 *	bondCount - 3 impropers for any ring. This enforces planarity
 * 14-12-99 mjh
 *	add definitions of angles and impropers to molecule structure.
 *	simplifies generation of dictionaries and force field parameters.
 * 14-11-99 mjh
 *	improve performance of connect() by caching bonding radii
 *	also use atoms array directly to avoid calls to getAtom()
 * 07-11-99 mjh	
 *	make changes to support pdb files. getRing() or getRingCount() will
 *	now call findRings if it hasn't already been called.
 *	findRings() will only check atoms that have more than 1 bond
 * 05-11-99 mjh
 *	make addBond set the bond order to SingleBond and add method that
 *	allows specification of the bond order.
 * 01-11-99 mjh
 *	extend functionality for addBond so that bonds are added to the
 *	atoms as they are created.
 * 28-10-99 mjh
 *	created
 */

import astex.generic.*;
import java.util.*;

public class Molecule extends Generic implements Selectable {
    /** Dynamic array of atoms. */
    private List<Atom> atoms = new ArrayList<Atom>(20);

    /** Dynamic array of bonds. */
    private List<Bond> bonds = new ArrayList<Bond>(20);

    /** Dynamic array of rings. */
    private List<Ring> rings = new ArrayList<Ring>(1);

    /** Dynamic array of chains. */
    private List<Chain> chains = new ArrayList<Chain>(1);

    /** The molecule name as a string. */
    private String moleculeName = null;

    /** The filename that the molecule came from. */
    private String filename = null;

    /** The type of molecule. */
    private String type = null;

    /** The center of the molecule. */
    private Point3d center = null;

    /** The radius of the molecule. */
    private double radius = 0.0;

    /** The symmetry object for this molecule. */
    private Symmetry symmetry = null;

    /** Flags for various properties. */
    private int flags;

    /* Bit masks for various properties. */

    /** Have rings been assigned. */
    private static final int RingsAssigned = 0x1;

    /** Total number of residues. */
    private static int residueCount = 0;
    
    /* Constants for various overall display styles. */

    /** Normal display style. */
    public static final int Normal = 1;

    /** Backbone trace. */
    public static final int Trace = 2;

    /** Backbone trace and atoms. */
    public static final int TraceAlways = 3;

    /** The display style for the molecule. */
    private int displayStyle = Normal;

    /** Is the molecule displayed or not. */
    private boolean displayed = true;

    /** What type of molecule is this. */
    private int moleculeType = NormalMolecule;

    /** Type for a normal molecule. */
    public static final int NormalMolecule   = 1;
    public static final int FeatureMolecule  = 2;
    public static final int SkeletonMolecule = 3;
    public static final int SymmetryMolecule = 4;

    /** Number of central atoms for .istr files. */
    private int centralAtomCount = 0;

    /** Set the number of central atoms. */
    public void setCentralAtomCount(int cac){
	centralAtomCount = cac;
    }

    /** Get the number of central atoms. */
    public int getCentralAtomCount(){
	return centralAtomCount;
    }

    /** Return what type of molecule this is. */
    public int getMoleculeType(){
	return moleculeType;
    }

    /** Set what type of molecule this is. */
    public void setMoleculeType(int t){
	moleculeType = t;
    }

    /** Constructor which prepares a standard molecule. */
    public Molecule(){
	set(DisplayHydrogens, Boolean.TRUE);
        set(DisplayBondDetails, Boolean.TRUE);
    }

    /** Generate a string representation. */
    @Override
    public String toString(){
	StringBuilder ret = new StringBuilder(getName());
	int atomCount = getAtomCount();
	int bondCount = getBondCount();
	int chainCount = getChainCount();

	if(chainCount > 1){
	    ret.append(" ").append(chainCount).append(" chains");
	}

	ret.append(" ").append(atomCount).append(" atoms");
	ret.append(" ").append(bondCount).append(" bonds");

	return ret.toString();
    }

    /** Set Display style. */
    public void setDisplayStyle(int style){
	displayStyle = style;
    }

    /** Get the display style. */
    public int getDisplayStyle(){
	return displayStyle;
    }

    /** Get the display style. */
    public boolean getDisplayStyle(int style){
	return (displayStyle & style) > 0;
    }

    /** Set if the molecule is on or off. */
    public void setDisplayed(int newState){
	if(newState == 0){
	    displayed = false;
	}else if(newState == 1){
	    displayed = true;
	}else if(newState == 2){
	    displayed = !displayed;
	}else{
	    System.out.println("setDisplayed: invalid state " + newState);
	}
    }

    /** Is the molecule displayed? */
    public boolean getDisplayed(){
	return displayed;
    }

    /** Return the number of atoms in the molecule. */
    public int getAtomCount(){
	return atoms.size();
    }

    /** Return the specified atom. */
    public Atom getAtom(int index){
	return atoms.get(index);
    }

    /** The atom we will start searching at when looking for id's. */
    private int startAtom = 0;

    /** Return the atom with the specified id. */
    //the idea behind the use of startAtom is that
    //this function is often used to look up the two
    //atoms in a bond, so... they are likely near each other
    //hopefully the first leaves startAtom a couple before the second
    public Atom getAtomWithId(int id){
	for(ListIterator<Atom> it = atoms.listIterator(startAtom); it.hasNext();){
	    Atom atom = it.next();

	    if(atom.getId() == id){
		startAtom =it.previousIndex(); //set to be the atom found
		return atom;
	    }
	}

	// we didn't find it so search backwards from startAtom
	for(ListIterator<Atom> it = atoms.listIterator(startAtom); it.hasNext();){
	    Atom atom = it.previous();
	    if(atom.getId() == id){
		startAtom = it.nextIndex();
		return atom;
	    }
	}

	// it really wasn't there...?
	return null;
    }

    /** Assign atom numbers. */
    public void assignAtomNumbers(){
	int a = 0;
	for(Atom atom: atoms){
	    atom.setId(a++);
	}
    }

    /** Is this a symmetry molecule. */
    public boolean isSymmetryMolecule(){
	if(moleculeName != null &&
	   moleculeName.startsWith("Symmetry")){
	    return true;
	}

	return false;
    }

    /** Return the number of bonds in the molecule. */
    public int getBondCount(){
	return bonds.size();
    }

    /** Return the specified bond. */
    public Bond getBond(int index){
	return bonds.get(index);
    }

    /** Make sure that rings have been assigned for the molecule. */
    private void ensureRingsAssigned(){
	if((flags & RingsAssigned) == 0){
	    findRings();
	    flags |= RingsAssigned;
	}
    }

    /** Return the number of rings. */
    public int getRingCount(){
	ensureRingsAssigned();

	return rings.size();
    }

    /** Return the number of chains. */
    public int getChainCount(){
	return chains.size();
    }

    /** Return the specified chain. */
    public Chain getChain(int index){
	return chains.get(index);
    }

    /** The current chain to which atoms are added. */
    private Chain currentChain;

    /** Get the current chain. */
    private Chain getCurrentChain(){
	if(currentChain == null){
	    addChain();
	}

	return currentChain;
    }

    /** Get the total number of residues. */
    public int getResidueCount(){
	int totalResidues = 0;

	for(Chain chain : chains){
	    totalResidues += chain.getResidueCount();
	}

	return totalResidues;
    }

    /** Add a chain to the molecule. */
    public Chain addChain(){
	currentChain = new Chain();
	currentChain.setParent(this);

	chains.add(currentChain);

	return currentChain;
    }

    /** Add a residue to the current chain. */
    public Residue addResidue(){
	Chain chain = getCurrentChain();
		
	Residue residue = chain.addResidue();

	residue.setSequentialNumber(residueCount++);

	return residue;
    }

    /** Add an atom to the molecule and return the reference to it. */
    public Atom addAtom(){
	Atom newAtom = Atom.create();

	atoms.add(newAtom);

	Residue currentResidue = currentChain.getCurrentResidue();

	currentResidue.addAtom(newAtom);
		
	newAtom.setParent(currentResidue);

	return newAtom;
    }

    /** Add a new ring to the molecule. */
    private Ring addRing(){
	Ring newRing = new Ring();
		
	rings.add(newRing);

	return newRing;
    }

    /** Add a bond to the molecule and return a reference to it. */
    public Bond addBond(Atom firstAtom, Atom secondAtom, int bondOrder){
	return addBond(firstAtom, secondAtom, bondOrder, true);
    }

    /** Add a bond to the molecule and return a reference to it. */
    public Bond addBond(Atom firstAtom, Atom secondAtom, int bondOrder, boolean explicit){
	// handle insertion codes for multiple positions of
	// atoms in pdb structures.
	int firstInsertion = firstAtom.getInsertionCode();
	int secondInsertion = secondAtom.getInsertionCode();

	Bond newBond = null;

	if((!explicit) && (firstAtom.hasExplicitBond() && secondAtom.hasExplicitBond())){
	    return null;
	}

	if(((firstInsertion == ' ' || secondInsertion == ' ') ||
	   firstInsertion == secondInsertion)&& !(firstAtom.isSolvent() || secondAtom.isSolvent())){
	    newBond = new Bond(firstAtom, secondAtom);

	    newBond.setBondOrder(bondOrder);

	    if(explicit){
		newBond.setExplicitBond(true);
	    }

	    bonds.add(newBond);

	    // and add the bond to the atoms
	    firstAtom.addBond(newBond);
	    secondAtom.addBond(newBond);
	}

	return newBond;
    }

    /** Add a bond to the molecule and return a reference to it. */
    public Bond addBond(int firstAtomIndex, int secondAtomIndex,
			int bondOrder){
	Atom firstAtom = getAtom(firstAtomIndex);
	Atom secondAtom = getAtom(secondAtomIndex);

	return addBond(firstAtom, secondAtom, bondOrder, true);
    }

    /** Add a bond to the molecule and return a reference to it. */
    public Bond addBondFromIds(int firstAtomIndex, int secondAtomIndex,
			       int bondOrder){
	// attempt to improve caching
	Atom a1 = getAtomWithId(firstAtomIndex);
	int searchAtom = startAtom;
	Atom a2 = getAtomWithId(secondAtomIndex);
	startAtom = searchAtom;
	Bond newBond = addBond(a1, a2, bondOrder, false);
	
	return newBond;
    }

    /** Remove an atom from the molecule. */
    public void removeAtom(Atom a){
        Residue residue = a.getResidue();
        residue.removeAtom(a);
	atoms.remove(a);
    }

    /** Remove a bond from the molecule. */
    public void removeBond(Bond b){
	bonds.remove(b);
    }

    /** Does this atom need treating specially for bonding. */
    private boolean isSpecialAtom(Atom atom){
	int element = atom.getElement();
	return (element == PeriodicTable.SULPHUR ||
	   element == PeriodicTable.PHOSPHORUS);
    }

    /* Linked cell connectivity calculation. */

    /** Is debugging on. */
    public boolean debug = false;

    /** The atom ids in a cell. */
    private int cell1[] = null;

    /** The atom ids in a cell. */
    private int cell2[] = null;

    /** The number of atoms in cell1. */
    private int nc1 = 0;

    /** The number of atoms in cell2. */
    private int nc2 = 0;

    /** Cache of the bonding radii used for a molecule. */
    private double bondingRadii[] = null;

    /** The list of cell positions for atoms. */
    private int list[] = null;

    /** The head pointers for each cell. */
    private int head[] = null;

    /** Generate a cell index for the given grid. */
    private int cellIndex(int ix, int iy, int iz,
			  int nx, int ny){
	return ix + iy * nx + iz * nx * ny;
    }

    /**
     * The cell offsets for the half space that generates
     * icell2 > icell1.
     */
    private int offsets[][] = {
	{-1,-1,-1},
	{ 0,-1,-1},
	{ 1,-1,-1},
	{-1, 0,-1},
	{ 0, 0,-1},
	{ 1, 0,-1},
	{-1, 1,-1},
	{ 0, 1,-1},
	{ 1, 1,-1},
	{-1,-1, 0},
	{ 0,-1, 0},
	{ 1,-1, 0},
	{-1, 0, 0},
    };

    /** Connect the atoms using a neighbour grid. */
    public void connect2(){
	double xmin = 1.e10, xmax = -1.e10;
	double ymin = 1.e10, ymax = -1.e10;
	double zmin = 1.e10, zmax = -1.e10;

	int atomCount = getAtomCount();

	bondingRadii = new double[atomCount];

	for(int a1 = 0; a1 < atomCount; a1++){
	    Atom atom = getAtom(a1);
	    bondingRadii[a1] = atom.getBondingRadius();
	}

	// Find the bounding box of the molecule
	for(Atom atom: atoms){
	    if(atom.x < xmin) xmin = atom.x;
	    if(atom.y < ymin) ymin = atom.y;
	    if(atom.z < zmin) zmin = atom.z;
	    if(atom.x > xmax) xmax = atom.x;
	    if(atom.y > ymax) ymax = atom.y;
	    if(atom.z > zmax) zmax = atom.z;
	}

	// Increase bounding box for comfort
	xmin -= 0.1; ymin -= 0.1; zmin -= 0.1;
	xmax += 0.1; ymax += 0.1; zmax += 0.1;

	if(debug){
	    System.out.println("xmin " + xmin);
	    System.out.println("ymin " + ymin);
	    System.out.println("zmin " + zmin);
	    System.out.println("xmax " + xmax);
	    System.out.println("ymax " + ymax);
	    System.out.println("zmax " + zmax);
	}

	// use fixed spacing and calculate the grid dimensions
	// alogorithm is reasonably invariant to grid size
	// even for really big structures.
	double spacing = 5.0;

	int nx = 1 + (int)((xmax - xmin) / spacing);
	int ny = 1 + (int)((ymax - ymin) / spacing);
	int nz = 1 + (int)((zmax - zmin) / spacing);

	int ncell = nx * ny * nz;

	// allocate space for working...
	// do it here so that mol files don't
	// allocate this memory (as they don't need it)
	cell1 = new int[1024];
	cell2 = new int[1024];

	if(debug){
	    System.out.println("nx " + nx + " ny " + ny + " nz " + nz);
	}

	head = new int[ncell];
	list = new int[atomCount];

	// initialise cell head pointers
	// -1 shows that the cell is empty
	for(int i = 0; i < ncell; i++){
	    head[i] = -1;
	}

	// put the atoms into the linked cell structure
	for(int a = 0; a < atomCount; a++){
	    Atom atom = getAtom(a);
	    int ix = (int)((atom.x - xmin)/spacing);
	    int iy = (int)((atom.y - ymin)/spacing);
	    int iz = (int)((atom.z - zmin)/spacing);
	    int icell = cellIndex(ix, iy, iz, nx, ny);

	    if(icell < 0 || icell >= ncell){
		System.out.println("invalid cell " + icell);
	    }

	    list[a] = head[icell];
	    head[icell] = a;
	}

	int icell = 0;
	int offsetlen = offsets.length;

	int occupiedCells = 0;

	for(int iz = 0; iz < nz; iz++){
	    for(int iy = 0; iy < ny; iy++){
		for(int ix = 0; ix < nx; ix++){
		    if(head[icell] != -1){
			occupiedCells++;
			connectCell(icell);

			for(int ioff = 0; ioff < offsetlen; ioff++){
			    int ix2 = ix - offsets[ioff][0];
			    int iy2 = iy - offsets[ioff][1];
			    int iz2 = iz - offsets[ioff][2];
			
			    if(ix2 >= 0 && ix2 < nx &&
			       iy2 >= 0 && iy2 < ny &&
			       iz2 >= 0 && iz2 < nz){
				int icell2 = cellIndex(ix2, iy2, iz2,
						       nx, ny);
				connectTwoCells(icell2);
			    }
			}
		    }
		    icell++;
		}
	    }
	}

	if(debug){
	    double percentOccupied =
		100.0 * (double)(occupiedCells)/(double)(ncell);
	    double atomsPerCell = (double)(atomCount)/(double)(occupiedCells);
	    System.out.println("total cells in grid    " + ncell);
	    System.out.println("occupied cells in grid " + occupiedCells);
	    System.out.println("% cells occupied       " + percentOccupied);
	    System.out.println("average atoms per cell " + atomsPerCell);
	}

	// try and release the memory we allocated
	// no guarantee that this will garbage collect it
	// but at least it will be available when gc() runs
	bondingRadii = null;
	list = null;
	head = null;
	cell1 = null;
	cell2 = null;

	System.gc();
    }

    /** Get the contents of the cell. */
    private int getCellContents(int icell, int c[]){
	int nc = 0;
	int j = head[icell];

	if(j == -1){
	    return 0;
	}

	while(j >= 0){
	    c[nc++] = j;
	    j = list[j];
	}

	return nc;
    }

    /**
     * Connect the contents of two cells.
     * Assumes that the target cell has been correctly
     * connected and the cell contents are in cell1
     */
    private void connectTwoCells(int icell2){
	// only need to get the contents of cell2
	nc2 = getCellContents(icell2, cell2);

	// don't forget to look up the atom ids
	// in the cell array.
	for(int i = 0; i < nc1; i++){
	    int i1 = cell1[i];
	    Atom a1 = atoms.get(i1);
	    double r1 = bondingRadii[i1];

	    for(int j = 0; j < nc2; j++){
		int i2 = cell2[j];
		Atom a2 = atoms.get(i2);
		double r2 = bondingRadii[i2];
		double d2 = r1 + r2;
		d2 *= d2;

		if(a1.distanceSq(a2) < d2){
		    addBond(a1, a2, Bond.SingleBond, false);
		}
	    }
	}
    }

    /** Connect the contents of one cell up. */
    private void connectCell(int icell){
	nc1 = getCellContents(icell, cell1);

	// don't forget to look up the atom ids
	// in the cell array.
	for(int i = 0; i < nc1; i++){
	    int i1 = cell1[i];
	    Atom a1 = atoms.get(i1);
	    double r1 = bondingRadii[i1];
	    for(int j = i + 1; j < nc1; j++){
		int i2 = cell1[j];
		Atom a2 = atoms.get(i2);
		double r2 = bondingRadii[i2];
		double d2 = r1 + r2;
		d2 *= d2;

		if(a1.distanceSq(a2) < d2){
		    addBond(a1, a2, Bond.SingleBond, false);
		}
	    }
	}
    }

    /** Connect the atoms in a molecule using standard bonding radii. */
    public void connect(){
	int atomCount = getAtomCount();

	// store all of the bonding radii so we don't have to
	// keep looking them up.
	double bondingRadii[] = new double[atomCount];

	for(int a1 = 0; a1 < atomCount; a1++){
	    Atom atom = getAtom(a1);
	    bondingRadii[a1] = atom.getBondingRadius();
	}

	// now check each atom.
	for(int a1 = 0; a1 < atomCount; a1++){
	    Atom firstAtom = atoms.get(a1);
	    double firstRadius = bondingRadii[a1];
	    int startAtom = 0;
	    int endAtom = atomCount;

	    boolean specialAtom = isSpecialAtom(firstAtom);

	    if(specialAtom){
		// its a special atom so we need to check all others
		startAtom = 0;
		endAtom = atomCount;
	    }else{
		// its a regular atom so we only need to check in the next 50
		startAtom = a1 + 1;
		endAtom = a1 + 50;
		if(endAtom > atomCount){
		    endAtom = atomCount;
		}
	    }

	    for(int a2 = startAtom; a2 < endAtom; a2++){
		if(a2 != a1){
		    Atom secondAtom = atoms.get(a2);
		    double secondRadius = bondingRadii[a2];
		    double dSquare = firstRadius + secondRadius;

		    dSquare *= dSquare;

		    if(firstAtom.distanceSq(secondAtom) < dSquare){
			if(specialAtom){
			    Bond bond = firstAtom.getBond(secondAtom);
						
			    if(bond == null){
				addBond(firstAtom, secondAtom, Bond.SingleBond, false);
			    }
			}else{
			    addBond(firstAtom, secondAtom, Bond.SingleBond, false);
			}
		    }
		}
	    }
	}
    }

    /** Find rings in the structure. */
    private void findRings(){

	// assign ids to the atoms so that we can compare
	// ordering for creating rings.
	int atomCount = getAtomCount();
	int oldIds[] = new int[atomCount];

	for(int i = 0; i < atomCount; i++){
	    Atom atom = getAtom(i);
	    oldIds[i] = atom.getId();
	    atom.setId(i + 1);
	}

	try {
	    for(int ringSize = 3; ringSize <= 6; ringSize++){
		findRings(ringSize);
	    }
	}catch(Exception e){
	    e.printStackTrace();
	}finally{
	    for(int i = 0; i < atomCount; i++){
		Atom atom = getAtom(i);
		atom.setId(oldIds[i]);
	    }

	    oldIds = null;
	}
    }

    /** Find rings of a particular size. */
    private void findRings(int ringSize){
	if(ringSize == 3){
	    find3Rings();
	}else{
	    // allocate space for keeping track of the 
	    // path we take through the molecule.
	    Atom atomPath[] = new Atom[ringSize];
	    Bond bondPath[] = new Bond[ringSize];

	    for(int a = 0; a < getAtomCount(); a++){
		Atom atom = getAtom(a);

				// no point looking if we only have one bond
		if(atom.getBondCount() > 1){
		    propagateRingSearch(0, ringSize,
					atom, atomPath, bondPath);
		}
	    }
	}
    }

    /** Find 3 membered rings by direct search. */
    private void find3Rings(){
	for(int a = 0; a < getAtomCount(); a++){
	    Atom atom = getAtom(a);
	    int bondCount = atom.getBondCount();

	    for(int i = 0; i < bondCount; i++){
		Bond bondi = atom.getBond(i);
		Atom atomi = bondi.getOtherAtom(atom);

		for(int j = 0; j < bondCount; j++){
                    if( i == j) continue;
		    Bond bondj = atom.getBond(j);
		    Atom atomj = bondj.getOtherAtom(atom);
					
		    Bond bondij = atomi.getBond(atomj);

		    if((bondij != null)&& (atom.getId() < atomi.getId() &&
			   atomi.getId() < atomj.getId())){
			// its a three membered ring.
			Ring newRing = addRing();
			newRing.addAtom(atom);
			newRing.addAtom(atomi);
			newRing.addAtom(atomj);
			newRing.addBond(bondi);
			newRing.addBond(bondij);
			newRing.addBond(bondj);
		    }
		}
	    }
	}
    }

    /** Recursive method for propagating the search for rings. */
    private void propagateRingSearch(int currentDepth, int maximumDepth,
				     Atom currentAtom,
				     Atom atomPath[], Bond bondPath[]){

	if(currentDepth == maximumDepth && currentAtom == atomPath[0]){

	    possiblyCreateRing(atomPath, bondPath, maximumDepth);
	}else if(currentDepth < maximumDepth){

	    atomPath[currentDepth] = currentAtom;

	    for(int b = 0; b < currentAtom.getBondCount(); b++){
		Bond bond = currentAtom.getBond(b);
		Atom otherAtom = bond.getOtherAtom(currentAtom);

		if(currentDepth == 0 || 
		   otherAtom != atomPath[currentDepth - 1]){
		    bondPath[currentDepth] = bond;
					
		    propagateRingSearch(currentDepth + 1, maximumDepth,
					otherAtom, atomPath, bondPath);
		}
	    }
	}
    }

    /**
     * Create a ring if the atoms are in the right order.
     *
     * The ring is only created if
     * 1. the first atom has the lowest index.
     * 2. the second atom has a lower index than the last atom.
     */
    private void possiblyCreateRing(Atom atomPath[], Bond bondPath[],
				   int ringSize){
	int firstAtomId = atomPath[0].getId();

	for(int i = 1; i < ringSize; i++){
	    Atom atom = atomPath[i];

	    if(firstAtomId >= atom.getId()){
		return;
	    }
	}

	if(atomPath[1].getId() >= atomPath[ringSize - 1].getId()){
	    return;
	}

	// ok, we have passed all the test add the ring
	Ring newRing = addRing();

	for(int i = 0; i < ringSize; i++){
	    newRing.addAtom(atomPath[i]);
	    newRing.addBond(bondPath[i]);
	}
    }

    /** Return the best ring containing this bond. */
    Ring getBestRingContainingBond(Bond bond){
	ensureRingsAssigned();

	for(int i = 1; i >= 0; i--){
	    for(Ring ring: rings){
		if(ring.contains(bond) && ring.getAtomCount() == (6 - i) &&
		   ring.isAromatic()){
		    return ring;
		}
	    }
	}

	for(Ring ring: rings){
	    if(ring.contains(bond)){
		return ring;
	    }
	}
					
	return null;
    }

    /** Set the molecule name */
    public void setName(String name){
	moleculeName = name;
    }

    /** Get the molecule name. */
    public String getName(){
	if(moleculeName == null)
	    return "Unnamed molecule";

	return moleculeName;
    }

    /** Set the filename. */
    public void setFilename(String s){
	filename = s;
    }	
    
    /** Get the filename. */
    public String getFilename(){
	return filename;
    }

    /** Set the type. */
    public void setType(String s){
	type = s;
    }

    /** Get the type. */
    public String getType(){
	return type;
    }

    /** Find the center of the molecule. */
    public Point3d getCenter(){
	if(center == null){
	    center = new Point3d();

	    int atomCount = getAtomCount();
	    for(int a = 0; a < atomCount; a++){
		Atom atom = getAtom(a);

		center.add(atom);
	    }

	    if(atomCount > 0){
		center.divide(atomCount);
	    }
	}

	return center.clone();
    }

    /** Get the radius of the molecule. */
    public double getRadius(){
	Point3d moleculeCenter = getCenter();

	radius = 0.0;

	int atomCount = getAtomCount();
	for(int a = 0; a < atomCount; a++){
	    Atom atom = getAtom(a);

	    double dSq = moleculeCenter.distanceSq(atom);

	    if(dSq > radius){
		radius = dSq;
	    }
	}

	radius = Math.sqrt(radius);

	return radius;
    }

    /** Make sure that the symmetry object is allocated. */
    private void ensureSymmetryAllocated(){
	if(symmetry == null){
	    symmetry = new Symmetry();
	}
    }

    /** Set the unit cell. */
    public void setUnitCell(double newCell[]){
	ensureSymmetryAllocated();

	symmetry.setUnitCell(newCell);
    }

    /** Get the symmetry object. */
    public Symmetry getSymmetry(){
	return symmetry;
    }

    /** Set the symmetry entry for this molecule. */
    public void setSymmetry(Symmetry s){
	symmetry = s;
    }

    /** Set the space group name. */
    public void setSpaceGroupName(String name){
	if(name == null){
	    symmetry = null;
	}else{
	    ensureSymmetryAllocated();
	    symmetry.setSpaceGroupName(name);
	}
    }

    /* Implementation of Selectable. */

    public String selectStatement(){
	return "molexact '" + getName() + "'";
    }

    /** Apply a selection recursively. */
    public int select(int state){
	int selectCount = 0;
	for(Chain chain : chains){
	    selectCount += chain.select(state);
	}

	return selectCount;
    }

    public static final String Displayed = "displayed";
    public static final String DisplayHydrogens = "hydrogens";
    public static final String DisplayBondDetails = "bondDetails";

    @Override
    public Object set(Object key, Object property){
        String name = (String)key;

        if(name.equals(Displayed)){
            setDisplayed(((Boolean)property).booleanValue() ? 1 : 0);
        }

        super.set(key, property);

        // should return old value
        return null;
    }

    @Override
    public Object get(Object key, Object def){
        if(key.equals(Displayed)){
            return Boolean.valueOf(getDisplayed());
        }

        return super.get(key, def);
    }
}
