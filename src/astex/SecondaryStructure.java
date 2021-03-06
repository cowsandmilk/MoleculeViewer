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
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Class for assigning secondary structure to a protein molecule.
 */
public class SecondaryStructure {
    /** Cut off for NH...O distance. */
    private static double MaxHBondDistance = 5.5;

    private static Tmesh tm = null;

    /** Assign secondary structure to residues in a protein molecule. */
    public static Tmesh assign(List<Molecule> molecules){
	tm = new Tmesh();

	for(Molecule mol : molecules){
	    assignMolecule(mol);
	}

	return tm;
    }

    // static workspace for secondary structure assignment
    private static final int MaxResidues = 10000;

    private static Residue residues[]  = new Residue[MaxResidues];
    private static Point3d hpos[]      = new Point3d[MaxResidues];
    private static Atom opos[]         = new Atom[MaxResidues];
    private static Residue.SS types[]  = new Residue.SS[MaxResidues];
    private static int mapping[]       = new int[MaxResidues];
    private static IntArrayList hbond_no[] = new IntArrayList[MaxResidues];
    private static IntArrayList hbond_on[] = new IntArrayList[MaxResidues];

    // number of residues including gaps in molecule.
    private static int nres = 0;

    public static boolean debug = false;

    /** Assign secondary structure for this molecule. */
    private static void assignMolecule(Molecule mol){
	Arguments args        = new Arguments();
	double hbondConstant  = args.getDouble("hbond.constant", -999.0);
	double hbondCutoff    = args.getDouble("hbond.cutoff", -999.0);

	nres = 0;
	int realRes = 0;

	int chainCount = mol.getChainCount();

	for(int c = 0; c < chainCount; c++){
	    Chain chain = mol.getChain(c);
	    int residueCount = chain.getResidueCount();
	    for(int r = 0; r < residueCount; r++){
		Residue res = chain.getResidue(r);

		res.setSecondaryStructure(Residue.SS.Coil);

		// need to assign gaps here to 
		// stop helix hbonds being fooled by gaps

		residues[nres] = res;
		types[nres] = res.getSecondaryStructure();
		hbond_no[nres] = new IntArrayList();
		hbond_on[nres] = new IntArrayList();
		hpos[nres] = null;
		opos[nres] = null;
		mapping[realRes] = nres;
		realRes++;
		nres++;
	    }

	    // add 4 residue gaps to stop helix
	    // h-bond spanning different chains.
	    for(int i = 0; i < 4; i++){
		residues[nres] = null;
		types[nres] = Residue.SS.Undefined;
		hbond_no[nres] = new IntArrayList();
		hbond_on[nres] = new IntArrayList();
		hpos[nres] = null;
		opos[nres] = null;
		nres++;
	    }
	}

	// generate amide hydrogen positions.
	// gather amide oxygen positions
	for(int r1 = 0; r1 < nres; r1++){
	    if(residues[r1] != null){
		hpos[r1] = getAmideHydrogen(residues[r1]);
		opos[r1] = residues[r1].getAtom("O");
	    }
	}

	IntArrayList neighbours = new IntArrayList();

	Lattice ol = new Lattice(MaxHBondDistance * 1.05);

	for(int r2 = 0; r2 < nres; r2++){
	    if(opos[r2] != null){
		Point3d o = opos[r2];
		ol.add(r2, o.x, o.y, o.z);
	    }
	}

	// assign mainchain hydrogen bonds.
	for(int r1 = 0; r1 < nres; r1++){
	    // NH...O
	    Point3d h = hpos[r1];
	    if(h != null){
		Atom n = residues[r1].getAtom("N");

		neighbours.clear();

		ol.getPossibleNeighbours(r1, n.x, n.y, n.z,
					 neighbours, true);

		int neighbourCount = neighbours.size();

		for(int i = 0; i < neighbourCount; i++){
		    int oid = neighbours.getInt(i);
		    Atom o = opos[oid];
		    
		    if(o != null){
			Atom c = o.getBondedAtom("C");

			double e = MoleculeRenderer.hbondEnergy(n, h, o, c, hbondConstant);

			if(e < hbondCutoff){
			    
			    hbond_no[r1].add(oid);
			    hbond_on[oid].add(r1);
			    
			    if(debug){
				System.out.println("adding NH..O " +
						   residues[r1] + " to " +
						   residues[oid] + " d=" + o.distance(h));
			    }
			}
		    }else{
			Log.error("shouldn't be a null reference in o lattice");
		    }
		}
	    }
	}

	// put turns in...
	for(int r1 = 3; r1 < nres; r1++){
	    if(hbonded(r1, r1 - 3)){
		for(int r2 = r1 - 2; r2 < r1; r2++){
		    types[r2] = Residue.SS.Turn;
		}
	    }
	}

	// now look for helices
	for(int r1 = 1; r1 < nres-4; r1++){
	    if(hbonded(r1 + 3, r1 - 1) &&
	       hbonded(r1 + 4, r1)){
		for(int r2 = r1; r2 <= r1 + 3; r2++){
		    types[r2] = Residue.SS.Helix;
		}
	    }
	    if(hbonded(r1 + 2, r1 - 1) &&
	       hbonded(r1 + 3, r1)){
		for(int r2 = r1; r2 <= r1 + 2; r2++){
		    types[r2] = Residue.SS.Helix;
		}
	    }
	}

	// assign beta-sheet secondary structure
	// according to basic rules in
	// Kabsch and Sander, Biopolymers, 22, 2577-2637 (1983).
	// anti-parallel
	for(int ri = 0; ri < nres; ri++){
	    if(types[ri] == Residue.SS.Coil || types[ri] == Residue.SS.Sheet){
		int hbondCount = hbond_no[ri].size();
		if(debug && hbondCount > 0){
		    System.out.println("checking residue " + residues[ri] + " " +
				       hbondCount + " hbonds");
		}
		for(int hb = 0; hb < hbondCount; hb++){
		    int rj = hbond_no[ri].getInt(hb);
		    if(debug){
			System.out.println("hydrogen bonded to " + residues[rj]);
		    }
		    if(ri < rj && rj >= 0 && rj < nres){
			if(debug){
			    System.out.println("## ri < rj rj valid");
			    System.out.println("### type is coilri < rj rj valid");
			}
			if((hbonded(ri, rj)     && hbonded(rj,ri))){
			    //(hbonded(ri-1, rj+1) && hbonded(rj-1,ri+1))){
			    // anti-parallel
			    if(debug){
				System.out.println("### anti-parallel");
			    }

			    assignSheetType(ri);
			    assignSheetType(rj);
			    assignSheetType(ri-1);
			    assignSheetType(rj+1);

			    if(Math.abs(ri - rj) >= 5){
				assignSheetType(ri+1);
				assignSheetType(rj-1);
			    }
			}
		    }
		}
	    }
	}

	for(int ri = 0; ri < nres; ri++){
	    if(types[ri] == Residue.SS.Coil || types[ri] == Residue.SS.Sheet){
		int hbondCount = hbond_no[ri].size();
		for(int hb = 0; hb < hbondCount; hb++){
		    int rrj = hbond_no[ri].getInt(hb);
		    for(int rj = rrj - 1; rj < rrj + 2; rj++){
			if((rj >= 0 && rj < nres) &&
			    ((hbonded(ri, rj-1) && hbonded(rj+1,ri)) ||
			     (hbonded(rj-1, ri) && hbonded(ri,rj+1)))){
			    // parallel
			    assignSheetType(ri);
			    assignSheetType(rj);
			    if(Math.abs(ri - rj) >= 5){
				assignSheetType(rj+1);
			    }
			    assignSheetType(rj-1);
			}
		    }
		}
	    }
	}

	// regularise the assignments
	regulariseSS(types, nres);

	nres = 0;
	    
	// gather the initial assignments.
	for(int c = 0; c < chainCount; c++){
	    Chain chain = mol.getChain(c);
	    int residueCount = chain.getResidueCount();

	    // copy them back
	    for(int r = 0; r < residueCount; r++){
		Residue res = chain.getResidue(r);
		res.setSecondaryStructure(types[mapping[nres++]]);
	    }
	}
    }

