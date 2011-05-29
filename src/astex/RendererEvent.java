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

/** Events fired from the renderer. */
public class RendererEvent {
    public RendererEvent(Type t, Object o){
	type = t;
	item = o;
    }

    public enum Type {
	ObjectAdded, ObjectRemoved, FrontClipMoved, BackClipMoved
    }

    /** The type of the event. */
    private Type type;
    
    /**
     * Get the value of type.
     * @return value of type.
     */
    public Type getType() {
	return type;
    }

    private Object item = null;
    
    /**
     * Get the value of item.
     * @return value of item.
     */
    public Object getItem() {
	return item;
    }
}
