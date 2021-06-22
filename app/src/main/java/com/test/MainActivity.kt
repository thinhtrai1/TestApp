package com.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private val list = listOf("Thinh", "Nguyen", "Duc", "Duc Thinh", "Nguyen Duc Thinh")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

//        mBinding.pdfView.setRatio(2).renderUrl("https://github.com/barteksc/AndroidPdfViewer/files/867321/testingcrashpdf.pdf")
//        mBinding.pdfView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val layoutManager = FlexboxLayoutManager(this, FlexDirection.ROW)
        val adapter = FlexAdapter()
        mBinding.rcvFlexBox.adapter = adapter
        mBinding.rcvFlexBox.layoutManager = layoutManager
        mBinding.rcvFlexBox.post { // for last line no grow
            val itemCount = layoutManager.flexLines.last().itemCount
            adapter.noGrowIndex = adapter.randoms.size - itemCount
            adapter.notifyItemRangeChanged(adapter.noGrowIndex, itemCount)
        }

        val gridLayoutManager = GridLayoutManager(this, (1..5).random())
        mBinding.rcvGrid.adapter = GridAdapter(gridLayoutManager)
        mBinding.rcvGrid.layoutManager = gridLayoutManager
    }

    inner class FlexAdapter : RecyclerView.Adapter<ViewHolder>() {
        var noGrowIndex = 1000
        val randoms = List(20) { list.random() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.item_rcv_flex, parent, false))
        }

        override fun getItemCount(): Int {
            return randoms.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = randoms[position]
            (holder.itemView.layoutParams as FlexboxLayoutManager.LayoutParams).flexGrow = if (position < noGrowIndex) 1f else 0F
        }
    }

    inner class GridAdapter(layoutManager: GridLayoutManager) : LoadMoreAdapter<ViewHolder>(layoutManager) {
        private val testSize = (1..20).random()

        override fun onCreateViewHolderImp(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.item_rcv_grid, parent, false))
        }

        override fun getItemCountImp(): Int {
            return testSize
        }

        override fun onBindViewHolderImp(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = list.random()
        }

        override fun onLoadMore(view: View) {
            recreate()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}