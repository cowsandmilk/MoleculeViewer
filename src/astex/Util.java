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

import java.awt.*;

public class Util {
    /** Array of timer start times. */
    private static long startTime[] = new long[10];

    /** Start the specified time. */
    public static void startTimer(int i){
	startTime[i] = System.currentTimeMillis();
    }

    /** Stop the current timer and return its delta in milliseconds. */
    public static void stopTimer(String s, int i){
        long now = System.currentTimeMillis();
	long delta = now - startTime[i];
	startTime[i] = now;
	FILE.out.print(s + " " + delta);
    }

    public static String replace(String s, String pattern, String replace){
        if ( pattern == null || pattern.length() == 0 || s == null) {
            return s;
        }
        
        final StringBuffer result = new StringBuffer(s.length() * 2);

        int start = 0;
        int old = 0;

        while((old = s.indexOf(pattern, start)) >= 0) {
            //grab a part of s which does not include pattern
            result.append(s.substring(start, old));
            //add replace to take place of pattern
            result.append(replace);
       
            //reset the start to just after the current match, to see
            //if there are any further matches
            start = old + pattern.length();
        }
        //the final chunk will go to the end of s
        result.append(s.substring(start));

        return result.toString();
    }

    /** Return the frame that contains the specified component. */
    public static Frame getFrame(Component c){
	Frame frame = null;

	while((c = c.getParent()) != null){
	    if(c instanceof Frame){
		frame = (Frame)c;
	    }
	}

	return frame;
    }
}