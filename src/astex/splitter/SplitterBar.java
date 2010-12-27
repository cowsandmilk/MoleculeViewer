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


package astex.splitter;

import java.awt.*;
import java.awt.event.*;

/** A class that implements a splitter bar.  It is only intended to be used
 *	in a SplitterLayout.  Because it is dervied from Panel, a SplitterBar
 *	can do anything a normal panel can do.  However, if you add any
 *	components to this panel, the scroll handle will not be accessible.  In a
 *	case like this, you need to explicitly add a SplitterSpace component to the
 *	SplitterBar, guaranteeing a place for the handle to be available.
 *
 * <p>Use this code at your own risk!  MageLang Institute is not
 * responsible for any damage caused directly or indirctly through
 * use of this code.
 * <p><p>
 * <b>SOFTWARE RIGHTS</b>
 * <p>
 * MageLang support classes, version 1.0, MageLang Institute
 * <p>
 * We reserve no legal rights to this code--it is fully in the
 * public domain. An individual or company may do whatever
 * they wish with source code distributed with it, including
 * including the incorporation of it into commerical software.
 *
 * <p>However, this code cannot be sold as a standalone product.
 * <p>
 * We encourage users to develop software with this code. However,
 * we do ask that credit is given to us for developing it
 * By "credit", we mean that if you use these components or
 * incorporate any source code into one of your programs
 * (commercial product, research project, or otherwise) that
 * you acknowledge this fact somewhere in the documentation,
 * research report, etc... If you like these components and have
 * developed a nice tool with the output, please mention that
 * you developed it using these components. In addition, we ask that
 * the headers remain intact in our source code. As long as these
 * guidelines are kept, we expect to continue enhancing this
 * system and expect to make other tools available as they are
 * completed.
 * <p>
 * The MageLang Support Classes Gang:
 * @version MageLang Support Classes 1.0, MageLang Insitute, 1997
 * @author <a href="http:www.scruz.net/~thetick">Scott Stanchfield</a>, <a href=http://www.MageLang.com>MageLang Institute</a>
 *	@see SplitterLayout
 *	@see SplitterSpace
*/
public class SplitterBar extends Panel {
	static final Cursor VERT_CURSOR = new Cursor(Cursor.N_RESIZE_CURSOR);
	static final Cursor HORIZ_CURSOR = new Cursor(Cursor.E_RESIZE_CURSOR);
	static final Cursor DEF_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
	private int orientation = SplitterLayout.VERTICAL;
	
	private boolean alreadyDrawn = false;
	private Rectangle originalBounds=null;
	private Window wBar;
	private boolean mouseInside=false;
	
    public Dimension getPreferredSize(){
        return new Dimension(6, 6);
    }

