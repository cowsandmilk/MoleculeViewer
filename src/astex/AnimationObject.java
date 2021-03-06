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

class AnimationObject {
    
    public enum Mode {
	Rock, Roll, Recenter, Command, MoleculeAnimation
    }

    /** The angle through which we rock (in degrees). */
    private double rockAngle         = 7.5;

    /** The sleep duration. */
    private int sleepDuration        = 100;

    /** The current animation mode. */
    private Mode animationMode        = Mode.Rock;

    /** The number of steps for an animation. */
    private int stepCount            = 0;

    /** The current step. */
    private int step                 = 0;

    /** The command to execute every animation step. */
    private String animationCommand  = null;

    /** The MoleculeViewer we are animating. */
    private MoleculeViewer moleculeViewer = null;

    /** The MoleculeViewer. */
    public void setMoleculeViewer(MoleculeViewer mv){
	moleculeViewer = mv;
    }

    /** Set the current mode. */
    public void setMode(Mode v){
	animationMode = v;
    }

    /** Set the sleep duration. */
    public void setSleepDuration(int sd){
	sleepDuration = sd;
    }

    /** Get the sleep duration. */
    public int getSleepDuration(){
	return sleepDuration;
    }

    /** Set the rock angle. */
    public void setRockAngle(double a){
	rockAngle = a;
    }

    /** Set the rock rate. */
    public void setRockRate(double r){
	deltaAngle = r;
    }

    /** Set the number of steps. */
    public void setSteps(int s){
	stepCount = s;
	step = 0;
    }

    /** Get the number of steps in this animation. */
    public int getSteps(){
	return stepCount;
    }

    /** Set the command. */
    public void setCommand(String c){
	animationCommand = c;
    }

    /** Start center. */
    private double sx = 0.0, sy = 0.0, sz = 0.0, sr = 0.0, scf = 0.0, scb = 0.0;

    /** Finish center. */
    private double fx = 0.0, fy = 0.0, fz = 0.0, fr = 0.0, fcf = 0.0, fcb = 0.0;

    /** Start matrix. */
    private Matrix startMatrix = null;

    /** Finish matrix. */
    private Matrix finishMatrix = null;

    /** Set the start view. */
    public void setStartCenter(double x, double y, double z, double r,
			       double cf, double cb){
	sx = x; sy = y; sz = z; sr = r; scf = cf; scb = cb;
    }

    /** Set the start view. */
    public void setFinishCenter(double x, double y, double z, double r,
				double cf, double cb){
	fx = x; fy = y; fz = z; fr = r; fcf = cf; fcb = cb;
    }

    /** Set the start matrix for view change. */
    public void setStartMatrix(Matrix ms){
	startMatrix = new Matrix(ms);
    }

    /** Set the start matrix for view change. */
    public void setFinishMatrix(Matrix ms){
	finishMatrix = new Matrix(ms);
    }

    private double currentAngle = 0.0;

    private double deltaAngle = 5.0;

    private double lastRock = 0.0;

    /** Initialise the animation object. */
    public void initialise(){
	step = 0;
    }

    /** Execute the actual animation function. */
    public boolean executeAnimationFunction(){

	if(step == stepCount){
	    return false;
	}

	//FILE.out.print("step %3d/", step);
	//FILE.out.print("%03d\n", stepCount);

	boolean retCode = animate();

	step++;

	return retCode;
    }

    boolean interactive = false;

