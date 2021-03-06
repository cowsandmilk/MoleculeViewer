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

/*
  PeriodicTable.java

  Data and utility functions for periodic table
*/

public class PeriodicTable {
    public static final AtomicElement elements[] = new AtomicElement[] {
	new AtomicElement(  0, "h+",    0.00000),
	new AtomicElement(  1, "H",     1.00797),
	new AtomicElement(  2, "He",    4.00260),
	new AtomicElement(  3, "Li",    6.93900),
	new AtomicElement(  4, "Be",    9.01220),
	new AtomicElement(  5, "B",    10.81100),
	new AtomicElement(  6, "C",    12.01115),
	new AtomicElement(  7, "N",    14.00670),
	new AtomicElement(  8, "O",    15.99940),
	new AtomicElement(  9, "F",    18.99840),
	new AtomicElement( 10, "Ne",   20.18300),
	new AtomicElement( 11, "Na",   22.98980),
	new AtomicElement( 12, "Mg",   24.31200),
	new AtomicElement( 13, "Al",   26.98150),
	new AtomicElement( 14, "Si",   28.08600),
	new AtomicElement( 15, "P",    30.97380),
	new AtomicElement( 16, "S",    32.06400),
	new AtomicElement( 17, "Cl",   35.45300),
	new AtomicElement( 18, "Ar",   39.94800),
	new AtomicElement( 19, "K",    39.10200),
	new AtomicElement( 20, "Ca",   40.08000),
	new AtomicElement( 21, "Sc",   44.95600),
	new AtomicElement( 22, "Ti",   47.90000),
	new AtomicElement( 23, "V",    50.94200),
	new AtomicElement( 24, "Cr",   51.99600),
	new AtomicElement( 25, "Mn",   54.93800),
	new AtomicElement( 26, "Fe",   55.84700),
	new AtomicElement( 27, "Co",   58.93320),
	new AtomicElement( 28, "Ni",   58.71000),
	new AtomicElement( 29, "Cu",   63.54600),
	new AtomicElement( 30, "Zn",   65.37000),
	new AtomicElement( 31, "Ga",   69.72000),
	new AtomicElement( 32, "Ge",   72.59000),
	new AtomicElement( 33, "As",   74.92160),
	new AtomicElement( 34, "Se",   78.96000),
	new AtomicElement( 35, "Br",   79.90400),
	new AtomicElement( 36, "Kr",   83.80000),
	new AtomicElement( 37, "Rb",   85.47000),
	new AtomicElement( 38, "Sr",   87.62000),
	new AtomicElement( 39, "Y",    88.90500),
	new AtomicElement( 40, "Zr",   91.22000),
	new AtomicElement( 41, "Nb",   92.90600),
	new AtomicElement( 42, "Mo",   95.94000),
	new AtomicElement( 43, "Tc",   98.90620),
	new AtomicElement( 44, "Ru",  101.07000),
	new AtomicElement( 45, "Rh",  102.90500),
	new AtomicElement( 46, "Pd",  106.40000),
	new AtomicElement( 47, "Ag",  107.86800),
	new AtomicElement( 48, "Cd",  112.40000),
	new AtomicElement( 49, "In",  114.82000),
	new AtomicElement( 50, "Sn",  118.69000),
	new AtomicElement( 51, "Sb",  121.75000),
	new AtomicElement( 52, "Te",  127.60000),
	new AtomicElement( 53, "I",   126.90440),
	new AtomicElement( 54, "Xe",  131.30000),
	new AtomicElement( 55, "Cs",  132.90500),
	new AtomicElement( 56, "Ba",  137.33000),
	new AtomicElement( 57, "La",  138.91000),
	new AtomicElement( 58, "Ce",  140.12000),
	new AtomicElement( 59, "Pr",  140.90700),
	new AtomicElement( 60, "Nd",  144.24000),
	new AtomicElement( 61, "Pm",  145.00000),
	new AtomicElement( 62, "Sm",  150.35000),
	new AtomicElement( 63, "Eu",  151.96000),
	new AtomicElement( 64, "Gd",  157.25000),
	new AtomicElement( 65, "Tb",  158.92400),
	new AtomicElement( 66, "Dy",  162.50000),
	new AtomicElement( 67, "Ho",  164.93000),
	new AtomicElement( 68, "Er",  167.26000),
	new AtomicElement( 69, "Tm",  168.93400),
	new AtomicElement( 70, "Yb",  173.04000),
	new AtomicElement( 71, "Lu",  174.97000),
	new AtomicElement( 72, "Hf",  178.49000),
	new AtomicElement( 73, "Ta",  180.94800),
	new AtomicElement( 74, "W",   183.85000),
	new AtomicElement( 75, "Re",  186.20000),
	new AtomicElement( 76, "Os",  190.20000),
	new AtomicElement( 77, "Ir",  192.20000),
	new AtomicElement( 78, "Pt",  195.09000),
	new AtomicElement( 79, "Au",  196.96700),
	new AtomicElement( 80, "Hg",  200.59000),
	new AtomicElement( 81, "Tl",  204.37000),
	new AtomicElement( 82, "Pb",  207.19000),
	new AtomicElement( 83, "Bi",  208.98000),
	new AtomicElement( 84, "Po",  209.00000),
	new AtomicElement( 85, "At",  210.00000),
	new AtomicElement( 86, "Rn",  222.00000),
	new AtomicElement( 87, "Fr",  223.00000),
	new AtomicElement( 88, "Ra",  226.03000),
	new AtomicElement( 89, "Ac",  227.00000),
	new AtomicElement( 90, "Th",  232.03800),
	new AtomicElement( 91, "Pa",  231.04000),
	new AtomicElement( 92, "U",   238.03000),
	new AtomicElement( 93, "Np",  237.05000),
	new AtomicElement( 94, "Pu",  244.00000),
	new AtomicElement( 95, "Am",  243.00000),
	new AtomicElement( 96, "Cm",  247.00000),
	new AtomicElement( 97, "Bk",  247.00000),
	new AtomicElement( 98, "Cf",  251.00000),
	new AtomicElement( 99, "Es",  254.00000),
	new AtomicElement(100, "Fm",  257.00000),
	new AtomicElement(101, "Md",  258.00000),
	new AtomicElement(102, "No",  259.00000),
	new AtomicElement(103, "Lr",  260.00000),
	new AtomicElement(104, "D",     2.01400),
	new AtomicElement(105, "T",     3.01605),
	new AtomicElement(106, "R",     0.00000),
	new AtomicElement(107, "X",     0.00000),
	new AtomicElement(108, "Gly",  57.04765),
	new AtomicElement(109, "Ala",  71.07474),
	new AtomicElement(110, "Val",  99.12892),
	new AtomicElement(111, "Leu", 113.15601),
	new AtomicElement(112, "Ile", 113.15601),
	new AtomicElement(113, "Ser",  87.07414),
	new AtomicElement(114, "Thr", 101.10123),
	new AtomicElement(115, "Asp", 115.08469),
	new AtomicElement(116, "Asn", 114.09996),
	new AtomicElement(117, "Glu", 129.11178),
	new AtomicElement(118, "Gln", 128.12705),
	new AtomicElement(119, "Lys", 128.17068),
	new AtomicElement(120, "Hyl", 144.17008),
	new AtomicElement(121, "His", 137.13753),
	new AtomicElement(122, "Arg", 156.18408),
	new AtomicElement(123, "Phe", 147.17352),
	new AtomicElement(124, "Tyr", 163.17292),
	new AtomicElement(125, "Trp", 186.21049),
	new AtomicElement(126, "Thy", 758.85682),
	new AtomicElement(127, "Cys", 103.13874),
	new AtomicElement(128, "Cst", 222.28154),
	new AtomicElement(129, "Met", 131.19292),
	new AtomicElement(130, "Pro",  97.11298),
	new AtomicElement(131, "Hyp", 113.11238),
	new AtomicElement(132, "H+",    1.00797),
	new AtomicElement(133, "H2",    2.01594),
    };

