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
 * 14-08-02 mjh
 *	isDisplayed checks to see if the parent molecule is
 *	displayed as well.
 * 17-02-00 mjh
 *	add Visited attribute.
 * 07-11-99 mjh
 *	change getBond(i) so that it doesn't perform range checking. it is
 *	better that we get out of bound exception rather than have to
 *	handle null that could be returned.
 * 05-11-99 mjh
 *	modify getAtomLabel() so that it will return a label
 *	made of the element type followed by the atom id if there
 *	is no label defined.
 * 28-10-99 mjh
 *	created
 */

import astex.generic.*; 
import java.util.*;
import java.awt.Color;

/**
 * A class for holding information about an atom.
 */
public class Atom extends Point3d implements Selectable, GenericInterface {
    /** Default constructor. */
    private Atom(){
	super();
	initialise();
    }

    /** Undefined atom id. */
    public static final int Undefined = -1;

    /** An integer for storing various attributes. */
    public EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

    /** The radius of the atom. */
    private float radius = -1.0f;

    /** The stick radius of the atom. */
    private float ballRadius = defaultBallRadius;
    
    public enum Attribute {
	Solvent, Labelled, Selected, Hetero, TemporarilySelected, Visited,
	Displayed, Wide, VDWSphere, Surface, SurfaceContext, Property, Ring,
	BallAndStick, Cylinder, CustomLabelled, ModellingActive, Aromatic,
	ModellingEnvironment, ModellingXray, ModellingFixed, NameLeftJustified
    }

    public static final EnumSet<Attribute> DisplayedMask =
	EnumSet.of(Attribute.Displayed, Attribute.Cylinder, Attribute.BallAndStick, Attribute.VDWSphere);

    /** The X coordinate attribute. */
    public static final int X = 0;

    /** The Y coordinate attribute. */
    public static final int Y = 1;

    /** The Z coordinate attribute. */
    public static final int Z = 2;

    /** The B coordinate attribute. */
    public static final int B = 3;

    /** The O coordinate attribute. */
    public static final int O = 4;

    /** The ID  attribute. */
    public static final int ID = 5;

    /** The partial charge attribute. */
    public static final int Q = 6;

    /** The energy. */
    public static final int E = 7;

    /** The color of selected atoms. */
    private static final int selectedColor = Color32.yellow;
    
    /** Default radius for balls in ball and stick. */
    private static final float defaultBallRadius = 0.3f;

    /** Hybridisation states. */
    public static final int sp3 = 3;
    public static final int sp2 = 2;
    public static final int sp = 1;

    /** Initialise an atom. */
    public void initialise(){
	bonds         = null;
	firstBond     = null;
	secondBond    = null;
	thirdBond     = null;
	id            = Undefined;
	color         = 0xff000000;
	transparency  = 255;
	charge        = 0;
	partialCharge = 0.0f;
	attributes    = EnumSet.of(Attribute.Displayed);
	bFactor       = 0.0f;
	occupancy     = 1.0f;
	radius        = -1.0f;
	ballRadius    = defaultBallRadius;
        atomType      = null;
	insertionCode = ' ';
    }

    /**
     * Dynamic array containing the list of bonds for this atom.
     * 
     * Used only when we have more than three bonds.
     */
    private List<Bond> bonds;
	
    /** Reference to first bond (or null if there are no bonds). */
    private Bond firstBond;
	
    /** Reference to second bond. */
    private Bond secondBond;
	
    /** Reference to third bond. */
    private Bond thirdBond;
	
    /** Screen x-coordinate. */
    public transient int xs;

    /** Screen y-coordinate. */
    public transient int ys;

    /** Screen z-coordinate. */
    public transient int zs;

    /** Insertion code if any. */
    public char insertionCode;

    /** The color index for this atom. */
    private int color;

    /** The transparency setting for spheres. */
    private int transparency;

    /** The (usually sybyl) atom type of the atom. */
    public String atomType = null;

    /** Set the atomType. */
    public void setAtomType(String s){
        atomType = s;
    }

    /** Get the atomType. */
    public String getAtomType(){
        return atomType;
    }

    /** Set the transparency. */
    public void setTransparency(int t){
	transparency = t;
    }

    /** Get the transparency. */
    public int getTransparency(){
	return transparency;
    }

