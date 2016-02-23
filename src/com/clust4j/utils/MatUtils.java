package com.clust4j.utils;

import static com.clust4j.GlobalState.ParallelismConf.ALLOW_AUTO_PARALLELISM;
import static com.clust4j.GlobalState.ParallelismConf.MAX_SERIAL_VECTOR_LEN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.AbstractRealMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.util.Precision;

import com.clust4j.utils.parallel.map.DistributedMatrixMultiplication;

public class MatUtils {
	final static String MAT_DIM_ERR_MSG = "illegal mat dim: ";
	
	/** Size at which to use BlockRealMatrix for multiplication */
	public final static int BLOCK_MAT_THRESH = 1000;
	public final static int MIN_ACCEPTABLE_MAT_LEN = 1;
	
	public static enum Axis {
		ROW, COL
	}
	
	
	/**
	 * Create a boolean matrix
	 * @author Taylor G Smith
	 */
	public static class MatSeries extends Series<boolean[][]> {
		final boolean[][] mat;
		final int m, n;
		
		private MatSeries(double[][] x) {
			checkDims(x);
			m = x.length;
			n = x[0].length;
			mat = new boolean[m][n];
		}
		
		public MatSeries(double[][] x, Inequality in, double val) {
			this(x);
			
			for(int i = 0; i < m; i++)
				for(int j = 0; j < n; j++)
					mat[i][j] = eval(x[i][j], in, val);
		}
		
		public MatSeries(double[] a, Inequality in, double[][] x) {
			this(x);
			
			if(a.length != n)
				throw new DimensionMismatchException(a.length, n);
			for(int i = 0; i < m; i++)
				for(int j = 0; j < n; j++)
					mat[i][j] = eval(a[j], in, x[i][j]);
		}
		
		@Override
		public boolean[][] get() {
			return copy(mat);
		}
		
		@Override
		public boolean[][] getRef() {
			return mat;
		}
	}
	
	
	
	static enum Operator {
		ADD, DIV, MULT, SUB
	}
	
