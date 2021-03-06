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
 * 09-08-02 mjh
 *	add functionality for managing InsightII grid map files
 *	in the Map object. 
 * 20-12-99 mjh
 *	not correctly handling big and little endian file types.
 * 11-11-99 mjh
 *	created
 */
import java.io.*;

/**
 * A class for storing an electron density map.
 */
public class Map extends Symmetry {
    /** Volume render. */
    public boolean volumeRender = false;

    /** Volume color. */
    public int volumeColor = Color32.red;

    /** Volume min. */
    public double volumeMin = 0.0;

    /** Volume max. */
    public double volumeMax = 1.0;

    /** Maximum number of contour levels. */
    public static final int MaximumContourLevels = 3;

    /** The actual contour levels. */
    private double contourLevels[] = new double[MaximumContourLevels];

    /** The color for the contour levels. */
    private int contourColors[] = new int[MaximumContourLevels];

    /** The color for the contour levels. */
    private int contourStyle[] = new int[MaximumContourLevels];
    
    /** Types for the display style. */
    public static final int Lines = 1;
    public static final int Surface = 2;

    /** Whether each level is displayed. */
    private boolean contourDisplayed[] = new boolean[MaximumContourLevels];

    /** Is the map file big endian or little endian. */
    private boolean littleEndian = true;

    /** Size of map grid. */
    public int grid[] = new int[3];

    /** Format that map data is stored in. */
    private int mode;

    /** Map center. */
    private Point3d center = new Point3d();

    /** Map radius. */
    private double radius;

    /** The file name or url that the map comes from. */
    private String filename;

    /** The name of the map. */
    private String name;

    /** The FILE object that we will read the data from. */
    private FILE file;

    /** Do we need to reread the file contents. */
    private boolean needsReadingFlag = true;

    /** Is header initialised. */
    public boolean headerInitialised = false;
    
    public enum MapType {
	CCP4_BINARY,SYBYL_ASCII,INSIGHT_ASCII,ASTEX_ASCII,O_BINARY
    }

    /** What type is the map? */
    private MapType mapType = MapType.CCP4_BINARY;

    /** The center grid point for the current stored map data. */
    public int centerGrid[] = new int[3];

    /** The minimum grid point for the current stored map data. */
    public int minimumGrid[] = new int[3];

    /** The maximum grid point for the current stored map data. */
    public int maximumGrid[] = new int[3];

    /* various bogus stuff in the map header (some useful). */
    public int nu[] = new int[3];
    public int nv[] = new int[3];
    public int axis[] = new int[3];
    private int ihdr3[] = new int[3];
    private int ihdr4[] = new int[17];
    private int ihdr5[] = new int[1];
    private char ihdr6[] = new char[800];
    private double cell[] = new double[6];
    private double rhdr2[] = new double[3];
    private double rhdr3[] = new double[12];
    public double rms;
    public float data[] = null;

    /* Regular format grids. */
    /** The position of the first grid point. */
    public Point3d origin = new Point3d();

    /** The spacing along each axis. */
    public Point3d spacing = new Point3d();

    /** The number of grid points along each axis. */
    public int ngrid[] = new int[3];

    public boolean initialiseContours = true;

    /** Private default constructor. */
    private Map(){
    }

    /** Public interface to map creation. */
    public static Map create(){
	return new Map();
    }

    /** Create a simple map for internal use. */
    public static Map createSimpleMap(){
	Map map = create();

	map.axis[0] = 0;
	map.axis[1] = 1;
	map.axis[2] = 2;

        map.nv[0] = map.nv[1] = map.nv[2] = 1;
	map.headerInitialised = true;
	map.setMapType(MapType.INSIGHT_ASCII);
	map.setSigma(1.0);

	map.initialiseContours = false;

	return map;
    }

    /** Return the sigma value for the map. */
    public double getSigma(){
	return rms;
    }

    /** Set the sigma level. */
    public void setSigma(double s){
	rms = s;
    }