    /** Set the insertion code. */
    public void setInsertionCode(char c){
	insertionCode = c;
    }

    /** Get the insertion code. */
    public char getInsertionCode(){
	return insertionCode;
    }

    /** Maximum number of atoms we will cache. */
    private static final int MaxAtomCacheSize = 2048;

    /** Cache for reusing atoms. */
    private static Stack<Atom> atomCache = new Stack<Atom>();

    /**
     * Create an atom.
     *
     * This is the only public interface for creating new atoms.
     */
    public static synchronized Atom create(){
	if(!atomCache.isEmpty()){
	    Atom atom = atomCache.pop();
	    atom.initialise();
	    return atom;
	}
	return new Atom();
    }

    /** Release an atom after we finished with it. */
    public synchronized void release(){
	if(atomCache.size() < MaxAtomCacheSize){
	    atomCache.push(this);
	}
    }

    /** The atomic number of this atom. */
    private int element;

    /** A label for the atom. */
    private String label;

    /** A custom label for the atom. */
    private String customLabel;

    /** An integer id for the atom. */
    private int id;

    /** Charge for the atom. */
    private int charge;

    /** BFactor for the atom. */
    private float bFactor;

    /** Occupancy for the atom. */
    private float occupancy;

    /** Partial charge for the atom. */
    private float partialCharge;

    /** Set the bFactor. */
    public void setBFactor(double b){
	bFactor = (float)b;
    }

    /** Return the bFactor. */
    public double getBFactor(){
	return (double)bFactor;
    }

    /** Set the occupancy. */
    public void setOccupancy(double o){
	occupancy = (float)o;
    }

    /** Get the occupancy. */
    public double getOccupancy(){
	return (double)occupancy;
    }

    /**
     * Get the value of partialCharge.
     * @return value of partialCharge.
     */
    public float getPartialCharge() {
	return partialCharge;
    }
    
    /**
     * Set the value of partialCharge.
     * @param v  Value to assign to partialCharge.
     */
    public void setPartialCharge(double  v) {
	this.partialCharge = (float)v;
    }
    

    /** Get an attribute. */
    public double getAttribute(int attribute){
	switch(attribute){
	case X: return x;
	case Y: return y;
	case Z: return z;
	case B: return bFactor;
	case O: return occupancy;
	case Q: return partialCharge;
	case E: return 0.0;
	case ID: return getId();
	default: 
	    System.out.println("Atom: attempt to get unknown attribute " +
			       attribute);
	    return 0.0;
	}
    }

    /** Set the id for this atom. */
    public void setId(int newId){
	if(newId == Undefined){
	    System.out.println("explicitly setting id to undefined " + this);
	    System.out.println("(anti-desirable)");
	}
	id = newId;
    }

    /** Get the id for this atom. */
    public int getId(){
	if(id == Undefined){
	    Molecule molecule = getMolecule();
	    molecule.assignAtomNumbers();
	}

	return id;
    }

    /** Set the element of this atom. */
    public void setElement(int newElement){
	element = newElement;
    }

    /** Get the element type of this atom. */
    public int getElement(){
	return element;
    }

    /** Get the atomic symbol for this atom. */
    public String getAtomSymbol(){
	String symbol = PeriodicTable.getAtomSymbolFromElement(element);

	if(symbol == null)
	    return "C";

        return symbol;
    }

    /** Set the atom label. */
    public void setAtomLabel(String newLabel){
	label = newLabel;
    }

    /** Get the atom label. */
    public String getAtomLabel(){
	if(label != null)
	    return label;

        String symbol =
	    PeriodicTable.getAtomSymbolFromElement(element);

	return symbol.toUpperCase() + getId();
    }

