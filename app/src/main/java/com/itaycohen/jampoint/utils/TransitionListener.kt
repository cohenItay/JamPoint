package com.itaycohen.jampoint.utils

import androidx.transition.Transition

interface TransitionListener : Transition.TransitionListener{
    override fun onTransitionCancel(transition: Transition) {}
    override fun onTransitionEnd(transition: Transition) {}
    override fun onTransitionPause(transition: Transition) {}
    override fun onTransitionResume(transition: Transition) {}
    override fun onTransitionStart(transition: Transition) {}
}