package com.fdm.plancito

import com.fdm.plancito.data.Invite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteTest {
    @Test
    fun codeFromAppLink() {
        assertEquals("ABC123", Invite.codeFrom("plancito://join/ABC123"))
        assertEquals("ABC123", Invite.codeFrom("plancito://join/abc123/"))
    }

    @Test
    fun codeFromSharedLink() {
        assertEquals("4F9A2C", Invite.codeFrom("https://xyz.supabase.co/functions/v1/invite?c=4f9a2c"))
        assertEquals("4F9A2C", Invite.codeFrom("https://xyz.supabase.co/functions/v1/invite?x=1&c=4F9A2C"))
    }

    @Test
    fun rejectsOtherLinks() {
        assertNull(Invite.codeFrom(null))
        assertNull(Invite.codeFrom(""))
        assertNull(Invite.codeFrom("https://example.com/?c=ABC123"))
        assertNull(Invite.codeFrom("plancito://join/"))
        assertNull(Invite.codeFrom("plancito://join/AB-12!"))
    }

    @Test
    fun normalizesTypedCode() {
        assertEquals("ABC123", Invite.normalize("  abc123 "))
        assertNull(Invite.normalize("ab"))
    }

    @Test
    fun linkPointsToEdgeFunction() {
        assertEquals("https://xyz.supabase.co/functions/v1/invite?c=ABC123", Invite.link("https://xyz.supabase.co/", "ABC123"))
    }
}
