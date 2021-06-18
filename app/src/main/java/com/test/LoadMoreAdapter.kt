package com.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.databinding.BtnLoadMoreBinding

abstract class LoadMoreAdapter<VH : RecyclerView.ViewHolder>(layoutManager: GridLayoutManager) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    abstract fun onCreateViewHolderLoad(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    abstract fun onLoadMoreListener(view: View)
    abstract fun getItemCountLoad(): Int
    abstract fun onBindViewHolderLoad(holder: VH, position: Int)
    private val spanCount = layoutManager.spanCount

    init {
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == itemCount - 1) spanCount else 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            -1 -> LoadMoreViewHolder(BtnLoadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            -2 -> object : RecyclerView.ViewHolder(View(parent.context)) {}
            else -> onCreateViewHolderLoad(parent, viewType)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val count = getItemCountLoad()
        return when {
            position < count -> 0
            position == itemCount - 1 -> -1
            else -> -2
        }
    }

    override fun getItemCount(): Int {
        val count = getItemCountLoad()
        return count + count % spanCount + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < getItemCountLoad()) {
            onBindViewHolderLoad(holder as VH, position)
        } else if (position == itemCount - 1) {
            (holder as LoadMoreViewHolder).view.btnLoadMore.setOnClickListener {
                onLoadMoreListener(it)
            }
        }
    }

    private class LoadMoreViewHolder(val view: BtnLoadMoreBinding) : RecyclerView.ViewHolder(view.root)
}