    /** Generate an atom label according to a format. */
    public String generateLabel(String format){
	// if there is no % character just return the original
        // or no url substitution character return as well
	if(format.indexOf('%') == -1 && format.indexOf('`') == -1) return format;

	StringBuilder s = new StringBuilder(16);
	int len = format.length();

	for(int i = 0; i < len; i++){
	    char c = format.charAt(i);

	    if(c == '%'){
		if(i == len - 1){
		    System.out.println("Atom.generateLabel: % at end of " +
				       "format string, just placing %");
		}else{
		    char nc = format.charAt(i + 1);
		    if(nc == '%'){
			s.append('%');
		    }else if(nc == 'x'){
			s.append(String.format("%.3f", x));
		    }else if(nc == 'y'){
			s.append(String.format("%.3f", y));
		    }else if(nc == 'z'){
			s.append(String.format("%.3f", z));
		    }else if(nc == 'e'){
			double v = 0.0;
			s.append(String.format("%.2f", v));
		    }else if(nc == 'b'){
			s.append(String.format("%.1f", getBFactor()));
		    }else if(nc == 'B'){
			s.append(String.format("%d", (int)getBFactor()));
		    }else if(nc == 'o'){
			s.append(String.format("%.2f", getOccupancy()));
		    }else if(nc == 'q'){
			s.append(String.format("%.2f", getPartialCharge()));
		    }else if(nc == 'i'){
			s.append(String.format("%-4d", getId()));
		    }else if(nc == 'I'){
			s.append(getInsertionCode());
		    }else if(nc == 'A'){
			Residue res = getResidue();
			label = getAtomLabel();
			if(res != null && res.isStandardAminoAcid()){
			    int labelLength = label.length();
			    for(int l = 0; l < labelLength; l++){
				char lc = label.charAt(l);
				// put the escape code for the 
				// greek letter
				if(l == 1){
				    s.append('\\');
				    s.append(Character.toLowerCase(lc));
				}else{
				    s.append(lc);
				}
			    }
			}else{
			    s.append(label);
			}
		    }else if(nc == 'a'){
			s.append(getAtomLabel());
		    }else if(nc == 'r'){
			Residue res = getResidue();
			if(res != null){
			    s.append(String.format("%d", res.getNumber()));
			    if(res.getInsertionCode() != 0 &&
			       res.getInsertionCode() != ' '){
				s.append(res.getInsertionCode());
			    }
			}
		    }else if(nc == 'R'){
			Residue res = getResidue();
			if(res != null){
			    s.append(res.getName());
			}
		    }else if(nc == 'f'){
			// fancy residue name. Capital followed by lower case
			Residue res = getResidue();
			if(res != null){
			    String name = res.getName();
			    s.append(name.charAt(0));
			    s.append(name.toLowerCase().substring(1));
			}
		    }else if(nc == 'c'){
			Residue res = getResidue();
			if(res != null){
			    Chain chain = res.getParent();
			    if(chain != null && !" ".equals(chain.getName())){
				s.append(chain.getName());
			    }
			}
		    }else if(nc == 'm'){
			Residue res = getResidue();
			if(res != null){
			    Chain chain = res.getParent();
			    if(chain != null){
				Molecule mol = chain.getParent();
				if(mol != null){
				    s.append(mol.getName());
				}
			    }
			}
		    }
		}

		i++;
	    }else{
		s.append(c);
	    }
	}

        format = s.toString();
	s.setLength(0);
	len = format.length();

        // substitue url enclosed within ` characters
        // done after variable substitution so that you can get atom info
        // into the url string
	for(int i = 0; i < len; i++){
	    char c = format.charAt(i);

            if(c == '`'){
                // its a url to call
                StringBuilder url = new StringBuilder(16);

                ++i;
                for( ; i < len; i++){
                    c = format.charAt(i);
                    if(c == '`') break;
                    
                    url.append(c);
                }

                FILE f = FILE.openURL(url.toString());

                if(f == null){
                    s.append("invalid_url");
                }else{

                    while(f.nextLine()){
                        String contents = f.getCurrentLineAsString();
                        s.append(contents);
                    }
                }
            }else{
                s.append(c);
            }
        }

	return s.toString();
    }

    /** Get the atom symbol for the element. */
    public String getSymbol(){
	return PeriodicTable.getAtomSymbolFromElement(element);
    }

    /** Get the default mass for the atom. */
    public double getMass(){
	return PeriodicTable.elements[element].mass;
    }

    /** Get the atom charge. */
    public int getCharge(){
	return charge;
    }

    /** Set the atom charge. */
    public void setCharge(int newCharge){
	charge = newCharge;
    }

    /** Set the atom color. */
    public void setColor(int newColor){
	color = newColor;
    }

