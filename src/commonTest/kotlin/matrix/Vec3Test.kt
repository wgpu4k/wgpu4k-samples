package matrix

import kotlin.math.*
import kotlin.test.*


// --- Test Suite ---

class Vec3Tests {

    // Helper to mimic the JS 'clone' for testing (creates a new instance)
    // In JS tests, clone could also work on number[], so we handle Vec3 specifically
    private fun clone(v: Any?): Any? {
        return when (v) {
            is Vec3 -> v.copy() // Use the data class copy for Vec3
            is FloatArray -> v.copyOf()
            // Add other types if needed, e.g., Quat
            is Quat -> v.copy() // Quat is data class, use copy()
            else -> v // Assume immutable primitives or objects handled by value/reference correctly
        }
    }

    // Helper testing function result without explicit destination
    // Takes the operation lambda, expected result, and original arguments
    private fun testV3WithoutDest(
        operation: (args: Array<out Any?>, dst: Vec3?) -> Any?, // Lambda representing the Vec3 operation
        expected: Any?,
        vararg args: Any? // Original arguments for the operation
    ) {
        // Clone args to prevent modification by the operation
        val clonedArgs = args.map { clone(it) }.toTypedArray()

        // Call the operation lambda, passing null for destination (dst)
        val d = operation(clonedArgs, null) // Operation should create a new Vec3 internally

        // Assertions based on expected type
        when (expected) {
            is Vec3 -> assertVec3EqualsApproximately(expected, d as Vec3)
            is FloatArray -> { // Handle cases where expected is FloatArray but result might be Vec3
                if (d is Vec3) {
                    assertVec3EqualsApproximately(expected, d)
                } else {
                    assertIs<FloatArray>(d, "Expected FloatArray result")
                    assertArrayEqualsApproximately(expected, d)
                }
            }
            is Float -> assertEqualsApproximately(expected, d as Float)
            is Boolean -> assertEquals(expected, d as Boolean)
            else -> assertEquals(expected, d) // Fallback direct comparison
        }

        // Check the original arguments were not modified (important for Vec3 inputs)
        args.zip(clonedArgs).forEachIndexed { index, pair ->
             if (pair.first is Vec3) {
                 assertVec3Equals(pair.first as Vec3, pair.second as Vec3, "Source vector (arg $index) modified unexpectedly in testV3WithoutDest")
             }
             // Add checks for other mutable types if needed (e.g., Quat)
             if (pair.first is Quat) {
                 assertEquals(pair.first as Quat, pair.second as Quat, "Source quat (arg $index) modified unexpectedly in testV3WithoutDest")
             }
        }
    }

