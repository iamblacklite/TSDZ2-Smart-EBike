import java.util.Scanner;

import java.io.*;

public class HallCalibrationActivity {

    private static double[][] px = new double[6][4];
    private static double[][] py = new double[6][4];
    private static LinearRegression[] linearRegression = new LinearRegression[6];
    private static int[] hallTOffset = new int[6];
    private static int[] phaseAngles = new int[6];
    private static int initialAngleOffset;
    private static int initialTimeOffset;
   
    public static void main(String[] args)
    {
        Scanner input = new Scanner(System.in);
            System.out.println("_______________");
            System.out.println("Enter initial Angle Offset: ");
            initialAngleOffset = input.nextInt();
            System.out.println("Enter initial Hall Time Offset: ");
            initialTimeOffset = input.nextInt();
    	for (int i = 0; i < 4; i++) {
            System.out.println("_______________");
            System.out.println("Cycle " + i);
            for (int j = 0; j < 6; j++) {
                System.out.print("Enter count for sensor " + j + ": ");
                py[j][i] = input.nextInt();
            }
            int x = 0;
            for (int j = 0; j < 6; j++) {
                x += py[j][i];
            }
            for (int j = 0; j < 6; j++) {
                px[j][i] = x;
            }
        }

        // closing the scanner object
        input.close();
  
        updateResult();

        System.out.println("_______________");

        for (int i = 0; i < 6; i++) {
            double x = linearRegression[i].R2();
            System.out.println("Linear Regression " + i + " R value " + x);
        }
        
        System.out.println("_______________");

        for (int i = 0; i < 6; i++) {
            System.out.println("Hall Angle " + i + ": " + phaseAngles[i]);
        }
        
        System.out.println("_______________");
        
        for (int i = 0; i < 6; i++) {
            System.out.println("Toffset " + i + ": " + hallTOffset[i]);
        }

    }

    /*
    * F1=Hall1 Tfall, R1=Hall1 TRise, F2=Hall2 TFall etc..
    * The 6 linear equations calculated with the linear regression are dependant and have
    * infinite solutions.
    * Lets set F1=0 and then calculate all the other values 6 times for each equation
    * combination and then calculate the average for each value.
    * At the end add an offset to all values so that the total average value is TSDZConst.DEFAULT_AVG_OFFSET.
    */
    private static void calculateOffsets() {
        final double[][] A1 = {
                { 0, 0,-1, 0, 1, 0}, // -F3 + R2 = linearRegression[0].intercept();
                { 1, 0, 0, 0,-1, 0}, //  F1 - R2 = linearRegression[1].intercept();
                {-1, 0, 0, 0, 0, 1}, // -F1 + R3 = linearRegression[2].intercept();
                { 0, 1, 0, 0, 0,-1}, //  F2 - R3 = linearRegression[3].intercept();
                { 0,-1, 0, 1, 0, 0}, // -F2 + R1 = linearRegression[4].intercept();
                { 0, 0, 1,-1, 0, 0}  //  F3 - R1 = linearRegression[5].intercept();
        };
        final double[] Ak = {1,0,0,0,0,0}; // F1 = 0

        double[][] A = new double[6][];
        double[] b = new double[6];

        double[] result = {0,0,0,0,0,0};

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (j == i) {
                    A[j] = Ak;
                    b[j] = 0;
                } else {
                    A[j] = A1[j];
                    b[j] = linearRegression[j].intercept();
                }
            }
            Cramer cramer = new Cramer(A, b);
            double[] solution = cramer.solve();
            for (int k = 0; k < solution.length; k++) {
                result[k] += solution[k];
            }
        }

        for (int i=0; i<6; i++)
            result[i] = result[i] / 6;
/*
        double avgOffset = 0;
        for (int i=0; i<6; i++)
            avgOffset += result[i];
        avgOffset /= 6;
        double diffOffset = 34 - avgOffset; */
        for (int i=0; i<6; i++) {
            result[i] += initialTimeOffset;
        }

        hallTOffset[0] = (int)Math.round(result[4]); // R2 - delay for Hall state 6
        hallTOffset[1] = (int)Math.round(result[0]); // F1 - delay for Hall state 2
        hallTOffset[2] = (int)Math.round(result[5]); // R3 - delay for Hall state 3
        hallTOffset[3] = (int)Math.round(result[1]); // F2 - delay for Hall state 1
        hallTOffset[4] = (int)Math.round(result[3]); // R1 - delay for Hall state 5
        hallTOffset[5] = (int)Math.round(result[2]); // F3 - delay for Hall state 4
    }

    private static void calculateAngles() {
        double[] calcRefAngles = {0d,0d,0d,0d,0d,0d};
        double value = 0;
        double error = 0;
        // Calculate the Hall incremental angles setting Hall state 6 = 0 degrees.
        // Calculate the average offset error in regard to the reference positions.
        for (int i=1; i<6; i++) {
            value += linearRegression[i].slope()*256D;
            error += value - ((double)i*256D/6D);
            calcRefAngles[i] = value;
        }

        // average error
        error /= 6D;

        // Calculate the Phase reference angles applying the error correction and the absolute
        // reference position correction.
        for (int i=0; i<6; i++) {
            // add 30 degree and subtract the calculated error
            calcRefAngles[i] = calcRefAngles[i] - error + 256D/12D;
            // Add rotor offset and subtract phase angle (90 degree)
            int v = (int)Math.round(calcRefAngles[i]) + initialAngleOffset - 64;
            if (v<0) v += 256;
            phaseAngles[i] = v;
        }
    }

    private static void updateResult() {
        for (int i=0; i<6; i++) {
            linearRegression[i] = new LinearRegression(px[i], py[i]);
        }

        calculateAngles();
        calculateOffsets();
    }
}
