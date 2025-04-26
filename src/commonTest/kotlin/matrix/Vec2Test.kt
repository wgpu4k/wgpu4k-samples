package matrix

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.math.sqrt

class Vec2Test {
    
    @Test
    fun testConstructor() {
        val v1 = Vec2()
        assertEquals(0f, v1.x)
        assertEquals(0f, v1.y)
        
        val v2 = Vec2(1f, 2f)
        assertEquals(1f, v2.x)
        assertEquals(2f, v2.y)
    }
    
    @Test
    fun testClone() {
        val v1 = Vec2(1f, 2f)
        val v2 = v1.clone()
        
        assertEquals(v1.x, v2.x)
        assertEquals(v1.y, v2.y)
        
        // Ensure they are different objects
        v2.x = 3f
        assertNotEquals(v1.x, v2.x)
    }
    
    @Test
    fun testSet() {
        val v = Vec2()
        v.set(3f, 4f)
        
        assertEquals(3f, v.x)
        assertEquals(4f, v.y)
    }
    
    @Test
    fun testAdd() {
        val v1 = Vec2(1f, 2f)
        val v2 = Vec2(3f, 4f)
        val result = v1.add(v2)
        
        assertEquals(4f, result.x)
        assertEquals(6f, result.y)
        
        // Test with destination
        val dst = Vec2()
        val result2 = v1.add(v2, dst)
        
        assertEquals(4f, result2.x)
        assertEquals(6f, result2.y)
        assertEquals(dst, result2)
    }
    
    @Test
    fun testSubtract() {
        val v1 = Vec2(5f, 8f)
        val v2 = Vec2(2f, 3f)
        val result = v1.subtract(v2)
        
        assertEquals(3f, result.x)
        assertEquals(5f, result.y)
    }
    
    @Test
    fun testMultiply() {
        val v1 = Vec2(2f, 3f)
        val v2 = Vec2(4f, 5f)
        val result = v1.multiply(v2)
        
        assertEquals(8f, result.x)
        assertEquals(15f, result.y)
    }
    
    @Test
    fun testDivide() {
        val v1 = Vec2(8f, 10f)
        val v2 = Vec2(2f, 5f)
        val result = v1.divide(v2)
        
        assertEquals(4f, result.x)
        assertEquals(2f, result.y)
    }
    
    @Test
    fun testScale() {
        val v = Vec2(2f, 3f)
        val result = v.scale(2f)
        
        assertEquals(4f, result.x)
        assertEquals(6f, result.y)
    }
    
    @Test
    fun testDistance() {
        val v1 = Vec2(0f, 0f)
        val v2 = Vec2(3f, 4f)
        
        assertEquals(5f, v1.distance(v2))
    }
    
    @Test
    fun testDistanceSquared() {
        val v1 = Vec2(0f, 0f)
        val v2 = Vec2(3f, 4f)
        
        assertEquals(25f, v1.distanceSquared(v2))
    }
    
    @Test
    fun testLength() {
        val v = Vec2(3f, 4f)
        
        assertEquals(5f, v.length())
    }
    
    @Test
    fun testLengthSquared() {
        val v = Vec2(3f, 4f)
        
        assertEquals(25f, v.lengthSquared())
    }
    
    @Test
    fun testNormalize() {
        val v = Vec2(3f, 4f)
        val result = v.normalize()
        
        assertEquals(0.6f, result.x, 0.0001f)
        assertEquals(0.8f, result.y, 0.0001f)
        assertEquals(1f, result.length(), 0.0001f)
    }
    
    @Test
    fun testDot() {
        val v1 = Vec2(2f, 3f)
        val v2 = Vec2(4f, 5f)
        
        assertEquals(23f, v1.dot(v2))
    }
    
    @Test
    fun testLerp() {
        val v1 = Vec2(0f, 0f)
        val v2 = Vec2(10f, 10f)
        
        val result1 = v1.lerp(v2, 0f)
        assertEquals(0f, result1.x)
        assertEquals(0f, result1.y)
        
        val result2 = v1.lerp(v2, 0.5f)
        assertEquals(5f, result2.x)
        assertEquals(5f, result2.y)
        
        val result3 = v1.lerp(v2, 1f)
        assertEquals(10f, result3.x)
        assertEquals(10f, result3.y)
    }
    
    @Test
    fun testEqualsApproximately() {
        val v1 = Vec2(1f, 2f)
        val v2 = Vec2(1.0000001f, 2.0000001f)
        val v3 = Vec2(1.1f, 2.1f)
        
        assertTrue(v1.equalsApproximately(v2))
        assertFalse(v1.equalsApproximately(v3))
    }
    
    @Test
    fun testEquals() {
        val v1 = Vec2(1f, 2f)
        val v2 = Vec2(1f, 2f)
        val v3 = Vec2(3f, 4f)
        
        assertEquals(v1, v2)
        assertNotEquals(v1, v3)
    }
    
    @Test
    fun testZero() {
        val v = Vec2.zero()
        
        assertEquals(0f, v.x)
        assertEquals(0f, v.y)
    }
    
    @Test
    fun testRandom() {
        val v = Vec2.random(5f)
        
        assertTrue(v.x >= 0f && v.x <= 5f)
        assertTrue(v.y >= 0f && v.y <= 5f)
    }
}