    /** Does the map need reading. */
    public boolean needsReading(){
        return needsReadingFlag;
    }

    /** Set whether the map needs reading. */
    public void setNeedsReading(boolean flag){
	needsReadingFlag = flag;
    }

    /**
     * Get the value of mapType.
     * @return value of mapType.
     */
    public MapType getMapType() {
	return mapType;
    }
    
    /**
     * Set the value of mapType.
     * @param v  Value to assign to mapType.
     */
    public void setMapType(MapType  v) {
	this.mapType = v;
    }

    /** Read a map from the specified file object. */
    public void read(){
	if(file != null){
	    file.close();
	    file = null;
	}

	if(!headerInitialised){
	    file = FILE.open(filename);
	    readHeader();
	}
    }

    /** Convert from big to little endian byte ordering. */
    public static int convertBigToLittle(int i){

	return (i >>> 24) | (i << 24) |
	    ((i << 8) & 0x00ff0000) | ((i >> 8) & 0x0000ff00);
    }

    /** Read a map from the specified file object. */
    public void readHeader(){

	if(headerInitialised){
	    return;
	}

	if(mapType == MapType.CCP4_BINARY){
	    readCCP4Header();
	}else if(mapType == MapType.O_BINARY){
	    readOHeader();
	}else if(mapType == MapType.INSIGHT_ASCII){
	    readInsightHeader();
	}else if(mapType == MapType.ASTEX_ASCII){
	    readAstexHeader();
	}
    }

    /**
     * Read an astex ascii file .sag (Simple Ascii Grid).
     *
     * Example file is 
     * <pre>
     * # Simple ascii grid file
     * # Header comments begin with '#' and
     * # are optional
     * # next come number of grid points (nx, ny, nz)
     * # then come position of first grid point in x, y, z
     * # then comes spacing of each cell.
     * # Grid must be rectilinear
     * # Finally come nx * ny * nz grid points
     * # x varying fastest.
     * 10 10 10
     * 0.0 0.0 0.0
     * 0.7 0.7 0.7
     * 0.0
     * ...
     * 998 more data points
     * ...
     * 12.3
     */
    private void readAstexHeader(){
	while(file.nextLine()){
	    if(file.getChar(0) != '#'){
		break;
	    }else{
		String header = file.getCurrentLineAsString();
		System.out.println(header);
	    }
	}

	ngrid[0] = file.readIntegerFromField(0);
	ngrid[1] = file.readIntegerFromField(1);
	ngrid[2] = file.readIntegerFromField(2);

	file.nextLine();

	origin.set(file.readDoubleFromField(0),
		   file.readDoubleFromField(1),
		   file.readDoubleFromField(2));

	file.nextLine();

	spacing.set(file.readDoubleFromField(0),
		    file.readDoubleFromField(1),
		    file.readDoubleFromField(2));

	int dataPoints = ngrid[0] * ngrid[1] * ngrid[2];

	ensureMapCapacity(dataPoints);

	for(int i = 0; i < dataPoints; i++){
	    file.nextLine();
	    data[i] = (float)file.readDoubleFromField(0);
	}

	// dummy rms to give direct contour levels
	rms = 1.0;

	file.close();
    }

