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

import java.awt.*;
import jclass.bwt.*;

class UI {
    // Convenience methods for BWT.

    /** Create a button. */
    public static JCButton button(String label, String command,
				  JCActionListener l){
	JCButton b = new JCButton(label);
	b.setActionCommand(command);
	if(l != null){
	    b.addActionListener(l);
	}

	return b;
    }

    /** Create a checkBox. */
    public static JCCheckbox checkbox(String label, String command,
				      JCItemListener l){
	JCCheckbox b = new JCCheckbox(label);
	b.setIndicator(JCCheckbox.INDICATOR_CHECK);
	b.setUserData(command);
	if(l != null){
	    b.addItemListener(l);
	}
	b.setInsets(new Insets(0, 0, 0, 0));

	return b;
    }

    /** Create a group box with nice insets. */
    public static JCGroupBox groupbox(String label){
	JCGroupBox gb = new JCGroupBox(label);
	gb.setInsets(new Insets(5, 5, 5, 5));

	return gb;
    }

    /** Create a spin box. */
    public static JCSpinBox spinbox(int columns,
				    int min, int max, int val, int dp,
				    int inc, JCSpinBoxListener sbl){
	JCSpinBox sb = new JCSpinBox(columns);
	sb.setMinimum(min);
	sb.setMaximum(max);
	sb.setDecimalPlaces(dp);
	sb.setIntValue(val);
	sb.setIncrement(inc);
	
	if(sbl != null){
	    sb.addSpinBoxListener(sbl);
	}

	sb.setBackground(Color.white);
	return sb;
    }
}
