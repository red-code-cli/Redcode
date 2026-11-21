package com.redcode.app.data

import com.redcode.app.data.model.MessageRole
import com.redcode.app.data.model.ProjectFolder
import com.redcode.app.data.model.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCodeRepositoryTest {

    @Test
    fun testProjectFolderDataIntegrity() {
        val folder = ProjectFolder(
            id = "proj_123",
            name = "workspace",
            path = "/storage/emulated/0/workspace",
            isSelected = true
        )
        assertEquals("proj_123", folder.id)
        assertEquals("workspace", folder.name)
        assertTrue(folder.isSelected)
    }

    @Test
    fun testSessionSummaryDefaultValues() {
        val session = SessionSummary(
            id = "ses_abc",
            title = "Test Session",
            project = "demo",
            snippet = "Hello world",
            timeAgo = "Just now",
            isUnread = false,
            isPinned = false,
            isArchived = false
        )
        assertEquals("ses_abc", session.id)
        assertFalse(session.isUnread)
        assertFalse(session.isPinned)
        assertFalse(session.isArchived)
    }

    @Test
    fun testMessageRoleValues() {
        assertEquals(MessageRole.USER, MessageRole.valueOf("USER"))
        assertEquals(MessageRole.ASSISTANT, MessageRole.valueOf("ASSISTANT"))
        assertEquals(MessageRole.SYSTEM, MessageRole.valueOf("SYSTEM"))
    }
}
