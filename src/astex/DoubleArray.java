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
/* Copyright Astex Technology Ltd. 2002 */
/* Copyright David Hall, Boston University, 2011 */

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
public class DoubleArray {
    /**
     * The array of objects.
     */
    private double objects[] = null;

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
    public DoubleArray(int initialSize, int increment){
        if(initialSize < 0){
            initialSize = 0;
        }
        if(increment < 0){
            increment = 0;
        }
        if(initialSize > 0){
            objects = new double[initialSize];
        }
        objectCount = 0;
        capacityIncrement = increment;
    }

    /**
     * Default constructor.
     */
    public DoubleArray(){
        this(0);
    }

    /**
     * Constructor which specifies the initial size.
     */
    public DoubleArray(int initialSize){
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

            double newObjects[] = new double[newCapacity];

            if(objects != null){
				System.arraycopy(objects, 0, newObjects, 0, objectCount);
            }

            objects = newObjects;
        }
    }

    /**
     * Add an entry to the CLASSNAME.
     */
    public int add(double object){
        ensureCapacity();

        objects[objectCount] = object;

        return objectCount++;
    }

    /**
     * Remove all elements from the dynamic array.
     */
    public void clear(){
        for(int i = 0; i < objectCount; i++){
            objects[i] = 0.0;
        }
        objectCount = 0;
    }

    /**
     * Return a specified element from the array.
     */
    public double get(int index){
        return objects[index];
    }

    /** Return the reference to the object array. */
    public double[] toArray(){
        return objects;
    }

    /**
     * Return the number of objects in the object array.
     */
    public int size(){
        return objectCount;
    }
}
