package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.apps.*
import com.ansa1r.projectadhd.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AppSelectionDraftTest {
    private val old = TrackedApp("old.app", "Old", 30)
    private val added = InstalledApp("new.app", "New")
    @Test fun togglesAreOnlyDraftAndDiscardRestoresPersistence() {
        val persisted = listOf(old)
        val draft = AppSelectionDraft.from(persisted).select(added, true).select(InstalledApp(old.packageName, old.displayName), false)
        assertEquals(listOf(old), persisted)
        assertEquals(listOf(old), draft.persisted)
        assertEquals(setOf("new.app"), draft.selected.keys)
        assertEquals(setOf("old.app"), AppSelectionDraft.from(persisted).selected.keys)
    }
    @Test fun existingLimitSurvivesAndNewAppUsesProjectDefault() {
        val draft = AppSelectionDraft.from(listOf(old)).select(added, true)
        assertEquals(30, draft.selected[old.packageName]?.minutes)
        assertEquals(TrackedApp(added.packageName, added.displayName).sessionLimitMinutes, draft.selected[added.packageName]?.minutes)
    }
    @Test fun removingAllProducesValidEmptyReplacement() {
        val draft = AppSelectionDraft.from(listOf(old)).select(InstalledApp(old.packageName, old.displayName), false)
        assertTrue(draft.selectionChanged)
        assertTrue(draft.valid)
        assertTrue(draft.savedApps().isEmpty())
    }
    @Test fun togglingBackToInitialSelectionClearsChangedFlag() {
        val draft = AppSelectionDraft.from(listOf(old)).select(added, true).select(added, false)
        assertFalse(draft.selectionChanged)
    }
    @Test fun sharedLimitStillAllowsIndividualOverride() {
        val draft = AppSelectionDraft.from(listOf(old)).select(added, true).applyToAll("20").limit("old.app", "45")
        assertEquals(mapOf("old.app" to 45, "new.app" to 20), draft.savedApps().associate { it.packageName to it.sessionLimitMinutes })
        assertEquals(30, draft.persisted.single().sessionLimitMinutes)
    }
    @Test fun invalidLimitsCannotBeSavedAndInvalidApplyAllDoesNothing() {
        val valid = AppSelectionDraft.from(listOf(old))
        for (input in listOf("", "0", "181", "-1", "abc")) {
            val draft = valid.limit(old.packageName, input)
            assertFalse(draft.valid)
            try { draft.savedApps(); fail("Invalid limit accepted") } catch (_: IllegalArgumentException) { }
            assertEquals(valid, valid.applyToAll(input))
        }
    }
    @Test fun oldDisabledRowsAreNotSelectedAndAreRemovedBySave() {
        val draft = AppSelectionDraft.from(listOf(old.copy(enabled = false)))
        assertTrue(draft.selected.isEmpty())
        assertTrue(draft.selectionChanged)
        assertTrue(draft.savedApps().isEmpty())
    }
    @Test fun reselectingExistingAppRetainsItsSavedLimit() {
        val draft = AppSelectionDraft.from(listOf(old)).select(InstalledApp("old.app", "Renamed"), false)
            .select(InstalledApp("old.app", "Renamed"), true)
        assertEquals(30, draft.savedApps().single().sessionLimitMinutes)
        assertEquals("Renamed", draft.savedApps().single().displayName)
    }
    @Test fun homeOnboardingExistsOnlyForZeroTrackedApps() {
        assertTrue(showAppSelectionCta(0))
        assertFalse(showAppSelectionCta(1))
        assertFalse(showAppSelectionCta(10))
    }
}
