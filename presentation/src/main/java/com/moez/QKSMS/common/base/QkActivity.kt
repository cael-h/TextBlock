/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.common.base

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.util.Preferences
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

abstract class QkActivity : AppCompatActivity() {
    @Inject lateinit var prefs: Preferences

    protected val menu: Subject<Menu> = BehaviorSubject.create()

    protected val toolbar: Toolbar? get() = findViewById(R.id.toolbar)
    protected val toolbarTitle: TextView? get() = findViewById(R.id.toolbarTitle)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
        disableScreenshots(prefs.disableScreenshots.get())
    }

    override fun onResume() {
        super.onResume()
        disableScreenshots(prefs.disableScreenshots.get())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(toolbar)
        applySystemBarInsetsForEdgeToEdge()
        title = title // The title may have been set before layout inflation
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        setSupportActionBar(toolbar)
        applySystemBarInsetsForEdgeToEdge()
        title = title // The title may have been set before layout inflation
    }

    private fun applySystemBarInsetsForEdgeToEdge() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        val screenToolbar = toolbar
        val toolbarLayoutParams = screenToolbar?.layoutParams as? ViewGroup.MarginLayoutParams
        val initialToolbarTopMargin = toolbarLayoutParams?.topMargin ?: 0
        val initialLeftPadding = root.paddingLeft
        val initialTopPadding = root.paddingTop
        val initialRightPadding = root.paddingRight
        val initialBottomPadding = root.paddingBottom

        fun resolveTopInset(insets: WindowInsetsCompat?): Int {
            val reportedInset = insets?.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )?.top ?: 0
            if (reportedInset > 0) return reportedInset

            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return resourceId.takeIf { it != 0 }
                ?.let(resources::getDimensionPixelSize)
                ?: 0
        }

        fun applyInsets(insets: WindowInsetsCompat?) {
            val topInset = resolveTopInset(insets)
            val bottomInset = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            if (toolbarLayoutParams != null) {
                toolbarLayoutParams.topMargin = initialToolbarTopMargin + topInset
                screenToolbar.layoutParams = toolbarLayoutParams
                root.setPadding(
                    initialLeftPadding,
                    initialTopPadding,
                    initialRightPadding,
                    initialBottomPadding + bottomInset
                )
            } else {
                root.setPadding(
                    initialLeftPadding,
                    initialTopPadding + topInset,
                    initialRightPadding,
                    initialBottomPadding + bottomInset
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            applyInsets(insets)
            insets
        }
        root.post {
            applyInsets(ViewCompat.getRootWindowInsets(root))
            ViewCompat.requestApplyInsets(root)
        }
    }

    override fun setTitle(titleId: Int) {
        title = getString(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        toolbarTitle?.text = title
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (menu != null) {
            this.menu.onNext(menu)
        }
        return result
    }

    protected open fun showBackButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    private fun disableScreenshots(disableScreenshots: Boolean) {
        if (disableScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

}
