package com.impulsos.app.domain

import com.impulsos.app.data.EmergencyContact

object EmergencyContactRules {
    fun normalizedPhone(value: String) = value.filter(Char::isDigit)

    fun validationError(name: String, phone: String, existing: List<EmergencyContact>, ignoredIndex: Int? = null): String? {
        if (name.trim().isBlank()) return "Escribe un nombre."
        val digits = normalizedPhone(phone)
        if (digits.length < 3) return "Escribe un número telefónico válido."
        val duplicate = existing.withIndex().any { (index, contact) ->
            index != ignoredIndex && normalizedPhone(contact.phone) == digits
        }
        if (duplicate) return "Ese número ya está guardado."
        return null
    }
}