    /** Get the atom color. */
    public int getColor(){
	if(color == 0xff000000){
	    switch(element){
	    case PeriodicTable.CARBON: color = Color32.green; break;
	    case PeriodicTable.OXYGEN: color = Color32.red; break;
	    case PeriodicTable.NITROGEN: color = Color32.blue; break;
	    case PeriodicTable.SULPHUR: color = Color32.yellow; break;
	    case PeriodicTable.PHOSPHORUS: color = Color32.magenta; break;
	    case PeriodicTable.CHLORINE: color = Color32.orange; break;
	    case PeriodicTable.FLUORINE: color = Color32.cyan; break;
	    case PeriodicTable.BROMINE: color = Color32.magenta; break;
	    case PeriodicTable.HYDROGEN:
	    default: color = Color32.white; break;
	    }
	}

	return color;
    }

    /** Get the color or selected color if selected. */
    public int getSelectedColor(){
	if(isSelected())
	    return selectedColor;

        return getColor();
    }

    /** Set color back to undefined. */
    public void resetColor(){
	color = 0xff000000;
    }

    /** The residue to which the atom belongs. */
    private Residue parentResidue;

    /** Get the molecule to which the atom belongs. */
    public Molecule getMolecule(){
	if(parentResidue != null){
	    Chain parentChain = parentResidue.getParent();
	    if(parentChain != null){
		return parentChain.getParent();
	    }
	}

	return null;
    }

    /** Set the residues to which the atom belongs. */
    public void setParent(Residue residue){
	parentResidue = residue;
    }

    /** Get the residue to which the atom belongs. */
    public Residue getResidue(){
	return parentResidue;
    }

    /** Add a bond to the atom. */
    public void addBond(Bond bond){
	if(bonds != null){
	    // we already have more than two bonds so just keep adding.
	    bonds.add(bond);
	}else if(firstBond == null){
	    // no bonds yet so stick it there.
	    firstBond = bond;
	}else if(secondBond == null){
	    // first slot full, second empty stick it here
	    secondBond = bond;
	}else if(thirdBond == null){
	    // first slot full, second empty stick it here
	    thirdBond = bond;
	}else{
	    // both slots are full so allocate the list
	    bonds = new ArrayList<Bond>(6);
	    bonds.add(firstBond);
	    bonds.add(secondBond);
	    bonds.add(thirdBond);
	    bonds.add(bond);
	    firstBond = null;
	    secondBond = null;
	    thirdBond = null;
	}
    }

    /** Return a specified bond or null if it doesn't exist. */
    public Bond getBond(int index){
	if(bonds != null) // have dynamic array so it should be in here
	    return bonds.get(index);
	if(index == 0)
	    return firstBond;
	if(index == 1)
	    return secondBond;
	if(index == 2)
	    return thirdBond;

	return null;
    }

    /** Return the specified bonded atom. */
    public Atom getBondedAtom(int index){
	Bond bond = getBond(index);

	if(bond != null){
	    return bond.getOtherAtom(this);
	}

	return null;
    }

    /** Return the total number of bonds. */
    public int getBondCount(){
	if(bonds != null)
	    return bonds.size();
	if(thirdBond != null)
	    return 3;
	if(secondBond != null)
	    return 2;
	if(firstBond != null)
	    return 1;

	return 0;
    }

    /** Does this atom have a bond to the specified atom. */
    public Bond getBond(Atom otherAtom){
	int bondCount = getBondCount();

	for(int i = 0; i < bondCount; i++){
	    Bond bond = getBond(i);

	    if(bond.getOtherAtom(this) == otherAtom){
		return bond;
	    }
	}

	return null;
    }

    /** Get bonded atom with this name. */
    public Atom getBondedAtom(String nm){
	int bondCount = getBondCount();
	for(int i = 0; i < bondCount; i++){
	    Atom otherAtom = getBondedAtom(i);
	    if(otherAtom.getAtomLabel().equals(nm) &&
	       (otherAtom.getInsertionCode() == ' ' ||
		otherAtom.getInsertionCode() == 'A')){
		return otherAtom;
	    }
	}

	return null;
    }

    /** Does this atom have a bond to the other atom. */
    public boolean hasBond(Atom otherAtom){
	return getBond(otherAtom) != null;
    }

    /** Does this atom have bonds that came from an explicit bond. */
    public boolean hasExplicitBond(){
	int bondCount = getBondCount();

	for(int b = 0; b < bondCount; b++){
	    Bond bond = getBond(b);
	    if(bond.isExplicitBond()){
		return true;
	    }
	}

	return false;
    }

