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

package astex.design;

import astex.*;
import java.util.*;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ActiveSite {
    /**
     * Handle an active site command.
     */
    public static void handleCommand(MoleculeViewer mv, Arguments args){
	List<Atom> superstar  = (List<Atom>)args.get("-superstar");
	List<Atom> asp        = (List<Atom>)args.get("-asp");
	List<Atom> spheres    = (List<Atom>)args.get("-spheres");
	List<Atom> pass       = (List<Atom>)args.get("-pass");

	exclusion  = (List<Atom>)args.get("-exclusion");

	setupExclusion();

	if(superstar != null){
	    generateSuperstarMap(mv, args, superstar);
	}else if(asp != null){
	    generateAspMap(mv, args, asp);
	}else if(spheres != null){
	    generateTangentSpheres(mv, args, spheres);
	}else if(pass != null){
	    Molecule mol = PASS.generatePASS(args, pass);
	    mv.addMolecule(mol);
	}

	System.gc();
    }

    private static void setupExclusion(){
	if(exclusion != null){
	    // get the exclusion atoms
	    // create the lattice object
	    lattice = new Lattice(4.1);

	    for(ListIterator<Atom> it = exclusion.listIterator(); it.hasNext();){
		int i = it.nextIndex();
		Atom atom = it.next();
		lattice.add(i, atom.x, atom.y, atom.z);
	    }
	}
    }

    private static Lattice lattice = null;
    private static List<Atom> exclusion = null;

    /** Arbitrary size for radius table. */
    private static final int MaxElements   = 200;

    /** The atomic number based radius table. */
    private static double vdwRadii[]       = new double[MaxElements];

    /** Various constants used in the superstar procedure. */
    private static double dtol             = 0.4;
    private static double rOffset          = 0.5;
    private static double rmsdWarningLevel = 0.1;
    private static double rmsdFailLevel    = 0.5;

    /** The hash of probe molecule names for this superstar map. */
    private static HashMap<String, Molecule> probeMolecules = null;

    /** Set up the radii from the superstar property file. */
    private static void setupRadii(){
	for(int r = 0; r < MaxElements; r++){
	    double rvdw = Settings.getDouble("superstar", "radius." + r, 1.4);
	    vdwRadii[r] = rvdw;
	}

	rOffset = Settings.getDouble("superstar", "radius.offset");

	System.out.println("radiusOffset is " + rOffset);

	rmsdWarningLevel = Settings.getDouble("superstar", "rmsd.warning");
	
	rmsdFailLevel = Settings.getDouble("superstar", "rmsd.fail");

	Log.info("rmsd fail level is %.1f", rmsdFailLevel);

	dtol = Settings.getDouble("superstar", "radius.dtol");

	Log.info("dtol is %.1f", dtol);
    }

    /**
     * Generate a simplified superstar map.
     */
    private static void generateSuperstarMap(MoleculeViewer mv,
					    Arguments args,
					    List<Atom> superstar){
	// The prefix for molecule names
	String molNamePrefix = args.getString("-prefix", "superstar");
	String type          = args.getString("-type", null);
        MoleculeRenderer mr  = mv.getMoleculeRenderer();

	if(molNamePrefix == null){
	    molNamePrefix = args.getString("-superstarprefix", "superstar");
	}

	// if we have no type we must return
	if(type == null){
	    Log.error("no map type defined");
	    return;
	}

	// make sure we have the superstar properties
	// before we do anything else.
	setupRadii();

	// hash for the istr molecules
	probeMolecules = new HashMap<String, Molecule>(200);

	for(Atom atom : superstar){
	    atom.setTemporarilySelected(false);
	}

	// generate the map name
	String mapName = molNamePrefix + "_" + type;

	// create a map and set it up
	astex.Map map = mr.getMap(mapName);
	boolean newMap = false;

	if(map == null){
	    Log.info("creating new map " + mapName);
	    map = astex.Map.createSimpleMap();
	    newMap = true;
	}

	map.setName(mapName);
	map.setFile(mapName);


	// initialise the map
	initialiseMap(args, map, superstar);

	for(int groups = 0; groups < 1000; groups++){
	    String groupLabel = "group." + groups;

	    String groupName = Settings.getString("superstar", groupLabel);

	    if(groupName != null){
		String groupTypeLabel = groupName + ".type";
		String groupType = Settings.getString("superstar",
						      groupTypeLabel);

		if(type.equals(groupType)){
		    Log.info(groupName);

		    processSuperstarGroup(mv, args, superstar,
					  groupName, molNamePrefix, map);
		}
	    }
	}

	finaliseMap(mapName, map);

        if(newMap){
            mr.addMap(map);
        }

	probeMolecules = null;
	exclusion      = null;
	lattice        = null;
    }

    /** The space in which we contribute the small map. */
    private static float scatterPlot[] = null;

    /** Set up the map. */
    private static astex.Map initialiseMap(Arguments args,
                                           astex.Map map,
					   List<Atom> superstarAtoms){
	if(superstarAtoms.isEmpty()){
	    // no atoms so do nothing.
	    return null;
	}

        // set of atoms over which field is defined
        List<Atom> boxAtoms = (List<Atom>)args.get("-boxatoms");

        if(boxAtoms == null || boxAtoms.isEmpty()){
            boxAtoms = superstarAtoms;
        }

	// spacing for the map
	double spacing    = args.getDouble("-mapspacing", 0.5);
	double gridBorder = args.getDouble("-border", 5.0);

	double xmin =  1.e10, ymin =  1.e10, zmin =  1.e10;
	double xmax = -1.e10, ymax = -1.e10, zmax = -1.e10;

	for(Atom atom : boxAtoms){
	    if(atom.x > xmax) xmax = atom.x;
	    if(atom.x < xmin) xmin = atom.x;
	    if(atom.y > ymax) ymax = atom.y;
	    if(atom.y < ymin) ymin = atom.y;
	    if(atom.z > zmax) zmax = atom.z;
	    if(atom.z < zmin) zmin = atom.z;
	}

	xmin -= gridBorder;
	ymin -= gridBorder;
	zmin -= gridBorder;
	xmax += gridBorder;
	ymax += gridBorder;
	zmax += gridBorder;

	map.origin.x = xmin;
	map.origin.y = ymin;
	map.origin.z = zmin;

	map.spacing.x = spacing;
	map.spacing.y = spacing;
	map.spacing.z = spacing;

	map.ngrid[0] = 1 + (int)(0.5 + (xmax - xmin)/spacing);
	map.ngrid[1] = 1 + (int)(0.5 + (ymax - ymin)/spacing);
	map.ngrid[2] = 1 + (int)(0.5 + (zmax - zmin)/spacing);
	
	int gridPoints = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	map.data = new float[gridPoints];

	scatterPlot = new float[gridPoints];

	// the map will accumulate probabilities and
	// so needs setting to 1.0 initially
	for(int i = 0; i < gridPoints; i++){
	    map.data[i] = 1.0f;
	}

	return map;
    }

    /** Set up the contour levels etc. */
    private static void finaliseMap(String mapName,
				    astex.Map map){
	int gridPoints      = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	double fmin    =  1.e10;
	double fmax    = -1.e10;
	double logfmin =  1.e10;
	double logfmax = -1.e10;

	for(int i = 0; i < gridPoints; i++){
	    double v = map.data[i];

	    if(v < fmin) fmin = v;
	    if(v > fmax) fmax = v;

	    // clear out the remaining 1.0's
	    if(map.data[i] == 1.0f){
		map.data[i] = 0.0f;
	    }else if(map.data[i] != 0.0f){
		map.data[i] = (float)Math.log(map.data[i]);

		if(map.data[i] < logfmin){
		    logfmin = map.data[i];
		}

		if(map.data[i] > logfmax){
		    logfmax = map.data[i];
		}
	    }
	}

	// now set zero to the minimum log
	// value (as Math.ln(0.0f) = inf
	for(int i = 0; i < gridPoints; i++){
	    if(map.data[i] == 0.0f){
		map.data[i] = (float)logfmin;
	    }
	}

	Log.info("minimum value    %8.2f", fmin);
	Log.info("maximum value    %8.2f", fmax);

	Log.info("minimum log value %8.3f", logfmin);
	Log.info("maximum log value %8.3f", logfmax);

	double startLevel = 4.5;

	String colors[] = {
	    "red",
	    "orange",
	    "yellow",
	};

	if(mapName.indexOf("donor") != -1){
	    startLevel = 2.5;
	    colors[0] = "blue";
	    colors[1] = "0x5ad2ff";
	    colors[2] = "cyan";
	}else if(mapName.indexOf("ali") != -1){
	    startLevel = 4.5;
	    colors[0] = "green";
	    colors[1] = "0x69ff69";
	    colors[2] = "0xc3ffc3";
	}else if(mapName.indexOf("aro") != -1){
	    startLevel = 4.5;
	    colors[0] = "brown";
	    colors[1] = "0xb41e00";
	    colors[2] = "0xd24b00";
	}

	for(int i = 0; i < astex.Map.MaximumContourLevels; i++){
            map.setContourLevel(i, startLevel + i);
            map.setContourDisplayed(i, true);
            map.setContourStyle(i, astex.Map.Lines);
            map.setContourColor(i, Color32.getColorFromName(colors[i]));
	}
    }


    /** Calculate grid boundary. */
    private static void gridBoundary(astex.Map map,
				     Point3d p, double r,
				     int gmin[], int gmax[]){
	int xp = (int)(0.5 + (p.x - map.origin.x)/map.spacing.x);
	int yp = (int)(0.5 + (p.y - map.origin.y)/map.spacing.y);
	int zp = (int)(0.5 + (p.z - map.origin.z)/map.spacing.z);
	
	int gx = 3 + (int)(r / map.spacing.x);
	int gy = 3 + (int)(r / map.spacing.y);
	int gz = 3 + (int)(r / map.spacing.z);

	gmin[0] = xp - gx;
	gmin[1] = yp - gy;
	gmin[2] = zp - gz;

	gmax[0] = xp + gx;
	gmax[1] = yp + gy;
	gmax[2] = zp + gz;

	for(int i = 0; i < 3; i++){
	    if(gmin[i] < 0)             gmin[i] = 0;
	    if(gmax[i] >= map.ngrid[i]) gmax[i] = map.ngrid[i];
	}
    }

    /** Calculate coordinates of grid point. */
    private static void gridPoint(astex.Map map,
				  int i, int j, int k,
				  Point3d p){
	p.x = map.origin.x + i * map.spacing.x;
	p.y = map.origin.y + j * map.spacing.y;
	p.z = map.origin.z + k * map.spacing.z;
    }

    /** Calculate the grid index for the grid point. */
    private static int gridIndex(astex.Map map, int i, int j, int k){
	return i + j * map.ngrid[0] + k * map.ngrid[0] * map.ngrid[1];
    }

    /** Prepare the current scatter plot mask. */
    private static void prepareScatterPlotRegion(astex.Map map,
						 float scatterPlot[],
						 double probeRadius,
						 List<Atom> centralAtoms,
						 int gmin[], int gmax[]){
	// grid min, max for each atom
	int gamin[] = new int[3];
	int gamax[] = new int[3];

	for(int i = 0; i < 3; i++){
	    gmin[i] = Integer.MAX_VALUE;
	    gmax[i] = 0;
	}

	if(centralAtoms == null){
	    Log.error("centralAtoms was null");
	    return;
	}

	double rp = probeRadius;

	for(Atom catom: centralAtoms){
	    double rc = vdwRadii[catom.getElement()];
	    double r = rp + rc + rOffset;

	    gridBoundary(map, catom, r, gamin, gamax);

	    for(int i = 0; i < 3; i++){
		if(gamin[i] < gmin[i]) gmin[i] = gamin[i];
		if(gamax[i] > gmax[i]) gmax[i] = gamax[i];
	    }
	}

	Point3d gp = new Point3d();

	for(int i = gmin[0]; i < gmax[0]; i++){
	    for(int j = gmin[1]; j < gmax[1]; j++){
		for(int k = gmin[2]; k < gmax[2]; k++){
		    int index = gridIndex(map, i, j, k);

		    scatterPlot[index] = 1.0f;

		    gridPoint(map, i, j, k, gp);

		    for(Atom catom : centralAtoms){
			double rc = vdwRadii[catom.getElement()];
			double r = rc - dtol;
			
			if(gp.distanceSquared(catom) < r*r){
			    scatterPlot[index] = 0.0f;
			    break;
			}
		    }
		}
	    }
	}
    }

    /** Multiply in the partial scatter plot. */
    private static void multiplyScatterPlot(astex.Map map,
					    float scatterPlot[],
					    int gmin[], int gmax[]){
	double spmin =  1.e10;
	double spmax = -1.e10;
	double mmin =  1.e10;
	double mmax = -1.e10;

	for(int i = gmin[0]; i < gmax[0]; i++){
	    for(int j = gmin[1]; j < gmax[1]; j++){
		for(int k = gmin[2]; k < gmax[2]; k++){
		    int index = gridIndex(map, i, j, k);
		    double v = scatterPlot[index];

		    if(v < spmin) spmin = v;
		    if(v > spmax) spmax = v;

		    map.data[index] *= v;

		    if(map.data[index] > mmax) mmax = map.data[index];
		    if(map.data[index] < mmin) mmin = map.data[index];
		}
	    }
	}
    }

    /** The number of grid points. */
    private static int gridOffset = 2;

    private static int gmin[] = new int[3];
    private static int gmax[] = new int[3];

    private static int    included[] = new int[1000];
    private static double contrib[]  = new double[1000];

    /** Trim the atoms that are in the superstar molecule. */
    private static void mapSuperstarMolecule(List<Atom> centralAtoms,
					     List<Atom> scatterPlotAtoms,
					     double probeRadius,
					     astex.Map map){
	int ninc = 0;

	double expConst = 2.*0.5*0.5;

	// form volume
	double volume = map.spacing.x * map.spacing.y * map.spacing.z;

	prepareScatterPlotRegion(map, scatterPlot,
				 probeRadius, centralAtoms,
				 gmin, gmax);

	Point3d pp = new Point3d();

	for(Atom atom : scatterPlotAtoms){
	    double d = 1.0 / (atom.getBFactor() * volume);

	    //Log.info("contribution %f", d);

	    int xp = (int)(0.5 + (atom.x - map.origin.x)/map.spacing.x);
	    int yp = (int)(0.5 + (atom.y - map.origin.y)/map.spacing.y);
	    int zp = (int)(0.5 + (atom.z - map.origin.z)/map.spacing.z);

	    if(xp < gmin[0] || xp >= gmax[0] ||
	       yp < gmin[1] || yp >= gmax[1] ||
	       zp < gmin[2] || zp >= gmax[2]){
                continue;
	    }

	    {
		int bxmin = Math.max(xp - gridOffset, 0);
		int bxmax = Math.min(xp + gridOffset, map.ngrid[0]);
		int bymin = Math.max(yp - gridOffset, 0);
		int bymax = Math.min(yp + gridOffset, map.ngrid[1]);
		int bzmin = Math.max(zp - gridOffset, 0);
		int bzmax = Math.min(zp + gridOffset, map.ngrid[2]);

		ninc = 0;

		for(int i = bxmin; i < bxmax; i++){
		    for(int j = bymin; j < bymax; j++){
			for(int k = bzmin; k < bzmax; k++){
			    gridPoint(map, i, j, k, pp);
			    double r2 = pp.distanceSquared(atom);
			    if(r2 < 1.0){
				int index = gridIndex(map, i, j, k);
				double v = Math.exp(-r2/expConst);
				included[ninc] = index;
				contrib[ninc] = v;
				ninc++;
			    }
			}
		    }
		}

		// calculate normalisation factor
		double norm = 0.0;

		for(int i = 0; i < ninc; i++){
		    norm += contrib[i] * contrib[i];
		}

		norm = Math.sqrt(norm);

		for(int i = 0; i < ninc; i++){
		    int index = included[i];
		    scatterPlot[index] += d * contrib[i] / norm;
		}
	    }
	}
	
	multiplyScatterPlot(map, scatterPlot, gmin, gmax);
    }

    /** Handle a superstar group definition. */
    private static void processSuperstarGroup(MoleculeViewer mv,
					      Arguments args,
					      List<Atom> superstar,
					      String groupName,
					      String molPrefix,
					      astex.Map map){
	MoleculeRenderer mr = mv.getMoleculeRenderer();
	String typeLabel  = groupName + ".type";
	String istr       = groupName + ".istr";
	String scaleLabel = groupName + ".scale";

	String istrName   = Settings.getString("superstar", istr);
	String typeString = Settings.getString("superstar", typeLabel);

	boolean keepScatterPlots = args.getBoolean("-scatterplots", false);

	if(typeString == null){
	    Log.error("no type for group " + groupName);
	    Log.error("fitting abandoned");
	    return;
	}

	// generate the molecule name from prefix and group type
	String moleculeName = molPrefix + "_" + typeString;

	Molecule superstarMol = null;

	if(keepScatterPlots && superstarMol == null){
	    // remove any old ones of that name...
	    mr.removeMoleculeByName(moleculeName);

	    superstarMol = new Molecule();
	    superstarMol.setName(moleculeName);
	    superstarMol.setMoleculeType(Molecule.SkeletonMolecule);
	    mv.addMolecule(superstarMol);
	    Log.info("creating " + moleculeName);
	}
	
	// look for the istr molecule 
	Molecule istrMol = probeMolecules.get(istrName);

	if(istrMol == null){
	    istrMol = MoleculeIO.read(istrName);
	    if(istrMol == null){
		Log.error("couldn't load " + istrName);
		return;
	    }

	    boolean keepCarbons = false;

	    if(typeString.indexOf("lipo") != -1){
		keepCarbons = true;
	    }

	    markUnwantedAtoms(istrMol, keepCarbons);

	    probeMolecules.put(istrName, istrMol);
	}

	double plotScale = 1.0;

	String scaleString = Settings.getString("superstar", scaleLabel);

	if(scaleString != null){
	    plotScale = FILE.readDouble(scaleString);
	    Log.info("plotScale %5.2f", plotScale);
	}

	List<String> pdbMap = new ArrayList<String>(220);
	List<String> istrMap = new ArrayList<String>(220);

	// look for the mappings
	for(int i = 0; i < 1000; i++){
	    String mapString = groupName + "." + i;

	    String namePairString = Settings.getString("superstar", mapString);

	    // absence of the next name pair indicates
	    // end of name pairs
	    if(namePairString == null){
		break;
	    }

	    String namePairs[] = FILE.split(namePairString, ",");

	    pdbMap.add(namePairs[0]);
	    istrMap.add(namePairs[1]);
	}

	List<Atom> pdbAtoms = new ArrayList<Atom>(pdbMap.size());
	List<Atom> istrAtoms = new ArrayList<Atom>(istrMap.size());

	for(String idLabel : istrMap){
	    int id = FILE.readInteger(idLabel);

	    Atom a = istrMol.getAtomWithId(id);

	    istrAtoms.add(a);
	}

	int mapCount = pdbMap.size();

	if(mapCount == 0){
	    Log.error("no mappings for " + groupName);
	    return;
	}

	String atomSelection[] = FILE.split(pdbMap.get(0), ".");

	if("*".equals(atomSelection[0])){
	    atomSelection[0] = null;
	}

	for(Atom atom : superstar){
	    Residue residue = atom.getResidue();

	    if(atom.getAtomLabel().equals(atomSelection[1]) &&
	       (atomSelection[0] == null ||
		residue.getName().equals(atomSelection[0]))){
		pdbAtoms.clear();

		pdbAtoms.add(atom);

		for(int match = 1; match < pdbMap.size(); match++){
		    String s = pdbMap.get(match);
		    Atom matchAtom = residue.findAtom(s);
		    
		    if(matchAtom == null){
			System.out.println("couldn't find match atom " + s +
					   " for " + residue);
			break;
		    }
		    pdbAtoms.add(matchAtom);
		    s = istrMap.get(match);
		}
		//FIXME I don't think pdbAtoms.size() can equal pdbMap.size()
		if(pdbAtoms.size() == pdbMap.size()){
		    fitSuperstarGroup(pdbAtoms, istrAtoms,
				      superstarMol, istrMol, plotScale, map);
		}
	    }
	}
    }

    /** Mark those atoms we don't want in a molecule. */
    private static void markUnwantedAtoms(Molecule mol, boolean keepCarbons){
	int centralAtomCount = mol.getCentralAtomCount();
	int istrCount        = mol.getAtomCount();

	// skip the central group atoms
	for(int i = centralAtomCount; i < istrCount; i++){
	    Atom a = mol.getAtom(i);
	    int elementa = a.getElement();

	    a.setOccupancy(-1.0);

	    if((keepCarbons || elementa != PeriodicTable.CARBON) &&
	       elementa != PeriodicTable.HYDROGEN){
		double rc = vdwRadii[elementa];

		for(int j = 0; j < centralAtomCount; j++){
		    Atom catom = mol.getAtom(j);
		    int celement = catom.getElement();
		    double rr = vdwRadii[celement];
		    double rcheck = rc + rr + rOffset;

		    if(a.distanceSquared(catom) < rcheck*rcheck){
			// record that this was an active atom
			a.setOccupancy(1.0);
			break;
		    }
		}
	    }
	}
    }

    private static DoubleArrayList x = new DoubleArrayList();
    private static DoubleArrayList y = new DoubleArrayList();
    private static DoubleArrayList z = new DoubleArrayList();
    private static DoubleArrayList xp = new DoubleArrayList();
    private static DoubleArrayList yp = new DoubleArrayList();
    private static DoubleArrayList zp = new DoubleArrayList();

    private static IntArrayList neighbours  = new IntArrayList();

    private static void fitSuperstarGroup(List<Atom> pdbAtoms,
					  List<Atom> istrAtoms,
					  Molecule superstarMol,
					  Molecule istrMol,
					  double plotScale,
					  astex.Map map){
	List<Atom> scatterPlotAtoms = new ArrayList<Atom>(20);

	int nfit = pdbAtoms.size();

	x.clear();
	y.clear();
	z.clear();
	xp.clear();
	yp.clear();
	zp.clear();

	for(int i = 0; i < nfit; i++){
	    Atom a = pdbAtoms.get(i);
	    x.add(a.x);
	    y.add(a.y);
	    z.add(a.z);

	    // mark the pdb atom as having been in a central group
	    a.setTemporarilySelected(true);

	    a = istrAtoms.get(i);
	    xp.add(a.x);
	    yp.add(a.y);
	    zp.add(a.z);

	}

	Matrix rot = new Matrix();

	double rmsd = Fit.fit(x.toDoubleArray(),
				    y.toDoubleArray(),
				    z.toDoubleArray(),
				    xp.toDoubleArray(),
				    yp.toDoubleArray(),
				    zp.toDoubleArray(), nfit, rot);

	if(rmsd > rmsdWarningLevel){
	    Atom baseAtom = pdbAtoms.get(0);
	    Residue res = baseAtom.getResidue();

	    Log.warn("residue " + res + " rmsd=%5.2f", rmsd);

	    if(rmsd > rmsdFailLevel){
		Log.error("fitting abandoned");
		return;
	    }
	}

	Point3d p            = new Point3d();
	int centralAtomCount = istrMol.getCentralAtomCount();
	int istrCount        = istrMol.getAtomCount();
	boolean addit        = true;
	Atom cacheHit        = null;

	int boxx             = Integer.MIN_VALUE;
	int boxy             = Integer.MIN_VALUE;
	int boxz             = Integer.MIN_VALUE;

	double probeRadius   = 0.0;

	// skip the central group atoms
	for(int i = centralAtomCount; i < istrCount; i++){
	    Atom a = istrMol.getAtom(i);
	    int elementa = a.getElement();
	    double ra    = vdwRadii[elementa];
	    probeRadius  = ra;

	    // occupancy > 0.0 indicates it is an
	    // atom we want to proceed with
	    if(a.getOccupancy() > 0.0){
		p.set(a);
		
		rot.transform(p);
		
		addit = true;
		
		// if there were exclusion atoms
		// check them for collisions
		if(exclusion != null){
		    if(cacheHit != null){
			int elementc = cacheHit.getElement();
			double rc    = vdwRadii[elementc];
			double rvdw  = (ra + rc) - 2.0 * dtol;

			if(cacheHit.distanceSquared(p) < rvdw*rvdw){
			    addit = false;
			}else{
			    cacheHit = null;
			}
		    }

		    if(addit){
			// check for whether we are looking
			// for neighbours in the same cell
			// as before.
			int pboxx = lattice.BOX(p.x);
			int pboxy = lattice.BOX(p.y);
			int pboxz = lattice.BOX(p.z);

			if(pboxx != boxx ||
			   pboxy != boxy ||
			   pboxz != boxz){
			    neighbours.clear();
			    lattice.getPossibleNeighbours(-1, p.x, p.y, p.z,
							  neighbours, true);
			    boxx = pboxx;
			    boxy = pboxy;
			    boxz = pboxz;
			}

			int ncount = neighbours.size();
		    
			for(int n = 0; n < ncount; n++){
			    int id             = neighbours.getInt(n);
			    Atom atomNeighbour = exclusion.get(id);
			    int elementn       = atomNeighbour.getElement();
			    double rn          = vdwRadii[elementn];
			    // allow closer approach to allow for hbonding
			    double rvdw        = (ra + rn) - 2.0 * dtol;
			    boolean ignore     = false;
			
			    if(p.distanceSquared(atomNeighbour) < rvdw*rvdw){
				// but we shouldn't allow collisions
				// with the actual fit atoms
				// of the central group itself
				for(int j = 0; j < nfit; j++){
				    Atom pdbAtom = pdbAtoms.get(j);
				    if(pdbAtom == atomNeighbour){
					ignore = true;
					break;
				    }
				}
			    
				if(!ignore){
				    addit = false;
				    cacheHit = atomNeighbour;
				    break;
				}
			    }
			}
		    }
		}

		// atom survived clipping by neighbours
		// so add it to the scatter plot
		if(addit){
		    // this should only be non-null if
		    // we are keeping scatterplots
		    if(superstarMol != null){
			//System.out.println("adding atom");
			Atom newAtom = superstarMol.addAtom();
			newAtom.setElement(a.getElement());
			newAtom.set(p);
			// multiply in the plot scale
			// at this point
			newAtom.setBFactor(a.getBFactor() / plotScale);
			newAtom.setCharge(0);
		    }

		    // hmm, duplicate atom creation here
		    Atom atom = Atom.create();
		    atom.setElement(a.getElement());
		    atom.set(p);
		    atom.setBFactor(a.getBFactor() / plotScale);
		    atom.setCharge(0);

		    scatterPlotAtoms.add(atom);
		}
	    }
	}

	mapSuperstarMolecule(pdbAtoms,
			     scatterPlotAtoms, probeRadius, map);

	// push the atoms from the scatter plot back
	// into the central atom cache
	for(Atom a : scatterPlotAtoms){
	    a.release();
	}
    }

    private static double xs[][] = {{0.0, 0.0, 0.0, 0.0},
				    {0.0, 0.0, 0.0, 0.0},
				    {0.0, 0.0, 0.0, 0.0},
    };
	
    private static double rs[] = {0.0, 0.0, 0.0, 0.0};

    private static final double xe[] = new double[4];

    private static final Atom atomQuad[] = new Atom[4];

    /**
     * Generate all spheres tangent to 4 spheres in the atom list.
     * The exclusion list is used to remove clashing spheres.
     */
    private static void generateTangentSpheres(MoleculeViewer mv,
					      Arguments args,
					      List<Atom> atoms){
	double max              = args.getDouble("-maxradius", 3.0);
	double min              = args.getDouble("-minradius", 1.5);
	double minAcceptedAngle = args.getDouble("-minangle", 1.5);
	String molName          = args.getString("-molecule", "spheres");

	int quadCount = 0;
	int sphereCount = 0;
	Point3d p = new Point3d();
		
	Molecule sphereMol = new Molecule();
	sphereMol.setName(molName);
	sphereMol.setMoleculeType(Molecule.SkeletonMolecule);
	mv.addMolecule(sphereMol);
	Log.info("creating molecule " + molName);

	double maxAngle = 0.0;

	for(ListIterator<Atom> it0 = atoms.listIterator(); it0.hasNext();){
	    int a0 = it0.nextIndex();
	    Atom atom0 = it0.next();
	    xs[0][0] = atom0.x; xs[1][0] = atom0.y; xs[2][0] = atom0.z;
	    rs[0] = atom0.getVDWRadius();
	    atomQuad[0] = atom0;

	    for(ListIterator<Atom> it1 = atoms.listIterator(a0+1); it1.hasNext();){
		int a1 = it1.nextIndex();
		Atom atom1 = it1.next();
		xs[0][1] = atom1.x; xs[1][1] = atom1.y; xs[2][1] = atom1.z;
		rs[1] = atom1.getVDWRadius();
		atomQuad[1] = atom1;

		double max01 = max + rs[0] + rs[1];

		if(atom0.distanceSquared(atom1) < max01*max01){
		    for(ListIterator<Atom> it2 = atoms.listIterator(a1+1); it2.hasNext();){
			int a2 = it2.nextIndex();
			Atom atom2 = it2.next();
			xs[0][2] = atom2.x;
			xs[1][2] = atom2.y;
			xs[2][2] = atom2.z;
			rs[2] = atom2.getVDWRadius();
			atomQuad[2] = atom2;

			double max02 = max + rs[0] + rs[2];
			double max12 = max + rs[1] + rs[2];

			if(atom0.distanceSquared(atom2) < (max02*max02) &&
			   atom1.distanceSquared(atom2) < (max12*max12)){
			    for(ListIterator<Atom> it3 = atoms.listIterator(a2+1); it3.hasNext();){
				Atom atom3 = it3.next();
				rs[3] = atom3.getVDWRadius();
				atomQuad[3] = atom3;

				double max03 = max + rs[0] + rs[3];
				double max13 = max + rs[1] + rs[3];
				double max23 = max + rs[2] + rs[3];

				if(atom0.distanceSquared(atom3) < (max03*max03) &&
				   atom1.distanceSquared(atom3) < (max13*max13) &&
				   atom2.distanceSquared(atom3) < (max23*max23)){
				    xs[0][3] = atom3.x;
				    xs[1][3] = atom3.y;
				    xs[2][3] = atom3.z;

				    boolean success = Apollo.tangentSphere(xs, rs, xe);

				    if(success && xe[3] < max && xe[3] > min){
					
					neighbours.clear();
					lattice.getPossibleNeighbours(-1, xe[0], xe[1], xe[2],
								      neighbours, true);
					
					p.set(xe[0], xe[1], xe[2]);
					
					boolean addit = true;
					
					int ncount = neighbours.size();
					
					for(int n = 0; n < ncount; n++){
					    int id = neighbours.getInt(n);
					    Atom aa = exclusion.get(id);
					    // skip the atoms that define the sphere.
					    if(aa == atom0 || aa == atom1 ||
					       aa == atom2 || aa == atom3){
						continue;
					    }
					    double r = xe[3] + aa.getVDWRadius();
					    if(p.distanceSquared(aa) < r*r){
						addit = false;
						break;
					    }
					}
					
					if(addit){
					    // check the angles
					    maxAngle = 0.0;

					    for(int s0 = 0; s0 < 4; s0++){
						for(int s1 = s0+1; s1 < 4; s1++){
						    double angle = Point3d.angle(atomQuad[s0],
										 p,
										 atomQuad[s1]);
						    angle = angle * 180.0/Math.PI;
						    if(angle > maxAngle){
							maxAngle = angle;
						    }
						}
					    }

					    if(maxAngle > minAcceptedAngle){
						Atom newAtom = sphereMol.addAtom();
						newAtom.set(p);
						newAtom.setVDWRadius(xe[3]);
						newAtom.setElement(PeriodicTable.UNKNOWN);
						sphereCount++;
					    }
					}
				    }
				    
				    quadCount++;
				}
			    }
			}
		    }
		}
	    }
	}

	Log.info("quadCount   %d", quadCount);
	Log.info("sphereCount %d", sphereCount);
    }

    /** Generate an Astex Statistical Potential map. */
    private static void generateAspMap(MoleculeViewer mv,
				      Arguments args,
				      List<Atom> aspAtoms){
	String aspPrefix = args.getString("-prefix", "asp");
	String probeName = args.getString("-type", "C.3");
	double maxd      = args.getDouble("-maxdistance", 6.0);
	MoleculeRenderer mr = mv.getMoleculeRenderer();

	// initialise the map
	String mapName = aspPrefix + "_" + probeName;
	
	// find where the maps are stored
	String location = Settings.getString("asp", "location");

	if(location == null){
	    Log.error("no location setting for asp pmf's");
	    return;
	}

	Log.info("location "+ location);

	// create a map and set it up
	astex.Map map = mr.getMap(mapName);
	boolean newMap = false;

	if(map == null){
	    Log.info("creating new map " + mapName);
	    map = astex.Map.createSimpleMap();
	    newMap = true;
	}

	map.setName(mapName);
	map.setFile(mapName);


	initialiseMap(args, map, aspAtoms);

	// build the lattice datastructure
	// but actually put the exclusion set into it
	int exclusionCount = exclusion.size();

	Lattice aspLattice = new Lattice(6.0);

	for(ListIterator<Atom> it = exclusion.listIterator(); it.hasNext();){
	    int i = it.nextIndex();
	    Atom aspAtom = it.next();
	    aspLattice.add(i, aspAtom.x, aspAtom.y, aspAtom.z);
	}

	// build the type information
	List<String> types = new ArrayList<String>(exclusionCount);
	HashMap<String, DoubleArrayList> pmfs = new HashMap<String, DoubleArrayList>(11);

	for(Atom atom : exclusion){
	    Residue res = atom.getResidue();
	    String description = res.getName() + "." + atom.getAtomLabel();

	    String type = Settings.getString("asp", description);

	    if(type == null){
		description = "*." + atom.getAtomLabel();

		type = Settings.getString("asp", description);

		if(type == null){
		    Log.warn("no type for " + description);
		    Log.warn("residue " + res.getName());
		    Log.warn("defaulting to C.3");
		    type = "C.3";
		}
	    }

	    String pmf = type + "_" + probeName;

	    if(pmfs.get(pmf) == null){
		loadPmf(pmfs, location, pmf);
	    }

	    types.add(pmf);
	}

	// now go through each atom, and sum the field
	for(ListIterator<Atom> it = exclusion.listIterator(); it.hasNext();){
	    int i = it.nextIndex();
	    Atom atom = it.next();
	    incorporatePotential(map, i, atom, types, pmfs, maxd);
	}
	
	int gridPoints = map.ngrid[0] * map.ngrid[1] * map.ngrid[2];

	double min =  1.e10;
	double max = -1.e10;

	for(int i = 0; i < gridPoints; i++){
	    map.data[i] = -map.data[i];
	    if(map.data[i] > max) max = map.data[i];
	    if(map.data[i] < min) min = map.data[i];
	}

	Log.info("map min %f", min);
	Log.info("map max %f", max);

	min = Math.rint(min);

	Log.info("round min %f", min);

	double startLevel = max - 3.0;

	String colors[] = new String[3];

	colors[0] = "green";
	colors[1] = "'0x69ff69'";
	colors[2] = "'0xc3ffc3'";

	for(int i = 0; i < astex.Map.MaximumContourLevels; i++){
            map.setContourLevel(i, startLevel - 2*i);
            map.setContourDisplayed(i, true);
            map.setContourStyle(i, astex.Map.Lines);
            map.setContourColor(i, Color32.getColorFromName(colors[i]));
	}

        if(newMap){
            mr.addMap(map);
        }
    }

    /** Add in the potential of an atom. */
    private static void incorporatePotential(astex.Map map, int iatom,
					    Atom atom, List<String> types,
					    HashMap<String,DoubleArrayList> pmfs, double maxd){
	String type = types.get(iatom);
	DoubleArrayList pmf = pmfs.get(type);

	if(pmf == null){
	    Log.error("couldn't find pmf for " + type);
	    return;
	}

	double maxd2 = maxd * maxd;

	int nx = map.ngrid[0];
	int ny = map.ngrid[1];
	int nz = map.ngrid[2];

	double ax = atom.x - map.origin.x;
	double ay = atom.y - map.origin.y;
	double az = atom.z - map.origin.z;

	int gxmin =     (int)((ax - maxd)/map.spacing.x);
	int gymin =     (int)((ay - maxd)/map.spacing.y);
	int gzmin =     (int)((az - maxd)/map.spacing.z);

	if(gxmin >= nx || gymin >= ny || gzmin >= nz) return;

	if(gxmin < 0) gxmin = 0;
	if(gymin < 0) gymin = 0;
	if(gzmin < 0) gzmin = 0;

	int gxmax = 1 + (int)((ax + maxd)/map.spacing.x);
	int gymax = 1 + (int)((ay + maxd)/map.spacing.y);
	int gzmax = 1 + (int)((az + maxd)/map.spacing.z);

	if(gxmax < 0 || gymax < 0 || gzmax < 0) return;

	if(gxmax >= nx) gxmax = nx - 1;
	if(gymax >= ny) gymax = ny - 1;
	if(gzmax >= nz) gzmax = nz - 1;

	Point3d gp = new Point3d();
	int pmfSize = pmf.size();
	double pmfData[] = pmf.toDoubleArray();

	for(int iz = gzmin; iz <= gzmax; iz++){
	    gp.z = map.origin.z + iz * map.spacing.z;
	    for(int iy = gymin; iy <= gymax; iy++){
		gp.y = map.origin.y + iy * map.spacing.y;
		for(int ix = gxmin; ix <= gxmax; ix++){
		    gp.x = map.origin.x + ix * map.spacing.x;

		    int gridPoint = ix + iy * nx + iz * nx * ny;

		    double d2 = gp.distanceSquared(atom);

		    if(d2 < maxd2){
			double d = Math.sqrt(d2);
			int bin = (int)(0.5 + (d / 0.1));

			if(bin < pmfSize){
			    map.data[gridPoint] += pmfData[bin];
			}
		    }
		}
	    }
	}
    }

    /** Try and load the pmf. */
    private static void loadPmf(HashMap<String, DoubleArrayList> pmfs, String location, String pmf){
	String filename = location + "/" + pmf + ".pmf";
	DoubleArrayList values = new DoubleArrayList();

	FILE f = FILE.open(filename);

	if(f == null){
	    Log.error("couldn't load " + filename);
	    return;
	}

	while(f.nextLine()){
	    double v = f.getDouble(1);
	    values.add(v);
	}

	f.close();

	pmfs.put(pmf, values);
    }

}