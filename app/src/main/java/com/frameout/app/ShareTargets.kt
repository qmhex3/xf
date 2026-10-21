package com.frameout.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent

internal fun Intent.asChooserWithoutSelf(context: Context, title: CharSequence? = null): Intent =
    Intent.createChooser(this, title).putExtra(
        Intent.EXTRA_EXCLUDE_COMPONENTS,
        arrayOf(ComponentName(context, MainActivity::class.java))
    )
