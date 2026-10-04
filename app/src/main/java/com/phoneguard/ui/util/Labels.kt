package com.phoneguard.ui.util

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.GetApp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.phoneguard.R
import com.phoneguard.domain.model.DeviceCheckType
import com.phoneguard.domain.model.InstallSource
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.RiskReason
import com.phoneguard.domain.model.RiskyPermission
import com.phoneguard.domain.model.SafeBrowsingStatus
import com.phoneguard.domain.model.ScamSignal
import com.phoneguard.domain.model.ScanStage
import com.phoneguard.domain.model.ScanTrigger
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.model.ScoreGrade
import com.phoneguard.domain.model.SuggestedAction
import com.phoneguard.domain.model.UrlRedFlag
import com.phoneguard.domain.model.WifiSecurity

// Maps domain enums to localised strings and icons. Keeping this in the UI layer means the
// domain stays free of Android resources and every word can be translated.

@get:StringRes
val RiskLevel.labelRes: Int
    get() = when (this) {
        RiskLevel.SAFE -> R.string.risk_safe
        RiskLevel.LOW -> R.string.risk_low
        RiskLevel.MEDIUM -> R.string.risk_medium
        RiskLevel.HIGH -> R.string.risk_high
    }

@get:StringRes
val RiskLevel.groupTitleRes: Int
    get() = when (this) {
        RiskLevel.HIGH -> R.string.group_high
        RiskLevel.MEDIUM -> R.string.group_medium
        RiskLevel.LOW -> R.string.group_low
        RiskLevel.SAFE -> R.string.group_safe
    }

@get:StringRes
val RiskReason.textRes: Int
    get() = when (this) {
        RiskReason.REMOTE_CONTROL_APP -> R.string.reason_remote_control
        RiskReason.SIDELOADED_SENSITIVE_ACCESS -> R.string.reason_sideloaded_sensitive
        RiskReason.SIDELOADED -> R.string.reason_sideloaded
        RiskReason.ACCESSIBILITY_ENABLED -> R.string.reason_accessibility
        RiskReason.DEVICE_ADMIN_ACTIVE -> R.string.reason_device_admin
        RiskReason.READS_SMS -> R.string.reason_sms
        RiskReason.READS_CALL_LOG -> R.string.reason_call_log
        RiskReason.READS_NOTIFICATIONS -> R.string.reason_notifications
        RiskReason.SCREEN_OVERLAY -> R.string.reason_overlay
        RiskReason.CAN_INSTALL_APPS -> R.string.reason_install_apps
        RiskReason.USES_MICROPHONE -> R.string.reason_microphone
        RiskReason.USES_CAMERA -> R.string.reason_camera
        RiskReason.USES_LOCATION -> R.string.reason_location
    }

@get:StringRes
val SuggestedAction.adviceRes: Int
    get() = when (this) {
        SuggestedAction.NO_ACTION -> R.string.advice_no_action
        SuggestedAction.REVIEW_PERMISSIONS -> R.string.advice_review_permissions
        SuggestedAction.TURN_OFF_ACCESSIBILITY -> R.string.advice_turn_off_accessibility
        SuggestedAction.REMOVE_DEVICE_ADMIN -> R.string.advice_remove_device_admin
        SuggestedAction.UNINSTALL_IF_UNKNOWN -> R.string.advice_uninstall_if_unknown
        SuggestedAction.UNINSTALL_NOW -> R.string.advice_uninstall_now
    }

@get:StringRes
val SuggestedAction.buttonRes: Int
    get() = when (this) {
        SuggestedAction.NO_ACTION, SuggestedAction.REVIEW_PERMISSIONS -> R.string.action_review_permissions
        SuggestedAction.TURN_OFF_ACCESSIBILITY -> R.string.action_turn_off_accessibility
        SuggestedAction.REMOVE_DEVICE_ADMIN -> R.string.action_remove_admin
        SuggestedAction.UNINSTALL_IF_UNKNOWN, SuggestedAction.UNINSTALL_NOW -> R.string.action_uninstall
    }

