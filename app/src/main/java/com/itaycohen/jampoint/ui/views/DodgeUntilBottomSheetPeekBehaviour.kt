package com.itaycohen.jampoint.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itaycohen.jampoint.R

class DodgeUntilBottomSheetPeekBehaviour(
    context: Context,
    attrs: AttributeSet?
) : FloatingActionButton.Behavior(context, attrs) {

    private val peekHeight = context.resources.getDimension(R.dimen.find_jams_peek_height)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        val p = dependency.layoutParams
        if(p !is CoordinatorLayout.LayoutParams) {
            return false
        }
        return p.behavior is BottomSheetBehavior<*>
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        val p = child.layoutParams as CoordinatorLayout.LayoutParams
        if (dependency.y < parent.height - peekHeight) {
            child.y = parent.height - peekHeight - (child.height) - p.bottomMargin
        } else {
            child.y = dependency.y - (child.height) - p.bottomMargin
        }
        return false
    }
}