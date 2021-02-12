package com.itaycohen.jampoint.ui.jam_team.vh

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.JamMeet
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.data.models.local.MembershipState
import com.itaycohen.jampoint.data.models.local.TeamItemFutureMeetings
import com.itaycohen.jampoint.databinding.JamTeamFutureMeetingsBinding
import com.itaycohen.jampoint.databinding.MeetingItemBinding
import com.itaycohen.jampoint.databinding.UserMeetingRowItemBinding
import com.itaycohen.jampoint.ui.jam_team.JamTeamViewModel
import com.itaycohen.jampoint.utils.DateTimeUtils
import com.itaycohen.jampoint.utils.LocationUtils
import com.itaycohen.jampoint.utils.UiUtils
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx
import com.itaycohen.jampoint.utils.toLocation
import java.util.*

class TeamFutureMeetingsViewHolder(
    v: View,
    private val jamTeamViewModel: JamTeamViewModel,
    private val childFragmentManager: FragmentManager,
    private val navController: NavController
) : JamTeamBaseHolder(v) {

    private val binding = JamTeamFutureMeetingsBinding.bind(v)
    private val inflater = LayoutInflater.from(v.context)

    fun bindViewHolder(item: TeamItemFutureMeetings, isInEditMode: Boolean) = with(binding) {
        UiUtils.syncViewGroupChildToAmount(futureJamsContainer, item.futureMeetings.size) { toIndex ->
            val view = inflater.inflate(R.layout.meeting_item, futureJamsContainer, false)
            val meetBinding = MeetingItemBinding.bind(view)
            meetBinding.joinBtn.setOnClickListener {
                jamTeamViewModel.onParticipateRequestClick(it.findNavController(), toIndex)
            }
            if (toIndex != item.futureMeetings.size-1) {
                val newParams = LinearLayout.LayoutParams(view.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(16).toInt()
                view.layoutParams = newParams
            }
            futureJamsContainer.addView(view)
        }
        bindInnerItems(item, isInEditMode)
    }

    private fun bindInnerItems(item: TeamItemFutureMeetings, isInEditMode: Boolean) = with(binding) {
        futureJamsContainer.forEachIndexed { index, view ->
            val futureMeet = item.futureMeetings[index]
            val meetBinding = MeetingItemBinding.bind(view)
            syncPendingContainer(meetBinding.approvedContainer, futureMeet.approvedMembers)
            meetBinding.approvedGroup.isVisible = !futureMeet.approvedMembers.isNullOrEmpty()

            syncPendingContainer(meetBinding.pendingsContaienr, futureMeet.pendingMembers)
            meetBinding.pendingGroup.isVisible = !futureMeet.pendingMembers.isNullOrEmpty() && isInEditMode


            with(meetBinding) {
                val addressText = futureMeet.toLocation()?.let { LocationUtils.getCompleteAddressString(
                    root.context,
                    it
                ) }
                addressTextInputEditText.setText(addressText, TextView.BufferType.NORMAL)
                addressTextInputEditText.isEnabled = false
                placesFragmentContainer.isVisible = isInEditMode
                if (isInEditMode)
                    addPlacesFragmentIfNeeded(itemView.context, futureMeet)
                addressTextInputEditText.setOnClickListener { }
                timeTextInputEditText.setText(futureMeet.getUiTime(), TextView.BufferType.NORMAL)
                timeTextInputEditText.isEnabled = isInEditMode
                timeTextInputEditText.setOnClickListener { v ->
                    showDateTimePicker(v.context) { utcTimeStamp ->
                        jamTeamViewModel.updateMeetingTime(futureMeet, utcTimeStamp)
                    }
                }
                joinBtn.isEnabled = item.membershipState == MembershipState.No
                joinBtn.text = root.context.getString(
                    if (!item.futureMeetingsSelfPendingList[index])
                        R.string.ask_to_join2
                    else
                        R.string.cancel_request2
                )

                bindAlreadyApproved(futureMeet, isInEditMode)
                bindPendingApproval(futureMeet, isInEditMode)
            }
        }
    }

    private fun MeetingItemBinding.bindAlreadyApproved(
        futureMeet: JamMeet,
        isInEditMode: Boolean
    ) {
        val approvedMembersList = futureMeet.approvedMembers?.values?.toList()
        approvedContainer.forEachIndexed { index, view ->
            with(UserMeetingRowItemBinding.bind(view)) {
                val user = approvedMembersList!![index]
                personNameTextView.text = user.fullName
                personInstrumentTextView.text = user.mainInstrument
                personInstrumentTextView.isVisible = !user.mainInstrument.isNullOrBlank()
                Glide.with(view)
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_baseline_account_circle_24)
                    .error(R.drawable.ic_baseline_account_circle_24)
                    .into(smallProfileImageView)
                confirmBtn.isVisible = false

                declineBtn.setOnClickListener {
                    jamTeamViewModel.removeUserFromMeeting(futureMeet, user)
                }
                declineBtn.isVisible = isInEditMode
            }
        }
    }

    private fun MeetingItemBinding.bindPendingApproval(
        futureMeet: JamMeet,
        isInEditMode: Boolean
    ) {
        val pendingMembersList = futureMeet.pendingMembers?.values?.toList()
        pendingsContaienr.forEachIndexed { index, view ->
            with(UserMeetingRowItemBinding.bind(view)) {
                val user = pendingMembersList!![index]
                personNameTextView.text = user.fullName
                personInstrumentTextView.text = user.mainInstrument
                personInstrumentTextView.isVisible = !user.mainInstrument.isNullOrBlank()
                Glide.with(view)
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_baseline_account_circle_24)
                    .error(R.drawable.ic_baseline_account_circle_24)
                    .into(smallProfileImageView)
                confirmBtn.setOnClickListener {
                    jamTeamViewModel.updateJoinMeetingConfirmation(futureMeet, user, true)
                }
                declineBtn.setOnClickListener {
                    jamTeamViewModel.updateJoinMeetingConfirmation(futureMeet, user, false)
                }
                confirmBtn.isVisible = isInEditMode
                declineBtn.isVisible = isInEditMode
            }
        }
    }

    private fun syncPendingContainer(
        container: ViewGroup,
        usersMap: Map<String, User>?
    ) {
        UiUtils.syncViewGroupChildToAmount(container, usersMap?.size ?: 0) { toIndex ->
            val approveUserView = inflater.inflate(R.layout.user_meeting_row_item, container, false)
            if (toIndex != usersMap!!.size - 1) {
                val newParams = LinearLayout.LayoutParams(approveUserView.layoutParams)
                newParams.bottomMargin = container.resources.convertDpToPx(4).toInt()
                approveUserView.layoutParams = newParams
            }
            container.addView(approveUserView)
        }
    }

    private fun showDateTimePicker(ctx: Context, timeStampCallback: (String) -> Unit) {
        val typedValue = TypedValue()
        val dialogTheme = if (ctx.theme.resolveAttribute(
                R.attr.materialCalendarTheme,
                typedValue,
                true
            )) {
            typedValue.data
        } else {
           return
        }
        val calendar = DateTimeUtils.getClearedUtc()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        calendar.timeInMillis = today
        calendar.roll(Calendar.MONTH, 2)
        val nextTwoMonths = calendar.timeInMillis
        val constraints = CalendarConstraints.Builder()
            .setStart(today)
            .setEnd(nextTwoMonths)
            .setOpenAt(today)
            .setValidator(DateValidatorPointForward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(dialogTheme)
            .setSelection(today)
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setTitleText(R.string.define_meet_time)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { timeInMillis ->
            showTimePicker(timeInMillis, timeStampCallback)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    private fun showTimePicker(timeForDateInMillis: Long, timeStampCallback: (String) -> Unit) {
        val materialTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .build()
        materialTimePicker.addOnPositiveButtonClickListener {
            val utcTimeStamp = with(DateTimeUtils.getClearedUtc()) {
                isLenient = false
                timeInMillis = timeForDateInMillis
                roll(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                roll(Calendar.MINUTE, materialTimePicker.minute)
                toInstant().toString()
            }
            timeStampCallback(utcTimeStamp)
        }
        materialTimePicker.show(childFragmentManager, materialTimePicker.toString())
    }

    @SuppressLint("InflateParams")
    private fun addPlacesFragmentIfNeeded(ctx: Context, futureMeet: JamMeet) {
        var frag = childFragmentManager.findFragmentById(R.id.placesFragmentContainer) as? AutocompleteSupportFragment
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
                            jamTeamViewModel.updateMeetingPlace(futureMeet, it)
                        }
                    }

                    override fun onError(status: Status) {
                        if (!status.isCanceled)
                            Toast.makeText(ctx, R.string.problem_with_place, Toast.LENGTH_SHORT).show()
                    }
                })
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.placesFragmentContainer, frag)
                .commit()
        }
    }

    companion object {
        private val TAG = TeamFutureMeetingsViewHolder::class.simpleName
        private const val PLACE_REQ_KEY = "bethE PUSHTHEBTN"
    }
}