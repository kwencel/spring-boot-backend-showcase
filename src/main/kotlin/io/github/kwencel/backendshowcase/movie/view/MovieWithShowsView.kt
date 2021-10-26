package io.github.kwencel.backendshowcase.movie.view

import io.github.kwencel.backendshowcase.show.view.ShowView

interface MovieWithShowsView : MovieView {
    val shows: List<ShowView>
}