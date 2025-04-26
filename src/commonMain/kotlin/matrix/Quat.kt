package matrix

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A quaternion representing a rotation.
 */
class Quat(val data: FloatArray) {
    constructor(x: Float = 0f, y: Float = 0f, z: Float = 0f, w: Float = 1f) : this(floatArrayOf(x, y, z, w))
    
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
     * Creates a clone of this quaternion.
     *
     * @return A new quaternion with the same values
     */
    fun clone(): Quat = Quat(x, y, z, w)
    
    /**
     * Sets the values of this quaternion.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @param w W component
     * @return This quaternion with updated values
     */
    fun set(x: Float, y: Float, z: Float, w: Float): Quat {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }
    
    /**
     * Multiplies this quaternion by another quaternion.
     *
     * @param b The quaternion to multiply by
     * @param dst Optional quaternion to store the result in
     * @return The product of the two quaternions
     */
    fun multiply(b: Quat, dst: Quat = Quat()): Quat {
        val ax = x
        val ay = y
        val az = z
        val aw = w
        val bx = b.x
        val by = b.y
        val bz = b.z
        val bw = b.w
        
        dst.x = ax * bw + aw * bx + ay * bz - az * by
        dst.y = ay * bw + aw * by + az * bx - ax * bz
        dst.z = az * bw + aw * bz + ax * by - ay * bx
        dst.w = aw * bw - ax * bx - ay * by - az * bz
        
        return dst
    }
    
    /**
     * Normalizes this quaternion.
     *
     * @param dst Optional quaternion to store the result in
     * @return The normalized quaternion
     */
    fun normalize(dst: Quat = Quat()): Quat {
        val length = sqrt(x * x + y * y + z * z + w * w)
        
        if (length > 0.00001f) {
            dst.x = x / length
            dst.y = y / length
            dst.z = z / length
            dst.w = w / length
        } else {
            dst.x = 0f
            dst.y = 0f
            dst.z = 0f
            dst.w = 1f
        }
        
        return dst
    }
    
    /**
     * Inverts this quaternion.
     *
     * @param dst Optional quaternion to store the result in
     * @return The inverted quaternion
     */
    fun inverse(dst: Quat = Quat()): Quat {
        val lengthSq = x * x + y * y + z * z + w * w
        
        if (lengthSq > 0.00001f) {
            val invLengthSq = 1f / lengthSq
            dst.x = -x * invLengthSq
            dst.y = -y * invLengthSq
            dst.z = -z * invLengthSq
            dst.w = w * invLengthSq
        } else {
            dst.x = 0f
            dst.y = 0f
            dst.z = 0f
            dst.w = 1f
        }
        
        return dst
    }
    
    /**
     * Calculates the dot product of this quaternion and another quaternion.
     *
     * @param b The second quaternion
     * @return The dot product
     */
    fun dot(b: Quat): Float {
        return x * b.x + y * b.y + z * b.z + w * b.w
    }
    
    /**
     * Linearly interpolates between this quaternion and another quaternion.
     *
     * @param b The end quaternion
     * @param t The interpolation coefficient
     * @param dst Optional quaternion to store the result in
     * @return The interpolated quaternion
     */
    fun lerp(b: Quat, t: Float, dst: Quat = Quat()): Quat {
        dst.x = x + (b.x - x) * t
        dst.y = y + (b.y - y) * t
        dst.z = z + (b.z - z) * t
        dst.w = w + (b.w - w) * t
        return dst.normalize()
    }
    