@get:StringRes
val RiskyPermission.titleRes: Int
    get() = when (this) {
        RiskyPermission.SMS -> R.string.perm_sms
        RiskyPermission.CALL_LOG -> R.string.perm_call_log
        RiskyPermission.ACCESSIBILITY -> R.string.perm_accessibility
        RiskyPermission.DEVICE_ADMIN -> R.string.perm_device_admin
        RiskyPermission.NOTIFICATION_ACCESS -> R.string.perm_notification_access
        RiskyPermission.OVERLAY -> R.string.perm_overlay
        RiskyPermission.INSTALL_PACKAGES -> R.string.perm_install_packages
        RiskyPermission.MICROPHONE -> R.string.perm_microphone
        RiskyPermission.CAMERA -> R.string.perm_camera
        RiskyPermission.LOCATION -> R.string.perm_location
    }

@get:StringRes
val RiskyPermission.descriptionRes: Int
    get() = when (this) {
        RiskyPermission.SMS -> R.string.perm_sms_desc
        RiskyPermission.CALL_LOG -> R.string.perm_call_log_desc
        RiskyPermission.ACCESSIBILITY -> R.string.perm_accessibility_desc
        RiskyPermission.DEVICE_ADMIN -> R.string.perm_device_admin_desc
        RiskyPermission.NOTIFICATION_ACCESS -> R.string.perm_notification_access_desc
        RiskyPermission.OVERLAY -> R.string.perm_overlay_desc
        RiskyPermission.INSTALL_PACKAGES -> R.string.perm_install_packages_desc
        RiskyPermission.MICROPHONE -> R.string.perm_microphone_desc
        RiskyPermission.CAMERA -> R.string.perm_camera_desc
        RiskyPermission.LOCATION -> R.string.perm_location_desc
    }

val RiskyPermission.icon: ImageVector
    get() = when (this) {
        RiskyPermission.SMS -> Icons.Rounded.Sms
        RiskyPermission.CALL_LOG -> Icons.Rounded.Call
        RiskyPermission.ACCESSIBILITY -> Icons.Rounded.TouchApp
        RiskyPermission.DEVICE_ADMIN -> Icons.Rounded.AdminPanelSettings
        RiskyPermission.NOTIFICATION_ACCESS -> Icons.Rounded.Notifications
        RiskyPermission.OVERLAY -> Icons.Rounded.Layers
        RiskyPermission.INSTALL_PACKAGES -> Icons.Rounded.GetApp
        RiskyPermission.MICROPHONE -> Icons.Rounded.Mic
        RiskyPermission.CAMERA -> Icons.Rounded.CameraAlt
        RiskyPermission.LOCATION -> Icons.Rounded.LocationOn
    }

@get:StringRes
val InstallSource.labelRes: Int
    get() = when (this) {
        InstallSource.PLAY_STORE -> R.string.source_play_store
        InstallSource.GALAXY_STORE -> R.string.source_galaxy_store
        InstallSource.AMAZON_APPSTORE -> R.string.source_amazon
        InstallSource.OTHER_APP -> R.string.source_other_app
        InstallSource.UNKNOWN -> R.string.source_unknown
    }

@get:StringRes
val DeviceCheckType.titleRes: Int
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> R.string.device_screen_lock
        DeviceCheckType.DEVELOPER_OPTIONS -> R.string.device_developer_options
        DeviceCheckType.USB_DEBUGGING -> R.string.device_usb_debugging
        DeviceCheckType.UNKNOWN_SOURCES -> R.string.device_unknown_sources
        DeviceCheckType.SECURITY_PATCH -> R.string.device_security_patch
        DeviceCheckType.PLAY_PROTECT -> R.string.device_play_protect
    }

/** Text shown when the check passes. */
@get:StringRes
val DeviceCheckType.passedRes: Int
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> R.string.device_screen_lock_ok
        DeviceCheckType.DEVELOPER_OPTIONS -> R.string.device_developer_options_ok
        DeviceCheckType.USB_DEBUGGING -> R.string.device_usb_debugging_ok
        DeviceCheckType.UNKNOWN_SOURCES -> R.string.device_unknown_sources_ok
        DeviceCheckType.SECURITY_PATCH -> R.string.device_security_patch_ok
        DeviceCheckType.PLAY_PROTECT -> R.string.device_play_protect_info
    }

