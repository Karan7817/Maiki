package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCleanModelName_stripsPrefix() {
    assertEquals("gemini-2.0-flash", com.example.data.api.GeminiApiClient.cleanModelName("models/gemini-2.0-flash"))
    assertEquals("gemini-1.5-flash", com.example.data.api.GeminiApiClient.cleanModelName("models/gemini-1.5-flash"))
    assertEquals("gemini-2.0-flash", com.example.data.api.GeminiApiClient.cleanModelName("/gemini-2.0-flash"))
    assertEquals("gemini-2.0-flash", com.example.data.api.GeminiApiClient.cleanModelName("  gemini-2.0-flash  "))
  }
}
