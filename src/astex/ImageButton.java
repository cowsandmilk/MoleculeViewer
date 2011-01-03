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

import java.util.*;
import java.awt.*;
import java.awt.event.*;

public class ImageButton extends Canvas implements MouseListener {
    private Image image;
    private boolean selected = false;
    private Dimension size;
    private int shadowWidth = 2;
    private boolean armed = false;

    public ImageButton(Image img, int s) {
	super();
	this.image = img;
	if (image == null) {
	    size = new Dimension(2*shadowWidth + s, 2*shadowWidth + s);
	} else {
	    size = new Dimension(image.getWidth(this) + 2*shadowWidth,
				 image.getHeight(this) + 2*shadowWidth);
	}
	addMouseListener(this);
    }

    Vector actionListeners = new Vector();

    public void addActionListener(ActionListener al){
	actionListeners.addElement(al);
    }

    private void fireActionEvent(){
	int listenerCount = actionListeners.size();

	if(listenerCount > 0){
	    ActionEvent ev = new ActionEvent(this, 1, "changed");

	    for(int i = 0; i < listenerCount; i++){
		ActionListener al =
		    (ActionListener)actionListeners.elementAt(i);
		al.actionPerformed(ev);
	    }
	}
    }

    public void mousePressed(MouseEvent e) {
	armed = true;
	repaint();
    }

    public void mouseReleased(MouseEvent e) {
	if (selected) {
	    armed = false;
	    repaint();
	    fireActionEvent();
	}
    }

    public void mouseEntered(MouseEvent e) {
	selected = true;
	repaint();
    }

    public void mouseExited(MouseEvent e) {
	selected = false;
	armed = false;
	repaint();
    }

    public void mouseClicked(MouseEvent e) { }

    public Dimension getMinimumSize() {
	return size;
    }

    public void paint(Graphics g) {
	Dimension s = getSize();
	Color bg = getBackground();

	for(int i = 0; i < shadowWidth; i++){
	    int t = i;
	    int b = s.height - i - 1;
	    int l = i;
	    int r = s.width  - i - 1;

	    if(armed){
		g.setColor(bg.darker());
	    }else{
		g.setColor(bg.brighter());
	    }
	    g.drawLine(l, t, r, t);
	    g.drawLine(l, t, l, b);

	    if(armed){
		g.setColor(bg.brighter());
	    }else{
		g.setColor(bg.darker());
	    }
	    g.drawLine(l, b, r, b);
	    g.drawLine(r, b, r, t);
	}

	if(image != null){
	    g.drawImage(image, shadowWidth, shadowWidth, this);
	}
    }

    public Dimension getPreferredSize() {
	return getMinimumSize();
    }

    public void update(Graphics g) {
	paint(g);
    }
}

