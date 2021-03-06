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
import java.awt.event.*;
import jclass.bwt.*;

class ObjectControl extends Panel implements JCItemListener, JCActionListener, JCAdjustmentListener {
    /** The tmesh object we control. */
    private UserInterface userInterface = null;
    public Tmesh tmesh            = null;
    public JCCheckbox selected    = null;
    private JCCheckbox displayed  = null;
    private JCCheckbox backface   = null;
    private JCSlider transparency = null;
    private JCButton delete       = null;
    private JCButton edit         = null;
    private ColorButton colorButton  = null;

    /** Public constructor. */
    public ObjectControl(UserInterface ui, Tmesh tm){
	super();

	tmesh = tm;
	userInterface = ui;

	setLayout(new JCGridLayout(1, 0));

	delete = new JCButton("X");
	delete.addActionListener(this);
	add(delete);

	transparency = new JCSlider(JCSlider.HORIZONTAL, 255, 0, 255);
	transparency.setPreferredSize(60, BWTEnum.NOVALUE);
	transparency.setNumTicks(5);
	transparency.addAdjustmentListener(this);
	add(transparency);

	backface = new JCCheckbox();
	backface.setIndicator(JCCheckbox.INDICATOR_FILL);
	backface.setSelectColor(Color.white);
	backface.setUnselectColor(Color.black);
	backface.addItemListener(this);
	add(backface);

	ActionListener al = new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    ColorButton b = (ColorButton)e.getSource();
		    MoleculeRenderer mr = userInterface.moleculeViewer.getMoleculeRenderer();
		    if(!b.wasCancelled()){
			String command =
			    "object '" + tmesh.getName() +
			    "' color '" + b.getValue() + "';";
			mr.execute(command);
			mr.repaint();
		    }
		}
	    };

	colorButton = new ColorButton(Color.white, 16);
	colorButton.addActionListener(al);
	add(colorButton);

	edit = new JCButton("Edit");
	edit.addActionListener(this);
	add(edit);

	displayed = new JCCheckbox(tmesh.getName());
	displayed.setIndicator(JCCheckbox.INDICATOR_CHECK);
	displayed.setState(1);
	displayed.addItemListener(this);
	add(displayed);
    }

    /** Listen for events on the object. */
    public void itemStateChanged(JCItemEvent e){
	Object item = e.getSource();
	boolean handled = true;
	MoleculeRenderer mr = userInterface.moleculeViewer.getMoleculeRenderer();

	if(item == displayed){
	    String on = (displayed.getState() == 0 ? "off" : "on");
	    String command = "object display '" + tmesh.getName() + "' " + on + ";";
	    mr.execute(command);
	}else if(item == backface){
	    String on = (backface.getState() == 0 ? "off" : "on");
	    String command = "object '" + tmesh.getName() + "' backface " + on + ";";
	    mr.execute(command);
	}else{
	    handled = false;
	}

	if(handled){
	    userInterface.moleculeViewer.repaint();
	}
    }

    /** Adjustment changed on slider for instance. */
    public void adjustmentValueChanged(JCAdjustmentEvent e){
	Object item = e.getSource();

	if(item == transparency){
	    int t = transparency.getValue();
	    MoleculeRenderer mr = userInterface.moleculeViewer.getMoleculeRenderer();
	    
	    String command = "object '" + tmesh.getName() + "' transparency " + t + ";";
	    mr.execute(command);
	    userInterface.moleculeViewer.repaint();
	}
    }

    /** Handle actions. */
    public void actionPerformed(JCActionEvent e){
	Object item = e.getSource();
	boolean handled = true;
	MoleculeRenderer mr = userInterface.moleculeViewer.getMoleculeRenderer();

	if(item == delete){
	    userInterface.objectContainer.remove(this);
	    userInterface.objectContainer.repaint();
	    mr.renderer.objects.remove(tmesh);
	    String command = "object remove '" + tmesh.getName() + "';";
	    mr.execute(command);
	}else if(item == edit){
	    Frame frame = Util.getFrame(userInterface.userInterfaceFrame);

	    ObjectPropertyDialog opd =
		ObjectPropertyDialog.getInstance(frame, "Object Properties...", mr);

	    opd.setTmesh(tmesh);

	    Point location = edit.getLocationOnScreen();
	    Dimension size = edit.getSize();
	    astex.MoleculeViewer.showAt(opd,
					location.x + size.width,
					location.y + size.height);
	}else if(item == colorButton){
	    Point location = colorButton.getLocationOnScreen();
	    Dimension size = colorButton.getSize();
	    
	    String newColorString =
		userInterface. moleculeViewer.getColor(location.x + size.width,
						       location.y + size.height);
	    if(newColorString != null){
		int newColor = Color32.getColorFromName(newColorString);

		if(newColor != UserInterface.undefinedColor){
		    String color = Color32.format(newColor);
		    String command = "object '" + tmesh.getName() + "' color " + color + ";";
		    mr.execute(command);
		    colorButton.setForeground(new Color(newColor));
		    colorButton.setBackground(new Color(newColor));
		}
	    }

	}else{
	    handled = false;
	}

	if(handled){
	    userInterface.moleculeViewer.repaint();
	}
    }
}
