package su.android.dialog

import su.android.core.R
import su.android.events.DialogBuilder
import su.android.view.SuperSUDialog

class SuperuserRevokeDialog(
    private val appName: String,
    private val onSuccess: () -> Unit
) : DialogBuilder {

    override fun build(dialog: SuperSUDialog) {
        dialog.apply {
            setTitle(R.string.su_revoke_title)
            setMessage(R.string.su_revoke_msg, appName)
            setButton(SuperSUDialog.ButtonType.POSITIVE) {
                text = android.R.string.ok
                onClick { onSuccess() }
            }
            setButton(SuperSUDialog.ButtonType.NEGATIVE) {
                text = android.R.string.cancel
            }
        }
    }
}
