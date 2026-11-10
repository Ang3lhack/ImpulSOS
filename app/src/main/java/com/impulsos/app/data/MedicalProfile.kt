package com.impulsos.app.data

data class MedicalProfile(
    val name: String = "",
    val conditions: String = "",
    val allergies: String = "",
    val medications: String = "",
    val rescueMedication: String = "",
    val avoid: String = "",
    val instructions: String = "",
    val trustedContact: String = "",
    val additionalInfo: String = "",
    val emergencyPhotoSet: Boolean = false,
    val contacts: List<EmergencyContact> = emptyList(),
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val relation: String = "",
    val additionalInfo: String = "",
)
