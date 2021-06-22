package com.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.databinding.ViewLoadMoreBinding

abstract class LoadMoreAdapter<VH : RecyclerView.ViewHolder>(layoutManager: RecyclerView.LayoutManager) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    abstract fun onCreateViewHolderImp(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    abstract fun getItemCountImp(): Int
    abstract fun onBindViewHolderImp(holder: VH, position: Int)
    abstract fun onLoadMore(view: View)
    private val spanCount: Int

    init {
        if (layoutManager is GridLayoutManager) {
            spanCount = layoutManager.spanCount
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == itemCount - 1) spanCount else 1
                }
            }
        } else {
            spanCount = 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < getItemCountImp() -> 0
            position == itemCount - 1 -> -1
            else -> -2
        }
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            -1 -> LoadMoreViewHolder(parent)
            -2 -> object : RecyclerView.ViewHolder(View(parent.context)) {}
            else -> onCreateViewHolderImp(parent, viewType)
        }
    }

    final override fun getItemCount(): Int {
        return getItemCountImp().let { it + it % spanCount + 1 }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < getItemCountImp()) {
            onBindViewHolderImp(holder as VH, position)
        }
    }

    inner class LoadMoreViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        ViewLoadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            btnLoadMore.setOnClickListener {
                onLoadMore(it)
            }
        }.root
    )
}


/** crash on submitList */
//abstract class LoadMoreAdapter<T, VH : RecyclerView.ViewHolder>(layoutManager: LinearLayoutManager, private val primaryKey: T.() -> Any?) : ListAdapter<T, RecyclerView.ViewHolder>(MainActivity.DiffUtilCallback { primaryKey() }) {
//    abstract fun onCreateViewHolderLoad(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
//    abstract fun getItemCountLoad(): Int
//    abstract fun onBindViewHolderLoad(holder: VH, position: Int)
//    abstract fun onLoadMoreListener(view: View)
//    private val spanCount: Int
//
//    init {
//        if (layoutManager is GridLayoutManager) {
//            spanCount = layoutManager.spanCount
//            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                override fun getSpanSize(position: Int): Int {
//                    return if (position == itemCount - 1) spanCount else 1
//                }
//            }
//        } else {
//            spanCount = 1
//        }
//    }
//
//    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return when (viewType) {
//            -1 -> LoadMoreViewHolder(BtnLoadMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false))
//            -2 -> object : RecyclerView.ViewHolder(View(parent.context)) {}
//            else -> onCreateViewHolderLoad(parent, viewType)
//        }
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        val count = getItemCountLoad()
//        return when {
//            position < count -> 0
//            position == itemCount - 1 -> -1
//            else -> -2
//        }
//    }
//
//    final override fun getItemCount(): Int {
//        val count = getItemCountLoad()
//        return count + count % spanCount + 1
//    }
//
//    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        if (position < getItemCountLoad()) {
//            onBindViewHolderLoad(holder as VH, position)
//        } else if (position == itemCount - 1) {
//            (holder as LoadMoreViewHolder).view.btnLoadMore.setOnClickListener {
//                onLoadMoreListener(it)
//            }
//        }
//    }
//
//    private class LoadMoreViewHolder(val view: BtnLoadMoreBinding) : RecyclerView.ViewHolder(view.root)
//}