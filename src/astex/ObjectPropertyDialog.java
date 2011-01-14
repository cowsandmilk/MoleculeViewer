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
import java.awt.event.*;
import java.util.*;

import jclass.bwt.*;

public class ObjectPropertyDialog extends Dialog
    implements
	WindowListener, JCActionListener, JCSpinBoxListener {
    /** The only instance of an ObjectPropertyDialog. */
    private static ObjectPropertyDialog opd = null;

    /** The object that we are currently operating on. */
    private Tmesh object = null;
    
    /** The MoleculeRenderer object. */
    private MoleculeRenderer moleculeRenderer = null;

    /** Is the object active yet. */
    private boolean active = false;

    /** Create the instance of the opd. */
    public synchronized static ObjectPropertyDialog
	getInstance(Frame f, String label,
		    MoleculeRenderer mr){
	if(opd == null){
	    opd = new ObjectPropertyDialog(f, label, mr);
	}

	return opd;
    }

    /** Set the tmesh object that is being edited. */
    public void setTmesh(Tmesh tm){
	object = tm;
	
	setMinMax();
    }

    /**
     * Create a dialog that lets us edit the texture based
     * properties of a tmesh object.
     */
    private ObjectPropertyDialog(Frame f, String label,
				MoleculeRenderer mr){
	super(f, label, false);

	moleculeRenderer = mr;

	addWindowListener(this);

	createControls();
    }

    /** Which texture coordinate we are editing. */
    private JCCheckboxGroup textureCoordinate = null;
    
    private static final int textureValues[] = {
	0, 1
    };

    private static final String[] textureCoords= {
	"u", "v"
    };

    private JCCheckbox applyCharges = null;
    private JCCheckbox applyMlp     = null;

    private JCSpinBox uminSB        = null;
    private JCSpinBox vminSB        = null;
    private JCSpinBox umaxSB        = null;
    private JCSpinBox vmaxSB        = null;

    private Hashtable<JCButton, String> imageHash = new Hashtable<JCButton, String>(11);

    /** Create the controls for this property editor. */
    private void createControls(){
	textureCoordinate =
	    JCCheckbox.makeGroup(textureCoords, textureValues, true);
	textureCoordinate.setOrientation(JCCheckboxGroup.VERTICAL);
	textureCoordinate.setTitle("Coordinate");
	
	Layout.fill(this, textureCoordinate, 0, 0, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCGroupBox properties = UI.groupbox("Properties");

	JCButton electrostatic = new JCButton("Electrostatic");
	electrostatic.setActionCommand("electrostatic");
	electrostatic.addActionListener(this);

	Layout.fill(properties, electrostatic, 1, 1, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	applyCharges = new JCCheckbox("Charge");
	applyCharges.setState(1);
	applyCharges.setIndicator(JCCheckbox.INDICATOR_CHECK);

	Layout.fill(properties, applyCharges, 1, 2, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCButton mlp = new JCButton("Lipophilicity");
	mlp.setActionCommand("lipophilicity");
	mlp.addActionListener(this);

	Layout.fill(properties, mlp, 1, 3, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	applyMlp = new JCCheckbox("Contributions");
	applyMlp.setState(1);
	applyMlp.setIndicator(JCCheckbox.INDICATOR_CHECK);

	Layout.fill(properties, applyMlp, 1, 4, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	JCButton distance = new JCButton("Distance");
	distance.setActionCommand("distance");
	distance.addActionListener(this);

	Layout.fill(properties, distance, 1, 5, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton curvature = new JCButton("Curvature");
	curvature.setActionCommand("curvature");
	curvature.addActionListener(this);

	Layout.fill(properties, curvature, 1, 6, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton atomColors = new JCButton("Atom colors");
	atomColors.setActionCommand("atom_colors");
	atomColors.addActionListener(this);

	Layout.fill(properties, atomColors, 1, 7, 1, 1,
		    GridBagConstraints.HORIZONTAL);
	
	JCButton clipObject = new JCButton("Clip");
	clipObject.setActionCommand("clip_object");
	clipObject.addActionListener(this);

	Layout.fill(properties, clipObject, 1, 8, 1, 1,
		    GridBagConstraints.HORIZONTAL);

	Layout.fill(this, properties, 1, 0, 1, 2,
		    GridBagConstraints.VERTICAL);

	//Texture ranges
	JCGroupBox rangeGB = UI.groupbox("Ranges");

	JCLabel uminLabel = new JCLabel("umin");
	uminSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel umaxLabel = new JCLabel("umax");
	umaxSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel vminLabel = new JCLabel("vmin");
	vminSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	JCLabel vmaxLabel = new JCLabel("vmax");
	vmaxSB = UI.spinbox(5, -10000, 10000, 0, 2, 5, this);

	Layout.nofill(rangeGB, uminLabel, 0, 0);
	Layout.nofill(rangeGB, uminSB,    1, 0);
	Layout.nofill(rangeGB, umaxLabel, 2, 0);
	Layout.nofill(rangeGB, umaxSB,    3, 0);
	Layout.nofill(rangeGB, vminLabel, 0, 1);
	Layout.nofill(rangeGB, vminSB,    1, 1);
	Layout.nofill(rangeGB, vmaxLabel, 2, 1);
	Layout.nofill(rangeGB, vmaxSB,    3, 1);

	Layout.fill(this, rangeGB, 0, 2, 2, 1,
		    GridBagConstraints.HORIZONTAL);

	JCGroupBox textureGB = UI.groupbox("Textures");

	Layout.fill(this, textureGB, 0, 1, 1, 1,
		    GridBagConstraints.BOTH);

	JCScrolledWindow textureList = new JCScrolledWindow();
	textureList.setScrollbarDisplay(JCScrolledWindow.DISPLAY_VERTICAL_ONLY);

	JCContainer textureContainer = new JCContainer();
	textureContainer.setLayout(new JCGridLayout(0, 3));

	textureList.add(textureContainer);

	Layout.fill(textureGB, textureList, 1, 1, 5, 1, GridBagConstraints.BOTH);

	JCActionListener tal = new JCActionListener(){
		public void actionPerformed(JCActionEvent e){
		    String texture = imageHash.get(e.getSource());
		    String textureName = Settings.getString("config", texture);
		    String textureImage = Settings.getString("config", texture + ".image");
		    String command =
			"texture load '" + textureName + "' '" + textureImage + "';";
		    command += 
			"object '" + object.getName() + "' texture '" + textureName +"';";

		    System.out.println("command " + command);
		    moleculeRenderer.execute(command);
		    moleculeRenderer.repaint();
		}
	    };
		

	for(int t = 0; t < 10000; t++){
	    String texture = "texture." + t;
	    String textureName = Settings.getString("config", texture);

	    if(textureName == null){
		break;
	    }

	    String smallImageTag = "texture." + t + ".small";
	    String smallImageName = Settings.getString("config", smallImageTag);

	    if(smallImageName != null){
		Image smallImage = Texture.loadImage(smallImageName);

		if(smallImage != null){

		    MediaTracker mt = new MediaTracker(this);
		    
		    mt.addImage(smallImage, 1);
	
		    try {
			mt.waitForAll();
		    }catch(InterruptedException e){
			Log.error("interrupted loading " + smallImageName);
		    }
		    
		    JCButton ib = new JCButton(smallImage);

		    ib.setPreferredSize(16, 16);
		    ib.setInsets(new Insets(0,0,0,0));
		    ib.addActionListener(tal);

		    imageHash.put(ib, texture);

		    textureContainer.add(ib);
		}else{
		    Log.error("couldn't load image defined by " + smallImageTag);
		}
	    }else{
		Log.error("no small image defined by " + texture);
	    }
	}

    }

    public void spinBoxChangeBegin(JCSpinBoxEvent e){
    }

    public void spinBoxChangeEnd(JCSpinBoxEvent e){
	if(!active) return;

	boolean handled = true;
	Object source   = e.getSource();
	String command  = "";

	if(source == uminSB || source == umaxSB ||
	   source == vminSB || source == vmaxSB){
	    command += "object '" +object.getName() + "' texture ";
	    if(source == uminSB || source == umaxSB){
		command +=
		    "urange " + uminSB.getValue() + " " + umaxSB.getValue();
	    }else{
		command +=
		    "vrange " + vminSB.getValue() + " " + vmaxSB.getValue();
	    }

	    command += ";";
	}else{
	    handled = false;
	}

	if(handled){
	    moleculeRenderer.execute(command);
	    moleculeRenderer.moleculeViewer.dirtyRepaint();
	}
    }

    /** Handle actions on the user interface. */
    public void actionPerformed(JCActionEvent e){
	if(!active) return;

	String actionCommand  = e.getActionCommand();
	boolean handled       = true;

	String name           = "'" + object.getName() + "'";
	int tc                = textureCoordinate.getValue();
	String texCoord       = textureCoords[tc];
	boolean needTexture   = true;
	String command        = "";

	if(actionCommand.equals("distance")){
	    command += "object " + name + " texture distance " +
		       texCoord + " default;";
	    command += "object " + name + " texture " +
		       texCoord + "div 8.0;";

	    if(tc == 0){
		command += "texture load white 'white.jpg';";
		command += "object " + name + " texture white;";
	    }

	    needTexture = false;

	}else if(actionCommand.equals("electrostatic")){
	    if(applyCharges.getState() == 1){
		command += "run 'charge.properties';";
	    }

	    command += "object " + name + 
		       " texture electrostatic " +
		       texCoord + " 12.0 default;";
	    command += "texture load rwb 'images/textures/rwb.jpg';";
	    command += "object " + name + " texture rwb;";
	    needTexture = false;
	}else if(actionCommand.equals("lipophilicity")){
	    if(applyMlp.getState() == 1){
		command += "run 'lipophilicity.properties';";
	    }

	    command += "object " + name + 
		       " texture lipophilicity " +
		       texCoord + " 7.0 default;";
	    
	    command += "texture load molcad 'images/textures/red2blue.jpg';";

	    command += "object " + name + " texture molcad;";
	    needTexture = false;
	}else if(actionCommand.equals("curvature")){
	    command += "object " + name + " texture curvature " +
		texCoord + " 6 default;";
	    command += "object " + name + " texture " +
		texCoord + "div 1.0;";
	    command += "texture load rwb 'images/textures/rwb.jpg';";
	    command += "object " + name + " texture rwb;";
	    needTexture = false;
	}else if(actionCommand.equals("atom_colors")){
	    command += "object " + name + " -map { current };";
	}else if(actionCommand.equals("clip_object")){
	    command += "object " + name + " clip " + texCoord + ";";
	    needTexture = false;
	}else{
	    handled = false;
	}

	if(needTexture){
	    moleculeRenderer.execute("texture close simple;");
	    moleculeRenderer.execute("object " + name + " texture close;");
	}

	if(handled){
	    moleculeRenderer.execute(command);

	    setMinMax();

	    moleculeRenderer.moleculeViewer.dirtyRepaint();
	    return;
	}
    }

    /** Set the min max values for this object. */
    private void setMinMax(){
	active = false;

	// add 0.5 to make rounding better when converting to int
	double umin = object.getInverseTexture(Tmesh.UTexture, 0.0);
	uminSB.setIntValue((int)(0.5 + 100.*umin));
	double umax = object.getInverseTexture(Tmesh.UTexture, 1.0);
	umaxSB.setIntValue((int)(0.5 + 100.*umax));
	double vmin = object.getInverseTexture(Tmesh.VTexture, 0.0);
	vminSB.setIntValue((int)(0.5 + 100.*vmin));
	double vmax = object.getInverseTexture(Tmesh.VTexture, 1.0);
	vmaxSB.setIntValue((int)(0.5 + 100.*vmax));

	active = true;
    }

    /* Implementation of WindowListener. */
    public void windowClosing(WindowEvent e){
	setVisible(false);
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }
}
