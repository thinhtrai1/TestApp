package com.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

//        mBinding.pdfView.setRatio(2).renderUrl("https://github.com/barteksc/AndroidPdfViewer/files/867321/testingcrashpdf.pdf")
//        mBinding.pdfView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val layoutManager = FlexboxLayoutManager(this, FlexDirection.ROW)
        val adapter = MyAdapter()
        mBinding.rcvFlexBox.adapter = adapter
        mBinding.rcvFlexBox.layoutManager = layoutManager
        mBinding.rcvFlexBox.post { // for last line no grow
            val itemCount = layoutManager.flexLines.last().itemCount
            adapter.noGrowIndex = 30 - itemCount
            adapter.notifyItemRangeChanged(adapter.noGrowIndex, itemCount)
        }
    }

    inner class MyAdapter: RecyclerView.Adapter<ViewHolder>() {
        var noGrowIndex = 1000
        private val list = listOf("Thinh", "Nguyen", "Duc", "Duc Thinh", "Nguyen Duc Thinh")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.item_rcv_test, parent, false))
        }

        override fun getItemCount(): Int {
            return 30
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = list.random()
            (holder.itemView.layoutParams as FlexboxLayoutManager.LayoutParams).flexGrow = if (position < noGrowIndex) 1.0f else 0.0F
        }
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view)
}