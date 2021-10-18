package com.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.test.databinding.ItemRcvSwipeBinding

abstract class SwipeOptionAdapter<VHC : RecyclerView.ViewHolder, VHO : RecyclerView.ViewHolder>(recyclerView: RecyclerView) : RecyclerView.Adapter<SwipeOptionAdapter.ViewHolder>() {
    abstract fun onCreateViewHolderContent(parent: ViewGroup, viewType: Int): VHC
    abstract fun onCreateViewHolderOption(parent: ViewGroup, viewType: Int): VHO
    abstract fun onBindViewHolderContent(holder: VHC, position: Int)
    abstract fun onBindViewHolderOption(holder: VHO, position: Int)
    abstract fun getOptionViewWidth(): Int

    private var mCurrentSwipeView: RecyclerView? = null
    private val mOptionViewHalfWidth by lazy { getOptionViewWidth() / 2 }

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && mCurrentSwipeView != null) {
                    mCurrentSwipeView!!.scrollToPosition(0)
                    mCurrentSwipeView = null
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRcvSwipeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.root.apply {
            adapter = Adapter()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (mCurrentSwipeView != recyclerView) {
                        mCurrentSwipeView?.scrollToPosition(0)
                        mCurrentSwipeView = recyclerView
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (computeHorizontalScrollOffset() > mOptionViewHalfWidth) {
                            smoothScrollToPosition(1)
                        } else {
                            smoothScrollToPosition(0)
                        }
                    }
                }
            })
        }
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemViewType(position: Int): Int {
            return position
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                onCreateViewHolderContent(parent, viewType)
            } else {
                onCreateViewHolderOption(parent, viewType)
            }
        }

        override fun getItemCount(): Int {
            return 2
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == 0) {
                onBindViewHolderContent(holder as VHC, position)
            } else {
                onBindViewHolderOption(holder as VHO, position)
            }
        }
    }

    class ViewHolder(val view: ItemRcvSwipeBinding) : RecyclerView.ViewHolder(view.root)
}