    /* standard element name/element no. constants */
    public static final int UNKNOWN       = 0;
    public static final int HYDROGEN      = 1;
    public static final int HELIUM        = 2;
    public static final int LITHIUM       = 3;
    public static final int BERYLLIUM     = 4;
    public static final int BORON         = 5;
    public static final int CARBON        = 6;
    public static final int NITROGEN      = 7;
    public static final int OXYGEN        = 8;
    public static final int FLUORINE      = 9;
    public static final int NEON          = 10;
    public static final int SODIUM        = 11;
    public static final int MAGNESIUM     = 12;
    public static final int ALUMINUM      = 13;
    public static final int SILICON       = 14;
    public static final int PHOSPHORUS    = 15;
    public static final int SULPHUR       = 16;
    public static final int CHLORINE      = 17;
    public static final int ARGON         = 18;
    public static final int POTASSIUM     = 19;
    public static final int CALCIUM       = 20;
    public static final int SCANDIUM      = 21;
    public static final int TITANIUM      = 22;
    public static final int VANADIUM      = 23;
    public static final int CHROMIUM      = 24;
    public static final int MANGANESE     = 25;
    public static final int IRON          = 26;
    public static final int COBALT        = 27;
    public static final int NICKEL        = 28;
    public static final int COPPER        = 29;
    public static final int ZINC          = 30;
    public static final int GALLIUM       = 31;
    public static final int GERMANIUM     = 32;
    public static final int ARSENIC       = 33;
    public static final int SELENIUM      = 34;
    public static final int BROMINE       = 35;
    public static final int KRYPTON       = 36;
    public static final int RUBIDIUM      = 37;
    public static final int STRONTIUM     = 38;
    public static final int YTTRIUM       = 39;
    public static final int ZIRCONIUM     = 40;
    public static final int NIOBIUM       = 41;
    public static final int MOLYBDENUM    = 42;
    public static final int TECHNETIUM    = 43;
    public static final int RUTHENIUM     = 44;
    public static final int RHODIUM       = 45;
    public static final int PALLADIUM     = 46;
    public static final int SILVER        = 47;
    public static final int CADMIUM       = 48;
    public static final int INDIUM        = 49;
    public static final int TIN           = 50;
    public static final int ANTIMONY      = 51;
    public static final int TELLURIUM     = 52;
    public static final int IODINE        = 53;
    public static final int XENON         = 54;
    public static final int CESIUM        = 55;
    public static final int BARIUM        = 56;
    public static final int LANTHANUM     = 57;
    public static final int CERIUM        = 58;
    public static final int PRASEODYMIUM  = 59;
    public static final int NEODYMIUM     = 60;
    public static final int PROMETHIUM    = 61;
    public static final int SAMARIUM      = 62;
    public static final int EUROPIUM      = 63;
    public static final int GADOLINIUM    = 64;
    public static final int TERBIUM       = 65;
    public static final int DYSPROSIUM    = 66;
    public static final int HOLMIUM       = 67;
    public static final int ERBIUM        = 68;
    public static final int THULIUM       = 69;
    public static final int YTTERBIUM     = 70;
    public static final int LUTETIUM      = 71;
    public static final int HAFNIUM       = 72;
    public static final int TANTALUM      = 73;
    public static final int WOLFRAM       = 74;
    public static final int RHENIUM       = 75;
    public static final int OSMIUM        = 76;
    public static final int IRIDIUM       = 77;
    public static final int PLATINUM      = 78;
    public static final int GOLD          = 79;
    public static final int MERCURY       = 80;
    public static final int THALLIUM      = 81;
    public static final int LEAD          = 82;
    public static final int BISMUTH       = 83;
    public static final int POLONIUM      = 84;
    public static final int ASTATINE      = 85;
    public static final int RADON         = 86;
    public static final int FRANCIUM      = 87;
    public static final int RADIUM        = 88;
    public static final int ACTINIUM      = 89;
    public static final int THORIUM       = 90;
    public static final int PROTACTINIUM  = 91;
    public static final int URANIUM       = 92;
    public static final int NEPTUNIUM     = 93;
    public static final int PLUTONIUM     = 94;
    public static final int AMERICIUM     = 95;
    public static final int CURIUM        = 96;
    public static final int BERKELIUM     = 97;
    public static final int CALIFORNIUM   = 98;
    public static final int EINSTEINIUM   = 99;
    public static final int FERMIUM       = 100;
    public static final int MENDELEVIUM   = 101;
    public static final int NOBELIUM      = 102;
    public static final int LAWRENCIUM    = 103;
    public static final int UNQ           = 104;
    public static final int UNP           = 105;