    private static void assignSheetType(int r){
	if(r >= 0 && r < nres &&
	   (types[r] == Residue.SS.Coil || types[r] == Residue.SS.Sheet)){
	    types[r] = Residue.SS.Sheet;
	}
    }

    /** Is there a mainchain h-bond from O to N of the two residues. */
    private static boolean hbonded(int ri, int rj){
	if(ri < 0 || ri >= nres || rj < 0 || rj >= nres){
	    return false;
	}

	if(hbond_no[ri].contains(rj)){
	    return true;
	}
	
	return false;
    }

    private static Point3d getAmideHydrogen(Residue r){
	if(r == null){
	    return null;
	}

	Atom N = r.getAtom("N");

	if(N == null){
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

    private static void regulariseSS(Residue.SS types[], int n){
	// single residue gaps in sheets/helix
	for(int r = 1; r < n - 1; r++){
	    if(types[r] == Residue.SS.Coil &&
	       ((types[r-1] == Residue.SS.Sheet &&
		 types[r+1] == Residue.SS.Sheet) ||
		((types[r-1] == Residue.SS.Helix &&
		  types[r+1] == Residue.SS.Helix)))){

		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] == Residue.SS.Helix &&
	       ((types[r-1] == Residue.SS.Sheet &&
		 types[r+1] == Residue.SS.Sheet))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] == Residue.SS.Sheet &&
	       ((types[r-1] != Residue.SS.Sheet &&
		 types[r+1] != Residue.SS.Sheet))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = types[r-1];
	    } else if(types[r] != Residue.SS.Coil &&
	       ((types[r-1] == Residue.SS.Coil &&
		 types[r+1] == Residue.SS.Coil))){
		if(debug){
		    System.out.println("changing " + residues[r]);
		}
		types[r] = Residue.SS.Coil;

	    } else if(types[r] == Residue.SS.Undefined){
		types[r] = Residue.SS.Coil;
	    }
	}
	

	// get rid of the two residue sheet.

	for(int r = 0; r < n - 2; r++){
	    if(r == 0 &&
	       types[r]   == Residue.SS.Sheet &&
	       types[r+1] == Residue.SS.Sheet &&
	       types[r+2] != Residue.SS.Sheet){
		System.out.println("removing 2 residue strand at n-terminus");
		types[r] = types[r+1] = Residue.SS.Coil;
	    }else if(r == n - 2 && 
	       types[r]   != Residue.SS.Sheet &&
	       types[r+1] == Residue.SS.Sheet &&
	       types[r+2] == Residue.SS.Sheet){
		types[r+1] = types[r+2] = Residue.SS.Coil;
		System.out.println("removing 2 residue strand at c-terminus");
	    }else if(
	       types[r]   != Residue.SS.Sheet &&
	       types[r+1] == Residue.SS.Sheet &&
	       types[r+2] == Residue.SS.Sheet &&
	       types[r+3] != Residue.SS.Sheet){
		System.out.println("removing 2 residue internal strand");
		types[r+1] = types[r+2] = Residue.SS.Coil;
	    }
	}

	for(int r = 0; r < n; r++){
	    if(types[r] == Residue.SS.Turn){
		types[r] = Residue.SS.Coil;
	    }
	}
    }
}