    /**
     * Is this atom connected by two bonds to the specified atom.
     *
     * That is are they 1,3 connected.
     */
    public boolean connected13(Atom targetAtom){
	for(int b1 = 0; b1 < getBondCount(); b1++){
	    Bond bond1 = getBond(b1);
	    Atom otherAtom = bond1.getOtherAtom(this);

	    for(int b2 = 0; b2 < otherAtom.getBondCount(); b2++){
		Bond bond2 = otherAtom.getBond(b2);
		Atom finalAtom = bond2.getOtherAtom(otherAtom);

		if(finalAtom == targetAtom){
		    return true;
		}
	    }
	}

	return false;
    }

    /**
     * Is this atom connected by three bonds to the specified atom.
     *
     * That is are they 1,4 connected.
     */
    public boolean connected14(Atom targetAtom){
	int bondCount = getBondCount();
	int targetBondCount = targetAtom.getBondCount();

	for(int b1 = 0; b1 < bondCount; b1++){
	    Bond bond1 = getBond(b1);
	    Atom bond1Other = bond1.getOtherAtom(this);
			
	    for(int b2 = 0; b2 < targetBondCount; b2++){
		Bond bond2 = targetAtom.getBond(b2);
		Atom bond2Other = bond2.getOtherAtom(targetAtom);

		if(bond1Other.hasBond(bond2Other)){
		    return true;
		}
	    }
	}

	return false;
    }

    /** Are these atoms connected 1,2, 1,3 or 1,4. */
    public boolean connected121314(Atom otherAtom){
	if(hasBond(otherAtom)){
	    return true;
	}

	if(connected13(otherAtom)){
	    return true;
	}

	if(connected14(otherAtom)){
	    return true;
	}

	return false;
    }

    /** Get the bonding radius for the atom. */
    public double getBondingRadius(){
	if(element == PeriodicTable.CARBON ||
	   element == PeriodicTable.NITROGEN ||
	   element == PeriodicTable.FLUORINE ||
	   element == PeriodicTable.OXYGEN)
	    return 0.9;
	if(element == PeriodicTable.HYDROGEN)
	    return 0.3;
	if(element == PeriodicTable.WOLFRAM)
	    return 1.0;
	if(element == PeriodicTable.UNKNOWN)
	    return 0.0;
	if(element == PeriodicTable.IRON)
	    return 1.25;
	if(element == PeriodicTable.MANGANESE)
            // we would prefer not to have Mn bonded to anything
	    return 0.0;
	return 1.2;
    }

    /** Get the VDW radius of this atom. */
    public double getVDWRadius(){
	if(radius < 0.0f){
	    if(element == PeriodicTable.CARBON){
		radius = 1.88f;
	    }else if(element == PeriodicTable.NITROGEN){
		radius = 1.64f;
	    }else if(element == PeriodicTable.OXYGEN){
		radius = 1.42f;
	    }else if(element == PeriodicTable.HYDROGEN){
		radius = 1.05f;
	    }else if(element == PeriodicTable.SULPHUR){
                radius = 1.77f;
            }else if(element == PeriodicTable.PHOSPHORUS){
		radius = 1.7f;
            }else if(element == PeriodicTable.IODINE){
                // www.webelements.com
		radius = 1.98f;
            }else if(element == PeriodicTable.BROMINE){
                // www.webelements.com
		radius = 1.85f;
            }else if(element == PeriodicTable.CHLORINE){
                // www.webelements.com
		radius = 1.75f;
	    }else{
		radius = 1.35f;
	    }
	}

	return (double)radius;
    }

    /** Get the biggest radius that is displayed. */
    public double getBiggestDisplayedRadius(){
	double maxRadius = 0.0;

	if(attributes.contains(Attribute.VDWSphere)){
	    double r = getVDWRadius();
	    if(r > maxRadius){
		maxRadius = r;
	    }
	}

	if(attributes.contains(Attribute.BallAndStick)){
	    double r = getBallRadius();
	    if(r > maxRadius){
		maxRadius = r;
	    }
	}

	// can't see where that is recorded...
	if(attributes.contains(Attribute.Cylinder)){
	    double r = getBallRadius();
	    if(r > maxRadius){
		maxRadius = r;
	    }
	}

	return maxRadius + 0.05;
    }