    /**
     * Find the atomic number (atom type) given a symbol.
     */
    public static int getElementFromSymbol( String symbol ) {
	// ok, based on length of label let's look it up...

	if( symbol.length() == 1 ) {
	    char c1 = symbol.charAt(0);
	    switch(c1){
	    case 'C': return CARBON;
	    case 'N': return NITROGEN;
	    case 'O': return OXYGEN;
	    case 'P': return PHOSPHORUS;
	    case 'S': return SULPHUR;
	    case 'H': return HYDROGEN;
	    }
	}

	// still have not figured out type, so we must
	//			search through the table...

	for(int pass = 0; pass < 2; pass++){
	    for (int i = 1; i < elements.length; i++) {
		if(elements[i].symbol.equals(symbol)){
		    return elements[i].atomicNumber;
		}
	    }
	    
	    // some cofactors have A as the first letter of the name.
	    // this really confuses astexviewer so remove the leading a.
	    if(symbol.charAt(0) == 'A'){
		symbol = " " + symbol.substring(1);
	    }
	}

	return UNKNOWN;
    }

    /** Get element from characters from pdb file. */
    public static int getElementFromSymbol(char e0, char e1){
	// ok, based on length of label let's look it up...

	if(e0 == ' '){
	    switch(e1){
	    case 'C': return CARBON;
	    case 'N': return NITROGEN;
	    case 'O': return OXYGEN;
	    case 'P': return PHOSPHORUS;
	    case 'S': return SULPHUR;
	    case 'H': return HYDROGEN;
	    }
	}

	String symbol = null;
	char buf[]    = null;

	if(e0 == ' '){
	    buf = new char[1];
	    buf[0] = e1;
	}else{
	    buf = new char[2];
	    buf[0] = e0;
	    buf[1] = Character.toLowerCase(e1);
	}
	
	symbol = new String(buf);

	return getElementFromSymbol(symbol);
    }

    /** returns the symbol for the specified atom type */
    public static String getAtomSymbolFromElement(int num ) {
	String sym = null;

	if (num == UNKNOWN) {
	    sym = "?";
	}else if (num > 0 && num < elements.length) {
	    sym = elements[num].symbol;
	} else {
	    sym = "";
	}
	return sym;
    }
}