    /** Do the step. */
    public boolean animate(){
	MoleculeRenderer moleculeRenderer = moleculeViewer.getMoleculeRenderer();
        interactive = moleculeViewer.interactiveAnimation();

	if(animationMode == Mode.Rock){
	    if(step == 0){
		currentAngle = 0.0;
		lastRock = 0.0;
		deltaAngle = 360.0/(double)stepCount;
	    }

	    currentAngle += deltaAngle * Math.PI/180.0;

	    double multiplier = Math.sin(currentAngle);

	    if(currentAngle < Math.PI*0.5){
		multiplier *= multiplier;
	    }else if(currentAngle > Math.PI * 1.5){
		multiplier *= -multiplier;
	    }

	    double disp = rockAngle * (Math.PI/180.0) * multiplier;

	    double diff = disp - lastRock;

	    diff *= 180.0 / Math.PI;

	    FILE.out.print("diff %.3f\n", diff);
	    FILE.out.print("curr %.3f\n", currentAngle*180.0/Math.PI);

	    String command = String.format("view -rotatez %.3f;", diff);

	    moleculeRenderer.execute(command);

	    lastRock = disp;
	}else if(animationMode == Mode.MoleculeAnimation){

	}else if(animationMode == Mode.Command){

	}else if(animationMode == Mode.Roll){
	    if(step == 0){
		deltaAngle = rockAngle/(double)stepCount;
                if(!interactive){
                    System.out.println("delta " + deltaAngle);
                }
	    }

            if(!interactive){
                Log.info("step      %d", step);
                Log.info("stepCount %d", stepCount);
            }

	    String command = String.format("view -rotatez %g;", deltaAngle);

	    moleculeRenderer.execute(command);

	}else if(animationMode == Mode.Recenter){
	    if(step == 0){
		Point3d center = moleculeRenderer.renderer.getCenter();
		double r       = moleculeRenderer.renderer.getRadius();
                if(!interactive){
                    System.out.println("###### step is 0 center is " + center);
                    System.out.println("###### step is 0 radius is " + r);
		}

		setStartCenter(center.x, center.y, center.z, r,
			       moleculeRenderer.renderer.front,
			       moleculeRenderer.renderer.back);

                if(finishMatrix != null){
                   setStartMatrix(moleculeRenderer.renderer.rotationMatrix);
		}
	    }

	    double frac = step / (double)(stepCount - 1);

	    double cx = sx + frac * (fx - sx);
	    double cy = sy + frac * (fy - sy);
	    double cz = sz + frac * (fz - sz);
	    double cr = sr + frac * (fr - sr);
	    double clipf = scf + frac * (fcf - scf);
	    double clipb = scb + frac * (fcb - scb);

	    final StringBuilder command = new StringBuilder("center");
	    command.append(String.format(" %g", cx));
	    command.append(String.format(" %g", cy));
	    command.append(String.format(" %g; ", cz));

	    command.append(String.format("radius %g;", cr));

	    command.append(String.format("clip %g ", clipf));
	    command.append(String.format("%g;", clipb));
	    
	    if(startMatrix != null){
		Matrix m = Matrix.interpolate(startMatrix, finishMatrix, frac);
                
                if(!interactive){
                    System.out.println(m.returnScript());
                }

		command.append(m.returnScript());
	    }

            if(!interactive){
                System.out.println("command |" + command +"|");
            }

	    moleculeRenderer.execute(command.toString());

	}else{
	    System.out.println("executeAnimationFunction: unknown animation mode " +
			       animationMode);
	}

	if(animationCommand != null){
	    String cmd = processCommand(animationCommand);

	    moleculeRenderer.execute(cmd);
	}

	return true;
    }

    /** Introduce variables etc. */
    public String processCommand(String s){
	int len = s.length();
	StringBuilder sb = new StringBuilder(len);

	for(int i = 0; i < len; i++){
	    char c = s.charAt(i);

	    if(c == '$'){
		i++;
		c = s.charAt(i);

		if(c == '{'){
		    int start = i + 1;
		    int end = -1;
		    do {
			i++;
			c = s.charAt(i);
		    } while(c != '}');

		    end = i;

		    //System.out.println("value " + s.substring(start, end));

		    String value = s.substring(start, end);

		    String tokens[] = FILE.split(value, ",");

		    double startValue = 0.0;
		    double stopValue  = 0.0;

		    if(tokens.length == 1){
			startValue = 0.0;
			stopValue  = FILE.readDouble(tokens[0]);
		    }else if(tokens.length == 2){
			startValue = FILE.readDouble(tokens[0]);
			stopValue  = FILE.readDouble(tokens[1]);
		    }

		    double frac = (double)step/(double)(stepCount - 1);

		    double val = startValue + frac * (stopValue - startValue);

		    i++;
		    c = s.charAt(i);

		    if(c == 'd'){
			sb.append(String.format("%d", (long)val));
		    }else{
			sb.append(String.format("%g", val));
		    }
                }
	    }else{
		sb.append(c);
	    }
	}
	
        if(!interactive){
            System.out.println("command |" + sb.toString() + "|");
        }
	return sb.toString();
    }
}
