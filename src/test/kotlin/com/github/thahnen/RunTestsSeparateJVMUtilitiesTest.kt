package com.github.thahnen

import org.junit.Assert
import org.junit.Test


/**
 *  RunTestsSeparateJVMUtilitiesTest:
 *  ================================
 *
 *  jUnit test cases on the RunTestsSeparateJVMUtilities for JaCoCo coverage
 */
open class RunTestsSeparateJVMUtilitiesTest {

    /** 1) Tests the multipleLet method by providing no null value */
    @Test fun testMultipleLetNotNull() {
        var setByMultipleLet = false

        multipleLet("not", "null") { (v1, v2) ->
            Assert.assertEquals("not", v1)
            Assert.assertEquals("null", v2)

            setByMultipleLet = true
        }

        Assert.assertTrue(setByMultipleLet)
    }


    /** 2) Tests the multipleLet method by providing a null value */
    @Test fun testMultipleLetNull() {
        var notSetByMultipleLet = true

        multipleLet("not", null) {
            notSetByMultipleLet = false
        }

        Assert.assertTrue(notSetByMultipleLet)
    }
}
