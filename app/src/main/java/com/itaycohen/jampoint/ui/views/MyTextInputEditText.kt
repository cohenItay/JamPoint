package com.itaycohen.jampoint.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.R
import com.google.android.material.textfield.TextInputEditText

class MyTextInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = R.attr.editTextStyle
) : TextInputEditText(
    context,
    attrs,
    defStyleRes
) {

    var listener: Listener? = null

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            listener?.onImeBack(this)
        }
        return super.dispatchKeyEvent(event)
    }

    fun interface Listener {
        fun onImeBack(editText: MyTextInputEditText)
    }
}