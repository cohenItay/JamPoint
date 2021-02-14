package com.itaycohen.jampoint.ui.jam_team.vh

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.JamMeet
import com.itaycohen.jampoint.data.models.local.TeamItemCreateJamMeet
import com.itaycohen.jampoint.databinding.CreateMeetingLayoutBinding
import com.itaycohen.jampoint.ui.jam_team.JamTeamViewModel
import com.itaycohen.jampoint.utils.DateTimeUtils
import com.itaycohen.jampoint.utils.toLocation

class TeamCreateJamMeetViewHolder(
    view: View,
    private val childFragmentManager: FragmentManager,
    private val jamTeamViewModel: JamTeamViewModel
) : JamTeamBaseHolder(view) {

    val binding = CreateMeetingLayoutBinding.bind(view)
    private var jamMeet = JamMeet()

    init {
        binding.createMeetingFab.setOnClickListener {
            if (binding.inputsContainer.isVisible) {
                if (areInputsValid(jamMeet))
                    jamTeamViewModel.createNewJamMeet(jamMeet)
            } else {
                TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
                binding.inputsContainer.isVisible = true
            }
        }
        binding.pickDateTimeBtn.setOnClickListener {
            DateTimeUtils.PickDateTimeHelper.launch(it.context, childFragmentManager) { utcTimeStamp ->
                jamMeet = jamMeet.copy(
                    utcTimeStamp = utcTimeStamp
                )
                binding.chosenTimeTextView.text = DateTimeUtils.utcTimeToUiLocaleTime(utcTimeStamp)
            }
        }
    }

    fun bindViewHolder(item: TeamItemCreateJamMeet, isInEditMode: Boolean) {
        binding.root.isVisible = isInEditMode
        binding.createMeetingFab.text = item.buttonText
        addPlacesFragmentIfNeeded(itemView.context)
    }



    private fun areInputsValid(jamMeet: JamMeet) = with(jamMeet) {
        binding.chosenTimeTextView.error = if (utcTimeStamp.isNullOrBlank())
            itemView.context.getString(R.string.please_enter_a_date)
        else
            null
        if (longitude == null || latitude == null)
            Toast.makeText(itemView.context, itemView.context.getString(R.string.problem_with_place), Toast.LENGTH_LONG).show()
        longitude != null && latitude != null && !utcTimeStamp.isNullOrBlank()
    }

    @SuppressLint("InflateParams")
    private fun addPlacesFragmentIfNeeded(ctx: Context) {
        var frag = childFragmentManager.findFragmentById(R.id.createMeetPlacesFragmentContainer) as? AutocompleteSupportFragment
        if (frag == null) {
            frag = AutocompleteSupportFragment.newInstance()
            frag.apply {
                this as AutocompleteSupportFragment
                setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
                setCountries("IL")
                setText("")
                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(place: Place) {
                        place.toLocation()?.also {
                            jamMeet = jamMeet.copy(
                                longitude = it.longitude,
                                latitude = it.latitude
                            )
                        }
                    }

                    override fun onError(status: Status) {
                        if (!status.isCanceled)
                            Toast.makeText(ctx, R.string.problem_with_place_try_later, Toast.LENGTH_SHORT).show()
                    }
                })
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.createMeetPlacesFragmentContainer, frag)
                .commit()
        }
    }
}
