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

package astex.thinlet;

import astex.*;
import thinlet.*;
import java.awt.*;
import java.awt.event.*;

public class LightCanvas extends CustomComponent implements MouseListener, MouseMotionListener {
    public MoleculeViewer moleculeViewer = null;
    public MoleculeRenderer moleculeRenderer  = null;
    public Renderer renderer = null;

    public Dimension getPreferredSize(){
	return new Dimension(200,200);
    }
    
    public void update(Graphics g){
	paint(g);
    }

    private int width = 0;
    private int height = 0;
    private int w2 = 0;
    private int h2 = 0;

    private int l2cx(double x){
	return (int)(w2 + x * w2);
    }

    private int l2cy(double y){
	return (int)(height - (h2 + y * h2));
    }

    private double c2lx(int x){
	return (double)(x - w2)/(double)w2;
    }

    private double c2ly(int y){
	return (double)(h2 - y)/(double)h2;
    }

    public void paint(Graphics g){
	width = getSize().width;
	height = getSize().height;
	w2 = width/2;
	h2 = height/2;
	g.setColor(Color.black);
	g.fillRect(0, 0, width, height);

	g.setColor(Color.lightGray);
	g.fillOval(0, 0, width, height);

	int lightCount = renderer.lights.size();

	for(int l = 0; l < lightCount; l++){
	    Light light = (Light)renderer.lights.get(l);

	    int lx = l2cx(light.pos[0]);
	    int ly = l2cy(light.pos[1]);

	    if(l == activeLight){
		g.setColor(Color.yellow);
	    }else{
		g.setColor(Color.blue);
	    }

	    g.drawString("" + l , lx, ly);
	}
    }

    private int activeLight = -1;

    public void mousePressed(MouseEvent e) {
	activeLight = -1;
	int dmin = 10000000;

	int lightCount = renderer.lights.size();
	
	int mx = e.getX();
	int my = e.getY();

	for(int l = 0; l < lightCount; l++){
	    Light light = (Light)renderer.lights.get(l);
	    int lx = l2cx(light.pos[0]);
	    int ly = l2cy(light.pos[1]);

	    int dx = lx - mx;
	    int dy = ly - my;

	    int d2 = dx*dx + dy*dy;

	    if(d2 < dmin && d2 < 100){
		dmin = d2;
		activeLight = l;
	    }
	}

	repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mouseClicked(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
	if(activeLight != -1){	    
	    double lx = c2lx(e.getX());
	    double ly = c2ly(e.getY());

	    double lz = Math.sqrt(1.- (lx*lx+ly*ly));

	    String command = "light " + activeLight + " -x " + lx + " -y " + ly + " -z " + lz + ";";

	    moleculeRenderer.execute(command);
	    
	    repaint();
	}

    }
    public void mouseMoved(MouseEvent e) {
    }

}