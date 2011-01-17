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

/*
 * 15-02-02 mjh
 *	fix space group look up to look at the quoted name
 *	in the space group file, rather than the compact
 *	name that is specified on the same line.
 * 30-11-99 mjh
 *	created
 */
import java.util.*;

/**
 * A class for generating symmetry related copies of molecules.
 */
public class Symmetry {
    /** The unit cell for this symmetry object. */
    public double unitCell[] = new double[6];

    /** The matrix that converts fractional to cartesian coordinates. */
    public Matrix fractionalToCartesian = new Matrix();

    /** The matrix that converts cartesian to fractional coordinates. */
    public Matrix cartesianToFractional = new Matrix();

    /**
     * The SCALE matrix from the PDB file if one exists.
     */
    public Matrix scale = null;

    /** The space group number. */
    private int spaceGroupNumber = 0;

    /** The space group name. */
    private String spaceGroupName = null;

    /** The original space group name. */
    private String originalSpaceGroupName = null;

    /** The list of symmetry operators. */
    private List<Matrix> symmetryOperators = null;

    /** The default location of the symmetry library. */
    private static final String symmetryLibrary = "symmetry.properties";

    /** Get the symmetry operators for the specified space group. */
    public List<Matrix> getSymmetryOperators(){
	if(symmetryOperators != null){
	    return Collections.unmodifiableList(symmetryOperators);
	}

	FILE file = FILE.open(symmetryLibrary);
	String compactedOriginalName = null;

	// compact the original space group name
	if(originalSpaceGroupName != null){
	    compactedOriginalName = originalSpaceGroupName.replace(" ","");
	}

	while(file.nextLine()){
	    String line = file.getCurrentLineAsString();
	    StringTokenizer tokenizer = new StringTokenizer(line);
	    String numberToken = tokenizer.nextToken();
	    String operatorCountToken = tokenizer.nextToken();
	    tokenizer.nextToken();
	    String shortName = tokenizer.nextToken();
	    String name = getSpaceGroupName(line);

	    int number = FILE.readInteger(numberToken);
	    int operatorCount = FILE.readInteger(operatorCountToken);

	    if(number == spaceGroupNumber ||
	       (compactedOriginalName != null && name.equals(compactedOriginalName)) ||
	       (spaceGroupName != null && shortName.equals(spaceGroupName))){

		System.out.println("spacegroup matched symmetry definition");
		System.out.println(line);

		symmetryOperators = new ArrayList<Matrix>(operatorCount);

		for(int i = 0; i < operatorCount; i++){
		    file.nextLine();
		    String operatorString = file.getCurrentLineAsString();
		    readSymmetryOperator(operatorString);
		}

		break;
	    }else{
		for(int i = 0; i < operatorCount; i++){
		    file.nextLine();
		}
	    }
	}

	file.close();

	if(symmetryOperators == null){
	    return Collections.emptyList();
	}
	return Collections.unmodifiableList(symmetryOperators);
    }

    /**
     * Return the symmetry name from the spage group line.
     *
     * The line is of the form
     * 4 2 2 P21 PG2 MONOCLINIC 'P 1 21 1'
     *
     * Previous versions compressed the name from the pdb file
     * and compared it to P21, but we need to compare it to
     * 'P 1 21 1'. This method returns that compacted name.
     */
    private static String getSpaceGroupName(String spaceGroupDescription){
	int firstApostrophe = spaceGroupDescription.indexOf('\'');
	int lastApostrophe = spaceGroupDescription.lastIndexOf('\'');
	String spaceGroupName = spaceGroupDescription.substring(firstApostrophe,lastApostrophe);
	spaceGroupName = spaceGroupName.replace(" ","");

	return spaceGroupName;
    }

