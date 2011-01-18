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

package astex.generic;

import java.util.*;

public abstract class Generic implements GenericInterface {
    private static final List<GenericInterface> emptyArrayList = Collections.emptyList();

    private HashMap<String,Object> properties = null;

    public Object get(Object key, Object def){
        if(properties == null){

            return def;
        }
	Object value = properties.get(key);

	return (value != null) ? value : def;
    }

    public Object set(Object name, Object value){
        Object oldValue = null;

        if(properties == null){
            properties = new HashMap<String,Object>();
        }else{
            oldValue = properties.get(name);
        }

        if(value == null){
            properties.remove(name);
        }else{
            properties.put((String) name,value);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.PropertyChanged,
                                               this, name, value);
            
            notifyListeners(ge);
        }

        return oldValue;
    }

    ArrayList<GenericInterface> children = null;

    public void addChild(GenericInterface child){
        if(children == null){
            children = new ArrayList<GenericInterface>(10);
        }

        children.add(child);

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ChildAdded,
                                               this, child, null);
            notifyListeners(ge);
        }
    }

    public void removeChild(GenericInterface child){
        if(children != null){
            children.remove(child);
        }else{
            throw new RuntimeException("no such child: " + child);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ChildRemoved,
                                               this, child, null);
            notifyListeners(ge);
        }
    }

    public Iterator<GenericInterface> getChildren(Object type){
        if(children == null){
            return emptyArrayList.iterator();
        }else{
            return children.iterator();
        }
    }

    ArrayList<GenericInterface> parents = null;

    public void addParent(GenericInterface parent){
        if(parents == null){
            parents = new ArrayList<GenericInterface>(10);
        }

        parents.add(parent);

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ParentAdded,
                                               this, parent, null);
            notifyListeners(ge);
        }
    }

    public void removeParent(GenericInterface parent){
        if(parents != null){
            parents.remove(parent);
        }else{
            throw new RuntimeException("no such parent: " + parent);
        }

        if(listeners != null){
            GenericEvent ge = new GenericEvent(GenericEvent.ParentRemoved,
                                               this, parent, null);
            notifyListeners(ge);
        }
    }

    public Iterator<GenericInterface> getParents(Object type){
        if(parents == null){
            return emptyArrayList.iterator();
        }
        return parents.iterator();
    }

    ArrayList<GenericEventInterface> listeners = null;

    public void addListener(GenericEventInterface gei){
        if(listeners == null){
            listeners = new ArrayList<GenericEventInterface>(2);
        }

        listeners.add(gei);
    }

    public void removeListener(GenericEventInterface gei){
        if(listeners != null){
            listeners.remove(gei);
        }
    }

    private void notifyListeners(GenericEvent ge){
        if(listeners != null){
            for(GenericEventInterface listener: listeners){
                listener.handleEvent(ge);
            }
        }
    }

    /** Get a double. */
    public double getDouble(Object property, double def){
        Double val = (Double)get(property, null);

        return val != null ? val.doubleValue() : def;
    }

    /** Get an int. */
    public int getInteger(Object property, int def){
        Integer val = (Integer)get(property, null);

        return val != null ? val.intValue() : def;
    }

    /** Get a String. */
    public String getString(Object property, String def){
        String val = (String)get(property, null);

        return val != null ? val : def;
    }

    /** Get a boolean. */
    public boolean getBoolean(Object property, boolean def){
        Boolean val = (Boolean)get(property, null);

        return val != null ? val.booleanValue() : def;
    }

    /** Set a double. */
    public void setDouble(Object property, double val){
        Double dval = Double.valueOf(val);

        set(property, dval);
    }

    /** Set an int. */
    public void setInteger(Object property, int val){
        Integer ival = Integer.valueOf(val);

        set(property, ival);
    }

    /** Set a String. */
    public void setString(Object property, String val){
        set(property, val);
    }

    /** Set a Boolean. */
    public void setBoolean(Object property, boolean val){
        if(val){
            set(property, Boolean.TRUE);
        }else{
            set(property, Boolean.FALSE);
        }
    }
}