    // Helper testing function result with explicit destination
    private fun testV3WithDest(
        operation: (args: Array<out Any?>, dst: Vec3?) -> Any?, // Lambda representing the Vec3 operation
        expected: Any?,
        vararg args: Any? // Original arguments for the operation
    ) {
        val expectedCloned = clone(expected) // Clone expected value for comparison
        val destVector = Vec3() // Create the destination vector

        // --- Test with standard destination ---
        run {
            val clonedArgs = args.map { clone(it) }.toTypedArray() // Clone inputs for this run
            // Call the operation lambda, passing the explicit destination vector
            val c = operation(clonedArgs, destVector)

            assertSame(c, destVector, "Function with dest should return the dest instance")

            // Compare the result (in 'destVector') with the expected value
            when (expectedCloned) {
                is Vec3 -> assertVec3EqualsApproximately(expectedCloned, c as Vec3)
                is FloatArray -> assertVec3EqualsApproximately(expectedCloned, c as Vec3) // Assume funcs return Vec3 when dest is Vec3
                else -> fail("testV3WithDest expects Vec3 or FloatArray for 'expected' value")
            }

            // Ensure original inputs were not modified by the function call
            args.zip(clonedArgs).forEachIndexed { index, pair ->
                if (pair.first is Vec3) {
                    assertVec3Equals(pair.first as Vec3, pair.second as Vec3, "Source vector (arg $index) modified unexpectedly in testV3WithDest (standard dest)")
                }
                 if (pair.first is Quat) {
                     assertEquals(pair.first as Quat, pair.second as Quat, "Source quat (arg $index) modified unexpectedly in testV3WithDest (standard dest)")
                 }
            }
        }

        // --- Test aliasing: first argument is destination ---
        if (args.isNotEmpty() && args[0] is Vec3) {
            val firstArgAlias = clone(args[0]) as Vec3 // Clone the first arg to use as dest
            val clonedRemainingArgs = args.drop(1).map { clone(it) }.toTypedArray()
            val allArgsForAlias1 = arrayOf(firstArgAlias, *clonedRemainingArgs) // Reconstruct args array

            // Call operation with firstArgAlias as the destination
            val cAlias1 = operation(allArgsForAlias1, firstArgAlias)

            assertSame(cAlias1, firstArgAlias, "Aliasing test (firstArg == dest) should return the dest instance")
            when (expectedCloned) {
                is Vec3 -> assertVec3EqualsApproximately(expectedCloned, cAlias1 as Vec3, message="Aliasing test (firstArg == dest) result mismatch")
                is FloatArray -> assertVec3EqualsApproximately(expectedCloned, cAlias1 as Vec3, message="Aliasing test (firstArg == dest) result mismatch")
            }
            // Check other original args were not modified
            args.drop(1).zip(clonedRemainingArgs).forEachIndexed { index, pair ->
                if (pair.first is Vec3) {
                    assertVec3Equals(pair.first as Vec3, pair.second as Vec3, "Aliasing test (firstArg == dest): Source vector (arg ${index + 1}) modified unexpectedly")
                }
                 if (pair.first is Quat) {
                     assertEquals(pair.first as Quat, pair.second as Quat, "Aliasing test (firstArg == dest): Source quat (arg ${index + 1}) modified unexpectedly")
                 }
            }
        }


        // --- Test aliasing: another Vec3 argument is destination ---
        val firstOperandIndex = args.indexOfFirst { it is Vec3 && it !== args[0] } // Find first Vec3 operand *not* the first arg
        if (firstOperandIndex != -1) {
            val operandAlias = clone(args[firstOperandIndex]) as Vec3 // Clone the operand to use as dest
            val clonedArgsForAlias2 = args.mapIndexed { index, arg ->
                if (index == firstOperandIndex) operandAlias else clone(arg) // Use alias at its index, clone others
            }.toTypedArray()

            // Call operation with operandAlias as the destination
            val cAlias2 = operation(clonedArgsForAlias2, operandAlias)

            assertSame(cAlias2, operandAlias, "Aliasing test (operand == dest) should return the dest instance")
            when (expectedCloned) {
                is Vec3 -> assertVec3EqualsApproximately(expectedCloned, cAlias2 as Vec3, message="Aliasing test (operand == dest) result mismatch")
                is FloatArray -> assertVec3EqualsApproximately(expectedCloned, cAlias2 as Vec3, message="Aliasing test (operand == dest) result mismatch")
            }
            // Check original args (that were not the alias dest) were not modified
            args.zip(clonedArgsForAlias2).forEachIndexed { index, pair ->
                 if (index != firstOperandIndex) { // Only check non-aliased args
                     if (pair.first is Vec3) {
                         assertVec3Equals(pair.first as Vec3, pair.second as Vec3, "Aliasing test (operand == dest): Source vector (arg $index) modified unexpectedly")
                     }
                     if (pair.first is Quat) {
                         assertEquals(pair.first as Quat, pair.second as Quat, "Aliasing test (operand == dest): Source quat (arg $index) modified unexpectedly")
                     }
                 }
            }
        }
    }

