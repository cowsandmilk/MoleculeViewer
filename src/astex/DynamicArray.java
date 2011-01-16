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
/* Copyright Astex Technology Ltd. 2002 */

/*
 */

/**
 * An object that implements a dynamic array.
 * 
 * The array grows as necessary as objects are added to it.
 * Array copying is currently performed by explicit loops
 * rather than using System.arraycopy.
 *
 */
public class DynamicArray {
    /**
     * The array of objects.
     */
    private Object objects[] = null;

    /**
     * The number of objects stored in the array.
     */
    private int objectCount = 0;

    /**
     * The amount by which the array is grown.
     *
     * If the value is 0 then the array is doubled in size.
     */
    private int capacityIncrement = 4;

    /**
     * Constructor which specifies the initial size and the
     * capacity increment.
     */
    public DynamicArray(int initialSize, int increment){
        if(initialSize < 0){
            initialSize = 0;
        }
        if(increment < 0){
            increment = 0;
        }
        if(initialSize > 0){
            objects = new Object[initialSize];
        }
        objectCount = 0;
        capacityIncrement = increment;
    }

    /**
     * Default constructor.
     */
    public DynamicArray(){
        this(0);
    }

    /**
     * Constructor which specifies the initial size.
     */
    public DynamicArray(int initialSize){
        this(initialSize, 0);
    }

    /**
     * Convenience method to make sure that the object array has enough
     * room for a new object.
     */
    private void ensureCapacity(){
        // we need to grow our array.
        if(objects == null || objectCount == objects.length){
            int newCapacity;

            if(capacityIncrement == 0){
                newCapacity = objectCount * 2;
            }else{
                newCapacity = objectCount + capacityIncrement;
            }

            if(newCapacity == 0){
                newCapacity = 1;
            }

            Object newObjects[] = new Object[newCapacity];

            if(objects != null){
				System.arraycopy(objects, 0, newObjects, 0, objectCount);
            }

            objects = newObjects;
        }
    }

    /**
     * Add an entry to the CLASSNAME.
     */
    public int add(Object object){
        ensureCapacity();

        objects[objectCount] = object;

        return objectCount++;
    }

    /**
     * Remove an object from the CLASSNAME.
     *
     * All occurrences of the object will be removed.
     */
    public void remove(Object object){
        for(int i = objectCount - 1; i >= 0; i--){
            if(objects[i] == object){
                remove(i);
            }
        }
    }

    /**
     * Remove a specified element from the CLASSNAME.
     */
    public void remove(int element){
        if(element == objectCount - 1){
            // its the last entry so we can just decrement the
            // object counter
            objectCount--;
            objects[objectCount] = null;
        }else if(element < objectCount && element >= 0){
            for(int i = element + 1; i < objectCount; i++){
                objects[i - 1] = objects[i];
            }

            objectCount--;
            objects[objectCount] = null;
        }
    }

    /**
     * Remove all elements from the dynamic array.
     */
    public void clear(){
        for(int i = 0; i < objectCount; i++){
            objects[i] = null;
        }
        objectCount = 0;
    }

    /**
     * Return a specified element from the array.
     */
    public Object get(int index){
        return objects[index];
    }

    /** Return a specified element from the array end of the array. */
    public Object getReverse(int index){
        return objects[objectCount - index - 1];
    }

    /** Set a specified element in the array. */
    public void set(int index, Object val){
        // don't check for array bounds conditions.
        // or even the allocation of the array
        objects[index] = val;
    }

    /** Return the reference to the object array. */
    public Object[] toArray(){
        return objects;
    }

    /** Does the array contain the specified object. */
    public boolean contains(Object object){
        return indexOf(object) != -1;
    }

    /** Return the location of the object or -1 if its not present. */
    public int indexOf(Object object){
        for(int i = 0; i < objectCount; i++){
            if(objects[i] == object){
                return i;
            }
        }

        return -1;
    }

    /**
     * Return the number of objects in the object array.
     */
    public int size(){
        return objectCount;
    }
}
