package com.example.push.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import com.example.push.R
import kotlin.ClassCastException

class ConfirmCancelDialog : AppCompatDialogFragment() {
    lateinit var listener : CancelServiceDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.confirm_cancel_dialog, null)

        val dialog = builder.setView(view)
            .setNegativeButton("No") { dialogInferface, i ->

            }
            .setPositiveButton("Yes") { dialogInterface, i->
                listener.confirmCancel()
            }.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(),
            R.color.cancelRed
        ))

        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = parentFragment as CancelServiceDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("Calling fragment must implement CancelServiceDialogListener")
        }
    }

    interface CancelServiceDialogListener {
        fun confirmCancel()
    }


}