	final static public void checkMultipliability(final double[][] a, final double[][] b) {
		checkDims(a);
		checkDims(b);
		if(a[0].length != b.length)
			throw new DimensionMismatchException(a[0].length, b.length);
	}
	
	
	
	
	// ========== DIM CHECKS =============
	private static final void dimAssess(int a) { if(a < MIN_ACCEPTABLE_MAT_LEN) throw new IllegalArgumentException(MAT_DIM_ERR_MSG + a); }
	private static final void dimAssessPermitEmpty(int a) { if(a < 0) throw new IllegalArgumentException("illegal dim: " + a); }
	private static final void throwDimException(int a, Throwable npe) { 
		throw new IllegalArgumentException("matrix rows have been initialized, "
			+ "but columns have not, i.e.: new double["+a+"][]", npe); 
	}
	
	
	
	
	/*
	 * For operations that forbid emptiness but permit jaggedness
	 */
	final static public void checkDims(final boolean[][] a) {
		dimAssess(a.length);
		
		// If you try it on a row-initialized matrix but not col-init
		try { VecUtils.checkDims(a[0]); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	final static public void checkDims(final int[][] a) {
		dimAssess(a.length);

		// If you try it on a row-initialized matrix but not col-init
		try { VecUtils.checkDims(a[0]); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	final static public void checkDims(final double[][] a) {
		dimAssess(a.length);

		// If you try it on a row-initialized matrix but not col-init
		try { VecUtils.checkDims(a[0]); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	
	
	
	
	
	/*
	 * For operations that mandate uniformity
	 */
	final static public void checkDimsForUniformity(final boolean[][] a) {
		checkDimsPermitEmpty(a);
		
		final int n = a[0].length;
		for(boolean[] b: a)
			if(b.length != n)
				throw new NonUniformMatrixException(b.length, n);
	}
	
	final static public void checkDimsForUniformity(final int[][] a) {
		checkDimsPermitEmpty(a);
		
		final int n = a[0].length;
		for(int[] i: a)
			if(i.length != n)
				throw new NonUniformMatrixException(i.length, n);
	}
	
	final static public void checkDimsForUniformity(final double[][] a) {
		checkDimsPermitEmpty(a);
		
		final int n = a[0].length;
		for(double[] d: a)
			if(d.length != n)
				throw new NonUniformMatrixException(d.length, n);
	}
	
	
	
	
	
	
	/*
	 * For operations that allow empty rows
	 */
	final static public void checkDimsPermitEmpty(final boolean[][] a) {
		dimAssess(a.length);

		// If you try it on a row-initialized matrix but not col-init
		try { dimAssessPermitEmpty(a[0].length); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	final static public void checkDimsPermitEmpty(final int[][] a) {
		dimAssess(a.length);

		// If you try it on a row-initialized matrix but not col-init
		try { dimAssessPermitEmpty(a[0].length); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	final static public void checkDimsPermitEmpty(final double[][] a) {
		dimAssess(a.length);

		// If you try it on a row-initialized matrix but not col-init
		try { dimAssessPermitEmpty(a[0].length); } 
		catch(NullPointerException npe) { throwDimException(a.length, npe); }
	}
	
	
	
	
	
	
	
	/*
	 * For operations checking for compatability
	 */
	final static public void checkDimsForUniformity(final double[][] a, final double[][] b) {
		checkDimsForUniformity(a);
		checkDimsForUniformity(b);
		
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		if(a[0].length != b[0].length)
			throw new DimensionMismatchException(a[0].length, b[0].length);
	}
	
	final static public void checkDimsForUniformity(final int[][] a, final int[][] b) {
		checkDimsForUniformity(a);
		checkDimsForUniformity(b);
		
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		if(a[0].length != b[0].length)
			throw new DimensionMismatchException(a[0].length, b[0].length);
	}
	
	final static public void checkDimsForUniformity(final boolean[][] a, final boolean[][] b) {
		checkDimsForUniformity(a);
		checkDimsForUniformity(b);
		
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		if(a[0].length != b[0].length)
			throw new DimensionMismatchException(a[0].length, b[0].length);
	}
	
	final static public void checkDims(final double[][] a, final double[][] b) {
		if(a.length == 0 || b.length == 0)
			throw new IllegalArgumentException("row dims are empty");
		checkDimsPermitEmpty(a,b);
	}
	
	final static public void checkDims(final boolean[][] a, final boolean[][] b) {
		if(a.length == 0 || b.length == 0)
			throw new IllegalArgumentException("row dims are empty");
		checkDimsPermitEmpty(a,b);
	}
	
	final static public void checkDims(final int[][] a, final int[][] b) {
		if(a.length == 0 || b.length == 0)
			throw new IllegalArgumentException("row dims are empty");
		checkDimsPermitEmpty(a,b);
	}
	
	final static public void checkDimsPermitEmpty(final double[][] a, final double[][] b) {
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		
		for(int i = 0; i < a.length; i++) {
			try {
				if(a[i].length != b[i].length)
					throw new DimensionMismatchException(a[i].length, b[i].length);
			} catch(NullPointerException npe) {
				throwDimException(a.length, npe);
			}
		}
	}
	
	final static public void checkDimsPermitEmpty(final boolean[][] a, final boolean[][] b) {
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		
		for(int i = 0; i < a.length; i++) {
			try {
				if(a[i].length != b[i].length)
					throw new DimensionMismatchException(a[i].length, b[i].length);
			} catch(NullPointerException npe) {
				throwDimException(a.length, npe);
			}
		}
	}
	
	final static public void checkDimsPermitEmpty(final int[][] a, final int[][] b) {
		if(a.length != b.length)
			throw new DimensionMismatchException(a.length, b.length);
		
		for(int i = 0; i < a.length; i++) {
			try {
				if(a[i].length != b[i].length)
					throw new DimensionMismatchException(a[i].length, b[i].length);
			} catch(NullPointerException npe) {
				throwDimException(a.length, npe);
			}
		}
	}
	
	/*
	 * AbstractRealMatrix won't allow any empty rows
	 */
	final static public void checkDims(final AbstractRealMatrix a) {
		int m = a.getRowDimension(), n = a.getColumnDimension();
		
		if(m < MIN_ACCEPTABLE_MAT_LEN) throw new IllegalArgumentException(MAT_DIM_ERR_MSG + m);
		if(n < MIN_ACCEPTABLE_MAT_LEN) throw new IllegalArgumentException(MAT_DIM_ERR_MSG + n);
	}
	
	final static public void checkDims(final AbstractRealMatrix a, final AbstractRealMatrix b) {
		checkDims(a);
		checkDims(b);
		
		int m1 = a.getRowDimension(), m2 = b.getRowDimension();
		int n1 = a.getColumnDimension(), n2 = b.getColumnDimension();
		
		if(m1 != m2)
			throw new DimensionMismatchException(m1, m2);
		if(n1 != n2)
			throw new DimensionMismatchException(n1, n2);
	}
	
	
	
	
	
	
	// ============= MATH FUNCTIONS ==================
	/**
	 * Compute the absolute value of every element in the matrix.
	 * This method allows for jagged (uneven) matrices.
	 * @param a
	 * @return a copy of the absolute value of the matrix
	 */
	public static final double[][] abs(final double[][] a) {
		checkDimsPermitEmpty(a);
		
		final double[][] b = new double[a.length][];
		for(int i = 0; i < b.length; i++)
			b[i] = VecUtils.abs(a[i]);
		
		return b;
	}
	
	/**
	 * Add two matrices together. This operation demands 
	 * uniformity of the input matrices, but permits matrices with
	 * empty rows to be added together so long as their dimensions match.
	 * @param a
	 * @param b
	 * @throws NonUniformMatrixException if either matrix is jagged
	 * @throws DimensionMismatchException if dimensions of matrices don't match
	 * @return the sum of two matrices
	 */
	public static final double[][] add(final double[][] a, final double[][] b) {
		checkDimsForUniformity(a, b);
		final int m = a.length, n = a[0].length;
			
		final double[][] c = new double[m][n];
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++)
				c[i][j] = a[i][j] + b[i][j];
		return c;
	}
	
	
	/**
	 * Computes the indices of the max along the provided axes.
	 * @param data
	 * @param axis - row or column wise. For {@link Axis#ROW}, returns
	 * the column index of the max for each row; for {@link Axis#COL}, returns
	 * the row index of the max for each column.
	 * @return an array of the indices of the arg max
	 * @throws NonUniformMatrixException if the matrix is non-uniform
	 * @see {@link VecUtils#argMax(double[])}
	 */
	public static int[] argMax(final double[][] data, final Axis axis) {
		return argMaxMin(data, axis, true);
	}
	
	
	/**
	 * Computes the indices of the min along the provided axes.
	 * @param data
	 * @param axis - row or column wise. For {@link Axis#ROW}, returns
	 * the column index of the min for each row; for {@link Axis#COL}, returns
	 * the row index of the min for each column.
	 * @return an array of the indices of the arg min
	 * @throws NonUniformMatrixException if the matrix is non-uniform
	 * @see {@link VecUtils#argMin(double[])}
	 */
	public static int[] argMin(final double[][] data, final Axis axis) {
		return argMaxMin(data, axis, false);
	}
	
	
	/**
	 * Computes either the argMin or the argMax depending on the boolean parameter
	 * @param data
	 * @param axis
	 * @param max - whether to compute the min or max
	 * @return the argMin or argMax vector
	 */
	private static int[] argMaxMin(final double[][] data, final Axis axis, final boolean max) {
		if(data.length == 0)
			return new int[0];
		checkDimsForUniformity(data);
		
		
		int[] out;
		final int m=data.length, n=data[0].length;
		if(axis.equals(Axis.COL)) {
			out = new int[n];
			double[] col;
			for(int i = 0; i < n; i++) {
				col = getColumn(data, i);
				out[i] = max ? VecUtils.argMax(col) : VecUtils.argMin(col);
			}
		} else {
			out = new int[m];
			for(int i = 0; i < m; i++)
				out[i] = max ? VecUtils.argMax(data[i]) : VecUtils.argMin(data[i]);
		}
		
		return out;
	}
	
	
	/**
	 * Compute the column means of a matrix. The matrix must
	 * be uniform in dimensions.
	 * @param data
	 * @throws NonUniformMatrixException if row lengths are non-uniform
	 * @return an array of column means
	 */
	public static double[] colMeans(final double[][] data) {
		return colMeansSums(data, true);
	}
	
	/**
	 * Compute the column sums of a matrix. The matrix must
	 * be uniform in dimensions.
	 * @param data
	 * @throws NonUniformMatrixException if row lengths are non-uniform
	 * @return an array of column sums
	 */
	public static double[] colSums(final double[][] data) {
		return colMeansSums(data, false);
	}
	
	/**
	 * Compute the column means or sums for a matrix
	 * @param data
	 * @param means
	 * @return
	 */
	private static double[] colMeansSums(final double[][] data, boolean means) {
		checkDimsForUniformity(data);
		
		final int n = data[0].length;
		final double[] out = new double[n];
		double[] col;
		for(int i = 0; i < n; i++) {
			col = getColumn(data, i);
			out[i] = means ? VecUtils.mean(col) : 
				VecUtils.sum(col);
		}
		
		return out;
	}
	
	/**
	 * Returns a matrix of complete cases, or rows which do not
	 * contain NaN values.
	 * @param data
	 * @throws IllegalArgumentException if there are no rows in the matrix
	 * @return the complete cases in the matrix
	 */
	public static double[][] completeCases(final double[][] data) {
		checkDimsPermitEmpty(data);
		
		final ArrayList<double[]> rows = new ArrayList<>();
		for(int i = 0; i < data.length; i++)
			if(!VecUtils.containsNaNForceSerial(data[i]))
				rows.add(data[i]);
		
		final double[][] out = new double[rows.size()][];
		for(int i =0; i < out.length; i++)
			out[i] = rows.get(i);
		
		return out;
	}
	
	/**
	 * Returns a matrix of complete cases, or rows which do not
	 * contain NaN values.
	 * @param data
	 * @throws IllegalArgumentException if there are no rows in the matrix
	 * @return the complete cases in the matrix
	 */
	public static double[][] completeCases(final AbstractRealMatrix data) {
		return completeCases(data.getData());
	}
	
	/**
	 * Returns true if there are any NaN values in the matrix.
	 * @throws IllegalArgumentException if there are no rows in the data
	 * @param mat
	 * @return true if the matrix contains NaN
	 */
	public static boolean containsNaN(final double[][] mat) {
		checkDimsPermitEmpty(mat);
		
		final int m = mat.length;
		for(int i = 0; i < m; i++)
			for(int j = 0; j < mat[i].length; j++)
				if(Double.isNaN(mat[i][j]))
					return true;
		
		return false;
	}
	
	/**
	 * Returns true if there are any NaN values in the matrix.
	 * @throws IllegalArgumentException if there are no rows in the data
	 * @param mat
	 * @return true if the matrix contains NaN
	 */
	public static boolean containsNaNDistributed(final double[][] mat) {
		checkDimsPermitEmpty(mat);
		
		final int m = mat.length;
		for(int i = 0; i < m; i++)
			if(VecUtils.containsNaNDistributed(mat[i]))
				return true;
		
		return false;
	}
	
	/**
	 * Returns true if there are any NaN values in the matrix.
	 * @throws IllegalArgumentException if there are no rows in the data
	 * @param mat
	 * @return true if the matrix contains NaN
	 */
	public static boolean containsNaN(final AbstractRealMatrix mat) {
		return containsNaN(mat.getData());
	}
	/**
	 * Returns true if there are any NaN values in the matrix.
	 * @throws IllegalArgumentException if there are no rows in the data
	 * @param mat
	 * @return true if the matrix contains NaN
	 */
	
	public static boolean containsNaNDistributed(final AbstractRealMatrix mat) {
		return containsNaNDistributed(mat.getData());
	}
	
	
	/**
	 * Copy a 2d double array
	 * @param data
	 * @return a copy of the input matrix
	 */
	public static final double[][] copy(final double[][] data) {
		final double[][] copy = new double[data.length][];
		for(int i = 0; i < copy.length; i++)
			copy[i] = VecUtils.copy(data[i]);
		
		return copy;
	}
	
	/**
	 * Copy a 2d boolean array
	 * @param data
	 * @return a copy of the input matrix
	 */
	public static final boolean[][] copy(final boolean[][] data) {
		final boolean[][] copy = new boolean[data.length][];
		for(int i = 0; i < copy.length; i++)
			copy[i] = VecUtils.copy(data[i]);
		
		return copy;
	}
	
	/**
	 * Copy a 2d int array
	 * @param data
	 * @return a copy of the input matrix
	 */
	public static final int[][] copy(final int[][] data) {
		final int[][] copy = new int[data.length][];
		for(int i = 0; i < copy.length; i++)
			copy[i] = VecUtils.copy(data[i]);
		
		return copy;
	}
	
	/**
	 * Extract the diagonal vector from a square matrix
	 * @param data
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws DimensionMismatchException if the row dims do not match the col dims
	 * @return the diagonal vector of a square matrix
	 */
	public static double[] diagFromSquare(final double[][] data) {
		checkDimsForUniformity(data);
		
		final int m = data.length, n = data[0].length;
		if(m!=n)
			throw new DimensionMismatchException(m, n);
		
		final double[] out = new double[n];
		for(int i = 0; i < m; i++)
			out[i] = data[i][i];
		
		return out;
	}
	
	/**
	 * Assess whether every element in the matrices are exactly equal
	 * @param a
	 * @param b
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @throws DimensionMismatchException if the matrix dims don't match
	 * @return true if all equal, false otherwise
	 */
	public static boolean equalsExactly(final double[][] a, final double[][] b) {
		return equalsWithTolerance(a,b,0.0);
	}
	
	/**
	 * Assess whether every element in the matrices are equal within 
	 * a default tolerance of {@value Precision#EPSILON}
	 * @param a
	 * @param b
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @throws DimensionMismatchException if the matrix dims don't match
	 * @return true if all equal, false otherwise
	 */
	public static boolean equalsWithTolerance(final double[][] a, final double[][] b) {
		return equalsWithTolerance(a,b,Precision.EPSILON);
	}
	
	/**
	 * Assess whether every element in the matrices are equal within 
	 * a tolerance
	 * @param a
	 * @param b
	 * @throws IllegalArgumentException if the matrix row dims are empty
	 * @throws DimensionMismatchException if the matrix dims don't match
	 * @return true if all equal, false otherwise
	 */
	public static boolean equalsWithTolerance(final double[][] a, final double[][] b, final double tol) {
		checkDimsPermitEmpty(a, b);
		
		for(int i = 0; i < a.length; i++)
			if(!VecUtils.equalsWithTolerance(a[i], b[i], tol))
				return false;
		return true;
	}
	
	/**
	 * Assess whether every element in the matrices are exactly equal
	 * @param a
	 * @param b
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @throws DimensionMismatchException if the matrix dims don't match
	 * @return true if all equal, false otherwise
	 */
	public static boolean equalsExactly(final int[][] a, final int[][] b) {
		checkDims(a, b);
		
		for(int i = 0; i < a.length; i++)
			if(!VecUtils.equalsExactly(a[i], b[i]))
				return false;
		return true;
	}
	
	/**
	 * Assess whether every element in the matrices are exactly equal
	 * @param a
	 * @param b
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @throws DimensionMismatchException if the matrix dims don't match
	 * @return true if all equal, false otherwise
	 */
	public static boolean equalsExactly(final boolean[][] a, final boolean[][] b) {
		checkDims(a, b);
		
		for(int i = 0; i < a.length; i++)
			if(!VecUtils.equalsExactly(a[i], b[i]))
				return false;
		return true;
	}
	
	/**
	 * Flatten a uniform matrix into a vector of M x N
	 * @param a
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @return a flattened matrix
	 */
	public static double[] flatten(final double[][] a) {
		checkDimsForUniformity(a);
		
		final int m = a.length, n = a[0].length;
		final double[] out = new double[m * n];
		int ctr = 0;
		for(int i = 0; i < m; i++) {
			final double[] row = a[i];
			for(int j = 0; j < n; j++)
				out[ctr++] = row[j];
		}
		
		return out;
	}
	
	/**
	 * Flatten a uniform matrix into a vector of M x N
	 * @param a
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IllegalArgumentException if the matrix rows are empty
	 * @return a flattened matrix
	 */
	public static int[] flatten(final int[][] a) {
		checkDimsForUniformity(a);
		
		final int m = a.length, n = a[0].length;
		final int[] out = new int[m * n];
		int ctr = 0;
		for(int i = 0; i < m; i++) {
			final int[] row = a[i];
			for(int j = 0; j < n; j++)
				out[ctr++] = row[j];
		}
		
		return out;
	}
	
	/**
	 * Flattens an upper triangular matrix into a vector of M choose 2 length
	 * @param mat - the square upper triangular matrix
	 * @throws DimensionMismatchException if the matrix is not square
	 * @throws IllegalArgumentException if the matrix has no rows
	 * @throws NonUniformMatrixException if the matrix is jagged
	 * @return the upper triangular vector
	 */
	public static double[] flattenUpperTriangularMatrix(final double[][] mat) {
		checkDimsForUniformity(mat);
		
		final int m = mat.length, n = mat[0].length;
		if(m != n)
			throw new DimensionMismatchException(m, n);
		
		final int s = m*(m-1)/2; // The shape of the flattened upper triangular matrix (m choose 2)
		final double[] vec = new double[s];
		for(int i = 0, r = 0; i < m - 1; i++)
			for(int j = i + 1; j < m; j++, r++)
				vec[r] = mat[i][j];
		
		return vec;
	}
	
	/**
	 * If a value in the matrix is less than min
	 * @param a
	 * @param min -- the value to compare to (less than this equals newMin)
	 * @param newMin -- the replace value
	 * @return the floored matrix
	 */
	public static double[][] floor(final double[][] a, final double min, final double newMin) {
		checkDimsPermitEmpty(a);
		
		final double[][] b = new double[a.length][];
		for(int i = 0; i < b.length; i++)
			b[i] = VecUtils.floor(a[i], min, newMin);
		
		return b;
	}
	
	/**
	 * Build a matrix from a vector. Repeating a vector ({0,1}) row-wise twice will
	 * yield a matrix {{0,0},{1,1}}; column-wise will yield {{0,1},{0,1}}
	 * @param v - the vector
	 * @param repCount - the number of time to repeat the vector
	 * @param axis: which axis each value in the vector represents
	 * @return a matrix
	 */
	public static double[][] fromVector(final double[] v, final int repCount, final Axis axis) {
		VecUtils.checkDimsPermitEmpty(v);
		
		if(repCount < 1)
			throw new IllegalArgumentException("repCount cannot be less than 1");
		
		double[][] out;
		if(axis.equals(Axis.ROW)) {
			out = new double[v.length][repCount];
			
			for(int i = 0; i < out.length; i++)
				for(int j = 0; j < out[0].length; j++)
					out[i][j] = v[i];
		} else {
			out = new double[repCount][v.length];
			
			for(int i = 0; i < out.length; i++)
				out[i] = VecUtils.copy(v);
		}
		
		return out;
	}
	
	/**
	 * Create a matrix from an ArrayList of vectors
	 * @param a
	 * @return a matrix
	 */
	public static double[][] fromList(final ArrayList<double[]> a) {
		final double[][] b = new double[a.size()][];
		
		int idx = 0;
		for(double[] vec: a)
			b[idx++] = VecUtils.copy(vec);
		
		return b;
	}
	
	/**
	 * Retrieve a column from a uniform matrix
	 * @param data
	 * @param idx
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the column at the idx
	 */
	public static double[] getColumn(final double[][] data, final int idx) {
		checkDimsForUniformity(data);
		
		final int m=data.length, n=data[0].length;
		if(idx >= n || idx < 0)
			throw new IndexOutOfBoundsException(idx+"");
		
		final double[] col = new double[m];
		for(int i = 0; i < m; i++)
			col[i] = data[i][idx];
		
		return col;
	}
	
	/**
	 * Retrieve a column from a uniform matrix
	 * @param data
	 * @param idx
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the column at the idx
	 */
	public static int[] getColumn(final int[][] data, final int idx) {
		checkDimsForUniformity(data);
		
		final int m=data.length, n=data[0].length;
		if(idx >= n || idx < 0)
			throw new IndexOutOfBoundsException(idx+"");
		
		final int[] col = new int[m];
		for(int i = 0; i < m; i++)
			col[i] = data[i][idx];
		
		return col;
	}
	
	/**
	 * Retrieve a set of columns from a uniform matrix
	 * @param data
	 * @param idx
	 * @throws IllegalArgumentException if the rows are empty
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the new matrix
	 */
	public static double[][] getColumns(final double[][] data, final int[] idcs) {
		checkDimsForUniformity(data);
		final double[][] out = new double[data.length][idcs.length];
		
		int idx = 0;
		for(int i = 0; i < idcs.length; i++)
			setColumnInPlace(out, idx++, getColumn(data, idcs[i]));
		
		return out;
	}
	
	/**
	 * Retrieve a set of columns from a uniform matrix
	 * @param data
	 * @param idx
	 * @throws IllegalArgumentException if the rows are empty
	 * @throws NonUniformMatrixException if the matrix is not uniform
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the new matrix
	 */
	public static double[][] getColumns(final double[][] data, final Integer[] idcs) {
		int[] i = new int[idcs.length];
		for(int j = 0; j < i.length; j++)
			i[j] = idcs[j];
		return getColumns(data, i);
	}
	
	/**
	 * Retrieve a set of rows from a matrix
	 * @param data
	 * @param idx
	 * @throws IllegalArgumentException if the rows are empty
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the new matrix
	 */
	public static double[][] getRows(final double[][] data, final int[] idcs) {
		checkDimsPermitEmpty(data);
		final double[][] out = new double[idcs.length][];
		
		int idx = 0;
		for(int i = 0; i < idcs.length; i++) {
			out[idx] = new double[data[i].length];
			setRowInPlace(out, idx++, data[idcs[i]]);
		}
		
		return out;
	}
	
	/**
	 * Retrieve a set of rows from a matrix
	 * @param data
	 * @param idx
	 * @throws IndexOutOfBoundsException if the idx is 
	 * less than 0 or >= the length of the matrix
	 * @return the new matrix
	 */
	public static double[][] getRows(final double[][] data, final Integer[] idcs) {
		int[] i = new int[idcs.length];
		for(int j = 0; j < i.length; j++)
			i[j] = idcs[j];
		return getRows(data, i);
	}
	
	/**
	 * Return a vector of maxes across an axis in a uniform matrix.
	 * @param data
	 * @param axis
	 * @throws IllegalArgumentException if no rows in matrix
	 * @throws NonUniformMatrixException if the matrix is non uniform
	 * @return a vector of maxes
	 */
	public static double[] max(final double[][] data, final Axis axis) {
		return minMax(data, axis, true);
	}
	
	/**
	 * Return a vector of mins across an axis in a uniform matrix.
	 * @param data
	 * @param axis
	 * @throws IllegalArgumentException if no rows in matrix
	 * @throws NonUniformMatrixException if the matrix is non uniform
	 * @return a vector of mins
	 */
	public static double[] min(final double[][] data, final Axis axis) {
		return minMax(data, axis, false);
	}
	
	/**
	 * Local helper function for computing min or max vectors across an axis
	 * @param data
	 * @param axis
	 * @param max
	 * @return
	 */
	private static double[] minMax(final double[][] data, final Axis axis, boolean max) {
		checkDimsForUniformity(data);
		
		double[] out;
		final int m=data.length, n=data[0].length;
		if(axis.equals(Axis.COL)) {
			out = new double[n];
			double[] col;
			for(int i = 0; i < n; i++) {
				col = getColumn(data, i);
				out[i] = max ? VecUtils.max(col) : VecUtils.min(col);
			}
		} else {
			out = new double[m];
			for(int i = 0; i < m; i++)
				out[i] = max ? VecUtils.max(data[i]) : VecUtils.min(data[i]);
		}
		
		return out;
	}
	
	/**
	 * Returns the mean row from a uniform matrix
	 * @param data
	 * @throws NonUniformMatrixException if the matrix is non uniform
	 * @throws IllegalArgumentException if the matrix has no rows
	 * @return the mean record
	 */
	public static double[] meanRecord(final double[][] data) {
		checkDimsForUniformity(data);
		
		// Note: could use VecUtils.mean(...) and getColumn(...)
		// in conjunction here, but this is a faster hack, though
		// somewhat code duplicative...
		
		final int m=data.length, n=data[0].length;
		final double[] sums = new double[n];
		
		for(int i = 0; i < m; i++) {
			for(int j = 0; j < n; j++) {
				sums[j] += data[i][j];
				if(i == m-1)
					sums[j] /= m;
			}
		}
		
		return sums;
	}
	
	/**
	 * Returns the median row from a uniform matrix
	 * @param data
	 * @throws NonUniformMatrixException if the matrix is non uniform
	 * @throws IllegalArgumentException if the matrix has no rows
	 * @return the median record
	 */
	public static double[] medianRecord(final double[][] data) {
		checkDimsForUniformity(data);
		
		final int n = data[0].length;
		final double[] median = new double[n];
		for(int j = 0; j < n; j++)
			median[j] = VecUtils.median(getColumn(data, j));
		
		return median;
	}
	
	/**
	 * Multiply two matrices. Auto schedules 
	 * parallel job if applicable
	 * @param a
	 * @param b
	 * @return the product A*B
	 */
	public static double[][] multiply(final double[][] a, final double[][] b) {
		if(ALLOW_AUTO_PARALLELISM && 
				(a.length>MAX_SERIAL_VECTOR_LEN 
			  || b.length>MAX_SERIAL_VECTOR_LEN)) {
			try {
				return multiplyDistributed(a, b);
			} catch(RejectedExecutionException e) { /*Perform normal execution*/ }
		}
		
		return multiplyForceSerial(a, b);
	}
	
	/**
	 * Multiply two matrices, A and B, in a parallel fashion using 
	 * {@link DistributedMatrixMultiplication}
	 * @param a
	 * @param b
	 * @throws DimensionMismatchException if the number of columns in A does not
	 * match the number of rows in B
	 * @throws IllegalArgumentException if the rows of either matrix are empty
	 * @return the product A*B
	 */
	public static double[][] multiplyDistributed(final double[][] a, final double[][] b) {
		return DistributedMatrixMultiplication.operate(a, b);
	}
	
	/**
	 * Multiply two matrices, A and B, serially
	 * @param a
	 * @param b
	 * @throws DimensionMismatchException if the number of columns in A does not
	 * match the number of rows in B
	 * @throws IllegalArgumentException if the rows of either matrix are empty
	 * @return the product A*B
	 */
	public static double[][] multiplyForceSerial(final double[][] a, final double[][] b) {
		checkDims(a);
		checkDims(b);
		
		// The following classes implicitly handle the multiplication criteria
		if(a.length > BLOCK_MAT_THRESH || b.length > BLOCK_MAT_THRESH) {
			final BlockRealMatrix aa = new BlockRealMatrix(a);
			final BlockRealMatrix bb = new BlockRealMatrix(b);
			return aa.multiply(bb).getData();
		}
		
		final Array2DRowRealMatrix aa = new Array2DRowRealMatrix(a, false);
		final Array2DRowRealMatrix bb = new Array2DRowRealMatrix(b, false);
		return aa.multiply(bb).getDataRef();
	}
	
	
	/**
	 * Invert the sign of every element in a matrix, return a copy
	 * @param data
	 * @throws IllegalArgumentException if the matrix's row dims are empty
	 * @return the matrix with every element's sign inverted
	 */
	public static double[][] negative(final double[][] data) {
		checkDimsPermitEmpty(data);
		
		final double[][] copy = MatUtils.copy(data);
		for(int i = 0; i < copy.length; i++)
			for(int j = 0; j < copy[i].length; j++)
				copy[i][j] = -copy[i][j];
		return copy;
	}
	
	public static double[][] partitionByRow(final double[][] a, final int kth) {
		checkDims(a);
		
		final int n = a.length;
		if(kth >= n || kth < 0)
			throw new IllegalArgumentException(kth+" is out of bounds");
		
		final double[] val = a[kth];
		final String strVl = Arrays.toString(val);
		
		String[] b = new String[n];
		for(int i = 0; i < n; i++)
			b[i] = Arrays.toString(a[i]);
		
		double[][] c = new double[n][];
		
		int idx = -1;
		Arrays.sort(b);
		for(int i = 0; i < n; i++) {
			if(b[i].equals(strVl)) {
				idx = i;
				break;
			}	
		}
		
		c[idx] = val;
		for(int i = 0, nextLow = 0, nextHigh = idx+1; i < n; i++) {
			if(i == kth) // This is the pivot point
				continue;
			if(Arrays.toString(a[i]).compareTo(strVl) < 0)
				c[nextLow++] = a[i];
			else {
				c[nextHigh++] = a[i];
			}
		}
		
		return c;
	}
	
	/**
	 * Creates a matrix of random Gaussians.
	 * @param m
	 * @param n
	 * @return a MxN matrix
	 */
	public static double[][] randomGaussian(final int m, final int n) {
		return randomGaussian(m, n, new Random());
	}
	
	/**
	 * Creates a matrix of random Gaussians.
	 * @param m
	 * @param n
	 * @param seed
	 * @return a MxN matrix
	 */
	public static double[][] randomGaussian(final int m, final int n, final Random seed) {
		if(m < 0 || n < 0)
			throw new IllegalArgumentException("illegal dimensions");
		
		final double[][] out = new double[m][n];
		for(int i = 0; i < m; i++)
			out[i] = VecUtils.randomGaussian(n, seed);
		
		return out;
	}
	
	/**
	 * Reorder the rows in a matrix
	 * @param data
	 * @param order
	 * @throws IllegalArgumentException if the data is empty
	 * @return the reordered matrix
	 */
	public static double[][] reorder(final double[][] data, final int[] order) {
		VecUtils.checkDims(order);
		checkDims(data);
		
		final int n = order.length;
		final double[][] out = new double[n][];
		
		int idx = 0;
		for(int i: order)
			out[idx++] = VecUtils.copy(data[i]);
		
		return out;
	}
	
	/**
	 * Reorder the rows in a matrix
	 * @param data
	 * @param order
	 * @throws IllegalArgumentException if the data is empty
	 * @return the reordered matrix
	 */
	public static int[][] reorder(final int[][] data, final int[] order) {
		VecUtils.checkDims(order);
		checkDims(data);
		
		final int n = order.length;
		final int[][] out = new int[n][];
		
		int idx = 0;
		for(int i: order)
			out[idx++] = VecUtils.copy(data[i]);
		
		return out;
	}
	
	public static double[][] rep(final double val, final int m, final int n) {
		if(m < 0 || n < 0)
			throw new IllegalArgumentException("illegal dimension");
		final double[][] out = new double[m][n];
		for(int i = 0; i < m; i++)
			out[i] = VecUtils.rep(val, n);
		return out;
	}
	
	public static double[][] rep(final double[] vec, final int m) {
		VecUtils.checkDims(vec);
		
		if(m < 0)
			throw new IllegalArgumentException("illegal dimension");
		final double[][] out = new double[m][vec.length];
		for(int i = 0; i < m; i++)
			out[i] = VecUtils.copy(vec);
		return out;
	}
	
	/**
	 * Reshape a matrix into new dimensions
	 * @param matrix
	 * @param mNew
	 * @param nNew
	 * @throws IllegalArgumentException if either new dimension is less than 0, or if the
	 * product of the new dimensions don't match the product of the current dimensions
	 * @return the reshaped matrix
	 */
	public static double[][] reshape(final double[][] matrix, final int mNew, final int nNew) {
		checkDimsForUniformity(matrix);
		
		final int mOld = matrix.length, nOld = matrix[0].length;
		
		if(mOld*nOld != mNew*nNew)
			throw new IllegalArgumentException("total matrix size cannot "
				+ "change (original: "+mOld+"x"+nOld+", "
				+ "new: "+mNew+"x"+nNew+")");
		if(mNew < 0 || nNew < 0) // either they both are, or neither is or it wouldn't make it to this check...
			throw new IllegalArgumentException("m, n must be greater than 0");
		
		final double[][] out = new double[mNew][nNew];
		
		int idx = 0;
		for(int i = 0; i < mNew; i++)
			for(int j = 0; j < nNew; j++)
				out[i][j] = matrix[idx / nOld][idx++ % nOld];
		
		return out;
	}
	
	public static double[][] reshape(final double[] vector, final int mNew, final int nNew) {
		VecUtils.checkDimsPermitEmpty(vector);
		final int n = vector.length;
		
		if(n != mNew*nNew)
			throw new IllegalArgumentException("vector size and m*n dims don't match");
		if(mNew < 0 || nNew < 0) // either they both are, or neither is or it wouldn't make it to this check...
			throw new IllegalArgumentException("m, n must be >= 0");
		final double[][] out = new double[mNew][nNew];
		
		int idx = 0;
		for(int i = 0; i < mNew; i++)
			for(int j = 0; j < nNew; j++)
				out[i][j] = vector[idx++];
		
		return out;
	}
	
	public static int[][] reshape(final int[] vector, final int mNew, final int nNew) {
		VecUtils.checkDimsPermitEmpty(vector);
		final int n = vector.length;
		
		if(n != mNew*nNew)
			throw new IllegalArgumentException("vector size and m*n dims don't match");
		if(mNew < 0 || nNew < 0) // either they both are, or neither is or it wouldn't make it to this check...
			throw new IllegalArgumentException("m, n must be >= 0");
		final int[][] out = new int[mNew][nNew];
		
		int idx = 0;
		for(int i = 0; i < mNew; i++)
			for(int j = 0; j < nNew; j++)
				out[i][j] = vector[idx++];
		
		return out;
	}
	
	public static int[][] reshape(final int[][] matrix, final int mNew, final int nNew) {
		checkDims(matrix);
		
		final int mOld = matrix.length, nOld = matrix[0].length;
		
		if(mOld*nOld != mNew*nNew)
			throw new IllegalArgumentException("total matrix size cannot change");
		if(mNew < 0 || nNew < 0) // either they both are, or neither is or it wouldn't make it to this check...
			throw new IllegalArgumentException("m, n must be greater than 0");
		
		final int[][] out = new int[mNew][nNew];
		
		int idx = 0;
		for(int i = 0; i < mNew; i++)
			for(int j = 0; j < nNew; j++)
				out[i][j] = matrix[idx / nOld][idx++ % nOld];
		
		return out;
	}
	
	public static double[] rowMeans(final double[][] data) {
		checkDims(data);
		
		final double[] out = new double[data.length];
		for(int i = 0; i < out.length; i++)
			out[i] = VecUtils.mean(data[i]);
		
		return out;
	}
	
	public static double[] rowSums(final double[][] data) {
		checkDims(data);
		
		final double[] out = new double[data.length];
		for(int i = 0; i < out.length; i++)
			out[i] = VecUtils.sum(data[i]);
		
		return out;
	}
	
	/**
	 * Scalar add a vector axis-wise to a matrix
	 * @param data
	 * @param vector
	 * @param axis - whether each element in the vector constitutes a row or column
	 * @return the scalar-operated matrix
	 */
	public static double[][] scalarAdd(final double[][] data, final double[] vector, final Axis axis) {
		return scalarOperate(data, vector, axis, Operator.ADD);
	}
	
	public static double[][] scalarAdd(final double[][] data, final double scalar) {
		return scalarOperate(data, scalar, Operator.ADD);
	}
	
	/**
	 * Scalar divide a vector axis-wise to a matrix
	 * @param data
	 * @param vector
	 * @param axis - whether each element in the vector constitutes a row or column
	 * @return the scalar-operated matrix
	 */
	public static double[][] scalarDivide(final double[][] data, final double[] vector, final Axis axis) {
		return scalarOperate(data, vector, axis, Operator.DIV);
	}
	
	public static double[][] scalarDivide(final double[][] data, final double scalar) {
		return scalarOperate(data, scalar, Operator.DIV);
	}
	
	/**
	 * Scalar multiply a vector axis-wise to a matrix
	 * @param data
	 * @param vector
	 * @param axis - whether each element in the vector constitutes a row or column
	 * @return the scalar-operated matrix
	 */
	public static double[][] scalarMultiply(final double[][] data, final double[] vector, final Axis axis) {
		return scalarOperate(data, vector, axis, Operator.MULT);
	}
	
	public static double[][] scalarMultiply(final double[][] data, final double scalar) {
		return scalarOperate(data, scalar, Operator.MULT);
	}
	
	private static double[][] scalarOperate(final double[][] data, final double[] vector, final Axis axis, Operator op) {
		checkDims(data);
		
		final int m = data.length, n = data[0].length;
		
		final double[][] out = new double[m][n];
		final boolean row = axis.equals(Axis.ROW);
		if(row) {
			if(vector.length != m)
				throw new DimensionMismatchException(vector.length, m);
			
			for(int i = 0; i < m; i++) {
				for(int j = 0; j < n; j++) {
					double scalar = vector[i];
					out[i][j] = op.equals(Operator.ADD) ? data[i][j] + scalar :
						op.equals(Operator.DIV) ? data[i][j] / scalar :
							op.equals(Operator.MULT) ? data[i][j] * scalar :
								data[i][j] - scalar;
				}
			}
		} else {
			if(vector.length != n)
				throw new DimensionMismatchException(vector.length, n);
			
			for(int i = 0; i < m; i++) {
				for(int j = 0; j < n; j++) {
					double scalar = vector[j];
					out[i][j] = op.equals(Operator.ADD) ? data[i][j] + scalar :
						op.equals(Operator.DIV) ? data[i][j] / scalar :
							op.equals(Operator.MULT) ? data[i][j] * scalar :
								data[i][j] - scalar;
				}
			}
		}
		
		return out;
	}
	
	private static double[][] scalarOperate(final double[][] data, final double scalar, final Operator op) {
		checkDims(data);
		
		final int m=data.length, n=data[0].length;
		final double[][] copy = new double[m][n];
		
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++) {
				copy[i][j] = op.equals(Operator.ADD) ? data[i][j] + scalar :
								op.equals(Operator.DIV) ? data[i][j] / scalar :
									op.equals(Operator.MULT) ? data[i][j] * scalar :
										data[i][j] - scalar;
			}
		
		return copy;
	}
	
	public static double[][] scalarSubtract(final double[][] data, final double scalar) {
		return scalarOperate(data, scalar, Operator.SUB);
	}
	
	/**
	 * Scalar subtract a vector axis-wise from a matrix
	 * @param data
	 * @param vector
	 * @param axis - whether each element in the vector constitutes a row or column
	 * @return the scalar-operated matrix
	 */
	public static double[][] scalarSubtract(final double[][] data, final double[] vector, final Axis axis) {
		return scalarOperate(data, vector, axis, Operator.SUB);
	}
	
	public static void setColumnInPlace(final double[][] a, final int idx, final double[] v) {
		checkDims(a);
		
		final int m = a.length, n = a[0].length;
		VecUtils.checkDims(getColumn(a, 0), v);
		
		if(idx < 0 || idx >= n)
			throw new IndexOutOfBoundsException("illegal idx: " + idx);
		
		for(int i = 0; i < m; i++)
			a[i][idx] = v[i];
	}
	
	public static void setRowInPlace(final double[][] a, final int idx, final double[] v) {
		checkDims(a);
		
		final int m = a.length, n = a[0].length;
		VecUtils.checkDims(a[0], v);

		if(idx < 0 || idx >= m)
			throw new IndexOutOfBoundsException("illegal idx: " + idx);
		
		for(int i = 0; i < n; i++)
			a[idx][i] = v[i];
	}
	
	public static double[][] sortAscByCol(final double[][] data, final int col) {
		checkDims(data);
		int[] sortedArgs = VecUtils.argSort(MatUtils.getColumn(data, col));
		return MatUtils.reorder(data, sortedArgs);
	}
	
	public static int[][] sortAscByCol(final int[][] data, final int col) {
		checkDims(data);
		int[] sortedArgs = VecUtils.argSort(MatUtils.getColumn(data, col));
		return MatUtils.reorder(data, sortedArgs);
	}
	
	public static double[][] sortDescByCol(final double[][] data, final int col) {
		checkDims(data);
		int[] sortedArgs = VecUtils.reverseSeries(VecUtils.argSort(MatUtils.getColumn(data, col)));
		return MatUtils.reorder(data, sortedArgs);
	}
	
	public static int[][] sortDescByCol(final int[][] data, final int col) {
		checkDims(data);
		int[] sortedArgs = VecUtils.reverseSeries(VecUtils.argSort(MatUtils.getColumn(data, col)));
		return MatUtils.reorder(data, sortedArgs);
	}
	
	public static final double[][] subtract(final double[][] a, final double[][] b) {
		checkDims(a);
		checkDims(b);
		
		final int m = a.length, n = a[0].length;
		if(b.length != m)
			throw new DimensionMismatchException(b.length, m);
		if(b[0].length != n)
			throw new DimensionMismatchException(b[0].length, n);
			
		final double[][] c = new double[m][n];
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++)
				c[i][j] = a[i][j] - b[i][j];
		return c;
	}
	
	public static double[][] toDouble(int[][] mat) {
		// Allow jagged arrays
		checkDimsPermitEmpty(mat);
		
		final int m = mat.length;
		double[][] out = new double[m][];
		for(int i = 0; i < m; i++) {
			out[i] = new double[mat[i].length];
			for(int j = 0; j < out[i].length; j++)
				out[i][j] = (double)mat[i][j];
		}
		
		return out;
	}
	
	public static double[][] transpose(final double[][] a) {
		checkDims(a);
		
		final int m = a.length, n = a[0].length;
		final double[][] t = new double[n][m];
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++)
				t[j][i] = a[i][j];
		return t;
	}
	
	public static double[][] transpose(final double[] a) {
		VecUtils.checkDims(a);
		
		final int m = a.length;
		final double[][] r = new double[m][1];
		for(int i = 0; i < m; i++)
			r[i][0] = a[i];
		
		return r;
	}

	public static double[][] where(final MatSeries series, double[][] x, double[][] y) {
		checkDimsForUniformity(x, y);
		
		final int m = x.length, n = x[0].length;
		final boolean[][] ser = series.get();
		
		checkDims(ser);
		if(ser.length != m)
			throw new DimensionMismatchException(ser.length, m);
		if(ser[0].length != n)
			throw new DimensionMismatchException(ser[0].length, n);
		
		final double[][] result = new double[m][n];
		for(int row = 0; row < m; row++)
			for(int i = 0; i < n; i++)
				result[row][i] = ser[row][i] ? x[row][i] : y[row][i];
				
		return result;
	}
	
	public static double[][] where(final MatSeries series, double[] x, double[][] y) {
		return where(series, rep(x, series.get().length), y);
	}
	
	public static double[][] where(final MatSeries series, double[][] x, double[] y) {
		return where(series, x, rep(y, series.get().length));
	}
	
	public static double[][] where(final MatSeries series, double[] x, double[] y) {
		VecUtils.checkDims(x,y);
		return where(series, rep(x, series.get().length), rep(y, series.get().length));
	}
}
