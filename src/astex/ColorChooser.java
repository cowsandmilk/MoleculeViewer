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
import jclass.bwt.*;
import java.awt.event.*;

class ColorChooser extends Panel
    implements JCActionListener,  MouseMotionListener,  MouseListener,
	       JCSpinBoxListener, ActionListener {

    private Dialog dialog;

    private ColorSpace cs;
    private JCSpinBox red;
    private JCSpinBox green;
    private JCSpinBox blue;
    private TextField hexText;
    private Panel colorSample;

    private JCButton ok;
    private JCButton cancel;

    public boolean accept = false;

    public ColorChooser(Dialog d){
	dialog = d;

	JCContainer spinboxContainer = new JCContainer();

	cs = new ColorSpace();
	cs.addMouseListener(this);
	cs.addMouseMotionListener(this);

	Layout.fill(this, cs, 0, 0, GridBagConstraints.BOTH);

	JCLabel redLabel = new JCLabel("Red");
	red = new JCSpinBox(3);
	red.setAutoArrowDisable(false);
	red.setMinimum(0);
	red.setMaximum(255);

	JCLabel greenLabel = new JCLabel("Green");
	green = new JCSpinBox(3);
	green.setMinimum(0);
	green.setMaximum(255);

	JCLabel blueLabel = new JCLabel("Blue");
	blue = new JCSpinBox(3);
	blue.setMinimum(0);
	blue.setMaximum(255);


	Layout.fill(spinboxContainer, redLabel, 0, 0,
		    GridBagConstraints.NONE);
	Layout.fill(spinboxContainer, red, 1, 0,
		    GridBagConstraints.NONE);
	
	Layout.fill(spinboxContainer, greenLabel, 0, 1,
		    GridBagConstraints.NONE);
	Layout.fill(spinboxContainer, green, 1, 1,
		    GridBagConstraints.NONE);
	
	Layout.fill(spinboxContainer, blueLabel, 0, 2,
		    GridBagConstraints.NONE);
	Layout.fill(spinboxContainer, blue, 1, 2,
		    GridBagConstraints.NONE);
	
	JCLabel thisColor = new JCLabel("New");
	colorSample = new Panel();

	// common colours

	JCContainer commonContainer = new JCContainer();

	String commonColors[] = {
	    "red", "orange", "brown", "yellow", "green", "blue",
	    "cyan", "magenta", "purple",
	    "white", "lightgrey", "grey", "darkgrey", "black"
	};

	for(int i = 0; i < commonColors.length; i++){
	    JCButton b = new JCButton();
	    b.setPreferredSize(2, 10);
	    b.setBackground(new Color(Color32.getColorFromName(commonColors[i])));
	    b.addActionListener(this);
	    Layout.fill(commonContainer, b, i, 0, GridBagConstraints.NONE);
	}

	Layout.fill(this, commonContainer, 0, 1, GridBagConstraints.HORIZONTAL);

	JCLabel hexLabel = new JCLabel("Hex");
	hexText = new TextField("");
	hexText.setColumns(6);
	hexText.setFont(new Font("courier", Font.PLAIN, 9));
	hexText.addActionListener(this);

	Layout.fill(spinboxContainer, hexLabel, 0, 4,
		    GridBagConstraints.HORIZONTAL);
	Layout.fill(spinboxContainer, hexText, 1, 4,
		    GridBagConstraints.HORIZONTAL);

	Layout.fill(spinboxContainer, thisColor, 0, 5,
		    GridBagConstraints.HORIZONTAL);
	Layout.fill(spinboxContainer, colorSample, 1, 5,
		    GridBagConstraints.HORIZONTAL);

	Layout.fill(this, spinboxContainer, 1, 0, GridBagConstraints.NONE);

	JCContainer buttonContainer = new JCContainer();

	ok = new JCButton("OK");
	ok.addActionListener(this);
	cancel = new JCButton("Cancel");
	cancel.addActionListener(this);
	
	Layout.fill(buttonContainer, ok, 0, 0, GridBagConstraints.NONE);
	Layout.fill(buttonContainer, cancel, 1, 0, GridBagConstraints.NONE);

	Layout.fill(this, buttonContainer, 0, 2, GridBagConstraints.NONE);

	doLayout();
    }

    public int rgb = 0xff000000;

    public void updateColor(Color c, Object source){
	// record colour for the object
	rgb = c.getRGB();

	if(source != red && source != green && source != blue){
	    red.setIntValue((rgb & 0xff0000) >> 16);
	    green.setIntValue((rgb & 0xff00) >> 8);
	    blue.setIntValue((rgb & 0xff));

	    hexText.setText("0x" + String.format("%06x", rgb &0xffffff));
	}

	colorSample.setBackground(c);
	colorSample.repaint();
    }

    public void spinBoxChangeBegin(JCSpinBoxEvent e){
    }

    public void spinBoxChangeEnd(JCSpinBoxEvent e){
	int r = red.getIntValue();
	int g = green.getIntValue();
	int b = blue.getIntValue();

	Color c = new Color(r, g, b);

	updateColor(c, e.getSource());
    }    

    public void actionPerformed(ActionEvent e){
	if(e.getSource() instanceof TextField){
	    TextField tf = (TextField)e.getSource();
	    String text = tf.getText();
	    String fields[] = FILE.split(text, " ,");
	    int r = 0;
	    int g = 0;
	    int b = 0;
	    if(fields.length == 3){
		// assumed decimal rgb values
		r = FILE.readInteger(fields[0]);
		g = FILE.readInteger(fields[1]);
		b = FILE.readInteger(fields[2]);
	    }else if(fields.length == 1){
		// assume 6 digit hex code
		int crgb = Color32.getColorFromName(fields[0]);
		r = (crgb & 0xff0000) >> 16;
		g = (crgb & 0xff00) >> 8;
		b = (crgb & 0xff);
	    }else{
		Log.error("illegal colour format");
		Log.error("should be 0xabcdef or 255 0 255 or colorname");
		return;
	    }

	    Color c = new Color(r, g, b);

	    updateColor(c, e.getSource());
	}
    }

    // implementation of java.awt.event.ActionListener interface
    public void actionPerformed(JCActionEvent e){
	if(e.getSource() == ok ||
	   e.getSource() == cancel){
	    if(e.getSource() == ok){
		accept = true;
	    }else{
		accept = false;
	    }

	    dialog.dispose();
	}else{
	    JCButton b = (JCButton)e.getSource();
	    Color c = b.getBackground();

	    updateColor(c, e.getSource());
	}
    }

    public void mousePressed(MouseEvent e){
	if(e.getSource() == cs){
	    int i = e.getX();
	    int j = e.getY();

	    Color c = new Color(cs.getRGB(i, j));
	
	    updateColor(c, e.getSource());
	}
    }
    
    public void mouseReleased(MouseEvent e){
    }
    
    public void mouseClicked(MouseEvent e){
    }
    
    public void mouseEntered(MouseEvent e){
    }
    
    public void mouseExited(MouseEvent e){
    }
    
    // implementation of java.awt.event.MouseMotionListener interface
    public void mouseDragged(MouseEvent e){
	if(e.getSource() == cs){
	    int i = e.getX();
	    int j = e.getY();

	    Color c = new Color(cs.getRGB(i, j));
	
	    updateColor(c, e.getSource());
	}
    }

    public void mouseMoved(MouseEvent e){
    }
}
