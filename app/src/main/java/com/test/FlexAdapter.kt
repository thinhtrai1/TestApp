package com.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class FlexAdapter : RecyclerView.Adapter<FlexAdapter.ViewHolder>() {
    var noGrowIndex = 1000
    val randoms = List(20) { MainActivity.DATA.random() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_rcv_flex, parent, false))
    }

    override fun getItemCount(): Int {
        return randoms.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as TextView).text = randoms[position]
        (holder.itemView.layoutParams as FlexboxLayoutManager.LayoutParams).flexGrow = if (position < noGrowIndex) 1f else 0F
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}