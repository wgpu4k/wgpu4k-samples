package matrix

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A 4-element vector.
 */
class Vec4(val data: FloatArray) {
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f, w: Float = 0f) : this(floatArrayOf(x, y, z, w))
    
    var x: Float
        get() = data[0]
        set(value) { data[0] = value }
    
    var y: Float
        get() = data[1]
        set(value) { data[1] = value }
    
    var z: Float
        get() = data[2]
        set(value) { data[2] = value }
    
    var w: Float
        get() = data[3]
        set(value) { data[3] = value }
    
    operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }
    
    /**
     * Creates a clone of this vector.
     *
     * @return A new vector with the same values
     */
    fun clone(): Vec4 = Vec4(x, y, z, w)
    
    /**
     * Sets the values of this vector.
     *
     * @param x First element
     * @param y Second element
     * @param z Third element
     * @param w Fourth element
     * @return This vector with updated values
     */
    fun set(x: Float, y: Float, z: Float, w: Float): Vec4 {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }
    
    /**
     * Adds another vector to this vector.
     *
     * @param b The vector to add
     * @param dst Optional vector to store the result in
     * @return The sum of the two vectors
     */
    fun add(b: Vec4, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x + b.x
        dst.y = y + b.y
        dst.z = z + b.z
        dst.w = w + b.w
        return dst
    }
    
    /**
     * Subtracts another vector from this vector.
     *
     * @param b The vector to subtract
     * @param dst Optional vector to store the result in
     * @return The difference of the two vectors
     */
    fun subtract(b: Vec4, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x - b.x
        dst.y = y - b.y
        dst.z = z - b.z
        dst.w = w - b.w
        return dst
    }
    
    /**
     * Multiplies this vector by another vector.
     *
     * @param b The vector to multiply by
     * @param dst Optional vector to store the result in
     * @return The product of the two vectors
     */
    fun multiply(b: Vec4, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x * b.x
        dst.y = y * b.y
        dst.z = z * b.z
        dst.w = w * b.w
        return dst
    }
    
    /**
     * Divides this vector by another vector.
     *
     * @param b The vector to divide by
     * @param dst Optional vector to store the result in
     * @return The quotient of the two vectors
     */
    fun divide(b: Vec4, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x / b.x
        dst.y = y / b.y
        dst.z = z / b.z
        dst.w = w / b.w
        return dst
    }
    
    /**
     * Scales this vector by a scalar.
     *
     * @param k The scalar value
     * @param dst Optional vector to store the result in
     * @return The scaled vector
     */
    fun scale(k: Float, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x * k
        dst.y = y * k
        dst.z = z * k
        dst.w = w * k
        return dst
    }
    
    /**
     * Calculates the distance between this vector and another vector.
     *
     * @param b The second vector
     * @return The distance between the two vectors
     */
    fun distance(b: Vec4): Float {
        val dx = x - b.x
        val dy = y - b.y
        val dz = z - b.z
        val dw = w - b.w
        return sqrt(dx * dx + dy * dy + dz * dz + dw * dw)
    }
    
    /**
     * Calculates the squared distance between this vector and another vector.
     *
     * @param b The second vector
     * @return The squared distance between the two vectors
     */
    fun distanceSquared(b: Vec4): Float {
        val dx = x - b.x
        val dy = y - b.y
        val dz = z - b.z
        val dw = w - b.w
        return dx * dx + dy * dy + dz * dz + dw * dw
    }
    
    /**
     * Calculates the length (magnitude) of this vector.
     *
     * @return The length of the vector
     */
    fun length(): Float {
        return sqrt(x * x + y * y + z * z + w * w)
    }
    
    /**
     * Calculates the squared length of this vector.
     *
     * @return The squared length of the vector
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z + w * w
    }
    
    /**
     * Normalizes this vector.
     *
     * @param dst Optional vector to store the result in
     * @return The normalized vector
     */
    fun normalize(dst: Vec4 = Vec4()): Vec4 {
        val length = this.length()
        if (length > 0.00001f) {
            dst.x = x / length
            dst.y = y / length
            dst.z = z / length
            dst.w = w / length
        } else {
            dst.x = 0f
            dst.y = 0f
            dst.z = 0f
            dst.w = 0f
        }
        return dst
    }
    
    /**
     * Negates this vector.
     *
     * @param dst Optional vector to store the result in
     * @return The negated vector
     */
    fun negate(dst: Vec4 = Vec4()): Vec4 {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = -w
        return dst
    }
    
    /**
     * Computes the dot product of this vector and another vector.
     *
     * @param b The second vector
     * @return The dot product
     */
    fun dot(b: Vec4): Float {
        return x * b.x + y * b.y + z * b.z + w * b.w
    }
    
    /**
     * Linearly interpolates between this vector and another vector.
     *
     * @param b The end vector
     * @param t The interpolation coefficient
     * @param dst Optional vector to store the result in
     * @return The interpolated vector
     */
    fun lerp(b: Vec4, t: Float, dst: Vec4 = Vec4()): Vec4 {
        dst.x = x + (b.x - x) * t
        dst.y = y + (b.y - y) * t
        dst.z = z + (b.z - z) * t
        dst.w = w + (b.w - w) * t
        return dst
    }
    
    /**
     * Checks if this vector is approximately equal to another vector.
     *
     * @param b The second vector
     * @return True if the vectors are approximately equal
     */
    fun equalsApproximately(b: Vec4): Boolean {
        return abs(x - b.x) < EPSILON && 
               abs(y - b.y) < EPSILON &&
               abs(z - b.z) < EPSILON &&
               abs(w - b.w) < EPSILON
    }
    
    /**
     * Transforms this vector by a 4x4 matrix.
     *
     * @param m The 4x4 matrix
     * @param dst Optional vector to store the result in
     * @return The transformed vector
     */
    fun transformMat4(m: FloatArray, dst: Vec4 = Vec4()): Vec4 {
        val x = this.x
        val y = this.y
        val z = this.z
        val w = this.w
        
        dst.x = m[0] * x + m[4] * y + m[8] * z + m[12] * w
        dst.y = m[1] * x + m[5] * y + m[9] * z + m[13] * w
        dst.z = m[2] * x + m[6] * y + m[10] * z + m[14] * w
        dst.w = m[3] * x + m[7] * y + m[11] * z + m[15] * w
        
        return dst
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec4) return false
        return x == other.x && y == other.y && z == other.z && w == other.w
    }
    
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + w.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Vec4(x=$x, y=$y, z=$z, w=$w)"
    }
    
    companion object {
        /**
         * Creates a random vector.
         *
         * @param scale The scale of the random values
         * @return The random vector
         */
        fun random(scale: Float = 1f): Vec4 {
            return Vec4(
                Random.nextFloat() * scale,
                Random.nextFloat() * scale,
                Random.nextFloat() * scale,
                Random.nextFloat() * scale
            )
        }
        
        /**
         * Creates a zero vector.
         *
         * @return The zero vector
         */
        fun zero(): Vec4 {
            return Vec4(0f, 0f, 0f, 0f)
        }
        
        /**
         * Creates a vector from values stored in an array at a specified offset.
         *
         * @param a The array to read values from
         * @param offset The offset at which to start reading values
         * @return The vector with values from the array
         */
        fun fromArray(a: FloatArray, offset: Int = 0): Vec4 {
            return Vec4(a[offset], a[offset + 1], a[offset + 2], a[offset + 3])
        }
    }
}