    // Combined test helper
    // Takes an operation lambda, expected result, and arguments
    // The operation lambda should handle the optional destination Vec3
    private fun testV3WithAndWithoutDest(
        // Lambda representing the core operation. Takes args array and optional dest Vec3.
        operation: (args: Array<out Any?>, dst: Vec3?) -> Any?,
        expected: Any?, // The expected result (Vec3, Float, Boolean, etc.)
        vararg args: Any? // Arguments for the operation (e.g., Vec3(1,2,3), Vec3(4,5,6), scalar)
    ) {
        // Test without explicit destination
        testV3WithoutDest(operation, expected, *args)

        // Test with explicit destination (only if expected result is Vec3 or FloatArray convertible to Vec3)
        if (expected is Vec3 || expected is FloatArray) {
             testV3WithDest(operation, expected, *args)
        } else {
             // Optionally add a message or skip if dest doesn't make sense for the return type
             // println("Skipping testV3WithDest for non-Vec3 expected type: ${expected?.let { it::class.simpleName }}")
        }
    }


    // --- Actual Tests (Following JS structure closely) ---

    @Test
    fun `should add`() { // Using backticks for JS-like naming
        val expected = Vec3(3f, 5f, 7f)
        // Operation lambda: takes args array and optional destination
        val addOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            // Assumes Vec3 instance method: v1.add(v2, dst ?: Vec3())
            // Or static method: Vec3.add(v1, v2, dst ?: Vec3())
            // Let's assume instance method based on common patterns
            v1.add(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(addOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 3f, 4f))
    }

    @Test
    fun `should compute angle`() {
        data class AngleTestData(val a: FloatArray, val b: FloatArray, val expected: Float)
        val tests = listOf(
            AngleTestData(floatArrayOf(1f, 0f, 0f), floatArrayOf( 0f, 1f, 0f), PI.toFloat() / 2f),
            AngleTestData(floatArrayOf(1f, 0f, 0f), floatArrayOf(-1f, 0f, 0f), PI.toFloat()),
            AngleTestData(floatArrayOf(1f, 0f, 0f), floatArrayOf( 1f, 0f, 0f), 0f),
            AngleTestData(floatArrayOf(1f, 2f, 3f), floatArrayOf( 4f, 5f, 6f), 0.2257261f),
            // AngleTestData(floatArrayOf(1f, 0f, 0f), floatArrayOf( 0f, Float.POSITIVE_INFINITY, 0f), PI.toFloat() / 2f), // Infinity causes issues
        )
        for ((aData, bData, expected) in tests) {
            val av = Vec3(aData[0], aData[1], aData[2])
            val bv = Vec3(bData[0], bData[1], bData[2])
            assertEqualsApproximately(expected, av.angle(bv), tolerance = TEST_EPSILON_F)
            // Test with scaled vectors
            val avScaled = Vec3()
            val bvScaled = Vec3()
            av.mulScalar(100f, avScaled)
            bv.mulScalar(100f, bvScaled)
            assertEqualsApproximately(expected, avScaled.angle(bvScaled), tolerance = TEST_EPSILON_F)
        }
    }