    /**
     * Read the insight header.
     * This seemingly straight forward ascii grid file
     * format has possibly the most confusing set of
     * interdependent parameters that describe the
     * layout of the grid.
     */
    private void readInsightHeader(){
	// make the rms 1.0 so that we contour
	// directly at the level specified.
	rms = 1.0;

	file.nextLine();
	System.out.println(file.getCurrentLineAsString());

	file.nextLine();
	System.out.println(file.getCurrentLineAsString());

	file.nextLine();
	String fields[] = FILE.split(file.getCurrentLineAsString());

	double lx = FILE.readDouble(fields[0]);
	double ly = FILE.readDouble(fields[1]);
	double lz = FILE.readDouble(fields[2]);

	file.nextLine();
	fields = FILE.split(file.getCurrentLineAsString());

	ngrid[0] = FILE.readInteger(fields[0]) + 1;
	ngrid[1] = FILE.readInteger(fields[1]) + 1;
	ngrid[2] = FILE.readInteger(fields[2]) + 1;

        for(int i = 0; i < 3; i++){
            minimumGrid[i] = 0;
            maximumGrid[i] = ngrid[i];
            nu[i] = 0;
        }

	file.nextLine();
	fields = FILE.split(file.getCurrentLineAsString());

	double dx = lx/(double)(ngrid[0] - 1);
	double dy = ly/(double)(ngrid[1] - 1);
	double dz = lz/(double)(ngrid[2] - 1);

	spacing.set(dx, dy, dz);

	double ox = FILE.readInteger(fields[1]) * dx;
	double oy = FILE.readInteger(fields[3]) * dy;
	double oz = FILE.readInteger(fields[5]) * dz;

	origin.set(ox, oy, oz);

	System.out.println("Origin ... " + origin);
	System.out.println("Spacing .." + spacing);

	int dataPoints = ngrid[0] * ngrid[1] * ngrid[2];

	ensureMapCapacity(dataPoints);

	for(int i = 0; i < dataPoints; i++){
	    file.nextLine();
	    data[i] = (float)file.readDouble(0, 10);
	}

	file.close();
    }
    
    int oheader[] = new int[23];
    byte odata[][][] = null;
    int extent[] = new int[3];
    double prod = 0.0;
    double plus = 0.0;

    /** Read an O header. */
    private void readOHeader(){
	readShortArray(file, oheader);

	for(int i = 0; i < 512-(23*2); i++){
	    int c = file.read();
	    if(c == FILE.EOF){
		System.out.println("eof reading o header");
		return;
	    }
	}

	int iprod = oheader[15];
	int iplus = oheader[16];
	int iscale1 = oheader[17];
	int iscale2 = oheader[18];
	int isigma = oheader[21];

	extent[0] = oheader[3];
	extent[1] = oheader[4];
	extent[2] = oheader[5];

	// and set up ccp4 map equivalents
	nu[0] = oheader[0];
	nu[1] = oheader[1];
	nu[2] = oheader[2];

	nv[0] = oheader[6];
	nv[1] = oheader[7];
	nv[2] = oheader[8];

	grid[0] = extent[0];
	grid[1] = extent[1];
	grid[2] = extent[2];

	// dummy axis reordering
	axis[0] = 0;
	axis[1] = 1;
	axis[2] = 2;

	if(iscale1 == 0) iscale1 = 100;
	if(iscale2 == 0) iscale2 = 100;

	prod = (double)iprod/(double)iscale2;
	plus = iplus;

	double sigma = ((double)isigma-plus)/prod;

	rms = sigma;

	// scale the cell parameters
	for(int i = 0; i < 6; i++){
	    cell[i] = (double)oheader[i+9]/(double)iscale1;
	}

	// now get the data...
	// taken mostly verbatim from Mark Harris'
	// reader for the EDS java viewer.

	int inx = extent[0]/8;	    // How many cubies in each direction
	int iny = extent[1]/8;	    
	int inz = extent[2]/8;	    
	
	if ((extent[0]%8) > 0) inx++; // Including any partials
	if ((extent[1]%8) > 0) iny++;
	if ((extent[2]%8) > 0) inz++;

	int xtraX = (extent[0]%8); // How much of last cubie to read ?
	int xtraY = (extent[1]%8);
	int xtraZ = (extent[2]%8);

	byte data[] = new byte[512];

	//odata = new byte[inx*8 * iny*8 * inz*8];
	odata = new byte[inx*8][iny*8][inz*8];

	for (int k=0; k < inz; k++) {
	    for (int j=0;  j < iny; j++) {
		for (int i=0; i < inx; i++) {
		    for(int d = 0; d < 512; d++){
			data[d] = (byte)file.read();
		    }
	
		    // Swap bytes on littleEndians !
		    
		    boolean byteSwap = true;
		    
		    if (byteSwap) {
			
			for (int ii=0; ii < 511; ii+=2) {
			    byte dum = data[ii];
			    data[ii] = data[ii+1];
			    data[ii+1] = dum;
			}
			
		    }
		    
		    int cubieSizeX = 8;
		    int cubieSizeY = 8;
		    int cubieSizeZ = 8;

		    // For partial cubies
		    if (xtraX > 0 && i == inx-1) cubieSizeX = xtraX;
		    if (xtraY > 0 && j == iny-1) cubieSizeY = xtraY;
		    if (xtraZ > 0 && k == inz-1) cubieSizeZ = xtraZ;

		    for (int n = 0; n < cubieSizeZ; n++) {
			for (int m = 0;  m < cubieSizeY; m++) {
			    for (int l = 0; l < cubieSizeX; l++) {
				
				byte sboxLMN = data[8*8*n+8*m+l];

				// No unsigned types in Java !
				int pt3 = (k)*8 + n;
				int pt2 = (j)*8 + m;
				int pt1 = (i)*8 + l;
				
				odata[pt1][pt2][pt3] = sboxLMN;
			    } // l
			} // m
		    } // n
		    
		} // i
	    } // j
	} // k

	// setup the different matrices. 
	setUnitCell(cell);

	headerInitialised = true;

	printHeader(System.out);
    }