	/** Creates a new SpliiterBar */
	public SplitterBar() {
            setBackground(new Color(0xe6e6e6));
		addMouseMotionListener(new SplitterBarMouseMotionListener(this));
		addMouseListener(new SplitterBarMouseListener(this));
		}
	private void checkOtherComponents() {
		Rectangle currBounds = getBounds();  // get current position
		Component comps[]    = getParent().getComponents();
		Insets    insets     = getParent().getInsets();
		Rectangle parentBounds = getParent().getBounds();

		// determine which component "this" is
		int curr;
		for(curr=0; (curr < comps.length) && (comps[curr] != this); curr++);
		int origCurr = curr; // hold for part II check

		if (orientation == SplitterLayout.VERTICAL) {
			if (currBounds.y < originalBounds.y) { // moved up
				// could have moved _into_ splitter bars above (or top edge)
				//   and/or away from splitter bars below (or bottom edge)

				// check to see it we've bumped into a splitter above us.
				boolean done=false;
				for (int temp=curr-1; !done && temp > -1; temp--) {
					if (comps[temp] instanceof SplitterBar) {
						Rectangle r = comps[temp].getBounds();
						if (currBounds.y <= r.y + r.height) { // touching or above...
							comps[temp].setLocation(r.x, currBounds.y - r.height);
							// any comps in between should be hidden
							for(int c=curr-1; c > temp; c--)
								comps[c].setVisible(false);
							curr       = temp;
							currBounds = comps[temp].getBounds();
							} // touching or above
						else done=true; // no more compression
						} // it's a splitter bar
					} // for each component before us

				// did we push to far?
				if (currBounds.y <= insets.top) {
					int delta = currBounds.y - insets.top;
					// hide all components before that one
					for(int temp=curr-1; temp > -1; temp--)
						comps[temp].setVisible(false);
					// push all splitter bars into view
					done = false;
					for(int temp=curr;!done && temp <= origCurr; temp++)
						if (comps[temp] instanceof SplitterBar) {
							Point p = comps[temp].getLocation();
							p.y -= delta;
							comps[temp].setLocation(p);
							}
						else done = comps[temp].isVisible();
					} // pushed highest component off top edge

				// next, check if we exposed components below us
				curr = origCurr;
				// if the next component is not visible, show all between us & next
				//    splitter bar or bottom edge
				for(int temp=curr+1; temp<comps.length && !comps[temp].isVisible(); temp++)
					comps[temp].setVisible(true);
				} // VERTICAL -- moved up


			else if (currBounds.y > originalBounds.y) { // moved down
				// could have moved _into_ splitter bars below (or bottom edge)
				//   and/or away from splitter bars above (or top edge)

				// check to see it we've bumped into a splitter below us.
				boolean done=false;
				for (int temp=curr+1; !done && temp < comps.length; temp++) {
					if (comps[temp] instanceof SplitterBar) {
						Rectangle r = comps[temp].getBounds();
						if (currBounds.y + currBounds.height >= r.y) { // touching or below...
							comps[temp].setLocation(r.x, currBounds.y + currBounds.height);
							// any comps in between should be hidden
							for(int c=curr+1; c < temp; c++)
								comps[c].setVisible(false);
							curr       = temp;
							currBounds = comps[temp].getBounds();
							} // touching or above
						else done=true; // no more compression
						} // it's a splitter bar
					} // for each component before us

				// did we push to far?
				if ((currBounds.y + currBounds.height) >= (parentBounds.height-insets.bottom)) {
					int delta = currBounds.y + currBounds.height - (parentBounds.height-insets.bottom);
					// hide all components before that one
					for(int temp=curr+1; temp < comps.length; temp++)
						comps[temp].setVisible(false);
					// push all splitter bars into view
					done = false;
					for(int temp=curr;!done && temp >= origCurr; temp--)
						if (comps[temp] instanceof SplitterBar) {
							Point p = comps[temp].getLocation();
							p.y -= delta;
							comps[temp].setLocation(p);
							}
						else done = comps[temp].isVisible();
					} // pushed highest component off top edge

				// next, check if we exposed components below us
				curr = origCurr;
				// if the next component is not visible, show all between us & next
				//    splitter bar or bottom edge
				for(int temp=curr-1; temp>-1 && !comps[temp].isVisible(); temp--)
					comps[temp].setVisible(true);
				} // VERTICAL -- moved down
			} // orientation==VERTICAL

		else { // orientation == HORIZONTAL
			if (currBounds.x < originalBounds.x) { // moved left
				// could have moved _into_ splitter bars to left (or left edge)
				//   and/or away from splitter bars to right (or right edge)

				// check to see it we've bumped into a splitter above us.
				boolean done=false;
				for (int temp=curr-1; !done && temp > -1; temp--) {
					if (comps[temp] instanceof SplitterBar) {
						Rectangle r = comps[temp].getBounds();
						if (currBounds.x <= r.x + r.width) { // touching or above...
							comps[temp].setLocation(currBounds.x - r.width, r.y);
							// any comps in between should be hidden
							for(int c=curr-1; c > temp; c--)
								comps[c].setVisible(false);
							curr       = temp;
							currBounds = comps[temp].getBounds();
							} // touching or above
						else done=true; // no more compression
						} // it's a splitter bar
					} // for each component before us

				// did we push to far?
				if (currBounds.x <= insets.left) {
					int delta = currBounds.x - insets.left;
					// hide all components before that one
					for(int temp=curr-1; temp > -1; temp--)
						comps[temp].setVisible(false);
					// push all splitter bars into view
					done = false;
					for(int temp=curr;!done && temp <= origCurr; temp++)
						if (comps[temp] instanceof SplitterBar) {
							Point p = comps[temp].getLocation();
							p.x -= delta;
							comps[temp].setLocation(p);
							}
						else done = comps[temp].isVisible();
					} // pushed highest component off top edge

				// next, check if we exposed components below us
				curr = origCurr;
				// if the next component is not visible, show all between us & next
				//    splitter bar or bottom edge
				for(int temp=curr+1; temp<comps.length && !comps[temp].isVisible(); temp++)
					comps[temp].setVisible(true);
				} // HORIZONTAL -- moved left

			else if (currBounds.x > originalBounds.x) { // moved right
				// could have moved _into_ splitter bars to right (or right edge)
				//   and/or away from splitter bars to left (or left edge)

				// check to see it we've bumped into a splitter to our right us.
				boolean done=false;
				for (int temp=curr+1; !done && temp < comps.length; temp++) {
					if (comps[temp] instanceof SplitterBar) {
						Rectangle r = comps[temp].getBounds();
						if (currBounds.x + currBounds.width >= r.x) { // touching or to right...
							comps[temp].setLocation(currBounds.x + currBounds.width, r.y);
							// any comps in between should be hidden
							for(int c=curr+1; c < temp; c++)
								comps[c].setVisible(false);
							curr       = temp;
							currBounds = comps[temp].getBounds();
							} // touching or above
						else done=true; // no more compression
						} // it's a splitter bar
					} // for each component before us

				// did we push to far?
				if ((currBounds.x + currBounds.width) >= (parentBounds.width-insets.right)) {
					int delta = currBounds.x + currBounds.width - (parentBounds.width-insets.right);
					// hide all components before that one
					for(int temp=curr+1; temp < comps.length; temp++)
						comps[temp].setVisible(false);
					// push all splitter bars into view
					done = false;
					for(int temp=curr;!done && temp >= origCurr; temp--)
						if (comps[temp] instanceof SplitterBar) {
							Point p = comps[temp].getLocation();
							p.x -= delta;
							comps[temp].setLocation(p);
							}
						else done = comps[temp].isVisible();
					} // pushed highest component off top edge

				// next, check if we exposed components below us
				curr = origCurr;
				// if the next component is not visible, show all between us & next
				//    splitter bar or bottom edge
				for(int temp=curr-1; temp>-1 && !comps[temp].isVisible(); temp--)
					comps[temp].setVisible(true);
				} // HORIZONTAL -- moved right
			} // orientation==HORIZONTAL

		} // checkComponents()
	public int getOrientation() {return orientation;}
	void mouseDrag(MouseEvent e) {
		if (SplitterLayout.dragee == null)
			SplitterLayout.dragee = this;
		else if (SplitterLayout.dragee != this)
			return;
		Component c = getParent();
		Point fl = c.getLocationOnScreen();
		while(c.getParent() != null)
			c = c.getParent();
		if (!alreadyDrawn) {
			originalBounds = getBounds();
			wBar = new Window((Frame)c);
			wBar.setBackground(getBackground().darker());
			}
		Container cp = getParent();
		Dimension parentDim = cp.getSize();
		Point l = getLocationOnScreen();
		Insets insets = ((Container)cp).getInsets();
		if (orientation == SplitterLayout.VERTICAL)
			parentDim.width -= insets.right + insets.left;
		else
			parentDim.height -= insets.top + insets.bottom;
		Rectangle r = getBounds(); // mouse event is relative to this...
		int x = l.x+(orientation==SplitterLayout.HORIZONTAL?e.getX():0);
		int y = l.y+(orientation==SplitterLayout.VERTICAL?e.getY():0);
		if (x<fl.x+insets.left) x = fl.x+insets.left;
		else if ((orientation==SplitterLayout.HORIZONTAL) && (x > fl.x+parentDim.width-r.width))
			x = fl.x+parentDim.width-r.width;
		if (y<fl.y+insets.top) y = fl.y+insets.top;
		else if ((orientation==SplitterLayout.VERTICAL) && (y > fl.y+parentDim.height-r.height))
			y = fl.y+parentDim.height-r.height;
		wBar.setBounds(x,y,
					 	(orientation==SplitterLayout.HORIZONTAL)?r.width:parentDim.width,
					 	(orientation==SplitterLayout.VERTICAL)?r.height:parentDim.height);
		if (!alreadyDrawn) {
			wBar.setVisible(true);
			alreadyDrawn=true;
			}
		}
	void mouseEnter(MouseEvent e) {
		if (SplitterLayout.dragee != null) return;
		setCursor((orientation == SplitterLayout.VERTICAL)?VERT_CURSOR:HORIZ_CURSOR);
		mouseInside = true;
		invalidate();
		validate();
		repaint();
		}
	void mouseExit(MouseEvent e) {
		if (SplitterLayout.dragee != null) return;
		setCursor(DEF_CURSOR);
		mouseInside = false;
		invalidate();
		validate();
		repaint();
	   	}   
	void mouseRelease(MouseEvent e) {
		if (alreadyDrawn) {
			if (SplitterLayout.dragee != this) return;
			SplitterLayout.dragee = null;
			wBar.setVisible(false); wBar.dispose(); wBar=null; alreadyDrawn=false;
			Rectangle r = getBounds(); // mouse event is relative to this...
			r.x += (orientation==SplitterLayout.HORIZONTAL?e.getX():0);
			r.y += (orientation==SplitterLayout.VERTICAL?e.getY():0);
			setLocation(r.x, r.y);
			setCursor(DEF_CURSOR);

			// check to see if we need to move other splitters and hide other
			// components that are controlled by the layout
			// First -- find what component this one is

			checkOtherComponents();
			mouseInside = false;
			invalidate();
			getParent().validate();
			SplitterLayout.dragee = null;
			}
		}
	/** Paints the image of a SplitterBar.  If nothing was added to
		the SplitterBar, this image will only be a thin, 3D raised line that
		will act like a handle for moving the SplitterBar.
		If other components were added the SplitterBar, the thin 3D raised
		line will onlty appear where SplitterSpace components were added.
	*/
	public void paint (Graphics g) {
            g.setColor(new Color(0x89899a));
            //if (mouseInside)
            //		g.setColor(Color.yellow);
            //else
            //	g.setColor(Colors.lightSkyBlue3);
		Component c[] = getComponents();
		if (c != null && c.length > 0)
	    	for(int i = 0; i <c.length;i++) {
				if (c[i] instanceof SplitterSpace) {
					// only draw boxes where SplitterSpace components appear
					Rectangle r = c[i].getBounds();
					if (orientation == SplitterLayout.VERTICAL)
						g.fill3DRect(r.x+2,r.y+r.height/2-1,r.width-5,3,true);
					else
						g.fill3DRect(r.x+r.width/2-1,r.y+2,3,r.y+r.height-5,true);
					}
				}
		else {
                    Rectangle r = getBounds();
			//if (orientation == SplitterLayout.VERTICAL)
			//	g.fill3DRect(2,r.height/2-1,r.width-5,3,true);
			//else
			//	g.fill3DRect(r.width/2-1,2,3,r.height-5,true);
			//}
			if (orientation == SplitterLayout.VERTICAL){
                            g.drawLine(2, r.height/2 -12, 2, r.height/2 + 12);
                            g.drawLine(r.width - 2, r.height/2 -12, r.width - 2, r.height/2 + 12);
			}else{
                            g.drawLine(2, r.height/2 -12, 2, r.height/2 + 12);
                            g.drawLine(r.width - 2, r.height/2 -12, r.width - 2, r.height/2 + 12);
			}
                }
        }
	void setOrientation(int o) {orientation = o;}
	public void swapOrientation() {
		setOrientation(getOrientation()==SplitterLayout.HORIZONTAL?SplitterLayout.VERTICAL:SplitterLayout.HORIZONTAL);
	}
	/** Called by AWT to update the image produced by the SplitterBar */
	public void update (Graphics g)  {paint(g);}
}
