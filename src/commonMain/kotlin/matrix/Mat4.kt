package matrix

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

/**
 * A 4x4 matrix stored in column-major order.
 */
class Mat4(val data: FloatArray) {
    constructor() : this(floatArrayOf(
        1f, 0f, 0f, 0f,  // Column 0
        0f, 1f, 0f, 0f,  // Column 1
        0f, 0f, 1f, 0f,  // Column 2
        0f, 0f, 0f, 1f   // Column 3
    ))
    
    operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }
    
    /**
     * Creates a clone of this matrix.
     *
     * @return A new matrix with the same values
     */
    fun clone(): Mat4 {
        val result = Mat4()
        for (i in 0 until 16) {
            result[i] = this[i]
        }
        return result
    }
    
    /**
     * Sets the values of this matrix.
     *
     * @param values The values to set
     * @return This matrix with updated values
     */
    fun set(values: FloatArray): Mat4 {
        for (i in 0 until 16) {
            data[i] = values[i]
        }
        return this
    }
    
    /**
     * Multiplies this matrix by another matrix.
     *
     * @param b The matrix to multiply by
     * @param dst Optional matrix to store the result in
     * @return The product of the two matrices
     */
    fun multiply(b: Mat4, dst: Mat4 = Mat4()): Mat4 {
        val a00 = this[0]
        val a01 = this[1]
        val a02 = this[2]
        val a03 = this[3]
        val a10 = this[4]
        val a11 = this[5]
        val a12 = this[6]
        val a13 = this[7]
        val a20 = this[8]
        val a21 = this[9]
        val a22 = this[10]
        val a23 = this[11]
        val a30 = this[12]
        val a31 = this[13]
        val a32 = this[14]
        val a33 = this[15]
        
        val b00 = b[0]
        val b01 = b[1]
        val b02 = b[2]
        val b03 = b[3]
        val b10 = b[4]
        val b11 = b[5]
        val b12 = b[6]
        val b13 = b[7]
        val b20 = b[8]
        val b21 = b[9]
        val b22 = b[10]
        val b23 = b[11]
        val b30 = b[12]
        val b31 = b[13]
        val b32 = b[14]
        val b33 = b[15]
        
        dst[0] = b00 * a00 + b01 * a10 + b02 * a20 + b03 * a30
        dst[1] = b00 * a01 + b01 * a11 + b02 * a21 + b03 * a31
        dst[2] = b00 * a02 + b01 * a12 + b02 * a22 + b03 * a32
        dst[3] = b00 * a03 + b01 * a13 + b02 * a23 + b03 * a33
        dst[4] = b10 * a00 + b11 * a10 + b12 * a20 + b13 * a30
        dst[5] = b10 * a01 + b11 * a11 + b12 * a21 + b13 * a31
        dst[6] = b10 * a02 + b11 * a12 + b12 * a22 + b13 * a32
        dst[7] = b10 * a03 + b11 * a13 + b12 * a23 + b13 * a33
        dst[8] = b20 * a00 + b21 * a10 + b22 * a20 + b23 * a30
        dst[9] = b20 * a01 + b21 * a11 + b22 * a21 + b23 * a31
        dst[10] = b20 * a02 + b21 * a12 + b22 * a22 + b23 * a32
        dst[11] = b20 * a03 + b21 * a13 + b22 * a23 + b23 * a33
        dst[12] = b30 * a00 + b31 * a10 + b32 * a20 + b33 * a30
        dst[13] = b30 * a01 + b31 * a11 + b32 * a21 + b33 * a31
        dst[14] = b30 * a02 + b31 * a12 + b32 * a22 + b33 * a32
        dst[15] = b30 * a03 + b31 * a13 + b32 * a23 + b33 * a33
        
        return dst
    }
    
    /**
     * Transposes this matrix.
     *
     * @param dst Optional matrix to store the result in
     * @return The transposed matrix
     */
    fun transpose(dst: Mat4 = Mat4()): Mat4 {
        dst[0] = this[0]
        dst[1] = this[4]
        dst[2] = this[8]
        dst[3] = this[12]
        dst[4] = this[1]
        dst[5] = this[5]
        dst[6] = this[9]
        dst[7] = this[13]
        dst[8] = this[2]
        dst[9] = this[6]
        dst[10] = this[10]
        dst[11] = this[14]
        dst[12] = this[3]
        dst[13] = this[7]
        dst[14] = this[11]
        dst[15] = this[15]
        
        return dst
    }
    
    /**
     * Translates this matrix by a vector.
     *
     * @param v The vector to translate by
     * @param dst Optional matrix to store the result in
     * @return The translated matrix
     */
    fun translate(v: Vec3, dst: Mat4 = Mat4()): Mat4 {
        val x = v.x
        val y = v.y
        val z = v.z
        
        dst[0] = this[0]
        dst[1] = this[1]
        dst[2] = this[2]
        dst[3] = this[3]
        dst[4] = this[4]
        dst[5] = this[5]
        dst[6] = this[6]
        dst[7] = this[7]
        dst[8] = this[8]
        dst[9] = this[9]
        dst[10] = this[10]
        dst[11] = this[11]
        dst[12] = this[0] * x + this[4] * y + this[8] * z + this[12]
        dst[13] = this[1] * x + this[5] * y + this[9] * z + this[13]
        dst[14] = this[2] * x + this[6] * y + this[10] * z + this[14]
        dst[15] = this[3] * x + this[7] * y + this[11] * z + this[15]
        
        return dst
    }
    
    /**
     * Scales this matrix by a vector.
     *
     * @param v The vector to scale by
     * @param dst Optional matrix to store the result in
     * @return The scaled matrix
     */
    fun scale(v: Vec3, dst: Mat4 = Mat4()): Mat4 {
        val x = v.x
        val y = v.y
        val z = v.z
        
        dst[0] = this[0] * x
        dst[1] = this[1] * x
        dst[2] = this[2] * x
        dst[3] = this[3] * x
        dst[4] = this[4] * y
        dst[5] = this[5] * y
        dst[6] = this[6] * y
        dst[7] = this[7] * y
        dst[8] = this[8] * z
        dst[9] = this[9] * z
        dst[10] = this[10] * z
        dst[11] = this[11] * z
        dst[12] = this[12]
        dst[13] = this[13]
        dst[14] = this[14]
        dst[15] = this[15]
        
        return dst
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mat4) return false
        
        for (i in 0 until 16) {
            if (this[i] != other[i]) return false
        }
        
        return true
    }
    
    /**
     * Checks if this matrix is approximately equal to another matrix.
     *
     * @param b The second matrix
     * @return True if the matrices are approximately equal
     */
    fun equalsApproximately(b: Mat4): Boolean {
        for (i in 0 until 16) {
            if (abs(this[i] - b[i]) > EPSILON) return false
        }
        return true
    }
    
    override fun hashCode(): Int {
        return data.contentHashCode()
    }
    
    override fun toString(): String {
        return "Mat4(\n" +
               "  ${this[0]}, ${this[4]}, ${this[8]}, ${this[12]},\n" +
               "  ${this[1]}, ${this[5]}, ${this[9]}, ${this[13]},\n" +
               "  ${this[2]}, ${this[6]}, ${this[10]}, ${this[14]},\n" +
               "  ${this[3]}, ${this[7]}, ${this[11]}, ${this[15]}\n" +
               ")"
    }
    
    companion object {
        /**
         * Creates an identity matrix.
         *
         * @param dst Optional matrix to store the result in
         * @return The identity matrix
         */
        fun identity(dst: Mat4 = Mat4()): Mat4 {
            dst[0] = 1f
            dst[1] = 0f
            dst[2] = 0f
            dst[3] = 0f
            dst[4] = 0f
            dst[5] = 1f
            dst[6] = 0f
            dst[7] = 0f
            dst[8] = 0f
            dst[9] = 0f
            dst[10] = 1f
            dst[11] = 0f
            dst[12] = 0f
            dst[13] = 0f
            dst[14] = 0f
            dst[15] = 1f
            
            return dst
        }
        
        /**
         * Creates a perspective projection matrix.
         *
         * @param fieldOfViewYInRadians The field of view in radians
         * @param aspect The aspect ratio (width / height)
         * @param near The near clipping plane
         * @param far The far clipping plane
         * @param dst Optional matrix to store the result in
         * @return The perspective projection matrix
         */
        fun perspective(
            fieldOfViewYInRadians: Float,
            aspect: Float,
            near: Float,
            far: Float,
            dst: Mat4 = Mat4()
        ): Mat4 {
            val f = 1.0f / tan(fieldOfViewYInRadians / 2)
            val rangeInv = 1.0f / (near - far)
            
            dst[0] = f / aspect
            dst[1] = 0f
            dst[2] = 0f
            dst[3] = 0f
            dst[4] = 0f
            dst[5] = f
            dst[6] = 0f
            dst[7] = 0f
            dst[8] = 0f
            dst[9] = 0f
            dst[10] = (near + far) * rangeInv
            dst[11] = -1f
            dst[12] = 0f
            dst[13] = 0f
            dst[14] = near * far * rangeInv * 2f
            dst[15] = 0f
            
            return dst
        }
    }
}