    /** Read a CCP4 header. */
    private void readCCP4Header(){
	readIntegerArray(file, grid);

	// check to see if little or big endian
	int firstWord = grid[0];

	if(firstWord > 65536 || firstWord < -65536){
	    littleEndian = false;
	    grid[0] = convertBigToLittle(grid[0]);
	    grid[1] = convertBigToLittle(grid[1]);
	    grid[2] = convertBigToLittle(grid[2]);
	}

	mode = readInteger(file);
	// we only support modes 0 and 2.
	if(mode != 0 && mode != 2){
	    file.close();
	    file = null;
	    return;
	}

	readIntegerArray(file, nu);
	readIntegerArray(file, nv);
	readDoubleArray(file, cell);
	readIntegerArray(file, axis);
	// make the axis 0-indexed
	for(int i = 0; i < 3; i++){
	    axis[i] -= 1;
	}
	readDoubleArray(file, rhdr2);
	readIntegerArray(file, ihdr3);
	readDoubleArray(file, rhdr3);
	readIntegerArray(file, ihdr4);
	rms = (double)readFloat(file);
	//System.out.println("rms " + rms);
	readIntegerArray(file, ihdr5);

	for(int i = 0; i < ihdr6.length; i++){
	    ihdr6[i] = (char)file.read();
	}

	// skip the symmetry operators
	for(int i = 0; i < ihdr3[1]; i++){
	    file.read();
	}
		
	// store the space group number
	setSpaceGroupNumber(ihdr3[0]);

	System.out.println("ihdr3[0] " + ihdr3[0]);
	System.out.println("gsgn     " + getSpaceGroupNumber());

	// setup the different matrices. 
	setUnitCell(cell);

	printHeader(System.out);
    }

