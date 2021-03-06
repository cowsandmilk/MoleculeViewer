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

package astex.design;

class Apollo {
    private static final double rd[]   = new double[2];
    private static final double rv[]   = new double[3];
    private static final double xd[][] = new double[3][4];
    private static final double xt[]   = new double[3];
    private static final double xt1[]  = new double[3];
    private static final double xv[][] = new double[3][3];
    private static final int    ip1[]  = { 1, 2, 0};
    private static final int    ip2[]  = { 2, 0, 1};

    public static boolean tangentSphere(double xs[][], double rs[], double xe[]){
	/*
	  PROGRAM APOLLO3D
	  C#### Solve 3-D Apollonius' problem (find sphere internally tangential
	  C#### to 4 given spheres), using Yeates' spherical inversion algorithm
	  C#### (JMB 1995, 249, 804-815).
	  C
	  C#### Example free-format input (centres & radii for the 4 spheres):
	  C       17.865  12.489  19.724  1.7
	  C       19.306  12.704  19.536  1.65
	  C       19.864  12.583  20.965  1.65
	  C       20.429  11.557  21.263  1.55
	  C
	  C#### Example output (2 extra columns are distances between centres of
	  C#### input spheres and tangent spheres & sum of radii which should be
	  C#### equal):
	  C       17.865   12.489   19.724    1.700    1.943    1.943
	  C       19.306   12.704   19.536    1.650    1.893    1.893
	  C       19.864   12.583   20.965    1.650    1.893    1.893
	  C       20.429   11.557   21.263    1.550    1.793    1.793
	  C
	  IMPLICIT NONE
	  LOGICAL LE
	  INTEGER I, IL, IM, IS, JS
	  REAL A, A1, A2, B, B1, B2, C, RT, RT1, S, T
	  C
	  INTEGER IP1(3), IP2(3)
	  REAL RD(2), RS(4), RV(3), XD(3,2), XS(3,4), XT(3), XT1(3), XV(3,3)
	  DATA IP1,IP2 /2,3,1,3,1,2/
	  
	  C
	  C#### Read the sphere centres & radii.
	  READ *,((XS(I,IS),I=1,3),RS(IS),IS=1,4)
	  C
	  C#### Which sphere has the smallest radius?
	  IL = 1
	  DO IS = 2,4
	  IF (RS(IS).LT.RS(IL)) IL = IS
	  ENDDO
	*/

	int il = 0;
	for(int is = 1; is < 4; is++){
	    if(rs[is] < rs[il]) il = is;
	}

	/*
	  C
	  C#### Invert each of the other spheres.
	*/
	//JS = 0
	int js = 0;

	//System.out.println("il "  + il);

	//DO IS = 1,4
        //IF (IS.NE.IL) THEN
	for(int is = 0; is < 4; is++){
	    if(is != il){
		//C
		//C#### Subtract the radius of the smallest sphere.
		//          JS = JS+1
		//          RV(JS) = RS(IS)-RS(IL)
		rv[js] = rs[is] - rs[il];
		//C
		//C#### Scale factor for spherical inversion through a unit sphere centred
		//C#### on the smallest sphere (i.e. the inversion sphere).
		//          S = -RV(JS)**2
		double s = -rv[js]*rv[js];
		//C
		//C#### Centre of input sphere relative to centre of inversion sphere.
		//          DO I = 1,3
		//            XV(I,JS) = XS(I,IS)-XS(I,IL)
		//            S = S+XV(I,JS)**2
		//          ENDDO
		//          IF (S.LT.1E-6) STOP'ERROR: No solution!'

		for(int i = 0; i < 3; i++){
		    xv[i][js] = xs[i][is] - xs[i][il];
		    s += xv[i][js]*xv[i][js];
		}
		if(s < 1.e-6){
		    //no solution
		    return false;
		}
		//C
		//C#### Radius & centre of inverted sphere.
		//          RV(JS) = RV(JS)/S
		//          DO I = 1,3
		//            XV(I,JS) = XV(I,JS)/S
		//          ENDDO
		//        ENDIF
		//

		rv[js] /= s;

		for(int i = 0; i < 3; i++){
		    xv[i][js] /= s;
		}
		js++;
	    }
	}
	//      ENDDO
	//C
	//C#### Find the common tangent planes to the 3 inverted spheres.
	//C#### Get differences in radius & centre (1-3) & (2-3).
	//      DO IS = 1,2
	//        RD(IS) = RV(IS)-RV(3)
	//        DO I = 1,3
	//          XD(I,IS) = XV(I,IS)-XV(I,3)
	//        ENDDO
	//      ENDDO

	for(int is = 0; is < 2; is++){
	    rd[is] = rv[is] - rv[2];
	    for(int i = 0; i < 3; i++){
		xd[i][is] = xv[i][is] - xv[i][2];
	    }
	}
	// C
	// C#### Get the intersection vector of the planes (i.e. cross product of
	// C#### the plane normals) & identify largest absolute component.
	//       IM = 1
	//       DO I = 1,3
	//         XT(I) = XD(IP1(I),1)*XD(IP2(I),2)-XD(IP2(I),1)*XD(IP1(I),2)
	//         IF (ABS(XT(I)).GT.ABS(XT(IM))) IM = I
	//       ENDDO
	// C      PRINT '(2(/A,3F9.3))',('XD ',(XD(I,IS),I=1,3),IS=1,2)
	// C      PRINT '(A,3F9.3)','XT ',XT
	//       IF (ABS(XT(IM)).LT.1E-6) STOP 'ERROR: No solution.'

	int im = 0;
	for(int i = 0; i < 3; i++){
	    xt[i] = xd[ip1[i]][0]*xd[ip2[i]][1] - xd[ip2[i]][0] * xd[ip1[i]][1];
	    if(Math.abs(xt[i]) > Math.abs(xt[im])) im = i;
	}

	if(Math.abs(xt[im]) < 1.e-6){
	    //no solution
	    return false;
	}
// C
// C#### Solve in terms of this component.
//       A1 = (XD(IP2(IM),1)*XD(IM,2)-XD(IP2(IM),2)*XD(IM,1))/XT(IM)
//       B1 = (XD(IP2(IM),1)*RD(2)-XD(IP2(IM),2)*RD(1))/XT(IM)
//       A2 = (XD(IP1(IM),2)*XD(IM,1)-XD(IP1(IM),1)*XD(IM,2))/XT(IM)
//       B2 = (XD(IP1(IM),2)*RD(1)-XD(IP1(IM),1)*RD(2))/XT(IM)
	double a1 = (xd[ip2[im]][0]*xd[im][1]-xd[ip2[im]][1]*xd[im][0])/xt[im];
	double b1 = (xd[ip2[im]][0]*rd[1]    -xd[ip2[im]][1]*rd[0])/xt[im];
	double a2 = (xd[ip1[im]][1]*xd[im][0]-xd[ip1[im]][0]*xd[im][1])/xt[im];
	double b2 = (xd[ip1[im]][1]*rd[0]    -xd[ip1[im]][0]*rd[1])/xt[im];

// C
// C#### First solution of quadratic equation for z component of plane.
//       A = A1**2+A2**2+1.
//       B = A1*B1+A2*B2
//       C = B**2-A*(B1**2+B2**2-1.)
// C      PRINT '(A,2F9.3,F12.6)','ABC',A,B,C
//       IF (C.LT.-1E-6) STOP 'ERROR: No solution!'
//       C = SQRT(MAX(C,0.))
//       XT1(IM) = (C-B)/A
	double a = a1*a1+a2*a2+1.;
	double b = a1*b1+a2*b2;
	double c = b*b-a*(b1*b1+b2*b2-1.);

	if(c < -1.e-6){
	    //no solution
	    return false;
	}
	c = Math.sqrt(Math.max(c,0.));
	xt1[im] = (c-b)/a;
// C
// C#### Solve for other components of plane normal.
//       XT1(IP1(IM)) = A1*XT1(IM)+B1
//       XT1(IP2(IM)) = A2*XT1(IM)+B2
	xt1[ip1[im]] = a1*xt1[im]+b1;
	xt1[ip2[im]] = a2*xt1[im]+b2;
// C
// C#### Distance of plane from centre of inversion sphere.
//       RT1 = 0.
//       DO I = 1,3
//         RT1 = RT1+XT1(I)*XV(I,1)
//       ENDDO
	double rt1 = rv[0];
	for(int i = 0; i < 3; i++){
	    rt1 += xt1[i] * xv[i][0];
	}
// C      PRINT '(A,3F8.3,F12.6)','XT1',XT1,RT1
// C
// C#### Repeat for second solution of quadratic.
//       XT(IM) = -(C+B)/A
//       XT(IP1(IM)) = A1*XT(IM)+B1
//       XT(IP2(IM)) = A2*XT(IM)+B2
// C
//       RT = 0.
//       DO I = 1,3
//         RT = RT+XT(I)*XV(I,1)
//       ENDDO
	xt[im] = -(c+b)/a;
	xt[ip1[im]] = a1*xt[im]+b1;
	xt[ip2[im]] = a2*xt[im]+b2;
	
	double rt = rv[0];
	for(int i = 0; i < 3; i++){
	    rt += xt[i] * xv[i][0];
	}

	//C      PRINT '(A,3F8.3,F12.6)','XT2',XT,RT
	//      IF (RT.LT.-1E-6 .AND. RT1.LT.-1E-6) STOP 'ERROR: No solution!'
	//C

	if(rt < 1.e-6 && rt1 < 1.e-6){
	    //no solution
	    return false;
	}
// C#### Correct solution has the smallest positive distance from origin.
//       IF (RT.LT.-1E-6 .OR. RT1.GE.-1E-6 .AND. RT1.LT.RT) THEN
//         RT = RT1
//         DO I = 1,3
//           XT(I) = XT1(I)
//         ENDDO
//       ENDIF
	if(rt < 1.e-6 || (rt1 >= 1.e-6 && rt1 < rt)){
	    rt = rt1;
	    System.arraycopy(xt1, 0, xt, 0, 3);
	}

// C
// C#### Finally invert the tangent plane back to the tangent sphere.
//       RT = .5/(MAX(RT,0.)+RV(1))
//       DO I = 1,3
//         XT(I) = XS(I,IL)+RT*XT(I)
//       ENDDO
//       RT = RT-RS(IL)
//       PRINT '(/A,4F9.3/)','Centre & radius of tangent sphere:',XT,RT

	//rt = 0.5/(Math.max(rt, 0.) + rv[0]);
	rt = 0.5 / rt;

	for(int i = 0; i < 3; i++){
	    xe[i] = xs[i][il] + rt * xt[i];
	}
	rt -= rs[il];

	xe[3] = rt;
	return true;
    }
// C
// C#### Check solution by comparing distances between centre of each input
// C#### sphere and that of tangent sphere with sum of radii.
//       LE = .FALSE.
//       DO IS = 1,4
//         S = 0.
//         DO I = 1,3
//           S = S+(XT(I)-XS(I,IS))**2
//         ENDDO
//         S = SQRT(S)
//         T = RS(IS)+RT
//         PRINT '(6F9.3)',(XS(I,IS),I=1,3),RS(IS),S,T
//         IF (ABS(S-T).GT..001*T) LE = .TRUE.
//       ENDDO
//       IF (LE) STOP 'ERROR: Sphere not tangential.'
//       END
}