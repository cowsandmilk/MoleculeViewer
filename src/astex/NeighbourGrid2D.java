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
import it.unimi.dsi.fastutil.ints.IntArrayList;

class NeighbourGrid2D {
    /** The maximum number of cells along an edge. */
    private static final int MaxDim = 64;

    /** The minimum coordinate of the box.. */
    private double xmin = 0.0;
    private double ymin = 0.0;

    /** The maximum coordinate of the box. */
    private double xmax = 0.0;
    private double ymax = 0.0;

    /** The minimum distance we wish to search. */
    private double spacing = 0.0;

    /** The number of boxes along each edge. */
    private int nx = 0;
    private int ny = 0;

    /** The total number of grid boxes. */
    private int ncell = 0;

    /** The list of cell positions for atoms. */
    private IntArrayList list = new IntArrayList();

    /** The head pointers for each cell. */
    private int head[] = null;

    /**
     * Construct a neighbour grid with the following specification.
     */
    public NeighbourGrid2D(){
    }

    /** Get the spacing. */
    public double getSpacing(){
	return spacing;
    }

    /** Reset and reuse a neighbour grid. */
    public void reset(double xmin, double ymin,
		      double xmax, double ymax,
		      double dmin){
	this.xmin = xmin;
	this.ymin = ymin;
	
	this.xmax = xmax;
	this.ymax = ymax;
	
	this.spacing = dmin;

	nx = 1 + (int)((xmax - xmin) / dmin);
	ny = 1 + (int)((ymax - ymin) / dmin);

	if(nx > MaxDim || ny > MaxDim){
	    double biggest = xmax - xmin;
	    if(ymax - ymin > biggest) biggest = ymax - ymin;
	    
	    spacing = biggest / (MaxDim + 1);

	    nx = 1 + (int)((xmax - xmin) / spacing);
	    ny = 1 + (int)((ymax - ymin) / spacing);
	}

	ncell = nx * ny;

	// make sure we have room
	if(head == null || head.length < ncell){
	    head = new int[ncell];
	}

	// initialise cell head pointers
	// -1 shows that the cell is empty
	for(int i = 0; i < ncell; i++){
	    head[i] = -1;
	}
	
	list.clear();
    }

    /** Add an object to the appropriate cell. */
    public void add(int i, double x, double y){
	if(x < xmin || y < ymin ||
	   x > xmax || y > ymax){
	    System.out.println("NeighbourGrid.add(): unable to add " + i +
			       " coordinate outside of box");
	    FILE.out.print("x %8.3f, ", x);
	    FILE.out.print("y %8.3f\n", y);
	    return;
	}

	int ix = (int)((x - xmin)/spacing);
	int iy = (int)((y - ymin)/spacing);
	int icell = findcell(ix, iy);

	if(icell < 0 || icell >= ncell){
	    System.out.println("invalid cell " + icell + " for object " + i);
	    return;
	}

	list.add(head[icell]);
	head[icell] = i;
    }

    /**
     * Return the possible neighbours of the point.
     * d - is the distance we want to find neighbours out to.
     */
    public int getPossibleNeighbours(int id,
				     double x, double y,
				     double d,
				     IntArrayList neighbours,
				     boolean allNeighbours){
	int ibox = ((int)((x - xmin)/spacing));
	int jbox = ((int)((y - ymin)/spacing));

	int offset = 0 + (int)(0.5 + d/spacing);

	int l[] = list.toIntArray();

	for(int i = -offset; i <= offset; i++){
	    int ii = ibox + i;
	    for(int j = -offset; j <= offset; j++){
		int jj = jbox + j;
		int c = findcell(ii, jj);

		if(c != -1){
		    int iobj = head[c];

		    if(iobj != -1){
			if(allNeighbours){
			    if(id == -1){
				while(iobj >= 0){
				    neighbours.add(iobj);
				    iobj = l[iobj];
				}
			    }else{
				while(iobj >= 0){
				    // don't put ourselves
				    // in the list of neighbours
				    if(iobj != id){
					neighbours.add(iobj);
				    }
				    iobj = l[iobj];
				}
			    }
			}else{
			    while(iobj >= 0){
				// don't put things less than us
				// in the list of neighbours
				if(iobj > id){
				    neighbours.add(iobj);
				}
				iobj = l[iobj];
			    }
			}
		    }
		}
	    }
	}

	return neighbours.size();
    }

    /** Find the cell that corresponds to the id's. */
    private int findcell(int i, int j){
	if(i < 0 || j < 0 || i >= nx || j >= ny) return -1;

	return i + j * nx;
    }

}