    /** Print the header for the map. */
    public void printHeader(PrintStream printStream){
	Format d5 = new Format("%5d");
	Format f5 = new Format("%10.5f");
	Format f3 = new Format("%8.3f");

	printStream.println("Map " + getFile());

	printStream.print("Grid in file  ");
	printStream.print(d5.format(grid[0]));
	printStream.print(d5.format(grid[1]));
	printStream.print(d5.format(grid[2]));
	printStream.println("");

	printStream.print("Grid origin   ");
	printStream.print(d5.format(nu[0]));
	printStream.print(d5.format(nu[1]));
	printStream.print(d5.format(nu[2]));
	printStream.println("");

	printStream.print("Grid size     ");
	printStream.print(d5.format(nv[0]));
	printStream.print(d5.format(nv[1]));
	printStream.print(d5.format(nv[2]));
	printStream.println("");

	printStream.print("RMS density   " + f5.format(rms));
	printStream.println("");

	printStream.print("Map mode      " + d5.format(mode));
	if(mode == 2){
	    printStream.print(" (4-byte float)");
	}
	printStream.println("");

	printStream.print("Axis order      ");
	for(int j = 0; j < 3; j++){
	    printStream.print(" ");
	    if(axis[j] == 0){
		printStream.print("X");
	    }else if(axis[j] == 1){
		printStream.print("Y");
	    }else if(axis[j] == 2){
		printStream.print("Z");
	    }
	}
	printStream.println("");

	printStream.print("Unit cell     ");
	printStream.print(f3.format(cell[0]));
	printStream.print(f3.format(cell[1]));
	printStream.print(f3.format(cell[2]));
	printStream.print(f3.format(cell[3]));
	printStream.print(f3.format(cell[4]));
	printStream.print(f3.format(cell[5]));
	printStream.println("");
    }

    /** Set the center point of the region we will contour. */
    public void setCenter(double x, double y, double z){
	center.set(x, y, z);
	needsReadingFlag = true;
    }

    /** Set the center point of the region we will contour. */
    public void setCenter(Point3d p){
	center = p.clone();
	needsReadingFlag = true;
    }

    /** Set the radius of the region we will contour. */
    public void setRadius(double r){
	radius = r;
    }

    /** Get the radius of the current map region. */
    public double getRadius(){
	return radius;
    }

    /** Set the map name. */
    public void setName(String mapName){
	name = mapName.replace('\\', '/');
    }

    /** Get the map name. */
    public String getName(){
	return name;
    }

    /**
     * Set the filename. We need to know where it came from so we
     * can reread the map when we recenter.
     */
    public void setFile(String file){
        System.out.println("file |"+file+"|");

	filename = file;
	if(file.indexOf(".grd") != -1){
	    mapType = MapType.INSIGHT_ASCII;
	}else if(file.indexOf(".acnt") != -1){
	    mapType = MapType.SYBYL_ASCII;
	}else if(file.indexOf(".map") != -1){
	    mapType = MapType.CCP4_BINARY;
	}else if(file.indexOf(".omap") != -1){
	    mapType = MapType.O_BINARY;
	}else if(file.indexOf(".sag") != -1){
	    mapType = MapType.ASTEX_ASCII;
	}
    }

    /** Get the filename. */
    public String getFile(){
	return filename;
    }

    /** Ensure we have enough room to store the map. */
    public void ensureMapCapacity(int dataPoints){
	if(data == null || dataPoints > data.length){
	    data = new float[dataPoints];
	}
    }

