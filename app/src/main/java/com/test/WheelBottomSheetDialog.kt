package com.test

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

class WheelBottomSheetDialog: BottomSheetDialogFragment() {
    companion object {
        fun newInstance(option: Int) = WheelBottomSheetDialog().apply {
            arguments = Bundle().apply {
                putInt("option", option)
            }
        }
    }

    private val months = ArrayList<String>().apply {
        val cal = Calendar.getInstance(Locale.JAPAN).apply { set(Calendar.YEAR, 2020) }
        while (cal[Calendar.YEAR] < 2025) {
            add(cal[Calendar.YEAR].toString() + "年" + String.format("%02d", cal[Calendar.MONTH] + 1) + "月")
            cal.add(Calendar.MONTH, 1)
        }
    }
    private var view: WheelScrollView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return if (arguments?.getInt("option") == 0) WheelPicker(requireContext(), null).apply {
            setData(months)
        } else WheelScrollView(requireContext(), null).apply {
            setData(months)
            view = this
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireContext().toast(view?.currentPosition?.let { months[it] })
    }
}