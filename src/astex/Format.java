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
 * Cay S. Horstmann & Gary Cornell, Core Java
 * Published By Sun Microsystems Press/Prentice-Hall
 * Copyright (C) 1997 Sun Microsystems Inc.
 * Copyright David Hall, Boston University, 2011 
 * All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this 
 * software and its documentation for NON-COMMERCIAL purposes
 * and without fee is hereby granted provided that this 
 * copyright notice appears in all copies. 
 * 
 * THE AUTHORS AND PUBLISHER MAKE NO REPRESENTATIONS OR 
 * WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. THE AUTHORS
 * AND PUBLISHER SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED 
 * BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING 
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
 
/**
 * A class for formatting numbers that follows printf conventions.
 * Also implements C-like atoi and atof functions
 * @version 1.04 13 Sep 1998
 * @author Cay Horstmann
 */

public class Format {
	/** 
	 * Formats the number following printf conventions.
	 * Main limitation: Can only handle one format parameter at a time
	 * Use multiple Format objects to format more than one number
	 * @param s the format string following printf conventions
	 * The string has a prefix, a format code and a suffix. The prefix and suffix
	 * become part of the formatted output. The format code directs the
	 * formatting of the (single) parameter to be formatted. The code has the
	 * following structure
	 * <ul>
	 * <li> a % (required)
	 * <li> a modifier (optional)
	 * <dl>
	 * <dt> + <dd> forces display of + for positive numbers
	 * <dt> 0 <dd> show leading zeroes
	 * <dt> - <dd> align left in the field
	 * <dt> space <dd> prepend a space in front of positive numbers
	 * <dt> # <dd> use "alternate" format. Add 0 or 0x for octal or hexadecimal numbers. Don't suppress trailing zeroes in general floating point format.
	 * </dl>
	 * <li> an integer denoting field width (optional)
	 * <li> a period followed by an integer denoting precision (optional)
	 * <li> a format descriptor (required)
	 * <dl>
	 * <dt>f <dd> floating point number in fixed format
	 * <dt>e, E <dd> floating point number in exponential notation (scientific format). The E format results in an uppercase E for the exponent (1.14130E+003), the e format in a lowercase e.
	 * <dt>g, G <dd> floating point number in general format (fixed format for small numbers, exponential format for large numbers). Trailing zeroes are suppressed. The G format results in an uppercase E for the exponent (if any), the g format in a lowercase e.
	 * <dt>d, i <dd> integer in decimal
	 * <dt>x <dd> integer in hexadecimal
	 * <dt>o <dd> integer in octal
	 * <dt>s <dd> string
	 * <dt>c <dd> character
	 * </dl>
	 * </ul>
	 * @exception IllegalArgumentException if bad format
	 */
	public Format(String s) {
		width = 0;
		precision = -1;
		pre = "";
		post = "";
		leading_zeroes = false;
		show_plus = false;
		alternate = false;
		show_space = false;
		left_align = false;
		fmt = ' '; 
      
		int length = s.length();
		int parse_state = 0; 
		// 0 = prefix, 1 = flags, 2 = width, 3 = precision,
		// 4 = format, 5 = end
		int i = 0;
      
		while (parse_state == 0) {
			if (i >= length) parse_state = 5;
			else if (s.charAt(i) == '%') {
				if (i < length - 1) {
					if (s.charAt(i + 1) == '%') {
						pre += '%';
						i++;
					}
					else
						parse_state = 1;
				}
				else throw new java.lang.IllegalArgumentException();
			}
			else
				pre += s.charAt(i);
			i++;
		}
		while (parse_state == 1) {
			if (i >= length) parse_state = 5;
			else if (s.charAt(i) == ' ') show_space = true;
			else if (s.charAt(i) == '-') left_align = true; 
			else if (s.charAt(i) == '+') show_plus = true;
			else if (s.charAt(i) == '0') leading_zeroes = true;
			else if (s.charAt(i) == '#') alternate = true;
			else { parse_state = 2; i--; }
			i++;
		}      
		while (parse_state == 2) {
			if (i >= length) parse_state = 5;
			else if ('0' <= s.charAt(i) && s.charAt(i) <= '9') {
				width = width * 10 + s.charAt(i) - '0';
				i++;
			}
			else if (s.charAt(i) == '.') {
				parse_state = 3;
				precision = 0;
				i++;
			}
			else 
				parse_state = 4;            
		}
		while (parse_state == 3) {
			if (i >= length) parse_state = 5;
			else if ('0' <= s.charAt(i) && s.charAt(i) <= '9') {
				precision = precision * 10 + s.charAt(i) - '0';
				i++;
			}
			else 
				parse_state = 4;                  
		}
		if (parse_state == 4)  {
			if (i >= length) parse_state = 5;
			else fmt = s.charAt(i);
			i++;
		}
		if (i < length)
			post = s.substring(i, length);
	}      
               
	/** 
	 * Formats a double into a string (like sprintf in C)
	 * @param x the number to format
	 * @return the formatted string 
	 * @exception IllegalArgumentException if bad argument
	 */
   
	public String format(double x) {
		if(Double.isInfinite(x)){
			return "Infinity";
		}

		String r;
		if (precision < 0) precision = 6;
		int s = 1;
		if (x < 0) { x = -x; s = -1; }
		if (fmt == 'f')
			r = fixed_format(x);
		else if (fmt == 'e' || fmt == 'E' || fmt == 'g' || fmt == 'G')
			r = exp_format(x);
		else throw new java.lang.IllegalArgumentException();
      
		if(Double.isInfinite(x))
			return "Infinity";

		return pad(sign(s, r));
	}
   