    /** Decode one symmetry operator. */
    private void readSymmetryOperator(String line){
	StringTokenizer lineTokenizer = new StringTokenizer(line, ",");
	String xToken = lineTokenizer.nextToken().trim();
	String yToken = lineTokenizer.nextToken().trim();
	String zToken = lineTokenizer.nextToken().trim();
	double c[] = new double[4];

	Matrix m = new Matrix();
	m.setIdentity();

	decodeSymmetryToken(xToken, c);
	m.x00 = c[0]; m.x10 = c[1]; m.x20 = c[2]; m.x30 = c[3];
	decodeSymmetryToken(yToken, c);
	m.x01 = c[0]; m.x11 = c[1]; m.x21 = c[2]; m.x31 = c[3];
	decodeSymmetryToken(zToken, c);
	m.x02 = c[0]; m.x12 = c[1]; m.x22 = c[2]; m.x32 = c[3];

	symmetryOperators.add(m);
    }

    /** The positive axes. */
    private static String positiveAxes[] = {"X", "Y", "Z"};

    /** The fractions. */
    private static String fractions[] = {
	"1/2", "1/3", "2/3", "1/4", "3/4", "1/6", "5/6"
    };

    private static double fractionValues[] = {
	1./2., 1./3., 2./3., 1./4., 3./4., 1./6., 5./6.
    };

    /** Decode the symmetry token in the String. */
    private static void decodeSymmetryToken(String token, double components[]){
	for(int i = 0; i < components.length; i++){
	    components[i] = 0.0;
	}

	for(int i = 0; i < 3; i++){
	    if(token.indexOf("-" + positiveAxes[i]) != -1){
		components[i] = -1.0;
	    }else if(token.indexOf(positiveAxes[i]) != -1){
		components[i] = 1.0;
	    }
	}

	for(int i = 0; i < fractions.length; i++){
	    if(token.indexOf("-" + fractions[i]) != -1){
		components[3] = - fractionValues[i];
		break;
	    }else if(token.indexOf(fractions[i]) != -1){
		components[3] = fractionValues[i];
		break;
	    }
	}
    }

    /** Set the unit cell. */
    public void setUnitCell(double newCell[]){
        System.arraycopy(newCell, 0, unitCell, 0, 6);

	cartesianToFractional = new Matrix();
	fractionalToCartesian = new Matrix();

	generateMatrices(unitCell,
			 cartesianToFractional,
			 fractionalToCartesian);
    }

    /**
     * Generate the cartesian/fractional interconversion matrices.
     * This is more involved than might appear at first.
     * We need to handle a variety of special cases, like
     * when the unit cell does not follow the standard pdb convention.
     * Also there can be mistakes in the supplied SCALE records
     * which we need to trap.
     */
    public void prepareSymmetry(){
	if(scale == null){
	    // no symmetry or definitely no special cases
	    // matrices should already be defined
	    return;
	}

	Matrix s = scale;

	Matrix c2f = getCartesianToFractionalMatrix();

	if(!s.equals(c2f)){
	    System.err.println("prepareSymmetry: SCALE does not match " +
			       "calculated cartesian->fractional matrix");
	    System.err.println("prepareSymmetry: fixing symmetry");

	    Matrix sinv = new Matrix();

	    Matrix.invert(s, sinv);

	    Matrix tmp = new Matrix(s);
	    tmp.transform(sinv);

	    Matrix f2c = getFractionalToCartesianMatrix();

	    Matrix check = new Matrix(s);
	    check.transform(f2c);

	    check.x30 = 0.0;
	    check.x31 = 0.0;
	    check.x32 = 0.0;

	    Matrix checkt = new Matrix(check);

	    checkt.transpose();

	    check.transform(checkt);

	    // this should really take account of the
	    // unit cell parameters but this value seems
	    // to work ok...

	    if(check.isIdentity(1.e-2)){
		// reorientation was pure rotation
		c2f.copy(s);
		f2c.copy(sinv);
	    }else{
		System.err.println("prepareSymmetry: inconsistent " +
				   "scale matrix - ignored");

		// remove all trace of the scale matrix as it was bogus
		scale = null;
	    }
	}
    }

