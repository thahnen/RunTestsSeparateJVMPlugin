package com.github.thahnen


/**
 *  Checks on multiple parameters if they are null instead of using something like this:
 *
 *  param1?.let { p1 ->
 *      param2?.let { p2 ->
 *          ...
 *      }
 *  }
 */
inline fun <T: Any> multipleLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null }) {
        closure(elements.filterNotNull())
    }
}
