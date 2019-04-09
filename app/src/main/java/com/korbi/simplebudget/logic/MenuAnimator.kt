/*
 * Copyright 2019 Korbinian Moser
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.korbi.simplebudget.logic

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.drawable.Drawable
import android.view.MenuItem

object MenuAnimator {

    fun setVisibility(item: MenuItem, visibility: Boolean, endAction: () -> Unit = { } ) {
        if (visibility) {
            if (!item.isVisible) showItem(item)
        } else {
            if (item.isVisible) hideItem(item, endAction)
        }
    }

    fun hideIcon(icon: Drawable, endAction: () -> Unit = { }) {
        ObjectAnimator.ofPropertyValuesHolder(icon,
                PropertyValuesHolder.ofInt("alpha", 255, 0)).apply {
            duration = 200
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    icon.setVisible(false, false)
                    endAction()
                }
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            start()
        }
    }

    fun showIcon(icon: Drawable) {
        ObjectAnimator.ofPropertyValuesHolder(icon,
                PropertyValuesHolder.ofInt("alpha", 0, 255)).apply {
            duration = 200
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    icon.setVisible(true, false)
                }
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            start()
        }
    }

    private fun hideItem(item: MenuItem, endAction: () -> Unit = { }) {
        ObjectAnimator.ofPropertyValuesHolder(item.icon,
                PropertyValuesHolder.ofInt("alpha", 255, 0)).apply {
            duration = 200
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    item.isVisible = false
                    endAction()
                }
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            start()
        }
    }

    private fun showItem(item: MenuItem) {
        item.isVisible = true
        ObjectAnimator.ofPropertyValuesHolder(item.icon,
                PropertyValuesHolder.ofInt("alpha", 0, 255)).apply {
            duration = 200
            start()
        }
    }
}