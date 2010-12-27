package thinlet;

import java.awt.*;

/**
 * CustomComponent extends the lightweight AWT Component class the way to be functional inside
 * <a href="http://www.thinlet.com/">Thinlet</a>. Thinlet itself manages its own components from
 * scratch and therefor we are not able to simply add new Components to it. This class, for instance,
 * provides the strict mimimum for Components to work in a Thinlet environment by simply changing
 * theyr inheritence from Component to CustomComponent. A much better technique would be a Proxy.
 *
 * @autor: stephan@stean.ch
 * @version: 1.0
 */
public class CustomComponent extends Component {
  private Object component;
  private Thinlet thinlet;

  /**
   * Default constructor
   */
  public CustomComponent() { }

  /**
   * Called by Thinlet when creating a new instance of this. For further processing we
   * need to know which component we are inside Thinlet.
   *
   * @param component
   */
  public void setComponent(Object component) {
    this.component=component;
  }
  /**
   * Called by Thinlet, we need a reference back to it.
   *
   * @param thinlet
   */
  public void setThinlet(Thinlet thinlet) {
    this.thinlet=thinlet;
  }
    
    /** Emergency access to thinlet. */
    public Thinlet getThinlet(){
        return thinlet;
    }

    /** Emergency access to component. */
    public Object getComponent(){
        return component;
    }

  /**
   * We do not have a Graphics instance on our own, therefor we need to get it from Thinlet
   * but with the right translation and clipping.
   *
   * @return the Graphics for this
   */
  public Graphics getGraphics() {
    Graphics g = this.thinlet.getGraphics();
    Object o = this.component;
    int x=0;
    int y=0;
    while(o!=null) {
      Rectangle bounds = this.thinlet.getRectangle(o,"bounds");
      if(bounds == null) return(null);
      x+=bounds.x;
      y+=bounds.y;
      o=this.thinlet.getParent(o);
    }
    Rectangle bounds = this.thinlet.getRectangle(this.component, "bounds");
    g.translate(x, y);
    g.clipRect(0, 0, bounds.width, bounds.height);
    return(g);
  }
  /**
   * Set the cursor image to a predefined cursor.<P>
   *
   * @param cursor One of the constants defined by the {@link Cursor} class. If
   *       this parameter is <TT>null</TT> the parent cursor will be inherited
   */
  //public void setCursor(Cursor cursor) {
   // this.thinlet.setCursor(cursor);
  //}
  /**
   * Gets the cursor set on this component.<P>
   *
   * @return  The cursor for this component.
   */
  //public Cursor getCursor() {
   // return(this.thinlet.getCursor());
  //}
  /**
   * Painting is done by Thinlet, so when this calls repaint it will simply be
   * redirected the way it should back to Thinlet.
   */
  public void repaint() {
    this.thinlet.repaint(this.component);
  }
}
