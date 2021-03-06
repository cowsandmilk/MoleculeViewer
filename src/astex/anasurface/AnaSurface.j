package astex.anasurface;

/*
 * Implementation of analytical molecular surface algorithm.
 *
 */

import astex.*;
import java.util.*;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class AnaSurface {
    /* Input coordinates and radii. */
    private double xyz[][] = null;
    private double radius[] = null;
    private double radius2[] = null;
    private double rsq[] = null;
    private int colors[] = null;
    private List<Edge> edgeList[];
    private List<Torus> torusList[];
    private int nxyz = 0;

    /** Is debugging on? */
    private static boolean debug = false;

    /** The default probe radius for the surface class. */
    public static double defaultProbeRadius = 1.5;

    /** The default quality setting for the surface. */
    public static int defaultQuality = 1;

    /** The probe radius for the surface. */
    private double probeRadius = 1.5;

    /** The tesselation depth for sphere template. */
    private int density = 1;

    /** Write the probes out to a file. */
    private String probesFilename = null;

    /** Probe placements. */
    private List<Probe> probes = new ArrayList<Probe>(1024);

    /** The faces of the molecular surface. */
    private List<Face> faces = new ArrayList<Face>(1024);

    /** The torii of the molecular surface. */
    private List<Torus> torii = new ArrayList<Torus>(1024);

    /** The number of torii with a single face. */
    private int singleFace = 0;

    /** The maximum number of edges on a single torus. */
    private int maximumTorusEdges = 0;

    /** The maximum number of edges on a single convex face. */
    private int maximumFaceEdges = 0;

    /** The number of self intersecting torii. */
    private int selfIntersectingTorii = 0;

    /** The number of self intersecting torii. */
    private int selfIntersectingProbes = 0;

    /** The number of distance comparisons made. */
    private int distanceComparisons = 0;

    /** The tmesh object that will hold the final surface. */
    private Tmesh tmesh = new Tmesh();

    /** Desired length of triangle edges. */
    // This is actually the probe separation along the
    // the torus center at the minute
    private double desiredTriangleLength = 1.5;

    /** The target length for toroidal triangles. */
    private double targetLen = 0.0;

    /** The current Color for the triangle. */
    public int backgroundColor = Color32.white;

    /** Edge lengths for different qualities. */
    private static final double qLength[] = {0.0, 1.5, 0.9, 0.5, 0.3};

    /** Default constructor. */
    public AnaSurface(double x[][], double r[], int colors[], int n){
	this.xyz = x;
	this.radius = r;
	this.colors = colors;
	this.nxyz = n;

	this.probeRadius = defaultProbeRadius;

	int quality = defaultQuality;

	if(quality > 0 && quality < qLength.length){
	    this.density = defaultQuality;
	    this.desiredTriangleLength = qLength[quality];
	}
    }

    /** Construct the surface. */
    public Tmesh construct(){

	long startTime = System.currentTimeMillis();

	initialise();

	long then = System.currentTimeMillis();

	buildNeighbourList();

	print("# neighbour list generation time (ms)",
	      (int)(System.currentTimeMillis() - then));

	then = System.currentTimeMillis();

	constructProbePlacements();

	print("# probe generation time (ms)",
	      (int)(System.currentTimeMillis() - then));

	processTorii();

	triangulate();

	triangulateAtoms();

	print("total probe placements", probes.size());
	print("total faces", faces.size());
	print("single face torii", singleFace);
	print("maximum torus edges", maximumTorusEdges);
	print("maximum face edges", maximumFaceEdges);
	print("self intersecting torii", selfIntersectingTorii);
	print("self intersecting probes", selfIntersectingProbes);
	print("distance comparisons", distanceComparisons);
	print("total memory (Mb)",
	      (int)(Runtime.getRuntime().totalMemory()/1000.));

	if(probesFilename != null){
	    outputProbes();
	}

	print("points in tmesh", tmesh.np);
	print("triangles in tmesh", tmesh.nt);
	print("(2n-4)", 2*tmesh.np-4);

	print("# total surface generation time (s)",
	      (float)(System.currentTimeMillis() - startTime)*0.001);


	System.out.println("starting decusp");
	deCuspSurface(tmesh);
	System.out.println("done");

	tmesh.setColorStyle(Tmesh.ColorStyle.VertexColor);

	return tmesh;
    }

    private void deCuspSurface(Tmesh tmesh){
	// build probe lattice
	Lattice l = new Lattice(probeRadius * 2.0);

	for(ListIterator<Probe> it = probes.listIterator(); it.hasNext(); ){
	    int p = it.nextIndex();
	    Probe probe = it.next();

	    l.add(p, probe.x[0], probe.x[1], probe.x[2]);
	}

	// measure distance to nearest probe center
	IntArrayList neighbours = new IntArrayList();

	for(int i = 0; i < tmesh.np; i++){
            tmesh.v[i] = 0.0001f;
	    neighbours.clear();
	    l.getPossibleNeighbours(-1,
				    tmesh.x[i], tmesh.y[i], tmesh.z[i],
				    neighbours, true);
	    int neighbourCount = neighbours.size();

	    double dmin = probeRadius;

	    for(int j = 0; j < neighbourCount; j++){
		int p = neighbours.getInt(j);
		Probe probe = probes.get(p);
		double d = distance(probe.x[0], probe.x[1], probe.x[2],
				    tmesh.x[i], tmesh.y[i], tmesh.z[i]);
		if(d < dmin){
		    dmin = d;
		}
	    }

	    tmesh.v[i] = (float)(probeRadius - dmin);
	}
    }
    
    /** Perform various setup tasks. */
    private void initialise(){
	// create the array for r + probeRadius
	radius2 = new double[nxyz];

	rsq = new double[nxyz];

	for(int i = 0; i < nxyz; i++){
	    radius2[i] = radius[i] + probeRadius;
	    rsq[i] = radius2[i] * radius2[i];
	}

	// space for keeping the edge lists
	edgeList = new List[nxyz];

	// space for keeping the torus lists
	torusList = new List[nxyz];

	// build target length approximation for
	// diagonal across toroidal triangles
	targetLen = desiredTriangleLength * desiredTriangleLength;
	targetLen = Math.sqrt(0.5 * targetLen);

	buildSphereTemplate(this.density);
    }

    private void triangulateAtoms(){
	for(int ia = 0; ia < nxyz; ia++){
	    transformSphere(xyz[ia], radius[ia]);

	    triangulateSphere(ia);
	}
    }

    /** Perform the various parts of the triangulation process. */
    private void triangulate(){
	// do the torus faces first, as they define edge
	// points for all other faces.
	for(Face f : faces){
	    int edgeCount = f.size();

	    if(edgeCount == 4 && f.type == Face.Type.Saddle){
		processToroidalFace(f);
	    }
	}

	for(Face f : faces){
	    int edgeCount = f.size();

	    if(!(edgeCount == 4 && f.type == Face.Type.Saddle) && !f.skip){
		processFace(f);
	    }
	}
    }

    /** Process a single face. */
    private void processFace(Face f){
	int edgeCount = f.size();

	if(f.type == Face.Type.Undefined){
	    processUndefinedFace(f);
	}else if(edgeCount == 4 && f.type == Face.Type.Saddle){
	    processToroidalFace(f);
	}else{
	    processIrregularFace(f, -1);
	}
    }

    /** Process simple face from cusp trimming. */
    private void processUndefinedFace(Face f){
	if(f.size() != 3){
	    System.out.println("undefined face has edges " + f.size());
	}

	Edge e0 = f.get(0);
	Edge e1 = f.get(1);
	Edge e2 = f.get(2);

	meshAddTriangle(e0.v0.vi, e1.v0.vi, e2.v0.vi);
    }

    /** Process irregular spherical face. */
    private void processIrregularFace(Face f, int ia){
	if(f.cen == null){
	    System.out.println("face has null center, skipping");
	    return;
	}

	if(f.type == Face.Type.Concave){
	    transformSphere(f.cen, f.r);
	}

	if(f.type != Face.Type.Undefined){
	    clipSphere(f, ia);
	    
	    addWholeTriangles();
	}

	processConvexFace(f);
    }

    /** Edges on the sphere. */
    private boolean used[] = new boolean[100];
    private Face convexFace = new Face(Face.Type.Convex);
    private Face sphereFace = new Face(Face.Type.Convex);

    /** The final triangulation stage for the remaining sphere surface. */
    private void triangulateSphere(int ia){
	if(edgeList[ia] == null){
	    return;
	}

	convexFace.type = Face.Type.Convex;
	copy(xyz[ia], convexFace.cen);
	convexFace.r = radius[ia];

	sphereFace.clear();

	for(Edge e : edgeList[ia]){
	    if(e.v0.i == ia && e.v1.i == ia){
		// edge is on atom
		sphereFace.add(e);
	    }
	}

	int unusedEdges = sphereFace.size();

	for(int i = 0; i < unusedEdges; i++){
	    used[i] = false;
	}

	while(unusedEdges != 0){
	    convexFace.clear();
	    Edge firstEdge = null;
	    Edge lastEdge = null;
	    Edge previousEdge = null;
	    boolean addedEdge = false;
	    
	    do {
		addedEdge = false;

		for(int i = 0; i < sphereFace.size(); i++){
		
		    if(!used[i]){
			Edge currentEdge = sphereFace.get(i);

			if(!convexFace.isEmpty()){
			    previousEdge = convexFace.peek();

			    if(previousEdge.v1.vi == currentEdge.v0.vi){
				convexFace.add(currentEdge);
				used[i] = true;
				unusedEdges--;
				addedEdge = true;
				break;
			    }
			}else{
			    convexFace.add(currentEdge);
			    used[i] = true;
			    unusedEdges--;
			    addedEdge = true;
			    break;
			}
		    }
		}
		
		firstEdge = convexFace.get(0);
		lastEdge = convexFace.peek();
	    } while(lastEdge.v1.vi != firstEdge.v0.vi && addedEdge);

	    if(!addedEdge){
		System.out.println("failed to extend contact face");
		sphereFace.print("faulty sphere face");
	    }else{
		if(!convexFace.isEmpty()){
		    processIrregularFace(convexFace, ia);
		}
	    }
	}
    }

    private void processConvexFace(Face f){
	vArray.clear();
	eArray.clear();

	if(f.type == Face.Type.Concave && f.size() > 3){
	    System.out.println("concave edge count "+ f.size());
	}

	for(int i = 0; i < 10000; i++){
	    euse[i] = 0;
	}

	int edgeCount = f.size();

	if(edgeCount > maximumFaceEdges){
	    maximumFaceEdges = edgeCount;
	}

	if(edgeCount > 31){
	    System.out.println("aaagh! more than 31 edges on a face");
	    System.exit(1);
	}

	for(int i = 0; i < edgeCount; i++){
	    Edge e = f.get(i);
	    int nv = e.size();

	    if(nv == 0){
		System.out.println("Edge has no vertices!!!!!\n!!!!\n!!!!");
	    }

	    // build bit mask of face edges that this
	    // vertex is on

	    for(int j = 0; j < nv-1; j++){

		if(debug){
		    FILE.out.print("adding to vlist %4d\n", e.getInt(j));
		}

		vArray.add(e.getInt(j));
		int edgeMask = (1 << i);

		if(j == 0){
		    int prevEdge = i - 1;
		    if(prevEdge == -1){
			prevEdge = edgeCount - 1;
		    }
		    edgeMask |= (1 << prevEdge);
		}

		eArray.add(edgeMask);
	    }
	}

	int nv = vArray.size();
	int vlist[] = vArray.toIntArray();
	int elist[] = eArray.toIntArray();

	if(debug){
	    for(int i = 0; i < nv; i++){
		FILE.out.print("v[%04d] = ", i);
		FILE.out.print("%04d mask ", vlist[i]);
		for(int ee = 10; ee >= 0; ee--){
		    if((elist[i] & (1 << ee)) != 0){
			System.out.print("1");
		    }else{
			System.out.print("0");
		    }
		}
		System.out.println("");
	    }
	}

	ecount = 0;

	// add boundary edges to edge list.
	for(int i = 0; i < nv; i++){
	    int i1 = i + 1;
	    if(i1 == nv){
		i1 = 0;
	    }

	    int v0 = vlist[i];
	    int v1 = vlist[i1];

	    if(debug){
		FILE.out.print("checking edge %4d", v0);
		FILE.out.print(" %4d\n", v1);
	    }

	    if(elist[i] != 0 && elist[i1] != 0){
		addEdgePair(v0, v1, false);
	    }
	}

	if(debug)System.out.println("after edges edge count " + ecount);

	if(f.type != Face.Type.Undefined){
	    for(int i = 0; i < nsp; i++){
		if(hull[i] == 1){
		    // always needs putting in list.
		    
		    vArray.add(clipped[i]);
		    eArray.add(0);
		}
	    }

	    // also add the boundary edges to the 
	    for(int i = 0; i < nst; i++){
		addBoundaryEdgeIfNeeded(si[i], sj[i]);
		addBoundaryEdgeIfNeeded(sj[i], sk[i]);
		addBoundaryEdgeIfNeeded(sk[i], si[i]);
	    }
	}

	if(debug) System.out.println("after boundary edge count " + ecount);

	if(debug) System.out.println("after clipping point count " +
				     vArray.size());

	addTriangles();
    }

    /** See if this edge is on the boundary. */
    private void addBoundaryEdgeIfNeeded(int svi, int svj){
	if(hull[svi] == 1 && hull[svj] == 1 &&
	   getHullCount(svi) < 3 && getHullCount(svj) < 3){
	    addEdgePair(svi, svj, true);
	}
    }

    /** The pairs of vertex indices. */
    int v0[] = new int[10000];
    int v1[] = new int[10000];
    int euse[] = new int[10000];
    int ecount = 0;

    /** Return how many times the edge v0-v1 is used. */
    private int edgePairCount(int vv0, int vv1){
	if(vv0 > vv1){
	    // swap vertex pair
	    int tmp = vv0;
	    vv0 = vv1;
	    vv1 = tmp;
	}

	for(int i = 0; i < ecount; i++){
	    if(v0[i] == vv0 && v1[i] == vv1){
		return euse[i];
	    }
	}

	return 0;
    }

    private int addEdgePair(int vv0, int vv1, boolean lookup){
	if(vv0 == vv1){
	    Log.error("vv0 == vv1");
	}

	if(debug){
	    System.out.println("addEdgePair " + vv0 + " " + vv1);
	}

	if(vv0 > vv1){
	    // swap vertex pair
	    int tmp = vv0;
	    vv0 = vv1;
	    vv1 = tmp;
	}

	if(lookup){
	    for(int i = 0; i < ecount; i++){
		if(v0[i] == vv0 && v1[i] == vv1){
		    if(debug){
			FILE.out.print("addEdgePair %4d", vv0);
			FILE.out.print(" %4d", vv1);
			FILE.out.print(" euse %d\n", euse[i]);
		    }

		    if(euse[i] >= 2){
			FILE.out.print("### edge already used twice %4d", vv0);
			FILE.out.print(" %4d\n", vv1);
		    }
		    euse[i]++;
		    return i;
		}
	    }
	}

	v0[ecount] = vv0;
	v1[ecount] = vv1;
	euse[ecount] = 1;

	return ecount++;
    }

    /** The list of edge vertices we have to triangulate. */
    private IntArrayList vArray = new IntArrayList(64);

    /** The list of edge ids associated with the vertices. */
    private IntArrayList eArray = new IntArrayList(64);

    private double pp0[] = new double[3];
    private double pp1[] = new double[3];
    private double pp2[] = new double[3];
    private double pp3[] = new double[3];

    private double npp0[] = new double[3];
    private double npp1[] = new double[3];
    private double npp2[] = new double[3];

    private double circum[] = new double[3];

    /** Triangulate an arbitrary collection of edges and interior points. */
    private void addTriangles(){
	int nv = vArray.size();
	int vlist[] = vArray.toIntArray();
	int elist[] = eArray.toIntArray();

	double rlim = currentLongestEdge * 1.5;
	rlim *= rlim;

	if(debug){
	    System.out.println("vertex list");
	    for(int i = 0; i < nv; i++){
		FILE.out.print("v[%03d] = ", i);
		FILE.out.print("%03d\n", vlist[i]);
	    }
	    System.out.println("vertex list end");
	}

	// find maximum edge vertex (i.e. not from sphere template)

	int nev = 0;

	for(int i = 0; i < nv; i++){
	    if(elist[i] == 0){
		nev = i;
		break;
	    }
	}
    
	if(debug) System.out.println("nev " + nev);

	for(int iteration = 0; iteration < 3; iteration++){
	    
	    for(int i = 0; i < nv - 2; i++){
		int vi = vlist[i];
		tmesh.getVertex(vi, pp0, npp0);
		
		for(int j = i+1; j < nv - 1; j++){
		    int vj = vlist[j];
		    
		    // if on same edge, only allow if
		    // one vertex apart
		    if(debug) FILE.out.print("checking edge %3d ", vi);
		    if(debug) FILE.out.print("%3d\n", vj);
		    
		    if((elist[i] & elist[j]) != 0){
			int ji = j - i;
			// check against maximum exterior edge vertex (nev)
			if(ji > 1 && ji < nev - 1){
			    if(debug){
				System.out.println("skipping points not edge neighbours");
				System.out.println("and " + (elist[i] & elist[j]));
				System.out.println("j - i " + (j - i));
			    }
			    continue;
			}
		    }
		    
		    tmesh.getVertex(vj, pp1, npp1);

		    if(distance2(pp0, pp1) > rlim){
			if(debug) System.out.println("skipping edge points not close enough");
			continue;
		    }
		    
		    for(int k = j+1; k < nv; k++){
			int vk = vlist[k];

			if(debug){
			    FILE.out.print("checking %3d ", vi);
			    FILE.out.print("%3d ", vj);
			    FILE.out.print("%3d\n", vk);
			}

			int perimeterCount = 0;
			if(elist[i] != 0) perimeterCount++;
			if(elist[j] != 0) perimeterCount++;
			if(elist[k] != 0) perimeterCount++;

			// conditions for accepting triangles
			// can't have 3 points from interior
			if(perimeterCount == 0){
			    if(debug) System.out.println("skipping all interior");
			    continue;
			}
			
			// can't have 3 points on same edge
			if(((elist[i] & elist[j]) & elist[k]) != 0){
			    if(debug) System.out.println("skipping all same edge");
			    continue;
			}
			
			// conditions for accepting triangles
			// can't have 3 edge points (maybe only first pass)
			if(iteration < 2 && perimeterCount == 3){
			    if(debug) System.out.println("skipping all perimeter");
			    continue;
			}

#if 0
			if(iteration == 2 && perimeterCount == 3){
			    // last iteration...
			    // only allow 3 perimeter points if they
			    // are one after the other.
			    int vmin = i;
			    int vmax = i;
			    if(j < vmin) vmin = j;
			    if(j > vmax) vmax = j;
			    if(k < vmin) vmin = k;
			    if(k > vmax) vmax = k;

			    if(vmax - vmin == 2){
				// ok
			    }else{
				vmax -= nev;
				if(vmax - vmin == 2){
				}else{
				    if(debug) System.out.println("skipping vertices are not edge neighbours");
				    continue;
				}
			    }
			}
#endif

			tmesh.getVertex(vk, pp2, npp2);
			
			if(distance2(pp1, pp2) > rlim ||
			   distance2(pp2, pp0) > rlim){
			    if(debug) System.out.println("skipping edges too long");
			    continue;
			}
			
			boolean tok = true;
			
			double rc = circumCircle(circum, pp0, pp1, pp2);
			
			if(rc == Double.POSITIVE_INFINITY){
			    if(debug) System.out.println("## no solution for circumCircle");
			    continue;
			}
			
			rc *= rc;
			
			for(int l = 0; l < nv; l++){
			    if(l != i && l != j && l != k){
				int vl = vlist[l];
				tmesh.getVertex(vl, pp3, null);
				
				if(distance2(circum, pp3) < rc){
				    if(debug) System.out.println("skipping delaunay violation");
				    tok = false;
				    break;
				}
			    }
			}
			
			if(tok){
			    if(edgePairCount(vj, vk) == 2){
				if(debug) System.out.println("skipping edges used");
				continue;
			    }
			    if(edgePairCount(vk, vi) == 2){
				if(debug) System.out.println("skipping edges used");
				continue;
			    }
			    if(edgePairCount(vi, vj) == 2){
				if(debug) System.out.println("skipping edges used");
				continue;
			    }
			    
			    addIfAcceptable(vi, vj, vk);
			}
		    }
		}
	    }
	}
	
#if 0
    boolean added = false;

	do {
	    added = false;

	    for(int i = 0; i < nv - 2; i++){
		int vi = vlist[i];
		tmesh.getVertex(vi, pp0, npp0);

		for(int j = i+1; j < nv - 1; j++){
		    int vj = vlist[j];

		    if(edgePairCount(vi, vj) == 1){

			tmesh.getVertex(vj, pp1, npp1);

			for(int k = j+1; k < nv; k++){
			    int vk = vlist[k];
			
			    if(elist[i] == 0 && elist[j] == 0 && elist[k] == 0){
				continue;
			    }

			    // can't have 3 points on same edge
			    if(((elist[i] & elist[j]) & elist[k]) != 0){
				continue;
			    }

			    if(edgePairCount(vi, vj) == 1 &&
			       edgePairCount(vj, vk) == 1 &&
			       edgePairCount(vk, vi) < 2){
			
				tmesh.getVertex(vj, pp1, npp1);
			
				addIfAcceptable(vi, vj, vk);
				added = true;
				break;
			    }
			}
		    }
		}
	    }
	} while(added);
#endif
        if(debug) System.out.println("end");
        if(debug) printIfNotComplete();
    }

    private void printIfNotComplete(){
	boolean printit = false;
	for(int i = 0; i < ecount; i++){
	    if(v0[i] > v1[i]){
		System.out.println("error disordered edge");
	    }
	    if(euse[i] != 2){
		printit = true;
		break;
	    }
	}

	if(printit){
	    System.out.println("vertex count " + vArray.size());
	    for(int i = 0; i < ecount; i++){
		if(euse[i] != 2){
		    FILE.out.print("edge[%03d] =", i);
		    FILE.out.print(" %4d", v0[i]);
		    FILE.out.print(",%4d", v1[i]);
		    FILE.out.print(" count = %d\n", euse[i]);
		}
	    }
	}
    }

    /** Perform some sanity checking of the new triangle. */
    private void addIfAcceptable(int vi, int vj, int vk){
	if(vi == 169 && vj == 654 && vk == 655){
	    Exception e = new Exception();
	    e.printStackTrace();
	}

	int ec0 = edgePairCount(vi, vj);
	int ec1 = edgePairCount(vj, vk);
	int ec2 = edgePairCount(vk, vi);

	if(ec0 < 2 && ec1 < 2 && ec2 < 2){

	    addEdgePair(vi, vj, true);
	    addEdgePair(vj, vk, true);
	    addEdgePair(vk, vi, true);

	    meshAddTriangle(vi, vj, vk);
	}else{
	    FILE.out.print("#### try to add triangle where edges are in use\n");
	    FILE.out.print("%4d ", vi);
	    FILE.out.print("%4d ", vj);
	    FILE.out.print("%4d\n", vk);
	    if(ec0 < 2){
		FILE.out.print("edge %4d-", vi);
		FILE.out.print("%4d\n", vj);
	    }
	    if(ec1 < 2){
		FILE.out.print("edge %4d-", vj);
		FILE.out.print("%4d\n", vk);
	    }
	    if(ec2 < 2){
		FILE.out.print("edge %4d-", vk);
		FILE.out.print("%4d\n", vi);
	    }
	}
    }

    /**
     * Yes this really is how to calculate the circum center
     * and radius of three points in 3d.
     *
     * Adapted from Graphics Gems.
     */
    private double circumCircle(double cc[],
			       double p1[], double p2[], double p3[]) {
	double d1 = 0.0;
	double d2 = 0.0;
	double d3 = 0.0;

	for(int i = 0; i < 3; i++){
	    d1 += (p3[i]-p1[i])*(p2[i]-p1[i]);
	    d2 += (p3[i]-p2[i])*(p1[i]-p2[i]);
	    d3 += (p1[i]-p3[i])*(p2[i]-p3[i]);
	}

	double c1 = d2*d3;
	double c2 = d1*d3;
	double c3 = d1*d2;
	double c = c1 + c2 + c3;
	double ccc = 2. * c;
	double c2c3 = (c2+c3)/ccc;
	double c3c1 = (c3+c1)/ccc;
	double c1c2 = (c1+c2)/ccc;

	for(int i = 0; i < 3; i++){
	    cc[i] = (c2c3*p1[i] + c3c1*p2[i] + c1c2*p3[i]);
	}

	return distance(cc, p1);
    }

    private int debugColor[] = {
	0xff781e,
	0x4bc3ff,
	0x37ffc3,
	0xaaff00,
    };	

    private void meshAddTriangle(int v0, int v1, int v2){
	if(v0 == 0 && v1 == 0 && v2 == 0){
	    try {
		Exception e = new RuntimeException("");
		e.printStackTrace();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	}

	tmesh.addTriangle(v0, v1, v2,
			  debugColor[tmesh.nt % debugColor.length]);
    }

    /* Working space for doubles. */
    private static double EPS = 1.e-8;

    private static double u[] = new double[3];
    private static double v[] = new double[3];
    private static double w[] = new double[3];

    public static double intersect(double p1[], double p2[],
				   double p3[], double p4[],
				   double pa[], double pb[]){
	vector(u, p1, p2);
	vector(v, p3, p4);
	vector(w, p3, p1);
	double    a = dot(u,u);        // always >= 0
	double    b = dot(u,v);
	double    c = dot(v,v);        // always >= 0
	double    d = dot(u,w);
	double    e = dot(v,w);
	double    D = a*c - b*b;       // always >= 0
	double    sc, sN, sD = D;      // sc = sN / sD, default sD = D >= 0
	double    tc, tN, tD = D;      // tc = tN / tD, default tD = D >= 0
	
	// compute the line parameters of the two closest points
	if (D < EPS) { // the lines are almost parallel
	    sN = 0.0;
	    tN = e;
	    tD = c;
	} else {                // get the closest points on the infinite lines
	    sN = (b*e - c*d);
	    tN = (a*e - b*d);
	    if (sN < 0) {       // sc < 0 => the s=0 edge is visible
		sN = 0.0;
		tN = e;
		tD = c;
	    } else if (sN > sD) {  // sc > 1 => the s=1 edge is visible
		sN = sD;
		tN = e + b;
		tD = c;
	    }
	}

	if (tN < 0) {           // tc < 0 => the t=0 edge is visible
	    tN = 0.0;
	    // recompute sc for this edge
	    if (-d < 0)
		sN = 0.0;
	    else if (-d > a)
		sN = sD;
	    else {
		sN = -d;
		sD = a;
	    }
	} else if (tN > tD) {      // tc > 1 => the t=1 edge is visible
	    tN = tD;
	    // recompute sc for this edge
	    if ((-d + b) < 0)
		sN = 0;
	    else if ((-d + b) > a)
		sN = sD;
	    else {
		sN = (-d + b);
		sD = a;
	    }
	}

	// finally do the division to get sc and tc
	sc = sN / sD;
	tc = tN / tD;
	
	// get the difference of the two closest points
	double dist = 0.0;
	double di = 0.0;
	for(int i = 0; i < 3; i++){
	    di = w[i] + (sc * u[i]) - (tc * v[i]);  // = S1(sc) - S2(tc)
	    dist += di * di;
	}
	
	return Math.sqrt(dist);
    }

    /** Temporary space for inverting surface normal. */
    private double invertn[] = new double[3];

    /** Distance from clip plane that is allowed. */
    private double clipTolerance = 0.0;

    /** Clip sphere according to plane list and add to tmesh. */
    private void clipSphere(Face f, int ia){
	// reset the clip and hull markers
	for(int isp = 0; isp < nsp; isp++){
	    clipped[isp] = -1;
	}

	clipTolerance = -0.15 * currentLongestEdge;

	if(ia == -1){
	    for(Edge e : f){
		for(int isp = 0; isp < nsp; isp++){
		    if(clipped[isp] == -1){
			// plane equation
			if(plane_eqn(tsx[isp], e.cen, e.n) > clipTolerance){
			    clipped[isp] = -2;
			}
		    }
		}
	    }
	}else{
	    for(int a = 0; a < count[ia]; a++){
		int j = nn[first[ia] + a];

		// generate torus radius and direction
		torusAxisUnitVector(uij, xyz[ia], xyz[j]);
            
		// generate contact circles on each end of torus
		contactCircle(cij, xyz[ia], radius[ia], xyz[j], radius[j]);
		
		for(int isp = 0; isp < nsp; isp++){
		    if(clipped[isp] == -1){
			// plane equation
			if(plane_eqn(tsx[isp], cij, uij) > clipTolerance){
			    clipped[isp] = -2;
			}
		    }
		}
	    }
	}

	// clear the hull markers
	for(int ii = 0; ii < nsp; ii++){
	    hull[ii] = 0;

	    if(clipped[ii] == -1){
		int nc = vncount[ii];
		for(int j = 0; j < nc; j++){
		    if(clipped[vn[ii][j]] == -2){
			hull[ii] = 1;
			break;
		    }
		}
	    }
	}

	for(int isp = 0; isp < nsp; isp++){
	    // add all the points
	    if(clipped[isp] == -1){
		if(f.type == Face.Type.Concave){
		    invertn[0] = -snx[isp][0];
		    invertn[1] = -snx[isp][1];
		    invertn[2] = -snx[isp][2];
		    clipped[isp] =
			tmesh.addPoint(tsx[isp], invertn, 0.0, 0.0);
		    if(debug){
			tmesh.addSphere(tsx[isp][0], tsx[isp][1], tsx[isp][2],
					0.05, Color32.red);
		    }

		    int color = colorPoint(f, tsx[isp]);
		    
		    tmesh.vcolor[clipped[isp]] = color;
		}else{
		    clipped[isp] =
			tmesh.addPoint(tsx[isp], snx[isp], 0.0, 0.0);
		    tmesh.vcolor[clipped[isp]] = colors[ia];
		}
	    }
	}
    }

    private int colorPoint(Face f, double p[]){
	Vertex v0 = (f.get(0)).v0;
	Vertex v1 = (f.get(1)).v0;
	Vertex v2 = (f.get(2)).v0;

	double d0 = distance(p[0], p[1], p[2], v0.x[0], v0.x[1], v0.x[2]);
	double d1 = distance(p[0], p[1], p[2], v1.x[0], v1.x[1], v1.x[2]);
	double d2 = distance(p[0], p[1], p[2], v2.x[0], v2.x[1], v2.x[2]);
	double sum = 2.0 * (d0 + d1 + d2);

	double comp0 = (d1 + d2)/sum;
	double comp1 = (d0 + d2)/sum;
	double comp2 = (d0 + d1)/sum;

	int r = 0, g = 0, b = 0;

	r += comp0 * Color32.getRed(colors[v0.i]);
	r += comp1 * Color32.getRed(colors[v1.i]);
	r += comp2 * Color32.getRed(colors[v2.i]);
	g += comp0 * Color32.getGreen(colors[v0.i]);
	g += comp1 * Color32.getGreen(colors[v1.i]);
	g += comp2 * Color32.getGreen(colors[v2.i]);
	b += comp0 * Color32.getBlue(colors[v0.i]);
	b += comp1 * Color32.getBlue(colors[v1.i]);
	b += comp2 * Color32.getBlue(colors[v2.i]);

	return Color32.pack(r, g, b);
    }

    /** Add the remaing triangles. */
    private void addWholeTriangles(){
	// now we clipped all the points add the
	// remaining triangles to the tmesh
	for(int t = 0; t < nst; t++){
	    if(clipped[si[t]] >= 0 &&
	       clipped[sj[t]] >= 0 &&
	       clipped[sk[t]] >= 0){
		// all the points were unclipped
		meshAddTriangle(clipped[si[t]],
				clipped[sj[t]],
				clipped[sk[t]]);
	    }
	}
    }

    /** Transform the sphere to this atom position/size. */
    private void transformSphere(double xs[], double rs){

	for(int i = 0; i < nsp; i++){
	    clipped[i] = -1;
	    for(int j = 0; j < 3; j++){
		tsx[i][j] = xs[j] + sx[i][j] * rs;
	    
	    }
	}

	currentLongestEdge = rs * longestEdge;
    }

    /** vector for coordinate frame. */
    private double pp[] = new double[3];
    private double ccij[] = new double[3];
    private double ccji[] = new double[3];

    /** The point on the torus. */
    private double tp[] = new double[3];

    /** The normal at the point on the torus. */
    private double ntp[] = new double[3];

    /** Where the saddle face points end up in the tmesh. */
    private int tmeshv[][] = new int[100][100];

    /** Deltas for the wrap angle calculation. */
    private double pp2cij[] = new double[3];
    private double pp2cji[] = new double[3];
    private double n1[] = new double[3];
    private double n2[] = new double[3];

    private void processToroidalFace(Face f){
	triangulateToroidalFace(f);
    }

    /**
     * Triangulate a toroidal face.
     *
     * Vital face ordering diagram for face triangulation.
     *            
     *                         <- aa                            
     *                i
     *               e3
     *     ----------->-----------
     *     |                     |
     *     |                     |          ii
     *     |                     |           |
     *     |                     |           v
     *  e1 ^                     v e0
     *     |                     |
     *     |                     |
     *     |                     |
     *     |                     |
     *     |                     |
     *     -----------<-----------
     *               e2
     *                j
     */
    private void triangulateToroidalFace(Face f){
	Torus t = f.torus;
	Edge e0 = f.get(0);
	Edge e1 = f.get(1);
	Edge e2 = f.get(2);
	Edge e3 = f.get(3);
	double a0 = f.startAngle;
	double a1 = f.stopAngle;

	if(e2.v0.i != t.j){
	    System.out.println("t.i " + t.i + " t.j " + t.j);
	    System.out.println("e0.v0.i " + e0.v0.i + " e0.v1.i " + e0.v1.i);
	    System.out.println("e1.v0.i " + e1.v0.i + " e1.v1.i " + e1.v1.i);
	    System.out.println("e2.v0.i " + e2.v0.i + " e2.v1.i " + e2.v1.i);
	    System.out.println("e3.v0.i " + e3.v0.i + " e3.v1.i " + e3.v1.i);
	}

	// form coordinate set.

	// correct for wrapped angles
	if(a1 < a0){
	    a1 += 2.0 * Math.PI;
	}

	if(a0 > a1){
	    System.out.println("angle error ");
	}

	// calculate angular step for probe center
	// a1 must be greater than a0
	// use largest raduis contact circle to
	// control triangulation parameters
	double effectiveArc = t.rcij;

	if(t.rcji > effectiveArc){
	    effectiveArc = t.rcji;
	}

	double arcLength = (a1 - a0) * (effectiveArc);
	// always at least two points
	int tpcount = 2 + (int)(arcLength/targetLen);
	int tpcount1 = tpcount - 1;
	double angle = a0;
	double wrapAngle = 0.0;
	double wrapAngleStep = 0.0;
	int nwap = 0;

	double step = (a1 - a0)/(tpcount1);

	e2.ensureCapacity(tpcount);
	e3.ensureCapacity(tpcount);

	for(int a = 0; a < tpcount; a++){
	    // tidy up any slight rounding error
	    if(angle > a1){
		angle = a1;
	    }

	    double sina = Math.sin(angle);
	    double cosa = Math.cos(angle);

	    // interpolate the vectors
	    for(int i = 0; i < 3; i++){
		double component = t.uijnorm2[i] * sina + t.uijnorm[i] * cosa;

		pp[i]   = t.tij[i] + t.rij * component;
		ccij[i] = f.iij[i] + t.rcij * component;
		ccji[i] = f.iji[i] + t.rcji * component;
	    }

	    // calculate the wrap angle
	    // and the offsets for this torus segment.
	    vector(pp2cij, pp, ccij);
	    vector(pp2cji, pp, ccji);
	    wrapAngle = angle(pp2cij, pp2cji);

	    cross(n1, pp2cij, pp2cji);
	    cross(n2, n1, pp2cij);
	    normalise(n2);
	    normalise(pp2cij);
	    double wrapArcLength = wrapAngle * probeRadius;

	    // always at least two points
	    nwap = 2 + (int)(wrapArcLength/targetLen);
	    int nwap1 = nwap - 1;

	    wrapAngleStep = wrapAngle/(nwap1);

	    if(a == 0){
		e0.ensureCapacity(nwap);
		e1.ensureCapacity(nwap);
	    }

	    double wa = 0.0;

	    // now interpolate from one end
	    // of the arc to the other.
	    for(int ii = 0; ii < nwap; ii++){
		double sinwa = Math.sin(wa);
		double coswa = Math.cos(wa);

		// tidy up any slight rounding error
		if(wa > wrapAngle){
		    wa = wrapAngle;
		}
		
		// relative vector
		for(int i = 0; i < 3; i++){
		    ntp[i] = coswa * pp2cij[i] + sinwa * n2[i];
		    tp[i] = probeRadius * ntp[i] + pp[i];
		    ntp[i] = -ntp[i];
		}

		// record the vertex index
		int vid = 0;

		// need to get the tmesh vertices for the
		// corner points from the vertex objects
		// to ensure mesh sharing
		if(a == 0 && ii == 0){
		    vid = e0.v0.vi;
		}else if(a == 0 && ii == nwap1){
		    vid = e0.v1.vi;
		}else if(a == tpcount1 && ii == nwap1){
		    vid = e1.v0.vi;
		}else if(a == tpcount1 && ii == 0){
		    vid = e1.v1.vi;
		}else{
		    // not corner
		    // need a new point
		    vid = tmesh.addPoint(tp, ntp, 0.0, 0.0);
		}

		tmeshv[a][ii] = vid;

		// interpolate color along arc
		double colorFrac = (double)ii/(double)(nwap-1);
		tmesh.vcolor[vid] = Color32.blend(colors[t.i],
						colors[t.j],
						1. - colorFrac);

		// assign the vertices to edge structures
		//
		// XXX need to retrieve the vertex indexes
		// for the corner vertices of the toriodal patch
		//!!
		// the ordering of these edges is crucial
		// do not change unless you know better than me
		if(a == 0)        e0.set(ii, vid);
		if(a == tpcount1) e1.set(nwap1-ii, vid);
		if(ii == 0)       e3.set(tpcount1-a, vid);
		if(ii == nwap1)   e2.set(a, vid);

		wa += wrapAngleStep;

		
	    }

	    if(!debug &&
		a > 0){
		for(int ii = 0; ii < nwap1; ii++){
		    meshAddTriangle(tmeshv[a-1][ii],
				    tmeshv[a][ii],
				    tmeshv[a-1][ii+1]);
		    meshAddTriangle(tmeshv[a][ii],
				    tmeshv[a-1][ii+1],
				    tmeshv[a][ii+1]);
		}
	    }
	    angle += step;
	}
    }

    /** Working space for torus processing. */
    private Edge torusEdges[] = new Edge[1000];

    /** Angles relative to torus normal. */
    private double angles[] = new double[1000];

    /** Vector form contact circle to torus vertex. */
    private double cij2v[] = new double[3];

    /** Working space for intersecting torii. */
    private double qij[] = new double[3];
    private double qji[] = new double[3];

    private double cusp[] = new double[3];
    private double nleft[] = new double[3];
    private double nright[] = new double[3];

    /** Process a single torus. */
    private void processTorus(Torus t){
	int edgeCount = t.edges.size();

	// try to prevent these getting into the
	// data structure at some point.
	if(edgeCount == 0){
	    // do nothing
	    System.out.println("!!! torus with no edges!!!!");

	    return;
	}

	// can't be an odd number of edge.
	if(edgeCount % 2 != 0){
	    System.out.println("atom i " + t.i + " j " + t.j);
	    System.out.println("odd number of edges " + edgeCount);

	    return;
	}

	for(int i = 0; i < edgeCount; i++){
	    torusEdges[i] = t.edges.get(i);
	}

	for(int ee = 0; ee < edgeCount; ee++){
	    Edge e = torusEdges[ee];

	    if(e.v0.i == t.i){
		vector(cij2v, t.cij, e.v0.x);
	    }else if(e.v1.i == t.i){
		vector(cij2v, t.cij, e.v1.x);
	    }else{
		System.out.println("edge doesn't involve i " + t.i);
		System.out.println("edge has " + e.v0.i + "," + e.v1.i);
	    }

	    angles[ee] = angle(cij2v, t.uijnorm, t.uijnorm2);
	}

	// sort them using bubble sort...
	// will do for now (not usually more than 4)
	for (int ia = 0; ia < edgeCount - 1; ia++) {
	    for (int ja = 0; ja < edgeCount - 1 - ia; ja++){
		if (angles[ja+1] > angles[ja]) {
		    double tmp = angles[ja];
		    angles[ja] = angles[ja+1];
		    angles[ja+1] = tmp;
			
		    Edge etmp = torusEdges[ja];
		    torusEdges[ja] = torusEdges[ja+1];
		    torusEdges[ja+1] = etmp;
		}
	    }
	}

	// check that we got the correct ordering
	for(int ee = 0; ee < edgeCount - 1; ee++){
	    if(angles[ee] < angles[ee + 1]){
		System.out.println("!!!! error sorting vertex angles " +
				   t.i + "," + t.j);
	    }
	}

	double tcen[] = new double[3];

	for(int ee = 0; ee < edgeCount; ee += 2){
	    Edge e0 = torusEdges[ee];
	    int ee1 = 0;
	    Edge e1 = null;

	    // depending on whether this edge start on i
	    // or runs to i, depends on whether the paired
	    // Edge is before or after us in the list.
	    if(e0.v1.i == t.i){
		// can never overflow
		ee1 = ee + 1;
	    }else{
		ee1 = ee - 1;
		// make sure we get the last edge
		// if we ask for -1.
		if(ee1 == -1){
		    ee1 = edgeCount - 1;
		}
	    }

	    e1 = torusEdges[ee1];

	    if(e0.v0.i != e1.v1.i || e0.v1.i != e1.v0.i){
		System.out.println("!! unpaired edges");
	    }

	    Edge e2 = null, e3 = null;

	    e2 = addEdge(e0.v1, e1.v0, t);
	    e3 = addEdge(e1.v1, e0.v0, t);
	    
	    if(false && t.selfIntersects){
		// need to generate 2 torus faces and
		// rejig the probe face on each side
		double dtq = Math.sqrt(probeRadius*probeRadius - t.rij*t.rij);
		mid(cusp, e1.probeFace.cen, e0.probeFace.cen);
		double dist =
		    0.5 * distance(e0.probeFace.cen, e1.probeFace.cen);
		double rint = Math.sqrt(probeRadius*probeRadius - dist*dist);

		// generate normals.
		vector(nleft, e1.probeFace.cen, e0.probeFace.cen);
		normalise(nleft);
		copy(nleft, nright);
		negate(nright);
		
		for(int i = 0; i < 3; i++){
		    qij[i] = t.tij[i] - dtq * t.uij[i];
		    qji[i] = t.tij[i] + dtq * t.uij[i];
		}

		if(e0.probeFace == null || e1.probeFace == null){
		    System.out.println("e0 or e1 has null probe face");
		}

		Vertex vi0 = null;
		Vertex vj0 = null;

		if(e0.v1.i == t.i){
		    vi0 = addVertex(qji, t.j, cusp);
		    vj0 = addVertex(qij, t.i, cusp);
		}else{
		    vi0 = addVertex(qij, t.i, cusp);
		    vj0 = addVertex(qji, t.j, cusp);
		}

		e2.add(e2.v0.vi);
		e2.add(e2.v1.vi);

		e3.add(e3.v0.vi);
		e3.add(e3.v1.vi);

		Edge topRight = addSimpleEdge(e0.v0, vi0, e0, null, null, -1.);
		Edge topLeft = addSimpleEdge(vi0, e1.v1, e1, null, null, -1.);

		Face topf = new Face(Face.Type.Undefined);
		topf.add(e3); topf.add(topRight); topf.add(topLeft);
		faces.add(topf);

		Edge bottomLeft =
		    addSimpleEdge(e1.v0, vj0, e1, null, null, -1.);
		Edge bottomRight =
		    addSimpleEdge(vj0, e0.v1, e0, null, null, -1.);

		Face bottomf = new Face(Face.Type.Undefined);
		bottomf.add(e2);
		bottomf.add(bottomLeft);
		bottomf.add(bottomRight);
		faces.add(bottomf);

		Edge connectLeft =
		    addSimpleEdge(vj0, vi0, null, cusp, nleft, rint);
		Edge connectRight =
		    addSimpleEdge(vi0, vj0, null, cusp, nright, rint);

		connectRight.clear();
		
		int vcount = connectLeft.size();

		for(int iv = 0; iv < vcount; iv++){
		    connectRight.add(connectLeft.getInt(vcount-iv-1));
		}

		// tidy up the probe boundary edges.
		replaceProbeEdges(e0, topRight, connectRight, bottomRight);

		replaceProbeEdges(e1, bottomLeft, connectLeft, topLeft);

	    }else{

		// and add the new face

		Face f = new Face(Face.Type.Saddle);
		
		f.torus = t;

		copy(t.cij, f.iij);
		copy(t.cji, f.iji);

		faces.add(f);

		if(e0.v1.i == t.i){
		    f.startAngle = angles[ee1];
		    f.stopAngle = angles[ee];
		    f.add(e1); f.add(e0); f.add(e3); f.add(e2);
		}else{
		    f.startAngle = angles[ee];
		    f.stopAngle = angles[ee1];
		    f.add(e0); f.add(e1); f.add(e2); f.add(e3);
		}
	    }
	}

	// record maximum number of torus edges we saw
	if(edgeCount > maximumTorusEdges){
	    maximumTorusEdges = edgeCount;
	}
    }

    private double vx0[] = new double[3];
    private double vx1[] = new double[3];
    private double pint[] = new double[3];

    private Edge addSimpleEdge(Vertex v0, Vertex v1, Edge refEdge,
			       double x[], double n[], double rr){
	Edge e = new Edge();
	e.v0 = v0;
	e.v1 = v1;

	e.add(v0.vi);

	if(refEdge != null){
	    copy(refEdge.n, e.n);
	    copy(refEdge.cen, e.cen);
	    e.r = refEdge.r;
	}else if(x != null){
	    copy(x, e.cen);
	    copy(n, e.n);
	    e.r = rr;

	    tmesh.getVertex(v0.vi, vx0, null);
	    tmesh.getVertex(v1.vi, vx1, null);

	    double dist = distance(vx0, vx1);
	    int points = 2 + (int)(4.*dist/desiredTriangleLength);

	    for(int i = 1; i < points - 1; i++){
		double frac = (double)i/(double)points;

		for(int j = 0; j < 3; j++){
		    pint[j] = vx0[j] + frac*(vx1[j] - vx0[j]);
		    pint[j] -= x[j];
		}

		normalise(pint);
		scale(pint, rr);

		for(int j = 0; j < 3; j++){
		    pint[j] += x[j];
		}

		Vertex vint = addVertex(pint, -1, x);
		e.add(vint.vi);
	    }
	}else{
	    System.out.println("addSimpleEdge: no edge/point for reference");
	    System.exit(1);
	}

	e.add(v1.vi);

	return e;
    }

    private Edge oldEdges[] = new Edge[100];

    private void replaceProbeEdges(Edge olde, Edge e0, Edge e1, Edge e2){
	Face probeFace = olde.probeFace;
	
	if(olde.v0 != e0.v0 || olde.v1 != e2.v1 ||
	   e0.v1 != e1.v0 || e1.v1 != e2.v0){
	    System.out.println("replacement edges don't span same vertices");
	    System.out.println("olde.v0 " + olde.v0.vi + " olde.v1 " + olde.v1.vi);
	    System.out.println("e0.v0 " + e0.v0.vi + " e0.v1 " + e0.v1.vi);
	    System.out.println("e1.v0 " + e1.v0.vi + " e1.v1 " + e1.v1.vi);
	    System.out.println("e2.v0 " + e2.v0.vi + " e2.v1 " + e2.v1.vi);

	    return;
	}

	if(probeFace == null){
	    System.out.println("edge had no probe in replaceProbeEdges");
	    return;
	}

	if(!probeFace.contains(olde)){
	    System.out.println("face didn't contain old edge...");
	    return;
	}
	
	int edgeCount = probeFace.size();

	for(int i = 0; i < edgeCount; i++){
	    oldEdges[i] = probeFace.get(i);
	}
	
	probeFace.clear();

	for(int i = 0; i < edgeCount; i++){
	    if(oldEdges[i] == olde){
		probeFace.add(e0);
		probeFace.add(e1);
		probeFace.add(e2);
	    }else{
		probeFace.add(oldEdges[i]);
	    }
	}

	if(!probeFace.isValid()){
	    System.out.println("new probeFace is not valid");
	}
    }
    
    /** Calculate contact circle center and radius. */
    private double contactCircle(double cij[],
				 double ai[], double ri,
				 double aj[], double rj){
	double rip = ri + probeRadius;

	double rij = torusCenter(tij, ai, ri, aj, rj, probeRadius);

	for(int ii = 0; ii < 3; ii++){
	    cij[ii] = (ri * tij[ii] + probeRadius * ai[ii])/rip;
	}

	return rij*ri/(rip);
    }

    /** Process all of the torii. */
    private void processTorii(){
	for(Torus t : torii){
	    processTorus(t);
	}
    }

    /** Working space for probe placements. */
    private double probe0[] = new double[3];
    private double probe1[] = new double[3];

    /** Construct probe placements from triplets of atoms. */
    private void constructProbePlacements(){

	int tripletCount = 0;

	for(int i = 0; i < nxyz; i++){

	    for(int a = 0; a < count[i]; a++){
		int j = nn[first[i] + a];
		if(j > i){
		    commonCount = commonElements(nn, first[i], count[i],
						 nn, first[j], count[j],
						 commonNeighbours);

		    for(int b = 0; b < commonCount; b++){
			int k = commonNeighbours[b];
			
			if(k > j){
			    tripletCount++;

			    if(constructProbePlacement(xyz[i], radius[i],
						       xyz[j], radius[j],
						       xyz[k], radius[k],
						       probeRadius,
						       probe0, probe1)){
				int probeCount = 0;
				if(!obscured(probe0, i, j, k)){
				    processPlacement(probe0, i, j, k);

				    probeCount++;
				}

				if(!obscured(probe1, i, j, k)){
				    processPlacement(probe1, i, j, k);
				    
				    probeCount++;
				}

				// placed both probes
				if(probeCount == 2){
				    double rp2 = (2.0*probeRadius)*(2.0*probeRadius);
				    if(distance2(probe0, probe1) < rp2){
					// they intersect one another.
					selfIntersectingProbes++;

					Face f1 = faces.get(faces.size() -1);
					Face f2 = faces.get(faces.size() -2);
					
					f1.intersection = Face.ProbeIntersection;
					f2.intersection = Face.ProbeIntersection;
				    }
				}
			    }
			}
		    }
		}
	    }
	}

	print("triplets", tripletCount);
    }

    /** The last sphere that occluded a probe placement. */
    int cacheSphere = -1;

    /**
     * Is p obscured by any of the neigbhours of i, j or k.
     * But not by i, j or k itself as these were used to
     * construct the point.
     */
    private boolean obscured(double p[], int i, int j, int k){

        // this order seems slightly more effective - k, i, j
        if(obscured2(p, k, i, j)){
            return true;
        }
        if(obscured2(p, i, j, k)){
            return true;
        }
        if(obscured2(p, j, i, k)){
            return true;
        }

        return false;
    }

    /** Is p obscured by a neighbour of i, except for j or k. */
    private boolean obscured2(double p[], int i, int j, int k){
        double localrsq[] = rsq;

        // check the last sphere that clipped
        // can often be the same one
        if(cacheSphere != -1 &&
           cacheSphere != j && cacheSphere != k && cacheSphere != i){
	    distanceComparisons++;

	    if(distance2(xyz[cacheSphere], p) < localrsq[cacheSphere]){
		return true;
	    }

	    cacheSphere = -1;
        }

        int lastn = first[i] + count[i];

        for(int a = first[i]; a < lastn; a++){
            int neighbour = nn[a];

            distanceComparisons++;

            double dx = p[0] - xyz[neighbour][0];
            double dy = p[1] - xyz[neighbour][1];
            double dz = p[2] - xyz[neighbour][2];

            if(dx*dx+dy*dy+dz*dz < localrsq[neighbour]){
                // measurably faster to check after
                // satisfying the distance
                if(neighbour != j && neighbour != k){
                    cacheSphere = neighbour;

                    return true;
                }
            }
        }

        return false;
    }

    /** Add a probe placement for the surface. */
    private Probe addProbePlacement(double pijk[], int i, int j, int k){
	Probe p = new Probe();

	copy(pijk, p.x);

	p.i = i; p.j = j; p.k = k;

	p.r = probeRadius;

	probes.add(p);

	return p;
    }

    /** Temporary for calculating normals. */
    private double nnv[] = new double[3];

    /** Add a vertex for the surface. */
    private Vertex addVertex(double vx[], int i, double px[]){
	Vertex v = new Vertex();

	vector(nnv, vx, px);
	normalise(nnv);

	v.i = i;

	v.vi = tmesh.addPoint(vx, nnv, 0.0, 0.0);

	// record the coords here for convenience.
	copy(vx, v.x);

	return v;
    }

    /** Add an edge for the molecular surface. */
    private Edge addEdge(Vertex v0, Vertex v1, Torus t){
	Edge e = new Edge();

	e.v0 = v0;
	e.v1 = v1;

	if(v0.i == t.i){
	    copy(t.cij, e.cen);
	    e.r = t.rcij;
	    copy(t.uij, e.n);
	}else{
	    copy(t.cji, e.cen);
	    e.r = t.rcji;
	    copy(t.uij, e.n);
	    negate(e.n);
	}

	if(edgeList[v0.i] == null) edgeList[v0.i] = new ArrayList<Edge>(10);
	if(edgeList[v1.i] == null) edgeList[v1.i] = new ArrayList<Edge>(10);

	edgeList[v0.i].add(e);

	if(v0.i != v1.i){
	    // half the torus faces have both edges
	    // on the same atom - make sure we only
	    // add it once.
	    edgeList[v1.i].add(e);
	}

	return e;
    }

    /* Vectors for the atom positions. */
    private static double uij[] =  new double[3];
    private static double uik[] =  new double[3];
    private static double tij[] =  new double[3];
    private static double tik[] =  new double[3];
    private static double uijk[] = new double[3];
    private static double utb[] =  new double[3];
    private static double bijk[] = new double[3];

    private static double cij[] =  new double[3];

    private static double api[] =  new double[3];
    private static double apj[] =  new double[3];
    private static double apk[] =  new double[3];

#define DEBUG 0

    /**
     * Construct the two probe placements for a single triplet.
     *
     * Follows the terminology of
     * Connolly M., J.Appl.Cryst. (1983), 16, 548-558.
     */
    public static boolean constructProbePlacement(double xi[], double ri,
						  double xj[], double rj,
						  double xk[], double rk,
						  double rp,
						  double p0[], double p1[]){
    
	torusAxisUnitVector(uij, xi, xj);
	torusAxisUnitVector(uik, xi, xk);

	// rejig in terms of 1-cos2
	double swijk = baseTriangleAngle(uij, uik);

	basePlaneNormalVector(uijk, uij, uik, swijk);

	torusBasepointUnitVector(utb, uijk, uij);

	basePoint(bijk, tij, utb, uik, tik, swijk);

	double hijk = probeHeight(ri + rp, bijk, xi);

	// special case for certain combinations of
	// sphere radii and positions.
	// i dont think it is an error
	if(hijk < 0.0){
	    return false;
	}

	// + the probe height
	probePosition(p0, bijk,  hijk, uijk);

	probePosition(p1, bijk, -hijk, uijk);

	return true;
    }

    private double pdir[] = new double[3];

    /** Process the probe placement. */
    private void processPlacement(double pijk[], int i, int j, int k){
	Probe p = addProbePlacement(pijk, i, j, k);
	
	// add the vertices
	constructVertex(api, pijk, xyz[i], radius[i]);
	Vertex v0 = addVertex(api, i, p.x);
	constructVertex(apj, pijk, xyz[j], radius[j]);
	Vertex v1 = addVertex(apj, j, p.x);
	constructVertex(apk, pijk, xyz[k], radius[k]);
	Vertex v2 = addVertex(apk, k, p.x);

	// get direction of probe placement
	vector(pdir, bijk, pijk);

	Edge edge0 = null, edge1 = null, edge2 = null;

	// assign edges depending on orientation
	if(dot(pdir, uijk) > 0.0){
	    edge0 = constructProbeEdge(v0, api, v1, apj, apk, pijk, probeRadius);
	    edge1 = constructProbeEdge(v1, apj, v2, apk, api, pijk, probeRadius);
	    edge2 = constructProbeEdge(v2, apk, v0, api, apj, pijk, probeRadius);
	}else{
	    edge0 = constructProbeEdge(v0, api, v2, apk, apj, pijk, probeRadius);
	    edge1 = constructProbeEdge(v2, apk, v1, apj, api, pijk, probeRadius);
	    edge2 = constructProbeEdge(v1, apj, v0, api, apk, pijk, probeRadius);
	}

	// store the face
	Face f = new Face(Face.Type.Concave);
	f.add(edge0);
	f.add(edge1);
	f.add(edge2);

	copy(pijk, f.cen);
	f.r = probeRadius;

	faces.add(f);
    }

    // working space
    private double edgen[] = new double[3];
    private double otherv[] = new double[3];

    private Edge constructProbeEdge(Vertex v0, double p0[],
				   Vertex v1, double p1[],
				   double pother[],
				   double pijk[], double rad){
	int i = v0.i;
	int j = v1.i;

	Edge edge = new Edge();

	// construct edge normal
	normal(edgen, p0, pijk, p1);
	vector(otherv, pijk, pother);

	// make sure it points towards the other vertex.
	if(dot(edgen, otherv) > 0.0){
	    negate(edgen);
	}

	edge.r = rad;
	edge.v0 = v0;
	edge.v1 = v1;

	copy(edgen, edge.n);
	copy(pijk, edge.cen);
	
	// now create the torus if necessary
	Torus torus = findTorus(v0.i, v1.i);

	if(torus == null){
	    torus = new Torus(v0.i, v1.i);

	    torusAxisUnitVector(torus.uij, xyz[i], xyz[j]);

	    torus.rij = torusCenter(torus.tij, xyz[i], radius[i], xyz[j], radius[j], probeRadius);

	    if(torus.rij < probeRadius){
		selfIntersectingTorii++;
		// if the radius is smaller than the probe
		// radius the torus intersects itself
		torus.selfIntersects = true;
	    }

	    // generate contact circles on each end of torus
	    torus.rcij = contactCircle(torus.cij, xyz[i], radius[i], xyz[j], radius[j]);
	    torus.rcji = contactCircle(torus.cji, xyz[j], radius[j], xyz[i], radius[i]);

	    normal(torus.uijnorm, torus.uij);
	    cross(torus.uijnorm2, torus.uij, torus.uijnorm);
	    normalise(torus.uijnorm2);

	    // ok we got the torus
	    // now stick it in all the data structures
	    if(torusList[i] == null) torusList[i] = new ArrayList<Torus>(4);
	    if(torusList[j] == null) torusList[j] = new ArrayList<Torus>(4);
	
	    torusList[i].add(torus);
	    torusList[j].add(torus);

	    if(edgeList[i] == null) edgeList[i] = new ArrayList<Edge>(4);
	    if(edgeList[j] == null) edgeList[j] = new ArrayList<Edge>(4);
	
	    // add edges to atom edge lists.
	    edgeList[i].add(edge);
	    edgeList[j].add(edge);

	    torii.add(torus);
	}

	torus.edges.add(edge);

	return edge;
    }

    /** Is there a torus between i and j. */
    private Torus findTorus(int i, int j){
	if(torusList[i] == null) return null;

	for(Torus torus : torusList[i]){
	    if((torus.i == i && torus.j == j) ||
	       (torus.i == j && torus.j == i)){
		return torus;
	    }
	}

	return null;
    }

    /** Calculate the torus axis unit vector. */
    private static void torusAxisUnitVector(double uij[],
					   double ai[], double aj[]){
	double dij = 1.0/distance(ai, aj);

	for(int i = 0; i < 3; i++){
	    uij[i] = (aj[i] - ai[i])*dij;
	}
    }

    /** Calculate the position of the torus center. */
    private static double torusCenter(double tij[],
				     double ai[], double ri,
				     double aj[], double rj,
				     double rp){
	double rip = ri + rp;
	double rjp = rj + rp;
	double dij2 = distance2(ai, aj);
	double dij = Math.sqrt(dij2);
	double rconst = ((rip*rip) - (rjp*rjp))/dij2;

	for(int i = 0; i < 3; i++){
	    tij[i] = 0.5 * (ai[i] + aj[i]) + 0.5 * (aj[i] - ai[i]) * rconst;
	}

	double rsum = rip + rjp;
	double rdiff = ri - rj;

	return 0.5 *
	    Math.sqrt((rsum*rsum) - dij2) *
	    Math.sqrt(dij2 - (rdiff * rdiff))/dij;
    }

    /** Base triangle angle. */
    private static double baseTriangleAngle(double uij[], double uik[]){
	double dot2 = dot(uij, uik);
	dot2 *= dot2;

	if(dot2 < -1.0) dot2 = -1.0;
	else if(dot2 > 1.0) dot2 = 1.0;

	return Math.sqrt(1. - dot2);
    }
    
    /** Calculate the base plane normal vector. */
    private static void basePlaneNormalVector(double uijk[], double uij[],
					     double uik[], double swijk){
	cross(uijk, uij, uik);

	uijk[0] /= swijk;
	uijk[1] /= swijk;
	uijk[2] /= swijk;
    }

    /** Generate the torus basepoint unit vector. */
    private static void torusBasepointUnitVector(double utb[], double uijk[],
						double uij[]){
	cross(utb, uijk, uij);
    }

    /** Generate the base point. */
    private static void basePoint(double bijk[], double tij[], double utb[],
				 double uik[], double tik[], double swijk){
	double dotut = 0.0;

	for(int i = 0; i < 3; i++){
	    dotut += uik[i] * (tik[i] - tij[i]);
	}

	dotut /= swijk;

	for(int i = 0; i < 3; i++){
	    bijk[i] = tij[i] + utb[i] * dotut;
	}
    }

    /** Calculate probe height. */
    private static double probeHeight(double rip, double bijk[], double ai[]){
	double h2 = (rip*rip) - distance2(bijk, ai);
	if(h2 < 0.0){
	    return -1.0;
	}
	return Math.sqrt(h2);
    }

    /**
     * Generate probe position.
     *
     * Call with -hijk for the opposite placement.
     */
    private static void probePosition(double pijk[], double bijk[],
				     double hijk, double uijk[]){
	for(int i = 0; i < 3; i++){
	    pijk[i] = bijk[i] + hijk * uijk[i];
	}
    }

    /** Construct vertex position. */
    private void constructVertex(double v[], double pijk[],
				 double ai[], double r){
	double rip = r + probeRadius;

	for(int i = 0; i < 3; i++){
	    v[i] = (r * pijk[i] + probeRadius * ai[i])/rip;
	}
    }

    /** Form cross product of two vectors (a = b x c). */
    private static double cross(double a[], double b[], double c[]){
	a[0] = (b[1] * c[2]) - (b[2] * c[1]);
	a[1] = (b[2] * c[0]) - (b[0] * c[2]);
	a[2] = (b[0] * c[1]) - (b[1] * c[0]);

	return a[0] + a[1] + a[2];
    }

    private double ab[] = new double[3];
    private double bc[] = new double[3];

    /** Form cross from three vectors (n = ab x bc). */
    private double normal(double n[],
			 double a[], double b[], double c[]){
	vector(ab, a, b);
	vector(bc, b, c);
	n[0] = (ab[1] * bc[2]) - (ab[2] * bc[1]);
	n[1] = (ab[2] * bc[0]) - (ab[0] * bc[2]);
	n[2] = (ab[0] * bc[1]) - (ab[1] * bc[0]);

	return n[0] + n[1] + n[2];
    }

    /** Negate the vector. */
    private void negate(double a[]){
	a[0]= -a[0];
	a[1]= -a[1];
	a[2]= -a[2];
    }

    /** Generate the plane equation. */
    private double plane_eqn(double p[], double o[], double n[]){
	double px = p[0] - o[0];
	double py = p[1] - o[1];
	double pz = p[2] - o[2];

	// plane equation
	return px*n[0] + py*n[1] + pz*n[2];
    }

    /** Generate the dot product. */
    private static double dot(double a[], double b[]){
	return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    /** Generate vector from a to b. */
    private static void vector(double v[], double a[], double b[]){
	v[0] = b[0] - a[0];
	v[1] = b[1] - a[1];
	v[2] = b[2] - a[2];
    }

    /* Form normal n to vector v. */
    private void normal(double n[], double v[]){
	n[0] = 1.0; n[1] = 1.0; n[2] = 1.0;

        if(v[0] != 0.) n[0] = (v[2] + v[1]) / -v[0];
        else if(v[1] != 0.) n[1] = (v[0] + v[2]) / -v[1];
        else if(v[2] != 0.) n[2] = (v[0] + v[1]) / -v[2];

        normalise(n);
    }

    /** Normalise the vector. */
    private void normalise(double p[]){
	double len = p[0]*p[0] + p[1]*p[1] + p[2]*p[2];

	if(len != 0.0){
	    len = Math.sqrt(len);
	    p[0] /= len;
	    p[1] /= len;
	    p[2] /= len;
	}else{
	    print("Can't normalise vector", p);
	}
    }

    /** Copy b into a. */
    private void copy(double b[], double a[]){
	a[0] = b[0]; a[1] = b[1]; a[2] = b[2];
    }

    private void mid(double m[], double a[], double b[]){
	for(int i = 0; i < 3; i++){
	    m[i] = 0.5 * (a[i] + b[i]);
	}
    }

    /** Distance between two points. */
    private static double distance(double a[], double b[]){
	double dx = a[0] - b[0];
	double dy = a[1] - b[1];
	double dz = a[2] - b[2];

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Distance squared between two points. */
    public static double distance2(double a[], double b[]){
	double dx = a[0] - b[0];
	double dy = a[1] - b[1];
	double dz = a[2] - b[2];

	return dx*dx + dy*dy + dz*dz;
    }

    /** Length of vector. */
    private static double length(double v[]){
	return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

    /** Scale the length of a vector. */
    private static void scale(double v[], double s){
	v[0] *= s; v[1] *= s; v[2] *= s;
    }

    /** Small value for zero comparisons. */
    private static double R_SMALL = 0.000000001;

    /** another angle function. */
    private double angle(double ref[], double n1[], double n2[]){
	double result = angle(ref, n1);

	if(dot(ref, n2) < 0.0){
	    result = -result;
	}

	return result;
    }

    /** Angle between two vectors. */
    private double angle(double v1[], double v2[]){
	double denom = length(v1) * length(v2);
	double result = 0.0;

	if(denom > R_SMALL){
	    result = dot(v1, v2)/denom;
	}else{
	    result = 0.0;
	}

	if(result < -1.0){
	    result = -1.0;
	}if(result > 1.0){
	    result = 1.0;
	}

	result = Math.acos(result);

	return result;
    }

    /** Print a vector. */
    private static void print(String s, double x[]){
	System.out.printf("%-10s", s);
	System.out.printf(" %8.3f,", x[0]);
	System.out.printf(" %8.3f,", x[1]);
	System.out.printf(" %8.3f\n", x[2]);
    }

    private static int dotCount = 45;

    /** Print an int. */
    private static void print(String s, int i){
	int len = s.length();
	System.out.print(s);
	System.out.print(" ");
	for(int dot = len + 1; dot < dotCount; dot++){
	    System.out.print(".");
	}

	System.out.printf(" %7d\n", i);
    }

    /** Print a double. */
    private static void print(String s, double d){
	int len = s.length();
	System.out.print(s);
	System.out.print(" ");
	for(int dot = len + 1; dot < dotCount; dot++){
	    System.out.print(".");
	}

	System.out.printf(" %10.2f\n", d);
    }

    /** Print a String. */
    private static void print(String s, String s2){
	int len = s.length();
	System.out.print(s);
	System.out.print(" ");
	for(int dot = len + 1; dot < dotCount; dot++){
	    System.out.print(".");
	}

	System.out.print(" ");
	System.out.println(s2);
    }

    /* Sphere neighbours. */
    private int first[] = null;
    private int count[] = null;
    private int nn[] = null;
    private int neighbourCount = 0;

    private int commonNeighbours[] = null;
    private int commonCount = 0;

    /**
     * Build a list of each spheres neighbours.
     *
     * A neighbour is any sphere within ri + rj + 2 * rp
     */
    private void buildNeighbourList(){
	first = new int[nxyz];
	count = new int[nxyz];
	// use IntArray to dynamically grow the 
	// neighbour list
	IntArrayList nList = new IntArrayList(nxyz*60);

	int maxNeighbours = 0;

	// replace with more sophisticated algorithm later
	for(int i = 0; i < nxyz; i++){
	    double ri = radius2[i];
	    first[i] = neighbourCount;
	    for(int j = 0; j < nxyz; j++){
		double dij2 = distance2(xyz[i], xyz[j]);
		double rirj = ri + radius2[j];
		if(dij2 < rirj*rirj && i != j){
		    count[i]++;
		    nList.add(j);
		    neighbourCount++;
		}
	    }

	    // record the maximum number of neighbours
	    if(count[i] > maxNeighbours){
		maxNeighbours = count[i];
	    }
	}

	// grab the neighbour list for easy reference
	nn = nList.toIntArray();

	print("total neighbours", neighbourCount);
	print("maximum neighbours", maxNeighbours);

	// allocate space for common neighbours.
	commonNeighbours = new int[maxNeighbours];
    }

    /**
     * Return common elements of sorted arrays a and b.
     * c is assumed long enough to receive all the elements.
     */
    public static int commonElements(int a[], int firsta, int na,
				     int b[], int firstb, int nb,
				     int c[]){

	// if either is the empty set, the intersection is empty
	if(na == 0 || nb == 0){
	    return 0;
	}

	int enda = firsta + na;
	int endb = firstb + nb;

	int j = firsta;
	int k = firstb;
	int t = 0;

	while(j < enda && k < endb){
	    if(a[j] == b[k]){
		c[t] = a[j];
		t++;
		j++;
		k++;
	    }else if(a[j] < b[k]){
		j++;
	    }else{
		k++;
	    }
	}

	return t;
    }

    /** Are the two points within distance d of each other. */
    private boolean within(double x1, double y1, double z1,
			  double x2, double y2, double z2,
			  double d){
	double dx = x2 - x1;
	double dy = y2 - y1;
	double dz = z2 - z1;

	return (dx*dx + dy*dy + dz*dz < d*d);
    }

    /** Calculate distance between two points. */
    private double distance(double x1, double y1, double z1,
			   double x2, double y2, double z2){
	double dx = x2 - x1;
	double dy = y2 - y1;
	double dz = z2 - z1;

	return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Output the probes file. */
    private void outputProbes(){
	FILE output = FILE.write(probesFilename);

	if(output == null){
	    System.err.println("couldn't open " + probesFilename);
	    return;
	}

	for(Probe p : probes){
	    output.print("%.3f", p.x[0]);
	    output.print(" %.3f", p.x[1]);
	    output.print(" %.3f", p.x[2]);
	    output.print(" %.3f\n", probeRadius);
	}

	output.close();
    }

    /** Maximum number of points in sphere template. */
    private static final int MAX_SPHERE_POINTS = 642;

    /** Maximum number of triangles in sphere template. */
    private static int MAX_SPHERE_TRIANGLES = 2*2*MAX_SPHERE_POINTS - 4;

    /* Sphere template data structures. */
    private double sx[][] = new double[MAX_SPHERE_POINTS][3];
    private double snx[][] = new double[MAX_SPHERE_POINTS][3];

    /* The transformed sphere points. */
    private double tsx[][] = new double[MAX_SPHERE_POINTS][3];

    /* Is the sphere point clipped. */
    private int clipped[] = new int[MAX_SPHERE_POINTS];

    /* Is the sphere point on the hull. */
    private int hull[] = new int[MAX_SPHERE_POINTS];

    /** Number of points in the sphere template. */
    private int nsp = 0;

    /* Sphere triangles. */
    private int si[] = new int[MAX_SPHERE_TRIANGLES];
    private int sj[] = new int[MAX_SPHERE_TRIANGLES];
    private int sk[] = new int[MAX_SPHERE_TRIANGLES];
    
    /** Vertex neighbours. */
    private int vn[][] = null;

    /** Vertex neighbour count. */
    private int vncount[] = new int[MAX_SPHERE_POINTS];

    /** Number of triangles in the sphere template. */
    private int nst = 0;

    /** Shortest edge length on sphere template. */
    private double shortestEdge = 0.0;

    /** Longest edge length on sphere template. */
    private double longestEdge = 0.0;

    /** Current longest edge. */
    private double currentLongestEdge = 0.0;

    /** Build sphere template. */
    private void buildSphereTemplate(int subDivisions){
	initialiseSphereTemplate();

	int firstTriangle = 0;
	int triangleCount = nst;

	int start = 0;
	int stop = 0;
	
	for(int sub = 0; sub < subDivisions; sub++){
	    for(int t = firstTriangle; t < triangleCount; t++){
		int midij = findSpherePoint(si[t], sj[t]);
		int midjk = findSpherePoint(sj[t], sk[t]);
		int midki = findSpherePoint(sk[t], si[t]);

		addTriangle(midij, midjk, midki);
		addTriangle(si[t], midij, midki);
		addTriangle(sj[t], midjk, midij);
		addTriangle(sk[t], midki, midjk);
	    }

	    start = triangleCount;
	    stop = nst;

	    firstTriangle = triangleCount;
	    triangleCount = nst;
	}

	// copy down the last group of triangles
	// as only the ones created by the final
	// subdivision are relevant
	nst = 0;

	for(int t = start; t < stop; t++){
	    si[nst] = si[t];
	    sj[nst] = sj[t];
	    sk[nst] = sk[t];
	    nst++;
	}

	for(int i = 0; i < nsp; i++){
	    System.arraycopy(sx[i], 0, snx[i], 0, 3);
	}

	longestEdge = 0.0;
	shortestEdge = 1.e10;
     
	for(int i = 0; i < nst; i++){
	    int vi = si[i];
	    int vj = sj[i];
	    int vk = sk[i];
	    double dedge = distance(sx[vi], sx[vj]);

	    if(dedge < shortestEdge){
		shortestEdge = dedge;
	    }
	    if(dedge > longestEdge){
		longestEdge = dedge;
	    }
	    
	    dedge = distance(sx[vi], sx[vk]);

	    if(dedge < shortestEdge){
		shortestEdge = dedge;
	    }
	    if(dedge > longestEdge){
		longestEdge = dedge;
	    }
	    
	    dedge = distance(sx[vk], sx[vj]);

	    if(dedge < shortestEdge){
		shortestEdge = dedge;
	    }
	    if(dedge > longestEdge){
		longestEdge = dedge;
	    }
	    
	}

	// build the vertex neighbour list.
	vn = new int[nsp][6];
	
	for(int i = 0; i < nst; i++){
	    addNeighbour(si[i], sj[i]);
	    addNeighbour(si[i], sk[i]);
	    addNeighbour(sj[i], sk[i]);
	}

	print("points in sphere template", nsp);
	print("triangles in sphere template", nst);
    }

    /** How many hull neighbours does this point have. */
    private int getHullCount(int svi){
	int hullCount = 0;

	for(int j = 0; j < vncount[svi]; j++){
	    if(hull[vn[svi][j]] == 1){
		hullCount++;
	    }
	}

	return hullCount;
    }

    /** Does this sphere vertex have the other one as a neighbour. */
    private boolean addNeighbour(int i, int v){
	for(int j = 0; j < vncount[i]; j++){
	    if(vn[i][j] == v){
		return true;
	    }
	}

	vn[i][vncount[i]++] = v;
	vn[v][vncount[v]++] = i;

	return false;
    }

    /** Add a triangle to the data structure. */
    private void addTriangle(int ti, int tj, int tk){
	si[nst] = ti;
	sj[nst] = tj;
	sk[nst] = tk;
	nst++;
    }

    /** Find the position of the current mid point. */
    private int findSpherePoint(int i, int j){
	double mx = 0.5 * (sx[i][0] + sx[j][0]);
	double my = 0.5 * (sx[i][1] + sx[j][1]);
	double mz = 0.5 * (sx[i][2] + sx[j][2]);
	double len = Math.sqrt(mx*mx + my*my + mz*mz);
	mx /= len; my /= len; mz /= len;

	for(int d = 0; d < nsp; d++){
	    if(within(mx, my, mz, sx[d][0], sx[d][1], sx[d][2], 0.0001)){
		return d;
	    }
	}
	
	sx[nsp][0] = mx; sx[nsp][1] = my; sx[nsp][2] = mz;

	nsp++;

	return nsp - 1;
    }

    /** Initialise the sphere template. */
    private void initialiseSphereTemplate(){
	sx[0][0]  = -0.851024; sx[0][1]  =         0; sx[0][2]  =  0.525126;
	sx[1][0]  =         0; sx[1][1]  =  0.525126; sx[1][2]  = -0.851024;
	sx[2][0]  =         0; sx[2][1]  =  0.525126; sx[2][2]  =  0.851024;
	sx[3][0]  =  0.851024; sx[3][1]  =         0; sx[3][2]  = -0.525126;
	sx[4][0]  = -0.525126; sx[4][1]  = -0.851024; sx[4][2]  =         0;
	sx[5][0]  = -0.525126; sx[5][1]  =  0.851024; sx[5][2]  =         0;
	sx[6][0]  =         0; sx[6][1]  = -0.525126; sx[6][2]  =  0.851024;
	sx[7][0]  =  0.525126; sx[7][1]  =  0.851024; sx[7][2]  =         0;
	sx[8][0]  =         0; sx[8][1]  = -0.525126; sx[8][2]  = -0.851024;
	sx[9][0]  =  0.851024; sx[9][1]  =         0; sx[9][2]  =  0.525126;
	sx[10][0] =  0.525126; sx[10][1] = -0.851024; sx[10][2] =         0;
	sx[11][0] = -0.851024; sx[11][1] =         0; sx[11][2] = -0.525126;
	nsp = 12;
	si[0]  =  9; sj[0]  =  2; sk[0]  =  6;
	si[1]  =  1; sj[1]  =  5; sk[1]  = 11;
	si[2]  = 11; sj[2]  =  1; sk[2]  =  8;
	si[3]  =  0; sj[3]  = 11; sk[3]  =  4;
	si[4]  =  3; sj[4]  =  7; sk[4]  =  1;
	si[5]  =  3; sj[5]  =  1; sk[5]  =  8;
	si[6]  =  9; sj[6]  =  3; sk[6]  =  7;
	si[7]  =  0; sj[7]  =  2; sk[7]  =  6;
	si[8]  =  4; sj[8]  =  6; sk[8]  = 10;
	si[9]  =  1; sj[9]  =  7; sk[9]  =  5;
	si[10] =  7; sj[10] =  2; sk[10] =  5;
	si[11] =  8; sj[11] = 10; sk[11] =  3;
	si[12] =  4; sj[12] = 11; sk[12] =  8;
	si[13] =  9; sj[13] =  2; sk[13] =  7;
	si[14] = 10; sj[14] =  6; sk[14] =  9;
	si[15] =  0; sj[15] = 11; sk[15] =  5;
	si[16] =  0; sj[16] =  2; sk[16] =  5;
	si[17] =  8; sj[17] = 10; sk[17] =  4;
	si[18] =  3; sj[18] =  9; sk[18] = 10;
	si[19] =  6; sj[19] =  4; sk[19] =  0;
	nst = 20;
    }

    /** Driver routine for the surface generation algorithm. */
    public static void main(String args[]){
	double qLength[] = {0.0, 1.5, 0.9, 0.5, 0.3};
	double probeRadius = 1.5;
	int subdivisions = 1;
	double edgeLength = 1.5;
	String probesFilename = null;

	if(args.length == 0){
	    System.out.print("usage: java surface [-r rp] [-e len]");
	    System.out.print(" [-d subdiv] [-o tmesh] [-t] [-q int]");
	    System.out.println(" file.xyzr");
	    System.exit(1);
	}

	int lastArg = args.length - 1;

	String inputFile = null;
	String tmeshFile = "surface.tmesh";
	
	boolean faceType = false;

	for(int i = 0; i < args.length; i++){
	    if("-r".equals(args[i])){
		if(i < lastArg){
		    probeRadius = FILE.readDouble(args[++i]);
		}
	    }else if("-e".equals(args[i])){
		if(i < lastArg){
		    edgeLength = FILE.readDouble(args[++i]);
		}
	    }else if("-d".equals(args[i])){
		if(i < lastArg){
		    subdivisions = FILE.readInteger(args[++i]);
		}
	    }else if("-q".equals(args[i])){
		if(i < lastArg){
		    int quality = FILE.readInteger(args[++i]);
		    if(quality > 0 && quality < qLength.length){
			subdivisions = quality;
			edgeLength = qLength[quality];
		    }
		}
	    }else if("-o".equals(args[i])){
		if(i < lastArg){
		    tmeshFile = args[++i];
		    if("none".equals(tmeshFile)){
			tmeshFile = null;
		    }
		}
	    }else if("-t".equals(args[i])){
		faceType = true;
	    }else if("-p".equals(args[i])){
		if(i < lastArg){
		    probesFilename = args[++i];
		}
	    }else{
		if(i == lastArg){
		    inputFile = args[i];
		}else{
		    System.err.println("error: unknown command line argument " +
				       args[i]);
		    System.exit(1);
		}
	    }
	}

	if(inputFile == null){
	    System.err.println("error: no input file specified.");
	    System.exit(1);
	}


	FILE f = FILE.open(inputFile);

	if(f == null){
	    System.out.println("error: couldn't open input file " + inputFile);
	    System.exit(2);
	}

	DoubleArrayList x = new DoubleArrayList(1024);
	DoubleArrayList y = new DoubleArrayList(1024);
	DoubleArrayList z = new DoubleArrayList(1024);
	DoubleArrayList r = new DoubleArrayList(1024);
	IntArrayList visible = new IntArrayList(1024);
	int n = 0;

	while(f.nextLine()){
	    if(f.getFieldCount() >= 4){
		x.add(f.getDouble(0));
		y.add(f.getDouble(1));
		z.add(f.getDouble(2));
		r.add(f.getDouble(3));
		visible.add(1);
		n++;
	    }
	}

	f.close();

	double xxx[][] = new double[n][3];

	for(int i = 0; i < n; i++){
	    xxx[i][0] = x.getDouble(i);
	    xxx[i][1] = y.getDouble(i);
	    xxx[i][2] = z.getDouble(i);
	}

	print("number of spheres", n);
	
	AnaSurface s = new AnaSurface(xxx, r.toDoubleArray(), null, n);

	s.density = subdivisions;
	s.probeRadius = probeRadius;
	s.desiredTriangleLength = edgeLength;
	s.probesFilename = probesFilename;

	Tmesh tmesh = s.construct();

	if(!faceType){
	    for(int i = 0; i < tmesh.nt; i++){
		tmesh.tcolor[i] = 0;
	    }
	}

	if(tmeshFile != null){
	    print("tmesh written to", tmeshFile);
	    tmesh.output(tmeshFile);
	}
    }
}
