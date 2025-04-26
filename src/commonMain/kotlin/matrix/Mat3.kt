package matrix

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Assuming these types are defined elsewhere in your Kotlin project
// or can be represented by standard Kotlin types like FloatArray.
// For this conversion, we'll assume Mat3Arg, Mat4Arg, Vec2Arg, Vec3Arg are FloatArray.
// You might want to define more specific data classes for better type safety
// if they are not just simple FloatArrays.
typealias Mat3Arg = FloatArray // Represents a 3x3 matrix stored in a 12-element array (like Mat4 layout)
typealias Mat4Arg = FloatArray
typealias Vec2Arg = FloatArray
typealias Vec3Arg = FloatArray

// Assuming a utility object for epsilon comparison
object Mat3Utils {
    const val EPSILON = 0.000001f // Or a suitable epsilon value
}



/**
 * Represents a 3x3 matrix stored in a 12-element FloatArray,
 * using a layout similar to a 4x4 matrix for potential compatibility or alignment.
 * The elements are stored in column-major order, but only the first 3 columns
 * and first 3 rows are used for 3x3 operations. The 4th column is ignored
 * or used for padding/homogenization as seen in the create function.
 *
 * The layout is:
 * 0  4  8   (12)
 * 1  5  9   (13)
 * 2  6  10  (14)
 * 3  7  11  (15)
 *
 * Where () are unused for Mat3 but might be present in the 12-element array.
 * The provided JS code uses indices 0-2, 4-6, 8-10, which corresponds to the
 * first 3 rows and first 3 columns of a 4x4 matrix.
 */