    /** Read the determined region from the map. */
    public void readRegion(){
	//System.out.println("readRegion");

	if(mapType != MapType.CCP4_BINARY && mapType != MapType.O_BINARY){
	    return;
	}

	if((mapType == MapType.CCP4_BINARY && mode != 0 && mode != 2)){
	    return;
	}

	// does this need to be +1?
	int dx = maximumGrid[0] - minimumGrid[0];
	int dy = maximumGrid[1] - minimumGrid[1];
	int dz = maximumGrid[2] - minimumGrid[2];

	int dataPoints = dx * dy * dz;
	ensureMapCapacity(dataPoints);

	// a running total of how many relevant grid points we read
	int point = 0;

	// read the data slowest axis first
	int grid0 = grid[0];
	int grid1 = grid[1];
	int grid2 = grid[2];
	int max0 = maximumGrid[0];
	int max1 = maximumGrid[1];
	int max2 = maximumGrid[2];
	int min0 = minimumGrid[0];
	int min1 = minimumGrid[1];
	int min2 = minimumGrid[2];

	if(mapType == MapType.CCP4_BINARY){
	    for(int s = 0; s < grid2; s++){
		if(s >= maximumGrid[2]){
		    // we got the region we were interested in
		    break;
		}

		for(int r = 0; r < grid1; r++){
		    for(int c = 0; c < grid0; c++){
			if(s >= min2 && s < max2 &&
			   r >= min1 && r < max1 &&
			   c >= min0 && c < max0){
			    
			    float f = readFloat(file);
			    data[point++] = f;
			}else{
			    // just read as integer if we aren't
			    // using it as we avoid overhead of converting
			    // to float
			    int i = file.skip(4);
			    if(i == FILE.EOF){
				System.out.println("unexpected EOF!");
			    }
			}
		    }
		}
	    }
	}else if(mapType == MapType.O_BINARY){
	    for(int s = 0; s < grid2; s++){
		for(int r = 0; r < grid1; r++){
		    for(int c = 0; c < grid0; c++){
			if(s >= min2 && s < max2 &&
			   r >= min1 && r < max1 &&
			   c >= min0 && c < max0){
			    
			    byte b = odata[c][r][s];

			    short sb = 0;
			    if(b < 0){
				sb = (short)((int)b + 256);
			    }else{
				sb = b;
			    }
			    float f = (float)(((float)sb - plus)/prod);

			    data[point++] = f;
			}
		    }
		}
	    }
	}else{
	    System.out.println("trying to reread data for type " + mapType);
	}

	if(file != null){
	    file.close();
	    file = null;
	}
    }

    /** Convert grid coordinates to cartesian. */
    public void relativeGridToCartesian(double ix, double iy, double iz,
					Point3d p){
	absoluteGridToCartesian(ix + nu[0] + minimumGrid[0],
				iy + nu[1] + minimumGrid[1],
				iz + nu[2] + minimumGrid[2],
				p);
    }

    private Point3d dummy = new Point3d();

    /** Get the map value at relative grid point. */
    public double getValueAtRelativeGrid(int gx, int gy, int gz){
	int xsize = maximumGrid[0] - minimumGrid[0];
	int ysize = maximumGrid[1] - minimumGrid[1];
	int zsize = maximumGrid[2] - minimumGrid[2];

        if(mapType == MapType.INSIGHT_ASCII){
            xsize = ngrid[0];
            ysize = ngrid[1];
            zsize = ngrid[2];
        }

	if(gx >= 0 && gx < xsize &&
	   gy >= 0 && gy < ysize &&
	   gz >= 0 && gz < zsize){
	    int pos = gx;
	    pos += gy * xsize;
	    pos += gz * xsize * ysize;

	    return data[pos];
	}
	return 0.0;
    }

    /** Calculate the relative grid index. */
    public int getRelativeGridIndex(int gx, int gy, int gz){
	int xsize = maximumGrid[0] - minimumGrid[0];
	int ysize = maximumGrid[1] - minimumGrid[1];
	int zsize = maximumGrid[2] - minimumGrid[2];

        if(mapType == MapType.INSIGHT_ASCII){
            xsize = ngrid[0];
            ysize = ngrid[1];
            zsize = ngrid[2];
        }

	if(gx >= 0 && gx < xsize &&
	   gy >= 0 && gy < ysize &&
	   gz >= 0 && gz < zsize){
	    int pos = gx;
	    pos += gy * xsize;
	    pos += gz * xsize * ysize;
	    return pos;
	}
	return -1;
    }

    /** Return the size of map we have loaded. */
    public void getMapBoxDimensions(int dims[]){
        if(mapType == MapType.INSIGHT_ASCII){
	    System.arraycopy(ngrid, 0, dims, 0, 3);
        }else{
            dims[0] = maximumGrid[0] - minimumGrid[0];
            dims[1] = maximumGrid[1] - minimumGrid[1];
            dims[2] = maximumGrid[2] - minimumGrid[2];
        }
    }

