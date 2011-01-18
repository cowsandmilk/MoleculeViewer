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

class GenericEvent extends Generic {

    public static final String Class               = "GenericEvent";

    public static final String Type                = "__type__";

    public static final String PropertyChanged     = "__property_changed__";
    public static final String ChildAdded          = "__child_added__";
    public static final String ChildRemoved        = "__child_removed__";
    public static final String ParentAdded         = "__parent_added__";
    public static final String ParentRemoved       = "__parent_removed__";

    public static final String Name                = "__name__";
    public static final String Value               = "__value__";
    public static final String Target              = "__target__";
    public static final String Child               = "__child__";
    public static final String Parent              = "__parent__";

    public GenericEvent(String type, Object target, Object a, Object b){
        super(Class);

        set(Type, type);
        set(Target, target);

        if(PropertyChanged.equals(type)){
            set(Name, a);
            set(Value, b);
        }else if(ChildAdded.equals(type) ||
                 ChildRemoved.equals(type)){
            set(Child, a);
        }else if(ParentAdded.equals(type) ||
                 ParentRemoved.equals(type)){
            set(Parent, a);
        }
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder(Class + ": ");
 
        Object type = get(Type, null);

        s.append(Type + "=").append(type).append(" ");
        s.append(Target + "=").append(get(Target, null)).append(" ");

        if(PropertyChanged == type){
            s.append(get(Name, null)).append("=").append(get(Value, null));
        }else if(ChildAdded == type){
            s.append(Child + "=").append(get(Child, null));
        }else if(ChildRemoved == type){
            s.append(Child + "=").append(get(Child, null));
        }else if(ParentAdded == type){
            s.append(Parent + "=").append(get(Parent, null));
        }else if(ParentRemoved == type){
            s.append(Parent + "=").append(get(Parent, null));
        }

        return s.toString();
    }
}
