package com.frameout.core.util

import com.frameout.core.R

object PickerStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun limitedAccess() = s(R.string.picker_limited_access)
    fun removeItem(name: String) = s(R.string.picker_remove_item, name)
    fun itemRemoved(name: String) = s(R.string.picker_item_removed, name)
    fun selectedState(number: Int) = s(R.string.picker_selected_state, number)
    fun continueDescription(n: Int) = s(R.string.picker_continue_description, n)
    fun reorderHint() = s(R.string.picker_reorder_hint)
}