    /** Get the actual data array. */
    public float[] getDataArray(){
	return data;
    }

    /** Private variable for converting coordinates. */
    private double xxx[] = new double[3];

    /** Private variable for converting coordinates. */
    private double swapped[] = new double[3];

    /** Convert grid coordiantes to cartesian. */
    public void absoluteGridToCartesian(double ix, double iy, double iz,
					Point3d p){
	xxx[0] = ix; xxx[1] = iy; xxx[2] = iz;

        if(mapType == MapType.INSIGHT_ASCII){
            p.x = origin.x + spacing.x * ix;
            p.y = origin.y + spacing.y * iy;
            p.z = origin.z + spacing.z * iz;

        }else{
            swapped[axis[0]] = xxx[0];
            swapped[axis[1]] = xxx[1];
            swapped[axis[2]] = xxx[2];

            for(int j = 0; j < 3; j++){
                //xxx[j] += nu[j];
                swapped[j] /= nv[j];
            }
		
            //p.set(xxx[0], xxx[1], xxx[2]);
            //p.set(xxx[axis[0]], xxx[axis[1]], xxx[axis[2]]);
            p.set(swapped[0], swapped[1], swapped[2]);
            
            p.transform(fractionalToCartesian);
        }
    }

    /** Find nearest relative grid point to the specified point. */
    public void nearestRelativeGrid(Point3d p, int g[]){
	dummy.set(p);
	dummy.transform(cartesianToFractional);

	xxx[0] = dummy.x * nv[0];
	xxx[1] = dummy.y * nv[1];
	xxx[2] = dummy.z * nv[2];

	swapped[0] = xxx[axis[0]];
	swapped[1] = xxx[axis[1]];
	swapped[2] = xxx[axis[2]];

	swapped[0] -= nu[0] + minimumGrid[0];
	swapped[1] -= nu[1] + minimumGrid[1];
	swapped[2] -= nu[2] + minimumGrid[2];

	g[0] = (int)(swapped[0] + 0.5);
	g[1] = (int)(swapped[1] + 0.5);
	g[2] = (int)(swapped[2] + 0.5);
    }

    /** Find nearest relative grid point to the specified point. */
    public void lowerRelativeGrid(Point3d p, int g[]){
	dummy.set(p);
	dummy.transform(cartesianToFractional);

	xxx[0] = dummy.x * nv[0];
	xxx[1] = dummy.y * nv[1];
	xxx[2] = dummy.z * nv[2];

	swapped[0] = xxx[axis[0]];
	swapped[1] = xxx[axis[1]];
	swapped[2] = xxx[axis[2]];

	swapped[0] -= nu[0] + minimumGrid[0];
	swapped[1] -= nu[1] + minimumGrid[1];
	swapped[2] -= nu[2] + minimumGrid[2];

	g[0] = (int)(swapped[0]);
	g[1] = (int)(swapped[1]);
	g[2] = (int)(swapped[2]);
    }

    /** Print an array. */
    public static void printArray(String name, int array[]){
	System.out.print(name);

	for(int i = 0; i < array.length; i++){
	    System.out.print(" " + array[i]);
	}

	System.out.println("");
    }

    /** Print an array. */
    public static void printArray(String name, double array[]){
	System.out.print(name);

	for(int i = 0; i < array.length; i++){
	    System.out.print(" " + array[i]);
	}

	System.out.println("");
    }

    /** Read an array of ints from underlying file. */
    private int readShortArray(FILE file, int array[]){
	return readShortArray(file, array, array.length);
    }

    /** Read an array of ints from underlying file. */
    private int readShortArray(FILE file, int array[], int count){
	for(int i = 0; i < count; i++){
	    array[i] = readShort(file);
	}

	return 0;
    }

    /** Read an array of ints from underlying file. */
    private int readIntegerArray(FILE file, int array[]){
	return readIntegerArray(file, array, array.length);
    }

