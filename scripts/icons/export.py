import os
import sys, os, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from assemble import ICONS, SRC, GROUP_ICONS
P=os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),"..","..")) + "/"
D=P+"app/src/main/res/drawable/"
HEAD='<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp"\n    android:height="24dp"\n    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
for k,paths in ICONS.items():
    d=" ".join(paths)
    xml=HEAD+'    <path\n        android:pathData="'+d+'"\n        android:strokeColor="#FFFFFFFF"\n        android:strokeWidth="2"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n</vector>\n'
    open(D+f"ic_ctag_{k}.xml","w").write(xml)

for k,paths in GROUP_ICONS.items():
    d=" ".join(paths)
    open(D+f"ic_cgroup_{k}.xml","w").write(HEAD+'    <path\n        android:pathData="'+d+'"\n        android:strokeColor="#FFFFFFFF"\n        android:strokeWidth="2"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n</vector>\n')
from svgconv import load, normalize
open(D+"ic_cgroup_chevron.xml","w").write(HEAD+'    <path\n        android:pathData="'+" ".join(normalize(x) for x in load("t","chevron-down"))+'"\n        android:strokeColor="#FFFFFFFF"\n        android:strokeWidth="2"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n</vector>\n')
grows=",\n".join(f'    "{k}" to R.drawable.ic_cgroup_{k}' for k in GROUP_ICONS)
rows=",\n".join(f'    "{k}" to R.drawable.ic_ctag_{k}' for k in ICONS)
kt=f'''package com.frameout.app.settings

import androidx.annotation.DrawableRes
import com.frameout.app.R

private val TagIcons: Map<String, Int> = mapOf(
{rows}
)

private val GroupIcons: Map<String, Int> = mapOf(
{grows}
)

@DrawableRes
internal fun contentTagIcon(id: String): Int = TagIcons[id] ?: R.drawable.ic_ctag_building

@DrawableRes
internal fun contentGroupIcon(id: String): Int = GroupIcons[id] ?: R.drawable.ic_ctag_building
'''
open(P+"app/src/main/java/com/frameout/app/settings/ContentTagIcons.kt","w").write(kt)
print(len(ICONS))
