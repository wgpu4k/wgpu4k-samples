package matrix

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A 3-element vector.
 */
class Vec3(val data: FloatArray) {
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f) : this(floatArrayOf(x, y, z))
    
    var x: Float
        get() = data[0]
        set(value) { data[0] = value }
    
    var y: Float
        get() = data[1]
        set(value) { data[1] = value }
    
    var z: Float
        get() = data[2]
        set(value) { data[2] = value }
    
    operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }
    
    /**
     * Creates a clone of this vector.
     *
     * @return A new vector with the same values
     */
    fun clone(): Vec3 = Vec3(x, y, z)
    
    /**
     * Sets the values of this vector.
     *
     * @param x First element
     * @param y Second element
     * @param z Third element
     * @return This vector with updated values
     */
    fun set(x: Float, y: Float, z: Float): Vec3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }
    
    /**
     * Adds another vector to this vector.
     *
     * @param b The vector to add
     * @param dst Optional vector to store the result in
     * @return The sum of the two vectors
     */
    fun add(b: Vec3, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x + b.x
        dst.y = y + b.y
        dst.z = z + b.z
        return dst
    }
    
    /**
     * Subtracts another vector from this vector.
     *
     * @param b The vector to subtract
     * @param dst Optional vector to store the result in
     * @return The difference of the two vectors
     */
    fun subtract(b: Vec3, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x - b.x
        dst.y = y - b.y
        dst.z = z - b.z
        return dst
    }
    
    /**
     * Multiplies this vector by another vector.
     *
     * @param b The vector to multiply by
     * @param dst Optional vector to store the result in
     * @return The product of the two vectors
     */
    fun multiply(b: Vec3, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x * b.x
        dst.y = y * b.y
        dst.z = z * b.z
        return dst
    }
    
    /**
     * Divides this vector by another vector.
     *
     * @param b The vector to divide by
     * @param dst Optional vector to store the result in
     * @return The quotient of the two vectors
     */
    fun divide(b: Vec3, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x / b.x
        dst.y = y / b.y
        dst.z = z / b.z
        return dst
    }
    
    /**
     * Scales this vector by a scalar.
     *
     * @param k The scalar value
     * @param dst Optional vector to store the result in
     * @return The scaled vector
     */
    fun scale(k: Float, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x * k
        dst.y = y * k
        dst.z = z * k
        return dst
    }
    
    /**
     * Calculates the distance between this vector and another vector.
     *
     * @param b The second vector
     * @return The distance between the two vectors
     */
    fun distance(b: Vec3): Float {
        val dx = x - b.x
        val dy = y - b.y
        val dz = z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Calculates the squared distance between this vector and another vector.
     *
     * @param b The second vector
     * @return The squared distance between the two vectors
     */
    fun distanceSquared(b: Vec3): Float {
        val dx = x - b.x
        val dy = y - b.y
        val dz = z - b.z
        return dx * dx + dy * dy + dz * dz
    }
    
    /**
     * Calculates the length (magnitude) of this vector.
     *
     * @return The length of the vector
     */
    fun length(): Float {
        return sqrt(x * x + y * y + z * z)
    }
    
    /**
     * Calculates the squared length of this vector.
     *
     * @return The squared length of the vector
     */
    fun lengthSquared(): Float {
        return x * x + y * y + z * z
    }
    
    /**
     * Normalizes this vector.
     *
     * @param dst Optional vector to store the result in
     * @return The normalized vector
     */
    fun normalize(dst: Vec3 = Vec3()): Vec3 {
        val length = this.length()
        if (length > 0.00001f) {
            dst.x = x / length
            dst.y = y / length
            dst.z = z / length
        } else {
            dst.x = 0f
            dst.y = 0f
            dst.z = 0f
        }
        return dst
    }
    
    /**
     * Negates this vector.
     *
     * @param dst Optional vector to store the result in
     * @return The negated vector
     */
    fun negate(dst: Vec3 = Vec3()): Vec3 {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        return dst
    }
    
    /**
     * Computes the cross product of this vector and another vector.
     *
     * @param b The second vector
     * @param dst Optional vector to store the result in
     * @return The cross product
     */
    fun cross(b: Vec3, dst: Vec3 = Vec3()): Vec3 {
        val ax = x
        val ay = y
        val az = z
        val bx = b.x
        val by = b.y
        val bz = b.z
        
        dst.x = ay * bz - az * by
        dst.y = az * bx - ax * bz
        dst.z = ax * by - ay * bx
        
        return dst
    }
    
    /**
     * Computes the dot product of this vector and another vector.
     *
     * @param b The second vector
     * @return The dot product
     */
    fun dot(b: Vec3): Float {
        return x * b.x + y * b.y + z * b.z
    }
    
    /**
     * Computes the angle between this vector and another vector.
     *
     * @param b The second vector
     * @return The angle in radians
     */
    fun angle(b: Vec3): Float {
        val mag1 = sqrt(x * x + y * y + z * z)
        val mag2 = sqrt(b.x * b.x + b.y * b.y + b.z * b.z)
        
        if (mag1 == 0f || mag2 == 0f) {
            return 0f
        }
        
        val cosine = (x * b.x + y * b.y + z * b.z) / (mag1 * mag2)
        
        return if (cosine > 1f) {
            0f
        } else if (cosine < -1f) {
            kotlin.math.PI.toFloat()
        } else {
            acos(cosine)
        }
    }
    
    /**
     * Linearly interpolates between this vector and another vector.
     *
     * @param b The end vector
     * @param t The interpolation coefficient
     * @param dst Optional vector to store the result in
     * @return The interpolated vector
     */
    fun lerp(b: Vec3, t: Float, dst: Vec3 = Vec3()): Vec3 {
        dst.x = x + (b.x - x) * t
        dst.y = y + (b.y - y) * t
        dst.z = z + (b.z - z) * t
        return dst
    }
    
    /**
     * Checks if this vector is approximately equal to another vector.
     *
     * @param b The second vector
     * @return True if the vectors are approximately equal
     */
    fun equalsApproximately(b: Vec3): Boolean {
        return abs(x - b.x) < EPSILON && 
               abs(y - b.y) < EPSILON &&
               abs(z - b.z) < EPSILON
    }
    
    /**
     * Transforms this vector by a 4x4 matrix.
     *
     * @param m The 4x4 matrix
     * @param dst Optional vector to store the result in
     * @return The transformed vector
     */
    fun transformMat4(m: FloatArray, dst: Vec3 = Vec3()): Vec3 {
        val x = this.x
        val y = this.y
        val z = this.z
        val w = m[3] * x + m[7] * y + m[11] * z + m[15]
        
        dst.x = (m[0] * x + m[4] * y + m[8] * z + m[12]) / w
        dst.y = (m[1] * x + m[5] * y + m[9] * z + m[13]) / w
        dst.z = (m[2] * x + m[6] * y + m[10] * z + m[14]) / w
        
        return dst
    }
    
    /**
     * Transforms this vector by a 3x3 matrix.
     *
     * @param m The 3x3 matrix
     * @param dst Optional vector to store the result in
     * @return The transformed vector
     */
    fun transformMat3(m: FloatArray, dst: Vec3 = Vec3()): Vec3 {
        val x = this.x
        val y = this.y
        val z = this.z
        
        dst.x = m[0] * x + m[3] * y + m[6] * z
        dst.y = m[1] * x + m[4] * y + m[7] * z
        dst.z = m[2] * x + m[5] * y + m[8] * z
        
        return dst
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3) return false
        return x == other.x && y == other.y && z == other.z
    }
    
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Vec3(x=$x, y=$y, z=$z)"
    }
    
    companion object {
        fun create() {
            TODO()
        }
        /**
         * Creates a random vector.
         *
         * @param scale The scale of the random values
         * @return The random vector
         */
        fun random(scale: Float = 1f): Vec3 {
            return Vec3(
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
        fun zero(): Vec3 {
            return Vec3(0f, 0f, 0f)
        }
        
        /**
         * Creates a vector from values stored in an array at a specified offset.
         *
         * @param a The array to read values from
         * @param offset The offset at which to start reading values
         * @return The vector with values from the array
         */
        fun fromArray(a: FloatArray, offset: Int = 0): Vec3 {
            return Vec3(a[offset], a[offset + 1], a[offset + 2])
        }
    }
}