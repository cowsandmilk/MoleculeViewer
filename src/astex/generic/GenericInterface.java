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

package astex.generic;

import java.util.*;

public interface GenericInterface {
    /** Some defined property constants. */
    public static final String Name = "name";

    /** Get Object representing key. */
    public Object get(Object key, Object def);

    /** Set an object value. */
    public Object set(Object key, Object value);

    /** Get an Enumeration of our parents. */
    public Iterator<GenericInterface> getParents(Object type);

    /** Add a parent. */
    public void addParent(GenericInterface parent);

    /** Remove a parent. */
    public void removeParent(GenericInterface parent);

    /** Get an enumeration of our children. */
    public Iterator<GenericInterface> getChildren(Object type);

    /** Add a child. */
    public void addChild(GenericInterface child);

    /** Remove a child. */
    public void removeChild(GenericInterface child);

    /** Add a listener. */
    public void addListener(GenericEventInterface geh);

    /** Remove a listener. */
    public void removeListener(GenericEventInterface geh);
}