/** Text shown when the check finds a problem. */
@get:StringRes
val DeviceCheckType.issueRes: Int
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> R.string.device_screen_lock_issue
        DeviceCheckType.DEVELOPER_OPTIONS -> R.string.device_developer_options_issue
        DeviceCheckType.USB_DEBUGGING -> R.string.device_usb_debugging_issue
        DeviceCheckType.UNKNOWN_SOURCES -> R.string.device_unknown_sources_issue
        DeviceCheckType.SECURITY_PATCH -> R.string.device_security_patch_issue
        DeviceCheckType.PLAY_PROTECT -> R.string.device_play_protect_info
    }

/** Why it matters, in one or two short sentences. */
@get:StringRes
val DeviceCheckType.whyRes: Int
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> R.string.device_screen_lock_why
        DeviceCheckType.DEVELOPER_OPTIONS -> R.string.device_developer_options_why
        DeviceCheckType.USB_DEBUGGING -> R.string.device_usb_debugging_why
        DeviceCheckType.UNKNOWN_SOURCES -> R.string.device_unknown_sources_why
        DeviceCheckType.SECURITY_PATCH -> R.string.device_security_patch_why
        DeviceCheckType.PLAY_PROTECT -> R.string.device_play_protect_why
    }

@get:StringRes
val DeviceCheckType.fixRes: Int
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> R.string.fix_set_screen_lock
        DeviceCheckType.DEVELOPER_OPTIONS -> R.string.fix_turn_off_developer
        DeviceCheckType.USB_DEBUGGING -> R.string.fix_turn_off_usb
        DeviceCheckType.UNKNOWN_SOURCES -> R.string.fix_review_installers
        DeviceCheckType.SECURITY_PATCH -> R.string.fix_check_updates
        DeviceCheckType.PLAY_PROTECT -> R.string.fix_open_play_protect
    }

val DeviceCheckType.icon: ImageVector
    get() = when (this) {
        DeviceCheckType.SCREEN_LOCK -> Icons.Rounded.Lock
        DeviceCheckType.DEVELOPER_OPTIONS -> Icons.Rounded.DeveloperMode
        DeviceCheckType.USB_DEBUGGING -> Icons.Rounded.Usb
        DeviceCheckType.UNKNOWN_SOURCES -> Icons.Rounded.GetApp
        DeviceCheckType.SECURITY_PATCH -> Icons.Rounded.SystemUpdate
        DeviceCheckType.PLAY_PROTECT -> Icons.Rounded.Shield
    }

@get:StringRes
val WifiSecurity.labelRes: Int
    get() = when (this) {
        WifiSecurity.OPEN -> R.string.wifi_security_open
        WifiSecurity.WEP -> R.string.wifi_security_wep
        WifiSecurity.WPA_PERSONAL -> R.string.wifi_security_wpa
        WifiSecurity.WPA3 -> R.string.wifi_security_wpa3
        WifiSecurity.ENTERPRISE -> R.string.wifi_security_enterprise
        WifiSecurity.ENHANCED_OPEN -> R.string.wifi_security_enhanced_open
        WifiSecurity.UNKNOWN -> R.string.wifi_security_unknown
    }

@get:StringRes
val UrlRedFlag.textRes: Int
    get() = when (this) {
        UrlRedFlag.INVALID_URL -> R.string.flag_invalid
        UrlRedFlag.IP_ADDRESS_HOST -> R.string.flag_ip_address
        UrlRedFlag.LOOKALIKE_DOMAIN -> R.string.flag_lookalike
        UrlRedFlag.USERINFO_TRICK -> R.string.flag_userinfo
        UrlRedFlag.URL_SHORTENER -> R.string.flag_shortener
        UrlRedFlag.NOT_HTTPS -> R.string.flag_not_https
        UrlRedFlag.PUNYCODE_DOMAIN -> R.string.flag_punycode
        UrlRedFlag.UPI_PAYMENT_LINK -> R.string.flag_upi
        UrlRedFlag.UNSUPPORTED_SCHEME -> R.string.flag_unsupported_scheme
        UrlRedFlag.SUSPICIOUS_TLD -> R.string.flag_suspicious_tld
        UrlRedFlag.MANY_SUBDOMAINS -> R.string.flag_many_subdomains
        UrlRedFlag.UNUSUAL_PORT -> R.string.flag_unusual_port
        UrlRedFlag.SCAM_KEYWORDS -> R.string.flag_scam_keywords
    }

