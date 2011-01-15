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

import java.io.*;
import java.util.zip.*;

/**
 * Write a pixel format image to a file.
 * Limited options currently (windows bitmap).
 */
public class ImageIO {
    private static int imageNumber = 0;

    public static boolean write(String name, int pixels[],
				int width, int height,
				boolean compress){
	if(name.endsWith(".gz")){
	    compress = true;
	}else if(compress && !name.endsWith(".gz")){
	    name += ".gz";
	}

	int posw = name.indexOf("%w");

	if(posw != -1){
	    StringBuilder newname = new StringBuilder(name.substring(0, posw));
	    newname.append(String.format("%d", width));
	    newname.append(name.substring(posw + 2, name.length()));

	    name = newname.toString();
	}

	int posh = name.indexOf("%h");

	if(posh != -1){
	    StringBuilder newname = new StringBuilder(name.substring(0, posh));
	    newname.append(String.format("%d", height));
	    newname.append(name.substring(posh + 2, name.length()));

	    name = newname.toString();
	}

	if(name.indexOf('%') != -1){
	    name = String.format(name, imageNumber);
	    imageNumber++;
	}

	Log.info("writing image to " + name);

	boolean status = false;

	if(name.endsWith(".gif")){
	    //WriteGifEncoder wge = new WriteGifEncoder(pixels, width, height);
	    //status = wge.writeFile(name);
            System.out.println("GIF file I/O not supported");
	}else{

	    BufferedOutputStream bos = null;

	    try {
		bos = new BufferedOutputStream(new FileOutputStream(name),
					       1048576);
		
		OutputStream os = bos;

		if(compress){
		    GZIPOutputStream gos = new GZIPOutputStream(os);
		    os = gos;
		}

		status = writeBMP(os, pixels, width, height);

		os.close();
	    }catch(Exception e){
		Log.error("exception " + e);
	    }
	}

	return status;
    }

    /**
     * Read a windows bitmap file.
     */
    public static int[] readBitmap(String file, int size[]){
	try {
	    FILE f = FILE.open(file);
	    InputStream fs = f.getInputStream();
	    int bflen=14;  // 14 byte BITMAPFILEHEADER
	    byte bf[]=new byte[bflen];
	    fs.read(bf,0,bflen);
	    int bilen=40; // 40-byte BITMAPINFOHEADER
	    byte bi[]=new byte[bilen];
	    fs.read(bi,0,bilen);

	    // Interperet data.
	    int nwidth = (((int)bi[7]&0xff)<<24)
		| (((int)bi[6]&0xff)<<16)
		| (((int)bi[5]&0xff)<<8)
		| (int)bi[4]&0xff;
	    //System.out.println("Width is :"+nwidth);

	    int nheight = (((int)bi[11]&0xff)<<24)
		| (((int)bi[10]&0xff)<<16)
		| (((int)bi[9]&0xff)<<8)
		| (int)bi[8]&0xff;
	    //System.out.println("Height is :"+nheight);

	    int nbitcount = (((int)bi[15]&0xff)<<8) | (int)bi[14]&0xff;
	    //System.out.println("BitCount is :"+nbitcount);

	    int nsizeimage = (((int)bi[23]&0xff)<<24)
		| (((int)bi[22]&0xff)<<16)
		| (((int)bi[21]&0xff)<<8)
		| (int)bi[20]&0xff;
	    //System.out.println("SizeImage is :"+nsizeimage);

	    int ndata[] = null;
	    
	    if (nbitcount==24) {
		// No Palatte data for 24-bit format but scan lines are
		// padded out to even 4-byte boundaries.
		int npad = (nsizeimage / nheight) - nwidth * 3;
		ndata = new int [nheight * nwidth];
		byte brgb[] = new byte [( nwidth + npad) * 3 * nheight];
		fs.read (brgb, 0, (nwidth + npad) * 3 * nheight);
		int nindex = 0;
		for (int j = 0; j < nheight; j++) {
		    for (int i = 0; i < nwidth; i++) {
			ndata [nwidth * (nheight - j - 1) + i] =
			    (255&0xff)<<24
			    | (((int)brgb[nindex+2]&0xff)<<16)
			    | (((int)brgb[nindex+1]&0xff)<<8)
			    | (int)brgb[nindex]&0xff;
			nindex += 3;
		    }
		    nindex += npad;
		}

		size[0] = nwidth;
		size[1] = nheight;
	    } else {
		System.out.println ("readBitmap: not a 24-bit or 8-bit Windows Bitmap, aborting...");
	    }
	    
	    fs.close();

	    return ndata;
	}catch(Exception e){
	    System.out.println("readBitmap: some sort of error: " + e);
	}

	return null;
    }

