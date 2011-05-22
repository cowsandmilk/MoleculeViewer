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

/**
 * Class for performing near neighbour calculations.
 */
package astex;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class Lattice {
    /** List of cells to search for all pairs search. */
    private static final int offsets[][] = {
	{  1, -1, 0 },
	{  1,  0, 0 },
	{  1,  1, 0 },
	{  0,  1, 0 },
	{ -1, -1, 1 },
	{  0, -1, 1 },
	{  1, -1, 1 },
	{ -1,  0, 1 },
	{  0,  0, 1 },
	{  1,  0, 1 },
	{ -1,  1, 1 },
	{  0,  1, 1 },
	{  1,  1, 1 }
    };

    /** Maximum distance we will search. */
    private double maxDistance = -1.0;

    /** Set the maximum search distance. */
    private void setMaximumDistance(double d){
	Log.check(d > 0.0, "search distance must be > 0.0");

	if(maxDistance > 0.0){
	    Log.error("can't reset maximum distance");
	}else{
	    maxDistance = d;
	}
    }

    /** Return the maximum search distance. */
    public double getMaximumDistance(){
	return maxDistance;
    }

    /** Create an empty lattice object. */
    public Lattice(double d){
	setMaximumDistance(d);
    }

    /** Add an object reference to the lattice. */
    public void add(int id, double x, double y, double z){
	int i = BOX(x);
	int j = BOX(y);
	int k = BOX(z);

	// map into an internal id range
	// so that we can trivially handle
	// non-contiguous ids
	int actualId = ids.size();

	ids.add(id);

	int c = findcell(i, j, k);

	if(c == -1){
	    int hashval = HASH(i, j, k);
	    IntArrayList cellList = hashTable[hashval];

	    if(cellList == null){
		cellList = new IntArrayList();
		hashTable[hashval] = cellList;
	    }

	    c = celli.size();

	    cellList.add(c);
	    // -1 indicates the cell is currently empty
	    // this will get changed below
	    head.add(-1);

	    celli.add(i);
	    cellj.add(j);
	    cellk.add(k);
	}
	
	// shuffle the object id's
	// to add the new object to the cell chain
	list.add(head.getInt(c));
	head.set(c, actualId);
    }

    /** Get the contents of the cell. */
    public int getCellContents(int icell, IntArrayList c){
	if(icell == -1){
	    return 0;
	}

	int j = head.getInt(icell);

	if(j == -1){
	    return 0;
	}

	while(j >= 0){
	    c.add(ids.getInt(j));
	    j = list.getInt(j);
	}

	return c.size();
    }

    /** Find the cell that corresponds to the id's. */
    private int findcell(int i, int j, int k){
	int hashval = HASH(i, j, k);

	// search list with this hashval, if not present return -1
	IntArrayList cellList = hashTable[hashval];

	if(cellList != null){
	    int ci[] = celli.toIntArray();
	    int cj[] = cellj.toIntArray();
	    int ck[] = cellk.toIntArray();
	    int cl[] = cellList.toIntArray();
	    int cellEntries = cellList.size();

	    for(int c = 0; c < cellEntries; c++){
		int cellIndex = cl[c];

		if(ci[cellIndex] == i &&
		   cj[cellIndex] == j &&
		   ck[cellIndex] == k){
		    return cellIndex;
		}
	    }
	}

	return -1;
    }

    /**
     * Return the possible neighbours of the point.
     */
    public int getPossibleNeighbours(int id,
				     double x, double y, double z,
				     IntArrayList neighbours,
				     boolean allNeighbours){
	int ibox = BOX(x);
	int jbox = BOX(y);
	int kbox = BOX(z);

	int h[] = head.toIntArray();
	int l[] = list.toIntArray();
	int idsArray[] = ids.toIntArray();

	for(int i = -1; i <= 1; i++){
	    int ii = ibox + i;
	    for(int j = -1; j <= 1; j++){
		int jj = jbox + j;
		for(int k = -1; k <= 1; k++){
		    int kk = kbox + k;
		    int c = findcell(ii, jj, kk);

		    if(c != -1){
			int iobj = h[c];

			if(iobj != -1){
			    if(allNeighbours){
				if(id == Undefined){
				    while(iobj >= 0){
					neighbours.add(idsArray[iobj]);
					iobj = l[iobj];
				    }
				}else{
				    while(iobj >= 0){
					// don't put ourselves
					// in the list of neighbours
					if(idsArray[iobj] != id){
					    neighbours.add(idsArray[iobj]);
					}
					iobj = l[iobj];
				    }
				}
			    }else{
				while(iobj >= 0){
				    // don't put things less than us
				    // in the list of neighbours
				    if(idsArray[iobj] > id){
					neighbours.add(idsArray[iobj]);
				    }
				    iobj = l[iobj];
				}
			    }
			}
		    }
		}
	    }
	}

	return neighbours.size();
    }

    /** Working space for cell objects gathers. */
    private IntArrayList cell1 = new IntArrayList();
    private IntArrayList cell2 = new IntArrayList();

    /** Get the possible pairs of neighbours from a cell. */
    public int getPossibleCellNeighbours(int cid, IntArrayList objects){
	cell1.clear();
	getCellContents(cid, cell1);

	int count1 = cell1.size();
	int c1[] = cell1.toIntArray();

	for(int i = 0; i < count1; i++){
	    int oi = c1[i];
	    for(int j = 0; j < count1; j++){
		if(i != j){
		    int oj = c1[j];
		    if(oi < oj){
			objects.add(oi);
			objects.add(oj);
		    }
		}
	    }
	}

	int icell = celli.getInt(cid);
	int jcell = cellj.getInt(cid);
	int kcell = cellk.getInt(cid);

	for(int ioff = 0; ioff < offsets.length; ioff++){
	    int ii = icell + offsets[ioff][0];
	    int jj = jcell + offsets[ioff][1];
	    int kk = kcell + offsets[ioff][2];

	    int c = findcell(ii, jj, kk);

	    if(c != -1){
		cell2.clear();
		getCellContents(c, cell2);
		int count2 = cell2.size();
		int c2[] = cell2.toIntArray();

		for(int i = 0; i < count1; i++){
		    int oi = c1[i];
		    for(int j = 0; j < count2; j++){
			int oj = c2[j];
			objects.add(ids.getInt(oi));
			objects.add(ids.getInt(oj));
		    }
		}
	    }
	}

	return objects.size();
    }

    /** Print info about the Lattice object. */
    public void printStatistics(int info){
	int occupiedHashSlots = 0;
	int minCells = Integer.MAX_VALUE;
	int maxCells = Integer.MIN_VALUE;
	int zeroCells = 0;

	for(int i = 0; i < HASHTABLESIZE; i++){
	    if(hashTable[i] != null){
		occupiedHashSlots++;
		IntArrayList cellList = hashTable[i];
		int cellCount = cellList.size();

		if(cellCount > maxCells){
		    maxCells = cellCount;
		}
		if(cellCount < minCells){
		    minCells = cellCount;
		}
	    }else{
		zeroCells++;
	    }
	}

	FILE.out.print("hash table size %5d\n", HASHTABLESIZE);
	FILE.out.print("occupied cells  %5d\n", occupiedHashSlots);
	FILE.out.print("zero cells      %5d\n", zeroCells);
	FILE.out.print("max cells/slot  %5d\n", maxCells);
	FILE.out.print("ave cells/slot  %7.1f\n",
		       (double)celli.size()/(double)HASHTABLESIZE);
    }

    /** Return hash value for object cell. */
    private int HASH(int i, int j, int k){
	if(i < 0) i = -i;
	if(j < 0) j = -j;
	if(k < 0) k = -k;

	return (i&HS_MASK) | ((j&HS_MASK) << SHIFT) | ((k&HS_MASK) << SHIFT2);
    }

    /** Return the cell box id along one axis. */
    public int BOX(double x){
	if(x > 0.0)
	    return (int)(x/maxDistance);

	return (int)(x/maxDistance)-1;
    }

    /** Size of Hash box. */
    private static final int HS = 16;

    /** Mask for mapping into Hash box range. */
    private static final int HS_MASK = HS - 1;

    /** Shift for mapping into Hash box range [ln2(HS)]. */
    private static final int SHIFT = 4;

    /** Shift for mapping into Hash box range [ln2(HS)]. */
    private static final int SHIFT2 = 2 * SHIFT;

    /** Size of hashtable. */
    private static final int HASHTABLESIZE = HS*HS*HS;

    /** Table of indexes to cells. */
    private IntArrayList hashTable[] = new IntArrayList[HASHTABLESIZE];

    /** The cell indexes for each cell. */
    private IntArrayList celli = new IntArrayList();
    private IntArrayList cellj = new IntArrayList();
    private IntArrayList cellk = new IntArrayList();

    /** The head and list pointers for each cell. */
    private IntArrayList head  = new IntArrayList();
    private IntArrayList list  = new IntArrayList();

    /** Mapping from passed ids to internal ids. */
    private IntArrayList ids   = new IntArrayList();

    /** Constant to indicate we don't care about ids. */
    public final static int Undefined = Integer.MIN_VALUE;
}