    /** Read an array of ints from underlying file. */
    private int readIntegerArray(FILE file, int array[], int count){
	for(int i = 0; i < count; i++){
	    array[i] = readInteger(file);
	}

	return 0;
    }

    /** Read an array of float from underlying file. */
    private int readDoubleArray(FILE file, double array[]){
	return readDoubleArray(file, array, array.length);
    }

    /** Read an array of float from underlying file. */
    private int readDoubleArray(FILE file, double array[], int count){
	for(int i = 0; i < count; i++){
	    array[i] = (double)readFloat(file);
	}

	return 0;
    }

    /**
     * Read a 2 byte word from the file.
     * This should really go in FILE but we'll keep it here for now.
     */
    private int readShort(FILE file){
	if(file == null){
	    System.out.println("Map.readShort: file is null");
	}
	int ch1 = file.read();
	int ch2 = file.read();

	if(ch1 == FILE.EOF|| ch2 == FILE.EOF){
	    System.out.println("eof in readShort");
	    return FILE.EOF;
	}

	// yes this does seem necessary to convert
	// to signed properly...
	if(littleEndian)
	    return (int)((short)(((ch1 << 8) + ((ch2)))));

	return (int)((short)(((ch2 << 8) + ((ch1)))));
    }

    /**
     * Read a 4 byte word from the file.
     * This should really go in FILE but we'll keep it here for now.
     */
    private int readInteger(FILE file){
	int ch1 = file.read();
	int ch2 = file.read();
	int ch3 = file.read();
	int ch4 = file.read();

	if ((ch1 == FILE.EOF|| ch2 == FILE.EOF||
	     ch3 == FILE.EOF|| ch4 == FILE.EOF)){
	    System.out.println("eof");
	}

	if(littleEndian)
	    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }

    /**
     * Read a float from the file.
     * This relies on the Float.intBitsToFloat() method
     * to correctly interpret the underlying data.
     */
    private float readFloat(FILE file){
	int data = readInteger(file);
		
	return Float.intBitsToFloat(data);
    }

    /* Methods for setting the various attributes of the contour leves. */
	
    /** Set the specified contour level. */
    public void setContourLevel(int i, double level){
	if(i >= 0 && i < MaximumContourLevels){
	    contourLevels[i] = level;
	}
    }

    /** Set the specified contour color. */
    public void setContourColor(int i, int color){
	if(i >= 0 && i < MaximumContourLevels){
	    contourColors[i] = color;
	}
    }

    /** Set the specified contour color. */
    public void setContourStyle(int i, int style){
	if(i >= 0 && i < MaximumContourLevels){
	    contourStyle[i] = style;
	}
    }

    /** Set if the specified contour is displayed. */
    public void setContourDisplayed(int i, boolean displayed){
	if(i >= 0 && i < MaximumContourLevels){
	    contourDisplayed[i] = displayed;
	}
    }

    /** Get the specified contour level. */
    public double getContourLevel(int i){
	if(i >= 0 && i < MaximumContourLevels){
	    return contourLevels[i];
	}

	return 0.0;
    }

    /** Get the specified contour color. */
    public int getContourColor(int i){
	if(i >= 0 && i < MaximumContourLevels){
	    return contourColors[i];
	}

	return 0;
    }

    /** Get the specified contour color. */
    public int getContourStyle(int i){
	if(i >= 0 && i < MaximumContourLevels){
	    return contourStyle[i];
	}

	return Lines;
    }

    /** Get if the specified contour is displayed. */
    public boolean getContourDisplayed(int i){
	if(i >= 0 && i < MaximumContourLevels){
	    return contourDisplayed[i];
	}

	return false;
    }

    public boolean hasContoursDisplayed(){
	for(int i = 0; i < MaximumContourLevels; i++){
	    if(getContourDisplayed(i)){
		return true;
	    }
	}

	return false;
    }
}