    /**
     * Write a windows bitmap file.
     * This is based on code from Javaworld, but with bugs ironed
     * out using code from the Java Image Utilities.
     */
    public static boolean writeBMP(OutputStream os, int pixels[],
				   int width, int height){
	writeBitmapFileHeader(os, width, height);
        writeBitmapInfoHeader(os);
        writeBitmap(os, pixels, width, height);

	return true;
    }

    private static void writeBitmap(OutputStream os, int pixels[],
				   int parWidth, int parHeight){
	try {
	    byte rgb [] = new byte [3];

	    int bytesPerRow = parWidth * 3;

	    if((bytesPerRow % 4) != 0){
		bytesPerRow = ((bytesPerRow + 3) / 4) * 4;
	    }

	    int pad = bytesPerRow - parWidth * 3;

	    for (int y = parHeight - 1, processed = 0;
		 processed < parHeight; y--, processed++) {
		int pos = y * parWidth;
		for (int x = 0; x < parWidth; x++) {
		    int value = pixels[pos];
		    rgb [0] = (byte) (value & 0xFF);
		    rgb [1] = (byte) ((value >> 8) & 0xFF);
		    rgb [2] = (byte) ((value >> 16) & 0xFF);
		    os.write (rgb);
		    pos++;
		}

		for(int i = 0; i < pad; i++){
		    os.write (0x00);
		}
	    }
	}catch(Exception e){
	    Log.error("exception " + e);
	}
    }

    private static void writeBitmapFileHeader(OutputStream os,
					      int parWidth, int parHeight) {
	try {
	    int bytesPerRow = parWidth * 3;

	    if((bytesPerRow % 4) != 0){
		bytesPerRow = ((bytesPerRow + 3) / 4) * 4;
	    }

	    biSizeImage = bytesPerRow * parHeight;

	    bfSize = biSizeImage +
		BITMAPFILEHEADER_SIZE +
		BITMAPINFOHEADER_SIZE;
	    biWidth = parWidth;
	    biHeight = parHeight;

	    os.write(bfType);

	    intToDWord(os, bfSize);
	    intToWord(os, bfReserved1);
	    intToWord(os, bfReserved2);
	    intToDWord(os, bfOffBits);
	}catch(Exception e){
	    Log.error("exception " + e);
	}
    }
 
    private static void writeBitmapInfoHeader(OutputStream os) {
        intToDWord(os, biSize);
        intToDWord(os, biWidth);
        intToDWord(os, biHeight);
        intToWord(os, biPlanes);
        intToWord(os, biBitCount);
        intToDWord(os, biCompression);
        intToDWord(os, biSizeImage);
        intToDWord(os, biXPelsPerMeter);
        intToDWord(os, biYPelsPerMeter);
        intToDWord(os, biClrUsed);
        intToDWord(os, biClrImportant);
    }

    private static void intToWord(OutputStream os, int parValue){
	try {
	    os.write((byte)(parValue & 0x00FF));
	    os.write((byte)((parValue >>  8) & 0x00FF));
	}catch(Exception e){
	    Log.error("exception " + e);
	}
    }

    private static void intToDWord(OutputStream os, int parValue){
	try {
	    os.write((byte)(parValue & 0x00FF));
	    os.write((byte)((parValue >>  8) & 0x000000FF));
	    os.write((byte)((parValue >>  16) & 0x000000FF));
	    os.write((byte)((parValue >>  24) & 0x000000FF));
	}catch(Exception e){
	    Log.error("exception " + e);
	}
    }

    private final static int BITMAPFILEHEADER_SIZE = 14;
    private final static int BITMAPINFOHEADER_SIZE = 40;
    //--- Private variable declaration
    //--- Bitmap file header
    private static byte bfType [] = {(byte)'B', (byte)'M'};
    private static int bfSize = 0;
    private static int bfReserved1 = 0;
    private static int bfReserved2 = 0;
    private static int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE;
  //--- Bitmap info header
    private static int biSize = BITMAPINFOHEADER_SIZE;
    private static int biWidth = 0;
    private static int biHeight = 0;
    private static int biPlanes = 1;
    private static int biBitCount = 24;
    private static int biCompression = 0;
    private static int biSizeImage = 0x030000;
    private static int biXPelsPerMeter = 2834;
    private static int biYPelsPerMeter = 2834;
    private static int biClrUsed = 0;
    private static int biClrImportant = 0;
}