    /** Set the vdw radius. */
    public void setVDWRadius(double r){
	radius = (float)r;
    }

    /** Set the vdw radius. */
    public void setBallRadius(double r){
	ballRadius = (float)r;
    }

    /** Get the vdw radius. */
    public double getBallRadius(){
	return ballRadius;
    }

    /** Transform this atom to screen coordinates. */
    public void transformToScreen(Matrix m){
	// add 0.5 to make it the nearest integer
	double xx = x*m.m00 + y*m.m10 + z*m.m20 + m.m30 + 0.5;
	double yy = x*m.m01 + y*m.m11 + z*m.m21 + m.m31 + 0.5;
	double zz = x*m.m02 + y*m.m12 + z*m.m22 + m.m32;

	xs = (int)(xx) << Renderer.FixedBits;
	ys = (int)(yy) << Renderer.FixedBits;
	zs = (int)((zz) * (1 << (Renderer.FixedBits+8)));
    }

    /* Various kinds of tests for particular atom types. */
	
    /** Is this an aliphatic hydrogen. */
    public boolean isAliphaticHydrogen(){
	if(element != PeriodicTable.HYDROGEN){
	    return false;
	}

	if(getBondCount() == 1){
	    Atom atom = getBondedAtom(0);
	    if(atom.getElement() == PeriodicTable.CARBON){
		return true;
	    }
	}
		
	return false;
    }

    /**
     * Is this atom an HBond donor.
     * Only suitable for standard aminoacid names.
     */
    public boolean isHBondDonor(){
	String name = label;

	if(isSolvent())
	    return true;
	if(!parentResidue.isStandardAminoAcid())
	    return false;
	
	String resname = parentResidue.getName();

	if("N".equals(name)){
	    return !("PRO".equals(resname));
	}
	if("NE1".equals(name) && "TRP".equals(resname))
	    return true;
	if("OG".equals(name) && "SER".equals(resname))
	    return true;
	if("OG1".equals(name) && "THR".equals(resname))
	    return true;
	if("ND2".equals(name) && "ASN".equals(resname))
	    return true;
	if("NE2".equals(name) && "GLN".equals(resname))
	    return true;
	if("OH".equals(name) && "TYR".equals(resname))
	    return true;
	if("NE1".equals(name) && "HIS".equals(resname))
	    return true;
	if(("NE2".equals(name) || "ND1".equals(name)) &&
		 "HIS".equals(resname))
	    return true;
	if("NZ".equals(name) && "LYS".equals(resname))
	    return true;
	return (("NE".equals(name) || "NH1".equals(name) ||
		 "NH2".equals(name)) && "ARG".equals(resname));
    }

    /**
     * Is this atom an HBond donor.
     * Only suitable for standard aminoacid names.
     */
    public boolean isHBondAcceptor(){
	String name = label;

	if(isSolvent())
	    return true;
	if(!parentResidue.isStandardAminoAcid())
	    return false;
	if("O".equals(name))
	    return true;

	String resname = parentResidue.getName();

	if("NE1".equals(name) && "TRP".equals(resname))
	    return true;
	if("OG".equals(name) && "SER".equals(resname))
	    return true;
	if("OG1".equals(name) && "THR".equals(resname))
	    return true;
	if("OD1".equals(name) && "ASN".equals(resname))
	    return true;
	if("OE1".equals(name) && "GLN".equals(resname))
	    return true;
	if("OH".equals(name) && "TYR".equals(resname))
	    return true;
	if("NE1".equals(name) && "HIS".equals(resname))
	    return true;
	if(("NE2".equals(name) || "ND1".equals(name)) &&
		 "HIS".equals(resname))
	    return true;
	if(("OD1".equals(name) || "OD2".equals(name)) &&
		 "ASP".equals(resname))
	    return true;
	return (("OE1".equals(name) || "OE2".equals(name)) &&
		 "GLU".equals(resname));
    }

    /** Does the atom have any attributes set? */
    public boolean hasAttributes(){
	return attributes.size() != 0;
    }

    /** Set or clear a particular attribute. */
    public void setOrClearAttribute(Attribute attribute, boolean state){
	if(state){
	    attributes.add(attribute);
	}else{
	    attributes.remove(attribute);
	}
    }