/*@JvmInline value*/ class Mat3 private constructor(private val values: FloatArray) {

    init {
        if (values.size != 12) {
            throw IllegalArgumentException("Mat3 requires a 12-element FloatArray for storage.")
        }
    }

    /**
     * Creates a new Mat3 with the given values.
     * The values are expected in column-major order, mapping to the
     * 0  4  8
     * 1  5  9
     * 2  6  10
     * positions in the internal 12-element array.
     */
    constructor(
        v0: Float = 0f, v1: Float = 0f, v2: Float = 0f,
        v3: Float = 0f, v4: Float = 0f, v5: Float = 0f,
        v6: Float = 0f, v7: Float = 0f, v8: Float = 0f
    ) : this(FloatArray(12).apply {
        this[0] = v0
        this[1] = v1
        this[2] = v2
        this[4] = v3
        this[5] = v4
        this[6] = v5
        this[8] = v6
        this[9] = v7
        this[10] = v8
        // The JS code explicitly sets these to 0, aligning with a 4x4 layout where the 4th column is not used for 3x3
        this[3] = 0f
        this[7] = 0f
        this[11] = 0f
    })

    companion object {
        /**
         * Creates a Mat3 from a 12-element FloatArray.
         * Assumes the array is already in the correct internal format.
         */
        fun fromFloatArray(values: FloatArray): Mat3 {
            if (values.size != 12) {
                throw IllegalArgumentException("Mat3.fromFloatArray requires a 12-element FloatArray.")
            }
            return Mat3(values.copyOf()) // Create a copy to ensure internal state is not modified externally
        }

        /**
         * Creates a new identity Mat3.
         * @param dst - Mat3 to hold the result. If null, a new one is created.
         * @returns A 3-by-3 identity matrix.
         */
        fun identity(dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()
            return newDst.apply {
                values[ 0] = 1f; values[ 1] = 0f; values[ 2] = 0f; values[ 3] = 0f
                values[ 4] = 0f; values[ 5] = 1f; values[ 6] = 0f; values[ 7] = 0f
                values[ 8] = 0f; values[ 9] = 0f; values[10] = 1f; values[11] = 0f
            }
        }

        /**
         * Creates a Mat3 from the upper left 3x3 part of a Mat4.
         * @param m4 - source matrix (16-element FloatArray).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns Mat3 made from m4.
         */
        fun fromMat4(m4: Mat4Arg, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()
            return newDst.apply {
                values[0] = m4[0];  values[1] = m4[1];  values[ 2] = m4[ 2];  values[ 3] = 0f
                values[4] = m4[4];  values[5] = m4[5];  values[ 6] = m4[ 6];  values[ 7] = 0f
                values[8] = m4[8];  values[9] = m4[9];  values[10] = m4[10];  values[11] = 0f
            }
        }

        /**
         * Creates a Mat3 rotation matrix from a quaternion.
         * @param q - quaternion to create matrix from (4-element FloatArray).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns Mat3 made from q.
         */
        fun fromQuat(q: FloatArray, dst: Mat3? = null): Mat3 { // Assuming QuatArg is FloatArray
            val newDst = dst ?: Mat3()

            val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3];
            val x2 = x + x; val y2 = y + y; val z2 = z + z;

            val xx = x * x2;
            val yx = y * x2;
            val yy = y * y2;
            val zx = z * x2;
            val zy = z * y2;
            val zz = z * z2;
            val wx = w * x2;
            val wy = w * y2;
            val wz = w * z2;

            return newDst.apply {
                values[ 0] = 1f - yy - zz;  values[ 1] = yx + wz;      values[ 2] = zx - wy;      values[ 3] = 0f;
                values[ 4] = yx - wz;      values[ 5] = 1f - xx - zz;  values[ 6] = zy + wx;      values[ 7] = 0f;
                values[ 8] = zx + wy;      values[ 9] = zy - wx;      values[10] = 1f - xx - yy;  values[11] = 0f;
            }
        }

        /**
         * Creates a 3-by-3 matrix which translates by the given vector v.
         * @param v - The vector by which to translate (2-element FloatArray).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns The translation matrix.
         */
        fun translation(v: Vec2Arg, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            return newDst.apply {
                values[ 0] = 1f;     values[ 1] = 0f;     values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;     values[ 5] = 1f;     values[ 6] = 0f; values[7] = 0f
                values[ 8] = v[0];  values[ 9] = v[1];  values[10] = 1f; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which rotates by the given angle.
         * @param angleInRadians - The angle by which to rotate (in radians).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns The rotation matrix.
         */
        fun rotation(angleInRadians: Float, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            val c = cos(angleInRadians);
            val s = sin(angleInRadians);

            return newDst.apply {
                values[ 0] =  c;  values[ 1] = s;  values[ 2] = 0f; values[3] = 0f
                values[ 4] = -s;  values[ 5] = c;  values[ 6] = 0f; values[7] = 0f
                values[ 8] =  0f;  values[ 9] = 0f;  values[10] = 1f; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which rotates around the x-axis by the given angle.
         * @param angleInRadians - The angle by which to rotate (in radians).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns The rotation matrix.
         */
        fun rotationX(angleInRadians: Float, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            val c = cos(angleInRadians);
            val s = sin(angleInRadians);

            return newDst.apply {
                values[ 0] = 1f;  values[ 1] =  0f; values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;  values[ 5] =  c;  values[ 6] = s; values[7] = 0f
                values[ 8] = 0f;  values[ 9] = -s;  values[10] = c; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which rotates around the y-axis by the given angle.
         * @param angleInRadians - The angle by which to rotate (in radians).
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns The rotation matrix.
         */
        fun rotationY(angleInRadians: Float, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            val c = cos(angleInRadians);
            val s = sin(angleInRadians);

            return newDst.apply {
                values[ 0] = c;  values[ 1] = 0f;  values[ 2] = -s; values[3] = 0f
                values[ 4] = 0f;  values[ 5] = 1f;  values[ 6] =  0f; values[7] = 0f
                values[ 8] = s;  values[ 9] = 0f;  values[10] =  c; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which rotates around the z-axis by the given angle.
         * @param angleInRadians - The angle by which to rotate (in radians).
         * @param dst - matrix to hold result. If not passed a new one is created.
         * @returns The rotation matrix.
         */
        fun rotationZ(angleInRadians: Float, dst: Mat3? = null): Mat3 = rotation(angleInRadians, dst)


        /**
         * Creates a 3-by-3 matrix which scales in each dimension by an amount given by
         * the corresponding entry in the given vector; assumes the vector has two
         * entries.
         * @param v - A vector of 2 entries specifying the factor by which to scale in each dimension.
         * @param dst - matrix to hold result. If null, a new one is created.
         * @returns The scaling matrix.
         */
        fun scaling(v: Vec2Arg, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            return newDst.apply {
                values[ 0] = v[0];  values[ 1] = 0f;     values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;     values[ 5] = v[1];  values[ 6] = 0f; values[7] = 0f
                values[ 8] = 0f;     values[ 9] = 0f;     values[10] = 1f; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which scales in each dimension by an amount given by
         * the corresponding entry in the given vector; assumes the vector has three
         * entries.
         * @param v - A vector of 3 entries specifying the factor by which to scale in each dimension.
         * @param dst - matrix to hold result. If not passed a new one is created.
         * @returns The scaling matrix.
         */
        fun scaling3D(v: Vec3Arg, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            return newDst.apply {
                values[ 0] = v[0];  values[ 1] = 0f;     values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;     values[ 5] = v[1];  values[ 6] = 0f; values[7] = 0f
                values[ 8] = 0f;     values[ 9] = 0f;     values[10] = v[2]; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which scales uniformly in the X and Y dimensions.
         * @param s - Amount to scale.
         * @param dst - matrix to hold result. If not passed a new one is created.
         * @returns The scaling matrix.
         */
        fun uniformScaling(s: Float, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            return newDst.apply {
                values[ 0] = s;  values[ 1] = 0f;  values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;  values[ 5] = s;  values[ 6] = 0f; values[7] = 0f
                values[ 8] = 0f;  values[ 9] = 0f;  values[10] = 1f; values[11] = 0f
            }
        }

        /**
         * Creates a 3-by-3 matrix which scales uniformly in each dimension.
         * @param s - Amount to scale.
         * @param dst - matrix to hold result. If not passed a new one is created.
         * @returns The scaling matrix.
         */
        fun uniformScaling3D(s: Float, dst: Mat3? = null): Mat3 {
            val newDst = dst ?: Mat3()

            return newDst.apply {
                values[ 0] = s;  values[ 1] = 0f;  values[ 2] = 0f; values[3] = 0f
                values[ 4] = 0f;  values[ 5] = s;  values[ 6] = 0f; values[7] = 0f
                values[ 8] = 0f;  values[ 9] = 0f;  values[10] = s; values[11] = 0f
            }
        }
    }

    /**
     * Gets the internal FloatArray representation of the matrix.
     * Modifying this array directly is not recommended as it bypasses
     * the Mat3 class's intended usage and might lead to unexpected behavior
     * if the 12-element layout is not fully understood.
     */
    fun toFloatArray(): FloatArray = values.copyOf() // Return a copy for safety

    /**
     * Sets the values of this Mat3.
     * @param v0 - value for element 0
     * @param v1 - value for element 1
     * @param v2 - value for element 2
     * @param v3 - value for element 3
     * @param v4 - value for element 4
     * @param v5 - value for element 5
     * @param v6 - value for element 6
     * @param v7 - value for element 7
     * @param v8 - value for element 8
     * @returns This Mat3 with values set.
     */
    fun set(
        v0: Float, v1: Float, v2: Float,
        v3: Float, v4: Float, v5: Float,
        v6: Float, v7: Float, v8: Float
    ): Mat3 = this.apply {
        values[0] = v0;  values[1] = v1;  values[ 2] = v2;  values[ 3] = 0f;
        values[4] = v3;  values[5] = v4;  values[ 6] = v5;  values[ 7] = 0f;
        values[8] = v6;  values[9] = v7;  values[10] = v8;  values[11] = 0f;
    }

    /**
     * Negates this matrix.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns -this.
     */
    fun negate(dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        return newDst.apply {
            values[ 0] = -this@Mat3.values[ 0];  values[ 1] = -this@Mat3.values[ 1];  values[ 2] = -this@Mat3.values[ 2]; values[3] = 0f
            values[ 4] = -this@Mat3.values[ 4];  values[ 5] = -this@Mat3.values[ 5];  values[ 6] = -this@Mat3.values[ 6]; values[7] = 0f
            values[ 8] = -this@Mat3.values[ 8];  values[ 9] = -this@Mat3.values[ 9];  values[10] = -this@Mat3.values[10]; values[11] = 0f
        }
    }

    /**
     * multiply this matrix by a scalar.
     * @param s - the scalar
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns this * s.
     */
    fun multiplyScalar(s: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        return newDst.apply {
            values[ 0] = this@Mat3.values[ 0] * s;  values[ 1] = this@Mat3.values[ 1] * s;  values[ 2] = this@Mat3.values[ 2] * s; values[3] = 0f
            values[ 4] = this@Mat3.values[ 4] * s;  values[ 5] = this@Mat3.values[ 5] * s;  values[ 6] = this@Mat3.values[ 6] * s; values[7] = 0f
            values[ 8] = this@Mat3.values[ 8] * s;  values[ 9] = this@Mat3.values[ 9] * s;  values[10] = this@Mat3.values[10] * s; values[11] = 0f
        }
    }

    /**
     * add another matrix to this matrix.
     * @param other - the other matrix.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns this + other.
     */
    fun add(other: Mat3, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        return newDst.apply {
            values[ 0] = this@Mat3.values[ 0] + other.values[ 0];  values[ 1] = this@Mat3.values[ 1] + other.values[ 1];  values[ 2] = this@Mat3.values[ 2] + other.values[ 2]; values[3] = 0f
            values[ 4] = this@Mat3.values[ 4] + other.values[ 4];  values[ 5] = this@Mat3.values[ 5] + other.values[ 5];  values[ 6] = this@Mat3.values[ 6] + other.values[ 6]; values[7] = 0f
            values[ 8] = this@Mat3.values[ 8] + other.values[ 8];  values[ 9] = this@Mat3.values[ 9] + other.values[ 9];  values[10] = this@Mat3.values[10] + other.values[10]; values[11] = 0f
        }
    }

    /**
     * Copies this matrix.
     * @param dst - The matrix to copy into. If null, a new one is created.
     * @returns A copy of this.
     */
    fun copy(dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()
        this.values.copyInto(newDst.values)
        return newDst
    }

    /**
     * Copies this matrix (same as copy).
     * @param dst - The matrix to copy into. If null, a new one is created.
     * @returns A copy of this.
     */
    fun clone(dst: Mat3? = null): Mat3 = copy(dst)

    /**
     * Check if this matrix is approximately equal to another matrix.
     * @param other Operand matrix.
     * @returns true if matrices are approximately equal.
     */
    fun equalsApproximately(other: Mat3): Boolean {
        return kotlin.math.abs(values[ 0] - other.values[ 0]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 1] - other.values[ 1]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 2] - other.values[ 2]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 4] - other.values[ 4]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 5] - other.values[ 5]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 6] - other.values[ 6]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 8] - other.values[ 8]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[ 9] - other.values[ 9]) < Mat3Utils.EPSILON &&
                kotlin.math.abs(values[10] - other.values[10]) < Mat3Utils.EPSILON
    }

    /**
     * Check if this matrix is exactly equal to another matrix.
     * @param other Operand matrix.
     * @returns true if matrices are exactly equal.
     */
    override fun equals(other: Any?): Boolean {
        return other is Mat3 &&
                values[ 0] == other.values[ 0] &&
                values[ 1] == other.values[ 1] &&
                values[ 2] == other.values[ 2] &&
                values[ 4] == other.values[ 4] &&
                values[ 5] == other.values[ 5] &&
                values[ 6] == other.values[ 6] &&
                values[ 8] == other.values[ 8] &&
                values[ 9] == other.values[ 9] &&
                values[10] == other.values[10]
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        // We only consider the relevant 9 elements for equality/hash code
        result = 31 * result + values[0].hashCode()
        result = 31 * result + values[1].hashCode()
        result = 31 * result + values[2].hashCode()
        result = 31 * result + values[4].hashCode()
        result = 31 * result + values[5].hashCode()
        result = 31 * result + values[6].hashCode()
        result = 31 * result + values[8].hashCode()
        result = 31 * result + values[9].hashCode()
        result = 31 * result + values[10].hashCode()
        return result
    }


    /**
     * Creates a 3-by-3 identity matrix.
     *
     * @param dst - matrix to hold result. If not passed a new one is created.
     * @returns A 3-by-3 identity matrix.
     */
    fun identity(dst: Mat3Arg? = null): Mat3  {
        val newDst = (dst ?: Mat3Arg(12));

        newDst[ 0] = 1f;  newDst[ 1] = 0f;  newDst[ 2] = 0f;
        newDst[ 4] = 0f;  newDst[ 5] = 1f;  newDst[ 6] = 0f;
        newDst[ 8] = 0f;  newDst[ 9] = 0f;  newDst[10] = 1f;

        return Mat3(newDst)
    }

    /**
     * Takes the transpose of this matrix.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The transpose of this.
     */
    fun transpose(dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()
        if (newDst === this) {
            // Perform in-place transpose
            var t: Float

            // 0 1 2
            // 4 5 6
            // 8 9 10

            t = values[1]; values[1] = values[4]; values[4] = t;
            t = values[2]; values[2] = values[8]; values[8] = t;
            t = values[6]; values[6] = values[9]; values[9] = t;

            return newDst
        }

        val m00 = values[0 * 4 + 0]
        val m01 = values[0 * 4 + 1]
        val m02 = values[0 * 4 + 2]
        val m10 = values[1 * 4 + 0]
        val m11 = values[1 * 4 + 1]
        val m12 = values[1 * 4 + 2]
        val m20 = values[2 * 4 + 0]
        val m21 = values[2 * 4 + 1]
        val m22 = values[2 * 4 + 2]

        return newDst.apply {
            values[ 0] = m00;  values[ 1] = m10;  values[ 2] = m20; values[3] = 0f
            values[ 4] = m01;  values[ 5] = m11;  values[ 6] = m21; values[7] = 0f
            values[ 8] = m02;  values[ 9] = m12;  values[10] = m22; values[11] = 0f
        }
    }

    /**
     * Computes the inverse of this 3-by-3 matrix.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The inverse of this.
     */
    fun inverse(dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val m00 = values[0 * 4 + 0]
        val m01 = values[0 * 4 + 1]
        val m02 = values[0 * 4 + 2]
        val m10 = values[1 * 4 + 0]
        val m11 = values[1 * 4 + 1]
        val m12 = values[1 * 4 + 2]
        val m20 = values[2 * 4 + 0]
        val m21 = values[2 * 4 + 1]
        val m22 = values[2 * 4 + 2]

        val b01 =  m22 * m11 - m12 * m21
        val b11 = -m22 * m10 + m12 * m20
        val b21 =  m21 * m10 - m11 * m20

        val det = m00 * b01 + m01 * b11 + m02 * b21
        if (det == 0f) {
            // Matrix is not invertible
            return newDst.identity() // Or throw an exception, or return a special value
        }
        val invDet = 1 / det

        return newDst.apply {
            values[ 0] = b01 * invDet; values[3] = 0f
            values[ 1] = (-m22 * m01 + m02 * m21) * invDet; values[7] = 0f
            values[ 2] = ( m12 * m01 - m02 * m11) * invDet; values[11] = 0f
            values[ 4] = b11 * invDet
            values[ 5] = ( m22 * m00 - m02 * m20) * invDet
            values[ 6] = (-m12 * m00 + m02 * m10) * invDet
            values[ 8] = b21 * invDet
            values[ 9] = (-m21 * m00 + m01 * m20) * invDet
            values[10] = ( m11 * m00 - m01 * m10) * invDet
        }
    }

    /**
     * Compute the determinant of this matrix.
     * @returns the determinant.
     */
    fun determinant(): Float {
        val m00 = values[0 * 4 + 0]
        val m01 = values[0 * 4 + 1]
        val m02 = values[0 * 4 + 2]
        val m10 = values[1 * 4 + 0]
        val m11 = values[1 * 4 + 1]
        val m12 = values[1 * 4 + 2]
        val m20 = values[2 * 4 + 0]
        val m21 = values[2 * 4 + 1]
        val m22 = values[2 * 4 + 2]

        return m00 * (m11 * m22 - m21 * m12) -
                m10 * (m01 * m22 - m21 * m02) +
                m20 * (m01 * m12 - m11 * m02)
    }

    /**
     * Computes the inverse of this 3-by-3 matrix. (same as inverse)
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The inverse of this.
     */
    fun invert(dst: Mat3? = null): Mat3 = inverse(dst)

    /**
     * Multiplies this matrix by another matrix `other` (this * other).
     * @param other - The matrix on the right.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The matrix product of this and other.
     */
    fun multiply(other: Mat3, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val a00 = values[0]
        val a01 = values[1]
        val a02 = values[2]
        val a10 = values[ 4 + 0]
        val a11 = values[ 4 + 1]
        val a12 = values[ 4 + 2]
        val a20 = values[ 8 + 0]
        val a21 = values[ 8 + 1]
        val a22 = values[ 8 + 2]
        val b00 = other.values[0]
        val b01 = other.values[1]
        val b02 = other.values[2]
        val b10 = other.values[ 4 + 0]
        val b11 = other.values[ 4 + 1]
        val b12 = other.values[ 4 + 2]
        val b20 = other.values[ 8 + 0]
        val b21 = other.values[ 8 + 1]
        val b22 = other.values[ 8 + 2]

        return newDst.apply {
            values[ 0] = a00 * b00 + a10 * b01 + a20 * b02; values[3] = 0f
            values[ 1] = a01 * b00 + a11 * b01 + a21 * b02; values[7] = 0f
            values[ 2] = a02 * b00 + a12 * b01 + a22 * b02; values[11] = 0f
            values[ 4] = a00 * b10 + a10 * b11 + a20 * b12
            values[ 5] = a01 * b10 + a11 * b11 + a21 * b12
            values[ 6] = a02 * b10 + a12 * b11 + a22 * b12
            values[ 8] = a00 * b20 + a10 * b21 + a20 * b22
            values[ 9] = a01 * b20 + a11 * b21 + a21 * b22
            values[10] = a02 * b20 + a12 * b21 + a22 * b22
        }
    }

    /**
     * Multiplies this matrix by another matrix `other` (this * other). (same as multiply)
     * @param other - The matrix on the right.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The matrix product of this and other.
     */
    fun mul(other: Mat3, dst: Mat3? = null): Mat3 = multiply(other, dst)

    /**
     * Sets the translation component of this 3-by-3 matrix to the given vector.
     * @param v - The vector (2-element FloatArray).
     * @param dst - matrix to hold result. If null, a new one is created (as an identity matrix).
     * @returns This matrix with translation set.
     */
    fun setTranslation(v: Vec2Arg, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: identity() // Use identity if dst is null

        if (this !== newDst) {
            newDst.values[ 0] = values[ 0];
            newDst.values[ 1] = values[ 1];
            newDst.values[ 2] = values[ 2];
            newDst.values[ 4] = values[ 4];
            newDst.values[ 5] = values[ 5];
            newDst.values[ 6] = values[ 6];
        }
        newDst.values[ 8] = v[0];
        newDst.values[ 9] = v[1];
        newDst.values[10] = 1f; // Ensure the bottom-right is 1 for translation
        return newDst
    }

    /**
     * Returns the translation component of this 3-by-3 matrix as a vector with 2
     * entries.
     * @param dst - vector to hold result. If null, a new one is created.
     * @returns The translation component of this matrix.
     */
    fun getTranslation(dst: Vec2Arg? = null): Vec2Arg {
        TODO()
//        val newDst = dst ?: Vec2.create()
//        newDst[0] = values[8]
//        newDst[1] = values[9]
//        return newDst
    }

    /**
     * Returns an axis of this 3x3 matrix as a vector with 2 entries.
     * @param axis - The axis 0 = x, 1 = y.
     * @param dst - vector to hold result. If null, a new one is created.
     * @returns The axis component of this matrix.
     */
    fun getAxis(axis: Int, dst: Vec2Arg? = null): Vec2Arg {
        TODO()
//        val newDst = dst ?: Vec2.create()
//        val off = axis * 4
//        newDst[0] = values[off + 0]
//        newDst[1] = values[off + 1]
//        return newDst
    }

    /**
     * Sets an axis of this 3x3 matrix using a vector with 2 entries.
     * @param v - the axis vector (2-element FloatArray).
     * @param axis - The axis 0 = x, 1 = y.
     * @param dst - The matrix to set. If null, a new one is created (as a copy of this).
     * @returns The matrix with axis set.
     */
    fun setAxis(v: Vec2Arg, axis: Int, dst: Mat3? = null): Mat3 {
        val newDst = if (dst === this) this else copy(dst)

        val off = axis * 4
        newDst.values[off + 0] = v[0]
        newDst.values[off + 1] = v[1]
        return newDst
    }

    /**
     * Returns the "2d" scaling component of the matrix as a Vec2.
     * @param dst - The vector to set. If null, a new one is created.
     * @returns The scaling vector.
     */
    fun getScaling(dst: Vec2Arg? = null): Vec2Arg {
        TODO()
//        val newDst = dst ?: Vec2.create()
//
//        val xx = values[0]
//        val xy = values[1]
//        val yx = values[4]
//        val yy = values[5]
//
//        newDst[0] = sqrt(xx * xx + xy * xy)
//        newDst[1] = sqrt(yx * yx + yy * yy)
//
//        return newDst
    }


    /**
     * Returns the "3d" scaling component of the matrix as a Vec3.
     * @param dst - The vector to set. If null, a new one is created.
     * @returns The scaling vector.
     */
    fun get3DScaling(dst: Vec3Arg? = null): Vec3Arg {
        TODO()
//        val newDst = dst ?: Vec3.create()
//
//        val xx = values[0]
//        val xy = values[1]
//        val xz = values[2]
//        val yx = values[4]
//        val yy = values[5]
//        val yz = values[6]
//        val zx = values[8]
//        val zy = values[9]
//        val zz = values[10]
//
//        newDst[0] = sqrt(xx * xx + xy * xy + xz * xz)
//        newDst[1] = sqrt(yx * yx + yy * yy + yz * yz)
//        newDst[2] = sqrt(zx * zx + zy * zy + zz * zz)
//
//        return newDst
    }

    /**
     * Translates this 3-by-3 matrix by the given vector v.
     * @param v - The vector by which to translate (2-element FloatArray).
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The translated matrix.
     */
    fun translate(v: Vec2Arg, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val v0 = v[0]
        val v1 = v[1]

        val m00 = values[0]
        val m01 = values[1]
        val m02 = values[2]
        val m10 = values[1 * 4 + 0]
        val m11 = values[1 * 4 + 1]
        val m12 = values[1 * 4 + 2]
        val m20 = values[2 * 4 + 0]
        val m21 = values[2 * 4 + 1]
        val m22 = values[2 * 4 + 2]

        if (this !== newDst) {
            newDst.values[ 0] = m00
            newDst.values[ 1] = m01
            newDst.values[ 2] = m02
            newDst.values[ 4] = m10
            newDst.values[ 5] = m11
            newDst.values[ 6] = m12
        }

        newDst.values[ 8] = m00 * v0 + m10 * v1 + m20
        newDst.values[ 9] = m01 * v0 + m11 * v1 + m21
        newDst.values[10] = m02 * v0 + m12 * v1 + m22

        return newDst
    }

    /**
     * Rotates this 3-by-3 matrix by the given angle around the Z axis (2D rotation).
     * @param angleInRadians - The angle by which to rotate (in radians).
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The rotated matrix.
     */
    fun rotate(angleInRadians: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val m00 = values[0 * 4 + 0]
        val m01 = values[0 * 4 + 1]
        val m02 = values[0 * 4 + 2]
        val m10 = values[1 * 4 + 0]
        val m11 = values[1 * 4 + 1]
        val m12 = values[1 * 4 + 2]
        val c = cos(angleInRadians)
        val s = sin(angleInRadians)

        newDst.values[ 0] = c * m00 + s * m10
        newDst.values[ 1] = c * m01 + s * m11
        newDst.values[ 2] = c * m02 + s * m12; newDst.values[3] = 0f

        newDst.values[ 4] = c * m10 - s * m00
        newDst.values[ 5] = c * m11 - s * m01
        newDst.values[ 6] = c * m12 - s * m02; newDst.values[7] = 0f


        if (this !== newDst) {
            newDst.values[ 8] = values[ 8];
            newDst.values[ 9] = values[ 9];
            newDst.values[10] = values[10]; newDst.values[11] = 0f
        }

        return newDst
    }

    /**
     * Rotates this 3-by-3 matrix around the x-axis by the given angle.
     * @param angleInRadians - The angle by which to rotate (in radians).
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The rotated matrix.
     */
    fun rotateX(angleInRadians: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val m10 = values[4]
        val m11 = values[5]
        val m12 = values[6]
        val m20 = values[8]
        val m21 = values[9]
        val m22 = values[10]

        val c = cos(angleInRadians)
        val s = sin(angleInRadians)

        newDst.values[4]  = c * m10 + s * m20; newDst.values[7] = 0f
        newDst.values[5]  = c * m11 + s * m21
        newDst.values[6]  = c * m12 + s * m22
        newDst.values[8]  = c * m20 - s * m10; newDst.values[11] = 0f
        newDst.values[9]  = c * m21 - s * m11
        newDst.values[10] = c * m22 - s * m12

        if (this !== newDst) {
            newDst.values[ 0] = values[ 0];
            newDst.values[ 1] = values[ 1];
            newDst.values[ 2] = values[ 2]; newDst.values[3] = 0f
        }

        return newDst
    }

    /**
     * Rotates the given 3-by-3 matrix around the y-axis by the given angle.
     * @param angleInRadians - The angle by which to rotate (in radians).
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The rotated matrix.
     */
    fun rotateY(angleInRadians: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val m00 = values[0 * 4 + 0]
        val m01 = values[0 * 4 + 1]
        val m02 = values[0 * 4 + 2]
        val m20 = values[2 * 4 + 0]
        val m21 = values[2 * 4 + 1]
        val m22 = values[2 * 4 + 2]
        val c = cos(angleInRadians)
        val s = sin(angleInRadians)

        newDst.values[ 0] = c * m00 - s * m20; newDst.values[3] = 0f
        newDst.values[ 1] = c * m01 - s * m21
        newDst.values[ 2] = c * m02 - s * m22
        newDst.values[ 8] = c * m20 + s * m00; newDst.values[11] = 0f
        newDst.values[ 9] = c * m21 + s * m01
        newDst.values[10] = c * m22 + s * m02

        if (this !== newDst) {
            newDst.values[ 4] = values[ 4];
            newDst.values[ 5] = values[ 5];
            newDst.values[ 6] = values[ 6]; newDst.values[7] = 0f
        }

        return newDst
    }

    /**
     * Rotates the given 3-by-3 matrix around the z-axis by the given angle.
     * @param angleInRadians - The angle by which to rotate (in radians).
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The rotated matrix.
     */
    fun rotateZ(angleInRadians: Float, dst: Mat3? = null): Mat3 = rotate(angleInRadians, dst)

    /**
     * Scales this 3-by-3 matrix in each dimension by an amount
     * given by the corresponding entry in the given vector; assumes the vector has
     * two entries.
     * @param v - A vector of 2 entries specifying the factor by which to scale in each dimension.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The scaled matrix.
     */
    fun scale(v: Vec2Arg, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val v0 = v[0]
        val v1 = v[1]

        newDst.values[ 0] = v0 * values[0 * 4 + 0]; newDst.values[3] = 0f
        newDst.values[ 1] = v0 * values[0 * 4 + 1]
        newDst.values[ 2] = v0 * values[0 * 4 + 2]

        newDst.values[ 4] = v1 * values[1 * 4 + 0]; newDst.values[7] = 0f
        newDst.values[ 5] = v1 * values[1 * 4 + 1]
        newDst.values[ 6] = v1 * values[1 * 4 + 2]

        if (this !== newDst) {
            newDst.values[ 8] = values[ 8];
            newDst.values[ 9] = values[ 9];
            newDst.values[10] = values[10]; newDst.values[11] = 0f
        }

        return newDst
    }

    /**
     * Scales the given 3-by-3 matrix in each dimension by an amount
     * given by the corresponding entry in the given vector; assumes the vector has
     * three entries.
     * @param v - A vector of 3 entries specifying the factor by which to scale in each dimension.
     * @param dst - matrix to hold result. If not passed a new one is created.
     * @returns The scaled matrix.
     */
    fun scale3D(v: Vec3Arg, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        val v0 = v[0]
        val v1 = v[1]
        val v2 = v[2]

        newDst.values[ 0] = v0 * values[0 * 4 + 0]; newDst.values[3] = 0f
        newDst.values[ 1] = v0 * values[0 * 4 + 1]
        newDst.values[ 2] = v0 * values[0 * 4 + 2]

        newDst.values[ 4] = v1 * values[1 * 4 + 0]; newDst.values[7] = 0f
        newDst.values[ 5] = v1 * values[1 * 4 + 1]
        newDst.values[ 6] = v1 * values[1 * 4 + 2]

        newDst.values[ 8] = v2 * values[2 * 4 + 0]; newDst.values[11] = 0f
        newDst.values[ 9] = v2 * values[2 * 4 + 1]
        newDst.values[10] = v2 * values[2 * 4 + 2]

        return newDst
    }

    /**
     * Scales this 3-by-3 matrix uniformly in the X and Y dimension by an amount given.
     * @param s - Amount to scale.
     * @param dst - matrix to hold result. If null, a new one is created.
     * @returns The scaled matrix.
     */
    fun uniformScale(s: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        newDst.values[ 0] = s * values[0 * 4 + 0]; newDst.values[3] = 0f
        newDst.values[ 1] = s * values[0 * 4 + 1]
        newDst.values[ 2] = s * values[0 * 4 + 2]

        newDst.values[ 4] = s * values[1 * 4 + 0]; newDst.values[7] = 0f
        newDst.values[ 5] = s * values[1 * 4 + 1]
        newDst.values[ 6] = s * values[1 * 4 + 2]

        if (this !== newDst) {
            newDst.values[ 8] = values[ 8];
            newDst.values[ 9] = values[ 9];
            newDst.values[10] = values[10]; newDst.values[11] = 0f
        }

        return newDst
    }

    /**
     * Scales this 3-by-3 matrix uniformly in each dimension by an amount given.
     * @param s - Amount to scale.
     * @param dst - matrix to hold result. If not passed a new one is created.
     * @returns The scaled matrix.
     */
    fun uniformScale3D(s: Float, dst: Mat3? = null): Mat3 {
        val newDst = dst ?: Mat3()

        newDst.values[ 0] = s * values[0 * 4 + 0]; newDst.values[3] = 0f
        newDst.values[ 1] = s * values[0 * 4 + 1]
        newDst.values[ 2] = s * values[0 * 4 + 2]

        newDst.values[ 4] = s * values[1 * 4 + 0]; newDst.values[7] = 0f
        newDst.values[ 5] = s * values[1 * 4 + 1]
        newDst.values[ 6] = s * values[1 * 4 + 2]

        newDst.values[ 8] = s * values[2 * 4 + 0]; newDst.values[11] = 0f
        newDst.values[ 9] = s * values[2 * 4 + 1]
        newDst.values[10] = s * values[2 * 4 + 2]

        return newDst
    }
}

// Example usage:
fun main() {
    val matA = Mat3(
        1f, 2f, 3f,
        4f, 5f, 6f,
        7f, 8f, 9f
    )
    val matB = Mat3.identity()

    println("MatA:")
    println(matA.toFloatArray().joinToString())

    println("MatB (Identity):")
    println(matB.toFloatArray().joinToString())

    val matC = matA.add(matB)
    println("MatA + MatB:")
    println(matC.toFloatArray().joinToString())

    val matD = matA.multiplyScalar(2f)
    println("MatA * 2:")
    println(matD.toFloatArray().joinToString())

    val matE = matA.transpose()
    println("MatA Transpose:")
    println(matE.toFloatArray().joinToString())

    val matF = Mat3.translation(floatArrayOf(10f, 20f))
    println("Translation Matrix:")
    println(matF.toFloatArray().joinToString())

    val matG = matA.translate(floatArrayOf(1f, 1f))
    println("MatA Translated:")
    println(matG.toFloatArray().joinToString())
}