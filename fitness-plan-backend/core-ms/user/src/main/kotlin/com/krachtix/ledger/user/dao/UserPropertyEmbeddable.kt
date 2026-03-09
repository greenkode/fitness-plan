package com.krachtix.user.dao

import com.krachtix.user.dto.UserProperty
import com.krachtix.user.dto.UserPropertyName
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class UserPropertyEmbeddable(
    @Enumerated(EnumType.STRING)
    @Column(name = "property_name")
    val name: UserPropertyName,
    @Column(name = "property_value")
    var value: String
) {

    fun toDomain(): UserProperty {
        return UserProperty(name, value)
    }
}