@get:StringRes
val ScamSignal.textRes: Int
    get() = when (this) {
        ScamSignal.FAKE_KYC -> R.string.signal_fake_kyc
        ScamSignal.DIGITAL_ARREST -> R.string.signal_digital_arrest
        ScamSignal.UPI_COLLECT -> R.string.signal_upi_collect
        ScamSignal.FAKE_DELIVERY -> R.string.signal_fake_delivery
        ScamSignal.PRIZE_OR_REFUND -> R.string.signal_prize
        ScamSignal.OTP_REQUEST -> R.string.signal_otp
        ScamSignal.ELECTRICITY_BILL -> R.string.signal_electricity
        ScamSignal.REMOTE_ACCESS -> R.string.signal_remote_access
        ScamSignal.URGENT_THREAT -> R.string.signal_urgent
    }

@get:StringRes
val SafeBrowsingStatus.textRes: Int
    get() = when (this) {
        SafeBrowsingStatus.NO_THREAT -> R.string.sb_no_threat
        SafeBrowsingStatus.THREAT_FOUND -> R.string.sb_threat
        SafeBrowsingStatus.NOT_CONFIGURED -> R.string.sb_not_configured
        SafeBrowsingStatus.OFFLINE -> R.string.sb_offline
        SafeBrowsingStatus.ERROR -> R.string.sb_error
        SafeBrowsingStatus.SKIPPED -> R.string.sb_skipped
    }

@get:StringRes
val ScoreCategory.labelRes: Int
    get() = when (this) {
        ScoreCategory.APPS -> R.string.category_apps
        ScoreCategory.DEVICE -> R.string.category_device
        ScoreCategory.NETWORK -> R.string.category_network
        ScoreCategory.LINKS -> R.string.category_links
    }

val ScoreCategory.icon: ImageVector
    get() = when (this) {
        ScoreCategory.APPS -> Icons.Rounded.Apps
        ScoreCategory.DEVICE -> Icons.Rounded.PhoneAndroid
        ScoreCategory.NETWORK -> Icons.Rounded.Wifi
        ScoreCategory.LINKS -> Icons.Rounded.Link
    }

@get:StringRes
val ScoreGrade.labelRes: Int
    get() = when (this) {
        ScoreGrade.GOOD -> R.string.grade_good
        ScoreGrade.FAIR -> R.string.grade_fair
        ScoreGrade.AT_RISK -> R.string.grade_at_risk
    }

@get:StringRes
val ScanTrigger.labelRes: Int
    get() = when (this) {
        ScanTrigger.MANUAL -> R.string.trigger_manual
        ScanTrigger.SCHEDULED -> R.string.trigger_scheduled
        ScanTrigger.NEW_APP -> R.string.trigger_new_app
    }

@get:StringRes
val ScanStage.labelRes: Int
    get() = when (this) {
        ScanStage.APPS -> R.string.stage_apps
        ScanStage.DEVICE -> R.string.stage_device
        ScanStage.NETWORK -> R.string.stage_network
        ScanStage.SCORING -> R.string.stage_scoring
    }

val ScanStage.icon: ImageVector
    get() = when (this) {
        ScanStage.APPS -> Icons.Rounded.Apps
        ScanStage.DEVICE -> Icons.Rounded.PhoneAndroid
        ScanStage.NETWORK -> Icons.Rounded.Wifi
        ScanStage.SCORING -> Icons.Rounded.Code
    }
