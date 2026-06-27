package su.android.dialog

import su.android.core.R
import su.android.events.DialogBuilder
import su.android.view.SuperSUDialog

class SecondSlotWarningDialog : DialogBuilder {

    override fun build(dialog: SuperSUDialog) {
        dialog.apply {
            setTitle(android.R.string.dialog_alert_title)
            setMessage(R.string.install_inactive_slot_msg)
            setButton(SuperSUDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
            }
            setCancelable(true)
        }
    }
}