	/** 
	 * Formats a long integer into a string (like sprintf in C)
	 * @param x the number to format
	 * @return the formatted string 
	 */
	
	public String format(long x) {
		String r; 
		int s = 0;
		if (fmt == 'd' || fmt == 'i') {
			if (x < 0)  {
				r = Long.toString(x).substring(1);
				s = -1; 
			}
			else  {
				r = Long.toString(x);
				s = 1;
			}
		}
		else if (fmt == 'o')
			r = convert(x, 3, 7, "01234567");
		else if (fmt == 'x')
			r = convert(x, 4, 15, "0123456789abcdef");
		else if (fmt == 'X')
			r = convert(x, 4, 15, "0123456789ABCDEF");
		else throw new java.lang.IllegalArgumentException();
         
		return pad(sign(s, r));
	}
   
	/** 
	 * Formats a character into a string (like sprintf in C)
	 * @param x the value to format
	 * @return the formatted string 
	 */
   
	private String format(char c) {
		if (fmt != 'c')
			throw new java.lang.IllegalArgumentException();
		String r = Character.toString(c);
		return pad(r);
	}
   
	/** 
	 * Formats a string into a larger string (like sprintf in C)
	 * @param x the value to format
	 * @return the formatted string 
	 */
   
	public String format(String s) {
		if (fmt != 's')
			throw new java.lang.IllegalArgumentException();
		if (precision >= 0 && precision < s.length()) 
			s = s.substring(0, precision);
		return pad(s);
	}

	private static String repeat(char c, int n) {
		if (n <= 0) return "";
		StringBuilder s = new StringBuilder(n);
		for (int i = 0; i < n; i++) s.append(c);
		return s.toString();
	}
	private static String convert(long x, int n, int m, String d) {
		if (x == 0) return "0";
		String r = "";
		while (x != 0) {
			r = d.charAt((int)(x & m)) + r;
			x >>>= n;
		}
		return r;
	}
	private String pad(String r) {
		String p = repeat(' ', width - r.length());
		if (left_align) return pre + r + p + post;
		else return pre + p + r + post;
	}
   
	private String sign(int s, String r) {
		String p = "";
		if (s < 0) p = "-"; 
		else if (s > 0) {
			if (show_plus) p = "+";
			else if (show_space) p = " ";
		}
		else {
			if (fmt == 'o' && alternate && r.length() > 0 && r.charAt(0) != '0') p = "0";
			else if (fmt == 'x' && alternate) p = "0x";
			else if (fmt == 'X' && alternate) p = "0X";
		}
		int w = 0;
		if (leading_zeroes) 
			w = width;
		else if ((fmt == 'd' || fmt == 'i' || fmt == 'x' || fmt == 'X' || fmt == 'o') 
				 && precision > 0) w = precision;
      
		return p + repeat('0', w - p.length() - r.length()) + r;
	}
   
	private String fixed_format(double d) {
		boolean removeTrailing
			= (fmt == 'G' || fmt == 'g') && !alternate;
		// remove trailing zeroes and decimal point
		if (d > 0x7FFFFFFFFFFFFFFFL) return exp_format(d);
		if (precision == 0) 
			return (long)(d + 0.5) + (removeTrailing ? "" : ".");
		long whole = (long)d;
		double fr = d - whole; // fractional part
		if (fr >= 1 || fr < 0) return exp_format(d);
		double factor = 1;
		StringBuilder leading_zeroes = new StringBuilder(precision);
		for (int i = 1; i <= precision && factor <= 0x7FFFFFFFFFFFFFFFL; i++)  {
			factor *= 10; 
			leading_zeroes.append('0');
		}
		long l = (long) (factor * fr + 0.5);
		if (l >= factor) { l = 0; whole++; } // CSH 10-25-97
      
		String z = leading_zeroes.toString() + l;
		z = "." + z.substring(z.length() - precision, z.length());
		if (removeTrailing) {
			int t = z.length() - 1;
			while (t >= 0 && z.charAt(t) == '0') t--;
			if (t >= 0 && z.charAt(t) == '.') t--;
			z = z.substring(0, t + 1);
		}
		return whole + z;
	}
	private String exp_format(double d) {
		StringBuilder f = new StringBuilder(16);
		int e = 0;
		double dd = d;
		double factor = 1;
		if (d != 0) {
			while (dd > 10) { e++; factor /= 10; dd /= 10; }
			while (dd < 1) { e--; factor *= 10; dd *= 10; }
		}
		if ((fmt == 'g' || fmt == 'G') && e >= -4 && e < precision) 
			return fixed_format(d);
      
		d *= factor;
		f.append(fixed_format(d));
      
		if (fmt == 'e' || fmt == 'g')
			f.append('e');
		else
			f.append('E');
		StringBuilder p = new StringBuilder("000");
		if (e >= 0)  {
			f.append('+');
			p.append(e);
		}
		else {
			f.append('-');
			p.append(-e);
		}
         
		return f + p.substring(p.length() - 3, p.length());
	}
   
	private int width;
	private int precision;
	private String pre;
	private String post;
	private boolean leading_zeroes;
	private boolean show_plus;
	private boolean alternate;
	private boolean show_space;
	private boolean left_align;
	private char fmt; // one of cdeEfgGiosxXos
}
