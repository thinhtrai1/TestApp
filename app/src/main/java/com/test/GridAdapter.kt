package com.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.MainActivity.Companion.DATA

class GridAdapter(layoutManager: GridLayoutManager, private val activity: MainActivity) : LoadMoreAdapter<GridAdapter.ViewHolder>(layoutManager) {
    private val testSize = (1..20).random()

    override fun onCreateViewHolderImp(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(R.layout.item_rcv_grid, parent, false))
    }

    override fun getItemCountImp(): Int {
        return testSize
    }

    override fun onBindViewHolderImp(holder: ViewHolder, position: Int) {
        (holder.itemView as TextView).text = DATA.random()
    }

    override fun onLoadMore(view: View) {
        activity.recreate()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}