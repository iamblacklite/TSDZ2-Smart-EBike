import java.util.Locale;
import java.util.Scanner;

import java.io.*;
  
class calc {
    public static void main(String[] args)
    {
        Scanner input = new Scanner(System.in);
    	for (int i = 0; i < 4; i++) {
            System.out.print("Enter ERPM for cycle" + i);
            int x = input.nextInt();
            for (int j = 0; j < 6; j++) {
                System.out.print("Enter data for cycle" + i);
                System.out.print("Enter count for sensor" + j);
                py[j][i] = input.nextInt();
                px[j][i] = x;
            }
        }

        // closing the scanner object
        input.close();
  
        updateResult();

    }
}

public class HallCalibrationActivity {

    private final double[][] px = new double[6][4];
    private final double[][] py = new double[6][4];
    private final LinearRegression[] linearRegression = new LinearRegression[6];

    private final int[] phaseAngles = new int[6];
    private final int[] hallTOffset = new int[6];
   

    /*
    * F1=Hall1 Tfall, R1=Hall1 TRise, F2=Hall2 TFall etc..
    * The 6 linear equations calculated with the linear regression are dependant and have
    * infinite solutions.
    * Lets set F1=0 and then calculate all the other values 6 times for each equation
    * combination and then calculate the average for each value.
    * At the end add an offset to all values so that the total average value is TSDZConst.DEFAULT_AVG_OFFSET.
    */
    private void calculateOffsets() {
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

        double avgOffset = 0;
        for (int i=0; i<6; i++)
            avgOffset += result[i];
        avgOffset /= 6;
        double diffOffset = TSDZConst.DEFAULT_AVG_OFFSET - avgOffset;
        for (int i=0; i<6; i++) {
            result[i] += diffOffset;
        }

        hallTOffset[0] = (int)Math.round(result[4]); // R2 - delay for Hall state 6
        hallTOffset[1] = (int)Math.round(result[0]); // F1 - delay for Hall state 2
        hallTOffset[2] = (int)Math.round(result[5]); // R3 - delay for Hall state 3
        hallTOffset[3] = (int)Math.round(result[1]); // F2 - delay for Hall state 1
        hallTOffset[4] = (int)Math.round(result[3]); // R1 - delay for Hall state 5
        hallTOffset[5] = (int)Math.round(result[2]); // F3 - delay for Hall state 4
    }

    private void calculateAngles() {
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
        error /= 6D;

        // Calculate the Phase reference angles applying the error correction and the absolute
        // reference position correction.
        for (int i=0; i<6; i++) {
            // add 30 degree and subtract the calculated error
            calcRefAngles[i] = calcRefAngles[i] - error + 256D/12D;
            // Add rotor offset and subtract phase angle (90 degree)
            int v = (int)Math.round(calcRefAngles[i]) + 4 - 64;
            if (v<0) v += 256;
            phaseAngles[i] = v;
        }
    }

    private void updateResult() {
        for (int i=0; i<6; i++) {
            linearRegression[i] = new LinearRegression(px[i], py[i]);
        }

        calculateAngles();
        calculateOffsets();
    }
}

public class LinearRegression {
    private final double intercept, slope;
    private final double r2;
    private final double svar0, svar1;

    /**
     * Performs a linear regression on the data points {@code (y[i], x[i])}.
     *
     * @param  x the values of the predictor variable
     * @param  y the corresponding values of the response variable
     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
     */
    public LinearRegression(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.length;

        // first pass
        double sumx = 0.0, sumy = 0.0;
        for (int i = 0; i < n; i++) {
            sumx  += x[i];
            sumy  += y[i];
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }
        slope  = xybar / xxbar;
        intercept = ybar - slope * xbar;

        // more statistical analysis
        double rss = 0.0;      // residual sum of squares
        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < n; i++) {
            double fit = slope*x[i] + intercept;
            rss += (fit - y[i]) * (fit - y[i]);
            ssr += (fit - ybar) * (fit - ybar);
        }

        int degreesOfFreedom = n-2;
        r2    = ssr / yybar;
        double svar  = rss / degreesOfFreedom;
        svar1 = svar / xxbar;
        svar0 = svar/n + xbar*xbar*svar1;
    }

    /**
     * Returns the <em>y</em>-intercept &alpha; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
     *
     * @return the <em>y</em>-intercept &alpha; of the best-fit line <em>y = &alpha; + &beta; x</em>
     */
    public double intercept() {
        return intercept;
    }

    /**
     * Returns the slope &beta; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
     *
     * @return the slope &beta; of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>
     */
    public double slope() {
        return slope;
    }

    /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>,
     *         which is a real number between 0 and 1
     */
    public double R2() {
        return r2;
    }

    /**
     * Returns the standard error of the estimate for the intercept.
     *
     * @return the standard error of the estimate for the intercept
     */
    public double interceptStdErr() {
        return Math.sqrt(svar0);
    }

    /**
     * Returns the standard error of the estimate for the slope.
     *
     * @return the standard error of the estimate for the slope
     */
    public double slopeStdErr() {
        return Math.sqrt(svar1);
    }
}

public class Cramer {

    double [][]A;
    double []B;
    int size;

    public Cramer() {
        A = null;
        B = null;
        size = 0;
    }

    public Cramer (double[][] m2, double[] x) {
        this.A = m2;
        this.B = x;
        this.size = x.length;
    }

    public double[] solve () {
        double [][]tempMatrix = new double[size][size];
        double []x = new double[size];
        double detCohef = this.calculateCoeficientsMatrixDeterminant();
        for (int i=0;i<size;i++) {
            for (int k=0;k<size;k++) {
                for (int l=0;l<size;l++) {
                    tempMatrix[k][l] = A[k][l];
                }
            }
            for (int k=0;k<size;k++) {
                tempMatrix[k][i] = B[k];
            }
            double det = this.calculateDeterminant(tempMatrix, size);
            x[i] = det/detCohef;
        }
        return x;
    }

    public double calculateCoeficientsMatrixDeterminant() {
        return calculateDeterminant(this.A, this.size);
    }

    public double calculateDeterminant (double [][]m, int sizeM ) {
        if ( sizeM == 1 ) {
            return m[0][0];
        } else if ( sizeM == 2 ) {
            // You should save a lot of recursive calls by calculating
            // a 2x2 determinant here
            return m[0][0] * m[1][1] - m[0][1] * m[1][0];
        } else {
            double sum = 0.0;
            int multiplier = -1;
            for (int i=0; i<sizeM;i++) {
                multiplier = multiplier == -1 ? 1 : -1;
                double [][]nM = this.copyMatrix(m, sizeM, i);
                double det = multiplier * m[0][i] * calculateDeterminant(nM, sizeM-1);
                sum += det;
            }
            return sum;
        }
    }

    public double[][] copyMatrix (double [][]m, int size, int col) {
        int sizeM = size -1;
        double [][]result = new double[sizeM][sizeM];
        int nI = 0;
        for (int i=1; i<size;i++) {
            int nJ = 0;
            for (int j=0; j<size;j++) {
                if ( j != col) {
                    result[nI][nJ] = m[i][j];
                    nJ++;
                }
            }
            nI++;
        }
        return result;
    }
}
