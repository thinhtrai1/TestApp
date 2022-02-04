package com.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

class WheelBottomSheetDialog: BottomSheetDialogFragment() {
    companion object {
        fun newInstance() = WheelBottomSheetDialog()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val months = ArrayList<String>()
        with(Calendar.getInstance(Locale.JAPAN).apply { set(Calendar.YEAR, 2020) }) {
            while (get(Calendar.YEAR) < 2025) {
                months.add(get(Calendar.YEAR).toString() + "年" + String.format("%02d", get(Calendar.MONTH) + 1) + "月")
                add(Calendar.MONTH, 1)
            }
        }
        return WheelPicker(requireContext(), null).apply {
            setData(months)
        }
    }
}