    /** Set the space group number. */
    public void setSpaceGroupNumber(int number){
	spaceGroupNumber = number;
	spaceGroupName = null;
    }

    /** Get the space group number. */
    public int getSpaceGroupNumber(){
	return spaceGroupNumber;
    }

    /** Set the space group name. */
    public void setSpaceGroupName(String name){
	spaceGroupName = name;
	spaceGroupNumber = 0;
    }

    /** Set the original space group name (with spaces). */
    public void setOriginalSpaceGroupName(String s){
	originalSpaceGroupName = s;
    }

    /** Get the original space group name. */
    public String getOriginalSpaceGroupName(){
	return originalSpaceGroupName;
    }

    /** Get the fractionalising matrix. */
    public Matrix getCartesianToFractionalMatrix(){
	return cartesianToFractional;
    }

    /** Get the defractionalising matrix. */
    public Matrix getFractionalToCartesianMatrix(){
	return fractionalToCartesian;
    }

    /** Return the square of the argument. */
    private static double SQ(double x){
	return x*x;
    }

    /** Generate the fractional to cartesian matrices. */
    private static  void generateMatrices(double cell[],
					 Matrix cartesianToFractional,
					 Matrix fractionalToCartesian){
	double cabg[] = new double[3];
	double cabgs[] = new double[3];
	double sabg[] = new double[3];
	double abcs[] = new double[3];
	double sabgs1;
	double volume;

	/* Initialise the transformation matrices. */

	cartesianToFractional.setIdentity();
	fractionalToCartesian.setIdentity();

	for(int i = 0; i < 3; i++){
	    cabg[i]=Math.cos(Math.PI*cell[i+3]/180.0);
	    sabg[i]=Math.sin(Math.PI*cell[i+3]/180.0);
	}

	cabgs[0]=(cabg[1]*cabg[2]-cabg[0])/(sabg[1]*sabg[2]);
	cabgs[1]=(cabg[2]*cabg[0]-cabg[1])/(sabg[2]*sabg[0]);
	cabgs[2]=(cabg[0]*cabg[1]-cabg[2])/(sabg[0]*sabg[1]);
	volume=cell[0]*cell[1]*cell[2]*
	    Math.sqrt(1.0+2.0*cabg[0]*cabg[1]*cabg[2]
		      -SQ(cabg[0])-SQ(cabg[1])-SQ(cabg[2]));
	abcs[0]=cell[1]*cell[2]*sabg[0]/ volume;
	abcs[1]=cell[0]*cell[2]*sabg[1]/ volume;
	abcs[2]=cell[0]*cell[1]*sabg[2]/ volume;
	sabgs1=Math.sqrt(1.0-SQ(cabgs[0]));
		
	/* Cartesian to fractional conversion matrix. */

	cartesianToFractional.x00=1.0/cell[0];
	cartesianToFractional.x10=-cabg[2]/(sabg[2]*cell[0]);
	cartesianToFractional.x20=-(cabg[2]*sabg[1]*cabgs[0]+cabg[1]*sabg[2])/
	    (sabg[1]*sabgs1*sabg[2]*cell[0]);
	cartesianToFractional.x11=1.0/(sabg[2]*cell[1]);
	cartesianToFractional.x21=cabgs[0]/(sabgs1*sabg[2]*cell[1]);
	cartesianToFractional.x22=1.0/(sabg[1]*sabgs1*cell[2]);

	/* Fractional to cartesian matrix. */

	fractionalToCartesian.x00= cell[0];
	fractionalToCartesian.x10= cabg[2]*cell[1];
	fractionalToCartesian.x20= cabg[1]*cell[2];
	fractionalToCartesian.x11= sabg[2]*cell[1];
	fractionalToCartesian.x21=-sabg[1]*cabgs[0]*cell[2];
	fractionalToCartesian.x22=sabg[1]*sabgs1*cell[2];
    }
}
