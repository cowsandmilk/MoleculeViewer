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

/**
 * Store the data for a texture map.
 */
import java.io.*;
import java.awt.*;
import java.awt.image.*;

public class Texture {
    /** Width of the texture. */
    int width = 0;

    /** Height of the texture. */
    int height = 0;

    /** Data for the texture maps. */
    int pixels[] = null;

    private Texture(int w, int h){
	width = w;
	height = h;

	pixels = new int[width*height];
    }

    public Texture(){
	this(256, 256);
    }

    public static Texture lipophilicityTexture(){
	Texture t = new Texture(256, 256);

	return t;
    }

    public static Texture simpleTexture(){
	Texture t = new Texture(256, 256);
	int entry = 0;

	int colors[] = {
	    Color32.pack(255,   0,   0),
	    Color32.pack(255,   0,   0),
	    Color32.pack(255,   0,   0),
	    Color32.pack(255,  64,  64),
	    Color32.pack(255, 128, 128),
	    Color32.pack(255, 192, 192),
	    Color32.pack(255, 255, 255),
	    Color32.pack(255, 255, 255),
	    Color32.pack(255, 255, 255),
	    Color32.pack(255, 255, 255),
	    Color32.pack(192, 192, 255),
	    Color32.pack(128, 128, 255),
	    Color32.pack( 64,  64, 255),
	    Color32.pack(  0,   0, 255),
	    Color32.pack(  0,   0, 255),
	    Color32.pack(  0,   0, 255),
	};

	for(int k = 0; k < 256; k++){
	    for(int j = 0; j < 16; j++){
		for(int i = 0; i < 16; i++){
		    t.pixels[entry++] = colors[j];
		}
	    }
	}

	return t;
    }

    /** Fill values with specified colors along texture coordinate. */
    public void fillValues(String colors[], int tc){
	int rgb[] = new int[colors.length];

	for(int i = 0; i < colors.length; i++){
	    rgb[i] = Color32.getColorFromName(colors[i]);
	}

	for(int i = 0; i < width; i++){
	    int pixel = i;
	    int slot = (int)(colors.length * (double)i/(double)width);
	    int rgbp = rgb[slot];

	    for(int j = 0; j < height; j++){
		pixels[pixel] = rgbp;
		pixel += width;
	    }
	}
    }

    /** Resample the texture to the specified size. */
    private void setSize(int w, int h) {
	System.out.println("Texture.setSize: original width " + width +
			   " height " + height);
	int offset=w*h;
	int offset2;
	if (w * h != 0) {
	    int newpixels[] = new int[w * h];
	    for(int j = h - 1 ; j >= 0; j--) {
		offset -= w;
		offset2 = (j * height/h) * width;
		for (int i = w - 1; i >= 0; i--)
		    newpixels[i + offset] = pixels[(i * width/w) + offset2];
	    }
	    width = w;
	    height = h;
	    pixels = newpixels;

	    System.out.println("Texture.setSize: new     width " + width +
			       " height " + height);
	}
    }

    /** Create an image from a file/url as appropriate. */
    public static Image loadImage(String resource){
        FILE file = FILE.open(resource);
        
        if(file == null){
            return null;
	}
        
        InputStream input = file.getInputStream();

        if(input == null){
            return null;
        }

        byte b[] = new byte[100000];

        try {
            input.read(b);
        } catch(Exception e){
            System.out.println("Image was bigger than byte array buffer " + e);
        }

	Toolkit tk = Toolkit.getDefaultToolkit();

	Image image = tk.createImage(b);

	return image;
    }


    /** Load a texture from file/url as appropriate. */
    public static Texture loadTexture(String resource){
	Texture tex = new Texture();

        FILE file = FILE.open(resource);
        
        if(file == null)
            return null;
        
        InputStream input = file.getInputStream();
        byte b[] = new byte[100000];
        
        try {
            input.read(b);
        } catch(Exception e){
            System.out.println("Image was bigger than byte array buffer " + e);
        }

	Toolkit tk = Toolkit.getDefaultToolkit();

	Image image = tk.createImage(b);

	tex.loadPixels(image);

	return tex;
    }

    /** Get the pixel values from the image. */
    private void loadPixels(Image img) {
	try {
	    while (((width = img.getWidth(null)) < 0) ||
		   ((height = img.getHeight(null)) < 0)){
		try{
		    Thread.sleep(10);
		}catch(InterruptedException e){
		    e.printStackTrace();
		}
	    }
	    
	    System.out.println("image loaded width " +
			       width + " height " + height);

	    pixels = new int[width * height];
	    PixelGrabber pg =
		new PixelGrabber(img, 0, 0, width, height, pixels, 0, width);

	    pg.grabPixels();

	    if(width != 256 || height != 256){
		setSize(256, 256);
	    }
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
}