    /** Set whether the atom is solvent. */
    public void setSolvent(boolean state){
	setOrClearAttribute(Attribute.Solvent, state);
    }

    /** Is this atom a solvent atom. */
    public boolean isSolvent(){
	return attributes.contains(Attribute.Solvent);
    }

    /** Set whether the atom is solvent. */
    public void setCustomLabel(String l){
	customLabel = l;

	if(customLabel == null){
	    setOrClearAttribute(Attribute.CustomLabelled, false);
	}else{
	    setOrClearAttribute(Attribute.CustomLabelled, true);
	}
    }

    /** Set whether the atom is solvent. */
    public String getCustomLabel(){
	return customLabel;
    }

    /** Is this atom a solvent atom. */
    public boolean isLabelled(){
	return attributes.contains(Attribute.Labelled);
    }

    /** Set whether the atom is solvent. */
    public void setLabelled(boolean state){
	setOrClearAttribute(Attribute.Labelled, state);
    }

    /** Set whether the atom is selected. */
    public void setSelected(boolean state){
	setOrClearAttribute(Attribute.Selected, state);
    }

    /** Is this atom selected. */
    public boolean isSelected(){
	return attributes.contains(Attribute.Selected);
    }

    /** Set whether the atom is a heteroatom. */
    public void setHeteroAtom(boolean state){
	setOrClearAttribute(Attribute.Hetero, state);
    }

    /** Is this atom a heteroatom. */
    public boolean isHeteroAtom(){
	return attributes.contains(Attribute.Hetero);
    }

    /** Set whether the atom is temporarily selected. */
    public void setTemporarilySelected(boolean state){
	setOrClearAttribute(Attribute.TemporarilySelected, state);
    }

    /** Is this atom temporarily selected. */
    public boolean isTemporarilySelected(){
	return attributes.contains(Attribute.TemporarilySelected);
    }

    /** Set whether the atom has been visited. */
    public void setVisited(boolean state){
	setOrClearAttribute(Attribute.Visited, state);
    }

    /** Has this atom been visited. */
    public boolean isVisited(){
	return attributes.contains(Attribute.Visited);
    }

    /** Set whether the atom has been wide. */
    public void setWide(boolean state){
	setOrClearAttribute(Attribute.Wide, state);
    }

    /** Has this atom been wide. Its a bond property.*/
    public boolean isWide(){
	return attributes.contains(Attribute.Wide);
    }

    /** Set whether the atom has been displayed. */
    public void setDisplayed(boolean state){
	setOrClearAttribute(Attribute.Displayed, state);
    }

    /** Has this atom been displayed. */
    public boolean isSimpleDisplayed(){
	return attributes.contains(Attribute.Displayed);
    }

    /** Has this atom been displayed. */
    public boolean isDisplayed(){
	Molecule mol = getMolecule();
	if(mol.getDisplayed()) {
	    EnumSet<Attribute> tmp = attributes.clone();
	    tmp.retainAll(DisplayedMask);
	    return !tmp.isEmpty();
	}

	return false;
    }

    /** Set the selection state. */
    public int select(int state){
	switch(state){
	case 1: setSelected(true); break;
	case 2: if(!isSelected()) setSelected(true); break;
	case 3: setSelected(false); break;
	}

	return isSelected() ? 1 : 0;
    }

    public String selectStatement(){
	Residue res = getResidue();
	String resSelect = res.selectStatement();

	String command = "id " + getId();

	return command + " and " + resSelect;
    }

    /** Return the hybridisation state of the atom. */
    public int getHybridisation(){
	int bondCount = getBondCount();

	for(int b = 0; b < bondCount; b++){
	    Bond bond = getBond(b);
	    Bond.BondOrder bondOrder = bond.getBondOrder();

	    if(bondOrder == Bond.BondOrder.DoubleBond)
		return sp2;
	    if(bondOrder == Bond.BondOrder.TripleBond)
		return sp;
	}

	return sp3;
    }

    /** Placeholder for the default format atom string. */
    private static String defaultLongFormat = null;

    @Override
    public String toString(){
	if(defaultLongFormat == null){
	    defaultLongFormat = Settings.getString("config", "atom.long.format");
	    if(defaultLongFormat == null){
		// no config so set it here
		defaultLongFormat = "%a %R %c:%r ID=%i  X=%x Y=%y Z=%z  O=%o B=%b %m";
	    }
	}
	
	return generateLabel(defaultLongFormat);
    }

