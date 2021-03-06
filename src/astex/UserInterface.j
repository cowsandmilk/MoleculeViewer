package astex;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.event.*;

import astex.generic.*;

import jclass.bwt.*;
import jclass.util.*;

import nanoxml.*;

class UserInterface implements MouseListener, JCOutlinerListener,
				      JCActionListener,
				      MoleculeRendererListener,
				      RendererEventListener,
				      JCSpinBoxListener, JCAdjustmentListener,
				      WindowListener, ItemListener  {
    private static JCOutlinerNodeStyle style = new JCOutlinerNodeStyle();
    
    MoleculeViewer moleculeViewer = null;
    MoleculeRenderer moleculeRenderer = null;

    private static String selectOptions[] = {
	"Select",
	"Append",
	"Exclude"
    };

    private static final int SelectMode = 1;
    private static final int AppendMode = 2;
    private static final int ExcludeMode = 3;

    private static int selectValues[] = {
	SelectMode,
	AppendMode,
	ExcludeMode
    };

    private JCCheckboxGroup selectMode = null;
    private JCCheckboxGroup textureCoordinate = null;

    private static final int none = GridBagConstraints.NONE;
    private static final int both = GridBagConstraints.BOTH;
    private static final int horizontal = GridBagConstraints.HORIZONTAL;

    public Dialog userInterfaceFrame = null;

    public UserInterface(MoleculeViewer mv){

	moleculeViewer = mv;
	moleculeRenderer = moleculeViewer.getMoleculeRenderer();

	Frame mainFrame = Util.getFrame(mv);
	userInterfaceFrame = new Dialog(mainFrame, "Control panel...", false);

	userInterfaceFrame.setBackground(new Color(212, 208, 200));
	JCContainer masterPanel = new JCContainer();

	userInterfaceFrame.add(masterPanel);

	masterPanel.setLayout(new GridBagLayout());

	JCTabManager tab_manager = new JCTabManager();

	Layout.fill(masterPanel, tab_manager, 0, 0);

	JCContainer panel = createSelectionContainer();
	tab_manager.addPage(new JCString("Select"), panel);

	panel = createGenerateContainer();
	tab_manager.addPage(new JCString("Generate"), panel);

	panel = createModifyContainer();
	tab_manager.addPage(new JCString("Modify"), panel);

	panel = createLightsContainer();
	tab_manager.addPage(new JCString("Lights"), panel);

	JCContainer view = createViewInterface();

	Layout.fill(masterPanel, view, 0, 1);

	userInterfaceFrame.setBounds(806, 0, 120, 400);
	userInterfaceFrame.pack();
	userInterfaceFrame.setVisible(true);

	Renderer r = moleculeRenderer.renderer;

	moleculeRenderer.addMoleculeRendererListener(this);
	
	r.rendererEventListeners.add(this);

	userInterfaceFrame.addWindowListener(this);
    }

    private JCCheckbox fog = null;
    private JCCheckbox aa = null;
    private JCCheckbox shadows = null;

    private JCCheckbox solidLabels = null;

    JCContainer createViewInterface(){
	JCGroupBox view = UI.groupbox("View controls");

	view.setLayout(new GridBagLayout());

	JCButton center = new JCButton("Centre");
	center.setActionCommand("center");
	center.addActionListener(this);

	Layout.fill(view, center, 0, 0, GridBagConstraints.HORIZONTAL);

	JCButton reset = new JCButton("Reset");
	reset.setActionCommand("reset");
	reset.addActionListener(this);

	Layout.fill(view, reset, 0, 1, GridBagConstraints.HORIZONTAL);

	fog = new JCCheckbox("Fog");
	fog.setIndicator(JCCheckbox.INDICATOR_CHECK);
	fog.addItemListener(this);
	fog.setBorderStyle(JCLabel.SHADOW_FRAME_OUT);
	Layout.fill(view, fog, 0, 2, GridBagConstraints.HORIZONTAL);

	aa = new JCCheckbox("AA");
	aa.setIndicator(JCCheckbox.INDICATOR_CHECK);
	aa.addItemListener(this);
	aa.setBorderStyle(JCLabel.SHADOW_FRAME_OUT);
	Layout.fill(view, aa, 0, 3, GridBagConstraints.HORIZONTAL);

	shadows = new JCCheckbox("Shadows");
	shadows.setIndicator(JCCheckbox.INDICATOR_CHECK);
	shadows.addItemListener(this);
	shadows.setBorderStyle(JCLabel.SHADOW_FRAME_OUT);
	Layout.fill(view, shadows, 0, 4, GridBagConstraints.HORIZONTAL);

	JCContainer clipContainer = new JCContainer();
	clipContainer.setLayout(new GridBagLayout());

	// front clipping plane controls
	JCLabel frontLabel = new JCLabel("Front");
	frontValue = new JCLabel();

	int initialFrontClip = 0;
	initialFrontClip = 
	    (int)(moleculeRenderer.renderer.getFrontClip() * 10);

	frontValue.setText(f.format(initialFrontClip * 0.1));
	frontValue.setPreferredSize(40, BWTEnum.NOVALUE);
	frontClip = new JCSlider(JCSlider.HORIZONTAL,
				 initialFrontClip, 5, 1000);
	frontClip.setPreferredSize(120, BWTEnum.NOVALUE);
	frontClip.setNumTicks(10);

	frontClip.addAdjustmentListener(this);

	Layout.fill(clipContainer, frontLabel, 0, 1, none);
	Layout.fill(clipContainer, frontClip,  1, 1, horizontal);
	Layout.fill(clipContainer, frontValue, 2, 1, none);

	// back clipping plane controls
	JCLabel backLabel = new JCLabel("Back");
	backValue = new JCLabel();

	int initialBackClip = 0;
	initialBackClip = 
	    -(int)(moleculeRenderer.renderer.getBackClip() * 10);

	backValue.setText(f.format(initialBackClip * 0.1));
	backValue.setPreferredSize(40, BWTEnum.NOVALUE);

	backClip = new JCSlider(JCSlider.HORIZONTAL,
				initialBackClip, 5, 1000);
	backClip.setPreferredSize(120, BWTEnum.NOVALUE);
	backClip.setNumTicks(10);

	backClip.addAdjustmentListener(this);

	Layout.fill(clipContainer, backLabel, 0, 2, none);
	Layout.fill(clipContainer, backClip,  1, 2, horizontal);
	Layout.fill(clipContainer, backValue, 2, 2, none);

	Layout.fill(view, clipContainer, 1, 0, 1, 2, both);

	return view;
    }

    private JCTextField surfaceName = null;
    private JCSpinBox surfaceQuality = null;
    private JCSpinBox probeRadius = null;
    private JCButton surfaceBackgroundColorButton = null;

    private int surfaceBackgroundColor = Color32.white;

    private JCTextField schematicName = null;
    private JCSpinBox schematicQuality = null;

    private JCOutlinerFolderNode structureBrowser = null;
    private JCOutliner outliner = null;

    private Checkbox lightOnOff = null;
    private JCSlider diffuseSlider = null;
    private JCSlider specularSlider = null;
    private JCSlider phongSlider = null;
    private JCSlider ambientSlider = null;
    private Checkbox cartoonCB = null;
    private JCSlider normalSlider = null;

    private JCContainer createLightsContainer(){
	JCContainer panel = new JCContainer();
	JCContainer lightPanel = new JCContainer();
	JCContainer controlsPanel = new JCContainer();
	Layout.nofill(panel, lightPanel, 0, 0);
	Layout.nofill(panel, controlsPanel, 1, 0);
	
	LightCanvas lc = new LightCanvas(moleculeViewer);

	Layout.nofill(lightPanel, lc, 0, 0);

	lightOnOff = new Checkbox("On/off");
	lightOnOff.addItemListener(this);

	Layout.nofill(controlsPanel, lightOnOff, 0, 0);

	diffuseSlider = new JCSlider(BWTEnum.HORIZONTAL, 255, 0, 255);
	diffuseSlider.setPreferredSize(80, BWTEnum.NOVALUE);
	diffuseSlider.setNumTicks(10);
	diffuseSlider.addAdjustmentListener(this);
	Layout.nofill(controlsPanel, diffuseSlider, 0, 1);
	Layout.nofill(controlsPanel, new JCLabel("Diffuse"), 1, 1);

	specularSlider = new JCSlider(BWTEnum.HORIZONTAL, 255, 0, 255);
	specularSlider.setPreferredSize(80, BWTEnum.NOVALUE);
	specularSlider.setNumTicks(10);
	specularSlider.addAdjustmentListener(this);
	Layout.nofill(controlsPanel, specularSlider, 0, 2);
	Layout.nofill(controlsPanel, new JCLabel("Specular"), 1, 2);

	phongSlider = new JCSlider(BWTEnum.HORIZONTAL, 50, 0, 100);
	phongSlider.setPreferredSize(80, BWTEnum.NOVALUE);
	phongSlider.setNumTicks(10);
	phongSlider.addAdjustmentListener(this);
	Layout.nofill(controlsPanel, phongSlider, 0, 4);
	Layout.nofill(controlsPanel, new JCLabel("Phong"), 1, 4);

	ambientSlider = new JCSlider(BWTEnum.HORIZONTAL,
				     Color32.getIntensity(moleculeRenderer.renderer.getAmbient()),
				     0, 255);
	ambientSlider.setPreferredSize(80, BWTEnum.NOVALUE);
	ambientSlider.setNumTicks(10);
	ambientSlider.addAdjustmentListener(this);
	Layout.nofill(controlsPanel, ambientSlider, 0, 5);
	Layout.nofill(controlsPanel, new JCLabel("Ambient"), 1, 5);

	cartoonCB = new Checkbox("Cartoon");
	cartoonCB.addItemListener(this);

	if(moleculeRenderer.renderer.getLightingModel() == Renderer.LightingModel.CartoonLightingModel){
	    cartoonCB.setState(true);
	}

	Layout.nofill(controlsPanel, cartoonCB, 0, 6);

	normalSlider = new JCSlider(BWTEnum.HORIZONTAL, 80, 0, 250);
	normalSlider.setPreferredSize(80, BWTEnum.NOVALUE);
	normalSlider.setNumTicks(10);
	normalSlider.addAdjustmentListener(this);
	Layout.nofill(controlsPanel, normalSlider, 0, 7);
	Layout.nofill(controlsPanel, new JCLabel("Normal"), 1, 7);

	return panel;
    }

    private int activeLight = -1;
    private Light light = null;

    public void setActiveLight(int al, Light l){
	activeLight = al;
	light = l;

	if(light.on){
	    lightOnOff.setState(true);
	}else{
	    lightOnOff.setState(false);
	}

	diffuseSlider.setValue(Color32.getIntensity(light.diffuse));
	specularSlider.setValue(Color32.getIntensity(light.specular));
	phongSlider.setValue((int)light.power);
    }

    /** Schematic controls. */
    private JCSpinBox ribbonWidth         = null;
    private JCSpinBox ribbonMinWidth      = null;
    private JCSpinBox ribbonThickness     = null;
    private JCCheckbox ribbonCylinders    = null;
    private JCCheckbox ribbonEllipse      = null;
    private JCCheckbox allTube            = null;
    private JCSpinBox arrowWidth          = null;
    private JCSpinBox arrowHeadWidth      = null;
    private JCSpinBox arrowThickness      = null;
    private JCSpinBox arrowSmoothing      = null;
    private JCSpinBox tubeWidth           = null;
    private JCSpinBox tubeTaper           = null;
    private JCSpinBox tubeSmoothing       = null;

    /** Label controls. */
    private JCCheckboxGroup labelsGB      = null;

    private JCCheckbox justifyCBS[][]     = new JCCheckbox[3][3];
    private JCCheckbox threedLabels       = null;
    private JCSpinBox stringPoints        = null;
    private JCSpinBox stringSize          = null;
    private JCSpinBox stringRadius        = null;
    private JCSpinBox stringXOff          = null;
    private JCSpinBox stringYOff          = null;
    private JCSpinBox stringZOff          = null;
    private JCTextField currentLabel      = null;
    private ColorButton stringColorButton = null;
    
    /** Distance controls. */
    private JCSpinBox dmaxSB              = null;
    private JCSpinBox contactSB           = null;
    private JCSpinBox dashSB              = null;
    private JCSpinBox gapSB               = null;
    private JCSpinBox radiusSB            = null;
    private Choice    modeChoice          = null;
    private TextField nameTF              = null;
    private TextField labelTF             = null;
    private ColorButton distanceColorButton  = null;

    private JCContainer createGenerateContainer(){
	JCContainer panel = new JCContainer();

	JCSpinBoxListener sbl = new JCSpinBoxListener() {
		public void spinBoxChangeBegin(JCSpinBoxEvent e){
		}
		public void spinBoxChangeEnd(JCSpinBoxEvent e){
		    buildSchematic();
		}
	    }
	    ;	

	JCItemListener cbl = new JCItemListener() {
		public void itemStateChanged(JCItemEvent e){
		    buildSchematic();
		}
	    }
	    ;

	JCGroupBox surfaceGB = UI.groupbox("Surface");

	int row = 0;

	row++;

	surfaceName = new JCTextField("surface_1", 7);
	surfaceName.setBackground(Color.white);
	Layout.nofill(surfaceGB, new JCLabel("Name"),     0, row);
	Layout.nofill(surfaceGB, surfaceName,              1, row);

	probeRadius = UI.spinbox(3, 0, 1000, 150, 2, 10, null);

	Layout.nofill(surfaceGB, new JCLabel("Probe"),   2, row);
	Layout.nofill(surfaceGB, probeRadius,            3, row);
	
	surfaceQuality = UI.spinbox(1, 1, 4, 1, 0, 1, null);
	    
	Layout.nofill(surfaceGB, new JCLabel("Quality"), 4, row);
	Layout.nofill(surfaceGB, surfaceQuality,         5, row);

	JCButton buildSurface = new JCButton("Build");
	buildSurface.setActionCommand("build_surface");
	buildSurface.addActionListener(this);

	Layout.nofill(surfaceGB, buildSurface,           6, row);

	row++;

	Layout.fill(panel, surfaceGB, 0, 0, GridBagConstraints.HORIZONTAL);

	// Schematics

	JCGroupBox schematicGB = UI.groupbox("Schematic");

	row = 0;

	schematicName = new JCTextField("schematic_1", -1);
	schematicName.setBackground(Color.white);

	Layout.nofill(schematicGB, new JCLabel("Name"),     0, row);
	Layout.constrain(schematicGB, schematicName,          1, row,
			 3, 1, GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST, 0., 0.0);


	schematicQuality = UI.spinbox(1, 1, 5, 1, 0, 1, sbl);
	Layout.nofill(schematicGB, new JCLabel("Quality"),   4, row);
	Layout.nofill(schematicGB, schematicQuality,         5, row);

	JCButton buildSchematic = new JCButton("Build");
	buildSchematic.setActionCommand("build_schematic");
	buildSchematic.addActionListener(this);

	Layout.nofill(schematicGB, buildSchematic,           6, row);

	row++;

	Layout.nofill(schematicGB, new JCLabel("Width"),     1, row);
	Layout.nofill(schematicGB, new JCLabel("Thick"),     2, row);
	Layout.nofill(schematicGB, new JCLabel("Taper"),     3, row);
	Layout.nofill(schematicGB, new JCLabel("Smooth"),    4, row);

	row++;

	Layout.nofill(schematicGB, new JCLabel("Helix"),   0, row);

	ribbonWidth  = UI.spinbox(3, 100, 500, 220, 2, 20, sbl);
	Layout.nofill(schematicGB, ribbonWidth,              1, row);

	ribbonThickness = UI.spinbox(3, 10, 100, 15, 2, 2, sbl);
	Layout.nofill(schematicGB, ribbonThickness,          2, row);

	ribbonMinWidth  = UI.spinbox(3, 10, 220, 40, 2, 1, sbl);
	Layout.nofill(schematicGB, ribbonMinWidth,           3, row);

	row++;

	ribbonEllipse = UI.checkbox("Oval", "ribbonEllipse", cbl);
	Layout.nofill(schematicGB, ribbonEllipse,           1, row);

	row++;

	ribbonCylinders = UI.checkbox("Edges", "ribbonCylinders", cbl);
	Layout.nofill(schematicGB, ribbonCylinders,         1, row);

	row++;

	//arrows
	Layout.nofill(schematicGB, new JCLabel("Arrows"),   0, row);

	arrowWidth  = UI.spinbox(3, 100, 500, 220, 2, 20, sbl);
	Layout.nofill(schematicGB, arrowWidth,              1, row);

	arrowThickness = UI.spinbox(3, 10, 100, 50, 2, 1, sbl);
	Layout.nofill(schematicGB, arrowThickness,          2, row);

	arrowHeadWidth = UI.spinbox(3, 100, 1000, 360, 2, 20, sbl);
	Layout.nofill(schematicGB, arrowHeadWidth,          3, row);

	arrowSmoothing = UI.spinbox(1, 0, 12, 3, 0, 1, sbl);
	Layout.nofill(schematicGB, arrowSmoothing,          4, row);

	row++;

	//coil
	Layout.nofill(schematicGB, new JCLabel("Coil"),     0, row);

	tubeWidth  = UI.spinbox(3, 0, 500, 20, 2, 2, sbl);
	Layout.nofill(schematicGB, tubeWidth,               1, row);

	tubeTaper  = UI.spinbox(3, 0, 500, 20, 2, 2, sbl);
	Layout.nofill(schematicGB, tubeTaper,               3, row);

	tubeSmoothing = UI.spinbox(1, 0, 12, 1, 0, 1, sbl);
	Layout.nofill(schematicGB, tubeSmoothing,           4, row);

	row++;

	allTube = UI.checkbox("Tube", "alltube", cbl);
	Layout.nofill(schematicGB, allTube,                 1, row);
	
	row++;
	    
	Layout.fill(panel, schematicGB, 0, 1, GridBagConstraints.HORIZONTAL);

	// Labels.
	labelsGB = new JCCheckboxGroup("Labels");

	JCLabel valueLabel = new JCLabel("Value");
	Layout.nofill(labelsGB, valueLabel, 0, 0);
	
	Choice labelChoices = new Choice();

	for(int i = 0; i < 1000; i++){
	    String labelEntry = Settings.getString("config", "label."+i);

	    if(labelEntry == null){
		break;
	    }

	    labelChoices.add(labelEntry);
	}

	Layout.nofill(labelsGB, labelChoices, 1, 0);

	ItemListener lil = new ItemListener(){
		public void itemStateChanged(ItemEvent e){
		    String s = (String)e.getItem();
		    if("clear".equals(s)){
			moleculeRenderer.execute("label clear current;");
			moleculeRenderer.repaint();
		    }else{
			currentLabel.insert(s, currentLabel.getCaretPosition());
		    }
		}
	    };

	labelChoices.addItemListener(lil);

	currentLabel = new JCTextField("%f%r", -1);

	JCActionListener cal = new JCActionListener(){
		public void actionPerformed(JCActionEvent e){
		    addLabels();
		}
	    };

	currentLabel.addActionListener(cal);

	Layout.constrain(labelsGB, currentLabel,          2, 0,
			 3, 1, GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST, 0., 0.0);


	ActionListener sal = new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    addLabels();
		}
	    };

	stringColorButton = new ColorButton(Color.white, 16);
	stringColorButton.addActionListener(sal);

	Layout.nofill(labelsGB, stringColorButton, 5, 0);

	JCButton applyLabels = UI.button("Apply", "apply_labels", cal);

	Layout.nofill(labelsGB, applyLabels, 6, 0);

	Panel labelPanel = new Panel();
	
	String v[] = { "t", "v", "b" };
	String h[] = { "l", "h", "r" };

	JCItemListener il = new JCItemListener(){
		public void itemStateChanged(JCItemEvent e){
		    if(e.getSource() == threedLabels){
			if(threedLabels.getState() == 1){
			    stringPoints.setEnabled(false);
			    stringSize.setEnabled(true);
			    stringRadius.setEnabled(true);
			}else{
			    stringPoints.setEnabled(true);
			    stringSize.setEnabled(false);
			    stringRadius.setEnabled(false);
			}
		    }
		    addLabels();
		}
	    };
	
	for(int j = 0; j < 3; j++){
	    for(int i = 0; i < 3; i++){
		JCCheckbox jcb = UI.checkbox(null, v[j] + h[i], il);
		justifyCBS[i][j] = jcb;
		jcb.setCheckboxGroup(labelsGB);
		jcb.setPreferredSize(10, 10);
		jcb.setInsets(new Insets(0, 0, 0, 0));
		jcb.setHighlightThickness(0);
		Layout.nofill(labelPanel, jcb, i, j);
		if(i == 0 && j == 2){
		    jcb.setState(BWTEnum.ON);
		}
	    }
	}

	JCLabel justifyLabel = new JCLabel("Justify");
	Layout.nofill(labelsGB, justifyLabel, 0, 1);
	
	Layout.nofill(labelsGB, labelPanel,   1, 1);
	
	labelsGB.setRadioBehavior(true);

	JCLabel xoff = new JCLabel("XYZ");
	Layout.nofill(labelsGB, xoff,         2, 1);

	JCSpinBoxListener lsbl = new JCSpinBoxListener() {
		public void spinBoxChangeBegin(JCSpinBoxEvent e){
		}
		public void spinBoxChangeEnd(JCSpinBoxEvent e){
		    addLabels();
		}
	    }
	    ;	

	stringXOff = UI.spinbox(4, -1000, 1000, 0, 2, 10, lsbl);
	stringYOff = UI.spinbox(4, -1000, 1000, 0, 2, 10, lsbl);
	stringZOff = UI.spinbox(4, -1000, 1000, 0, 2, 10, lsbl);

	Layout.nofill(labelsGB, stringXOff,   3, 1);
	Layout.nofill(labelsGB, stringYOff,   4, 1);
	Layout.constrain(labelsGB, stringZOff,          5, 1,
			 2, 1, GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST, 0., 0.0);


	JCLabel sizeLabel = new JCLabel("Size");
	Layout.nofill(labelsGB, sizeLabel,    0, 2);
	
	stringPoints = UI.spinbox(3, 1, 28, 12, 0, 4, lsbl);

	Layout.nofill(labelsGB, stringPoints, 1, 2);

	threedLabels = UI.checkbox("3d", null, il);

	Layout.nofill(labelsGB, threedLabels, 2, 2);

	stringSize = UI.spinbox(4, 1, 10000, 50, 2, 2, lsbl);
	stringSize.setEnabled(false);

	Layout.nofill(labelsGB, stringSize,   3, 2);

	stringRadius = UI.spinbox(4, 1, 1000, 5, 2, 1, lsbl);
	stringRadius.setEnabled(false);

	Layout.nofill(labelsGB, stringRadius, 4, 2);


	Layout.fill(panel, labelsGB, 0, 2, GridBagConstraints.HORIZONTAL);

	// Distances

	JCGroupBox distanceGB = UI.groupbox("Distances");

	JCLabel modeLabel = new JCLabel("Mode");
	Layout.nofill(distanceGB, modeLabel,    0, 0);

	modeChoice = new Choice();
	modeChoice.add("pairs");
	modeChoice.add("nbpairs");
	modeChoice.add("bumps");
	modeChoice.add("centroid");

	JCSpinBoxListener dspl = new JCSpinBoxListener(){
		public void spinBoxChangeBegin(JCSpinBoxEvent e){
		}
		public void spinBoxChangeEnd(JCSpinBoxEvent e){
		    addDistances();
		}
	    };


	Layout.nofill(distanceGB, modeChoice,   1, 0);

	JCLabel dmaxLabel = new JCLabel("Dmax");
	Layout.nofill(distanceGB, dmaxLabel,    2, 0);

	dmaxSB = UI.spinbox(4, 0, 10000, 500, 2, 10, dspl);
	Layout.nofill(distanceGB, dmaxSB,       3, 0);

	JCLabel contactLabel = new JCLabel("Contact");
	Layout.nofill(distanceGB, contactLabel, 4, 0);

	contactSB = UI.spinbox(4, -10000, 10000, 50, 2, 5, dspl);
	Layout.nofill(distanceGB, contactSB,    5, 0);

	Layout.constrain(distanceGB, contactSB,          5, 0,
			 2, 1, GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST, 0., 0.0);

	JCLabel dashLabel = new JCLabel("Dash");
	Layout.nofill(distanceGB, dashLabel,   0, 1);

	dashSB = UI.spinbox(4, 0, 1000, 20, 2, 1, dspl);
	Layout.nofill(distanceGB, dashSB,      1, 1);

	JCLabel gapLabel = new JCLabel("Gap");
	Layout.nofill(distanceGB, gapLabel,    2, 1);

	gapSB = UI.spinbox(4, 0, 1000, 20, 2, 1, dspl);
	Layout.nofill(distanceGB, gapSB,       3, 1);

	JCLabel radiusLabel = new JCLabel("Radius");
	Layout.nofill(distanceGB, radiusLabel, 4, 1);

	radiusSB = UI.spinbox(4, 0, 100, 2, 2, 1, dspl);
	Layout.constrain(distanceGB, radiusSB,          5, 1,
			 2, 1, GridBagConstraints.HORIZONTAL,
			 GridBagConstraints.WEST, 0., 0.0);


	JCLabel nameLabel = new JCLabel("Name");
	Layout.nofill(distanceGB, nameLabel,   0, 2);

	ActionListener al = new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    addDistances();
		}
	    };

	nameTF = new TextField("set1", 5);
	nameTF.addActionListener(al);
	Layout.nofill(distanceGB, nameTF,        1, 2);

	JCLabel labelLabel = new JCLabel("Label");
	Layout.nofill(distanceGB, labelLabel,  2, 2);

	labelTF = new TextField("%.2fA", 3);
	labelTF.addActionListener(al);
	Layout.nofill(distanceGB, labelTF,       3, 2);

	JCLabel distanceColorLabel = new JCLabel("Color");
	Layout.nofill(distanceGB, distanceColorLabel,    4, 2);
	
	JCActionListener dal = new JCActionListener(){
		public void actionPerformed(JCActionEvent e){
		    addDistances();
		}
	    };

	distanceColorButton = new ColorButton(Color.white, 16);
	distanceColorButton.addActionListener(al);

	Layout.nofill(distanceGB, distanceColorButton, 5, 2);

	JCButton applyDistances = UI.button("Apply", "apply_distances", dal);

	Layout.nofill(distanceGB, applyDistances, 6, 2);

	Layout.fill(panel, distanceGB, 0, 3, GridBagConstraints.BOTH);

	// pack out the rest of the space.
	Layout.fill(panel, new Panel(), 0, 4, GridBagConstraints.BOTH);

	return panel;
    }

    /** Central method for adding distances in the user interface. */
    private void addDistances(){
	StringBuffer command = new StringBuffer(100);
	command.append("distance -delete '").append(nameTF.getText())
		.append("'; distance");

	String mode = modeChoice.getSelectedItem();
	command.append(" -mode ").append(mode)
	       .append(" -dmax ").append(dmaxSB.getValue())
	       .append(" -contact ").append(contactSB.getValue())
	       .append(" -on ").append(dashSB.getValue())
	       .append(" -off ").append(gapSB.getValue())
	       .append(" -radius ").append(radiusSB.getValue())
	       .append(" -name '").append(nameTF.getText()).append("' -colour '")
	       .append(distanceColorButton.getValue()).append("' -from { peek 0 }");

	if(!"bumps".equals(mode)){
	    command.append(" -to { peek 1 }");
	}

	String prefix = buildLabelPrefix();
	String format = labelTF.getText();

	command.append(" -format '").append(prefix).append(format).append("';");

	moleculeRenderer.execute(command.toString());
	moleculeRenderer.repaint();
    }

    /** Build label prefix. */
    private String buildLabelPrefix(){
	StringBuilder prefix = new StringBuilder(100);
	prefix.append('<');

	String justifyString = "bl";
	JCCheckbox selectedCB = null;

	for(int i = 0; i < 3; i++){
	    for(int j = 0; j < 3; j++){
		JCCheckbox cb = justifyCBS[i][j];
		if(cb.getState() == 1){
		    selectedCB = cb;
		    break;
		}
	    }
	}

	if(selectedCB != null){
	    justifyString = (String)selectedCB.getUserData();
	    prefix.append("justify=").append(justifyString).append(",");
	}

	if(threedLabels.getState() == 1){
	    prefix.append("3d=true,");
	}

	prefix.append("points=").append(stringPoints.getValue()).append(",");

	prefix.append("size=").append(stringSize.getValue()).append(",");

	prefix.append("radius=").append(stringRadius.getValue()).append(",");

	prefix.append("xoff=").append(stringXOff.getValue()).append(',')
	      .append("yoff=").append(stringYOff.getValue()).append(',')
	      .append("colour=").append(stringColorButton.getValue()).append(',')
	      .append("zoff=").append(stringZOff.getValue()).append('>');

	return prefix.toString();
    }

    /** Add labels according to current settings. */
    private void addLabels(){
	String prefix = buildLabelPrefix();
	String label = currentLabel.getText();

	String command = "label '" + prefix + label + "' current;";

	moleculeRenderer.execute(command);
	moleculeRenderer.repaint();
    }

    private void addMolecule(Molecule mol){
	String name = mol.getName();
	int len = name.length();
	if(len > 19){
	    int trimPoint = name.lastIndexOf('\\');
	    if(trimPoint == -1){
		trimPoint = name.lastIndexOf('/');
	    }
	    
	    if(trimPoint != -1){
		name = ".../" + name.substring(trimPoint+1, len);
	    }else{
		name = "..." + name.substring(len - 16, len);
	    }
	}
	JCOutlinerFolderNode molFolder =
	    new JCOutlinerFolderNode(null, BWTEnum.FOLDER_CLOSED, name);

	// create copy of style for this node
	JCOutlinerNodeStyle newStyle = new JCOutlinerNodeStyle(style);

	int molType = mol.getMoleculeType();
	
	if(molType == Molecule.NormalMolecule){
	    newStyle.setForeground(Color.black);
	}else{
	    newStyle.setForeground(Color.gray);
	}

	molFolder.setStyle(newStyle);
	molFolder.setUserData(mol);
	structureBrowser.addNode(molFolder);

	JCOutlinerComponent outlinerComponent = outliner.getOutliner();
	JCOutlinerFolderNode rootNode =
	    (JCOutlinerFolderNode)outlinerComponent.getRootNode();
	outliner.folderChanged(rootNode);

	if(outliner != null){
	    outliner.repaint();
	}
    }

    private void removeMolecule(Molecule mol){
	JCOutlinerComponent outlinerComponent =
	    outliner.getOutliner();
	JCOutlinerFolderNode rootNode =
	    (JCOutlinerFolderNode)outlinerComponent.getRootNode();
	
	JCVector children = rootNode.getChildren();
	JCOutlinerNode nodeToRemove = null;
	if(children != null){

	    for(int i = 0; i < children.size(); i++){
		JCOutlinerNode child = (JCOutlinerNode)children.at(i);
		if(child.getUserData() == mol){
		    nodeToRemove = child;
		    System.out.println("seen molecule to remove");
		    break;
		}
	    }
	}

	if(nodeToRemove != null){
	    rootNode.removeChild(nodeToRemove);
	    outliner.folderChanged(rootNode);
	}else{
	    Log.warn("couldn't find molecule that was removed");
	}
    }

    private JCSpinBox lineWidth = null;
    private JCSpinBox cylinderRadius = null;
    private JCSpinBox ballRadius = null;
    private JCSpinBox stickRadius = null;
    private JCSpinBox vdwRadius = null;
    private JCSpinBox transp = null;
    private JCButton  atomColorButton = null;
    private Choice labelChoices = null;

    private Image rainbowImage = null;

    private JCContainer createSelectionContainer(){
	JCContainer panel = new JCContainer();

	panel.setLayout(new GridBagLayout());
	
	selectMode =
	    JCCheckbox.makeGroup(selectOptions, selectValues, true);
	selectMode.setOrientation(JCCheckboxGroup.HORIZONTAL);
	selectMode.setTitle("Mode");

	Layout.fill(panel, selectMode, 0, 0, 2, 1, horizontal);
	
	structureBrowser =
	    new JCOutlinerFolderNode(null, "Molecules");

	outliner = new JCOutliner();
	JCOutlinerComponent component =
	    new JCOutlinerComponent(structureBrowser);
	outliner.setOutliner(component);

	style.setShortcut(true);
	style.setFolderOpenIcon(null);
	style.setFolderClosedIcon(null);
	style.setItemIcon(null);
	style.setFont(new Font("courier", Font.PLAIN, 12));

	for(int i = 0; i < moleculeRenderer.getMoleculeCount(); i++){
	    Molecule mol = moleculeRenderer.getMolecule(i);
	    addMolecule(mol);
	}

	structureBrowser.setStyle(style);

	component.addMouseListener(this);

	outliner.setBackground(Color.white);

	outliner.setPreferredSize(140, 300);
	outliner.setRootVisible(true);
	outliner.addItemListener(this);

	Layout.fill(panel, outliner, 0, 1, both);

	JCOutliner builtins = createBuiltins("builtins.properties");

	Layout.fill(panel, builtins, 1, 1, both);

	JCGroupBox displayStyle = UI.groupbox("Display as");

	displayStyle.setLayout(new GridLayout());

	String styles[] = {
	    "Lines", "Cylinders", "Sticks", "Spheres"
	};

	displayStyle.setLayout(new JCAlignLayout(3, 10, 0));

	for(int i = 0; i < styles.length; i++){
	    String lower = styles[i].toLowerCase();
	    JCLabel label = new JCLabel(styles[i]);
	    JCButton plus =
		onOffButton(this, "+", "display " + lower + " on current;");
	    JCButton minus =
		onOffButton(this, "-", "display " + lower + " off current;");
	    Layout.fill(displayStyle, label, 0, i, none);
	    Layout.fill(displayStyle, plus, 1, i, none);
	    Layout.fill(displayStyle, minus, 2, i, none);
	}

	JCLabel lineWidthLabel = new JCLabel("Width");
	lineWidth = UI.spinbox(2, 1, 3, 1, 0, 1, this);
	Layout.fill(displayStyle, lineWidthLabel, 3, 0, none);
	Layout.fill(displayStyle, lineWidth,      4, 0, none);

	JCLabel cylinderRadiusLabel = new JCLabel("Radius");
	cylinderRadius = UI.spinbox(3, 1, 300, 15, 2, 1, this);
	Layout.fill(displayStyle, cylinderRadiusLabel, 3, 1, none);
	Layout.fill(displayStyle, cylinderRadius,      4, 1, none);

	JCLabel ballRadiusLabel = new JCLabel("Ball");
	ballRadius = UI.spinbox(3, 1, 300, 30, 2, 1, this);
	Layout.fill(displayStyle, ballRadiusLabel, 3, 2, none);
	Layout.fill(displayStyle, ballRadius,      4, 2, none);

	JCLabel stickRadiusLabel = new JCLabel("Stick");
	stickRadius = UI.spinbox(3, 1, 300, 15, 2, 1, this);
	Layout.fill(displayStyle, stickRadiusLabel, 5, 2, none);
	Layout.fill(displayStyle, stickRadius,      6, 2, none);

	JCLabel vdwRadiusLabel = new JCLabel("Radius");
	vdwRadius = UI.spinbox(3, 1, 1000, 150, 2, 1, this);
	Layout.fill(displayStyle, vdwRadiusLabel, 3, 3, none);
	Layout.fill(displayStyle, vdwRadius,      4, 3, none);

	JCLabel transpLabel = new JCLabel("Transp");
	transp = UI.spinbox(3, 0, 255, 255, 0, 10, this);
	Layout.fill(displayStyle, transpLabel, 5, 3, none);
	Layout.fill(displayStyle, transp,      6, 3, none);

	JCLabel colorLabel = new JCLabel("Colour");

	JCImageCreator im = 
	    new JCImageCreator(colorLabel, rainbow[0].length(), rainbow.length);
	im.setColor('r', Color.red);
	im.setColor('o', Color.orange);
	im.setColor('y', Color.yellow);
	im.setColor('g', Color.green);
	im.setColor('b', Color.blue);
	rainbowImage = im.create(rainbow);

	atomColorButton = new JCButton(rainbowImage);
	atomColorButton.setInsets(new Insets(0, 0, 0, 0));
	atomColorButton.setPreferredSize(16, 20);

	atomColorButton.addActionListener(this);
	Layout.fill(displayStyle, colorLabel,           0, 4, none);
	Layout.fill(displayStyle, atomColorButton,      1, 4, none);

	JCLabel label = new JCLabel("Labels");

	labelChoices = new Choice();

	for(int i = 0; i < 1000; i++){
	    String labelEntry = Settings.getString("config", "label."+i);

	    if(labelEntry == null){
		break;
	    }

	    labelChoices.add(labelEntry);
	}

	labelChoices.setBackground(Color.white);
	labelChoices.setSize(30, -1);
	labelChoices.addItemListener(this);

	solidLabels = new JCCheckbox("Solid");
	solidLabels.setIndicator(JCCheckbox.INDICATOR_CHECK);
	solidLabels.addItemListener(this);
	solidLabels.setBorderStyle(JCLabel.SHADOW_FRAME_OUT);

	Layout.fill(displayStyle, label,           3, 4, none);
	Layout.fill(displayStyle, labelChoices,    4, 4, none);
	Layout.fill(displayStyle, solidLabels,     6, 4, horizontal);

	Layout.fill(panel, displayStyle, 0, 2, 2, 1, both);

	return panel;
    }

    private JCScrolledWindow objectList = null;
    public JCContainer objectContainer = null;

    private JCSpinBox distanceMax = null;
    private JCSlider frontClip = null;
    private JCLabel frontValue = null;
    private JCSlider backClip = null;
    private JCLabel backValue = null;

    private JCContainer createModifyContainer(){
	JCContainer panel = new JCContainer();

	panel.setLayout(new GridBagLayout());

	objectList = new JCScrolledWindow();
	objectList.setScrollbarDisplay(JCScrolledWindow.DISPLAY_VERTICAL_ONLY);
	objectContainer = new JCContainer();
	objectContainer.setLayout(new JCGridLayout(0, 1));

	objectList.add(objectContainer);

	Layout.fill(panel, objectList, 1, 1, 5, 1, GridBagConstraints.BOTH);

	Renderer r = moleculeViewer.getMoleculeRenderer().renderer;

	for(Tmesh tmesh : r.objects){

	    ObjectControl oc = new ObjectControl(this, tmesh);

	    objectContainer.add(oc);
	}
	return panel;
    }

    private JCOutliner createBuiltins(String properties){
	FILE file = FILE.open(properties);
	Reader reader = null;
        XMLElement xml = new XMLElement();

        try {
	    InputStreamReader isr =
		new InputStreamReader(file.getInputStream());
            reader = (Reader)new BufferedReader(isr);
            xml.parseFromReader(reader);
        } catch(Exception e){
            System.out.println("Couldn't open or parse xml file " + e);

	    return null;
        }

	JCOutlinerFolderNode root =
	    new JCOutlinerFolderNode(null, "Builtins");
	
	if(!"builtins".equals(xml.getName())){
	    System.out.println(properties + " has invalid format");
	    return null;
	}

	buildBuiltins(root, xml);

	root.setStyle(style);

	JCOutliner outliner = new JCOutliner();
	JCOutlinerComponent component =
	    new JCOutlinerComponent(root);
	outliner.setOutliner(component);

	component.addMouseListener(this);
	outliner.setBackground(Color.white);

	outliner.setPreferredSize(140, 150);
	outliner.setRootVisible(true);
	outliner.addItemListener(this);

	return outliner;
    }

    /* Read the defintions of the commands from xml fie. */
    private void buildBuiltins(JCOutlinerFolderNode parent, XMLElement xml){
	Enumeration objects = xml.enumerateChildren();

	while(objects.hasMoreElements()){
	    XMLElement child = (XMLElement)objects.nextElement();
	    if("node".equals(child.getName())){
		String label = child.getStringAttribute("LABEL");
		String selection = child.getStringAttribute("SELECTION");

		JCOutlinerNode node =
		    new JCOutlinerNode(null, parent);
		node.setLabel(label);
		node.setUserData(selection);
		node.setStyle(style);
	    }else if("folder".equals(child.getName())){
		String label = child.getStringAttribute("LABEL");

		JCOutlinerFolderNode newParent =
		    new JCOutlinerFolderNode(null, BWTEnum.FOLDER_CLOSED,
					     label);
		parent.addNode(newParent);
		newParent.setStyle(style);

		buildBuiltins(newParent, child);
	    }
	}
    }

    private static Font onOffFont = null;

    private static JCButton onOffButton(UserInterface ui,
				       String s, String action){
	if(onOffFont == null){
	    onOffFont = new Font("TimesRoman", Font.BOLD, 16);
	}

	JCButton button = new JCButton(s);
	button.setFont(onOffFont);
	button.setInsets(new Insets(0,0,0,0));
	button.setPreferredSize(new Dimension(16,16));
	button.setActionCommand(action);
	button.addActionListener(ui);

	return button;
    }

    public void outlinerFolderStateChangeBegin(JCOutlinerEvent ev){
	JCOutlinerFolderNode folder = (JCOutlinerFolderNode)ev.getNode();
	JCOutliner outliner = (JCOutliner)ev.getSource();
	if(ev.getNewState() == BWTEnum.FOLDER_OPEN_ALL){
	    if(folder.getChildren() != null){
		return;
	    }
	    Object userData = folder.getUserData();
	    if(userData instanceof Molecule){
		Molecule mol = (Molecule)userData;
		for(int c = 0; c < mol.getChainCount(); c++){
		    Chain chain = mol.getChain(c);
		    String chainName = chain.getName();
		    if(" ".equals(chainName)){
			chainName = "_";
		    }
		    JCOutlinerFolderNode chainFolder =
			new JCOutlinerFolderNode(null,
						 BWTEnum.FOLDER_CLOSED,
						 "Chain " + chainName);
		    chainFolder.setStyle(style);
		    folder.addNode(chainFolder);
		    chainFolder.setUserData(chain);
		}
	    }else if(userData instanceof Chain){
		Chain chain = (Chain)userData;
		for(int rr = 0;  rr < chain.getResidueCount(); rr++){
		    Residue res = chain.getResidue(rr);
		    String resName = res.getName() + " " + res.getNumber() +
			res.getInsertionCode();
		    JCOutlinerFolderNode resFolder =
			new JCOutlinerFolderNode(null, BWTEnum.FOLDER_CLOSED,
						 resName);
		    folder.addNode(resFolder);
		    resFolder.setUserData(res);
		    resFolder.setStyle(style);
		}
	    }else if(userData instanceof Residue){
		Residue res = (Residue)userData;
		for(int a = 0;  a < res.getAtomCount(); a++){
		    Atom atom = res.getAtom(a);
		    JCOutlinerNode atomFolder =
			new JCOutlinerNode(null, folder);
		    atomFolder.setLabel(atom.getAtomLabel());
		    atomFolder.setUserData(atom);
		    atomFolder.setStyle(style);
		}
	    }
	}

	outliner.folderChanged(folder);
    }

    public void outlinerFolderStateChangeEnd(JCOutlinerEvent ev) {
    }

    public void outlinerNodeSelectBegin(JCOutlinerEvent ev) {
    }

    public void outlinerNodeSelectEnd(JCOutlinerEvent ev) {
    }

    public void itemStateChanged(ItemEvent ev) {
	if(ev.getSource() == labelChoices){
	    String labelFormat = labelChoices.getSelectedItem();

	    if(!"clear".equals(labelFormat)){
		labelFormat = "'" + labelFormat + "'";
	    }

	    moleculeRenderer.execute("label " + labelFormat + " current;");

	}else if(ev.getSource() == cartoonCB){
	    if(!cartoonCB.getState()){
		moleculeRenderer.execute("view -lightingmodel normal;");
	    }else{
		moleculeRenderer.execute("view -lightingmodel cartoon;");
	    }
	}else if(ev.getSource() == lightOnOff){
	    if(!lightOnOff.getState()){
		moleculeRenderer.execute("light " + activeLight + " -on false;");
	    }else{
		moleculeRenderer.execute("light " + activeLight + " -on true;");
	    }
	}

	moleculeViewer.dirtyRepaint();
    }

    public void itemStateChanged(JCItemEvent ev) {
	boolean handled = true;

	if(ev.getSource() == fog){
	    if(fog.getState() == 0){
		moleculeRenderer.execute("view -fog false;");
	    }else{
		moleculeRenderer.execute("view -fog true;");
	    }
	}else if(ev.getSource() == solidLabels){
	    if(solidLabels.getState() == 0){
		moleculeRenderer.execute("view -solidfonts false;");
	    }else{
		moleculeRenderer.execute("view -solidfonts true;");
	    }
	}else if(ev.getSource() == aa){
	    if(aa.getState() == 0){
		moleculeRenderer.renderer.setAntiAlias(false);
	    }else{
		moleculeRenderer.renderer.setAntiAlias(true);
	    }
	}else if(ev.getSource() == shadows){
	    if(shadows.getState() == 0){
		moleculeRenderer.execute("view -shadows false -realspheres false;");
	    }else{
		moleculeRenderer.execute("view -shadows true -realspheres true;");
	    }
	}else{
	    handled = false;
	}

	if(handled){
	    moleculeViewer.dirtyRepaint();
	    return;
	}
    }
    
    public static final int undefinedColor = 0xff000000;

    private Dialog colorChooserDialog = null;
    private ColorChooser colorChooser = null;

    private int getColor(){
	Frame mainFrame = Util.getFrame(moleculeViewer);

	if(colorChooserDialog == null){
	    colorChooserDialog = new Dialog(mainFrame, true);

	    colorChooser = new ColorChooser(colorChooserDialog);
	    colorChooserDialog.add(colorChooser);
	}

	colorChooserDialog.pack();
	colorChooserDialog.setVisible(true);

	if(colorChooser.accept){
	    return colorChooser.rgb;
	}
	return undefinedColor;
    }

    Format hexFormat = new Format("0x%06x");

    /** Handle actions on the user interface. */
    public void actionPerformed(JCActionEvent e){
	String command = e.getActionCommand();
	boolean handled = true;
	MoleculeRenderer mr =
	    moleculeViewer.getMoleculeRenderer();

	if(e.getSource() == labelChoices){
	    System.out.println("label changed");
	}

	if(e.getSource() == atomColorButton){
	    int newColor = getColor();

	    if(newColor != undefinedColor){
		String s = hexFormat.format(newColor & 0xffffff);

		mr.execute("color '" + s + "' current;");
	    }
	}else if(e.getSource() == surfaceBackgroundColorButton){
	    int newColor = getColor();

	    if(newColor != undefinedColor){
		surfaceBackgroundColor = newColor;
	    }
	}else if("build_surface".equals(command)){
	    double r = FILE.readDouble(probeRadius.getValue());
	    String name = surfaceName.getText();

	    String colorString =
		hexFormat.format(surfaceBackgroundColor & 0xffffff);

	    mr.execute("surface -probe " + r + " -solid true " +
		       name + " " + colorString + " current;");
	}else if("build_schematic".equals(command)){
	    buildSchematic();
	    
	}else if("center".equals(command)){
	    mr.execute("center default;");
	}else if("reset".equals(command)){
	    mr.resetView();
	}else if(!command.startsWith("display")){
	    List<Tmesh> selectedObjects = getSelectedObjects();

	    if(selectedObjects == null){
		handled = false;
	    }
	}else{
	    handled = false;
	}

	if(handled){
	    moleculeViewer.dirtyRepaint();
	    return;
	}

	if(command.startsWith("display")){
	    mr.execute(command);

	    moleculeViewer.dirtyRepaint();
	}
    }

    /** Build the schematic using all the options. */
    private void buildSchematic(){
	String name = schematicName.getText();
	StringBuilder command = new StringBuilder(100);
	if(allTube.getState() == 0){
	    command.append("secstruc current; ");
	}
	command.append("schematic -name ").append(name)
	       .append(" -quality ").append(schematicQuality.getValue())
	       .append(" -ribbonwidth ").append(ribbonWidth.getValue())
	       .append(" -ribbonthickness ").append(ribbonThickness.getValue())
	       .append(" -ribbonminwidth ").append(ribbonMinWidth.getValue())
	       .append(" -arrowwidth ").append(arrowWidth.getValue())
	       .append(" -arrowheadwidth ").append(arrowHeadWidth.getValue())
	       .append(" -arrowthickness ").append(arrowThickness.getValue())
	       .append(" -arrowsmoothing ").append(arrowSmoothing.getValue())
	       .append(" -tuberadius ").append(tubeWidth.getValue())
	       .append(" -tubetaperradius ").append(tubeTaper.getValue())
	       .append(" -tubesmoothing ").append(tubeSmoothing.getValue())
	       .append(" -tubetaper true ")
	       .append(" -ribboncylinders ")
	       .append((ribbonCylinders.getState() == 1) ? "true" : "false")
	       .append(" -ribbonellipse ")
	       .append((ribbonEllipse.getState() == 1) ? "true" : "false")
	       .append(" -alltube ")
	       .append((allTube.getState() == 1) ? "true" : "false")
	       .append(" default and aminoacid;");

	moleculeRenderer.execute(command.toString());
	moleculeViewer.dirtyRepaint();
    }

    private List<Tmesh> selectedObjects = new ArrayList<Tmesh>();
    
    private List<Tmesh> getSelectedObjects(){
	Component components[] = objectContainer.getComponents();
	selectedObjects.clear();
	
	for(int i = 0; i < components.length; i++){
	    if(components[i] instanceof ObjectControl){
		ObjectControl oc = (ObjectControl) components[i];
		if(oc.selected.getState() == 1){
		    selectedObjects.add(oc.tmesh);
		}
	    }
	}

	return selectedObjects;
    }

    /** Handle events in the renderer. */
    public boolean handleRendererEvent(RendererEvent re){
	if(re.getType() == RendererEvent.Type.ObjectAdded){
	    Tmesh tm = (Tmesh)re.getItem();

	    ObjectControl oc = new ObjectControl(this, tm);

	    objectContainer.add(oc);
	    objectContainer.doLayout();
	}else if(re.getType() == RendererEvent.Type.ObjectRemoved){
	    Tmesh tm = (Tmesh)re.getItem();

	    System.out.println("remove " + tm);

	    Component ocs[] = objectContainer.getComponents();

	    for(int i = 0; i < ocs.length; i++){
		ObjectControl oc = (ObjectControl)ocs[i];
		if(oc.tmesh == tm){
		    // it was this one that was deleted.
		    objectContainer.remove(oc);
		    objectContainer.doLayout();
		}
	    }

	}else if(re.getType() == RendererEvent.Type.FrontClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = d.doubleValue();
		
		frontClip.setValue((int)(val * 10), false);
		frontValue.setText(f.format(val));
	    }
	}else if(re.getType() == RendererEvent.Type.BackClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = -d.doubleValue();
		
		backClip.setValue((int)(val * 10), false);
		backValue.setText(f.format(val));
	    }
	}

	return false;
    }

    public void spinBoxChangeBegin(JCSpinBoxEvent e){
    }

    public void spinBoxChangeEnd(JCSpinBoxEvent e){
	boolean handled = true;
	Object source = e.getSource();

	if(source == distanceMax){

	    List<Tmesh> selectedObjects = getSelectedObjects();
	    if(selectedObjects != null){
		    
		for(Tmesh tm : selectedObjects){
		    String name = "'" + tm.getName() + "'";
		    int tc = textureCoordinate.getValue();
		    String texCoord = null;
		    if(tc == 0){
			texCoord = "u";
		    }else{
			texCoord = "v";
		    }
			
		    double d = FILE.readDouble(distanceMax.getValue());

		    moleculeRenderer.execute("object " + name + " texture distance "
					     + texCoord + " default;");
		    moleculeRenderer.execute("object " + name + " texture " +
					     texCoord + "div " + d +";");
		    System.out.println("object " + name + " texture " +
				       texCoord + "div " + d +";");
		}
	    }
	}else if(source == lineWidth){
	    int lw = lineWidth.getIntValue();
	    moleculeRenderer.execute("bond_width " + lw + " current;");
	}else if(source == cylinderRadius){
	    String cr = cylinderRadius.getValue();
	    moleculeRenderer.execute("cylinder_radius " + cr + " current;");

	}else if(source == stickRadius){
	    String cr = stickRadius.getValue();
	    moleculeRenderer.execute("stick_radius " + cr + " current;");

	}else if(source == ballRadius){
	    String cr = ballRadius.getValue();
	    moleculeRenderer.execute("ball_radius " + cr + " current;");

	}else if(source == vdwRadius){
	    String cr = vdwRadius.getValue();
	    moleculeRenderer.execute("vdw " + cr + " current;");

	}else if(source == transp){
	    String cr = transp.getValue();
	    moleculeRenderer.execute("transparency " + cr + " current;");

	}else{
	    handled = false;
	}

	if(handled){
	    moleculeViewer.dirtyRepaint();
	}
    }

    public void genericAdded(MoleculeRenderer renderer, Generic generic){
    }

    public void genericRemoved(MoleculeRenderer renderer, Generic generic){
    }

    /** A molecule was added. */
    public void moleculeAdded(MoleculeRenderer renderer, Molecule molecule){
	addMolecule(molecule);
    }

    /** A molecule was removed. */
    public void moleculeRemoved(MoleculeRenderer renderer, Molecule molecule){
	removeMolecule(molecule);
    }

    /** A map was added. */
    public void mapAdded(MoleculeRenderer renderer, Map map){
    }

    /** A map was removed. */
    public void mapRemoved(MoleculeRenderer renderer, Map map){
    }

    /** An atom was selected. */
    public void atomSelected(MoleculeRenderer renderer, Atom atom){
    }

    private Format f = new Format("%4.1f");

    public void adjustmentValueChanged(JCAdjustmentEvent e){
	Object o = e.getSource();

	if(o == frontClip){
	    int value = frontClip.getValue();
	    double val = value * 0.1;
	    String s = f.format(val);
	    frontValue.setText(s);
	    moleculeRenderer.renderer.setFrontClip(val, false);
	}else if(o == backClip){
	    int value = backClip.getValue();
	    double val = -value * 0.1;
	    String s = f.format(val);
	    backValue.setText(s);
	    moleculeRenderer.renderer.setBackClip(val, false);
	}else if(o == phongSlider){
	    int value = phongSlider.getValue();
	    moleculeRenderer.execute("light " + activeLight + " -phongpower " + value + ";");
	}else if(o == ambientSlider){
	    int value = ambientSlider.getValue();
	    moleculeRenderer.execute("view -ambient "+ value + ";");
	}else if(o == diffuseSlider){
	    int value = diffuseSlider.getValue();
	    moleculeRenderer.execute("light " + activeLight + " -diffuseint " + value + ";");
	}else if(o == specularSlider){
	    int value = specularSlider.getValue();
	    moleculeRenderer.execute("light " + activeLight + " -specularint " + value + ";");
	}else if(o == normalSlider){
	    int value = normalSlider.getValue();
	    double v = value * 0.001;
	    moleculeRenderer.execute("view -normalcutoff " + v + ";");
	}
	moleculeViewer.dirtyRepaint();
    }

    private static final String[] rainbow = {
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        "oooooooooooooooo",
        "oooooooooooooooo",
        "oooooooooooooooo",
        "yyyyyyyyyyyyyyyy",
        "yyyyyyyyyyyyyyyy",
        "yyyyyyyyyyyyyyyy",
        "gggggggggggggggg",
        "gggggggggggggggg",
        "gggggggggggggggg",
        "bbbbbbbbbbbbbbbb",
        "bbbbbbbbbbbbbbbb",
        "bbbbbbbbbbbbbbbb",
    };

    /* Implementation of WindowListener. */
    public void windowClosing(WindowEvent e){
	Window w = e.getWindow();
	w.setVisible(false);
	//w.dispose();
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }

    /**
     * Override the behaviour of the outliner component.
     * All clicks are translated to events here.
     * This means that selected objects can be picked
     * again to trigger their event.
     *
     * This does not seem to be possible in the default
     * implemenation of JCOutliner et al.
     */
    public void mouseReleased(MouseEvent ev){
	if(ev.getSource() instanceof JCOutlinerComponent){
	    JCOutlinerComponent oc = (JCOutlinerComponent)ev.getSource();

	    Event e = new Event(oc, Event.MOUSE_UP, null);
	    e.x = ev.getX();
	    e.y = ev.getY();
	    
	    JCOutlinerNode node = oc.eventToNode(e);

	    if(!oc.eventInShortcut(e, node)){
		Object userData = node.getUserData();
		
		int mode = selectMode.getValue();

		if(ev.isShiftDown()){
		    mode = AppendMode;
		}else if(ev.isControlDown()){
		    mode = ExcludeMode;
		}
		
		if((ev.getModifiers() & MouseEvent.BUTTON1_MASK) == 0){
		    // pressed a button other than button 1
		    if(userData instanceof Molecule){
			Molecule mol = (Molecule)userData;
			// 2 indicates to toggle display state.
			mol.setDisplayed(2);

			JCOutlinerNodeStyle currentStyle = node.getStyle();
			
			if(mol.getDisplayed()){
			    currentStyle.setForeground(Color.black);
			}else{
			    currentStyle.setForeground(Color.gray);
			}

			node.setStyle(currentStyle);
		    }

		}else{
		    if(userData instanceof Selectable){
			Selectable sel = (Selectable)userData;
			String command = buildSelectionCommand(sel, mode);
			moleculeViewer.getMoleculeRenderer().execute(command);
		    }else if(userData instanceof String &&
			     userData != null){
			String command = null;
			if(mode == SelectMode){
			    command = "select " + userData;
			}else if(mode == AppendMode){
			    command = "append " + userData;
			}else if(mode == ExcludeMode){
			    command = "exclude " + userData;
			}

			command += ";";

			moleculeViewer.getMoleculeRenderer().execute(command);
		    }
		}
		moleculeViewer.dirtyRepaint();
	    }
	}
    }

    public void mouseEntered(MouseEvent ev){
    }

    public void mouseExited(MouseEvent ev){
    }

    public void mousePressed(MouseEvent ev){
    }

    public void mouseClicked(MouseEvent ev){
    }

    /** Build a string that will perform a selection operation. */
    private String buildSelectionCommand(Selectable sel, int mode){
	String prefix = null;

	if(mode == SelectMode){
	    prefix = "select ";
	}else if(mode == AppendMode){
	    prefix = "append ";
	}else if(mode == ExcludeMode){
	    prefix = "exclude ";
	}

	String command = prefix + sel.selectStatement() +";";

	return command;
    }
}
