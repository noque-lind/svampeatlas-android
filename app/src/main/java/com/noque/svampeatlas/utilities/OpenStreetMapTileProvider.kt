package com.noque.svampeatlas.utilities

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL

class OpenStreetMapTileProvider() : UrlTileProvider(256, 256) {

    private val url = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
        return URL(url.replace("{z}", zoom.toString()).replace("{x}", x.toString()).replace("{y}", y.toString()))
    }
}