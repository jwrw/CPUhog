/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jwatson.fastmatrix;

import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class representing a dense r x r matrices of double values.
 * <p>
 * The design aim is efficiency rather than OO elegance.
 * <p>
 * The data[][] array is exposed to permit direct manipulation, although it
 * is expected that this should not be used outside of this class.
 * <p>
 * Many of the methods act directly on the matrix and return a reference.  
 * Use the clone() method to keep the original matrix intact.  (r.e. methods 
 * don't usually make a copy unless there is a good reason why a copy
 * has to be made - e.g. the result is different size)
 * 
 * @author jim
 */
public class Matrix
        implements Cloneable {

    public static class SingluarMatrixException extends Exception {

        public SingluarMatrixException() {
            super("Matrix is singular and cannot be inverted");
        }
    }

    public static class NoRealSolutionException extends Exception {

        public NoRealSolutionException(String hint) {
            super("No solution found" + hint);
        }
    }
    /**
     * The storage of the matrix data
     */
    private double data[][];

    /**
     * Exception raised to indicate something is wrong with
     * the dimensions of the matrices or vector.
     * <p>
     * The dimension of the matrix are more normally known at coding time
     * r.e. it's more likley to be a coding error than a runtime problem.
     */
    public static class DimensionError extends Error {

        public DimensionError(Matrix target, Matrix m) {
            super(String.format(
                    "Passed matrix (%d,%d) cannot operate on this matrix (%d,%d)",
                    m.rows(), m.columns(), target.rows(), target.columns()));
        }

        public DimensionError(Matrix target) {
            super(String.format(
                    "Cannot perform operation on matrix with dimensions (%d,%d)",
                    target.rows(), target.columns()));
        }
    }

    /**
     * Create a square matrix filled with zeros
     * @param size 
     */
    public Matrix(int size) {
        this(size, size);
    }

    /**
     * Create an arbitrary sized matrix filled with zeros
     * @param rows
     * @param columns
     */
    public Matrix(int rows, int columns) {
        data = new double[rows][columns];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                data[r][c] = 0;
            }
        }
    }

    /**
     * Create a matrix from some data
     * <p>
     * Data is not copied so changes to it will appear in the matrix and vice versa.
     * @param data
     */
    public Matrix(double[][] data) {
        setData(data);
    }

    /**
     * Create an identity matrix (square matrix of zeros with ones 
     * on leading diagonal).
     *
     * @param size Number or rows and columns (identity matrices must be square).
     * @return An identity matrix of the requested size
     */
    public static Matrix createIdentityMatrix(int size) {
        Matrix m = new Matrix(size);

        for (int i = 0; i < size; i++) {
            m.data[i][i] = 1;
        }

        return m;
    }

    /**
     * Create a matrix with a single column containing the data from an array.
     * <p>
     * The data is copied into the matrix and is therefore unaffected by 
     * any subsequent change to the array data.
     * @param array
     * @return
     */
    public static Matrix createColumnMatrix(double[] array) {
        Matrix m = new Matrix(array.length, 1);

        for (int i = 0; i < array.length; i++) {
            m.data[i][0] = array[i];
        }
        return m;
    }

    /**
     * Create a matrix with a single row containing the data from an array.
     * <p>
     * The resultant matrix uses the data in the array and therefore  
     * any subsequent change to the array data will affect the matrix also.
     * @param array
     * @return
     */
    public static Matrix createRowMatrix(double[] array) {
        Matrix m = new Matrix(1, array.length);

        m.data[0] = array;

        return m;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Object o = super.clone();
        Matrix mo = ((Matrix) o);
        mo.data = new double[rows()][];
        for (int i = 0; i < rows(); i++) {
            mo.data[i] = data[i].clone();
        }
        return o;
    }

    /**
     * A efficient way to get the data out of the matrix
     * <p>
     * The data is ordered as data[rows][columns]
     * <p>
     * Note this is not a copy, so changes to the returned array will affect
     * the matrix.
     * @return An array representing the matrix data.
     */
    public double[][] getData() {
        return data;
    }

    /**
     * Set the data to the array ordered as data[rows][columns].
     * This will resize to Matrix according to the passed in data.
     * <p>
     * Setting data to null may cause subsequent methods to fail.
     * <p>
     * The data is not copied so subsequent changes in the array will affect the
     * matrix.
     * @param data Data to place in the matrix
     */
    public void setData(double[][] data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        int hash = 7 * rows() + 11 * columns();
        if (this.data != null) {
            for (int r = 0; r < rows(); r++) {
                for (int c = 0; c < columns(); c++) {
                    hash = hash * 31 + Double.valueOf(data[r][c]).hashCode();
                }
            }
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Matrix other = (Matrix) obj;
        if (this.data == other.data) {
            return true;
        }
        if (this.data == null || other.data == null) {
            return false;
        }
        if (this.rows() != other.rows() || this.columns() != other.columns()) {
            return false;
        }
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < columns(); c++) {
                if (this.data[r][c] != other.data[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Number of columns in matrix
     * @return Number of columns
     */
    public int columns() {
        return (data.length == 0) ? 0 : data[0].length;
    }

    /**
     * Number of rows in matrix
     * @return Number of rows
     */
    public int rows() {
        return data.length;
    }

    /**
     * Adds a matrix to this matrix.
     * <p>
     * The added matrix must be the same size as this or an error is thrown.
     * @param m
     * @return
     * @throws com.jwatson.fastmatrix.Matrix.DimensionError 
     */
    public Matrix add(Matrix m) throws DimensionError {
        if (m.rows() != this.rows() || m.columns() != this.columns()) {
            throw new DimensionError(this, m);
        }

        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < columns(); c++) {
                data[r][c] += m.data[r][c];
            }
        }
        return this;
    }

    /**
     * Creates a new matrix by post multiplying this matrix by m
     * <p>
     * As generally the result is of different dimensions to the two original
     * matrices, a new matrix is created and returned.
     * <p>
     * I.e. this matrix and matrix m are not changed
     * @param m Matrix to post-multiply this matrix by
     * @return New matrix = this * m
     * @throws com.jwatson.fastmatrix.Matrix.DimensionError
     */
    public Matrix postMultiply(Matrix m) throws DimensionError {
        if (m.rows() != this.columns()) {
            throw new DimensionError(this, m);
        }

        Matrix result = new Matrix(this.rows(), m.columns());

        double sum;
        for (int r = 0; r < this.rows(); r++) {
            for (int c = 0; c < m.columns(); c++) {
                sum = 0;
                for (int rc = 0; rc < this.columns(); rc++) {
                    sum += this.data[r][rc] * m.data[rc][c];
                }
                result.data[r][c] = sum;
            }
        }

        return result;
    }

    /**
     * Creates a new matrix by pre multiplying this matrix by m
     * <p>
     * Implemented via call to postMultiply
     * @param m Matrix to pre-multiply this matrix by
     * @return New matrix = m * this
     * @throws com.jwatson.fastmatrix.Matrix.DimensionError
     */
    public Matrix preMultiply(Matrix m) throws DimensionError {
        return m.postMultiply(this);
    }

    /**
     * Subtracts a matrix from this matrix.
     * <p>
     * The subtracted matrix must be the same size as this or an error is thrown.
     * @param m
     * @return A reference to this matrix after m has been subtracted
     * @throws com.jwatson.fastmatrix.Matrix.DimensionError If sizes do not match.
     */
    public Matrix subtract(Matrix m) throws DimensionError {
        if (m.rows() != this.rows() || m.columns() != this.columns()) {
            throw new DimensionError(this, m);
        }

        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < columns(); c++) {
                data[r][c] -= m.data[r][c];
            }
        }
        return this;
    }

    /**
     * Multiplies the matrix by a scalar
     * 
     * @param scalar
     * @return A reference to the matrix
     */
    public Matrix multiply(double scalar) {
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < columns(); c++) {
                data[r][c] *= scalar;
            }
        }
        return this;
    }

    /**
     * Inverts this matrix.
     * <p>
     * The values in the original matrix are preserved.
     * @return A new matrix that is the inverse of this matrix
     * @throws com.jwatson.fastmatrix.Matrix.SingluarMatrixException If the matrix
     * can't be inverted.
     */
    public Matrix inverse() throws SingluarMatrixException {
        Matrix work = null;
        try {
            work = (Matrix) this.clone();
        } catch (CloneNotSupportedException ex) {
            throw new Error("Clone not supported where it should be");
        }

        return work.inverseInPlace();
    }

    /**
     * Inverts this matrix using its contents as work space.
     * <p>
     * The values in the matrix are destroyed and replaced by its inverse.
     * <p>
     * This method is used by the more friendly inverse() method that works
     * on a copy and so does not affect the original matrix.
     * @return A new matrix that is the inverse of this matrix
     * @throws com.jwatson.fastmatrix.Matrix.SingluarMatrixException If the matrix
     * can't be inverted.
     */
    public Matrix inverseInPlace() throws SingluarMatrixException {
        int size = rows();

        // Check for square
        if (rows() < 1 || columns() != rows()) {
            throw new Matrix.DimensionError(this);
        }

        Matrix b = Matrix.createIdentityMatrix(size);

        for (int r = 0; r < size; r++) {
            // find row with biggest value in r'th column for use as pivot
            double mag = 0;
            int bestRow = -1;
            for (int tryRow = r; tryRow < size; tryRow++) {
                double mag2 = Math.abs(data[tryRow][r]);
                if (mag2 > mag) {
                    mag = mag2;
                    bestRow = tryRow;
                }
            }

            // no bestRow (error)
            if (bestRow == -1 || mag == 0) {
                throw new Matrix.SingluarMatrixException();
            }

            // move bestRow row into position
            if (bestRow != r) {
                double[] temp = data[r];
                data[r] = data[bestRow];
                data[bestRow] = temp;

                temp = b.data[r];
                b.data[r] = b.data[bestRow];
                b.data[bestRow] = temp;
            }

            // normalize pivot row
            mag = data[r][r];     //get actual value - not the abs
            for (int c = r; c < size; c++) {
                data[r][c] /= mag;
            }
            for (int c = 0; c < size; c++) {
                b.data[r][c] /= mag;
            }

            // eliminate pivot row component from other rows
            for (int r1 = 0; r1 < size; r1++) {
                if (r1 == r) {
                    continue;
                }
                double mag2 = data[r1][r];

                data[r1][r] = 0;
                for (int c = r + 1; c < size; c++) {
                    data[r1][c] -= mag2 * data[r][c];
                }
                for (int c = 0; c < size; c++) {
                    b.data[r1][c] -= mag2 * b.data[r][c];
                }
            }
        }

        this.data = b.data;
        return this;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("");
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < rows(); c++) {
                str.append(data[r][c]);
                str.append('\t');
            }
            str.append('\n');
        }
        return str.toString();
    }

    /**
     * Computes the determinant of the matrix.
     * <p>
     * This is only valid for square matrices
     * @return
     */
    public double determinant() {
        // TODO check must be square
        int[] cols = new int[rows()];
        for (int i = 0; i < rows(); i++) {
            cols[i] = i;
        }

        return detOfSubmatrix(cols);
    }

    /**
     * Returns the determinant of a submatrix of the matrix.
     * <p>
     * This is not intended to be used directly - most applications will use
     * the public {@link inverse()} method.  This rountine can call itself 
     * recursively with smaller and smaller submatrices to compute a determinant.
     * This minimises the amount of copying of data so should scale effectively
     * for larger matrices.
     * <p>
     * No checks are made and no errors thrown.
     * <p>
     * The submatrix is defined by a list of column indexes.  The cols parameter 
     * is an array that specifies which columns are to be regarded as forming
     * the submatrix.  The submatrix must be square (or it would not be valid to 
     * compute its determinant) and the first n rows of this matrix are used 
     * in the submatrix (where n = cols.length).
     * @param cols
     * @return
     */
    protected double detOfSubmatrix(int[] cols) {
        // First off we have special cases for submatrix size 1 or size 2
        if (cols.length <= 1) {
            return data[0][cols[0]];
        } else if (cols.length == 2) {
            return data[0][cols[0]] * data[1][cols[1]] -
                    data[1][cols[0]] * data[0][cols[1]];
        } else {
            // If the matrix is bigger than 1 or 2 then we compute the determinant
            // by considering submatrices formed by excluding each column in turn
            //
            int[] submatrixCols = new int[cols.length - 1];
            for (int i = 0; i < submatrixCols.length; i++) {
                submatrixCols[i] = cols[i + 1];
            }

            double result = 0.0;
            boolean add = (cols.length % 2) != 0;
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) {
                    submatrixCols[c - 1] = cols[c - 1];
                }
                if (data[cols.length - 1][cols[c]] != 0) {
                    if (add) {
                        result += data[cols.length - 1][cols[c]] * detOfSubmatrix(submatrixCols);
                    } else {
                        result -= data[cols.length - 1][cols[c]] * detOfSubmatrix(submatrixCols);
                    }
                }
                add = !add;
            }

            return result;
        }
    }

    /**
     * Computes the coefficients of a nth degree polynomial passing though the 
     * n+1 points.
     * 
     * @param points
     * @return An array holding the coefficients of the polynomial a0, a1, a2 ...
     * @throws com.jwatson.fastmatrix.Matrix.NoRealSolutionException 
     */
    public static double[] polyThroughPoints(Point2D points[])
            throws NoRealSolutionException {
        try {
            Matrix yValues = new Matrix(points.length, 1);
            Matrix xValues = new Matrix(points.length);

            int r = 0;
            for (Point2D p : points) {
                double x = p.getX();
                xValues.data[r][0] = 1.;
                double v = 1.;
                for (int c = 1; c < points.length; c++) {
                    v *= x;
                    xValues.data[r][c] = v;
                }

                yValues.data[r][0] = p.getY();
                r++;
            }

            xValues.inverseInPlace();

            Matrix result = xValues.postMultiply(yValues);

            return result.getColumnAsArray(0);

        } catch (SingluarMatrixException ex) {
            throw new NoRealSolutionException(" - possibly duplicate points in input");
        }
    }

    /**
     * Get a copy of the data in the specified row.
     * @param row
     * @return The row data as an array.
     */
    public double[] getRowAsArray(int row) {
        if (row < 0 || row >= rows()) {
            throw new Error("Invalid row number: " + row);
        }

        double result[] = new double[columns()];
        for (int c = 0; c < columns(); c++) {
            result[c] = data[row][c];
        }
        return result;
    }

    /**
     * Get a copy of the data in the specified column.
     * @param column
     * @return The column data as an array.
     */
    public double[] getColumnAsArray(int column) {
        if (column < 0 || column >= columns()) {
            throw new Error("Invalid coumn number: " + column);
        }
        double result[] = new double[rows()];
        for (int r = 0; r < rows(); r++) {
            result[r] = data[r][column];
        }
        return result;
    }
}
