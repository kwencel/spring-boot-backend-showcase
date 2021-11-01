package io.github.kwencel.backendshowcase.rating.dto

import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class RatingUpdateRequest(
    @field:Min(1)
    @field:Max(5)
    val rating: Short
)
