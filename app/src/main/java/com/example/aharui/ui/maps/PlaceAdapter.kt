package com.example.aharui.ui.maps

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aharui.R
import com.example.aharui.data.Place

class PlaceAdapter(
    private var places: List<Place>,
    private val onDirectionsClick: (Place) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.bind(place, onDirectionsClick)
    }

    override fun getItemCount(): Int = places.size

    fun updatePlaces(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeNameTextView: TextView = itemView.findViewById(R.id.place_name_textview)
        private val placeDistanceTextView: TextView = itemView.findViewById(R.id.place_distance_textview)
        private val placeAddressTextView: TextView = itemView.findViewById(R.id.place_address_textview)
        private val placeRatingTextView: TextView = itemView.findViewById(R.id.place_rating_textview)
        private val directionsButton: Button = itemView.findViewById(R.id.directions_button)

        fun bind(place: Place, onDirectionsClick: (Place) -> Unit) {
            placeNameTextView.text = place.name
            placeDistanceTextView.text = place.distance.ifEmpty { "Distance: N/A" }
            placeAddressTextView.text = place.address

            // Hide rating if not available
            if (place.rating.isNotEmpty()) {
                placeRatingTextView.text = "⭐ ${place.rating}"
                placeRatingTextView.visibility = View.VISIBLE
            } else {
                placeRatingTextView.visibility = View.GONE
            }

            directionsButton.setOnClickListener {
                onDirectionsClick(place)

                // Open directions in a map app
                val gmmIntentUri = Uri.parse(
                    "geo:${place.geoPoint.latitude},${place.geoPoint.longitude}?q=${place.geoPoint.latitude},${place.geoPoint.longitude}(${place.name})"
                )
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                // Try to open in any available map app
                if (mapIntent.resolveActivity(itemView.context.packageManager) != null) {
                    itemView.context.startActivity(mapIntent)
                }
            }
        }
    }
}