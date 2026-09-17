package com.example

import com.example.data.local.PreferencesManager
import com.example.data.model.Configuracoes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSupabaseDefaultCredentials() {
    val config = Configuracoes()
    assertEquals("https://muxvtacywjfgclfvtgia.supabase.co", config.supabaseUrl)
    assertTrue(config.supabaseAnonKey.startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
    assertEquals("https://muxvtacywjfgclfvtgia.supabase.co", PreferencesManager.DEFAULT_SUPABASE_URL)
  }
}
