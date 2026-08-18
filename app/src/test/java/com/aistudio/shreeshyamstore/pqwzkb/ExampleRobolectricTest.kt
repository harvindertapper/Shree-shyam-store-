package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // standard stable SDK
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Shree Shyam General Store", appName)
  }

  @Test
  fun `launch main activity`() {
    try {
      ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        scenario.onActivity { activity ->
          println("MainActivity launched successfully: $activity")
        }
      }
    } catch (e: Throwable) {
      e.printStackTrace()
      throw e
    }
  }
}
