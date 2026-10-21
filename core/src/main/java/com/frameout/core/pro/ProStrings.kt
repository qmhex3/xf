package com.frameout.core.pro

import com.frameout.core.R
import com.frameout.core.util.StringSource

object ProStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)

    fun title() = s(R.string.pro_title)
    fun subtitle() = s(R.string.pro_subtitle)
    fun badge() = s(R.string.pro_badge)
    fun active() = s(R.string.pro_active)
    fun activeBody() = s(R.string.pro_active_body)
    fun monthly() = s(R.string.pro_monthly)
    fun yearly() = s(R.string.pro_yearly)
    fun lifetime() = s(R.string.pro_lifetime)
    fun trialBadge() = s(R.string.pro_trial_badge)
    fun bestValue() = s(R.string.pro_best_value)
    fun oneTime() = s(R.string.pro_one_time)
    fun restore() = s(R.string.pro_restore)
    fun notNow() = s(R.string.pro_not_now)
    fun loading() = s(R.string.pro_loading)
    fun unavailable() = s(R.string.pro_unavailable)
    fun billingError() = s(R.string.pro_billing_error)
    fun pending() = s(R.string.pro_pending)
    fun restored() = s(R.string.pro_restored)
    fun nothingToRestore() = s(R.string.pro_nothing_to_restore)
    fun settingsTitle() = s(R.string.pro_settings_title)
    fun settingsFree() = s(R.string.pro_settings_free)
    fun manage() = s(R.string.pro_manage)
    fun noTrials() = s(R.string.pro_no_trials)
    fun trialsLeft(n: Int) = s(R.string.pro_trials_left, n)
    fun planMonthlyHint() = s(R.string.pro_plan_monthly_hint)
    fun planYearlyHint() = s(R.string.pro_plan_yearly_hint)
    fun perMonth() = s(R.string.pro_per_month)
    fun perYear() = s(R.string.pro_per_year)
    fun perOnce() = s(R.string.pro_per_once)
    fun tlTitle() = s(R.string.pro_tl_title)
    fun tlToday() = s(R.string.pro_tl_today)
    fun tlTodayBody() = s(R.string.pro_tl_today_body)
    fun tlReminder() = s(R.string.pro_tl_reminder)
    fun tlCharge() = s(R.string.pro_tl_charge)
    fun trying(name: String) = s(R.string.pro_trying, name)
    fun planTrialHint(days: Int) = s(R.string.pro_plan_trial_hint, days)
    fun bestSave(percent: Int) = s(R.string.pro_best_save, percent)
    fun ctaTrial(days: Int) = s(R.string.pro_cta_trial, days)
    fun ctaMonth(price: String) = s(R.string.pro_cta_month, price)
    fun ctaYear(price: String) = s(R.string.pro_cta_year, price)
    fun ctaLifetime(price: String) = s(R.string.pro_cta_lifetime, price)
    fun fineTrial(price: String) = s(R.string.pro_fine_trial, price)
    fun fineMonth(price: String) = s(R.string.pro_fine_month, price)
    fun fineYear(price: String) = s(R.string.pro_fine_year, price)
    fun fineLifetime(price: String) = s(R.string.pro_fine_lifetime, price)

    fun featureName(feature: ProFeature): String = when (feature) {
        ProFeature.SmartExtract -> s(R.string.pro_name_smart)
        ProFeature.Filters -> s(R.string.pro_name_filters)
        ProFeature.Shapes -> s(R.string.pro_name_shapes)
        ProFeature.EditTools -> s(R.string.pro_name_edit_tools)
        ProFeature.ExportFormats -> s(R.string.pro_name_formats)
        ProFeature.AdvancedExport -> s(R.string.pro_name_adv_export)
        ProFeature.Collage -> s(R.string.pro_name_collage)
        ProFeature.Batch -> s(R.string.pro_name_batch)
        ProFeature.Compare -> s(R.string.pro_name_compare)
        ProFeature.Accents -> s(R.string.pro_name_accents)
        ProFeature.AppIcons -> s(R.string.pro_name_icons)
        ProFeature.Insights -> s(R.string.pro_name_insights)
        ProFeature.General -> s(R.string.pro_title)
    }

    fun featureLine(feature: ProFeature): String = when (feature) {
        ProFeature.SmartExtract -> s(R.string.pro_feature_smart)
        ProFeature.Filters -> s(R.string.pro_feature_filters)
        ProFeature.Shapes -> s(R.string.pro_feature_shapes)
        ProFeature.EditTools -> s(R.string.pro_feature_edit_tools)
        ProFeature.ExportFormats -> s(R.string.pro_feature_formats)
        ProFeature.AdvancedExport -> s(R.string.pro_feature_adv_export)
        ProFeature.Collage -> s(R.string.pro_feature_collage)
        ProFeature.Batch -> s(R.string.pro_feature_batch)
        ProFeature.Compare -> s(R.string.pro_feature_compare)
        ProFeature.Accents -> s(R.string.pro_feature_accents)
        ProFeature.AppIcons -> s(R.string.pro_feature_icons)
        ProFeature.Insights -> s(R.string.pro_feature_insights)
        ProFeature.General -> s(R.string.pro_subtitle)
    }

    fun doneTitle(): String = s(R.string.pro_done_title)

    fun doneBody(): String = s(R.string.pro_done_body)

    fun doneCta(): String = s(R.string.pro_done_cta)

    fun doneChips(): List<String> = ProShowcase.parse(s(R.string.pro_done_chips))
}