    /** Remove a bond from this atom. */
    public void removeBond(Bond b){
	if(bonds != null){
	    bonds.remove(b);
	}else{
	    if(firstBond == b){
		firstBond = secondBond;
		secondBond = thirdBond;
	    }else if(secondBond == b){
		secondBond = thirdBond;
	    }else if(thirdBond == b){
		thirdBond = null;
	    }
	}

	Molecule mol = getMolecule();

	mol.removeBond(b);
    }

    /** Remove all bonds. */
    public void removeAllBonds(){
        if(bonds != null){
            bonds.clear();
            bonds = null;
        }

        firstBond = secondBond = thirdBond = null;
    }

    public static final String XAttribute = "x";
    public static final String YAttribute = "y";
    public static final String ZAttribute = "z";
    public static final String BAttribute = "b";
    public static final String OAttribute = "o";
    public static final String QAttribute = "charge";
    public static final String Color = "color";
    public static final String Radius = "radius";
    public static final String Opacity = "opacity";
    public static final String Element = "element";

    /** Arbitrary properties. */
    private HashMap<Object,Object> properties = null;

    /** Get Object representing key. */
    public Object get(Object key, Object def){
        Object val = null;

        if(key.equals(XAttribute)) val = Double.valueOf(getAttribute(X));
        else if(key.equals(YAttribute)) val = Double.valueOf(getAttribute(Y));
        else if(key.equals(ZAttribute)) val = Double.valueOf(getAttribute(Z));
        else if(key.equals(BAttribute)) val = Double.valueOf(getAttribute(B));
        else if(key.equals(OAttribute)) val = Double.valueOf(getAttribute(O));
        else if(key.equals(QAttribute)) val = Double.valueOf(getAttribute(Q));
        else if(key.equals(Element)) val = Integer.valueOf(element);
        else if(key.equals(Color)) val = new Color(getColor());
        else if(key.equals(Radius)) val = Double.valueOf(getVDWRadius());
        else if(key.equals(Opacity)) val = Integer.valueOf(transparency);
        else {
            if(properties != null){
                val = properties.get(key);
            }
        }

        return val == null ? def : val;
    }

    public void edit(String name, String value){
        if("type".equals(name)){
            setAtomType(value);
        }else{
            System.err.println("Atom.edit: unknown parameter name " + name);
        }
    }

    /** Set an object value. */
    public Object set(Object key, Object value){
        if(key.equals(XAttribute)) x = ((Double)value).doubleValue();
        else if(key.equals(YAttribute)) y = ((Double)value).doubleValue();
        else if(key.equals(ZAttribute)) z = ((Double)value).doubleValue();
        else if(key.equals(BAttribute)) bFactor = (float)((Double)value).doubleValue();
        else if(key.equals(OAttribute)) occupancy = (float)((Double)value).doubleValue();
        else if(key.equals(QAttribute)) partialCharge = (float)((Double)value).doubleValue();
        else if(key.equals(Radius)) radius = (float)((Double)value).doubleValue();
        else if(key.equals(Element)) element = ((Integer) value).intValue();
        else if(key.equals(Color)) setColor(((Color)value).getRGB());
        else if(key.equals(Opacity)) transparency = ((Integer)value).intValue();
        else{
            if(properties == null){
                properties = new HashMap<Object,Object>(20);
            }
            if(value == null){
                properties.remove(key);
            }else{
                properties.put(key, value);
            }
        }

        return null;
    }

    /** Get an Enumeration of our parents. */
    public Iterator<GenericInterface> getParents(Object type){
        return null;
    }

    /** Add a parent. */
    public void addParent(GenericInterface parent){
    }

    /** Remove a parent. */
    public void removeParent(GenericInterface parent){
    }

    /** Get an enumeration of our children. */
    public Iterator<GenericInterface> getChildren(Object type){
        return null;
    }

    /** Add a child. */
    public void addChild(GenericInterface child){
    }

    /** Remove a child. */
    public void removeChild(GenericInterface child){
    }

    /** Add a listener. */
    public void addListener(GenericEventInterface geh){
    }

    /** Remove a listener. */
    public void removeListener(GenericEventInterface geh){
    }

}
