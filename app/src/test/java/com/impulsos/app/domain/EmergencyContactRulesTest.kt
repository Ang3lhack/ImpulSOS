package com.impulsos.app.domain

import com.impulsos.app.data.EmergencyContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmergencyContactRulesTest {
    @Test fun rejectsBlankName() {
        assertEquals("Escribe un nombre.", EmergencyContactRules.validationError(" ", "5551234", emptyList()))
    }

    @Test fun rejectsShortPhone() {
        assertEquals("Escribe un número telefónico válido.", EmergencyContactRules.validationError("Ana", "12", emptyList()))
    }

    @Test fun rejectsDuplicateDigitsRegardlessOfFormatting() {
        val existing = listOf(EmergencyContact("Contacto", "555-010-0100"))
        assertEquals("Ese número ya está guardado.", EmergencyContactRules.validationError("Otra persona", "(555) 010 0100", existing))
    }

    @Test fun allowsEditingTheSameContactAndValidNewContact() {
        val existing = listOf(EmergencyContact("Contacto", "+52 555-010-0100"))
        assertNull(EmergencyContactRules.validationError("Contacto", "+52 555-010-0100", existing, ignoredIndex = 0))
        assertNull(EmergencyContactRules.validationError("Otra persona", "5551234567", existing))
    }
}