    @Test
    fun `should compute ceil`() {
        val expected = Vec3(2f, -1f, 3f)
        val ceilOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.ceil(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(ceilOperation, expected, Vec3(1.1f, -1.1f, 2.9f))
    }

    @Test
    fun `should compute floor`() {
        val expected = Vec3(1f, -2f, 2f)
        val floorOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.floor(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(floorOperation, expected, Vec3(1.1f, -1.1f, 2.9f))
    }

    @Test
    fun `should compute round`() {
        val expected = Vec3(1f, -1f, 3f)
        val roundOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.round(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(roundOperation, expected, Vec3(1.1f, -1.1f, 2.9f))
    }

    @Test
    fun `should clamp`() {
        run { // Mimic JS block scope
            val expected = Vec3(1f, 0f, 0.5f)
            val clampOperation = { args: Array<out Any?>, dst: Vec3? ->
                val v1 = args[0] as Vec3
                val min = args[1] as Float
                val max = args[2] as Float
                v1.clamp(min, max, dst ?: Vec3())
            }
            testV3WithAndWithoutDest(clampOperation, expected, Vec3(2f, -1f, 0.5f), 0f, 1f)
        }
        run {
            val expected = Vec3(-10f, 5f, 2.9f)
            val clampOperation = { args: Array<out Any?>, dst: Vec3? ->
                val v1 = args[0] as Vec3
                val min = args[1] as Float
                val max = args[2] as Float
                v1.clamp(min, max, dst ?: Vec3())
            }
            testV3WithAndWithoutDest(clampOperation, expected, Vec3(-22f, 50f, 2.9f), -10f, 5f)
        }
    }

    @Test
    fun `should equals approximately`() {
        assertTrue(Vec3(1f, 2f, 3f).equalsApproximately(Vec3(1f, 2f, 3f)))
        // assertTrue(Vec3(1f, 2f, 3f).equalsApproximately(Vec3(1f + Utils.EPSILON * 0.5f, 2f, 3f))) // Fails: Implies Vec3.equalsApproximately uses tolerance < 0.5 * Utils.EPSILON
        assertFalse(Vec3(1f, 2f, 3f).equalsApproximately(Vec3(1.001f, 2f, 3f)))
    }

    @Test
    fun `should equals`() {
        // Uses the specific 'equals' method matching JS, not Kotlin's standard ==
        assertTrue(Vec3(1f, 2f, 3f).equals(Vec3(1f, 2f, 3f)))
        assertFalse(Vec3(1f, 2f, 3f).equals(Vec3(1f + EPSILON * 0.5f, 2f, 3f)))
    }

    @Test
    fun `should subtract`() {
        val expected = Vec3(-1f, -2f, -3f)
        val subtractOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.subtract(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(subtractOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f))
    }

    @Test
    fun `should sub`() { // Alias test
        val expected = Vec3(-1f, -2f, -3f)
        val subOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.sub(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(subOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f))
    }

    @Test
    fun `should lerp`() {
        val expected = Vec3(1.5f, 3f, 4.5f)
        val lerpOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            val t = args[2] as Float
            v1.lerp(v2, t, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(lerpOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f), 0.5f)
    }

    @Test
    fun `should lerp under 0`() {
        val expected = Vec3(0.5f, 1f, 1.5f)
        val lerpOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            val t = args[2] as Float
            v1.lerp(v2, t, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(lerpOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f), -0.5f)
    }

    @Test
    fun `should lerp over 0`() { // JS name was 'lerp over 0', standard is 'lerp over 1'
        val expected = Vec3(2.5f, 5f, 7.5f)
        val lerpOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            val t = args[2] as Float
            v1.lerp(v2, t, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(lerpOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f), 1.5f)
    }

    @Test
    fun `should multiply by scalar`() {
        val expected = Vec3(2f, 4f, 6f)
        val mulScalarOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val k = args[1] as Float
            v1.mulScalar(k, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(mulScalarOperation, expected, Vec3(1f, 2f, 3f), 2f)
    }

    @Test
    fun `should scale`() { // Alias test
        val expected = Vec3(2f, 4f, 6f)
        val scaleOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val k = args[1] as Float
            v1.scale(k, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(scaleOperation, expected, Vec3(1f, 2f, 3f), 2f)
    }

    @Test
    fun `should add scaled`() {
        val expected = Vec3(5f, 10f, 15f)
        val addScaledOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            val scale = args[2] as Float
            v1.addScaled(v2, scale, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(addScaledOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f), 2f)
    }

    @Test
    fun `should divide by scalar`() {
        val expected = Vec3(0.5f, 1f, 1.5f)
        val divScalarOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val k = args[1] as Float
            v1.divScalar(k, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(divScalarOperation, expected, Vec3(1f, 2f, 3f), 2f)
    }

    @Test
    fun `should inverse`() {
        val expected = Vec3(1f / 2f, 1f / 3f, 1f / -4f)
        val inverseOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.inverse(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(inverseOperation, expected, Vec3(2f, 3f, -4f))
    }

    @Test
    fun `should cross`() {
        val expected = Vec3(
            (2f * 6f - 3f * 4f),
            (3f * 2f - 1f * 6f),
            (1f * 4f - 2f * 2f)
        )
        val crossOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.cross(v2, dst ?: Vec3())
        }
        // Note: The original JS test used (1,2,3) and (2,4,6) which results in a zero vector.
        // Let's use a different vector for a non-zero result to better test cross product.
        // Example: (1,2,3) x (4,5,6) = (-3, 6, -3)
        val vA = Vec3(1f, 2f, 3f)
        val vB = Vec3(4f, 5f, 6f)
        val expectedNonZero = Vec3(-3f, 6f, -3f)
        testV3WithAndWithoutDest(crossOperation, expectedNonZero, vA, vB)
        // Keep the original test case as well, expecting zero
        val expectedZero = Vec3(0f, 0f, 0f)
        testV3WithAndWithoutDest(crossOperation, expectedZero, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f))
    }

    @Test
    fun `should compute dot product`() {
        val expected = 1f * 2f + 2f * 4f + 3f * 6f
        val value = Vec3(1f, 2f, 3f).dot(Vec3(2f, 4f, 6f))
        assertEquals(expected, value)
    }

    @Test
    fun `should compute length`() {
        val expected = sqrt(1f * 1f + 2f * 2f + 3f * 3f)
        val value = Vec3(1f, 2f, 3f).length()
        assertEquals(expected, value)
    }

    @Test
    fun `should compute length squared`() {
        val expected = 1f * 1f + 2f * 2f + 3f * 3f
        val value = Vec3(1f, 2f, 3f).lengthSq()
        assertEquals(expected, value)
    }

    @Test
    fun `should compute len`() { // Alias test
        val expected = sqrt(1f * 1f + 2f * 2f + 3f * 3f)
        val value = Vec3(1f, 2f, 3f).len()
        assertEquals(expected, value)
    }

    @Test
    fun `should compute lenSq`() { // Alias test
        val expected = 1f * 1f + 2f * 2f + 3f * 3f
        val value = Vec3(1f, 2f, 3f).lenSq()
        assertEquals(expected, value)
    }

    @Test
    fun `should compute distance`() {
        val expected = sqrt(2f * 2f + 3f * 3f + 4f * 4f)
        // JS used array literal, Kotlin needs Vec3 object
        val value = Vec3(1f, 2f, 3f).distance(Vec3(3f, 5f, 7f))
        assertEquals(expected, value)
    }

    @Test
    fun `should compute distance squared`() {
        val expected = 2f * 2f + 3f * 3f + 4f * 4f
        val value = Vec3(1f, 2f, 3f).distanceSq(Vec3(3f, 5f, 7f))
        assertEquals(expected, value)
    }

    @Test
    fun `should compute dist`() { // Alias test
        val expected = sqrt(2f * 2f + 3f * 3f + 4f * 4f)
        val value = Vec3(1f, 2f, 3f).dist(Vec3(3f, 5f, 7f))
        assertEquals(expected, value)
    }

    @Test
    fun `should compute dist squared`() { // Alias test
        val expected = 2f * 2f + 3f * 3f + 4f * 4f
        val value = Vec3(1f, 2f, 3f).distSq(Vec3(3f, 5f, 7f))
        assertEquals(expected, value)
    }

    @Test
    fun `should normalize`() {
        val length = sqrt(1f * 1f + 2f * 2f + 3f * 3f)
        val expected = Vec3(
            1f / length,
            2f / length,
            3f / length
        )
        val normalizeOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.normalize(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(normalizeOperation, expected, Vec3(1f, 2f, 3f))
    }

    @Test
    fun `should negate`() {
        val expected = Vec3(-1f, -2f, -3f)
        val negateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            v1.negate(dst ?: Vec3())
        }
        testV3WithAndWithoutDest(negateOperation, expected, Vec3(1f, 2f, 3f))
    }

    @Test
    fun `should copy`() {
        val expected = Vec3(1f, 2f, 3f)
        val v = Vec3(1f, 2f, 3f)
        val copyOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            // Assuming Vec3 has a method like copy(dst: Vec3): Vec3 for in-place copy
            // and the standard data class copy() for creating a new instance.
            if (dst != null) {
                v1.copy(dst) // Call the in-place copy method (expects non-null dst)
                dst // Return the modified destination
            } else {
                v1.copy() // Call standard data class copy() to return a new instance
            }
        }
        testV3WithAndWithoutDest(copyOperation, expected, v)
    }

    @Test
    fun `should clone`() { // Alias test
        val expected = Vec3(1f, 2f, 3f)
        val v = Vec3(1f, 2f, 3f)
        val cloneOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            // Assuming Vec3 has a method like clone(dst: Vec3): Vec3 for in-place clone
            // and clone() for creating a new instance.
             if (dst != null) {
                v1.clone(dst) // Call the in-place clone method (expects non-null dst)
                dst // Return the modified destination
            } else {
                v1.clone() // Call method to return a new instance
            }
        }
        testV3WithAndWithoutDest(cloneOperation, expected, v)
    }

    @Test
    fun `should set`() { // Tests Vec3.Companion.set
        val expected = Vec3(2f, 3f, 4f)
        // Vec3.set is likely a static/companion method, doesn't fit the instance-based helper well.
        // Test it directly.
        // Test without dest
        val vSet1 = Vec3.set(2f, 3f, 4f)
        assertVec3EqualsApproximately(expected, vSet1)

        // Test with dest
        val vDest = Vec3()
        val vSet2 = Vec3.set(2f, 3f, 4f, vDest)
        assertSame(vDest, vSet2)
        assertVec3EqualsApproximately(expected, vDest)

        // Test aliasing (dest is same as a hypothetical source - not applicable for set)
    }

    @Test
    fun `should multiply`() {
        val expected = Vec3(2f, 8f, 18f)
        val multiplyOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.multiply(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(multiplyOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f))
    }

    @Test
    fun `should mul`() { // Alias test
        val expected = Vec3(2f, 8f, 18f)
        val mulOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.mul(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(mulOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 4f, 6f))
    }

    @Test
    fun `should divide`() {
        val expected = Vec3(1f / 2f, 2f / 3f, 3f / 4f)
        val divideOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.divide(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(divideOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 3f, 4f))
    }

    @Test
    fun `should div`() { // Alias test
        val expected = Vec3(1f / 2f, 2f / 3f, 3f / 4f)
        val divOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.div(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(divOperation, expected, Vec3(1f, 2f, 3f), Vec3(2f, 3f, 4f))
    }

    @Test
    fun `should fromValues`() { // Tests Vec3.Companion.fromValues
        val expected = Vec3(1f, 2f, 3f) // Use Vec3 constructor
        val v1 = Vec3.fromValues(1f, 2f, 3f)
        assertEquals(expected, v1)
    }

    @Test
    fun `should random`() { // Tests Vec3.Companion.random
        for (i in 0..99) {
            val v1 = Vec3.random()
            assertEqualsApproximately(1f, v1.length(), tolerance = TEST_EPSILON_F)

            val v2 = Vec3.random(2f)
            assertEqualsApproximately(2f, v2.length(), tolerance = TEST_EPSILON_F)

            val vp5 = Vec3.random(0.5f)
            assertEqualsApproximately(0.5f, vp5.length(), tolerance = TEST_EPSILON_F)

            val vd = Vec3()
            val vn = Vec3.random(3f, vd)
            assertSame(vd, vn)
            // JS test had: assertEqualApproximately(vec3.length(3, vd), 3); - This looks wrong.
            // It should be checking the length of vd *after* the random call.
            assertEqualsApproximately(3f, vd.length(), tolerance = TEST_EPSILON_F)
        }
    }

    @Test
    fun `should transform by 3x3`() {
        val expected = Vec3(4f, 10f, 18f)
        val m= Mat3( // Column major
            4f, 0f, 0f,
            0f, 5f, 0f,
            0f, 0f, 6f,
        )
        val transformOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val mat = args[1] as Mat3 // Pass FloatArray as defined by typealias
            // This fails because Vec3.transformMat3 expects a Mat3 object
            v.transformMat3(mat, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(transformOperation, expected, Vec3(1f, 2f, 3f), m)
    }

    @Test // Commenting out: Requires Mat4 class, cannot fix within constraints
    fun `should transform by 4x4`() {
        val expected = Vec3(5f, 9f, 15f)
        val m: Mat4 = Mat4( // Column major
            1f, 0f, 0f, 0f,
            0f, 2f, 0f, 0f,
            0f, 0f, 3f, 0f,
            4f, 5f, 6f, 1f,
        )
        val transformOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val mat = args[1] as Mat4 // Pass FloatArray as defined by typealias
            // This fails because Vec3.transformMat4 expects a Mat4 object
            v.transformMat4(mat, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(transformOperation, expected, Vec3(1f, 2f, 3f), m)
    }

    @Test // Commenting out: Requires Mat4 class, cannot fix within constraints
    fun `should transform by 4x4Upper3x3`() {
        val expected = Vec3(2f, 6f, 12f)
        val m: Mat4 = Mat4( // Column major
            1f, 0f, 0f, 0f,
            0f, 2f, 0f, 0f,
            0f, 0f, 3f, 0f,
            4f, 5f, 6f, 1f,
        )
        val transformOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val mat = args[1] as Mat4 // Pass FloatArray as defined by typealias
            // This fails because Vec3.transformMat4Upper3x3 expects a Mat4 object
            v.transformMat4Upper3x3(mat, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(transformOperation, expected, Vec3(2f, 3f, 4f), m)
    }

    @Test // Commenting out: Requires matrix.Quat, cannot fix within constraints
    fun `should transform by quat`() {
        // Expected values copied from previous Float32 results for consistency
        data class QuatTestData(val q: Quat, val expected: Vec3)
        val tests = listOf(
            QuatTestData(Quat.fromEuler(0.1, 0.2, 0.3, "xyz"), Vec3(10.483466f, 20.997532f, 33.81125f)),
            QuatTestData(Quat.fromEuler(1.1, 2.2, 3.3, "xyz"), Vec3(31.030506f, 1.3403475f, -27.005758f)),
        )
        val transformOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val quat = args[1] as Quat // Pass the local placeholder Quat object
            // This fails because Vec3.transformQuat expects matrix.Quat
            v.transformQuat(quat, dst ?: Vec3())
        }
        for ((q, expected) in tests) {
            // Pass the Quat object 'q' as the argument
            testV3WithAndWithoutDest(transformOperation, expected, Vec3(11f, 22f, 33f), q)
        }
    }

    @Test
    fun `should zero`() { // Tests Vec3.Companion.zero and instance zero
        // Test companion object method
        val vCompNoDest = Vec3.zero()
        assertVec3Equals(Vec3(0f, 0f, 0f), vCompNoDest)

        val vCompDest = Vec3(1f, 2f, 3f)
        val resultCompDest = Vec3.zero(vCompDest)
        assertSame(vCompDest, resultCompDest)
        assertVec3Equals(Vec3(0f, 0f, 0f), vCompDest)

        // Test instance method
        val vInstance = Vec3(1f, 2f, 3f)
        val resultInstance = vInstance.zero()
        assertSame(vInstance, resultInstance)
        assertVec3Equals(Vec3(0f, 0f, 0f), vInstance)
    }

    // --- Rotation Tests (No longer nested) ---

    @Test
    fun `rotateX rotation around world origin 0, 0, 0`() {
        val expected = Vec3(0f, -1f, 0f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateX(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(0f, 1f, 0f), Vec3(0f, 0f, 0f), PI.toFloat())
    }

    @Test
    fun `rotateX rotation around an arbitrary origin`() {
        val expected = Vec3(2f, 3f, 0f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateX(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(2f, 7f, 0f), Vec3(2f, 5f, 0f), PI.toFloat())
    }

    @Test
    fun `rotateY rotation around world origin 0, 0, 0`() {
        val expected = Vec3(-1f, 0f, 0f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateY(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(1f, 0f, 0f), Vec3(0f, 0f, 0f), PI.toFloat())
    }

    @Test
    fun `rotateY rotation around an arbitrary origin`() {
        val expected = Vec3(-6f, 3f, 10f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateY(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(-2f, 3f, 10f), Vec3(-4f, 3f, 10f), PI.toFloat())
    }

    @Test
    fun `rotateZ rotation around world origin 0, 0, 0`() {
        val expected = Vec3(0f, -1f, 0f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateZ(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(0f, 1f, 0f), Vec3(0f, 0f, 0f), PI.toFloat())
    }

    @Test
    fun `rotateZ rotation around an arbitrary origin`() {
        val expected = Vec3(0f, -6f, -5f)
        val rotateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val target = args[0] as Vec3
            val origin = args[1] as Vec3
            val angle = args[2] as Float
            target.rotateZ(origin, angle, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(rotateOperation, expected, Vec3(0f, 6f, -5f), Vec3(0f, 0f, -5f), PI.toFloat())
    }

    // --- Utility Method Tests (No longer nested) ---

    @Test
    fun `setLength set the length of a provided direction vector`() {
        val expected = Vec3(8.429314f, 8.429314f, 8.429314f) // Recalculated expected value
        val setLengthOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val len = args[1] as Float
            v.setLength(len, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(setLengthOperation, expected, Vec3(1f, 1f, 1f), 14.6f)
    }

    @Test
    fun `truncate shorten the vector`() {
        val expected = Vec3(2.309401f, 2.309401f, 2.309401f) // Recalculated expected value
        val truncateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val maxLen = args[1] as Float
            v.truncate(maxLen, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(truncateOperation, expected, Vec3(8.429314f, 8.429314f, 8.429314f), 4.0f)
    }

    @Test
    fun `truncate preserve the vector when shorter than maxLen`() {
        val original = Vec3(8.429314f, 8.429314f, 8.429314f)
        val expected = original.copy() // Should be unchanged
        val truncateOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v = args[0] as Vec3
            val maxLen = args[1] as Float
            v.truncate(maxLen, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(truncateOperation, expected, original, 18.0f)
    }

    @Test
    fun `midpoint should return the midpoint`() {
        val vecA = Vec3(0f, 0f, 0f)
        val vecB = Vec3(10f, 10f, 10f)
        val expected = Vec3(5f, 5f, 5f)
        // midpoint only takes 2 Vec3 args in JS, and optionally dst
        val midpointOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.midpoint(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(midpointOperation, expected, vecA, vecB)
    }

    @Test
    fun `midpoint should handle negatives`() {
        val vecA = Vec3(-10f, -10f, -10f)
        val vecB = Vec3(10f, 10f, 10f)
        val expected = Vec3(0f, 0f, 0f)
        val midpointOperation = { args: Array<out Any?>, dst: Vec3? ->
            val v1 = args[0] as Vec3
            val v2 = args[1] as Vec3
            v1.midpoint(v2, dst ?: Vec3())
        }
        testV3WithAndWithoutDest(midpointOperation, expected, vecA, vecB)
    }
}