    /**
     * Spherically interpolates between this quaternion and another quaternion.
     *
     * @param b The end quaternion
     * @param t The interpolation coefficient
     * @param dst Optional quaternion to store the result in
     * @return The interpolated quaternion
     */
    fun slerp(b: Quat, t: Float, dst: Quat = Quat()): Quat {
        var cosHalfTheta = dot(b)
        
        // If the dot product is negative, slerp won't take the shorter path.
        // Fix by reversing one quaternion.
        val bx: Float
        val by: Float
        val bz: Float
        val bw: Float
        
        if (cosHalfTheta < 0f) {
            cosHalfTheta = -cosHalfTheta
            bx = -b.x
            by = -b.y
            bz = -b.z
            bw = -b.w
        } else {
            bx = b.x
            by = b.y
            bz = b.z
            bw = b.w
        }
        
        // If the quaternions are very close, use linear interpolation
        if (cosHalfTheta > 0.9999f) {
            dst.x = x + (bx - x) * t
            dst.y = y + (by - y) * t
            dst.z = z + (bz - z) * t
            dst.w = w + (bw - w) * t
            return dst.normalize()
        }
        
        // Calculate the slerp coefficients
        val halfTheta = acos(cosHalfTheta)
        val sinHalfTheta = sqrt(1.0f - cosHalfTheta * cosHalfTheta)
        
        // If the angle is too close to zero, use linear interpolation
        if (abs(sinHalfTheta) < 0.001f) {
            dst.x = x * 0.5f + bx * 0.5f
            dst.y = y * 0.5f + by * 0.5f
            dst.z = z * 0.5f + bz * 0.5f
            dst.w = w * 0.5f + bw * 0.5f
            return dst.normalize()
        }
        
        val ratioA = sin((1f - t) * halfTheta) / sinHalfTheta
        val ratioB = sin(t * halfTheta) / sinHalfTheta
        
        dst.x = x * ratioA + bx * ratioB
        dst.y = y * ratioA + by * ratioB
        dst.z = z * ratioA + bz * ratioB
        dst.w = w * ratioA + bw * ratioB
        
        return dst
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quat) return false
        return x == other.x && y == other.y && z == other.z && w == other.w
    }
    
    /**
     * Checks if this quaternion is approximately equal to another quaternion.
     *
     * @param b The second quaternion
     * @return True if the quaternions are approximately equal
     */
    fun equalsApproximately(b: Quat): Boolean {
        return abs(x - b.x) < EPSILON && 
               abs(y - b.y) < EPSILON &&
               abs(z - b.z) < EPSILON &&
               abs(w - b.w) < EPSILON
    }
    
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + w.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "Quat(x=$x, y=$y, z=$z, w=$w)"
    }
    
    companion object {
        /**
         * Creates an identity quaternion.
         *
         * @param dst Optional quaternion to store the result in
         * @return The identity quaternion
         */
        fun identity(dst: Quat = Quat()): Quat {
            dst.x = 0f
            dst.y = 0f
            dst.z = 0f
            dst.w = 1f
            return dst
        }
        
        /**
         * Creates a quaternion from axis-angle representation.
         *
         * @param axis The axis of rotation
         * @param angleInRadians The angle of rotation in radians
         * @param dst Optional quaternion to store the result in
         * @return The quaternion
         */
        fun fromAxisAngle(axis: Vec3, angleInRadians: Float, dst: Quat = Quat()): Quat {
            val halfAngle = angleInRadians * 0.5f
            val s = sin(halfAngle)
            
            dst.x = axis.x * s
            dst.y = axis.y * s
            dst.z = axis.z * s
            dst.w = cos(halfAngle)
            
            return dst.normalize()
        }
        
        /**
         * Creates a quaternion from Euler angles.
         *
         * @param x Rotation around X axis in radians
         * @param y Rotation around Y axis in radians
         * @param z Rotation around Z axis in radians
         * @param dst Optional quaternion to store the result in
         * @return The quaternion
         */
        fun fromEuler(x: Float, y: Float, z: Float, dst: Quat = Quat()): Quat {
            val halfX = x * 0.5f
            val halfY = y * 0.5f
            val halfZ = z * 0.5f
            
            val sx = sin(halfX)
            val cx = cos(halfX)
            val sy = sin(halfY)
            val cy = cos(halfY)
            val sz = sin(halfZ)
            val cz = cos(halfZ)
            
            dst.x = sx * cy * cz - cx * sy * sz
            dst.y = cx * sy * cz + sx * cy * sz
            dst.z = cx * cy * sz - sx * sy * cz
            dst.w = cx * cy * cz + sx * sy * sz
            
            return dst
        }
    }
}